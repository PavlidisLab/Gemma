package ubic.gemma.core.mail;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.ReportInvalidReferences;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class VelocityConfig {

    @Bean
    public VelocityEngine velocityEngine() {
        Properties props = new Properties();
        props.setProperty( "resource.loaders", "classpathLoader" );
        props.setProperty( "resource.loader.classpathLoader.class", ClasspathResourceLoader.class.getName() );
        props.setProperty( "event_handler.invalid_references.class", ReportInvalidReferences.class.getName() );
        props.setProperty( "event_handler.invalid_references.exception", "true" );
        props.setProperty( "event_handler.invalid_references.null", "true" );
        return new VelocityEngine( props );
    }
}