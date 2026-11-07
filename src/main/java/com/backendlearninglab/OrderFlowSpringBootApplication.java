package com.backendlearninglab;

import com.backendlearninglab.spring.config.OrderFlowProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * OrderFlow — Spring Boot Application Entry Point (Phase 2).
 * <p>
 * WHAT @SpringBootApplication ACTUALLY DOES:
 * It is a meta-annotation composed of three fundamental Spring annotations:
 * <ol>
 *   <li>{@code @SpringBootConfiguration}: Specialized {@code @Configuration} class that marks this
 *       class as a source of bean definitions for the Spring {@link ApplicationContext}.</li>
 *   <li>{@code @EnableAutoConfiguration}: Instructs Spring Boot to scan all JAR files on the classpath
 *       (via {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}),
 *       evaluating {@code @ConditionalOnClass} and {@code @ConditionalOnMissingBean} to automatically
 *       configure DataSource, Hibernate, Tomcat, Jackson, and DispatcherServlet.</li>
 *   <li>{@code @ComponentScan}: Scans this package ({@code com.backendlearninglab}) and all sub-packages
 *       for classes annotated with {@code @Component}, {@code @Service}, {@code @Repository},
 *       {@code @Controller}, and {@code @RestController}.</li>
 * </ol>
 */
@SpringBootApplication
@EnableConfigurationProperties(OrderFlowProperties.class)
public class OrderFlowSpringBootApplication {

    private static final Logger log = LoggerFactory.getLogger(OrderFlowSpringBootApplication.class);

    public static void main(String[] args) {
        /*
         * WHAT HAPPENS INSIDE SpringApplication.run():
         * 1. Creates an environment (Environment) by merging system properties, OS env vars,
         *    and application.properties.
         * 2. Instantiates the ApplicationContext (AnnotationConfigServletWebServerApplicationContext for web).
         * 3. Runs BeanDefinitionRegistryPostProcessors to scan and parse all @Component classes.
         * 4. Starts the embedded Servlet Container (Tomcat on port 8080).
         * 5. Instantiates all Singleton Beans in topological dependency order.
         * 6. Calls all CommandLineRunner and ApplicationRunner beans.
         */
        ApplicationContext context = SpringApplication.run(OrderFlowSpringBootApplication.class, args);
        log.info("OrderFlow Spring Boot started successfully! Bean definitions count: {}",
                context.getBeanDefinitionCount());
    }

    /**
     * CommandLineRunner executes immediately after the Spring ApplicationContext is fully initialized.
     */
    @Bean
    public CommandLineRunner startupAudit(OrderFlowProperties properties) {
        return args -> {
            log.info("==================================================================");
            log.info("   OrderFlow Spring Boot 3.3 System Active                        ");
            log.info("   Store Name:            {}", properties.getStoreName());
            log.info("   Default Currency:      {}", properties.getDefaultCurrency());
            log.info("   Low Stock Threshold:   {}", properties.getInventory().getLowStockThreshold());
            log.info("==================================================================");
        };
    }
}
