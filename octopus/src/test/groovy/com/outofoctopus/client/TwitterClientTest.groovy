package com.outofoctopus.client

import org.junit.Rule
import org.mockito.MockitoAnnotations

import static com.google.common.truth.Truth.assertThat
import static org.mockito.Mockito.when;

import org.mockito.Mockito
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.Twitter
import com.outofoctopus.proto.TwitterProtos.TwitterAccount
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import org.mockito.Mock;

class TwitterClientTest extends GroovyTestCase {

    private static TwitterAccount testUser
    private static TwitterClient realTwitterClient
    private static TwitterClient mockTwitterClient
    @Mock private Twitter mockTwitter

    void setUp() {
        super.setUp()

        InputStream input = getClass().getClassLoader().getResourceAsStream("twitter.properties")
        Properties consumer = new Properties()
        consumer.load(input)

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(consumer.getProperty("app.oauth.consumerKey"))
            .setOAuthConsumerSecret(consumer.getProperty("app.oauth.consumerSecret"));

        realTwitterClient = new TwitterClient(new TwitterFactory(cb.build()).getInstance());
        testUser = TwitterAccount.newBuilder()
                .setHandle(consumer.getProperty("testaccount.accountName"))
                .setAccessToken(consumer.getProperty("testaccount.oauth.accessToken"))
                .setAccessTokenSecret(consumer.getProperty("testaccount.oauth.accessTokenSecret"))
                .build()
        realTwitterClient.authenticate(testUser)

        MockitoAnnotations.initMocks(this);
        mockTwitterClient = new TwitterClient(mockTwitter)
    }

    void tearDown() {
        super.tearDown()
    }

    void testlastTweetSent() {
        assertThat(realTwitterClient.lastTweetSentId()).isEqualTo(904389367774404608L);
    }

    void testReturnsZeroWhenNoTweetsSent() {
        ResponseList<Status> emptyResponse = Mockito.mock(ResponseList.class)
        when(emptyResponse.isEmpty()).thenReturn(true)
        when(mockTwitter.getUserTimeline()).thenReturn(emptyResponse)
        assertThat(mockTwitterClient.lastTweetSentId()).isEqualTo(0)
    }

    // get last tweet sent

    // get tweets since last

    // make reply to tweet

    // send tweets
}
