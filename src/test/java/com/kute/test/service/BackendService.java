package com.kute.test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * created by bailong001 on 2019/01/09 12:09
 */
public class BackendService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendService.class);

    public String doSomething() {
        LOGGER.info("doSomething method");
        return "doSomething";
    }

    public void throwException() {
        LOGGER.info("throwException method");
        throw new RuntimeException();
    }
}
