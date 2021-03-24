package dev.dules.methodlogger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import dev.dules.methodlogger.MethodLogger;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({MethodLogger.class})
public @interface EnableMethodLogger {
}