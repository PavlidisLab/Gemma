package ubic.gemma.core.security.concurrent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author poirigui
 */
@Configuration
public class TaskExecutorConfig {

    @Bean
    public static TaskExecutorSecurityContextDelegatePostProcessor taskExecutorSecurityContextDelegatePostProcessor() {
        return new TaskExecutorSecurityContextDelegatePostProcessor();
    }
}
