package com.outofoctopus.scanner.twitter;

import com.google.cloud.Timestamp;
import com.outofoctopus.db.TwitterDAO;
import com.outofoctopus.proto.TwitterProtos.TwitterAccount;

class TwitterProcessor {

    private final Timestamp currentTime;
    private final TwitterDAO dao;
    private final TwitterAccount account;

    TwitterProcessor(TwitterAccount account, TwitterDAO dao) {
        this.dao = dao;
        this.account = account;
        this.currentTime = Timestamp.now();
    }

    process() {
        // If no longer should be active -> set inactive and exit
//        if (account.getActive() && Timestamp.fromProto(account.getActiveUntil()).compareTo(currentTime) <= 0) {
//            updateDatastore(updateActive(account, false));
//            return;
//        }
//
//        // Switch it on if it is now past its start time and it is going to be active until later
//        if (!account.getActive()
//                && Timestamp.fromProto(account.getActiveFrom()).compareTo(currentTime) <= 0
//                && Timestamp.fromProto(account.getActiveUntil()).compareTo(currentTime) >= 0) {
//            updateDatastore(updateActive(account, true));
//        }

        System.out.println("If you're happy and you know it");

        // For active accounts,
        // Find tweet number to start from -> where last left off -> should save it in datastore?
        //  -> Last tweet sent by user?
        //  -> Last one we replied to?
        // Get all tweets since then that mention this user
        // Check the user hasn't already replied...
        // Send a reply to each of them
        // Possibly: update the tweet number last considered...
    }

    private TwitterAccount updateActive(TwitterAccount account, boolean newActiveStatus) {
        return account.toBuilder()
                .setActive(newActiveStatus)
                .build();
    }

    private void updateDatastore(TwitterAccount account) {
        dao.update(account);
    }
}
