package com.kute.test;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.kute.test.service.BackendService;
import io.github.resilience4j.adapter.RxJava2Adapter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
import io.reactivex.Observable;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static io.github.resilience4j.ratelimiter.event.RateLimiterEvent.Type.FAILED_ACQUIRE;

/**
 * created by bailong001 on 2019/02/05 14:12
 * 速率限制：QPS
 */
public class RateLimiterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterTest.class);

    private RateLimiterConfig rateLimiterConfig;
    private RateLimiter rateLimiter;
    private AtomicInteger num = new AtomicInteger(0);

    private BackendService backendService = new BackendService();

    @Before
    public void before() {

//        rateLimiterConfig = RateLimiterConfig.ofDefaults();
        rateLimiterConfig = RateLimiterConfig.custom()
                // 每个刷新周期内的token数，default 50
                .limitForPeriod(2)
                // 刷新周期，default 500 nanos
                .limitRefreshPeriod(Duration.ofMillis(1000))
                // 限制之后的等待时间，default 5 seconds
                .timeoutDuration(Duration.ofMillis(500))
                .build();

//        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        RateLimiterRegistry registry = RateLimiterRegistry.of(rateLimiterConfig);

//        rateLimiter = RateLimiter.ofDefaults("rateLimiter");
//        rateLimiter = RateLimiter.of("rateLimiter", rateLimiterConfig);
        rateLimiter = registry.rateLimiter("rateLimiter");

        rateLimiter.getEventPublisher()
                .onSuccess(event -> LOGGER.debug("success:{}", event.toString()))
                .onFailure(event -> LOGGER.debug("failure:{}", event.toString()));

        RxJava2Adapter.toFlowable(rateLimiter.getEventPublisher())
                .filter(event -> event.getEventType() == FAILED_ACQUIRE)
                .subscribe(event -> LOGGER.error("failure event:{}", event.toString()));

    }

    private void function(RateLimiter rateLimiter, int sleep) {
        LOGGER.debug("function run with:{}, metrics:{}", num.incrementAndGet(), JSONObject.toJSONString(rateLimiter.getMetrics()));
        if(sleep > 0) {
            try{
                TimeUnit.MILLISECONDS.sleep(sleep);
            } catch(Exception e){

            }
        }
    }

    private void printMerics(RateLimiter rateLimiter) {
        //  availablePermissions：可用 token数，可为负值
        // nanosToWait: 等待下一个有token的大概时间
        // numberOfWaitingThreads：正在等待token的线程数
        // {"availablePermissions":0,"cycle":0,"nanosToWait":506463482,"numberOfWaitingThreads":2}
        LOGGER.info("ratelimiter metrics:{}", JSONObject.toJSONString(rateLimiter.getMetrics()));
    }

    @Test
    public void test() {
        CheckedRunnable checkedRunnable = RateLimiter.decorateCheckedRunnable(rateLimiter, () -> this.function(rateLimiter, 0));

        IntStream.rangeClosed(1, 4).parallel().forEach(i -> {
            Try<Void> run = Try.run(checkedRunnable);
            LOGGER.info("checkedRunnable run index={}, result={}", i, run.isSuccess());
        });

        // 动态调整 token 数
        rateLimiter.changeLimitForPeriod(10);
        // 动态调整 等待时间
        rateLimiter.changeTimeoutDuration(Duration.ZERO);

    }

    @Test
    public void test1() {

        // qps: 2 ，只有 2 个访问有效
//        run(2, 1000, 500, 4);
        // qps: 1 ，只有 3 个访问有效，限制后等待 2000 可以共获得 2 个token，所以 有 3个访问有效
        run(1, 1000, 2000, 4);

    }

    private void run(int limit, int refresh, int timeout, int max) {
        RateLimiter rateLimiter = RateLimiter.of("defaultRateLimiter", RateLimiterConfig.custom()
                .limitForPeriod(limit)
                .limitRefreshPeriod(Duration.ofMillis(refresh))
                .timeoutDuration(Duration.ofMillis(timeout)).build());
        CheckedRunnable checkedRunnable = RateLimiter.decorateCheckedRunnable(rateLimiter,
                () -> this.function(rateLimiter, 0));
        String tag = Joiner.on("/").join(limit, refresh, timeout);
        IntStream.rangeClosed(1, max).parallel().forEach(i -> {
//            boolean permission = rateLimiter.getPermission(Duration.ofMillis(timeout));
            Try<Void> run = Try.run(checkedRunnable);
            run.onSuccess(aVoid -> LOGGER.info("checkedRunnable[{}] run index={}, result=true", tag, i))
                    .onFailure(throwable -> LOGGER.info("checkedRunnable[{}] run index={}, result=false, reason={}", tag, i, throwable.getClass().getSimpleName()));
        });
    }

    @Test
    public void testRxJava() {
        RateLimiter rateLimiter = RateLimiter.ofDefaults("backendName");
        Observable.fromCallable(backendService::doSomething)
                .lift(RateLimiterOperator.of(rateLimiter));
    }

}
