package com.outofoctopus.scanner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.outofoctopus.client.TwitterClientModule;
import com.outofoctopus.db.DAOModule;
import com.outofoctopus.scanner.twitter.TwitterScanner;
import java.io.IOException;
import twitter4j.TwitterException;

class OctopusScanner {
//    private static final Datastore DATASTORE =
//            DatastoreOptions.newBuilder()
//                    .setHost("http://localhost:8081")
//                    .setProjectId("outofoctopus")
//                    .build()
//                    .getService();

    public static void main(String[] args) throws IOException, TwitterException {
        Injector injector = Guice.createInjector(
                new ScannerModule(),
                new DAOModule(),
                new TwitterClientModule());
        TwitterScanner twitterScanner = injector.getInstance(TwitterScanner.class);
        twitterScanner.scan();
    }
}
