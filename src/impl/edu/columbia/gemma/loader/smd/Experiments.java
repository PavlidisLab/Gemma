package edu.columbia.gemma.loader.smd;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.xml.sax.SAXException;

import edu.columbia.gemma.loader.smd.util.SmdUtil;
import edu.columbia.gemma.loader.smd.model.SMDBioAssay;
import edu.columbia.gemma.loader.smd.model.SMDExperiment;
import edu.columbia.gemma.loader.smd.model.SMDPublication;

/**
 * Given a set of Publications from SMD, get all the meta-data for the experiments (bioassays) for the experiment_sets
 * (MAGE::Experiments) that it refers too.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class Experiments {

    protected static final Log log = LogFactory.getLog( Publications.class );
    private String baseDir = "smd/publications/";

    private Publications pubs;
    private Set experiments;
    private SpeciesExperimentMap sem;
    private FTPClient f;

    // for each publication, get the experiment_sets. Then get the experiment set metadata file. then get the species.

    /**
     * @throws ConfigurationException
     * @throws IOException
     */
    public Experiments( Publications pubs ) throws IOException, ConfigurationException {
        this.pubs = pubs;
        experiments = new HashSet();

        Configuration config = new PropertiesConfiguration( "smd.properties" );

        baseDir = ( String ) config.getProperty( "smd.publication.baseDir" );

        sem = new SpeciesExperimentMap();
    }

    /**
     * @throws IOException
     * @throws SAXException
     */
    public void retrieveByFTP() throws IOException, SAXException {

        if ( !f.isConnected() ) f = SmdUtil.connect( FTP.ASCII_FILE_TYPE );

        for ( Iterator iter = pubs.getIterator(); iter.hasNext(); ) {
            SMDPublication pubM = ( SMDPublication ) iter.next();

            log.info( "Seeking details for publication: " + pubM.getTitle() );

            List expSets = pubM.getExperimentSets();
            for ( Iterator iterator = expSets.iterator(); iterator.hasNext(); ) {
                SMDExperiment expM = ( SMDExperiment ) iterator.next();
                expM.setPublicationId( pubM.getId() );

                log.info( "Seeking experiment set meta file for " + expM.getName() );

                // now, the experiments for this won't be filled in. So we have to retrive it.
                FTPFile[] expSetFiles = f.listFiles( baseDir + "/" + pubM.getId() + "/" + expM.getNumber() );

                for ( int i = 0; i < expSetFiles.length; i++ ) {
                    String expFile = expSetFiles[i].getName();

                    if ( !expFile.matches( "exptset_[0-9]+.meta" ) ) continue;

                    InputStream is = f.retrieveFileStream( baseDir + "/" + pubM.getId() + "/" + expM.getNumber() + "/"
                            + expFile );
                    if ( is == null ) throw new IOException( "Could not get stream for " + expFile );
                    SMDExperiment newExptSet = new SMDExperiment();
                    newExptSet.read( is );
                    is.close();

                    if ( !f.completePendingCommand() ) {
                        log.error( "Failed to complete download of " + expFile );
                        continue;
                    }

                    experiments.add( newExptSet );

                    expM.setExperiments( newExptSet.getExperiments() );

                    log.info( "Retrieved " + expFile + " for publication " + pubM.getId() );
                }
            }
        }
    }

    /**
     * Print out a tabbed listing of all the experiments found.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();

        for ( Iterator iter = pubs.getIterator(); iter.hasNext(); ) {
            SMDPublication pubM = ( SMDPublication ) iter.next();
            List expSets = pubM.getExperimentSets();

            String pubString = pubM.toString();

            for ( Iterator iterator = expSets.iterator(); iterator.hasNext(); ) {
                SMDExperiment expM = ( SMDExperiment ) iterator.next();

                String expSetString = expM.toString();

                List exps = expM.getExperiments();
                for ( Iterator itr = exps.iterator(); itr.hasNext(); ) {
                    SMDBioAssay exp = ( SMDBioAssay ) itr.next();

                    buf.append( pubString + "\t" + expSetString + "\t" + exp + "\t" + sem.getSpecies( exp.getId() )
                            + "\n" );

                }
            }
        }

        return buf.toString();
    }

    public Iterator getExperimentsIterator() {
        return experiments.iterator();
    }

    public static void main( String[] args ) {
        try {
            Publications foo = new Publications();
            foo.retrieveByFTP( 15 );
            Experiments bar = new Experiments( foo );
            bar.retrieveByFTP();

            System.out.print( bar );
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( SAXException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( ConfigurationException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}