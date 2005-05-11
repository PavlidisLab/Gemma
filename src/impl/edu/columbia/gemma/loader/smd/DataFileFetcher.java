package edu.columbia.gemma.loader.smd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.xml.sax.SAXException;

import edu.columbia.gemma.loader.smd.model.SMDBioAssay;
import edu.columbia.gemma.loader.smd.model.SMDExperiment;
import edu.columbia.gemma.loader.smd.model.SMDFile;
import edu.columbia.gemma.loader.smd.util.SmdUtil;

/**
 * Download (but do not parse) data files for a given ExperimentSet.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DataFileFetcher {

    protected static final Log log = LogFactory.getLog( DataFileFetcher.class );
    private String localBasePath = "//cgcfs1/projects/pavlidis/grp/arraydata/__incoming/smd/rawData";
    private String baseDir = "smd/experiments/";
    private Map cuts;
    private FTPClient f;
    private boolean success = false;
    private Set localFiles; // set of SMDExternalFiles recording download information.
    private boolean force = false; // force-redownload of files

    public DataFileFetcher() throws IOException, ConfigurationException {
        localFiles = new HashSet();
        Configuration config = new PropertiesConfiguration( "Gemma.properties" );

        localBasePath = ( String ) config.getProperty( "smd.local.datafile.basepath" );
        baseDir = ( String ) config.getProperty( "smd.experiments.baseDir" );

        getCuts();

    }

    /**
     * @see DataFileFetcher#findCut(int)
     * @throws IOException
     */
    private void getCuts() throws IOException {
        FTPFile[] files = f.listFiles( baseDir );
        cuts = new TreeMap();
        for ( int i = 0; i < files.length; i++ ) {
            String name = files[i].getName();
            String[] cutPoints = name.split( "-" );
            cuts.put( new Integer( cutPoints[0] ), name );
        }
        log.info( "Got cuts" );
    }

    /**
     * @param expM
     * @throws SocketException
     * @throws IOException
     */
    public void retrieveByFTP( SMDExperiment expM ) throws SocketException, IOException {
        log.info( "Seeking experiment data files for " + expM.getName() );

        if ( !f.isConnected() ) SmdUtil.connect( FTP.BINARY_FILE_TYPE );

        // create a place to store the files.
        File newDir = new File( localBasePath + "/" + expM.getNumber() );

        if ( !newDir.exists() ) {
            success = newDir.mkdir();
            if ( !success ) {
                f.disconnect();
                throw new IOException( "Could not create output directory" );
            }
            log.info( "Created directory " + localBasePath + "/" + expM.getNumber() );
        }

        List bioAssays = expM.getExperiments();

        for ( Iterator iter = bioAssays.iterator(); iter.hasNext(); ) {
            SMDBioAssay bA = ( SMDBioAssay ) iter.next();
            int assayId = bA.getId();

            String group = findCut( assayId );
            if ( group == null ) {
                throw new IllegalStateException( "Could not find remote directory for assay number " + assayId );
            }

            String outputFileName = newDir + "/" + assayId + ".xls.gz";
            File outputFile = new File( outputFileName );

            String seekFile = baseDir + group + "/" + assayId + ".xls.gz";
            FTPFile[] allfilesInGroup = f.listFiles( seekFile );
            if ( allfilesInGroup.length == 0 ) {
                log.error( "File " + seekFile + " does not seem to exist on the remote host" );
                continue;
            }

            long expectedSize = allfilesInGroup[0].getSize();
            if ( outputFile.exists() && outputFile.length() == expectedSize && !force ) {
                log.warn( "Output file" + outputFileName + " already exists with correct size. Will not re-download" );
                continue;
            }

            OutputStream os = new FileOutputStream( outputFileName );

            log.info( "Seeking file " + seekFile + " with size " + expectedSize + " bytes" );
            success = f.retrieveFile( seekFile, os );
            os.close();
            if ( !success ) {
                log.error( "Failed to complete download of " + assayId );
                continue;
            }

            // get meta-data about the file.
            SMDFile file = new SMDFile();
            file.setDownloadDate( new SimpleDateFormat().format( new Date() ) );
            file.setDownloadURL( seekFile );
            file.setLocalPath( outputFileName );
            file.setSize( outputFile.length() );
            localFiles.add( file );

            log.info( "Retrieved " + assayId + ".xls.gz" + " for experiment(set) " + expM.getNumber()
                    + " .Output file is " + outputFileName );
        }

    }

    /**
     * The experiments on the SMD ftp site are divided up into directories holding ~1000 assays each. This method finds
     * the correct directory to find an assay with a given id.
     * 
     * @param assayId such as 1294
     * @return the directory name to look in such as "1001-2000"
     */
    private String findCut( int assayId ) {
        Integer previous = null;
        for ( Iterator iterator = cuts.keySet().iterator(); iterator.hasNext(); ) {
            Integer element = ( Integer ) iterator.next();
            int v = element.intValue();
            if ( assayId < v ) {
                // log.info( "Will seek " + assayId + " in " + ( String ) cuts.get( previous ) );
                return ( String ) cuts.get( previous );
            }
            previous = element;
        }
        return null;
    }

    public static void main( String[] args ) {
        try {
            DataFileFetcher fb = new DataFileFetcher();
            Publications foo = new Publications();

            foo.retrieveByFTP( 10 );
            Experiments bar = new Experiments( foo );
            bar.retrieveByFTP();

            for ( Iterator iter = bar.getExperimentsIterator(); iter.hasNext(); ) {
                SMDExperiment element = ( SMDExperiment ) iter.next();
                fb.retrieveByFTP( element );
            }

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

    public boolean isForce() {
        return force;
    }

    public void setForce( boolean force ) {
        this.force = force;
    }
}