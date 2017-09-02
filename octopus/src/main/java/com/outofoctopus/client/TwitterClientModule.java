package com.outofoctopus.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TwitterClientModule extends AbstractModule {

    private static final String PROPERTIES_FILE = "twitter.properties";

    @Override
    protected void configure() {}

    @Provides
    Twitter provideTwitter() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        Properties consumer = new Properties();
        consumer.load(input);
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(consumer.getProperty("oauth.consumerKey"))
                .setOAuthConsumerSecret(consumer.getProperty("oauth.consumerSecret"));
        return new TwitterFactory(cb.build()).getInstance();
    }
}
