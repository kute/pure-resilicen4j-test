package com.kute.resilience4j.predicate;

import com.kute.resilience4j.exception.DefaultException;

import static io.vavr.API.*;
import static io.vavr.Predicates.*;

import java.util.function.Predicate;

/**
 * created by bailong001 on 2019/02/14 12:08
 */
public class DefaultFailurePredicate implements Predicate<Throwable> {
    @Override
    public boolean test(Throwable throwable) {
        return Match(throwable).of(
                Case($(instanceOf(DefaultException.class)), true),
                Case($(), false)
        );
    }
}
