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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.xml.sax.SAXException;

import ubic.gemma.loader.expression.smd.model.SMDBioAssay;
import ubic.gemma.loader.expression.smd.model.SMDExperiment;
import ubic.gemma.loader.expression.smd.model.SMDPublication;
import ubic.gemma.loader.expression.smd.util.SmdUtil;
import ubic.gemma.loader.util.fetcher.SmdFetcher;

/**
 * Given a set of Publications from SMD, get all the meta-data for the experiments (bioassays) for the experiment_sets
 * (MAGE::Experiments) that it refers too.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class ExperimentFetcher extends SmdFetcher {

    private PublicationFetcher pubs;
    private Set<SMDExperiment> experiments;
    private SpeciesExperimentMap sem;

    // for each publication, get the experiment_sets. Then get the experiment set metadata file. then get the species.

    /**
     * @throws ConfigurationException
     * @throws IOException
     */
    public ExperimentFetcher( PublicationFetcher pubs ) throws IOException {
        super();
        this.pubs = pubs;
        experiments = new HashSet<SMDExperiment>();

        sem = new SpeciesExperimentMap();
    }

    /**
     * @throws IOException
     * @throws SAXException
     */
    public void fetch() throws IOException, SAXException {
        if ( !ftpClient.isConnected() ) ftpClient = ( new SmdUtil() ).connect( FTP.ASCII_FILE_TYPE );

        for ( Iterator<SMDPublication> iter = pubs.getIterator(); iter.hasNext(); ) {
            SMDPublication pubM = iter.next();

            log.info( "Seeking details for publication: " + pubM.getTitle() );

            List<SMDExperiment> expSets = pubM.getExperimentSets();
            for ( SMDExperiment expM : expSets ) {
                expM.setPublicationId( pubM.getId() );

                log.info( "Seeking experiment set meta file for " + expM.getName() );

                // now, the experiments for this won't be filled in. So we have to retrive it.
                FTPFile[] expSetFiles = ftpClient.listFiles( remoteBaseDir + "/" + pubM.getId() + "/"
                        + expM.getNumber() );

                for ( int i = 0; i < expSetFiles.length; i++ ) {
                    String expFile = expSetFiles[i].getName();

                    if ( !expFile.matches( "exptset_[0-9]+.meta" ) ) continue;

                    InputStream is = ftpClient.retrieveFileStream( remoteBaseDir + "/" + pubM.getId() + "/"
                            + expM.getNumber() + "/" + expFile );
                    if ( is == null ) throw new IOException( "Could not get stream for " + expFile );
                    SMDExperiment newExptSet = new SMDExperiment();
                    newExptSet.read( is );
                    is.close();

                    if ( !ftpClient.completePendingCommand() ) {
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
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for ( Iterator<SMDPublication> iter = pubs.getIterator(); iter.hasNext(); ) {
            SMDPublication pubM = iter.next();
            List<SMDExperiment> expSets = pubM.getExperimentSets();

            String pubString = pubM.toString();

            for ( SMDExperiment expM : expSets ) {
                String expSetString = expM.toString();

                List<SMDBioAssay> exps = expM.getExperiments();
                for ( SMDBioAssay exp : exps ) {
                    buf.append( pubString + "\t" + expSetString + "\t" + exp + "\t" + sem.getSpecies( exp.getId() )
                            + "\n" );
                }
            }
        }
        return buf.toString();
    }

    public Iterator<SMDExperiment> getExperimentsIterator() {
        return experiments.iterator();
    }

}