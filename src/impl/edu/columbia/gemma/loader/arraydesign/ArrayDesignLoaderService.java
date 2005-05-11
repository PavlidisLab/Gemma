package edu.columbia.gemma.loader.arraydesign;

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

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.loader.loaderutils.BulkCreator;

/**
 * Load ArrayDesigns in bulk.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignLoaderService"
 * @spring.property name="arrayDesignDao" ref="arrayDesignDao"
 */
public class ArrayDesignLoaderService implements BulkCreator {

    protected static final Log log = LogFactory.getLog( ArrayDesignLoaderService.class );

    private ArrayDesignDao arrayDesignDao;

    private int auditTrailCol;
    private int bioAssayCol;
    private int identifierCol;
    private int nameCol;
    private int numOfCompositeSequencesCol;
    private int numOfFeaturesCol;
    private int securityCol;

    private String view;
    Configuration conf;

    /**
     * @throws ConfigurationException TODO fill in the auditTrail, security, and bioAssay to complete associations
     */
    public ArrayDesignLoaderService() throws ConfigurationException {
        conf = new PropertiesConfiguration( "Gemma.properties" );
        identifierCol = conf.getInt( "arrayDesign.identifierCol" );
        nameCol = conf.getInt( "arrayDesign.nameCol" );
        numOfCompositeSequencesCol = conf.getInt( "arrayDesign.numOfCompositeSequencesCol" );
        numOfFeaturesCol = conf.getInt( "arrayDesign.numOfFeaturesCol" );
        //        auditTrailCol = conf.getInt( "arrayDesign.auditTrailCol" );
        //        securityCol = conf.getInt( "arrayDesign.securityCol" );
        //        bioAssayCol = conf.getInt( "arrayDesign.bioAssayCol" );
        //        view = "arrayDesign";
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
     * @return Returns the arrayDesignDao.
     */
    public ArrayDesignDao getArrayDesignDao() {
        return arrayDesignDao;
    }

    /**
     * @param br
     * @throws IOException TODO put in loaderutils
     */
    public void handleHeader( BufferedReader br ) throws IOException {
        br.readLine();
    }

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @param line
     * @throws NumberFormatException
     */
    private boolean createFromRow( String line ) throws NumberFormatException {
        String[] sArray = line.split( "\t" );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( sArray[nameCol] );
        ad.setNumberOfFeatures( Integer.parseInt( sArray[numOfFeaturesCol] ) );
        //ad.setNumberOfCompositeSequences( Integer.parseInt( sArray[numOfCompositeSequencesCol] ) );

        Collection arrayDesignCol = this.arrayDesignDao.findByName( ad.getName() );

        if ( arrayDesignCol.size() > 0 ) {
            log.info( "ArrayDesign with name: " + ad.getName() + " already exists, skipping." );
            return false;
        }
        this.arrayDesignDao.create( ad );
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