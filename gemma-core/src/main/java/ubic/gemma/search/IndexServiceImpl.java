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

import org.compass.core.spi.InternalCompass;

import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.maintenance.IndexerResult;
import ubic.gemma.tasks.maintenance.IndexerTask;
import ubic.gemma.tasks.maintenance.IndexerTaskCommand;
import ubic.gemma.util.CompassUtils;

/**
 * Services for updating the search indexes.
 * 
 * @author keshav
 * @version $Id$
 */
public class IndexServiceImpl extends AbstractTaskService implements IndexService {

    /*
     * NOTE not configured using annotations because they get confused by the interfaces here.
     */

    /**
     * Used for in-process.
     * 
     * @author klc
     * @version $Id$
     */
    class IndexJob extends BackgroundJob<IndexerTaskCommand> {

        /**
         * @param commandObj
         */
        public IndexJob( IndexerTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return indexerTask.execute( this.command );
            /*
             * In-process we don't need to swap the indexes afterwards.
             */
        }
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author Paul
     * @version $Id$
     */
    private class IndexInSpaceJob extends BackgroundJob<IndexerTaskCommand> {

        final IndexerTask indexGemmaTaskProxy = ( IndexerTask ) getProxy();

        public IndexInSpaceJob( IndexerTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        protected TaskResult processJob() {
            IndexerResult result = indexGemmaTaskProxy.execute( this.command );

            /*
             * When the rebuild is done in another JVM, in the client the index must be 'swapped' to refer to the new
             * one.
             */

            try {
                if ( this.command.isIndexGene() )
                    CompassUtils.swapCompassIndex( compassGene, result.getPathToGeneIndex() );
                if ( this.command.isIndexEE() )
                    CompassUtils.swapCompassIndex( compassExpression, result.getPathToGeneIndex() );
                if ( this.command.isIndexAD() )
                    CompassUtils.swapCompassIndex( compassArray, result.getPathToGeneIndex() );
                if ( this.command.isIndexBibRef() )
                    CompassUtils.swapCompassIndex( compassBibliographic, result.getPathToGeneIndex() );
                if ( this.command.isIndexBioSequence() )
                    CompassUtils.swapCompassIndex( compassBiosequence, result.getPathToGeneIndex() );
                if ( this.command.isIndexProbe() )
                    CompassUtils.swapCompassIndex( compassProbe, result.getPathToGeneIndex() );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

            return result;

        }
    }

    private IndexerTask indexerTask;

    private InternalCompass compassArray;

    private InternalCompass compassBibliographic;

    private InternalCompass compassBiosequence;

    private InternalCompass compassExpression;

    private InternalCompass compassGene;

    private InternalCompass compassProbe;

    public IndexServiceImpl() {
        this.setBusinessInterface( IndexerTask.class );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#index(ubic.gemma.grid.javaspaces.task.index.IndexerTaskCommand)
     */
    public String index( IndexerTaskCommand command ) {
        return this.run( command );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexAll()
     */
    public String indexAll() {
        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setAll( true );
        return this.run( c );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexArrayDesigns()
     */
    public String indexArrayDesigns() {
        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexAD( true );
        return this.run( c );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexBibligraphicReferences()
     */
    public String indexBibligraphicReferences() {
        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexBibRef( true );
        return this.run( c );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexBioSequences()
     */
    public String indexBioSequences() {
        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexBioSequence( true );
        return this.run( c );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexExpressionExperiments()
     */
    public String indexExpressionExperiments() {
        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexEE( true );
        return this.run( c );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexGenes()
     */
    public String indexGenes() {
        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexGene( true );
        return this.run( c );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.search.IndexService#indexProbes()
     */
    public String indexProbes() {
        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexProbe( true );
        return this.run( c );
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
     * @param indexerTask the indexerTask to set
     */
    public void setIndexerTask( IndexerTask indexerTask ) {
        this.indexerTask = indexerTask;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesProgressService#getRunner(ubic.gemma.grid.javaspaces.TaskCommand)
     */
    @Override
    protected BackgroundJob<IndexerTaskCommand> getInProcessRunner( TaskCommand command ) {
        return new IndexJob( ( IndexerTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController#getSpaceRunner(java.lang.String,
     * org.springframework.security.core.context.SecurityContext, javax.servlet.http.HttpServletRequest,
     * java.lang.Object, ubic.gemma.web.util.MessageUtil)
     */
    @Override
    protected BackgroundJob<IndexerTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new IndexInSpaceJob( ( IndexerTaskCommand ) command );
    }

}
