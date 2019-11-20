package com.reginald.andinvoker.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Remote Interface Annotation
 */
@Documented
@Target({TYPE, METHOD, PARAMETER})
@Retention(RUNTIME)
public @interface RemoteInterface {
}
