package edu.columbia.gemma.loader.smd;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import edu.columbia.gemma.loader.smd.util.SmdUtil;

/**
 * The list of all BioAssays for a given species.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpeciesBioAssayList {

    protected static final Log log = LogFactory.getLog( SpeciesBioAssayList.class );
    private String baseDir = "smd/organisms/";
    private SMDSpecies speciesMap;
    private Set experiments;
    private FTPClient f;

    /**
     * @param species
     * @throws IOException
     */
    public SpeciesBioAssayList() {
        Configuration config = null;
        try {
            config = new PropertiesConfiguration( "Gemma.properties" );
        } catch ( ConfigurationException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        speciesMap = new SMDSpecies();
        experiments = new HashSet();
        baseDir = ( String ) config.getProperty( "smd.organism.baseDir" );

    }

    /**
     * Retrieve the list of experiments (bioassays) for a given species. The species can be something like "human" or
     * "Homo sapiens".
     * 
     * @param species
     * @throws IOException
     */
    public void retrieveByFTP( String species ) throws IOException {
        if ( !f.isConnected() ) f = SmdUtil.connect( FTP.ASCII_FILE_TYPE );

        String work = baseDir + speciesMap.getCode( species );
        FTPFile[] files = f.listFiles( work );

        for ( int i = 0; i < files.length; i++ ) {
            if ( !files[i].isDirectory() ) {
                String name = files[i].getName();
                name = name.replaceAll( "\\.xls\\.gz", "" );
                experiments.add( name );
            }
        }

        log.info( experiments.size() + " experiments found for " + species );
        f.disconnect();
    }

    /**
     * @return
     */
    public Set getExperiments() {
        return experiments;
    }

    /**
     * @param experiments
     */
    public void setExperiments( Set experiments ) {
        this.experiments = experiments;
    }

    public static void main( String[] args ) {
        try {
            SpeciesBioAssayList foo = new SpeciesBioAssayList();
            foo.retrieveByFTP( "human" );
            System.err.println( foo.getExperiments() );
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}