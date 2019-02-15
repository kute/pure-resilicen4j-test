package com.kute.resilience4j.predicate;

import java.util.function.Predicate;

/**
 * created by bailong001 on 2019/02/15 12:02
 */
public class UserFailurePredicate implements Predicate<Throwable> {
    @Override
    public boolean test(Throwable throwable) {
        return null != throwable && "UserException".equalsIgnoreCase(throwable.getClass().getSimpleName());
    }
}
