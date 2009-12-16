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

package ubic.gemma.grid.javaspaces.task.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.spi.InternalCompass;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.progress.grid.javaspaces.SpacesProgressAppender;

/**
 * @author klc
 * @version $Id$
 */
public class IndexerTaskImpl extends BaseSpacesTask implements IndexerTask {

    private static final String PATH_PROPERTY = "compass.engine.connection";

    private static final String PATH_SUFFIX = "/index/";

    private static final String FILE = "file://";

    private Log log = LogFactory.getLog( this.getClass().getName() );

    private CompassGpsInterfaceDevice geneGps;

    private CompassGpsInterfaceDevice expressionGps;

    private CompassGpsInterfaceDevice arrayGps;

    private CompassGpsInterfaceDevice bibliographicGps;

    private CompassGpsInterfaceDevice probeGps;

    private CompassGpsInterfaceDevice bioSequenceGps;

    private InternalCompass compassExpression;

    private InternalCompass compassGene;

    private InternalCompass compassArray;

    private InternalCompass compassProbe;

    private InternalCompass compassBiosequence;

    private InternalCompass compassBibliographic;

    public void setArrayGps( CompassGpsInterfaceDevice arrayGps ) {
        this.arrayGps = arrayGps;
    }

    public void setBibliographicGps( CompassGpsInterfaceDevice bibliographicGps ) {
        this.bibliographicGps = bibliographicGps;
    }

    public void setExpressionGps( CompassGpsInterfaceDevice expressionGps ) {
        this.expressionGps = expressionGps;
    }

    public void setProbeGps( CompassGpsInterfaceDevice probeGps ) {
        this.probeGps = probeGps;
    }

    public void setGeneGps( CompassGpsInterfaceDevice geneGps ) {
        this.geneGps = geneGps;
    }

    public void setBioSequenceGps( CompassGpsInterfaceDevice bsGps ) {
        this.bioSequenceGps = bsGps;
    }

    public void setCompassArray( InternalCompass compassArray ) {
        this.compassArray = compassArray;
    }

    public void setCompassBibliographic( InternalCompass compassBibliographic ) {
        this.compassBibliographic = compassBibliographic;
    }

    public void setCompassBiosequence( InternalCompass compassBiosequence ) {
        this.compassBiosequence = compassBiosequence;
    }

    public void setCompassExpression( InternalCompass compassExpression ) {
        this.compassExpression = compassExpression;
    }

    public void setCompassGene( InternalCompass compassGene ) {
        this.compassGene = compassGene;
    }

    public void setCompassProbe( InternalCompass compassProbe ) {
        this.compassProbe = compassProbe;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(java.lang.String, boolean, boolean)
     */
    public IndexerResult execute( IndexerTaskCommand command ) {

        SpacesProgressAppender spacesProgressAppender = super.initProgressAppender( this.getClass() );

        IndexerResult result = new IndexerResult();

            if ( command.isIndexGene() ) {
                rebuildIndex( geneGps, "Gene index" );
                result.setPathToGeneIndex( compassGene.getSettings().getSetting( PATH_PROPERTY )
                        .replaceFirst( FILE, "" )
                        + PATH_SUFFIX );
            }
            if ( command.isIndexEE() ) {
                rebuildIndex( expressionGps, "Expression Experiment index" );
                result.setPathToExpresionIndex( compassExpression.getSettings().getSetting( PATH_PROPERTY )
                        .replaceFirst( FILE, "" )
                        + PATH_SUFFIX );
            }
            if ( command.isIndexAD() ) {
                rebuildIndex( arrayGps, "Array Design index" );
                result.setPathToArrayIndex( compassArray.getSettings().getSetting( PATH_PROPERTY ).replaceFirst( FILE,
                        "" )
                        + PATH_SUFFIX );

            }
            if ( command.isIndexBibRef() ) {
                rebuildIndex( bibliographicGps, "Bibliographic Reference Index" );
                result.setPathToBibliographicIndex( compassBibliographic.getSettings().getSetting( PATH_PROPERTY )
                        .replaceFirst( FILE, "" )
                        + PATH_SUFFIX );
            }
            if ( command.isIndexProbe() ) {
                rebuildIndex( probeGps, "Probe Reference Index" );
                result.setPathToProbeIndex( compassProbe.getSettings().getSetting( PATH_PROPERTY ).replaceFirst( FILE,
                        "" )
                        + PATH_SUFFIX );
            }
            if ( command.isIndexBioSequence() ) {
                rebuildIndex( bioSequenceGps, "Biosequence Reference Index" );
                result.setPathToBiosequenceIndex( compassBiosequence.getSettings().getSetting( PATH_PROPERTY )
                        .replaceFirst( FILE, "" )
                        + PATH_SUFFIX );

            }

    

        super.tidyProgress( spacesProgressAppender );

        return result;
    }

    protected void rebuildIndex( CompassGpsInterfaceDevice device, String whatIndexingMsg ) {

        long time = System.currentTimeMillis();

        log.info( "Rebuilding " + whatIndexingMsg );
        try{
            CompassUtils.rebuildCompassIndex( device );
        } catch ( Exception e ) {
            log.error( e );
        }
        time = System.currentTimeMillis() - time;

        log.info( "Finished rebuilding " + whatIndexingMsg + ".  Took (ms): " + time );
        log.info( " \n " );

    }

}
