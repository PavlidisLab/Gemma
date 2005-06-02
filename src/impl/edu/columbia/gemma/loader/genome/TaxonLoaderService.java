package edu.columbia.gemma.loader.genome;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.loader.loaderutils.BulkCreator;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="taxonLoaderService"
 * @spring.property name="taxonDao" ref="taxonDao"
 */
public class TaxonLoaderService implements BulkCreator {
    protected static final Log log = LogFactory.getLog( TaxonLoaderService.class );
    private int abbreviationCol;
    private int commonNameCol;
    private int ncbiIdCol;
    private int scientificNameCol;
    private Taxon taxon;
    private TaxonDao taxonDao;
    private String view;

    /**
     * @throws ConfigurationException
     */
    public TaxonLoaderService() throws ConfigurationException {
        Configuration conf = new PropertiesConfiguration( "Gemma.properties" );
        scientificNameCol = conf.getInt( "taxon.scientificNameCol" );
        commonNameCol = conf.getInt( "taxon.commonNameCol" );
        abbreviationCol = conf.getInt( "taxon.abbreviationCol" );
        ncbiIdCol = conf.getInt( "taxon.ncbiIdCol" );
        view = "taxon";
    }

    /**
     * @param is
     * @param hasHeader Indicate if the stream is from a file that has a one-line header
     * @throws IOException
     */
    public int bulkCreate( InputStream is, boolean hasHeader ) throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        if ( hasHeader ) handleHeader( br );

        int count = 0;
        String line = null;
        while ( ( line = br.readLine() ) != null ) {
            if ( createFromRow( line ) ) count++;
        }
        br.close();
        return count;
    }

    /**
     * @param filename
     * @param hasHeader Indicate if the stream is from a file that has a one-line header
     * @return String
     * @throws IOException
     */
    public String bulkCreate( String filename, boolean hasHeader ) throws IOException {
        log.info( "Reading from " + filename );
        bulkCreate( openFileAsStream( filename ), hasHeader );

        return view;
    }

    /**
     * @return Returns the taxon.
     */
    public edu.columbia.gemma.genome.Taxon getTaxon() {
        return taxon;
    }

    /**
     * @param taxon The taxon to set.
     */
    public void setTaxon( edu.columbia.gemma.genome.Taxon taxon ) {
        this.taxon = taxon;
    }

    /**
     * @param geneDao
     */
    public void setTaxonDao( edu.columbia.gemma.genome.TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }

    /**
     * @param line
     * @throws NumberFormatException
     */
    private boolean createFromRow( String line ) throws NumberFormatException {
        String[] sArray = line.split( "\t" );
        edu.columbia.gemma.genome.Taxon t = edu.columbia.gemma.genome.Taxon.Factory.newInstance();
        // t.setIdentifier( "taxon::" + sArray[scientificNameCol] );
        // t.setName( sArray[commonNameCol] );
        t.setScientificName( sArray[scientificNameCol] );
        t.setCommonName( sArray[commonNameCol] );
        t.setAbbreviation( sArray[abbreviationCol] );
        t.setNcbiId( Integer.parseInt( sArray[ncbiIdCol] ) );
        Collection col = this.taxonDao.findByScientificName( t.getScientificName() );
        if ( col.size() > 0 ) {
            log.info( "Taxon with scientificName: " + t.getScientificName() + " already exists, skipping." );
            return false;
        }
        this.taxonDao.create( t );
        return true;

    }

    /**
     * @param br
     * @throws IOException
     */
    private void handleHeader( BufferedReader br ) throws IOException {
        br.readLine();
    }

    /**
     * @return InputStream
     * @throws IOException
     */
    private InputStream openFileAsStream( String filename ) throws IOException {
        File file = new File( filename );
        if ( !file.canRead() ) throw new IOException( "Can't read from file " + file );

        return new FileInputStream( file );

    }

}
