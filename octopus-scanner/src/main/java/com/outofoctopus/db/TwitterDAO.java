package com.outofoctopus.db;

import com.google.common.collect.ImmutableList;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import java.util.Optional;

public interface TwitterDAO {
    ImmutableList<TwitterAccount> getActiveAccounts();

    ImmutableList<TwitterAccount> getAccountsToActivate();

    Optional<TwitterAccount> getAccount(String handle);

    // Updates to database -- returns true iff operation succeeded
    boolean insert(TwitterAccount newAccount);

    boolean delete(String handle);

    boolean update(TwitterAccount updatedAccount);
}