package ubic.gemma.persistence.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Beans declaration for making the settings available via the Spring Environment and placeholder substitution.
 * @author poirigui
 */
@CommonsLog
@Configuration
public class SettingsConfig {

    /**
     * Populates the Spring {@link org.springframework.core.env.Environment} with the content of the settings.
     */
    @Bean
    public static PropertySourcesConfigurer propertySourcesConfigurer() throws IOException {
        return new PropertySourcesConfigurer( propertySources() );
    }

    /**
     * Allow for substitution placeholders with values from the settings.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer settingsBasedPlaceholderConfigurer() throws IOException {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        MutablePropertySources mutablePropertySources = new MutablePropertySources();
        for ( PropertySource<?> ps : propertySources() ) {
            mutablePropertySources.addLast( ps );
        }
        configurer.setPropertySources( mutablePropertySources );
        return configurer;
    }

    /**
     * This is needed for environment-based substitution which is the default for {@link PropertySourcesPlaceholderConfigurer}.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    private static List<PropertySource<?>> propertySources() throws IOException {
        List<PropertySource<?>> result = new ArrayList<>();
        boolean userConfigLoaded = false;
        String catalinaBase = System.getenv( "CATALINA_BASE" );
        if ( catalinaBase != null ) {
            File f = Paths.get( catalinaBase, "Gemma.properties" ).toFile();
            log.debug( "Loading configuration from " + f.getAbsolutePath() + " since $CATALINA_BASE is defined." );
            FileSystemResource r = new FileSystemResource( f );
            if ( !r.exists() ) {
                throw new RuntimeException( f.getAbsolutePath() + " could not be loaded." );
            }
            result.add( new ResourcePropertySource( r ) );
            userConfigLoaded = true;
        }
        FileSystemResource r = new FileSystemResource( Paths.get( System.getProperty( "user.home" ), "Gemma.properties" ).toFile() );
        if ( r.exists() ) {
            result.add( new ResourcePropertySource( r ) );
        } else if ( !userConfigLoaded ) {
            throw new RuntimeException( "Gemma.properties could not be loaded and no other user configuration were supplied." );
        }
        result.add( new ResourcePropertySource( new ClassPathResource( "default.properties" ) ) );
        result.add( new ResourcePropertySource( new ClassPathResource( "project.properties" ) ) );
        // FIXME: local local.properties from ${gemma.appdata.home}
        result.add( new ResourcePropertySource( new ClassPathResource( "ubic/gemma/version.properties" ) ) );
        return result;
    }
}
