package com.kute.resilience4j.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.kute.resilience4j.aware.ApplicationUtil;
import com.kute.resilience4j.service.DemoService;
import com.kute.resilience4j.service.UserService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.IntStream;

/**
 * created by bailong001 on 2019/01/08 20:26
 * <p>
 * 监控信息地址：
 * http://localhost:8090/actuator
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoController.class);

    @Resource
    private DemoService demoService;
    @Resource
    private UserService userService;
    @Autowired
    private CircuitBreakerProperties circuitBreakerProperties;

    @GetMapping("/demo/{method}")
    public String call(@PathVariable("method") String method,
                       @RequestParam(value = "paraller", required = false, defaultValue = "1") Integer paraller,
                       @RequestParam(value = "breaker", required = false, defaultValue = "defaultBreaker") String breaker,
                       HttpServletRequest request) {
        LOGGER.info("call:{}", method);
        switch (method) {
            // http://localhost:8090/demo/demo/fail?paraller=1&breaker=defaultBreaker&type=0
            case "fail":
                Integer type = Integer.valueOf(request.getParameter("type"));
                IntStream.rangeClosed(1, paraller).parallel().forEach(i -> {
                    Try<String> tryDemo = Try.ofSupplier(() -> demoService.fail(type));
                    tryDemo.onSuccess(action -> LOGGER.info("demoService.fail run ok"))
                            .onFailure(throwable -> LOGGER.info("demoService.fail cause:{}", throwable.getClass().getName()));
                });
                break;
            // http://localhost:8090/demo/demo/user?paraller=1&breaker=userBreaker&type=0
            case "user":
                Integer ntype = Integer.valueOf(request.getParameter("type"));
                IntStream.rangeClosed(1, paraller).parallel().forEach(i -> {
                    Try<String> tryDemo = Try.ofSupplier(() -> userService.getUserName(ntype));
                    tryDemo.onSuccess(action -> LOGGER.info("userService.getUserName run ok"))
                            .onFailure(throwable -> LOGGER.info("userService.getUserName cause:{}", throwable.getClass().getName()));
                });
                break;
            default:
        }
        CircuitBreaker circuitBreaker = getCircuitbreaker(breaker);
        LOGGER.info("=============================================");
        LOGGER.info("{}", JSONObject.toJSONString(ImmutableMap.of("metrics", circuitBreaker.getMetrics(),
                "state", circuitBreaker.getState())));
        return null;
    }

    /**
     * 以原有配置 重新new一个breaker
     *
     * @param breakerName
     * @return
     */
    private CircuitBreaker newCircuitBreaker(String breakerName) {
        return CircuitBreaker.of(breakerName, circuitBreakerProperties.createCircuitBreakerConfig(breakerName));
    }

    /**
     * 获取 已有 breaker
     *
     * @param breakerName
     * @return
     */
    private CircuitBreaker getCircuitbreaker(String breakerName) {
        return getCircuitBreakerRegistry().circuitBreaker(breakerName);
    }

    private List<CircuitBreaker> getAllCircuitBreakers() {
        return getCircuitBreakerRegistry().getAllCircuitBreakers().asJava();
    }

    private CircuitBreakerRegistry getCircuitBreakerRegistry() {
        return ApplicationUtil.getBean("circuitBreakerRegistry", CircuitBreakerRegistry.class);
    }


}
