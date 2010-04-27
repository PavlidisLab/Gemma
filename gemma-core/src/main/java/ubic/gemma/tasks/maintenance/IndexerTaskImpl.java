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
import ubic.gemma.util.CompassUtils;

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

    private InternalCompass compassExpression;

    private InternalCompass compassGene;

    private InternalCompass compassProbe;

    private SingleCompassGps expressionGps;

    private SingleCompassGps geneGps;

    private Log log = LogFactory.getLog( this.getClass().getName() );

    private SingleCompassGps probeGps;

    @TaskMethod
    public IndexerResult execute( IndexerTaskCommand command ) {
        IndexerResult result = new IndexerResult( command );

        if ( command.isIndexEE() ) {
            rebuildIndex( expressionGps, "Expression Experiment index" );
            result.setPathToExpresionIndex( getIndexPath( compassExpression ) );

        }
        if ( command.isIndexAD() ) {
            rebuildIndex( arrayGps, "Array Design index" );
            result.setPathToArrayIndex( getIndexPath( compassArray ) );

        }
        if ( command.isIndexBibRef() ) {
            rebuildIndex( bibliographicGps, "Bibliographic Reference Index" );
            result.setPathToBibliographicIndex( getIndexPath( compassBibliographic ) );

        }
        if ( command.isIndexBioSequence() ) {
            rebuildIndex( biosequenceGps, "Biosequence Reference Index" );
            result.setPathToBiosequenceIndex( getIndexPath( compassBiosequence ) );

        }

        if ( command.isIndexProbe() ) {
            rebuildIndex( probeGps, "Probe Reference Index" );
            result.setPathToProbeIndex( getIndexPath( compassProbe ) );
        }

        if ( command.isIndexGene() ) {
            rebuildIndex( geneGps, "Gene index" );
            result.setPathToGeneIndex( getIndexPath( compassGene ) );

        }
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
     * @param compassProbe the compassProbe to set
     */
    public void setCompassProbe( InternalCompass compassProbe ) {
        this.compassProbe = compassProbe;
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
     * @param probeGps the probeGps to set
     */
    public void setProbeGps( SingleCompassGps probeGps ) {
        this.probeGps = probeGps;
    }

    private String getIndexPath( InternalCompass compass ) {
        return compass.getSettings().getSetting( PATH_PROPERTY ).replaceFirst( FILE, "" ) + PATH_SUFFIX;
    }

    private void rebuildIndex( CompassGpsInterfaceDevice device, String whatIndexingMsg ) {

        StopWatch timer = new StopWatch();
        timer.start();
        log.info( "Rebuilding " + whatIndexingMsg );

        CompassUtils.rebuildCompassIndex( device );

        log.info( "Finished rebuilding " + whatIndexingMsg + ".  Took (ms): " + timer.getTime() );
        log.info( " \n " );

    }

}
