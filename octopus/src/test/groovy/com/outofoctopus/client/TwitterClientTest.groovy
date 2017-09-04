package com.outofoctopus.client

import static com.google.common.truth.Truth.assertThat
import static org.mockito.Mockito.when;

import com.outofoctopus.proto.TwitterProtos.TwitterAccount
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import org.mockito.Mock;
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class TwitterClientTest extends GroovyTestCase {

    private static final long LAST_TWEET_SENT_ID = 904389367774404608L
    private static final long FIRST_MENTION_ID = 904563738581155840L
    private static final long SECOND_MENTION_ID = 904573053123813376L

    private static TwitterAccount testUser
    private static TwitterClient realTwitterClient
    private static TwitterClient mockTwitterClient
    @Mock private Twitter mockTwitter

    void setUp() {
        super.setUp()

        InputStream input = getClass().getClassLoader().getResourceAsStream("twitter.properties")
        Properties consumer = new Properties()
        consumer.load(input)

        ConfigurationBuilder cb = new ConfigurationBuilder()
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(consumer.getProperty("app.oauth.consumerKey"))
            .setOAuthConsumerSecret(consumer.getProperty("app.oauth.consumerSecret"))

        realTwitterClient = new TwitterClient(new TwitterFactory(cb.build()).getInstance())
        testUser = TwitterAccount.newBuilder()
                .setHandle(consumer.getProperty("testaccount.accountName"))
                .setAccessToken(consumer.getProperty("testaccount.oauth.accessToken"))
                .setAccessTokenSecret(consumer.getProperty("testaccount.oauth.accessTokenSecret"))
                .build()

        MockitoAnnotations.initMocks(this);
        mockTwitterClient = new TwitterClient(mockTwitter)
    }

    void tearDown() {
        super.tearDown()
    }

    void testLastTweetSent() {
        realTwitterClient.authenticate(testUser)
        assertThat(realTwitterClient.lastTweetSentId()).isEqualTo(LAST_TWEET_SENT_ID);
    }

    void testReturnsZeroWhenNoTweetsSent() {
        ResponseList<Status> emptyResponse = Mockito.mock(ResponseList.class)
        when(emptyResponse.isEmpty()).thenReturn(true)
        when(mockTwitter.getUserTimeline()).thenReturn(emptyResponse)
        assertThat(mockTwitterClient.lastTweetSentId()).isEqualTo(0)
    }

    void testNewTweetsSinceAllTime() {
        realTwitterClient.authenticate(testUser)
        ResponseList<Status> results = realTwitterClient.newTweets(0)
        assertThat(results).hasSize(2)
        assertThat(results.get(0).getId()).isEqualTo(SECOND_MENTION_ID) // newest first
        assertThat(results.get(1).getId()).isEqualTo(FIRST_MENTION_ID)
    }

    void testNewTweetsSinceTime() {
        realTwitterClient.authenticate(testUser)
        ResponseList<Status> results = realTwitterClient.newTweets(FIRST_MENTION_ID)
        assertThat(results).hasSize(1)
        assertThat(results.get(0).getId()).isEqualTo(SECOND_MENTION_ID)
    }

    // make reply to tweet

    // send tweets
}
