package com.outofoctopus.db;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.KeyFactory;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class MediumDAOModule extends AbstractModule {

    private static final Datastore DATASTORE = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory MEDIUM_KEY_FACTORY = DATASTORE.newKeyFactory().setKind("medium");

    @Override
    public void configure() {
        bind(MediumDAO.class).to(MediumDatastoreDAO.class);
        bind(KeyFactory.class).annotatedWith(MediumInject.class).toInstance(MEDIUM_KEY_FACTORY);
    }

    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface MediumInject {}
}
