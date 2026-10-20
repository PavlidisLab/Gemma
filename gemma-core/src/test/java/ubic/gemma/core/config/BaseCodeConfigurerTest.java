package ubic.gemma.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
// Configuration is ubic.gemma.core.config.Configuration (same package), no explicit import needed.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static ubic.gemma.core.util.test.Maps.map;

public class BaseCodeConfigurerTest {

    @AfterEach
    public void resetBaseCodeConfiguration() {
        Configuration.reset();
    }

    @Test
    public void test() {
        BaseCodeConfigurer bcc = new BaseCodeConfigurer();
        MutablePropertySources ps = new MutablePropertySources();
        ps.addLast( new MapPropertySource( "test", map( "basecode.a", "b" ) ) );
        bcc.setPropertySources( ps );
        bcc.postProcessBeanFactory( mock() );
        assertEquals( "b", Configuration.getString( "a" ) );
    }

    @Test
    public void testBackwardCompatibleProps() {
        // The subject here is that an UNPREFIXED property is ignored -- as of 1.32 the `basecode.`
        // prefix is required -- so the shipped default survives the attempted override.
        //
        // It is NOT that CHEBI defaults to any particular file. This assertion used to hardcode
        // chebi_lite.owl and went red when basecode.properties moved to the full chebi.owl, which
        // is the only published variant carrying synonyms. That is a config decision this test has
        // no stake in, and pinning it here made an unrelated fix look like a regression. Compare
        // against whatever the file ships instead.
        String shipped = Configuration.getString( "url.chebiOntology" );
        assertNotNull( shipped, "basecode.properties should ship a CHEBI URL" );

        BaseCodeConfigurer bcc = new BaseCodeConfigurer();
        MutablePropertySources ps = new MutablePropertySources();
        ps.addLast( new MapPropertySource( "test", map( "url.chebiOntology", "foo" ) ) );
        bcc.setPropertySources( ps );
        bcc.postProcessBeanFactory( mock() );

        assertEquals( shipped, Configuration.getString( "url.chebiOntology" ),
                "an unprefixed property must not override the shipped default" );
    }
}