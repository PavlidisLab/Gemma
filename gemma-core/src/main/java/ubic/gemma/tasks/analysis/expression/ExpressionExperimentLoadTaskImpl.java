package ubic.gemma.tasks.analysis.expression;

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

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author keshav
 * @version $Id$
 */
@Component
@Scope("prototype")
public class ExpressionExperimentLoadTaskImpl implements ExpressionExperimentLoadTask {

    private Log log = LogFactory.getLog( this.getClass().getName() );

    @Autowired
    private GeoService geoDatasetService;
    @Autowired
    private PreprocessorService preprocessorService;

    private ExpressionExperimentLoadTaskCommand command;

    @Override
    public void setCommand( ExpressionExperimentLoadTaskCommand command ) {
        this.command = command;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#execute(java.lang.Object)
     */
    @Override
    public TaskResult execute() {
        ExpressionExperimentLoadTaskCommand jsEeLoadCommand = command;

        String accession = jsEeLoadCommand.getAccession();
        boolean loadPlatformOnly = jsEeLoadCommand.isLoadPlatformOnly();
        boolean doSampleMatching = !jsEeLoadCommand.isSuppressMatching();
        boolean aggressiveQtRemoval = jsEeLoadCommand.isAggressiveQtRemoval();
        boolean splitByPlatform = jsEeLoadCommand.isSplitByPlatform();
        boolean allowSuperSeriesLoad = jsEeLoadCommand.isAllowSuperSeriesLoad();
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

            try {
                preprocessorService.process( ee );
            } catch ( PreprocessingException e ) {
                log.error( "Error during postprocessing", e );
            }

        }
    }

}
