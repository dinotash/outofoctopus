package com.outofoctopus.client;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import twitter4j.Twitter;

public class TwitterClientModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(TwitterEncryptionKey.class).toInstance("octopus-twitter");
        bind(Twitter.class).toProvider(TwitterProvider.class);
    }

    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface TwitterEncryptionKey {}
}
