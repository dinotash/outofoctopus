package com.outofoctopus.client;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Properties;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterClientModule extends AbstractModule {

    private static final String PROPERTIES_FILE = "twitter.properties";

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(TwitterEncryptionKey.class).toInstance("octopus-twitter");
    }

    @Provides
    Twitter provideTwitter() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        Properties consumer = new Properties();
        consumer.load(input);
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(consumer.getProperty("app.oauth.consumerKey"))
            .setOAuthConsumerSecret(consumer.getProperty("app.oauth.consumerSecret"));
        return new TwitterFactory(cb.build()).getInstance();
    }

    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface TwitterEncryptionKey {}
}
