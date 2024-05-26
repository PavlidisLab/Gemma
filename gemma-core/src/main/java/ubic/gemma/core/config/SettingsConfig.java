package ubic.gemma.core.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Properties;
import java.util.Set;

/**
 * Beans declaration for making the settings available via the Spring Environment and placeholder substitution.
 * @author poirigui
 */
@CommonsLog
@Configuration
public class SettingsConfig {

    /**
     * Prefix for system properties.
     */
    private static final String SYSTEM_PROPERTY_PREFIX = "gemma.";

    /**
     * System property for loading a specific user configuration file.
     */
    private static final String GEMMA_CONFIG_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "config";

    /**
     * The name of the file users can use to configure Gemma.
     */
    private static final String USER_CONFIGURATION = "Gemma.properties";

    /**
     * Name of the resource that is used to configure Gemma internally.
     */
    private static final String BUILTIN_CONFIGURATION = "project.properties";

    /**
     * Name of the resource containing defaults that the user can override (classpath)
     */
    private static final String DEFAULT_CONFIGURATION = "default.properties";

    /**
     * List of default configurations.
     */
    private static final String[] DEFAULT_CONFIGURATIONS = { DEFAULT_CONFIGURATION, BUILTIN_CONFIGURATION };

    /**
     * Allow for substitution placeholders with values from the settings.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer( @Qualifier("settingsPropertySources") PropertySources ps ) {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setPropertySources( ps );
        return configurer;
    }

    @Bean
    public static BaseCodeConfigurer baseCodeConfigurer( @Qualifier("settingsPropertySources") PropertySources ps ) {
        BaseCodeConfigurer configurer = new BaseCodeConfigurer();
        configurer.setPropertySources( ps );
        return configurer;
    }

    /**
     * The default Spring conversion service has limited support for Java 8 types.
     * <p>
     * This might be resolved in Spring 4+, in which case we could remove this declaration.
     */
    @Bean
    public ConversionService conversionService() {
        DefaultFormattingConversionService service = new DefaultFormattingConversionService();
        service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
        return service;
    }

    /**
     * This is necessary because we read settings twice: once before the context is initialized to get the active
     * profiles and a second time via {@link #propertySourcesPlaceholderConfigurer(PropertySources)} and
     * {@link #baseCodeConfigurer(PropertySources)}.
     */
    private static PropertySources cachedSettingsPropertySources = null;

    /**
     * Property sources populated from various settings files.
     * <p>
     * This is mainly used by {@link #propertySourcesPlaceholderConfigurer(PropertySources)} for substituting
     * {@code ${...}} placeholders.
     */
    @Bean
    public static synchronized PropertySources settingsPropertySources() throws IOException {
        if ( cachedSettingsPropertySources != null ) {
            return cachedSettingsPropertySources;
        }

        MutablePropertySources result = new MutablePropertySources();

        result.addLast( new PropertiesPropertySource( "system", filteredSystemProperties() ) );

        boolean userConfigLoaded = false;

        String gemmaConfig = System.getProperty( GEMMA_CONFIG_SYSTEM_PROPERTY );
        if ( gemmaConfig != null ) {
            Path p = Paths.get( gemmaConfig );
            log.debug( "Loading user configuration from " + p.toAbsolutePath() + " since -Dgemma.config is defined." );
            FileSystemResource r = new FileSystemResource( p.toFile() );
            if ( !r.exists() ) {
                throw new RuntimeException( p + " could not be loaded." );
            }
            warnIfReadableByGroupOrOthers( p );
            result.addLast( new ResourcePropertySource( r.getDescription() + " (from -Dgemma.config)", r ) );
            userConfigLoaded = true;
        }

        // load configuration from $CATALINA_BASE
        // TODO: move this in Gemma Web
        String catalinaBase;
        if ( !userConfigLoaded && ( catalinaBase = System.getenv( "CATALINA_BASE" ) ) != null ) {
            Path p = Paths.get( catalinaBase, USER_CONFIGURATION );
            FileSystemResource r = new FileSystemResource( p.toFile() );
            if ( r.exists() ) {
                log.debug( "Loading user configuration from " + p.toAbsolutePath() + " since $CATALINA_BASE is defined." );
                warnIfReadableByGroupOrOthers( p );
                result.addLast( new ResourcePropertySource( r.getDescription() + " (from $CATALINA_BASE)", r ) );
                userConfigLoaded = true;
            }
        }

        // load configuration from the home directory
        // TODO: move this in Gemma CLI
        Path p = Paths.get( System.getProperty( "user.home" ), USER_CONFIGURATION );
        FileSystemResource r = new FileSystemResource( p.toFile() );
        if ( !userConfigLoaded && r.exists() ) {
            log.debug( "Loading user configuration from " + p.toAbsolutePath() + "." );
            warnIfReadableByGroupOrOthers( p );
            result.addLast( new ResourcePropertySource( r.getDescription() + " (from $HOME)", r ) );
            userConfigLoaded = true;
        }

        // at least one user configuration should be loaded
        if ( !userConfigLoaded ) {
            throw new RuntimeException( USER_CONFIGURATION + " could not be loaded and no other user configuration were supplied." );
        }

        log.debug( "Loading default configuration files from classpath." );
        for ( String loc : DEFAULT_CONFIGURATIONS ) {
            result.addLast( new ResourcePropertySource( new ClassPathResource( loc ) ) );
        }

        ClassPathResource versionResource = new ClassPathResource( "ubic/gemma/version.properties" );
        if ( versionResource.exists() ) {
            result.addLast( new ResourcePropertySource( versionResource ) );
        } else {
            log.warn( "The ubic/gemma/version.properties resource was not found; run `mvn generate-resources -pl gemma-core` to generate it." );
        }

        cachedSettingsPropertySources = result;

        return result;
    }

    /**
     * Filter system properties that are declared in the default locations.
     */
    private static Properties filteredSystemProperties() throws IOException {
        Properties props = new Properties();
        for ( String loc : DEFAULT_CONFIGURATIONS ) {
            try ( InputStream is = new ClassPathResource( loc ).getInputStream() ) {
                Properties defaultProperties = new Properties();
                defaultProperties.load( is );
                for ( String key : defaultProperties.stringPropertyNames() ) {
                    String val;
                    if ( key.startsWith( SYSTEM_PROPERTY_PREFIX ) ) {
                        val = System.getProperty( key );
                    } else {
                        val = System.getProperty( key );
                        if ( val != null ) {
                            // allow unprefixed keys for backward-compatibility
                            log.warn( String.format( "System property %s should be prefixed with '%s'.", key, SYSTEM_PROPERTY_PREFIX ) );
                        } else {
                            val = System.getProperty( SYSTEM_PROPERTY_PREFIX + key );
                        }
                    }
                    if ( val != null ) {
                        props.setProperty( key, val );
                    }
                }
            }
        }
        return props;
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
