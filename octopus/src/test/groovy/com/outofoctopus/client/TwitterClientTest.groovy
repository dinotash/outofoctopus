package com.outofoctopus.client

import org.apache.commons.lang3.StringUtils

import static com.google.common.truth.Truth.assertThat

import com.google.cloud.Timestamp
import com.google.common.collect.ImmutableList
import com.outofoctopus.proto.TwitterProtos.TwitterAccount
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.TwitterFactory
import twitter4j.TwitterObjectFactory
import twitter4j.conf.ConfigurationBuilder

class TwitterClientTest extends GroovyTestCase {

    private static final long FIRST_MENTION_ID = 904563738581155840L
    private static final long SECOND_MENTION_ID = 904573053123813376L
    private static final long SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS = 1504688400000000L

    private static TwitterAccount mainTestUser
    private static TwitterAccount sendTestUser
    private static TwitterAccount noTweetsTestUser

    private static TwitterClient twitterClient

    void setUp() {
        super.setUp()

        InputStream input = getClass().getClassLoader().getResourceAsStream("twitter.properties")
        Properties consumer = new Properties()
        consumer.load(input)

        ConfigurationBuilder cb = new ConfigurationBuilder()
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(consumer.getProperty("app.oauth.consumerKey"))
            .setOAuthConsumerSecret(consumer.getProperty("app.oauth.consumerSecret"))

        twitterClient = new TwitterClient(new TwitterFactory(cb.build()).getInstance())

        mainTestUser = TwitterAccount.newBuilder()
                .setHandle(consumer.getProperty("mainTestAccount.accountName"))
                .setAccessToken(consumer.getProperty("mainTestAccount.oauth.accessToken"))
                .setAccessTokenSecret(consumer.getProperty("mainTestAccount.oauth.accessTokenSecret"))
                .build()

        sendTestUser = TwitterAccount.newBuilder()
                .setHandle(consumer.getProperty("sendTestAccount.oauth.accessToken"))
                .setAccessToken(consumer.getProperty("sendTestAccount.oauth.accessToken"))
                .setAccessTokenSecret(consumer.getProperty("sendTestAccount.oauth.accessTokenSecret"))
                .build()

        noTweetsTestUser = TwitterAccount.newBuilder()
                .setHandle(consumer.getProperty("noTweetsTestAccount.oauth.accessToken"))
                .setAccessToken(consumer.getProperty("noTweetsTestAccount.oauth.accessToken"))
                .setAccessTokenSecret(consumer.getProperty("noTweetsTestAccount.oauth.accessTokenSecret"))
                .build()
    }

    void tearDown() {
        super.tearDown()
    }

    void testNoRepliesWhenNothingToReplyTo() {
        twitterClient.authenticate(noTweetsTestUser)
        assertThat(twitterClient.makeReplies()).isEmpty()
    }

    void testReplyDefaultMessage() {
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(mainTestUser)
                .setActiveUntil(Timestamp.ofTimeMicroseconds(SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS).toProto())
                .setLocale("en-GB")
                .setTimezone("Europe/Paris")
                .build()
        twitterClient.authenticate(replyUser)

        String expectedReply = "Hi @oooctopustest2, I am away with no internet access until Sep 6, 2017 and may not see your tweet."
        checkMainTestUserPreparedReplies(expectedReply, twitterClient.makeReplies())
    }

    void testReplyCustomMessage() {
        String customMessage = "wibbly wobbly"
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(mainTestUser)
                .setMessage(customMessage)
                .build()
        twitterClient.authenticate(replyUser)

        checkMainTestUserPreparedReplies(customMessage, twitterClient.makeReplies())
    }

    void testReplyDifferentLocale() {
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(mainTestUser)
                        .setActiveUntil(Timestamp.ofTimeMicroseconds(SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS).toProto())
                        .setLocale("de")
                        .setTimezone("America/New_York")
                        .build()
        twitterClient.authenticate(replyUser)

        String expectedReply = "Hi @oooctopustest2, I am away with no internet access until 06.09.2017 and may not see your tweet."
        checkMainTestUserPreparedReplies(expectedReply, twitterClient.makeReplies())
    }

    void testReplyDifferentTimeZone() {
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(mainTestUser)
                        .setActiveUntil(Timestamp.ofTimeMicroseconds(SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS).toProto())
                        .setLocale("en-GB")
                        .setTimezone("Pacific/Honolulu") // more than 9 hours behind GMT so it's the day before
                        .build()
        twitterClient.authenticate(replyUser)

        String expectedReply = "Hi @oooctopustest2, I am away with no internet access until Sep 5, 2017 and may not see your tweet."
        checkMainTestUserPreparedReplies(expectedReply, twitterClient.makeReplies())
    }

    void testReplyTooLongAbbreviated() {
        TwitterAccount replyUser =
                TwitterAccount.newBuilder(mainTestUser)
                .setMessage(StringUtils.repeat("a", 200))
                .build()
        twitterClient.authenticate(replyUser)

        String expectedReply = StringUtils.repeat("a", 137) + "..."
        checkMainTestUserPreparedReplies(expectedReply, twitterClient.makeReplies())
    }

    void testSendReply() {
        twitterClient.authenticate(sendTestUser)
        String replyOne = String.format("Reply one %s", System.currentTimeMillis())
        String replyTwo = String.format("Reply two %s", System.currentTimeMillis() + 123456L)

        ImmutableList<StatusUpdate> updates = ImmutableList.of(
                new StatusUpdate(replyOne),
                new StatusUpdate(replyTwo))
        ImmutableList<Status> sent = twitterClient.sendReplies(updates)

        // Check return value
        assertThat(sent).hasSize(2)
        Status sentOne = sent.get(0)
        Status sentTwo = sent.get(1)
        assertThat(sentOne.getText()).isEqualTo(replyOne)
        assertThat(sentOne.isRetweet()).isFalse()
        assertThat(sentTwo.getText()).isEqualTo(replyTwo)
        assertThat(sentTwo.isRetweet()).isFalse()

        // Check it actually sent
        long firstReply = Math.min(sentOne.getId(), sentTwo.getId())
        ResponseList<Status> actuallySent = twitterClient.newTweets(firstReply - 1)
        assertThat(actuallySent).containsExactly(sentOne, sentTwo)
    }

    void checkMainTestUserPreparedReplies(String expectedReply, List<StatusUpdate> updates) {
        assertThat(updates).hasSize(2)
        assertThat(updates.get(0).getStatus()).isEqualTo(expectedReply)
        assertThat(updates.get(1).getStatus()).isEqualTo(expectedReply)
        ImmutableList<Long> inReplyToIds =
                ImmutableList.of(updates.get(0).getInReplyToStatusId(), updates.get(1).getInReplyToStatusId())
        assertThat(inReplyToIds).containsExactly(FIRST_MENTION_ID, SECOND_MENTION_ID)
    }
}
