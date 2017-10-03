package com.outofoctopus.db;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.inject.AbstractModule;

public class DatastoreModule extends AbstractModule {
    @Override
    public void configure() {
        bind(Datastore.class).toInstance(DatastoreOptions.getDefaultInstance().getService());
    }
}
