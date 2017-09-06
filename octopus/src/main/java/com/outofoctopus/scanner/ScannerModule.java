package com.outofoctopus.scanner;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Clock;

public class ScannerModule extends AbstractModule {

    private static final String PROJECT_NAME = "outofoctopus";

    @Override
    public void configure() {
        bind(Clock.class).toInstance(Clock.systemUTC());
        bind(String.class).annotatedWith(ProjectName.class).toInstance(PROJECT_NAME);
    }

    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface ProjectName{}
}
