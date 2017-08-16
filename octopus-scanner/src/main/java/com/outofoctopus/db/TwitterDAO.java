package com.outofoctopus.db;

import com.google.common.collect.ImmutableList;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;
import java.util.Optional;

public interface TwitterDAO {
    enum TwitterDAOResult {
        UNKNOWN,
        SUCCESS,
        ALREADY_EXISTS,
        NOT_FOUND,
        INVALID_ARGUMENT,
        ERROR_CAN_RETRY,
        ERROR_DO_NOT_RETRY
    }

    ImmutableList<TwitterAccount> getActiveAccounts();

    ImmutableList<TwitterAccount> getAccountsToActivate();

    Optional<TwitterAccount> getAccount(String handle);

    // Updates to database -- returns true iff operation succeeded
    TwitterDAOResult insert(TwitterAccount newAccount);

    TwitterDAOResult delete(String handle);

    TwitterDAOResult update(TwitterAccount updatedAccount);
}