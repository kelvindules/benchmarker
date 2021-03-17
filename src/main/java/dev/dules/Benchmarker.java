package dev.dules;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dules.annotation.SensitiveInfo;
import dev.dules.util.ClassUtils;

public class Benchmarker {
	static final Logger logger = LoggerFactory.getLogger(Benchmarker.class);

	private boolean loggingEnabled = true;
	private boolean logMethodResult = true;
	private boolean logMethodResultObjectToString = true;

	static final String SENSITIVE_PARAMETER_ANNOTATION = SensitiveInfo.class.getName();
	static final String BENCHMARK_JOINPOINT_TEMPLATE = "[R] {} -> {}: {}";
	static final String BENCHMARK_TEMPLATE = "[B] {} -> {}{} em {} ms";

	@Around("@annotation(dev.dules.annotation.Benchmarked)")
	public Object benchmarkedMethod(final ProceedingJoinPoint joinPoint) throws Throwable {

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
				if (logMethodResultObjectToString
						|| (result instanceof String && !ClassUtils.isPrimitiveOrWrapper(result.getClass()))) {
					logger.info(BENCHMARK_JOINPOINT_TEMPLATE, className, methodName, result);
				} else {
					logger.info(BENCHMARK_JOINPOINT_TEMPLATE, className, methodName,
							result != null ? result.getClass().getSimpleName() : null);
				}
			}

			return result;

		} finally {

			finish = Instant.now();

			final long executionTime = Duration.between(start, finish).toMillis();

			if (loggingEnabled) {
				logger.info(BENCHMARK_TEMPLATE, className, methodName, sanitizedArgs, executionTime);
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
