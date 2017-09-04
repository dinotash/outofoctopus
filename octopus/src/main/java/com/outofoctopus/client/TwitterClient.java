package com.outofoctopus.client;

import com.google.inject.Inject;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.ResponseList;
import twitter4j.auth.AccessToken;
import java.io.IOException;

public class TwitterClient {

    private final Twitter twitter;
    private TwitterAccount account;

    @Inject
    TwitterClient(Twitter twitter) throws TwitterException, IOException {
        this.twitter = twitter;
        this.account = TwitterAccount.getDefaultInstance();
    }

    public void authenticate(TwitterAccount account) throws TwitterException {
        AccessToken accessToken = new AccessToken(
          account.getAccessToken(),
          account.getAccessTokenSecret());
        twitter.setOAuthAccessToken(accessToken);
        this.account = account;
    }

    public long lastTweetSentId() throws TwitterException {
        ResponseList<Status> latestTweets = twitter.getUserTimeline();
        if (latestTweets.isEmpty()) {
            return 0;
        }
        return latestTweets.get(0).getId();
    }

    public ResponseList<Status> newTweets(long sinceId) throws TwitterException {
        Paging paging = sinceId == 0 ? new Paging() : new Paging(sinceId);
        return twitter.getMentionsTimeline(paging);
    }
}
