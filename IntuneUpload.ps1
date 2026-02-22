param (
    [Parameter(Mandatory=$true)] [string]$AccessToken,
    [Parameter(Mandatory=$true)] [string]$IntunewinFile,
    [Parameter(Mandatory=$true)] [string]$AppId,
    [Parameter(Mandatory=$true)] [string]$AppName,
    [Parameter(Mandatory=$true)] [string]$Publisher,
    [Parameter(Mandatory=$true)] [string]$Version,
    [Parameter(Mandatory=$false)] [string]$Description = ""
)

$ErrorActionPreference = "Stop"

function Log-Status ($Msg) {
    Write-Host "STATUS: $Msg"
}

function Read-ApiError($ex) {
    try {
        if ($ex.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($ex.Exception.Response.GetResponseStream())
            return $reader.ReadToEnd()
        }
    } catch {}
    return $ex.Exception.Message
}

function Invoke-GraphRequest($Method, $Url, $Body, $BearerToken) {
    $headers = @{
        "Authorization" = "Bearer $BearerToken"
        "Content-Type"  = "application/json"
    }
    try {
        if ($Body) {
            return Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -Body $Body -ErrorAction Stop
        } else {
            return Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -ErrorAction Stop
        }
    } catch {
        $detail = Read-ApiError $_
        Log-Status "API ERROR [$Method $Url]: $detail"
        throw $_
    }
}

function Wait-ForFileState($Url, $TargetState, $BearerToken) {
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 5
        $file = Invoke-GraphRequest "Get" $Url $null $BearerToken
        Log-Status "File state: $($file.uploadState)"
        if ($file.uploadState -eq $TargetState) { return $file }
        if ($file.uploadState -like "*Failed*") {
            throw "Intune returned failed state: $($file.uploadState)"
        }
    }
    throw "Timeout waiting for file state: $TargetState"
}

try {
    $BaseUrl = "https://graph.microsoft.com/v1.0"
    $LOBType = "microsoft.graph.win32LobApp"

    Log-Status "Extracting encryption manifest from package..."
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $TempDir = Join-Path $env:TEMP ([Guid]::NewGuid().ToString())
    New-Item -ItemType Directory -Path $TempDir | Out-Null

    $zip = [System.IO.Compression.ZipFile]::OpenRead($IntunewinFile)

    # Extract Detection.xml first to get the content filename
    $manifestEntry = $zip.Entries | Where-Object { $_.Name -eq "Detection.xml" } | Select-Object -First 1
    if (-not $manifestEntry) { throw "Could not find Detection.xml in the .intunewin package." }
    $manifestPath = Join-Path $TempDir "Detection.xml"
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($manifestEntry, $manifestPath, $true)
    [xml]$xmlMeta = Get-Content $manifestPath -Raw
    
    # The encrypted content filename comes from the XML
    $contentFileName = $xmlMeta.ApplicationInfo.FileName
    Log-Status "Looking for encrypted content file: $contentFileName"
    Log-Status "DEBUG: All entries in archive: $($zip.Entries.Name -join ', ')"

    # Extract the encrypted content file by name
    $contentEntry = $zip.Entries | Where-Object { $_.Name -eq $contentFileName } | Select-Object -First 1
    if (-not $contentEntry) {
        # Fallback: try to find any non-xml, non-directory file in Contents/
        $contentEntry = $zip.Entries | Where-Object { $_.FullName -like "*/Contents/*" -and $_.Length -gt 0 } | Select-Object -First 1
    }
    if (-not $contentEntry) { 
        Log-Status "All entries: $($zip.Entries | ForEach-Object { $_.FullName } | Out-String)"
        throw "Could not find encrypted content file '$contentFileName' in the .intunewin package." 
    }
    
    $contentFilePath = Join-Path $TempDir $contentEntry.Name
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($contentEntry, $contentFilePath, $true)
    $zip.Dispose()

    Log-Status "Found encrypted content file: $($contentEntry.Name) ($($contentEntry.Length) bytes)"

    # Parse Detection.xml (already loaded above)
    $appInfo = $xmlMeta.ApplicationInfo

    # The UNENCRYPTED size comes from the XML — this is what 'size' must be set to
    [int64]$unencryptedSize = $appInfo.UnencryptedContentSize
    # The ENCRYPTED size is the actual content file on disk
    [int64]$encryptedSize = (Get-Item $contentFilePath).Length

    Log-Status "Unencrypted size: $unencryptedSize bytes, Encrypted content size: $encryptedSize bytes"

    # Build encryption info from XML
    $encryptionKey = $appInfo.EncryptionInfo.EncryptionKey
    $keyBytes = [Convert]::FromBase64String($encryptionKey)
    Log-Status "DEBUG: EncryptionKey length is $($keyBytes.Length) bytes (expected 32)"

    $encryptionInfo = @{
        "encryptionKey"        = $appInfo.EncryptionInfo.EncryptionKey
        "macKey"               = $appInfo.EncryptionInfo.MacKey
        "initializationVector" = $appInfo.EncryptionInfo.InitializationVector
        "mac"                  = $appInfo.EncryptionInfo.Mac
        "fileDigest"           = $appInfo.EncryptionInfo.FileDigest
        "fileDigestAlgorithm"  = $appInfo.EncryptionInfo.FileDigestAlgorithm
        "profileIdentifier"    = "ProfileVersion1"
    }
    
    # 1. Create Content Version
    Log-Status "Creating content version container..."
    $contentVersionUrl = "$BaseUrl/deviceAppManagement/mobileApps/$AppId/$LOBType/contentVersions"
    $contentVersion = Invoke-GraphRequest "Post" $contentVersionUrl "{}" $AccessToken
    $contentVersionId = $contentVersion.id
    Log-Status "Content version created: $contentVersionId"

    # 2. Create Content File entry with CORRECT sizes
    # 'name' comes from ApplicationInfo.FileName (the setup filename), not the encrypted content file name
    Log-Status "Requesting Azure Storage upload URI..."
    $filesUrl = "$BaseUrl/deviceAppManagement/mobileApps/$AppId/$LOBType/contentVersions/$contentVersionId/files"
    $setupFileName = $appInfo.FileName
    Log-Status "DEBUG: Using setup filename: $setupFileName, unencryptedSize: $unencryptedSize, encryptedSize: $encryptedSize"
    $fileBody = @{
        "@odata.type"   = "#microsoft.graph.mobileAppContentFile"
        "name"          = $setupFileName
        "size"          = $unencryptedSize
        "sizeEncrypted" = $encryptedSize
        "manifest"      = $null
    } | ConvertTo-Json
    $file = Invoke-GraphRequest "Post" $filesUrl $fileBody $AccessToken
    $fileId = $file.id
    $fileStatusUrl = "$filesUrl/$fileId"

    # 3. Wait for Azure Storage URI to be provisioned
    Log-Status "Waiting for Azure Storage URI allocation..."
    $file = Wait-ForFileState $fileStatusUrl "azureStorageUriRequestSuccess" $AccessToken
    $storageUri = $file.azureStorageUri
    Log-Status "Storage URI received."

    # 4. Upload encrypted .bin to Azure Storage using 6MB chunks (reference implementation standard)
    Log-Status "Uploading encrypted content to Azure Storage..."
    $chunkSizeBytes = 1024 * 1024 * 6  # 6MB - Microsoft reference standard
    $fileSize = (Get-Item $contentFilePath).Length
    $chunkCount = [Math]::Ceiling($fileSize / $chunkSizeBytes)
    $chunkIds = @()

    $binaryReader = New-Object System.IO.BinaryReader([System.IO.File]::Open($contentFilePath, [System.IO.FileMode]::Open))
    try {
        for ($chunk = 0; $chunk -lt $chunkCount; $chunk++) {
            # 4-digit zero-padded chunk ID (reference impl uses this exact format)
            $chunkId = [Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($chunk.ToString("0000")))
            $chunkIds += $chunkId

            $length = [Math]::Min($chunkSizeBytes, $fileSize - ($chunk * $chunkSizeBytes))
            $bytes = $binaryReader.ReadBytes($length)

            $putBlockUrl = "${storageUri}&comp=block&blockid=$chunkId"
            $headers = @{ "x-ms-blob-type" = "BlockBlob" }
            
            # Retry logic for chunk uploads (as per reference)
            $uploaded = $false
            for ($retry = 0; $retry -lt 5; $retry++) {
                try {
                    Invoke-RestMethod -Uri $putBlockUrl -Method Put -Body $bytes -Headers $headers | Out-Null
                    $uploaded = $true
                    break
                } catch {
                    Start-Sleep -Seconds 5
                }
            }
            if (-not $uploaded) { throw "Failed to upload chunk $chunk after retries." }

            $pct = [Math]::Round((($chunk + 1) / $chunkCount) * 100, 1)
            Log-Status "PROGRESS: $pct% (chunk $($chunk+1)/$chunkCount)"
        }
    } finally {
        $binaryReader.Close()
    }

    # 5. Finalize with block list (no extra headers — just XML body)
    Log-Status "Finalizing Azure Storage upload (Put Block List)..."
    $blockListXml = "<?xml version='1.0' encoding='utf-8'?><BlockList>"
    foreach ($id in $chunkIds) { $blockListXml += "<Latest>$id</Latest>" }
    $blockListXml += "</BlockList>"
    $putBlockListUrl = "${storageUri}&comp=blocklist"
    
    $finalizeOk = $false
    for ($retry = 0; $retry -lt 5; $retry++) {
        try {
            Invoke-RestMethod -Uri $putBlockListUrl -Method Put -Body $blockListXml -ContentType "application/xml" | Out-Null
            $finalizeOk = $true
            break
        } catch {
            $detail = Read-ApiError $_
            Log-Status "Block list retry $($retry+1): $detail"
            Start-Sleep -Seconds 10
        }
    }
    if (-not $finalizeOk) { throw "Failed to finalize block list after retries." }

    # 6. Commit the file to Intune (send encryption info)
    Log-Status "Committing encrypted content to Intune..."
    $commitUrl = "$BaseUrl/deviceAppManagement/mobileApps/$AppId/$LOBType/contentVersions/$contentVersionId/files/$fileId/commit"
    $commitBody = @{ "fileEncryptionInfo" = $encryptionInfo } | ConvertTo-Json -Depth 5
    Invoke-GraphRequest "Post" $commitUrl $commitBody $AccessToken | Out-Null

    # 7. Wait for Intune to verify the file
    Log-Status "Polling for commit verification (commitFileSuccess)..."
    Wait-ForFileState $fileStatusUrl "commitFileSuccess" $AccessToken | Out-Null

    # 8. Final PATCH: Link app to the committed content version
    Log-Status "Linking app to content version..."
    $patchUrl = "$BaseUrl/deviceAppManagement/mobileApps/$AppId"
    # @odata.type required for polymorphic mobileApps endpoint
    $commitAppBody = @{
        "@odata.type"             = "#$LOBType"
        "committedContentVersion" = $contentVersionId
    } | ConvertTo-Json
    Invoke-GraphRequest "Patch" $patchUrl $commitAppBody $AccessToken | Out-Null

    Log-Status "SUCCESS: Application is now READY."
    Remove-Item $TempDir -Recurse -Force -ErrorAction SilentlyContinue
}
catch {
    Log-Status "ERROR: $($_.Exception.Message)"
    Remove-Item $TempDir -Recurse -Force -ErrorAction SilentlyContinue
    exit 1
}
