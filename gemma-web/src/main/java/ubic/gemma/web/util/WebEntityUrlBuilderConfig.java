package ubic.gemma.web.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContext;

@Configuration
public class WebEntityUrlBuilderConfig {

    @Bean
    public WebEntityUrlBuilder webEntityUrlBuilder( @Value("${gemma.hosturl}") String gemmaHostUrl, ServletContext servletContext ) {
        return new WebEntityUrlBuilder( gemmaHostUrl, servletContext.getContextPath() );
    }
}
