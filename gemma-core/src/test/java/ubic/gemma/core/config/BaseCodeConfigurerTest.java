package ubic.gemma.core.config;

import org.junit.After;
import org.junit.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import ubic.basecode.util.Configuration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static ubic.gemma.core.util.test.Maps.map;

public class BaseCodeConfigurerTest {

    @After
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
        BaseCodeConfigurer bcc = new BaseCodeConfigurer();
        MutablePropertySources ps = new MutablePropertySources();
        ps.addLast( new MapPropertySource( "test", map( "url.chebiOntology", "foo" ) ) );
        bcc.setPropertySources( ps );
        bcc.postProcessBeanFactory( mock() );
        // as of 1.32, this no-longer works
        assertEquals( "http://purl.obolibrary.org/obo/chebi.owl", Configuration.getString( "url.chebiOntology" ) );
    }
}