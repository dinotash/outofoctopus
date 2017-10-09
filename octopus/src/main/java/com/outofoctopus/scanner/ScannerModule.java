package com.outofoctopus.scanner;

import com.google.inject.AbstractModule;
import java.time.Clock;

public class ScannerModule extends AbstractModule {

    private static final String PROJECT_NAME = "outofoctopus";

    @Override
    public void configure() {
        bind(Clock.class).toInstance(Clock.systemUTC());
    }
}
