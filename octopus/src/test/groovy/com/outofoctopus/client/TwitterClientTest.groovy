package com.outofoctopus.client

import org.apache.commons.lang3.StringUtils

import static com.google.common.truth.Truth.assertThat
import static org.mockito.Mockito.when;

import com.google.cloud.Timestamp
import com.google.common.collect.ImmutableList
import com.outofoctopus.proto.TwitterProtos.TwitterAccount
import org.mockito.Mock;
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.TwitterObjectFactory
import twitter4j.conf.ConfigurationBuilder

class TwitterClientTest extends GroovyTestCase {

    private static final long LAST_TWEET_SENT_ID = 904389367774404608L
    private static final long FIRST_MENTION_ID = 904563738581155840L
    private static final long SECOND_MENTION_ID = 904573053123813376L
    private static final long SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS = 1504688400000000L

    private static final int TWEET_ID = 227
    private static final ImmutableList<Status> TWEETS = ImmutableList.of(
            TwitterObjectFactory.createStatus(
            "{'id': " + TWEET_ID.toString() + ", 'user': {'screen_name': 'xyz'}, 'text': 'test message'}"))

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

    void testReplyDefaultMessage() {
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(testUser)
                .setActiveUntil(Timestamp.ofTimeMicroseconds(SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS).toProto())
                .setLocale("en-GB")
                .setTimezone("Europe/Paris")
                .build()
        realTwitterClient.authenticate(replyUser)

        ImmutableList<StatusUpdate> results = realTwitterClient.prepareReplies(TWEETS)
        String expectedReply = "Hi @xyz, I am away with no internet access until Sep 6, 2017 and may not see your tweet."
        assertThat(results).hasSize(1)
        assertThat(results.get(0).getStatus()).isEqualTo(expectedReply)
        assertThat(results.get(0).getInReplyToStatusId()).isEqualTo(TWEET_ID)

    }

    void testReplyCustomMessage() {
        String customMessage = "wibbly wobbly"
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(testUser)
                .setMessage(customMessage)
                .build()
        realTwitterClient.authenticate(replyUser)
        ImmutableList<StatusUpdate> results = realTwitterClient.prepareReplies(TWEETS)
        assertThat(results).hasSize(1)
        assertThat(results.get(0).getStatus()).isEqualTo( customMessage)
        assertThat(results.get(0).getInReplyToStatusId()).isEqualTo(TWEET_ID)

    }

    void testReplyDifferentLocale() {
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(testUser)
                        .setActiveUntil(Timestamp.ofTimeMicroseconds(SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS).toProto())
                        .setLocale("de")
                        .setTimezone("America/New_York")
                        .build()
        realTwitterClient.authenticate(replyUser)

        ImmutableList<StatusUpdate> results = realTwitterClient.prepareReplies(TWEETS)
        String expectedReply = "Hi @xyz, I am away with no internet access until 06.09.2017 and may not see your tweet."
        assertThat(results).hasSize(1)
        assertThat(results.get(0).getStatus()).isEqualTo(expectedReply)
        assertThat(results.get(0).getInReplyToStatusId()).isEqualTo(TWEET_ID)
    }

    void testReplyDifferentTimeZone() {
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(testUser)
                        .setActiveUntil(Timestamp.ofTimeMicroseconds(SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS).toProto())
                        .setLocale("en-GB")
                        .setTimezone("Pacific/Honolulu") // more than 9 hours behind GMT so it's the day before
                        .build()
        realTwitterClient.authenticate(replyUser)

        ImmutableList<StatusUpdate> results = realTwitterClient.prepareReplies(TWEETS)
        String expectedReply = "Hi @xyz, I am away with no internet access until Sep 5, 2017 and may not see your tweet."
        assertThat(results).hasSize(1)
        assertThat(results.get(0).getStatus()).isEqualTo(expectedReply)
        assertThat(results.get(0).getInReplyToStatusId()).isEqualTo(TWEET_ID)
    }

    void testReplyTooLongAbbreviated() {
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(testUser)
                .setMessage(StringUtils.repeat("a", 200))
                .build()
        realTwitterClient.authenticate(replyUser)

        ImmutableList<StatusUpdate> results = realTwitterClient.prepareReplies(TWEETS)
        String expectedReply = StringUtils.repeat("a", 137) + "..."
        assertThat(results).hasSize(1)
        assertThat(results.get(0).getStatus()).isEqualTo(expectedReply)
        assertThat(results.get(0).getInReplyToStatusId()).isEqualTo(TWEET_ID)
    }
}
