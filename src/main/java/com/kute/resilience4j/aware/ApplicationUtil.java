package com.kute.resilience4j.aware;

import com.google.common.base.Preconditions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * created by bailong001 on 2019/02/15 10:47
 */
@Configuration
public class ApplicationUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationUtil.applicationContext = applicationContext;
    }

    public static  <T> T getBean(Class<T> tClass) {
        Preconditions.checkNotNull(ApplicationUtil.applicationContext);
        return ApplicationUtil.applicationContext.getBean(tClass);
    }

    public static  <T> T getBean(String name, Class<T> tClass) {
        Preconditions.checkNotNull(ApplicationUtil.applicationContext);
        return ApplicationUtil.applicationContext.getBean(name, tClass);
    }

}
