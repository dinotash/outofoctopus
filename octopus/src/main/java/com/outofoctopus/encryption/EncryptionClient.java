package com.outofoctopus.encryption;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.cloudkms.v1.CloudKMS;
import com.google.api.services.cloudkms.v1.model.DecryptRequest;
import com.google.api.services.cloudkms.v1.model.DecryptResponse;
import com.google.api.services.cloudkms.v1.model.EncryptRequest;
import com.google.api.services.cloudkms.v1.model.EncryptResponse;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptionClient {
    private final CloudKMS kms;

    @Inject
    public EncryptionClient(CloudKMS kms) {
        this.kms = kms;
    }

    public String encrypt(String key, String plaintext) throws IOException {
        try {
            EncryptRequest request = new EncryptRequest().encodePlaintext(plaintext.getBytes(StandardCharsets.UTF_8));
            EncryptResponse response =
                kms
                .projects()
                .locations()
                .keyRings()
                .cryptoKeys()
                .encrypt(getKeyResourceName(key), request)
                .execute();
            byte[] cipher = response.decodeCiphertext();
            return Base64.getEncoder().withoutPadding().encodeToString(cipher);
        } catch (GoogleJsonResponseException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to encrypt message %s with key %s", plaintext, key), e);
        }
    }

    public String decrypt(String key, String ciphertext) throws IOException {
        try {
            byte[] cipher = Base64.getDecoder().decode(ciphertext);
            DecryptRequest request = new DecryptRequest().encodeCiphertext(cipher);
            DecryptResponse response =
                kms
                .projects()
                .locations()
                .keyRings()
                .cryptoKeys()
                .decrypt(getKeyResourceName(key), request)
                .execute();
            return new String(response.decodePlaintext(), StandardCharsets.UTF_8);
        } catch (GoogleJsonResponseException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to decrypt message %s with key %s", ciphertext, key), e);
        }
    }

    private static String getKeyResourceName(String keyName) {
        return String.format(
            "projects/%s/locations/%s/keyRings/%s/cryptoKeys/%s",
            "outofoctopus", "global", "octopus", keyName);
    }
}
