package ubic.gemma.core.tasks.analysis.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author keshav
 */
@Component
@Scope("prototype")
public class ExpressionExperimentLoadTaskImpl extends AbstractTask<ExpressionExperimentLoadTaskCommand>
        implements ExpressionExperimentLoadTask {

    private final Log log = LogFactory.getLog( this.getClass().getName() );

    @Autowired
    private GeoService geoDatasetService;
    @Autowired
    private PreprocessorService preprocessorService;

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult call() {
        String accession = taskCommand.getAccession();
        boolean loadPlatformOnly = taskCommand.isLoadPlatformOnly();
        boolean doSampleMatching = !taskCommand.isSuppressMatching();
        boolean splitByPlatform = taskCommand.isSplitByPlatform();
        boolean allowSuperSeriesLoad = taskCommand.isAllowSuperSeriesLoad();
        boolean allowSubSeriesLoad = true;

        TaskResult result;
        if ( loadPlatformOnly ) {
            Collection<ArrayDesign> arrayDesigns = ( Collection<ArrayDesign> ) geoDatasetService
                    .fetchAndLoad( accession, true, doSampleMatching, splitByPlatform );
            ArrayList<ArrayDesign> minimalDesigns = null;
            if ( arrayDesigns != null ) {
                /* Don't send the full array designs to space. Instead, create a minimal result. */
                minimalDesigns = new ArrayList<>();
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
            @SuppressWarnings("ConstantConditions") // Better readability
                    Collection<ExpressionExperiment> datasets = ( Collection<ExpressionExperiment> ) geoDatasetService
                    .fetchAndLoad( accession, loadPlatformOnly, doSampleMatching, splitByPlatform, allowSuperSeriesLoad,
                            allowSubSeriesLoad );

            if ( datasets == null || datasets.isEmpty() ) {
                // can happen with cancellation.
                throw new IllegalStateException( "Failed to load anything" );
            }

            log.info( "Loading done, starting postprocessing" );
            this.postProcess( datasets );

            /* Don't send the full experiments to space. Instead, create a minimal result. */
            ArrayList<ExpressionExperiment> minimalDatasets = new ArrayList<>();
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
     * @param ees experiments
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