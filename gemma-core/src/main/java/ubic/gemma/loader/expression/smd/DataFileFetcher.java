/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.expression.smd;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;

import ubic.basecode.util.NetUtils;
import ubic.gemma.loader.expression.smd.model.SMDBioAssay;
import ubic.gemma.loader.expression.smd.model.SMDExperiment;
import ubic.gemma.loader.expression.smd.model.SMDFile;
import ubic.gemma.loader.expression.smd.util.SmdUtil;
import ubic.gemma.loader.util.fetcher.SmdFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

/**
 * Download (but do not parse) data files for a given ExperimentSet.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class DataFileFetcher extends SmdFetcher {

    private Map<Integer, String> cuts;
    private Set<SMDFile> localFiles;

    public DataFileFetcher() throws IOException {
        super();
        localFiles = new HashSet<SMDFile>();
        getCuts();
    }

    /**
     * @see DataFileFetcher#findCut(int)
     * @throws IOException
     */
    private void getCuts() throws IOException {
        FTPFile[] files = ftpClient.listFiles( remoteBaseDir );
        cuts = new TreeMap<Integer, String>();
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
    public void fetch( SMDExperiment expM ) throws SocketException, IOException {
        log.info( "Seeking experiment data files for " + expM.getName() );

        if ( !ftpClient.isConnected() ) ( new SmdUtil() ).connect( FTP.BINARY_FILE_TYPE );

        // create a place to store the files.
        File newDir = mkdir( Integer.toString( expM.getNumber() ) );

        List<SMDBioAssay> bioAssays = expM.getExperiments();

        for ( SMDBioAssay bA : bioAssays ) {
            int assayId = bA.getId();

            String group = findCut( assayId );
            if ( group == null ) {
                throw new IllegalStateException( "Could not find remote directory for assay number " + assayId );
            }

            String outputFileName = newDir.getAbsolutePath() + File.separatorChar + assayId + ".xls.gz";

            String seekFile = remoteBaseDir + group + "/" + assayId + ".xls.gz";

            NetUtils.ftpDownloadFile( ftpClient, seekFile, outputFileName, force );

            // get meta-data about the file.
            SMDFile file = new SMDFile();
            file.setDownloadDate( new SimpleDateFormat().format( new Date() ) );
            file.setDownloadURL( seekFile );
            file.setLocalPath( outputFileName );
            // file.setSize( outputFile.length() );
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
        for ( Integer element : cuts.keySet() ) {
            int v = element.intValue();
            if ( assayId < v ) {
                log.debug( "Will seek " + assayId + " in " + cuts.get( previous ) );
                return cuts.get( previous );
            }
            previous = element;
        }
        return null;
    }

    /**
     * @return
     */
    @Override
    public boolean isForce() {
        return force;
    }

    /**
     * @param force
     */
    @Override
    public void setForce( boolean force ) {
        this.force = force;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Fetcher#fetch(java.lang.String)
     */
    @Override
    public Collection<LocalFile> fetch( String identifier ) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#initConfig()
     */
    @Override
    protected void initConfig() {
        localBasePath = ConfigUtils.getString( "smd.local.datafile.basepath" );
        remoteBaseDir = ConfigUtils.getString( "smd.experiments.baseDir" );
    }
}