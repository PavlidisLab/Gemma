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

package ubic.gemma.tasks.maintenance;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.spi.InternalCompass;
import org.compass.gps.impl.SingleCompassGps;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.MailEngine;

/**
 * @author klc
 * @version $Id$
 */
public class IndexerTaskImpl implements IndexerTask {

    /*
     * NOTE not configured using annotations because they get confused by the interfaces here.
     */

    private static final String FILE = "file://";

    private static final String PATH_PROPERTY = "compass.engine.connection";

    private static final String PATH_SUFFIX = "/index/";

    private SingleCompassGps arrayGps;

    private SingleCompassGps bibliographicGps;

    private SingleCompassGps biosequenceGps;

    private InternalCompass compassArray;

    private InternalCompass compassBibliographic;

    private InternalCompass compassBiosequence;

    private InternalCompass compassExperimentSet;

    private InternalCompass compassExpression;

    private InternalCompass compassGene;

    private InternalCompass compassGeneSet;

    private InternalCompass compassProbe;

    private SingleCompassGps experimentSetGps;

    private SingleCompassGps expressionGps;

    private SingleCompassGps geneGps;

    private SingleCompassGps geneSetGps;

    private Log log = LogFactory.getLog( this.getClass().getName() );

    private MailEngine mailEngine;

    private SingleCompassGps probeGps;

    @TaskMethod
    public IndexerResult execute( IndexerTaskCommand command ) {
        IndexerResult result = new IndexerResult( command );

        if ( command.isIndexEE() ) {
            if ( rebuildIndex( expressionGps, "Expression Experiment index" ) )
                result.setPathToExpressionIndex( getIndexPath( compassExpression ) );
            else
                result.setPathToExpressionIndex( null );

        }
        if ( command.isIndexAD() ) {
            if ( rebuildIndex( arrayGps, "Array Design index" ) )
                result.setPathToArrayIndex( getIndexPath( compassArray ) );
            else
                result.setPathToArrayIndex( null );

        }
        if ( command.isIndexBibRef() ) {
            if ( rebuildIndex( bibliographicGps, "Bibliographic Reference Index" ) )
                result.setPathToBibliographicIndex( getIndexPath( compassBibliographic ) );
            else
                result.setPathToBibliographicIndex( null );

        }
        if ( command.isIndexBioSequence() ) {
            if ( rebuildIndex( biosequenceGps, "Biosequence Reference Index" ) )
                result.setPathToBiosequenceIndex( getIndexPath( compassBiosequence ) );
            else
                result.setPathToBiosequenceIndex( null );
        }

        if ( command.isIndexProbe() ) {
            if ( rebuildIndex( probeGps, "Probe Reference Index" ) )
                result.setPathToProbeIndex( getIndexPath( compassProbe ) );
            else
                result.setPathToProbeIndex( null );
        }

        if ( command.isIndexGene() ) {
            if ( rebuildIndex( geneGps, "Gene index" ) )
                result.setPathToGeneIndex( getIndexPath( compassGene ) );
            else
                result.setPathToGeneIndex( null );
        }

        if ( command.isIndexGeneSet() ) {
            if ( rebuildIndex( geneSetGps, "Gene set index" ) )
                result.setPathToGeneSetIndex( getIndexPath( compassGeneSet ) );
            else
                result.setPathToGeneSetIndex( null );
        }

        if ( command.isIndexExperimentSet() ) {
            if ( rebuildIndex( experimentSetGps, "Experiment set index" ) )
                result.setPathToExperimentSetIndex( getIndexPath( compassExperimentSet ) );
            else
                result.setPathToExperimentSetIndex( null );
        }
        log.info( "Indexing Finished. Returning result to space. Result is: " + result );
        return result;

    }

    /**
     * @param arrayGps the arrayGps to set
     */
    public void setArrayGps( SingleCompassGps arrayGps ) {
        this.arrayGps = arrayGps;
    }

    /**
     * @param bibliographicGps the bibliographicGps to set
     */
    public void setBibliographicGps( SingleCompassGps bibliographicGps ) {
        this.bibliographicGps = bibliographicGps;
    }

    /**
     * @param bioSequenceGps the bioSequenceGps to set
     */
    public void setBiosequenceGps( SingleCompassGps biosequenceGps ) {
        this.biosequenceGps = biosequenceGps;
    }

    /**
     * @param compassArray the compassArray to set
     */
    public void setCompassArray( InternalCompass compassArray ) {
        this.compassArray = compassArray;
    }

    /**
     * @param compassBibliographic the compassBibliographic to set
     */
    public void setCompassBibliographic( InternalCompass compassBibliographic ) {
        this.compassBibliographic = compassBibliographic;
    }

    /**
     * @param compassBiosequence the compassBiosequence to set
     */
    public void setCompassBiosequence( InternalCompass compassBiosequence ) {
        this.compassBiosequence = compassBiosequence;
    }

    /**
     * @param compassExperimentSet the compassExperimentSet to set
     */
    public void setCompassExperimentSet( InternalCompass compassExperimentSet ) {
        this.compassExperimentSet = compassExperimentSet;
    }

    /**
     * @param compassExpression the compassExpression to set
     */
    public void setCompassExpression( InternalCompass compassExpression ) {
        this.compassExpression = compassExpression;
    }

    /**
     * @param compassGene the compassGene to set
     */
    public void setCompassGene( InternalCompass compassGene ) {
        this.compassGene = compassGene;
    }

    /**
     * @param compassGeneSet the compassGeneSet to set
     */
    public void setCompassGeneSet( InternalCompass compassGeneSet ) {
        this.compassGeneSet = compassGeneSet;
    }

    /**
     * @param compassProbe the compassProbe to set
     */
    public void setCompassProbe( InternalCompass compassProbe ) {
        this.compassProbe = compassProbe;
    }

    /**
     * @param experimentSetGps the experimentSetGps to set
     */
    public void setExperimentSetGps( SingleCompassGps experimentSetGps ) {
        this.experimentSetGps = experimentSetGps;
    }

    /**
     * @param expressionGps the expressionGps to set
     */
    public void setExpressionGps( SingleCompassGps expressionGps ) {
        this.expressionGps = expressionGps;
    }

    /**
     * @param geneGps the geneGps to set
     */
    public void setGeneGps( SingleCompassGps geneGps ) {
        this.geneGps = geneGps;
    }

    /**
     * @param geneSetGps the geneSetGps to set
     */
    public void setGeneSetGps( SingleCompassGps geneSetGps ) {
        this.geneSetGps = geneSetGps;
    }

    /**
     * @param mailEngine
     */
    public void setMailEngine( MailEngine mailEngine ) {
        this.mailEngine = mailEngine;
    }

    /**
     * @param probeGps the probeGps to set
     */
    public void setProbeGps( SingleCompassGps probeGps ) {
        this.probeGps = probeGps;
    }

    private String getIndexPath( InternalCompass compass ) {
        return compass.getSettings().getSetting( PATH_PROPERTY ).replaceFirst( FILE, "" ) + PATH_SUFFIX;
    }

    private Boolean rebuildIndex( CompassGpsInterfaceDevice device, String whatIndexingMsg ) {

        StopWatch timer = new StopWatch();
        timer.start();
        log.info( "Rebuilding " + whatIndexingMsg + ". First attempt.");

        Boolean success = CompassUtils.rebuildCompassIndex( device );
        
        // First attempt failed. Wait a bit then re-try.
        // See bug#2031. There are intermittent indexing failures. The cause is unknown at the moment.
        if (!success) {
            log.warn( "Failed to index " + whatIndexingMsg + ". Trying it again...");
            try {
                Thread.sleep( 120000 ); // sleep for 2 minutes.
            } catch ( InterruptedException e ) {
                log.warn( "Job to index" + whatIndexingMsg + " was interrupted." );
                return false;
            }
            
            timer.reset();
            timer.start();
            log.info( "Rebuilding " + whatIndexingMsg + ". Second attempt.");
            success = CompassUtils.rebuildCompassIndex( device );
        }
                
        // If failed for the second time send an email to administrator.
        if ( !success ) {
            mailEngine.sendAdminMessage( "Failed to index " + whatIndexingMsg, "Failed to index " + whatIndexingMsg
                    + ".  See logs for details" );
            log.info( "Failed rebuilding index for " + whatIndexingMsg + ".  Took (ms): " + timer.getTime() );

        } else {
            log.info( "Finished rebuilding " + whatIndexingMsg + ".  Took (ms): " + timer.getTime() );
        }

        return success;

    }

}
