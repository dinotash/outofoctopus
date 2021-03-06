package com.outofoctopus.db;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.KeyFactory;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class TwitterDAOModule extends AbstractModule {

    private static final Datastore DATASTORE = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory TWITTER_KEY_FACTORY = DATASTORE.newKeyFactory().setKind("twitter");
    private static final String PROJECT_NAME = "outofoctopus";

    @Override
    public void configure() {
        bind(TwitterDAO.class).to(TwitterDatastoreDAO.class);
        bind(KeyFactory.class).annotatedWith(TwitterInject.class).toInstance(TWITTER_KEY_FACTORY);
        bind(String.class).annotatedWith(ProjectName.class).toInstance(PROJECT_NAME);
    }

    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    @interface TwitterInject {}

    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    @interface ProjectName{}
}
