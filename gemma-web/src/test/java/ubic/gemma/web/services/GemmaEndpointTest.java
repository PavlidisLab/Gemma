package ubic.gemma.web.services;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GemmaEndpointTest extends TestCase {

    private static Log log = LogFactory.getLog( GemmaEndpointTest.class.getName() );

    /*
     * Test method for 'ubic.gemma.web.services.GemmaEndpointTest.readReport'
     */
    public void testReadReport() throws Exception {

        class TestEndpoint extends AbstractGemmaEndpoint {
            public TestEndpoint() {

            }

            public Document readTest() throws IOException {
                InputStream stream = GemmaEndpointTest.class.getResourceAsStream( "/data/DEDVforEE-159-test.xml" );
                assert stream != null;
                return this.readReport( stream );
            }

            @Override
            @SuppressWarnings("unchecked")
            protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
                return null;
            }
        }

        try {
            TestEndpoint testEp = new TestEndpoint();
            Document doc = testEp.readTest();
            if (doc == null)
                assertTrue( false );
            
            NodeList nl = doc.getElementsByTagName( "dedv" );
            int numActual = nl.getLength();
            assertEquals( 12625, numActual ); // used grep -o "<dedv>" DEDVforEE-159-test.xml | wc -l to verify
        } catch ( Exception e ) {            
            log.info( "Test Fail error is: " + e );
            throw new RuntimeException(e);
        }
    }
}
