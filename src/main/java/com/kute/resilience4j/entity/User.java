package com.kute.resilience4j.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * created by bailong001 on 2019/02/14 12:15
 */
@Data
public class User implements Serializable {

    private Long id;

    private String name;

    private Integer age;

    private Date birthday;

    private Double score;
}
