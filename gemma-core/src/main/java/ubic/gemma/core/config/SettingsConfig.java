package ubic.gemma.core.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import ubic.gemma.core.util.ManifestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Beans declaration for making the settings available via the Spring Environment and placeholder substitution.
 * @author poirigui
 */
@Slf4j
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

        // 12-factor: environment-variable overrides (GEMMA_FOO_BAR -> gemma.foo.bar). Listed FIRST so that container
        // env vars take precedence over a Gemma.properties file on disk. Only keys declared in default.properties /
        // project.properties are passed through, matching the filtering already done for -D system properties.
        result.addLast( new PropertiesPropertySource( "environment", filterEnvironmentVariables( System.getenv() ) ) );

        result.addLast( new PropertiesPropertySource( "system", filterSystemProperties( System.getProperties() ) ) );

        // expose a small set of JVM-standard system properties (java.io.tmpdir, user.home, user.dir, user.name,
        // os.name, file.separator, line.separator, path.separator) so that default.properties can reference them
        // via ${...} placeholders -- needed because PropertySourcesPlaceholderConfigurer with an explicit
        // PropertySources list does NOT fall back to the Environment for unresolved placeholders.
        result.addLast( new PropertiesPropertySource( "jvm-standard", jvmStandardProperties() ) );

        boolean userConfigLoaded = false;

        String gemmaConfig = System.getProperty( GEMMA_CONFIG_SYSTEM_PROPERTY );
        if ( gemmaConfig != null ) {
            Path p = Paths.get( gemmaConfig );
            log.debug( "Loading user configuration from " + p.toAbsolutePath() + " since -Dgemma.config is defined." );
            FileSystemResource r = new FileSystemResource( p.toFile() );
            if ( !r.exists() ) {
                throw new RuntimeException( p + " could not be loaded." );
            }
            warnIfReadableByOthers( p );
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
                warnIfReadableByOthers( p );
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
            warnIfReadableByOthers( p );
            result.addLast( new ResourcePropertySource( r.getDescription() + " (from $HOME)", r ) );
            userConfigLoaded = true;
        }

        // at least one user configuration should normally be loaded; if no on-disk file resolves we now WARN and
        // continue rather than throwing, so a container can supply every required property via environment variables
        // / JVM system properties (which are already a higher-priority source). Required properties without defaults
        // (e.g. gemma.db.url, gemma.db.user, gemma.db.password) will still fail loudly at their @Value injection
        // site if they remain unresolved — see CONFIG_AUDIT.md HIGH issue #1.
        if ( !userConfigLoaded ) {
            log.warn( USER_CONFIGURATION + " was not found via -Dgemma.config, $CATALINA_BASE, or $HOME. "
                    + "Continuing without an on-disk user configuration; required properties must be supplied "
                    + "via environment variables or JVM -D system properties (see CONTAINER_CONFIG.md)." );
        }

        log.debug( "Loading default configuration files from classpath." );
        for ( String loc : DEFAULT_CONFIGURATIONS ) {
            result.addLast( new ResourcePropertySource( new ClassPathResource( loc ) ) );
        }

        // include build information from the manifest
        result.addLast( new PropertiesPropertySource( "manifest", ManifestUtils.readGemmaPropertiesFromManifest() ) );

        cachedSettingsPropertySources = result;

        return result;
    }

    private static Map<String, String> cachedSettingsDescriptions = null;

    @Bean
    public static synchronized Map<String, String> settingsDescriptions() throws IOException {
        if ( cachedSettingsDescriptions != null ) {
            return cachedSettingsDescriptions;
        }

        Map<String, String> result = new HashMap<>();
        for ( String configFile : DEFAULT_CONFIGURATIONS ) {
            try ( BufferedReader br = new BufferedReader( new InputStreamReader( new ClassPathResource( configFile ).getInputStream(), StandardCharsets.UTF_8 ) ) ) {
                String description = null;
                String line;
                while ( ( line = br.readLine() ) != null ) {
                    if ( line.startsWith( "#" ) ) {
                        if ( line.contains( "suppress inspection" ) ) {
                            continue;
                        }
                        description = StringUtils.stripStart( line, "#" );
                    } else if ( line.contains( "=" ) ) {
                        String[] pieces = line.split( "=", 2 );
                        String prop = StringUtils.strip( pieces[0] );
                        String defaultValue = pieces[1];
                        String desc = "";
                        if ( description != null ) {
                            desc = StringUtils.capitalize( StringUtils.strip( description ) );
                        }
                        if ( StringUtils.isNotBlank( defaultValue ) ) {
                            if ( !desc.isEmpty() ) {
                                desc = StringUtils.appendIfMissing( desc, "." ) + " ";
                            }
                            // TODO: use the placeholder resolver
                            desc += "Default value is '" + defaultValue + "'.";
                        }
                        result.put( prop, desc );
                        description = null;
                    }
                }
            }
        }

        cachedSettingsDescriptions = result;

        return result;
    }

    /**
     * Standard JVM properties that {@code default.properties} / {@code project.properties} are allowed to
     * reference via {@code ${...}} placeholders. Keeping this list small + explicit so we don't accidentally
     * leak unrelated system properties into the placeholder resolver.
     */
    private static final String[] JVM_STANDARD_PROPERTY_KEYS = {
            "java.io.tmpdir", "user.home", "user.dir", "user.name",
            "os.name", "file.separator", "line.separator", "path.separator"
    };

    static Properties jvmStandardProperties() {
        Properties props = new Properties();
        for ( String key : JVM_STANDARD_PROPERTY_KEYS ) {
            String value = System.getProperty( key );
            if ( value != null ) {
                props.setProperty( key, value );
            }
        }
        return props;
    }

    /**
     * Translate POSIX-style environment variables ({@code GEMMA_DB_URL}, {@code GEMMA_APPDATA_HOME}) to
     * dot-separated Gemma keys ({@code gemma.db.url}, {@code gemma.appdata.home}) and keep only those that
     * correspond to a key declared in {@link #DEFAULT_CONFIGURATIONS}. This lets a container supply any
     * Gemma property via {@code -e GEMMA_FOO_BAR=...} without needing a {@code Gemma.properties} file on disk.
     */
    static Properties filterEnvironmentVariables( Map<String, String> env ) throws IOException {
        Properties props = new Properties();
        for ( String loc : DEFAULT_CONFIGURATIONS ) {
            try ( InputStream is = new ClassPathResource( loc ).getInputStream() ) {
                Properties defaultProperties = new Properties();
                defaultProperties.load( is );
                for ( String key : defaultProperties.stringPropertyNames() ) {
                    if ( props.containsKey( key ) ) {
                        continue;
                    }
                    String envKey = key.toUpperCase().replace( '.', '_' );
                    if ( env.containsKey( envKey ) ) {
                        props.setProperty( key, env.get( envKey ) );
                    }
                }
            }
        }
        return props;
    }

    /**
     * Filter system properties that are declared in the default locations.
     */
    static Properties filterSystemProperties( Properties allProperties ) throws IOException {
        Properties props = new Properties();
        for ( String loc : DEFAULT_CONFIGURATIONS ) {
            try ( InputStream is = new ClassPathResource( loc ).getInputStream() ) {
                Properties defaultProperties = new Properties();
                defaultProperties.load( is );
                for ( String key : defaultProperties.stringPropertyNames() ) {
                    if ( props.containsKey( key ) ) {
                        continue;
                    }
                    if ( allProperties.containsKey( SYSTEM_PROPERTY_PREFIX + key ) ) {
                        props.setProperty( key, allProperties.getProperty( SYSTEM_PROPERTY_PREFIX + key ) );
                    } else if ( allProperties.containsKey( key ) ) {
                        if ( key.startsWith( SYSTEM_PROPERTY_PREFIX ) ) {
                            props.setProperty( key, allProperties.getProperty( key ) );
                        } else {
                            log.warn( String.format( "System property %s matches a Gemma property, but it is not prefixed with with '%s'. It will be ignored.", key, SYSTEM_PROPERTY_PREFIX ) );
                        }
                    }
                }
            }
        }
        return props;
    }

    private static void warnIfReadableByOthers( Path path ) throws IOException {
        Set<PosixFilePermission> permissions;
        try {
            permissions = Files.getPosixFilePermissions( path );
        } catch ( UnsupportedOperationException e ) {
            return;
        }
        if ( permissions.contains( PosixFilePermission.OTHERS_READ ) ) {
            log.warn( String.format( "%s may contain credentials and is readable by others. Adjust the permissions by running 'chmod o-r %s' to remove this warning.", path.getFileName(), path.toAbsolutePath() ) );
        }
    }
}
