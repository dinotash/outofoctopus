package com.outofoctopus.scanner.twitter;

import com.google.cloud.Timestamp;
import com.outofoctopus.client.TwitterClient;
import com.outofoctopus.db.TwitterDAO;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TwitterProcessor {

    private final Timestamp currentTime;
    private final TwitterDAO dao;
    private final TwitterAccount account;
    private final TwitterClient client;

    TwitterProcessor(TwitterAccount account, TwitterDAO dao) throws IOException, TwitterException {
        this.dao = dao;
        this.account = account;
        this.currentTime = Timestamp.now();
        this.client = new TwitterClient(new TwitterFactory(loadTwitterConfiguration()));
    }

    void process() {
        // If no longer should be active -> set inactive and exit
//        if (account.getActive() && Timestamp.fromProto(account.getActiveUntil()).compareTo(currentTime) <= 0) {
//            updateDatastore(updateActive(account, false));
//            return;
//        }
//
//        // Switch it on if it is now past its start time and it is going to be active until later
//        if (!account.getActive()
//                && Timestamp.fromProto(account.getActiveFrom()).compareTo(currentTime) <= 0
//                && Timestamp.fromProto(account.getActiveUntil()).compareTo(currentTime) >= 0) {
//            updateDatastore(updateActive(account, true));
//        }

        System.out.println("If you're happy and you know it");

        // For active accounts,
        // Find tweet number to start from -> where last left off -> should save it in datastore?
        //  -> Last tweet sent by user?
        //  -> Last one we replied to?
        // Get all tweets since then that mention this user
        // Check the user hasn't already replied...
        // Send a reply to each of them
        // Possibly: update the tweet number last considered...
    }

    private TwitterAccount updateActive(TwitterAccount account, boolean newActiveStatus) {
        return account.toBuilder()
                .setActive(newActiveStatus)
                .build();
    }

    private void updateDatastore(TwitterAccount account) {
        dao.update(account);
    }

    private Configuration loadTwitterConfiguration() throws IOException {
        // Specify consumer key and consumer secret in file main/resources/twitter.properties
        InputStream input = getClass().getClassLoader().getResourceAsStream("twitter.properties");
        Properties consumer = new Properties();
        consumer.load(input);
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(consumer.getProperty("oauth.consumerKey"))
                .setOAuthConsumerSecret(consumer.getProperty("oauth.consumerSecret"));
        return cb.build();
    }
}
