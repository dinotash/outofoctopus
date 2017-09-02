package com.outofoctopus.scanner.twitter;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.outofoctopus.db.TwitterDAO;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import twitter4j.TwitterException;
import java.io.IOException;
import java.util.List;

public class TwitterScanner {
    private final TwitterDAO twitterDAO;
    private final TwitterProcessor twitterProcessor;

    @Inject
    TwitterScanner(TwitterDAO twitterDAO, TwitterProcessor twitterProcessor) {
        this.twitterDAO = twitterDAO;
        this.twitterProcessor = twitterProcessor;
    }

    public void scan() throws IOException, TwitterException {
        List<TwitterAccount> activeAccounts = twitterDAO.getActiveAccounts();
        List<TwitterAccount> accountsToActivate = twitterDAO.getAccountsToActivate();
        for (TwitterAccount account : Iterables.concat(activeAccounts, accountsToActivate)) {
            twitterProcessor.setAccount(account).process();

            System.out.println(account.getHandle());
            System.out.println(account.getActiveUntil().toString());
        }
        System.out.println("And we're off to the races");
    }
}
