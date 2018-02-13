package com.amazonaws.demo.appsync;

import android.content.Context;

import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;

public class ClientFactory {
    private static volatile AWSAppSyncClient client;

    public synchronized static AWSAppSyncClient getInstance(Context context) {
        if (client == null) {
            client = AWSAppSyncClient.builder()
                    .context(context)
                    .apiKey(new BasicAPIKeyAuthProvider(Constants.APPSYNC_API_KEY))
                    .region(Constants.APPSYNC_REGION)
                    .serverUrl(Constants.APPSYNC_ENDPOINT)
                    .build();
        }
        return client;
    }
}
