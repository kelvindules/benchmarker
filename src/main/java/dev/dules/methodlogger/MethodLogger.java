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

    @Value("${method-logger.logging.enabled: true}")
    boolean loggingEnabled;
    @Value("${method-logger.logging.show-result: false}")
    boolean logMethodResult;

    static final String SENSITIVE_PARAMETER_ANNOTATION = SensitiveInfo.class.getName();
    static final String LOG_TEMPLATE = "\n{} -> {}{}: {}[{}]";

    private static final ObjectMapper mapper;
    private static final ObjectWriter writer;

    static {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); 
        writer = mapper.writerWithDefaultPrettyPrinter();
    }

    @Around("@annotation(dev.dules.methodlogger.annotation.LogMethod)")
    public Object loggedMethod(final ProceedingJoinPoint joinPoint) throws Throwable {

        if (!loggingEnabled) {
            return joinPoint.proceed();
        }

        final Object target = joinPoint.getTarget();
        final String className = target.getClass().getSimpleName();
        final String methodName = joinPoint.getSignature().getName();
        final String sanitizedArgs = this.getArgsAsString(joinPoint);

        Instant start = Instant.now();
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } finally {

            final Instant finish = Instant.now();

            final long executionTime = Duration.between(start, finish).toMillis();
            final String executionTimeString = String.format("%s ms", executionTime);

            String resultString = "";

            if (logMethodResult) {
                resultString = writer.writeValueAsString(result);
            }

            logger.info(LOG_TEMPLATE, className, methodName, sanitizedArgs, resultString, executionTimeString);

            result = null;
        }
    }

    private String getArgsAsString(final ProceedingJoinPoint joinPoint) {
        return getSanitizedArgs(joinPoint).toString().replace("[", "(").replace("]", ")");
    }

    private List<Object> getSanitizedArgs(final ProceedingJoinPoint joinPoint) {

        final Object[] args = joinPoint.getArgs();

        final Annotation[][] matrix = this.getAnnotations(joinPoint.getTarget(), joinPoint.getSignature().getName(),
                this.getParameterTypes((MethodSignature) joinPoint.getSignature()));

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
