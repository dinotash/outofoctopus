package com.outofoctopus.scanner;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;

public class OctopusScanner {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public static void main(String[] args) {
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("twitter").build();
        QueryResults<Entity> results = datastore.run(query);
        while (results.hasNext()) {
            Entity thingy = results.next();
            System.out.println(thingy.getValue("handle").get());
        }
    }

}
