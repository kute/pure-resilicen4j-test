package com.kute.resilience4j.service.impl;

import com.kute.resilience4j.exception.DefaultException;
import com.kute.resilience4j.service.DemoService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * created by bailong001 on 2019/02/13 17:11
 */
// 放在类上，表示 对所有 public 方法生效，也可以放在单个方法上，name为 circuitbreaker名称
@CircuitBreaker(name = "defaultBreaker")
@Service("demoService")
public class DemoServiceImpl implements DemoService {
    @Override
    public String success() {
        return null;
    }

    @Override
    public String fail(Integer type) {
        switch (type) {
            case 0:
                throw new DefaultException();
            case 1:
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            case 2:
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            default:
                return "default-fail";
        }
    }
}
