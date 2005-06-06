package edu.columbia.gemma.loader.association;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * 
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 */
public class GeneOntologyParserTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( GeneOntologyParserTest.class );

    GoMappings goMappings=null;
    GeneOntologyParserImpl goParser=null;
    Method m = null;
    
    /**
     * 
     * @throws NoSuchMethodException
     */
    public void testFindParseLineMethod() throws NoSuchMethodException {
        log.info( "Testing class: GeneOntologyParser method: public Method findParseLineMethod( String species ) throws NoSuchMethodException" );
        m = goParser.findParseLineMethod( "human" );
    }
    

    public void testParse() throws IOException {
        log.info( "Testing class: GeneOntologyParser method: public Map parse( InputStream is, Method m ) throws IOException" );
        InputStream is = goParser.retrieveByHTTP("human");
        goParser.parse(is,m);
    }
    
    /**
     * 
     */
    protected void setUp() throws Exception {
        super.setUp();

        BeanFactory ctx = SpringContextUtil.getApplicationContext();
        goParser = new GeneOntologyParserImpl();
        // goLoader = new GeneLoaderImpl();
        // GoMappings goMappings = new GoMappings( ( TaxonDao ) ctx.getBean( "taxonDao" ) );
        goParser.setGoMappings( new GoMappings() );
        // geneLoader.setGeneDao( ( GeneDao ) ctx.getBean( "geneDao" ) );
        // map = new HashMap();

    }
    
    /**
     * 
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        goParser = null;
        goMappings = null;
    }

}
