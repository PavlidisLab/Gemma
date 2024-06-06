package ubic.gemma.core.context;

import org.springframework.context.annotation.Configuration;

import java.lang.annotation.*;

/**
 * This will exclude the component or configuration from component scanning.
 * <p>
 * This is inspired by Spring Boot <a href="https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/context/TestComponent.html">@TestComponent</a>
 *
 * @see Configuration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestComponent {
}
