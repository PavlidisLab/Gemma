package ubic.gemma.core.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Lint various aspects of the configuration for the CLI profile.
 */
@CommonsLog
@Profile("cli")
@Component
public class ConfigurationLinter implements InitializingBean {

    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    @Value("${load.homologene}")
    private boolean loadHomologene;

    @Value("${gemma.hibernate.hbm2ddl.auto}")
    private String hbm2ddl;

    @Override
    public void afterPropertiesSet() {
        if ( autoLoadOntologies ) {
            log.warn( "Auto-loading of ontologies is enabled, this is not recommended for the CLI. Disable it by setting load.ontologies=false in Gemma.properties." );
        }

        if ( loadHomologene ) {
            log.warn( "Homologene is enabled, this is not recommended for the CLI. Disable it by setting load.homologene=false in Gemma.properties." );
        }

        if ( "validate".equals( hbm2ddl ) ) {
            log.warn( "Hibernate is configured to validate the database schema, this is not recommended for the CLI. Disable it by setting gemma.hibernate.hbm2ddl.auto= in Gemma.properties." );
        }
    }
}
