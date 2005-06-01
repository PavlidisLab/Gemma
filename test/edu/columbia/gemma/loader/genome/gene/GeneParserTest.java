package edu.columbia.gemma.loader.genome.gene;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.loader.loaderutils.Loader;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneParserTest extends BaseServiceTestCase {

    protected static final Log log = LogFactory.getLog( GeneParserTest.class );

    private GeneLoaderImpl geneLoader = null;
    private GeneParserImpl geneParser = null;
    private Map map = null;

    public void testParseValidFile() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/gene/geneinfo" );
        Method m = geneParser.findParseLineMethod( "geneinfo" );
        geneParser.parse( is, m );

        InputStream is2 = this.getClass().getResourceAsStream( "/data/loader/genome/gene/gene2accession" );
        Method m2 = geneParser.findParseLineMethod( "gene2accession" );
        map = geneParser.parse( is2, m2 );

        // add a new stream for each file.

        geneLoader.create( map.values() );

        assertEquals( null, null );

    }

    protected void setUp() throws Exception {
        super.setUp();

        BeanFactory ctx = SpringContextUtil.getApplicationContext();
        geneParser = new GeneParserImpl();
        geneLoader = new GeneLoaderImpl();
        GeneMappings geneMappings = new GeneMappings( ( TaxonDao ) ctx.getBean( "taxonDao" ) );
        geneParser.setGeneMappings( geneMappings );
        geneLoader.setGeneDao( ( GeneDao ) ctx.getBean( "geneDao" ) );
        map = new HashMap();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // TODO can you pass arguments to JUnit tests so I can select this option at runtime?
        geneLoader.removeAll( map.values() );
        geneParser = null;
        geneLoader = null;
        map = null;
    }

    // public void testParseFileInvalidFile() throws Exception {
    //
    // geneParser.parseFile( "badfilename" );
    //
    // assertEquals( null, null );
    // }
}
