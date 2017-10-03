package com.outofoctopus.client

import static com.google.common.truth.Truth.assertThat

import com.google.cloud.Timestamp
import com.google.common.collect.ImmutableList
import com.google.inject.Guice
import com.google.inject.Injector
import com.outofoctopus.db.DatastoreModule
import com.outofoctopus.db.MediumDAO
import com.outofoctopus.db.MediumDAO.MediumName
import com.outofoctopus.db.MediumDAOModule
import com.outofoctopus.db.TwitterDAO
import com.outofoctopus.db.TwitterDAOModule
import com.outofoctopus.keys.KeyClient
import com.outofoctopus.keys.KeyModule
import com.outofoctopus.proto.TwitterProtos.TwitterAccount
import org.apache.commons.lang3.StringUtils
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

class TwitterClientTest extends GroovyTestCase {

    private static final long FIRST_MENTION_ID = 904563738581155840L
    private static final long SECOND_MENTION_ID = 904573053123813376L
    private static final long SIXTH_SEPTEMBER_2017_9AM_GMT_MICROSECONDS = 1504688400000000L
    private static final String TWITTER_KEY = "octopus-twitter"

    private static TwitterAccount mainTestUser
    private static TwitterAccount sendTestUser
    private static TwitterAccount noTweetsTestUser

    private static TwitterClient twitterClient

    void setUp() {
        super.setUp()

        Injector injector = Guice.createInjector(
                new DatastoreModule(),
                new MediumDAOModule(),
                new TwitterDAOModule(),
                new KeyModule())
        MediumDAO mediumDAO = injector.getInstance(MediumDAO.class)
        TwitterDAO twitterDAO = injector.getInstance(TwitterDAO.class)
        KeyClient keyClient = injector.getInstance(KeyClient.class)

        String consumerKey = keyClient.decrypt(TWITTER_KEY, mediumDAO.getConsumerKey(MediumName.TWITTER).get())
        String consumerSecret = keyClient.decrypt(TWITTER_KEY, mediumDAO.getConsumerSecret(MediumName.TWITTER).get())

        ConfigurationBuilder cb = new ConfigurationBuilder()
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(consumerKey)
            .setOAuthConsumerSecret(consumerSecret)

        twitterClient = new TwitterClient(new TwitterFactory(cb.build()).getInstance(), keyClient, "octopus-twitter")

        mainTestUser = twitterDAO.getAccount("oooctopustest").get()
        sendTestUser = twitterDAO.getAccount("oooctopustest3").get()
        noTweetsTestUser = twitterDAO.getAccount("oooctopustest4").get()
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
