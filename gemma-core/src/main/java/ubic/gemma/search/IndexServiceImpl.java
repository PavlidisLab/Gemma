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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.grid.javaspaces.AbstractSpacesProgressService;

import ubic.gemma.grid.javaspaces.task.index.IndexerResult;
import ubic.gemma.grid.javaspaces.task.index.IndexerTask;
import ubic.gemma.grid.javaspaces.task.index.IndexerTaskCommand;
import ubic.gemma.grid.javaspaces.util.SpacesEnum;
import ubic.gemma.util.CompassUtils;
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
public class IndexServiceImpl extends AbstractSpacesProgressService implements IndexService {

    /*
     * Note: IndexService has not been configured with annotations because we it needs to reside in
     * applicationContext-search.xml so we can choose whether or not to load this part of the application context at
     * spring startup. FIXME: will this work with @Secured on the interface?
     */

    /**
     * This inner class is used for creating a seperate thread that will delete the compass ee index
     * 
     * @author klc
     * @version $Id$
     */
    class IndexJob extends BackgroundProgressJob<ModelAndView> {

        private String description;

        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public IndexJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        public ModelAndView call() throws Exception {

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), "Attempting to index" );

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

        protected void index( IndexerTaskCommand c ) {

            if ( c.isIndexAD() ) indexArrayDesigns();
            if ( c.isIndexBibRef() ) indexBibligraphicReferences();
            if ( c.isIndexEE() ) indexExpressionExperiments();
            if ( c.isIndexGene() ) indexGenes();
            if ( c.isIndexProbe() ) indexProbes();
            if ( c.isIndexBioSequence() ) indexBioSequences();

        }
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
        public IndexInSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

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

    private CompassGpsInterfaceDevice arrayGps;

    private CompassGpsInterfaceDevice bibliographicGps;

    private CompassGpsInterfaceDevice biosequenceGps;

    private InternalCompass compassArray;

    private InternalCompass compassBibliographic;

    private InternalCompass compassBiosequence;

    private InternalCompass compassExpression;

    private InternalCompass compassGene;

    private InternalCompass compassProbe;

    private CompassGpsInterfaceDevice expressionGps;

    private CompassGpsInterfaceDevice geneGps;

    private CompassGpsInterfaceDevice probeGps;

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexAll()
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

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexArrayDesigns()
     */
    public void indexArrayDesigns() {
        CompassUtils.rebuildCompassIndex( arrayGps );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexBibligraphicReferences()
     */
    public void indexBibligraphicReferences() {
        CompassUtils.rebuildCompassIndex( bibliographicGps );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexBioSequences()
     */
    public void indexBioSequences() {
        CompassUtils.rebuildCompassIndex( biosequenceGps );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexExpressionExperiments()
     */
    public void indexExpressionExperiments() {
        CompassUtils.rebuildCompassIndex( expressionGps );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexGenes()
     */
    public void indexGenes() {
        CompassUtils.rebuildCompassIndex( geneGps );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexProbes()
     */
    public void indexProbes() {
        CompassUtils.rebuildCompassIndex( probeGps );
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
    public void replaceBibliographicIndex( String pathToNewIndex ) throws IOException {
        CompassUtils.swapCompassIndex( compassBibliographic, pathToNewIndex );
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
     * @return Used by quartz to start the index process in a space
     */
    public String runAll() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setAll( true );
        return run( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), IndexerTask.class.getName(), false );
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

    @Override
    protected BackgroundProgressJob<ModelAndView> getRunner( String taskId, Object command ) {
        return new IndexJob( taskId, command );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController#getSpaceRunner(java.lang.String,
     * org.springframework.security.core.context.SecurityContext, javax.servlet.http.HttpServletRequest,
     * java.lang.Object, ubic.gemma.web.util.MessageUtil)
     */
    @Override
    protected BackgroundProgressJob<ModelAndView> getSpaceRunner( String taskId, Object command ) {
        return new IndexInSpaceJob( taskId, command );
    }

}
