package ubic.gemma.persistence.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

@ActiveProfiles(EnvironmentProfiles.TEST)
@ContextConfiguration
public class SettingsConfigTest extends AbstractJUnit4SpringContextTests {

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
            // FIXME: local local.properties from ${gemma.appdata.home}
            result.addLast( new ResourcePropertySource( new ClassPathResource( "ubic/gemma/version.properties" ) ) );
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
    }
}
