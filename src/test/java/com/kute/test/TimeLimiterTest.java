package com.kute.test;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * created by bailong001 on 2019/02/12 18:39
 * <p>
 * 超时限制
 */
public class TimeLimiterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeLimiterTest.class);

    private TimeLimiterConfig timeLimiterConfig;
    private TimeLimiter timeLimiter;

    @Before
    public void before() {
//        timeLimiterConfig = TimeLimiterConfig.ofDefaults();
        timeLimiterConfig = TimeLimiterConfig.custom()
                // 超时时间
                .timeoutDuration(Duration.ofMillis(2000))
                // 超时后是否中断运行
                .cancelRunningFuture(true)
                .build();

//        timeLimiter = TimeLimiter.ofDefaults();
//        timeLimiter = TimeLimiter.of(Duration.ofMillis(2000L));
        timeLimiter = TimeLimiter.of(timeLimiterConfig);

    }

    @Test
    public void test() {

        Supplier<Future<Integer>>  supplier = this.sleepFunction();

        Callable<?> callable = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);

        Try.ofCallable(callable)
                .onFailure(throwable -> LOGGER.error("try.onFailure", throwable));

    }

    public Supplier<Future<Integer>> sleepFunction() {
        return () -> Executors.newSingleThreadExecutor().submit(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(3000);
            } catch (Exception e) {
                LOGGER.error("sleepFunction error", e);
            }
            return 0;
        });
    }


}
