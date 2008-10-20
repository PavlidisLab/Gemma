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
package ubic.gemma.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.compass.core.spi.InternalCompass;
import org.compass.gps.spi.CompassGpsInterfaceDevice;
import org.compass.spring.web.mvc.CompassIndexResults;
import org.springframework.security.context.SecurityContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.grid.javaspaces.AbstractSpacesProgressService;

import ubic.gemma.grid.javaspaces.index.IndexerResult;
import ubic.gemma.grid.javaspaces.index.IndexerTask;
import ubic.gemma.grid.javaspaces.index.IndexerTaskCommand;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.BackgroundProgressJob;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * Services for updating the search indexes.
 * 
 * @author keshav
 * @version $Id$
 */
public class IndexService extends AbstractSpacesProgressService {

    /*
     * Note: IndexService has not been configured with xdoclet tags because we it needs to reside in
     * applicationContext-search.xml so we can choose whether or not to load this part of the application context at
     * spring startup. Note: three inherited dependencies from AbstractSpacesProgressService: TaskRunningService,
     * SpacesUtil, manualAuthenticationProcessing
     */

    private CompassGpsInterfaceDevice expressionGps;

    private CompassGpsInterfaceDevice geneGps;

    private CompassGpsInterfaceDevice arrayGps;

    private CompassGpsInterfaceDevice probeGps;

    private CompassGpsInterfaceDevice biosequenceGps;

    private CompassGpsInterfaceDevice bibliographicGps;

    private InternalCompass compassExpression;

    private InternalCompass compassGene;

    private InternalCompass compassArray;

    private InternalCompass compassProbe;

    private InternalCompass compassBiosequence;

    private InternalCompass compassBibliographic;

    /**
     * Indexes expression experiments, genes, array designs, probes and bibliographic references. This is a convenience
     * method for Quartz to schedule indexing of the entire database.
     */
    public void indexAll() {
        log.debug( "rebuilding compass index" );
        CompassUtils.rebuildCompassIndex( expressionGps );
        CompassUtils.rebuildCompassIndex( geneGps );
        CompassUtils.rebuildCompassIndex( arrayGps );
        CompassUtils.rebuildCompassIndex( probeGps );
        CompassUtils.rebuildCompassIndex( biosequenceGps );
        CompassUtils.rebuildCompassIndex( bibliographicGps );
    }

    /**
     * @return Used by quartz to start the index process in a space
     */
    public String runAll() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setAll( true );
        return run( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), IndexerTask.class.getName(), false );
    }

    /**
     * Indexes array designs.
     */
    public void indexArrayDesigns() {
        CompassUtils.rebuildCompassIndex( arrayGps );
    }

    /**
     * Indexes bibliographic references.
     */
    public void indexBibligraphicReferences() {
        CompassUtils.rebuildCompassIndex( bibliographicGps );
    }

    /**
     * Indexes sequences
     */
    public void indexBioSequences() {
        CompassUtils.rebuildCompassIndex( biosequenceGps );
    }

    /**
     * Indexes expression experiments.
     */
    public void indexExpressionExperiments() {
        CompassUtils.rebuildCompassIndex( expressionGps );
    }

    /**
     * Indexes genes.
     */
    public void indexGenes() {
        CompassUtils.rebuildCompassIndex( geneGps );
    }

    /**
     * Indexes probes.
     */
    public void indexProbes() {
        CompassUtils.rebuildCompassIndex( probeGps );
    }

    /**
     * @param pathToNewIndex
     * @throws IOException
     */
    public void replaceExperimentIndex( String pathToNewIndex ) throws IOException {
        CompassUtils.swapCompassIndex( compassExpression, pathToNewIndex );
    }

    /**
     * @param pathToNewIndex
     * @throws IOException
     */
    public void replaceGeneIndex( String pathToNewIndex ) throws IOException {
        CompassUtils.swapCompassIndex( compassGene, pathToNewIndex );
    }

    /**
     * @param pathToNewIndex
     * @throws IOException
     */
    public void replaceProbeIndex( String pathToNewIndex ) throws IOException {
        CompassUtils.swapCompassIndex( compassProbe, pathToNewIndex );
    }

    /**
     * @param pathToNewIndex
     * @throws IOException
     */
    public void replaceArrayIndex( String pathToNewIndex ) throws IOException {
        CompassUtils.swapCompassIndex( compassArray, pathToNewIndex );
    }

    /**
     * @param pathToNewIndex
     * @throws IOException
     */
    public void replaceBiosequenceIndex( String pathToNewIndex ) throws IOException {
        CompassUtils.swapCompassIndex( compassBiosequence, pathToNewIndex );
    }

    /**
     * @param pathToNewIndex
     * @throws IOException
     */
    public void replaceBibliographicIndex( String pathToNewIndex ) throws IOException {
        CompassUtils.swapCompassIndex( compassBibliographic, pathToNewIndex );
    }

    @Override
    protected BackgroundProgressJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
            Object command ) {

        return new IndexJob( taskId, securityContext, command );
    }

    /**
     * Starts the job on a compute server resource if the space is running and the task can be serviced. If runInWebapp
     * is true, the task will be run in the webapp virtual machine. If false the task will only be run if the space is
     * started and workers that can service the task exist.
     * 
     * @param command
     * @param spaceUrl
     * @param taskName
     * @param runInWebapp
     * @return {@link ModelAndView}
     */
    @Override
    public synchronized ModelAndView startJob( Object command, String spaceUrl, String taskName, boolean runInWebapp ) {
        String taskId = run( command, spaceUrl, taskName, runInWebapp );

        ModelAndView mnv = new ModelAndView( new RedirectView( "/Gemma/processProgress.html?taskid=" + taskId ) );
        mnv.addObject( TaskRunningService.JOB_ATTRIBUTE, taskId );
        return mnv;
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController#getSpaceRunner(java.lang.String,
     * org.springframework.security.context.SecurityContext, javax.servlet.http.HttpServletRequest, java.lang.Object,
     * ubic.gemma.web.util.MessageUtil)
     */
    @Override
    protected BackgroundProgressJob<ModelAndView> getSpaceRunner( String taskId, SecurityContext securityContext,
            Object command ) {
        return new IndexInSpaceJob( taskId, securityContext, command );
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author Paul
     * @version $Id$
     */
    private class IndexInSpaceJob extends IndexJob {

        final IndexerTask indexGemmaTaskProxy = ( IndexerTask ) updatedContext.getBean( "proxy" );

        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public IndexInSpaceJob( String taskId, SecurityContext parentSecurityContext, Object commandObj ) {
            super( taskId, parentSecurityContext, commandObj );

        }

        @Override
        protected void index( IndexerTaskCommand indexCommand ) {
            log.info( "Submitting job to space" );
            indexCommand.setTaskId( this.taskId );

            IndexerResult result = ( IndexerResult ) indexGemmaTaskProxy.execute( indexCommand );

            try {
                if ( indexCommand.isIndexGene() ) {
                    replaceGeneIndex( result.getPathToGeneIndex() );
                }
                if ( indexCommand.isIndexEE() ) {
                    replaceExperimentIndex( result.getPathToExpressionIndex() );
                }
                if ( indexCommand.isIndexAD() ) {
                    replaceArrayIndex( result.getPathToArrayIndex() );
                }
                if ( indexCommand.isIndexBibRef() ) {
                    replaceBibliographicIndex( result.getPathToBibliographicIndex() );
                }
                if ( indexCommand.isIndexProbe() ) {
                    replaceProbeIndex( result.getPathToProbeIndex() );
                }
                if ( indexCommand.isIndexBioSequence() ) {
                    replaceBiosequenceIndex( result.getPathToBiosequenceIndex() );
                }

            } catch ( IOException ioe ) {
                log.error( "Unable to swap indexes. " + ioe );
            }
        }
    }

    /**
     * @author klc
     * @version $Id$ This inner class is used for creating a
     *          seperate thread that will delete the compass ee index
     */
    class IndexJob extends BackgroundProgressJob<ModelAndView> {

        private String description;

        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public IndexJob( String taskId, SecurityContext parentSecurityContext, Object commandObj ) {
            super( taskId, parentSecurityContext, commandObj );

        }

        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Attempting to index" );

            long time = System.currentTimeMillis();

            log.info( "Preparing to rebuild selected indexes" );

            IndexerTaskCommand indexGemmaCommand = ( ( IndexerTaskCommand ) command );

            index( indexGemmaCommand );

            time = System.currentTimeMillis() - time;
            CompassIndexResults indexResults = new CompassIndexResults( time );
            Map<Object, Object> data = new HashMap<Object, Object>();
            data.put( "indexResults", indexResults );

            ProgressManager.destroyProgressJob( job );

            ModelAndView mv = new ModelAndView( "indexer" );
            mv.addObject( "time", time );
            mv.addObject( "description", this.description );

            return mv;

        }

        protected void index( IndexerTaskCommand command ) {

            if ( command.isIndexAD() ) indexArrayDesigns();
            if ( command.isIndexBibRef() ) indexBibligraphicReferences();
            if ( command.isIndexEE() ) indexExpressionExperiments();
            if ( command.isIndexGene() ) indexGenes();
            if ( command.isIndexProbe() ) indexProbes();
            if ( command.isIndexBioSequence() ) indexBioSequences();

        }
    }

    /**
     * @param arrayGps The arrayGps to set.
     */
    public void setArrayGps( CompassGpsInterfaceDevice arrayGps ) {
        this.arrayGps = arrayGps;
    }

    public void setBibliographicGps( CompassGpsInterfaceDevice bibliographicGps ) {
        this.bibliographicGps = bibliographicGps;
    }

    /**
     * @param expressionGps The expressionGps to set.
     */
    public void setExpressionGps( CompassGpsInterfaceDevice expressionGps ) {
        this.expressionGps = expressionGps;
    }

    /**
     * @param geneGps The geneGps to set.
     */
    public void setGeneGps( CompassGpsInterfaceDevice geneGps ) {
        this.geneGps = geneGps;
    }

    /**
     * @param probeGps
     */
    public void setProbeGps( CompassGpsInterfaceDevice probeGps ) {
        this.probeGps = probeGps;
    }

    /**
     * @param biosequenceGps
     */
    public void setBiosequenceGps( CompassGpsInterfaceDevice biosequenceGps ) {
        this.biosequenceGps = biosequenceGps;
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

}
