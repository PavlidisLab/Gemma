package edu.columbia.gemma.loader.sequence.gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.loader.loaderutils.BulkCreator;
import edu.columbia.gemma.sequence.gene.Gene;
import edu.columbia.gemma.sequence.gene.GeneDao;
import edu.columbia.gemma.sequence.gene.Taxon;
import edu.columbia.gemma.sequence.gene.TaxonDao;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneLoaderService implements BulkCreator {
    protected static final Log log = LogFactory.getLog( GeneLoaderService.class );
    private Gene gene;
    private GeneDao geneDao;
    private int ncbiIdCol;
    private int officialNameCol;
    private int refIdCol;
    private int symbolCol;
    private int taxonCol;
    private TaxonDao taxonDao;
    private String view;
    Collection col;
    Configuration conf;
    Object first = new Object();
    Iterator i;

    /**
     * @throws ConfigurationException
     */
    public GeneLoaderService() throws ConfigurationException {
        conf = new PropertiesConfiguration( "gene.properties" );
        symbolCol = conf.getInt( "gene.symbolCol" );
        officialNameCol = conf.getInt( "gene.officialNameCol" );
        refIdCol = conf.getInt( "gene.refIdCol" );
        ncbiIdCol = conf.getInt( "gene.ncbiIdCol" );
        taxonCol = conf.getInt( "gene.taxonCol" );
        view = "gene";
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
     */
    public String bulkCreate( String filename, boolean hasHeader ) {
        log.info( "Reading from " + filename );
        try {
            view = "gene";
            int count = bulkCreate( openFileAsStream( filename ), hasHeader );
            log.info( "Created " + count + " new taxa from " + filename );
        } catch ( IOException e ) {
            view = "error";
        }
        return view;
    }

    /**
     * @return Returns the gene.
     */
    public Gene getGene() {
        return gene;
    }

    /**
     * @return Returns the geneDao.
     */
    public GeneDao getGeneDao() {
        return geneDao;
    }

    /**
     * @return Returns the taxonDao.
     */
    public TaxonDao getTaxonDao() {
        return taxonDao;
    }

    /**
     * @param gene The gene to set.
     */
    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    /**
     * @param geneDao
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

    /**
     * @param taxonDao The taxonDao to set.
     */
    public void setTaxonDao( TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }

    /**
     * @param line
     * @throws NumberFormatException
     */
    private boolean createFromRow( String line ) throws NumberFormatException {
        String[] sArray = line.split( "\t" );
        Gene g = Gene.Factory.newInstance();
        g.setSymbol( sArray[symbolCol] );
        g.setOfficialName( sArray[officialNameCol] );
        if ( sArray[refIdCol].equals( "LocusLink" ) ) g.setNcbiId( Integer.parseInt( sArray[ncbiIdCol] ) );
        if ( Long.parseLong( sArray[taxonCol] ) == 0 || Long.parseLong( sArray[taxonCol] ) > 4 )
                sArray[taxonCol] = "1";
        g.setTaxon( mapTaxon( Long.parseLong( sArray[taxonCol] ) ) );
        Collection geneCol = this.geneDao.findByOfficalName( g.getOfficialName() );
        if ( geneCol.size() > 0 ) {
            log.info( "Gene with name: " + g.getOfficialName() + " already exists, skipping." );
            return false;
        }
        this.geneDao.create( g );
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
     * @return
     */
    private Taxon mapTaxon( long taxonValue ) {
        Taxon t = Taxon.Factory.newInstance();
        Long taxonValueL = new Long( taxonValue );
        t = ( Taxon ) getTaxonDao().load( taxonValueL );
        return t;
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

