package com.outofoctopus.db;

import com.google.common.collect.ImmutableList;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;

public interface TwitterDAO {
    ImmutableList<TwitterAccount> getActiveAccounts();

    ImmutableList<TwitterAccount> getAccountsToActivate();

    TwitterAccount getAccount(String handle);

    TwitterAccount insert(TwitterAccount newAccount);

    void delete(String handle);

    void update(TwitterAccount updatedAccount);

    // test this shit...
}