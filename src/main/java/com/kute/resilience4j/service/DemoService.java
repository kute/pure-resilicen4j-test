package com.kute.resilience4j.service;

/**
 * created by bailong001 on 2019/02/13 17:11
 */
public interface DemoService {

    String success();

    String fail(Integer type);

}
