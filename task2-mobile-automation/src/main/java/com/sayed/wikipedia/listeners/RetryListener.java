package com.sayed.wikipedia.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Applies {@link RetryAnalyzer} to every test automatically.
 *
 * <p>Without this, each {@code @Test} would need {@code retryAnalyzer = RetryAnalyzer.class} on it -
 * boilerplate that someone eventually forgets. Registered once in {@code testng.xml}.
 */
public class RetryListener implements IAnnotationTransformer {

    // Raw Class/Constructor because that is the signature TestNG's interface declares; parameterising
    // them compiles as an overload rather than an override, and the listener silently never runs.
    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null
                || !RetryAnalyzer.class.equals(annotation.getRetryAnalyzerClass())) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
