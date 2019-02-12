package com.kute.test;

import com.alibaba.fastjson.JSONObject;
import com.kute.test.service.BackendService;
import io.github.resilience4j.adapter.RxJava2Adapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Observable;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
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
    private BackendService backendService = new BackendService();

    @Before
    public void before() {
        circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ignoreExceptions(ClassCastException.class, ClassNotFoundException.class)
                .recordExceptions(Exception.class)
                // 额外的失败条件，当 满足此断言时 resillience4j会认为是失败情况
                // 自定义异常解析，何种异常算 一次失败
                .recordFailure(throwable -> Match(throwable).of(
                        Case($(instanceOf(RuntimeException.class)), true),
                        Case($(), false)
                ))
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

        // 针对每种类型的事件，注册事件监听
        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> LOGGER.info("禁止调用：{}", event.toString()))
                .onError(event -> LOGGER.info("失败调用:{}", event.toString()))
                .onReset(event -> LOGGER.info("状态重置事件:{}", event.toString()))
                .onStateTransition(event -> LOGGER.info("状态转变事件：{}", event.toString()))
                .onIgnoredError(event -> LOGGER.info("当错误忽略时触发:{}", event.toString()))
                .onSuccess(event -> LOGGER.info("一次成功的调用", event.toString()));

        // 监听所有事件
        circuitBreaker.getEventPublisher()
                .onEvent(event -> LOGGER.info("事件类型：{}", event.getEventType().toString()));

        // 使用 ring buffer 存储所有事件，然后 使用rxjava 消费
//        CircularEventConsumer<CircuitBreakerEvent> eventConsumer = new CircularEventConsumer<>(10);
//        circuitBreaker.getEventPublisher().onEvent(eventConsumer);
//        List<CircuitBreakerEvent> bufferedEventList = eventConsumer.getBufferedEvents();
//        LOGGER.info("bufferedEventList :{}", bufferedEventList);

        // turn event publish to rxjava reactive stream
        RxJava2Adapter.toFlowable(circuitBreaker.getEventPublisher())
                .filter(event -> event.getEventType() == CircuitBreakerEvent.Type.ERROR)
                .cast(CircuitBreakerOnErrorEvent.class)
                .subscribe(event -> LOGGER.info("rxjava event:{}", event.toString()));
    }

    @Test
    public void test() {
        CheckedFunction0<String> checkedFunction0 = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> "checkedFunction0");

        CircuitBreaker annotherBreaker = CircuitBreaker.ofDefaults("test2");

        CheckedFunction1<String, String> checkedFunction1 = CircuitBreaker.decorateCheckedFunction(annotherBreaker, (input) -> input + ", checkedFunction1");

        Try<String> tryof = Try.of(checkedFunction0)
                .mapTry(checkedFunction1)
                .map(s -> s + "=");

        assertTrue(tryof.isSuccess());
        assertEquals("assert failed", tryof.get(), "checkedFunction0, checkedFunction1=");

        String result = circuitBreaker.executeSupplier(() -> "executeSupplier");
        assertEquals("assert failed", result, "executeSupplier");

        CheckedFunction0<String> checkedFunction01 = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> {
            throw new RuntimeException();
        });

        Try<String> v = Try.of(checkedFunction01)
                .recover(exception -> Match(exception).of(
                        Case($(instanceOf(RuntimeException.class)), () -> "recover-value"),
                        Case($(), () -> "default value")
                ));
        Assert.assertTrue(v.isSuccess());
        Assert.assertEquals("recover-value", v.get());

    }

    /**
     * 1、未达到 buffer size，失败率一直为 -1
     */
    @Test
    public void test1() {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(5)
                .failureRateThreshold(61)
                // 自定义异常解析，何种异常算 一次失败
                .recordFailure(throwable -> Match(throwable).of(
                        Case($(instanceOf(RuntimeException.class)), true),
                        Case($(), false)
                ))
                .build();

        AtomicInteger integer = new AtomicInteger(-1);

        CircuitBreaker breaker = CircuitBreaker.of("test1", config);

        // reset all state
        breaker.reset();

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

        Observable.fromCallable(backendService::doSomething)
                .lift(CircuitBreakerOperator.of(circuitBreaker));

    }

    /**
     * Reactor
     */
    @Test
    public void mono() {

    }

}
