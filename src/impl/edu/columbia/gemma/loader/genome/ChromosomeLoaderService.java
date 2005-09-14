/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package edu.columbia.gemma.loader.genome;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
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

    private static boolean alreadyRetreivedTaxa;
    protected static final Log log = LogFactory.getLog( ChromosomeLoaderService.class );
    private Chromosome chromosome;
    private ChromosomeDao chromosomeDao;
    private int nameCol;
    private int taxonCol;
    private TaxonDao taxonDao;
    private String view;
    Configuration conf;
    Map<Integer, Taxon> taxaMap;

    /**
     * @throws ConfigurationException
     */
    public ChromosomeLoaderService() throws ConfigurationException {
        conf = new PropertiesConfiguration( "Gemma.properties" );
        nameCol = conf.getInt( "chromosome.nameCol" );
        taxonCol = conf.getInt( "chromosome.taxonCol" );
        alreadyRetreivedTaxa = false;
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
    @SuppressWarnings("unchecked")
    private boolean createFromRow( String line ) throws NumberFormatException {
        String[] sArray = line.split( "\t" );
        Chromosome ch = Chromosome.Factory.newInstance();
        ch.setName( sArray[nameCol] );

        if ( !alreadyRetreivedTaxa ) taxaMap = findAllTaxa();

        ch.setTaxon( loadOrCreateTaxon( Integer.parseInt( sArray[taxonCol] ), null ) );

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
    @SuppressWarnings("unchecked")
    private Map findAllTaxa() {
        Collection<Taxon> taxa = this.taxonDao.loadAll();
        taxaMap = new HashMap<Integer, Taxon>();

        int id = 1;
        for ( Taxon taxon : taxa ) {
            Integer Id = new Integer( id );
            taxaMap.put( Id, taxon );
            id++;
        }
        alreadyRetreivedTaxa = true;
        return taxaMap;
    }

    /**
     * @param id
     * @param s
     * @return Taxon TODO put in taxonutils after making taxaMap in createFromRow static FIXME - what does this method
     *         do?
     */
    private Taxon loadOrCreateTaxon( int id, Taxon taxon ) {
        Taxon t;
        Integer Id = new Integer( id );
        if ( taxaMap.containsKey( Id ) ) {
            t = taxaMap.get( Id );
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