package com.kute.resilience4j.service.impl;

import com.kute.resilience4j.exception.UserException;
import com.kute.resilience4j.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * created by bailong001 on 2019/02/14 11:46
 */
@Service
public class UserServiceImpl implements UserService {

    @CircuitBreaker(name = "userBreaker")
    @Override
    public String getUserName(Integer type) {
        if (0 == type) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (1 == type) {
            throw new UserException("UserException");
        }
        if (2 == type) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return "kute";
    }
}
