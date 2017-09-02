package com.outofoctopus.client;

import com.outofoctopus.proto.TwitterProtos;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import java.io.IOException;

public class TwitterClient {

    private final Twitter twitter;

    public TwitterClient(TwitterFactory twitterFactory) throws TwitterException, IOException {
        this.twitter = twitterFactory.getInstance();
    }

    public void authenticate(TwitterProtos.TwitterAccount account) throws TwitterException {
        AccessToken accessToken = new AccessToken(
          account.getAccessToken(),
          account.getAccessTokenSecret());
        twitter.setOAuthAccessToken(accessToken);
    }
}
