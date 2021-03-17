package dev.dules.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import dev.dules.Benchmarker;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({Benchmarker.class})
public @interface EnableBenchmarker {
}