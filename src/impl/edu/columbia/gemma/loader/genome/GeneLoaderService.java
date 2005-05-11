package edu.columbia.gemma.loader.genome;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.genome.TaxonImpl;
import edu.columbia.gemma.loader.loaderutils.BulkCreator;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="geneLoaderService"
 * @spring.property name="geneDao" ref="geneDao"
 * @spring.property name="taxonDao" ref="taxonDao"
 */
public class GeneLoaderService implements BulkCreator {
    protected static final Log log = LogFactory.getLog( GeneLoaderService.class );
    
    private static boolean alreadyRetreivedTaxa;
    private edu.columbia.gemma.genome.Gene gene;
    private edu.columbia.gemma.genome.GeneDao geneDao;
    private int ncbiIdCol;
    private int officialNameCol;
    private int refIdCol;
    private int symbolCol;
    private int taxonCol;
    private TaxonDao taxonDao;
    private String view;
    Configuration conf;
    Map taxaMap;

    /**
     * @throws ConfigurationException
     */
    public GeneLoaderService() throws ConfigurationException {
        conf = new PropertiesConfiguration( "Gemma.properties" );
        symbolCol = conf.getInt( "gene.symbolCol" );
        officialNameCol = conf.getInt( "gene.officialNameCol" );
        refIdCol = conf.getInt( "gene.refIdCol" );
        ncbiIdCol = conf.getInt( "gene.ncbiIdCol" );
        taxonCol = conf.getInt( "gene.taxonCol" );
        alreadyRetreivedTaxa = false;
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
     * @throws IOException
     */
    public String bulkCreate( String filename, boolean hasHeader ) throws IOException {
        log.info( "Reading from " + filename );
        bulkCreate( openFileAsStream( filename ), hasHeader );

        return view;
    }

    /**
     * @return Map TODO put in taxonutils after making taxaMap in createFromRow static
     */
    private Map findAllTaxa() {
        Collection taxa = this.taxonDao.findAllTaxa();
        Map taxaMap = new HashMap();
        Iterator iter = taxa.iterator();
        int id = 1;
        while ( iter.hasNext() ) {
            Integer Id = new Integer( id );
            taxaMap.put( Id, iter.next() );
            id++;
        }
        alreadyRetreivedTaxa = true;
        return taxaMap;
    }

    /**
     * @return Returns the gene.
     */
    public edu.columbia.gemma.genome.Gene getGene() {
        return gene;
    }

    /**
     * @return Returns the geneDao.
     */
    public edu.columbia.gemma.genome.GeneDao getGeneDao() {
        return geneDao;
    }

    /**
     * @return Returns the taxonDao.
     */
    public edu.columbia.gemma.genome.TaxonDao getTaxonDao() {
        return taxonDao;
    }

    /**
     * @param br
     * @throws IOException TODO put in loaderutils
     */
    public void handleHeader( BufferedReader br ) throws IOException {
        br.readLine();
    }

    /**
     * @param taxaMap
     * @param id
     * @param s
     * @return Taxon TODO put in taxonutils after making taxaMap in createFromRow static
     */
    private Taxon loadOrCreateTaxon( Map taxaMap, int id, Taxon taxon ) {
        Taxon t;
        Integer Id = new Integer( id );
        if ( taxaMap.containsKey( Id ) )
            t = ( TaxonImpl ) taxaMap.get( Id );
        else {
            t = taxon;
        }
        return t;
    }

    /**
     * @param gene The gene to set.
     */
    public void setGene( edu.columbia.gemma.genome.Gene gene ) {
        this.gene = gene;
    }

    /**
     * @param geneDao
     */
    public void setGeneDao( edu.columbia.gemma.genome.GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

    /**
     * @param taxonDao The taxonDao to set.
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
        Gene g = Gene.Factory.newInstance();
        g.setOfficialSymbol( sArray[symbolCol] );
        g.setOfficialName( sArray[officialNameCol] );
        if ( sArray[refIdCol].equals( "LocusLink" ) ) g.setNcbiId( sArray[ncbiIdCol] );

        if ( !alreadyRetreivedTaxa ) taxaMap = findAllTaxa();

        g.setTaxon( loadOrCreateTaxon( taxaMap, Integer.parseInt( sArray[taxonCol] ), null ) );
        Collection geneCol = this.geneDao.findByOfficalSymbol( g.getOfficialName() );
        if ( geneCol.size() > 0 ) {
            log.info( "Gene with name: " + g.getOfficialName() + " already exists, skipping." );
            return false;
        }
        this.geneDao.create( g );
        return true;

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

