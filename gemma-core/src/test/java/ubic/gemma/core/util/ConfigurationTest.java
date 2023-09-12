package ubic.gemma.core.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.persistence.util.SpringProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration("classpath:ubic/gemma/applicationContext-serviceBeans.xml")
public class ConfigurationTest extends AbstractJUnit4SpringContextTests {

    @Value("${cors.allowedOrigins}")
    private String allowedOrigins;

    @Value("${gemma.version}")
    private String version;

    @Value("${gemma.externalDatabases.featured}")
    private String[] featuredExternalDatabases;

    @Test
    public void test() {
        assertNotNull( version );
        assertNotNull( allowedOrigins );
        assertThat( featuredExternalDatabases )
                .isNotNull()
                .contains( "hg38", "mm10" );
    }
}
