package ubic.gemma.web.util;

import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.core.context.AsyncFactoryBean;

@Configuration
public class UserAgentAnalyzerConfig {

    @Bean
    public AsyncFactoryBean<UserAgentAnalyzer> userAgentAnalyzer() {
        return AsyncFactoryBean.singleton( () -> UserAgentAnalyzer.newBuilder().build());
    }
}
