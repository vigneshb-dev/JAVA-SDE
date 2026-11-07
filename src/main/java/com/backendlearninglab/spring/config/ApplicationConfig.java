package com.backendlearninglab.spring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Spring Java Configuration.
 * <p>
 * TEACHING FOCUS: @Configuration vs @Component & @Bean vs @Component
 * <ul>
 *   <li><b>@Component</b>: Used when YOU own the source code and can place the annotation on the class.</li>
 *   <li><b>@Bean</b>: Used inside a {@code @Configuration} class when you need to configure and instantiate
 *       a third-party library class (e.g. {@link Clock}, AmazonS3Client, ObjectMapper) whose source code
 *       you cannot modify!</li>
 *   <li><b>CGLIB Proxying:</b> A class annotated with {@code @Configuration} is enhanced by CGLIB.
 *       Calling a {@code @Bean} method multiple times inside the config class intercepts the call and
 *       returns the existing singleton bean rather than re-executing the method!</li>
 * </ul>
 */
@Configuration
public class ApplicationConfig {

    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);

    /**
     * Exposes java.time.Clock as a Spring bean.
     * WHY: Allows unit and integration tests to mock Clock.fixed() for deterministic time testing!
     */
    @Bean
    public Clock clock() {
        log.info("[ApplicationConfig] Registering system UTC Clock bean in ApplicationContext.");
        return Clock.systemUTC();
    }
}
