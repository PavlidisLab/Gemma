package edu.columbia.gemma.loader.genome;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.easymock.MockControl;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class TaxonLoaderServiceTest extends BaseServiceTestCase {
    private MockControl control;
    BufferedReader br;
    Configuration conf;
    File file;
    TaxonDao taxonDaoMock;
    TaxonLoaderService tls;
    InputStream stream;
    InputStream testStream;
    InputStream anotherTestStream;

    /**
     * @see TestCase#setUp()
     * @throws Exception
     */
    protected void setUp() throws Exception {
        super.setUp();
        conf = new PropertiesConfiguration( "testtaxa.properties" );
        //file = new File( conf.getString( "testtaxa.filename" ) );
        tls = new TaxonLoaderService();
        control = MockControl.createControl( TaxonDao.class );
        taxonDaoMock = ( TaxonDao ) control.getMock();
        tls.setTaxonDao( taxonDaoMock );
    }

    /**
     * @see TestCase#tearDown()
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        tls = null;
    }

    /**
     * Tests bulkCreate(InputStream, boolean) behaviour = findByScientificName(String)
     * 
     * @throws IOException
     */
    public void testBulkCreateInputStreamboolean() throws IOException {
        Collection col = new HashSet();
        stream = TaxonLoaderServiceTest.class.getResourceAsStream( conf.getString( "testtaxa.data.filename" ) );
        testStream = TaxonLoaderServiceTest.class.getResourceAsStream( conf.getString( "testtaxa.data.filename" ) );
        for ( int i = 0; i < 4; i++ ) {
            Taxon tt = Taxon.Factory.newInstance();
            col.add( tt );
        }
        br = new BufferedReader( new InputStreamReader( stream ) );
        String line;
        br.readLine();
        while ( ( line = br.readLine() ) != null ) {
            String sArray[] = line.split( "\t" );
            taxonDaoMock.findByScientificName( sArray[2] ); //recording method calls
            control.setReturnValue( col );
        }
        control.replay();//playback mode (mock object behaves like a mock object here)
        tls.bulkCreate( testStream, true );
        control.verify();
    }

    /**
     * Tests bulkCreate(String filename, boolean) behaviour = findByScientificName(String) behaviour = create(Taxon)
     * 
     * @throws IOException TODO this test works ... sometimes.
     */
    //    public void testBulkCreatefilenamebooleanCreate() throws IOException {
    //        Collection col = new HashSet();
    //        stream = TaxonLoaderServiceTest.class.getResourceAsStream( conf.getString( "testtaxa.data.filename" ) ) ;
    //        testStream = TaxonLoaderServiceTest.class.getResourceAsStream( conf.getString( "testtaxa.data.filename" ) ) ;
    //        br = new BufferedReader( new InputStreamReader( stream) );
    //        String line;
    //        br.readLine();
    //        while ( ( line = br.readLine()) != null ) {
    //            String sArray[] = line.split( "\t" );
    //            taxonDaoMock.findByScientificName( sArray[2] );
    //            control.setReturnValue( col );
    //            taxonDaoMock.create( Taxon.Factory.newInstance() );//recording method calls
    //            control.setReturnValue( null );
    //        }
    //        control.replay();//playback mode (mock object behaves like a mock object here)
    //        tls.bulkCreate( testStream, true );
    //        control.verify();
    //    }
}