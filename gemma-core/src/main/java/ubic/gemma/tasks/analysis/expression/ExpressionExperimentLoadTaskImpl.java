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
package ubic.gemma.tasks.analysis.expression;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.preprocess.PreprocessorService;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
@Component
public class ExpressionExperimentLoadTaskImpl implements ExpressionExperimentLoadTask {
    private Log log = LogFactory.getLog( this.getClass().getName() );

    @Autowired
    private GeoService geoDatasetService = null;

    @Autowired
    private ArrayExpressLoadService arrayExpressLoadService;

    @Autowired
    private PreprocessorService preprocessorService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#execute(java.lang.Object)
     */
    @Override
    @TaskMethod
    public TaskResult execute( ExpressionExperimentLoadTaskCommand command ) {
        ExpressionExperimentLoadTaskCommand jsEeLoadCommand = command;

        String accession = jsEeLoadCommand.getAccession();
        boolean loadPlatformOnly = jsEeLoadCommand.isLoadPlatformOnly();
        boolean doSampleMatching = !jsEeLoadCommand.isSuppressMatching();
        boolean aggressiveQtRemoval = jsEeLoadCommand.isAggressiveQtRemoval();
        boolean splitByPlatform = jsEeLoadCommand.isSplitByPlatform();
        boolean allowSuperSeriesLoad = jsEeLoadCommand.isAllowSuperSeriesLoad();
        boolean allowSubSeriesLoad = true; // FIXME

        TaskResult result;
        if ( jsEeLoadCommand.isArrayExpress() ) {
            ExpressionExperiment dataset = arrayExpressLoadService.load( accession,
                    jsEeLoadCommand.getArrayDesignName(), jsEeLoadCommand.isAllowArrayExpressDesign() );
            ExpressionExperiment minimalDataset = null;
            if ( dataset != null ) {
                /* Don't send the full experiment to the space. Instead, create a minimal result. */
                minimalDataset = ExpressionExperiment.Factory.newInstance();
                minimalDataset.setId( dataset.getId() );
                minimalDataset.setName( dataset.getName() );
                minimalDataset.setDescription( dataset.getDescription() );
            }
            result = new TaskResult( command, minimalDataset );
        } else if ( loadPlatformOnly ) {
            Collection<ArrayDesign> arrayDesigns = ( Collection<ArrayDesign> ) geoDatasetService.fetchAndLoad(
                    accession, true, doSampleMatching, aggressiveQtRemoval, splitByPlatform );
            ArrayList<ArrayDesign> minimalDesigns = null;
            if ( arrayDesigns != null ) {
                /* Don't send the full array designs to space. Instead, create a minimal result. */
                minimalDesigns = new ArrayList<ArrayDesign>();
                for ( ArrayDesign ad : arrayDesigns ) {
                    ArrayDesign minimalDesign = ArrayDesign.Factory.newInstance();
                    minimalDesign.setId( ad.getId() );
                    minimalDesign.setName( ad.getName() );
                    minimalDesign.setDescription( ad.getDescription() );

                    minimalDesigns.add( minimalDesign );
                }
            }
            result = new TaskResult( command, minimalDesigns );
        } else {
            Collection<ExpressionExperiment> datasets = ( Collection<ExpressionExperiment> ) geoDatasetService
                    .fetchAndLoad( accession, loadPlatformOnly, doSampleMatching, aggressiveQtRemoval, splitByPlatform,
                            allowSuperSeriesLoad, allowSubSeriesLoad );

            if ( datasets == null || datasets.isEmpty() ) {
                // can happen with cancellation.
                throw new IllegalStateException( "Failed to load anything" );
            }

            log.info( "Loading done, starting postprocessing" );
            postProcess( datasets );

            /* Don't send the full experiments to space. Instead, create a minimal result. */
            ArrayList<ExpressionExperiment> minimalDatasets = new ArrayList<ExpressionExperiment>();
            for ( ExpressionExperiment ee : datasets ) {
                ExpressionExperiment minimalDataset = ExpressionExperiment.Factory.newInstance();
                minimalDataset.setId( ee.getId() );
                minimalDataset.setName( ee.getName() );
                minimalDataset.setDescription( ee.getDescription() );

                minimalDatasets.add( minimalDataset );
            }

            result = new TaskResult( command, minimalDatasets );
        }

        return result;
    }

    /**
     * Do missing value and processed vector creation steps.
     * 
     * @param ees
     */
    private void postProcess( Collection<ExpressionExperiment> ees ) {
        log.info( "Postprocessing ..." );
        for ( ExpressionExperiment ee : ees ) {

            preprocessorService.process( ee );
        }
    }

}