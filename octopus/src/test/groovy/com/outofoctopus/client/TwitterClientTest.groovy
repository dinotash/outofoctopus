package com.outofoctopus.client

import com.outofoctopus.proto.TwitterProtos.TwitterAccount
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

class TwitterClientTest extends GroovyTestCase {

    private static TwitterAccount testUser
    private static TwitterClient twitterClient

    void setUp() {
        super.setUp()

        InputStream input = getClass().getClassLoader().getResourceAsStream("twitter.properties")
        Properties consumer = new Properties()
        consumer.load(input)

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(consumer.getProperty("oauth.consumerKey"))
            .setOAuthConsumerSecret(consumer.getProperty("oauth.consumerSecret"));

        twitterClient = new TwitterClient(new TwitterFactory(cb.build()).getInstance());
        testUser = TwitterAccount.newBuilder()
                .setHandle(consumer.getProperty("accountName"))
                .setAccessToken(consumer.getProperty("oauth.accessToken"))
                .setAccessTokenSecret(consumer.getProperty("oauth.accessTokenSecret"))
                .build()
        twitterClient.authenticate(testUser)
    }

    void tearDown() {
        super.tearDown()
    }

    void testWoo() {

    }

    // get last tweet sent

    // get tweets since last

    // make reply to tweet

    // send tweets
}
