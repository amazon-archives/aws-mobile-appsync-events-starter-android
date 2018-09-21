package com.amazonaws.demo.appsync;

import android.content.Context;

import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;

public class ClientFactory {
    private static volatile AWSAppSyncClient client;

    public synchronized static AWSAppSyncClient getInstance(Context context) {
        if (client == null) {
            AWSConfiguration awsConfig = new AWSConfiguration(context);

            client = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(awsConfig)
                    .build();
        }
        return client;
    }
}
