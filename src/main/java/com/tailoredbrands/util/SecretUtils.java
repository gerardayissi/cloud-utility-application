package com.tailoredbrands.util;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import lombok.val;

import java.io.IOException;

public class SecretUtils {

    public static String resolveSecret(String project, String secret) {
        try (val client = SecretManagerServiceClient.create()) {

            val name = SecretVersionName.of(project, secret.replace("secret_", ""), "latest");
            val request = AccessSecretVersionRequest.newBuilder().setName(name.toString()).build();
            val response = client.accessSecretVersion(request);

            return response.getPayload().getData().toStringUtf8();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
