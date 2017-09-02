package com.outofoctopus.client

import com.outofoctopus.proto.TwitterProtos.TwitterAccount

class TwitterClientTest extends GroovyTestCase {

    private static TwitterAccount testUser
    private static TwitterClient twitterClient

    void setUp() {
        super.setUp()

        InputStream input = getClass().getClassLoader().getResourceAsStream("twitter.properties")
        Properties consumer = new Properties()
        consumer.load(input)

        testUser = TwitterAccount.newBuilder()
                .setHandle(consumer.getProperty("accountName"))
                .setAccessToken(consumer.getProperty("oauth.accessToken"))
                .setAccessTokenSecret(consumer.getProperty("oauth.accessTokenSecret"))
                .build()

        twitterClient = new TwitterClient()
        twitterClient.authenticate(testUser)
    }

    void testWoo() {

    }

    // get last tweet sent

    // get tweets since last

    // make reply to tweet

    // send tweets
}
