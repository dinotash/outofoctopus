package com.outofoctopus.client;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.outofoctopus.client.TwitterClientModule.TwitterEncryptionKey;
import com.outofoctopus.db.MediumDAO;
import com.outofoctopus.db.MediumDAO.MediumName;
import com.outofoctopus.encryption.EncryptionClient;
import java.io.IOException;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterProvider implements Provider<Twitter> {

    private final MediumDAO mediumDAO;
    private final EncryptionClient encryptionClient;
    private final String encryptionKeyName;

    @Inject
    TwitterProvider(
            MediumDAO mediumDAO,
            EncryptionClient encryptionClient,
            @TwitterEncryptionKey String encryptionKeyName) {
        this.mediumDAO = mediumDAO;
        this.encryptionClient = encryptionClient;
        this.encryptionKeyName = encryptionKeyName;
    }

    public Twitter get() {
        try {
            String consumerKey =
                    encryptionClient.decrypt(encryptionKeyName, mediumDAO.getConsumerKey(MediumName.TWITTER).get());
            String consumerSecret =
                    encryptionClient.decrypt(encryptionKeyName, mediumDAO.getConsumerSecret(MediumName.TWITTER).get());

            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(consumerKey)
                    .setOAuthConsumerSecret(consumerSecret);

            return new TwitterFactory(cb.build()).getInstance();
        } catch (IOException e) {
            return null;
        }
    }
}
