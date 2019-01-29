package com.kute.test;

import com.alibaba.fastjson.JSONObject;
import com.kute.test.service.BackenService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Observable;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.Predicates;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * created by bailong001 on 2019/01/09 10:15
 */
public class CircuitBreakerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreakerTest.class);

    private CircuitBreakerConfig circuitBreakerConfig;
    private CircuitBreakerRegistry registry;
    private CircuitBreaker circuitBreaker;
    private BackenService backenService = new BackenService();

    @Before
    public void before() {
        circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ignoreExceptions(ClassCastException.class, ClassNotFoundException.class)
                .recordExceptions(Exception.class)
                // 额外的失败条件，当 满足此断言时 resillience4j会认为是失败情况
                .recordFailure(Predicates.isNull())
                // 熔断开启的失败率阀值，百分比，默认 50%
                .failureRateThreshold(40)
                // 当熔断器关闭时 ring buffer 大小，即允许通过的请求数，默认 100
                .ringBufferSizeInClosedState(5)
                // 当熔断器半开时 ring buffer 大小，即允许通过的请求数， 默认 10
                .ringBufferSizeInHalfOpenState(3)
                // 允许在经过 waitDurationInOpenState 时间后，从全开转变为半开状态
                .enableAutomaticTransitionFromOpenToHalfOpen()
                // 熔断器在切换至半开状态前，应保持多久的开启状态，默认 60s
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();

//        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        registry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        // 创建熔断器
//        circuitBreaker = registry.circuitBreaker("test", circuitBreakerConfig);
//        circuitBreaker = CircuitBreaker.ofDefaults("test");
//        circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);
        circuitBreaker = registry.circuitBreaker("test");
    }

    @Test
    public void test() {
        CheckedFunction0<String> checkedFunction0 = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> "checkedFunction0");

        CircuitBreaker annotherBreaker = CircuitBreaker.ofDefaults("test2");

        CheckedFunction1<String, String> checkedFunction1 = CircuitBreaker.decorateCheckedFunction(annotherBreaker, (input) ->  input + ", checkedFunction1");

        Try<String> tryof = Try.of(checkedFunction0)
                .mapTry(checkedFunction1)
                .map(s -> s + "=");

        assertTrue(tryof.isSuccess());
        assertEquals("assert failed", tryof.get(), "checkedFunction0, checkedFunction1=");

        String  result = circuitBreaker.executeSupplier(() -> "executeSupplier");
        assertEquals("assert failed", result, "executeSupplier");

    }

    /**
     *
     * 1、未达到 buffer size，失败率一直为 -1
     *
     */
    @Test
    public void test1() {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(5)
                .failureRateThreshold(61)
                .build();

        AtomicInteger integer = new AtomicInteger(-1);

        CircuitBreaker breaker = CircuitBreaker.of("test1", config);

        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));

        // 模拟失败
//        breaker.onError(0, new RuntimeException("1"));
        breaker.onSuccess(0);
        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));

        breaker.onError(0, new RuntimeException("2"));
        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));

        breaker.onError(0, new RuntimeException("3"));
        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));

        // buffer size未达到，失败率也一直为 -1
        assertEquals("", CircuitBreaker.State.CLOSED, breaker.getState());

        // 模拟成功
//        breaker.onSuccess(0);  // 4 call
        breaker.onError(0, new RuntimeException("1"));
        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));

        // buffer size未达到，失败率也一直为 -1
        assertEquals("", CircuitBreaker.State.CLOSED, breaker.getState());

        breaker.onSuccess(0);  // 5call
        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));

        // buffer size 已达到，失败率 = 3 / 5 = 60% 未达到 61%
        assertEquals("", CircuitBreaker.State.CLOSED, breaker.getState());

        breaker.onError(0, new RuntimeException("6"));
        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));

        // buffer size 已达到，失败率 = 3 / 5 = 60%
        assertEquals("", CircuitBreaker.State.OPEN, breaker.getState());
//        assertEquals("", CircuitBreaker.State.CLOSED, breaker.getState());

//        breaker.onError(0, new RuntimeException("7"));
//        breaker.onError(0, new RuntimeException("8"));
//        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));
//
//        // buffer size 已达到，失败率 = 3 / 5 = 60%
//        assertEquals("", CircuitBreaker.State.CLOSED, breaker.getState());
//
//        breaker.onError(0, new RuntimeException("9"));
//        LOGGER.info("{}={}", integer.incrementAndGet(), JSONObject.toJSONString(breaker.getMetrics()));
//
//        // buffer size 已达到，失败率 = 4 / 5 = 80%，已达到失败率 61， 故熔断器打开
//        assertEquals("", CircuitBreaker.State.OPEN, breaker.getState());

    }

    @Test
    public void rxjava() {

        Observable.fromCallable(backenService::doSomething)
                .lift(CircuitBreakerOperator.of(circuitBreaker));

    }

    @Test
    public void mono() {

    }

}
