package edu.columbia.gemma.loader.sequence.gene;

//Parser packages.
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
import edu.columbia.gemma.sequence.gene.Taxon;
import edu.columbia.gemma.sequence.gene.TaxonDao;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class TaxonLoaderService {
    protected static final Log log = LogFactory.getLog( TaxonLoaderService.class );
    private TaxonDao taxonDao;
    int identifierCol;
    int nameCol;
    int scientificNameCol;
    int commonNameCol;
    int abbreviationCol;
    int ncbiIdCol;
    String localBasePath;
    String filename;
    String filepath;

    public TaxonLoaderService( TaxonDao td ) {
        this.taxonDao = td;
    }

    /**
     * @throws ConfigurationException
     */
    public TaxonLoaderService() throws ConfigurationException {
        Configuration conf = new PropertiesConfiguration( "taxon.properties" );
        localBasePath = ( String ) conf.getProperty( "taxon.local.datafile.basepath" );
        filename = ( String ) conf.getProperty( "taxon.filename" );
        filepath = localBasePath + "\\" + filename;
        identifierCol = conf.getInt( "taxon.identifierCol" );
        nameCol = conf.getInt( "taxon.nameCol" );
        scientificNameCol = conf.getInt( "taxon.scientificNameCol" );
        commonNameCol = conf.getInt( "taxon.commonNameCol" );
        abbreviationCol = conf.getInt( "taxon.abbreviationCol" );
        ncbiIdCol = conf.getInt( "taxon.ncbiIdCol" );
    }

    /**
     * @param geneDao
     */
    public void setTaxonDao( TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }

    /**
     * @return
     */
    protected TaxonDao getTaxonDao() {
        return this.taxonDao;
    }

    /**
     * @throws IOException
     */
    /*public void loadDatabase() throws IOException {
        saveTaxons( openFileAsStream() );
    }*/
    public void bulkLoad(String filename) throws IOException {
        if (filename==null){
            filename = this.filename;
        }
        else{
            this.filename = filename;
        }
            
        bulkLoad( openFileAsStream() );
    }

    /**
     * @return InputStream
     * @throws IOException
     */
    public InputStream openFileAsStream() throws IOException {
        File file = new File( filepath );
        if ( !file.canRead() ) throw new IOException( "Can't read from file " + filepath );

        return new FileInputStream( file );

    }

    /**
     * @param is
     * @throws IOException
     */
    public void bulkLoad( InputStream is ) throws IOException {
        int count = 0;
        String line = null;
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        br.readLine();
        log.info( "Reading file from " + filepath );
        while ( ( line = br.readLine() ) != null ) {
            String[] sArray = line.split( "\t" );
            Taxon t = Taxon.Factory.newInstance();
            t.setIdentifier( "taxon::" + count + "::" + sArray[identifierCol] );
            t.setName( sArray[nameCol] );
            t.setScientificName( sArray[nameCol] );
            t.setCommonName( sArray[commonNameCol] );
            t.setAbbreviation( sArray[abbreviationCol] );
            t.setNcbiId( Integer.parseInt( sArray[ncbiIdCol] ) );
            Collection col = getTaxonDao().findByScientificName( t.getScientificName() );
            if ( col.size() > 0 ) {
                log.info( " Object with scientificName: " + t.getScientificName() + " already exists." );
            } else {
                getTaxonDao().create( t );
            }
            count++;
        }
        br.close();
    }
    
   /**
    * 
    * @param scientificName
    * @return Collection
    */
    public Collection findByScientificName( String scientificName ) {
        if ( scientificName == null ) {
            throw new IllegalArgumentException(
                    "edu.columbia.gemma.sequence.gene.GeneService.findByScientificName(java.lang.String officialName) - 'scientificName' can not be null" );
        }
        return getTaxonDao().findByScientificName( scientificName );
    }

}

