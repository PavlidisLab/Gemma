package ubic.gemma.core.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static ubic.gemma.core.config.SettingsConfig.filterSystemProperties;

@ActiveProfiles(EnvironmentProfiles.TEST)
@ContextConfiguration
public class SettingsConfigTest extends BaseTest {

    @Configuration
    @TestComponent
    static class ConfigurationTestContextConfiguration {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setPropertySources( propertySources() );
            return configurer;
        }

        private static PropertySources propertySources() throws IOException {
            MutablePropertySources result = new MutablePropertySources();
            result.addLast( new ResourcePropertySource( new ClassPathResource( "default.properties" ) ) );
            result.addLast( new ResourcePropertySource( new ClassPathResource( "project.properties" ) ) );
            Properties buildProps = new Properties();
            buildProps.setProperty( "gemma.version", "1.32.0-SNAPSHOT" );
            buildProps.setProperty( "gemma.build.timestamp", "2026-01-21T20:47:30Z" );
            buildProps.setProperty( "gemma.build.gitHash", "07f91f2083d625f05d367a7a8e6100bfbf83fea8" );
            result.addLast( new PropertiesPropertySource( "manifest", buildProps ) );
            return result;
        }
    }

    @Value("${gemma.appdata.home}")
    public String appDataHome;

    @Value("${gemma.download.path}")
    public String downloadPath;

    @Value("${gemma.search.dir}")
    public String searchDir;

    @Value("${gemma.compass.dir}")
    public String compassDir;

    @Value("${cors.allowedOrigins}")
    private String allowedOrigins;

    @Value("${gemma.version}")
    private String version;

    @Value("${gemma.externalDatabases.featured}")
    private String[] featuredExternalDatabases;

    @Value("${gemma.hosturl}")
    private String hostUrl;

    @Value("${gemma.project.dir}")
    private String projectDir;

    @Value("${gemma.log.dir}")
    private String logDir;

    @Test
    public void test() {
        assertNotNull( version );
        assertTrue( allowedOrigins.contains( hostUrl ) );
        assertThat( featuredExternalDatabases )
                .isNotNull()
                .contains( "hg38", "mm39" );
        assertEquals( "/var/tmp/gemmaData", appDataHome );
        assertEquals( appDataHome + "/download", downloadPath );
        assertEquals( appDataHome + "/searchIndices", searchDir );
        assertEquals( searchDir, compassDir );
        assertEquals( ".", projectDir );
        assertEquals( ".", logDir );
    }

    @Test
    public void testFilteredProperties() throws IOException {
        Properties props;

        props = new Properties();
        props.setProperty( "gemma.fastaCmd.exe", "foo" );
        assertThat( filterSystemProperties( props ) )
                .containsEntry( "fastaCmd.exe", "foo" );

        // support for these has been dropped in 1.32, a warning is still emitted.
        props = new Properties();
        props.setProperty( "fastaCmd.exe", "foo" );
        assertThat( filterSystemProperties( props ) )
                .doesNotContainKey( "fastaCmd.exe" );

        props = new Properties();
        props.setProperty( "gemma.fastaCmd.exe", "foo" );
        props.setProperty( "fastaCmd.exe", "bar" );
        assertThat( filterSystemProperties( props ) )
                .containsEntry( "fastaCmd.exe", "foo" );
    }

    @Test
    public void testSettingsDescriptions() throws IOException {
        assertThat( SettingsConfig.settingsDescriptions() )
                .containsEntry( "ga.tracker", "Google Analytics 4" );
    }
}
