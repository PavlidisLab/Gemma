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

package ubic.gemma.core.tasks.maintenance;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.spi.InternalCompass;
import org.compass.gps.impl.SingleCompassGps;
import org.compass.gps.spi.CompassGpsInterfaceDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.persistence.util.CompassUtils;
import ubic.gemma.persistence.util.MailEngine;

/**
 * NOTE do not set this up to run on a remote worker. The search index directory may not be accessible.
 *
 * @author klc
 */
@SuppressWarnings({ "unused", "SpringJavaInjectionPointsAutowiringInspection" })
// Possible external use, compass qualifiers in unparsed context files
@Component
@Scope("prototype")
public class IndexerTaskImpl extends AbstractTask<IndexerResult, IndexerTaskCommand> implements IndexerTask {

    /*
     * NOTE not configured using annotations because they get confused by the interfaces here.
     */
    private static final String FILE = "file://";
    private static final String PATH_PROPERTY = "compass.engine.connection";
    private static final String PATH_SUFFIX = "/index/";

    @Autowired
    @Qualifier("arrayGps")
    private SingleCompassGps arrayGps;

    @Autowired
    @Qualifier("bibliographicGps")
    private SingleCompassGps bibliographicGps;

    @Autowired
    @Qualifier("biosequenceGps")
    private SingleCompassGps biosequenceGps;

    @Autowired
    @Qualifier("experimentSetGps")
    private SingleCompassGps experimentSetGps;

    @Autowired
    @Qualifier("expressionGps")
    private SingleCompassGps expressionGps;

    @Autowired
    @Qualifier("geneGps")
    private SingleCompassGps geneGps;
    @Autowired
    @Qualifier("geneSetGps")
    private SingleCompassGps geneSetGps;

    @Autowired
    @Qualifier("probeGps")
    private SingleCompassGps probeGps;

    @Autowired
    @Qualifier("compassArray")
    private InternalCompass compassArray;

    @Autowired
    @Qualifier("compassBibliographic")
    private InternalCompass compassBibliographic;

    @Autowired
    @Qualifier("compassBiosequence")
    private InternalCompass compassBiosequence;

    @Autowired
    @Qualifier("compassExperimentSet")
    private InternalCompass compassExperimentSet;

    @Autowired
    @Qualifier("compassExpression")
    private InternalCompass compassExpression;

    @Autowired
    @Qualifier("compassGene")
    private InternalCompass compassGene;

    @Autowired
    @Qualifier("compassGeneSet")
    private InternalCompass compassGeneSet;

    @Autowired
    @Qualifier("compassProbe")
    private InternalCompass compassProbe;

    @Autowired
    private OntologyService ontologyService;

    private Log log = LogFactory.getLog( this.getClass().getName() );

    @Autowired
    private MailEngine mailEngine;

    @Override
    public IndexerResult call() {
        IndexerResult result = new IndexerResult( taskCommand );

        if ( taskCommand.isIndexEE() ) {
            if ( this.rebuildIndex( expressionGps, "Expression Experiment index" ) )
                result.setPathToExpressionIndex( this.getIndexPath( compassExpression ) );
            else
                result.setPathToExpressionIndex( null );

        }
        if ( taskCommand.isIndexAD() ) {
            if ( this.rebuildIndex( arrayGps, "Platform/Array design index" ) )
                result.setPathToArrayIndex( this.getIndexPath( compassArray ) );
            else
                result.setPathToArrayIndex( null );

        }
        if ( taskCommand.isIndexBibRef() ) {
            if ( this.rebuildIndex( bibliographicGps, "Bibliographic Reference Index" ) )
                result.setPathToBibliographicIndex( this.getIndexPath( compassBibliographic ) );
            else
                result.setPathToBibliographicIndex( null );

        }
        if ( taskCommand.isIndexBioSequence() ) {
            if ( this.rebuildIndex( biosequenceGps, "Biosequence Reference Index" ) )
                result.setPathToBiosequenceIndex( this.getIndexPath( compassBiosequence ) );
            else
                result.setPathToBiosequenceIndex( null );
        }

        if ( taskCommand.isIndexProbe() ) {
            if ( this.rebuildIndex( probeGps, "Probe Reference Index" ) )
                result.setPathToProbeIndex( this.getIndexPath( compassProbe ) );
            else
                result.setPathToProbeIndex( null );
        }

        if ( taskCommand.isIndexGene() ) {
            if ( this.rebuildIndex( geneGps, "Gene index" ) )
                result.setPathToGeneIndex( this.getIndexPath( compassGene ) );
            else
                result.setPathToGeneIndex( null );
        }

        if ( taskCommand.isIndexGeneSet() ) {
            if ( this.rebuildIndex( geneSetGps, "Gene set index" ) )
                result.setPathToGeneSetIndex( this.getIndexPath( compassGeneSet ) );
            else
                result.setPathToGeneSetIndex( null );
        }

        if ( taskCommand.isIndexExperimentSet() ) {
            if ( this.rebuildIndex( experimentSetGps, "Experiment set index" ) )
                result.setPathToExperimentSetIndex( this.getIndexPath( compassExperimentSet ) );
            else
                result.setPathToExperimentSetIndex( null );
        }

        if ( taskCommand.isIndexOntologies() ) {
            ontologyService.reindexAllOntologies();
        }

        log.info( "Indexing Finished" );

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
     * @param biosequenceGps the bioSequenceGps to set
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
        return compass.getSettings().getSetting( IndexerTaskImpl.PATH_PROPERTY )
                .replaceFirst( IndexerTaskImpl.FILE, "" ) + IndexerTaskImpl.PATH_SUFFIX;
    }

    private Boolean rebuildIndex( CompassGpsInterfaceDevice device, String whatIndexingMsg ) {

        StopWatch timer = new StopWatch();
        timer.start();
        log.info( "Rebuilding " + whatIndexingMsg + ". First attempt." );

        Boolean success = CompassUtils.rebuildCompassIndex( device );

        // First attempt failed. Wait a bit then re-try.
        // See bug#2031. There are intermittent indexing failures. The cause is unknown at the moment.
        if ( !success ) {
            log.warn( "Failed to index " + whatIndexingMsg + ". Trying it again..." );
            try {
                Thread.sleep( 120000 ); // sleep for 2 minutes.
            } catch ( InterruptedException e ) {
                log.warn( "Job to index" + whatIndexingMsg + " was interrupted." );
                return false;
            }

            timer.reset();
            timer.start();
            log.info( "Rebuilding " + whatIndexingMsg + ". Second attempt." );
            success = CompassUtils.rebuildCompassIndex( device );
        }

        // If failed for the second time send an email to administrator.
        if ( !success ) {
            mailEngine.sendAdminMessage( "Failed to index " + whatIndexingMsg,
                    "Failed to index " + whatIndexingMsg + ".  See logs for details" );
            log.info( "Failed rebuilding index for " + whatIndexingMsg + ".  Took (ms): " + timer.getTime() );

        } else {
            log.info( "Finished rebuilding " + whatIndexingMsg + ".  Took (ms): " + timer.getTime() );
        }

        return success;

    }

}
