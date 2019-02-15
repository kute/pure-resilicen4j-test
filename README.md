1、user guide
http://resilience4j.github.io/resilience4j/
https://blog.csdn.net/lonewolf79218/article/details/85260833
http://www.vavr.io/vavr-docs/
https://github.com/RobWin/resilience4j-spring-boot2-demo

> Core modules:

- resilience4j-circuitbreaker: Circuit breaking

- resilience4j-ratelimiter: Rate limiting

- resilience4j-bulkhead: Bulkheading

- resilience4j-retry: Automatic retrying (sync and async)

- resilience4j-cache: Response caching

- resilience4j-timelimiter: Timeout handling

> Add-on modules

- resilience4j-reactor: Spring Reactor adapter

- resilience4j-rxjava2: RxJava2 adapter

- resilience4j-micrometer: Micrometer Metrics exporter

- resilience4j-metrics: Dropwizard Metrics exporter

- resilience4j-prometheus: Prometheus Metrics exporter

- resilience4j-spring-boot: Spring Boot Starter

- resilience4j-ratpack: Ratpack Starter

- resilience4j-retrofit: Retrofit Call Adapter Factories

- resilience4j-vertx: Vertx Future decorator

- resilience4j-consumer: Circular Buffer Event consumer

2、github
https://github.com/resilience4j/resilience4j

3、状态转换

CLOSED ==> OPEN：单向转换。当请求失败率超过阈值时，熔断器的状态由关闭状态转换到打开状态。失败率的阈值默认50%，可以通过设置CircuitBreakerConfig实例的failureRateThreshold属性值进行改变。

OPEN <==> HALF_OPEN：双向转换。打开状态的持续时间结束(由waitDurationInOpenState设置)，熔断器的状态由打开状态转换到半开状态。这时允许一定数量的请求通过，当这些请求的失败率超过阈值，熔断器的状态由半开状态转换回打开状态。半开时请求的数量是由CircuitBreakerConfig实例的ringBufferSizeInHalfOpenState属性值设置的。

HALF_OPEN ==> CLOSED：如果请求失败率小于阈值，则熔断器的状态由半开状态转换到关闭状态。
