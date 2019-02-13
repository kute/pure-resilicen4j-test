package com.kute.test;

import com.kute.test.service.BackendService;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.CheckedFunction0;
import io.vavr.Predicates;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.WebServiceException;

import static io.vavr.API.*;

/**
 * created by bailong001 on 2019/02/12 16:41
 */
public class RetryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryTest.class);

    private RetryConfig retryConfig;
    private Retry retry;

    private BackendService backendService = new BackendService();

    // 入参：重试次数(不算第一次），出参：时间 ms
    private IntervalFunction function = attempt -> {
        LOGGER.info("retry with times:{}", attempt);
        return attempt.longValue() * 1000;
    };

    // time 表示时间
//    private IntervalFunction function = IntervalFunction.of(2000L, time -> time * 2);

    @Before
    public void before() {

//        retryConfig = RetryConfig.ofDefaults();
        retryConfig = RetryConfig.custom()
                // 配置 不会去重试的异常
                .ignoreExceptions(ClassCastException.class)
                // 最大重试次数
                .maxAttempts(3)
                // 定义 作为判定为失败的异常，以此增加失败率
                .retryExceptions(Exception.class)
                // 自定义
                .intervalFunction(function)
                // 定义 对于 异常的重试
                .retryOnException(throwable -> Match(throwable).of(
                        Case($(Predicates.instanceOf(WebServiceException.class)), true),
                        Case($(Predicates.instanceOf(ClassNotFoundException.class)), true),
                        Case($(), false)
                ))
                // 定义 对于 结果的重试
                .retryOnResult(result -> "null".equalsIgnoreCase(String.valueOf(result)))
//                // 重试间隔,注意与 intervalFunction冲突
//                .waitDuration(Duration.ofMillis(1000))
                .build();

//        retry = Retry.ofDefaults("retry");
        retry = Retry.of("retry", retryConfig);

        retry.getEventPublisher().onSuccess(event -> LOGGER.info("success:{}", event.toString()))
                .onError(event -> LOGGER.info("error:{}", event.toString()))
                .onRetry(event -> LOGGER.info("retry:{}", event.toString()))
                .onIgnoredError(event -> LOGGER.info("ignoreerror:{}", event.toString()));

//        retry.getEventPublisher().onEvent(event -> LOGGER.info("onEvent:{}", event.toString()));

    }

    @Test
    public void test() {

        CheckedFunction0<String > function0 = Retry.decorateCheckedSupplier(retry, backendService::throwException);

        Try<String > result = Try.of(function0)
                .recover(throwable -> "recover value");

        LOGGER.info("success:{}, result={}", result.isSuccess(), result.get());

        int n = 0;
        // dead loop
        /*
        do try{
            if(n == 2) {
                throw new RuntimeException();
            }
            n++;
        } catch(Exception e){
            LOGGER.error("error", e);
        } while ( n < 4);
        */
    }

}
