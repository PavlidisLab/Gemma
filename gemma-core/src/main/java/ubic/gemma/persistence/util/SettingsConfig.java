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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
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
        MutablePropertySources mutablePropertySources = new MutablePropertySources();
        for ( PropertySource<?> ps : propertySources() ) {
            mutablePropertySources.addLast( ps );
        }
        configurer.setPropertySources( mutablePropertySources );
        return configurer;
    }

    private static List<PropertySource<?>> propertySources() throws IOException {
        List<PropertySource<?>> result = new ArrayList<>();
        boolean userConfigLoaded = false;
        String catalinaBase = System.getenv( "CATALINA_BASE" );
        if ( catalinaBase != null ) {
            Path p = Paths.get( catalinaBase, "Gemma.properties" );
            File f = p.toFile();
            log.debug( "Loading configuration from " + f.getAbsolutePath() + " since $CATALINA_BASE is defined." );
            FileSystemResource r = new FileSystemResource( f );
            if ( !r.exists() ) {
                throw new RuntimeException( f.getAbsolutePath() + " could not be loaded." );
            }
            warnIfReadableByGroupOrOthers( p );
            result.add( new ResourcePropertySource( r ) );
            userConfigLoaded = true;
        }
        Path p = Paths.get( System.getProperty( "user.home" ), "Gemma.properties" );
        FileSystemResource r = new FileSystemResource( p.toFile() );
        if ( r.exists() ) {
            warnIfReadableByGroupOrOthers( p );
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

    private static void warnIfReadableByGroupOrOthers( Path path ) throws IOException {
        Set<PosixFilePermission> permissions;
        try {
            permissions = Files.getPosixFilePermissions( path );
        } catch ( UnsupportedOperationException e ) {
            return;
        }
        if ( permissions.contains( PosixFilePermission.GROUP_READ ) || permissions.contains( PosixFilePermission.OTHERS_READ ) ) {
            log.warn( String.format( "%s may contain credentials and is not exclusively readable its owner. Adjust the permissions by running 'chmod go-r %s' to remove this warning.",
                    path.getFileName(), path.toAbsolutePath() ) );
        }
    }
}
