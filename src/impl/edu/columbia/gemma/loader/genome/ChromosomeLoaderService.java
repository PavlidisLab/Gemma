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

import edu.columbia.gemma.genome.Chromosome;
import edu.columbia.gemma.genome.ChromosomeDao;
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
 * @spring.bean id="chromosomeLoaderService"
 * @spring.property name="chromosomeDao" ref="chromosomeDao"
 * @spring.property name="taxonDao" ref="taxonDao"
 */
public class ChromosomeLoaderService implements BulkCreator {
    private static boolean alreadyLoaded;

    private static boolean alreadyRetreivedTaxa;
    protected static final Log log = LogFactory.getLog( ChromosomeLoaderService.class );
    private Chromosome chromosome;
    private ChromosomeDao chromosomeDao;
    private int nameCol;
    private int taxonCol;
    private TaxonDao taxonDao;
    private String view;
    Configuration conf;
    Map taxaMap;

    /**
     * @throws ConfigurationException
     */
    public ChromosomeLoaderService() throws ConfigurationException {
        conf = new PropertiesConfiguration( "chromosome.properties" );
        nameCol = conf.getInt( "chromosome.nameCol" );
        taxonCol = conf.getInt( "chromosome.taxonCol" );
        alreadyRetreivedTaxa = false;
        alreadyLoaded = false;
        view = "chromosome";
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
     * @return Returns the chromosome.
     */
    public edu.columbia.gemma.genome.Chromosome getChromosome() {
        return chromosome;
    }

    /**
     * @return Returns the chromosomeDao.
     */
    public edu.columbia.gemma.genome.ChromosomeDao getChromosomeDao() {
        return chromosomeDao;
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
     * @param chromosome The chromosome to set.
     */
    public void setChromosome( edu.columbia.gemma.genome.Chromosome chromosome ) {
        this.chromosome = chromosome;
    }

    /**
     * @param chromosomeDao
     */
    public void setChromosomeDao( edu.columbia.gemma.genome.ChromosomeDao chromosomeDao ) {
        this.chromosomeDao = chromosomeDao;
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
        Chromosome ch = Chromosome.Factory.newInstance();
        ch.setName( sArray[nameCol] );

        if ( !alreadyRetreivedTaxa ) taxaMap = findAllTaxa();

        ch.setTaxon( loadOrCreateTaxon( taxaMap, Integer.parseInt( sArray[taxonCol] ), null ) );

        Collection chromosomeCol = this.chromosomeDao.findByName( ch.getName() );

        if ( chromosomeCol.size() > 0 ) {
            log.info( "Chromosome with name: " + ch.getName() + " already exists, skipping." );
            return false;
        }
        this.chromosomeDao.create( ch );

        return true;
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
     * @param taxaMap
     * @param id
     * @param s
     * @return Taxon TODO put in taxonutils after making taxaMap in createFromRow static
     */
    private Taxon loadOrCreateTaxon( Map taxaMap, int id, Taxon taxon ) {
        Taxon t;
        Integer Id = new Integer( id );
        if ( taxaMap.containsKey( Id ) ) {
            t = ( TaxonImpl ) taxaMap.get( Id );
            System.err.println( t.toString() );
        } else {
            t = taxon;
        }
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