package ubic.gemma.persistence.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration
public class SettingsConfigTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class ConfigurationTestContextConfiguration {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            MutablePropertySources mps = new MutablePropertySources();
            for ( PropertySource<?> ps : propertySources() ) {
                mps.addLast( ps );
            }
            configurer.setPropertySources( mps );
            return configurer;
        }

        private static List<PropertySource<?>> propertySources() throws IOException {
            return Arrays.asList( new ResourcePropertySource( "classpath:default.properties" ),
                    new ResourcePropertySource( "classpath:project.properties" ),
                    new ResourcePropertySource( "classpath:ubic/gemma/version.properties" ) );
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
                .contains( "hg38", "mm10" );
        assertEquals( "/var/tmp/gemmaData", appDataHome );
        assertEquals( appDataHome + "/download", downloadPath );
        assertEquals( appDataHome + "/searchIndices", searchDir );
        assertEquals( searchDir, compassDir );
    }
}
