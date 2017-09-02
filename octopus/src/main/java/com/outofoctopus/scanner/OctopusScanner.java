package com.outofoctopus.scanner;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.outofoctopus.scanner.twitter.TwitterScanner;
import twitter4j.TwitterException;

import java.io.IOException;
//import twitter4j.management.APIStatistics;

class OctopusScanner {
    private static final Datastore DATASTORE = DatastoreOptions.getDefaultInstance().getService();
    private static final String PROJECT_NAME = "outofoctopus";

//    private static final Datastore DATASTORE =
//            DatastoreOptions.newBuilder()
//                    .setHost("http://localhost:8081")
//                    .setProjectId("outofoctopus")
//                    .build()
//                    .getService();

    public static void main(String[] args) throws IOException, TwitterException {
        TwitterScanner twitterScanner = new TwitterScanner(DATASTORE, PROJECT_NAME);
        twitterScanner.scan();
    }
}
