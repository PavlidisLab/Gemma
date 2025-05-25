package ubic.gemma.core.logging.log4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author poirigui
 */
@Configuration
public class Log4jConfig {

    @Bean
    public static TaskExecutorThreadContextInheritPostProcessor taskExecutorThreadContextInheritPostProcessor() {
        return new TaskExecutorThreadContextInheritPostProcessor();
    }
}
