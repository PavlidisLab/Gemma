package ubic.gemma.persistence.util;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Nano
@Configuration
public class MessagesConfig {

    /**
     * Message source for this context, loaded from localized "messages_xx" files
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource bundle = new ReloadableResourceBundleMessageSource();
        bundle.setBasenames( "classpath:messages", "classpath:ubic/gemma/core/messages" );
        bundle.setFallbackToSystemLocale( false );
        return bundle;
    }
}
