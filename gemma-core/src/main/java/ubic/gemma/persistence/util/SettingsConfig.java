package ubic.gemma.persistence.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Beans declaration for making the settings available via the Spring Environment and placeholder substitution.
 * @author poirigui
 */
@CommonsLog
@Configuration
public class SettingsConfig {

    /**
     * Allow for substitution placeholders with values from the settings.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setPropertySources( propertySources() );
        return configurer;
    }

    private static PropertySources propertySources() throws IOException {
        MutablePropertySources result = new MutablePropertySources();

        result.addLast( new PropertiesPropertySource( "system", System.getProperties() ) );

        boolean userConfigLoaded = false;

        String gemmaConfig = System.getProperty( "gemma.config" );
        if ( gemmaConfig != null ) {
            Path p = Paths.get( gemmaConfig );
            log.debug( "Loading user configuration from " + p.toAbsolutePath() + " since -Dgemma.config is defined." );
            FileSystemResource r = new FileSystemResource( p.toFile() );
            if ( !r.exists() ) {
                throw new RuntimeException( p + " could not be loaded." );
            }
            warnIfReadableByGroupOrOthers( p );
            result.addLast( new ResourcePropertySource( r ) );
            userConfigLoaded = true;
        }

        // load configuration from $CATALINA_BASE
        // TODO: move this in Gemma Web
        String catalinaBase;
        if ( !userConfigLoaded && ( catalinaBase = System.getenv( "CATALINA_BASE" ) ) != null ) {
            Path p = Paths.get( catalinaBase, "Gemma.properties" );
            FileSystemResource r = new FileSystemResource( p.toFile() );
            if ( r.exists() ) {
                log.debug( "Loading user configuration from " + p.toAbsolutePath() + " since $CATALINA_BASE is defined." );
                warnIfReadableByGroupOrOthers( p );
                result.addLast( new ResourcePropertySource( r ) );
                userConfigLoaded = true;
            }
        }

        // load configuration from the home directory
        // TODO: move this in Gemma CLI
        Path p = Paths.get( System.getProperty( "user.home" ), "Gemma.properties" );
        FileSystemResource r = new FileSystemResource( p.toFile() );
        if ( !userConfigLoaded && r.exists() ) {
            log.debug( "Loading user configuration from " + p.toAbsolutePath() + "." );
            warnIfReadableByGroupOrOthers( p );
            result.addLast( new ResourcePropertySource( r ) );
            userConfigLoaded = true;
        }

        // at least one user configuration should be loaded
        if ( !userConfigLoaded ) {
            throw new RuntimeException( "Gemma.properties could not be loaded and no other user configuration were supplied." );
        }

        log.debug( "Loading default configuration files from classpath." );
        result.addLast( new ResourcePropertySource( new ClassPathResource( "default.properties" ) ) );
        result.addLast( new ResourcePropertySource( new ClassPathResource( "project.properties" ) ) );

        ClassPathResource versionResource = new ClassPathResource( "ubic/gemma/version.properties" );
        if ( versionResource.exists() ) {
            result.addLast( new ResourcePropertySource( versionResource ) );
        } else {
            log.warn( "The ubic/gemma/version.properties resource was not found; run `mvn generate-resources -pl gemma-core` to generate it." );
        }

        return result;
    }

    private static void warnIfReadableByGroupOrOthers( Path path ) throws IOException {
        Set<PosixFilePermission> permissions;
        try {
            permissions = Files.getPosixFilePermissions( path );
        } catch ( UnsupportedOperationException e ) {
            return;
        }
        if ( permissions.contains( PosixFilePermission.GROUP_READ ) || permissions.contains( PosixFilePermission.OTHERS_READ ) ) {
            log.warn( String.format( "%s may contain credentials and is not exclusively readable by its owner. Adjust the permissions by running 'chmod go-r %s' to remove this warning.",
                    path.getFileName(), path.toAbsolutePath() ) );
        }
    }
}
