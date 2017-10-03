package com.outofoctopus.client;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.outofoctopus.client.TwitterClientModule.TwitterEncryptionKey;
import com.outofoctopus.encryption.EncryptionClient;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.ResponseList;
import twitter4j.auth.AccessToken;

public class TwitterClient {

    private static final String DEFAULT_MESSAGE = "Hi {USER}, I am away with no internet access until {END_DATE} and may not see your tweet.";
    private static final Locale DEFAULT_LOCALE = new Locale("en-GB");
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");
    private static final int TWEET_LENGTH = 140;

    private final Twitter twitter;
    private final EncryptionClient encryptionClient;
    private final String encryptionKeyName;
    private TwitterAccount account;

    @Inject
    TwitterClient(
            Twitter twitter,
            EncryptionClient encryptionClient,
            @TwitterEncryptionKey String encryptionKeyName
    ) throws TwitterException, IOException {
        this.twitter = twitter;
        this.account = TwitterAccount.getDefaultInstance();
        this.encryptionClient = encryptionClient;
        this.encryptionKeyName = encryptionKeyName;
    }

    public void authenticate(TwitterAccount account) throws TwitterException, IOException {
        AccessToken accessToken = new AccessToken(
          encryptionClient.decrypt(encryptionKeyName, account.getAccessToken()),
          encryptionClient.decrypt(encryptionKeyName, account.getAccessTokenSecret()));
        twitter.setOAuthAccessToken(accessToken);
        this.account = account;
    }

    public ImmutableList<StatusUpdate> makeReplies() throws TwitterException {
        return prepareReplies(newMentions(lastTweetSentId()));
    }

    public ImmutableList<Status> sendReplies(ImmutableList<StatusUpdate> repliesToSend)
            throws TwitterException {
        ImmutableList.Builder<Status> sent = ImmutableList.builder();
        for (StatusUpdate reply : repliesToSend) {
            sent.add(twitter.updateStatus(reply));
        }
        return sent.build();
    }

    private long lastTweetSentId() throws TwitterException {
        ResponseList<Status> latestTweets = twitter.getUserTimeline();
        if (latestTweets.isEmpty()) {
            return 0;
        }
        return latestTweets.get(0).getId();
    }

    private ResponseList<Status> newMentions(long sinceId) throws TwitterException {
        Paging paging = sinceId == 0 ? new Paging() : new Paging(sinceId);
        return twitter.getMentionsTimeline(paging);
    }

    private ResponseList<Status> newTweets(long sinceId) throws TwitterException {
        Paging paging = sinceId == 0 ? new Paging() : new Paging(sinceId);
        return twitter.getUserTimeline(paging);
    }

    private ImmutableList<StatusUpdate> prepareReplies(List<Status> tweetsToReplyTo) {
        ImmutableList.Builder<StatusUpdate> replies = ImmutableList.builder();
        for (Status tweetToReplyTo : tweetsToReplyTo) {
            replies.add(prepareReply(tweetToReplyTo));
        }
        return replies.build();
    }

    private StatusUpdate prepareReply(Status tweetToReplyTo) {
        String baseMessage = account.hasMessage() ? account.getMessage() : DEFAULT_MESSAGE;
        String finalMessage = handleMessageReplacement(baseMessage, tweetToReplyTo);
        StatusUpdate reply = new StatusUpdate(finalMessage);
        reply.setInReplyToStatusId(tweetToReplyTo.getId());
        reply.setDisplayCoordinates(false);
        return reply;
    }

    private String handleMessageReplacement(String baseMessage, Status tweetToReplyTo) {
        String user = tweetToReplyTo.getUser().getScreenName();
        String endDate = getEndDateString();
        return replacedMessage(baseMessage, user, endDate);
    }

    private String replacedMessage(String baseMessage, String user, String endDate) {
        String newMessage = baseMessage
                .replace("{USER}", "@" + user)
                .replace("{END_DATE}", endDate);
        return StringUtils.abbreviate(newMessage, TWEET_LENGTH);
    }

    private String getEndDateString() {
        Instant endMoment = Instant.ofEpochSecond(account.getActiveUntil().getSeconds());
        LocalDate endDate = endMoment.atZone(getZoneId()).toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(getLocale());
        return endDate.format(formatter);
    }

    private Locale getLocale() {
        try {
            return new Locale(account.getLocale());
        } catch (Exception e) {
            return DEFAULT_LOCALE;
        }
    }

    private ZoneId getZoneId() {
        try {
            return ZoneId.of(account.getTimezone());
        } catch (Exception e) {
            return DEFAULT_TIMEZONE;
        }
    }
}
