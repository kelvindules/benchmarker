package dev.dules.methodlogger;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.dules.methodlogger.annotation.SensitiveInfo;
import dev.dules.methodlogger.util.ClassUtils;

@Component
@Aspect
public class MethodLogger {
    static final Logger logger = LoggerFactory.getLogger(MethodLogger.class);

    @Value("${methodlogger.logging.enabled: true}")
    boolean loggingEnabled;
    @Value("${methodlogger.logging.show-method-result: false}")
    boolean logMethodResult;
    @Value("${methodlogger.logging.serialize-method-result: false}")
    boolean serializeMethodResult;

    static final String SENSITIVE_PARAMETER_ANNOTATION = SensitiveInfo.class.getName();
    static final String LOG_JOINPOINT_TEMPLATE = "[R] {} -> {}: {}";
    static final String LOG_TEMPLATE = "[L] {} -> {}{} em {} ms";
    private static final ObjectMapper mapper;
    private static final ObjectWriter writer;

    static {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        writer = mapper.writerWithDefaultPrettyPrinter();
    }

    @Around("@annotation(dev.dules.annotation.LogMethod)")
    public Object loggedMethod(final ProceedingJoinPoint joinPoint) throws Throwable {

        final Object target = joinPoint.getTarget();
        final String className = target.getClass().getSimpleName();

        final String methodName = joinPoint.getSignature().getName();
        final Object[] methodArgs = joinPoint.getArgs();

        final MethodSignature methodSign = (MethodSignature) joinPoint.getSignature();

        final Annotation[][] annotationMatrix = this.getAnnotations(target, methodName,
                this.getParameterTypes(methodSign));

        final List<Object> sanitizedArgs = this.getSanitizedArgs(methodArgs, annotationMatrix);

        Instant start = Instant.now();
        Instant finish;
        try {

            start = Instant.now();

            Object result = joinPoint.proceed();

            if (loggingEnabled && logMethodResult) {
                if (serializeMethodResult
                        || (result instanceof String && !ClassUtils.isPrimitiveOrWrapper(result.getClass()))) {

                    final String serializedResult = writer.writeValueAsString(result);

                    logger.info(LOG_JOINPOINT_TEMPLATE, className, methodName, serializedResult);
                } else {
                    logger.info(LOG_JOINPOINT_TEMPLATE, className, methodName,
                            result != null ? result.getClass().getSimpleName() : null);
                }
            }

            return result;

        } finally {

            finish = Instant.now();

            final long executionTime = Duration.between(start, finish).toMillis();

            if (loggingEnabled) {
                logger.info(LOG_TEMPLATE, className, methodName, sanitizedArgs, executionTime);
            }

        }
    }

    private List<Object> getSanitizedArgs(final Object[] args, final Annotation[][] matrix) {
        final List<Object> sanitizedArgs = new ArrayList<>();
        for (int x = 0; x < args.length; x++) {

            if (args[x] == null) {
                continue;
            }

            boolean isSensitive = false;
            for (final Annotation annotation : matrix[x]) {
                if (annotation.annotationType().getName().contains(SENSITIVE_PARAMETER_ANNOTATION)) {
                    isSensitive = true;
                }
            }
            if (!isSensitive) {
                String arg = "";
                if (args[x] instanceof String || ClassUtils.isPrimitiveOrWrapper(args[x].getClass())) {
                    arg = String.valueOf(args[x]);
                } else {
                    arg = args[x].getClass().getSimpleName();
                }
                sanitizedArgs.add(arg);
            }
        }
        return sanitizedArgs;
    }

    private Annotation[][] getAnnotations(final Object target, final String methodName,
            final Class<?>[] parameterTypes) {
        try {
            return target.getClass().getMethod(methodName, parameterTypes).getParameterAnnotations();
        } catch (final NoSuchMethodException ex) {
            return new Annotation[][] {};
        }
    }

    private Class<?>[] getParameterTypes(final MethodSignature signature) {
        return signature.getMethod().getParameterTypes();
    }
}
