package edu.columbia.gemma.loader.genome.gene;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseServiceTestCase;
/**
 * 
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 */
public class GeneParserTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( GeneParserTest.class );

    private static final String GENE_INFO = "C:\\Documents and Settings\\keshav\\My Documents\\Gemma\\gene_info";
    private static final String GENE2ACCESSION = "C:\\Documents and Settings\\keshav\\My Documents\\Gemma\\gene2accession";
    private GeneParser geneParser = null;
    private GeneLoader geneLoader = null;
    private Map map = null;

    private String filename = null;
    private String filename2 = null;
    private File file = null;

    protected void setUp() throws Exception {
        super.setUp();
        geneParser = new GeneParserImpl();
        geneLoader = new GeneLoaderImpl();
        map = new HashMap();
        // TODO don't hardcode
        filename = GENE_INFO;
        filename2 = GENE2ACCESSION;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        geneParser = null;
        geneLoader = null;
        map = null;
        filename = null;
    }

    public void testParseFileValidFile() throws Exception {
        geneParser.parseFile( filename );
        map = geneParser.parseFile( filename2 );

        geneLoader.create( map.values() );

        assertEquals( null, null );

    }

    // public void testParseFileInvalidFile() throws Exception {
    // try {
    // geneParser.parseFile( "badfilename" );
    // } catch ( IOException e ) {
    // e.printStackTrace();
    // assertEquals( null, null );
    // }
    // }
}
