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
import java.util.concurrent.ExecutionException;

import org.compass.core.spi.InternalCompass;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.SubmittedTask;
import ubic.gemma.job.TaskRunningService;
import ubic.gemma.tasks.maintenance.IndexerResult;
import ubic.gemma.tasks.maintenance.IndexerTaskCommand;
import ubic.gemma.util.CompassUtils;

/**
 * Services for updating the search indexes.
 * 
 * @author keshav
 * @version $Id$
 */
@Component
public class IndexServiceImpl implements IndexService {

    @Autowired private TaskRunningService taskRunningService;

    @Autowired @Qualifier("compassArray") private InternalCompass compassArray;
    @Autowired @Qualifier("compassBibliographic") private InternalCompass compassBibliographic;
    @Autowired @Qualifier("compassBiosequence") private InternalCompass compassBiosequence;
    @Autowired @Qualifier("compassExperimentSet") private InternalCompass compassExperimentSet;
    @Autowired @Qualifier("compassExpression") private InternalCompass compassExpression;
    @Autowired @Qualifier("compassGene") private InternalCompass compassGene;
    @Autowired @Qualifier("compassGeneSet") private InternalCompass compassGeneSet;
    @Autowired @Qualifier("compassProbe") private InternalCompass compassProbe;

    /*
     * NOTE not configured using annotations because they get confused by the interfaces here.
     */

    /**
     * Job that loads in a javaspace.
     *
     * @author Paul
     * @version $Id$
     */
    // This is started locally. Calls two job: ReIndex and SwapIndices if needed.
    private class IndexerJob extends BackgroundJob<IndexerTaskCommand, IndexerResult> {

        public IndexerJob( IndexerTaskCommand command ) {
            super( command );
        }

        @Override
        protected IndexerResult processJob() {
            IndexerResult result = null;
            String taskId = taskRunningService.submitRemoteTask( command );
            SubmittedTask<IndexerResult> indexingTask = taskRunningService.getSubmittedTask( taskId );
            try {
                result = indexingTask.getResult();
            } catch (ExecutionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            if ( indexingTask.isRunningRemotely() && result != null ) {
                loadExternalIndices( command, result );
            }

            return result;
        }
    }


    private void loadExternalIndices( IndexerTaskCommand indexerTaskCommand, IndexerResult remoteIndexTaskResult ) {
            /*
             * When the rebuild is done in another JVM, in the client the index must be 'swapped' to refer to the new
             * one.
             *
             * Put in multiple try catch blocks so that if one swapping fails they all don't fail :)
             */
        try {
            if ( indexerTaskCommand.isIndexGene() && remoteIndexTaskResult.getPathToGeneIndex() != null )
                CompassUtils.swapCompassIndex( compassGene, remoteIndexTaskResult.getPathToGeneIndex() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        try {
            if ( indexerTaskCommand.isIndexEE() && remoteIndexTaskResult.getPathToExpressionIndex() != null )
                CompassUtils.swapCompassIndex( compassExpression, remoteIndexTaskResult.getPathToExpressionIndex() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        try {
            if ( indexerTaskCommand.isIndexAD() && remoteIndexTaskResult.getPathToArrayIndex() != null )
                CompassUtils.swapCompassIndex( compassArray, remoteIndexTaskResult.getPathToArrayIndex() );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        try {
            if ( indexerTaskCommand.isIndexBibRef() && remoteIndexTaskResult.getPathToBibliographicIndex() != null )
                CompassUtils.swapCompassIndex( compassBibliographic, remoteIndexTaskResult.getPathToBibliographicIndex() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        try {
            if ( indexerTaskCommand.isIndexBioSequence() && remoteIndexTaskResult.getPathToBiosequenceIndex() != null )
                CompassUtils.swapCompassIndex( compassBiosequence, remoteIndexTaskResult.getPathToBiosequenceIndex() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        try {
            if ( indexerTaskCommand.isIndexProbe() && remoteIndexTaskResult.getPathToProbeIndex() != null )
                CompassUtils.swapCompassIndex( compassProbe, remoteIndexTaskResult.getPathToProbeIndex() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        try {
            if ( indexerTaskCommand.isIndexGeneSet() && remoteIndexTaskResult.getPathToGeneSetIndex() != null )
                CompassUtils.swapCompassIndex( compassGeneSet, remoteIndexTaskResult.getPathToGeneSetIndex() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        try {
            if ( indexerTaskCommand.isIndexExperimentSet() && remoteIndexTaskResult.getPathToExperimentSetIndex() != null )
                CompassUtils.swapCompassIndex( compassExperimentSet, remoteIndexTaskResult.getPathToExperimentSetIndex() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param compassGeneSet the compassGeneSet to set
     */
//    public void setCompassGeneSet( InternalCompass compassGeneSet ) {
//        this.compassGeneSet = compassGeneSet;
//    }
//
//    /**
//     * @param compassExperimentSet the compassExperimentSet to set
//     */
//    public void setCompassExperimentSet( InternalCompass compassExperimentSet ) {
//        this.compassExperimentSet = compassExperimentSet;
//    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.IndexService#index(ubic.gemma.grid.javaspaces.task.index.IndexerTaskCommand)
     */
    @Override
    public String index( IndexerTaskCommand command ) {
        return taskRunningService.submitLocalJob( new IndexerJob( command ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.IndexService#indexAll()
     */
    @Override
    public String indexAll() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setAll( true );
        return taskRunningService.submitLocalJob( new IndexerJob( command ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.IndexService#indexArrayDesigns()
     */
    @Override
    public String indexArrayDesigns() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setIndexAD( true );
        return taskRunningService.submitLocalJob( new IndexerJob( command ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.IndexService#indexBibligraphicReferences()
     */
    @Override
    public String indexBibligraphicReferences() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setIndexBibRef( true );
        return taskRunningService.submitLocalJob( new IndexerJob( command ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.IndexService#indexBioSequences()
     */
    @Override
    public String indexBioSequences() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setIndexBioSequence( true );
        return taskRunningService.submitLocalJob( new IndexerJob( command ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.IndexService#indexExpressionExperiments()
     */
    @Override
    public String indexExpressionExperiments() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setIndexEE( true );
        return taskRunningService.submitLocalJob( new IndexerJob( command ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.IndexService#indexGenes()
     */
    @Override
    public String indexGenes() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setIndexGene( true );
        return taskRunningService.submitLocalJob( new IndexerJob( command ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.IndexService#indexProbes()
     */
    @Override
    public String indexProbes() {
        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setIndexProbe( true );
        return taskRunningService.submitLocalJob( new IndexerJob( command ) );
    }

//    /**
//     * @param compassArray the compassArray to set
//     */
//    public void setCompassArray( InternalCompass compassArray ) {
//        this.compassArray = compassArray;
//    }
//
//    /**
//     * @param compassBibliographic the compassBibliographic to set
//     */
//    public void setCompassBibliographic( InternalCompass compassBibliographic ) {
//        this.compassBibliographic = compassBibliographic;
//    }
//
//    /**
//     * @param compassBiosequence the compassBiosequence to set
//     */
//    public void setCompassBiosequence( InternalCompass compassBiosequence ) {
//        this.compassBiosequence = compassBiosequence;
//    }
//
//    /**
//     * @param compassExpression the compassExpression to set
//     */
//    public void setCompassExpression( InternalCompass compassExpression ) {
//        this.compassExpression = compassExpression;
//    }
//
//    /**
//     * @param compassGene the compassGene to set
//     */
//    public void setCompassGene( InternalCompass compassGene ) {
//        this.compassGene = compassGene;
//    }
//
//    /**
//     * @param compassProbe the compassProbe to set
//     */
//    public void setCompassProbe( InternalCompass compassProbe ) {
//        this.compassProbe = compassProbe;
//    }
//
//    /**
//     * @param indexerTask the indexerTask to set
//     */
//    public void setIndexerTask( IndexerTask indexerTask ) {
//        this.indexerTask = indexerTask;
//    }
}
