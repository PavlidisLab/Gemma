package ubic.gemma.tasks.analysis.expression;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.preprocess.PreprocessingException;
import ubic.gemma.analysis.preprocess.PreprocessorService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.AbstractTask;

/**
 * @author keshav
 * @version $Id$
 */
@Component
@Scope("prototype")
public class ExpressionExperimentLoadTaskImpl extends AbstractTask<TaskResult, ExpressionExperimentLoadTaskCommand>
        implements ExpressionExperimentLoadTask {

    private Log log = LogFactory.getLog( this.getClass().getName() );

    @Autowired
    private GeoService geoDatasetService;
    @Autowired
    private PreprocessorService preprocessorService;

    @Override
    public TaskResult execute() {
        // ExpressionExperimentLoadTaskCommand taskCommand = this.taskCommand;

        String accession = taskCommand.getAccession();
        boolean loadPlatformOnly = taskCommand.isLoadPlatformOnly();
        boolean doSampleMatching = !taskCommand.isSuppressMatching();
        boolean aggressiveQtRemoval = taskCommand.isAggressiveQtRemoval();
        boolean splitByPlatform = taskCommand.isSplitByPlatform();
        boolean allowSuperSeriesLoad = taskCommand.isAllowSuperSeriesLoad();
        boolean allowSubSeriesLoad = true; // FIXME

        TaskResult result;
        if ( loadPlatformOnly ) {
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
            result = new TaskResult( this.taskCommand, minimalDesigns );
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

            result = new TaskResult( this.taskCommand, minimalDatasets );
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

            try {
                preprocessorService.process( ee );
            } catch ( PreprocessingException e ) {
                log.error( "Error during postprocessing", e );
            }

        }
    }

}

// private class LoadLocalJob extends AbstractTask<TaskResult, ExpressionExperimentLoadTaskCommand> {
//
// public LoadLocalJob( ExpressionExperimentLoadTaskCommand commandObj ) {
// super( commandObj );
// if ( geoDatasetService.getGeoDomainObjectGenerator() == null ) {
// geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
// }
// }
//
// @Override
// public TaskResult execute() {
//
// if ( taskCommand.isLoadPlatformOnly() ) {
// return processPlatformOnlyJob( taskCommand );
// } else if ( taskCommand.isArrayExpress() ) {
// throw new UnsupportedOperationException( "Array express no longer supported" );
// } else /* GEO */{
// return processGEODataJob( taskCommand );
// }
// }
//
// /**
// * @param arrayDesigns
// * @return
// */
// protected TaskResult processArrayDesignResult( Collection<ArrayDesign> arrayDesigns ) {
// Map<Object, Object> model = new HashMap<Object, Object>();
// model.put( "arrayDesigns", arrayDesigns );
//
// if ( arrayDesigns.size() == 1 ) {
// return new TaskResult( taskCommand, new ModelAndView( new RedirectView(
// "/Gemma/arrays/showArrayDesign.html?id=" + arrayDesigns.iterator().next().getId() ) ) );
// }
// String list = "";
// for ( ArrayDesign ad : arrayDesigns )
// list += ad.getId() + ",";
// return new TaskResult( taskCommand, new ModelAndView( new RedirectView(
// "/Gemma/arrays/showAllArrayDesigns.html?ids=" + list ) ) );
// }
//
// /**
// * @param result
// * @return
// */
// protected TaskResult processArrayExpressResult( ExpressionExperiment result ) {
// if ( result == null ) {
// throw new IllegalStateException( "Loading failed" );
// }
// Map<Object, Object> model = new HashMap<Object, Object>();
// model.put( "expressionExperiment", result );
// return new TaskResult( taskCommand, new ModelAndView( new RedirectView(
// "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + result.getId() ) ) );
// }
//
// protected TaskResult processGEODataJob( ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {
//
// String accession = getAccession( expressionExperimentLoadCommand );
// boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
// boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();
// boolean splitIncompatiblePlatforms = expressionExperimentLoadCommand.isSplitByPlatform();
// boolean allowSuperSeriesLoad = expressionExperimentLoadCommand.isAllowSuperSeriesLoad();
// boolean allowSubSeriesLoad = true; // FIXME
//
// Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) geoDatasetService
// .fetchAndLoad( accession, false, doSampleMatching, aggressiveQtRemoval, splitIncompatiblePlatforms,
// allowSuperSeriesLoad, allowSubSeriesLoad );
//
// if ( result == null ) {
// throw new RuntimeException( "No results were returned (cancelled or failed)" );
// }
//
// postProcess( result );
//
// return processGeoLoadResult( result );
// }
//
// protected TaskResult processGeoLoadResult( Collection<ExpressionExperiment> result ) {
// Map<Object, Object> model = new HashMap<Object, Object>();
// if ( result == null || result.size() == 0 ) {
// throw new RuntimeException( "No results were returned (cancelled or failed)" );
// }
// if ( result.size() == 1 ) {
// ExpressionExperiment loaded = result.iterator().next();
// model.put( "expressionExperiment", loaded );
// return new TaskResult( taskCommand, new ModelAndView( new RedirectView(
// "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
// + result.iterator().next().getId() ) ) );
// }
//
// String list = "";
// for ( ExpressionExperiment ee : result ) {
// list += ee.getId() + ",";
// }
// return new TaskResult( taskCommand, new ModelAndView( new RedirectView(
// "/Gemma/expressionExperiment/showAllExpressionExperiments.html?ids=" + list ) ) );
// }
//
// /**
// * For when we're only loading the platform.
// */
// protected TaskResult processPlatformOnlyJob( ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {
// String accession = getAccession( expressionExperimentLoadCommand );
//
// boolean doSampleMatching = !expressionExperimentLoadCommand.isSuppressMatching();
// boolean aggressiveQtRemoval = expressionExperimentLoadCommand.isAggressiveQtRemoval();
// Collection<ArrayDesign> arrayDesigns = ( Collection<ArrayDesign> ) geoDatasetService.fetchAndLoad(
// accession, true, doSampleMatching, aggressiveQtRemoval, false );
//
// return processArrayDesignResult( arrayDesigns );
// }
//
// /**
// * Clean up the access provided by the user.
// *
// * @param expressionExperimentLoadCommand
// * @return
// */
// private String getAccession( ExpressionExperimentLoadTaskCommand expressionExperimentLoadCommand ) {
// String accesionNum = expressionExperimentLoadCommand.getAccession();
// accesionNum = StringUtils.strip( accesionNum );
// accesionNum = StringUtils.upperCase( accesionNum );
// return accesionNum;
// }
//
// /**
// * Do missing value and processed vector creation steps.
// *
// * @param ees
// */
// private void postProcess( Collection<ExpressionExperiment> ees ) {
//
// if ( ees == null ) return;
//
// log.info( "Postprocessing ..." );
// for ( ExpressionExperiment ee : ees ) {
//
// try {
// preprocessorService.process( ee );
// } catch ( PreprocessingException e ) {
// log.error( "Error during postprocessing of " + ee, e );
// }
//
// }
// }
//
// }

// /**
// * Job that loads in a javaspace.
// *
// * @author Paul
// * @version $Id$
// */
// private class LoadRemoteJob extends LoadLocalJob {
//
// ExpressionExperimentLoadTask eeLoadTask;
//
// public LoadRemoteJob( ExpressionExperimentLoadTaskCommand commandObj ) {
// super( commandObj );
//
// }
//
// @Override
// protected TaskResult processGEODataJob( ExpressionExperimentLoadTaskCommand eeLoadCommand ) {
// TaskResult result = this.process( eeLoadCommand );
// return super.processGeoLoadResult( ( Collection<ExpressionExperiment> ) result.getAnswer() );
// }
//
// @Override
// protected TaskResult processPlatformOnlyJob( ExpressionExperimentLoadTaskCommand eeLoadCommand ) {
// TaskResult result = this.process( eeLoadCommand );
// return super.processArrayDesignResult( ( Collection<ArrayDesign> ) result.getAnswer() );
// }
//
// private TaskResult process( ExpressionExperimentLoadTaskCommand cmd ) {
// try {
// TaskResult result = eeLoadTask.execute();
// return result;
// } catch ( Exception e ) {
// throw new RuntimeException( e );
// }
// }
// }
