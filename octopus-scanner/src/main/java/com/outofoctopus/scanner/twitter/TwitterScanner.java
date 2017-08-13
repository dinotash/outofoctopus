package com.outofoctopus.scanner.twitter;

import com.google.common.collect.Iterables;
import com.google.cloud.datastore.Datastore;
import com.outofoctopus.db.TwitterDAO;
import com.outofoctopus.db.TwitterDatastoreDAO;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import java.util.List;

public class TwitterScanner {
    private final TwitterDAO twitterDAO;

    public TwitterScanner(Datastore datastore, String projectName) {
        this.twitterDAO = new TwitterDatastoreDAO(datastore, projectName);
    }

    public void scan() {
        List<TwitterAccount> activeAccounts = twitterDAO.getActiveAccounts();
        List<TwitterAccount> accountsToActivate = twitterDAO.getAccountsToActivate();
        for (TwitterAccount account : Iterables.concat(activeAccounts, accountsToActivate)) {
            new TwitterProcessor(account, twitterDAO).process();
            System.out.println(account.getHandle());
            System.out.println(account.getActiveUntil().toString());
        }
        System.out.println("And we're off to the races");
    }
}
