package com.kute.test.service;

/**
 * created by bailong001 on 2019/01/09 12:09
 */
public class BackenService {

    public String doSomething() {
        return "doSomething";
    }

    public void throwException() {
        throw new RuntimeException();
    }
}
