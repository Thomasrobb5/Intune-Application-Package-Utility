package com.antigravity.intunepackager;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.InteractiveBrowserCredential;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;

public class GraphAuthService {

    private final String clientId;
    private final String tenantId;

    public GraphAuthService(String clientId, String tenantId) {
        this.clientId = clientId;
        this.tenantId = tenantId;
    }

    /**
     * Authenticates the user interactively and returns a GraphServiceClient.
     * 
     * @return Authenticated GraphServiceClient
     */
    private InteractiveBrowserCredential credential;
    private final String[] scopes = new String[] { "DeviceManagementApps.ReadWrite.All" };

    public GraphServiceClient getGraphClient() {
        if (credential == null) {
            credential = new InteractiveBrowserCredentialBuilder()
                    .clientId(clientId)
                    .tenantId(tenantId)
                    .build();
        }
        return new GraphServiceClient(credential, scopes);
    }

    public String getAccessToken() {
        if (credential == null) {
            getGraphClient();
        }
        AccessToken token = credential.getToken(new TokenRequestContext().addScopes(scopes)).block();
        return token != null ? token.getToken() : null;
    }
}
