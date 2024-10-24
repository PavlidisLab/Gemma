package ubic.gemma.core.apps;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentAggregatorService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentSplitService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class SingleCellDataAggregatorCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private AggregatorHelperService helperService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Nullable
    private String qtName;
    @Nullable
    private String ctaName;
    private boolean makePreferred;

    @Nullable
    @Override
    public String getCommandName() {
        return "aggregateSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Aggregate single cell data into pseudo-bulks";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addOption( "qt", "quantitation-type", true, "Identifier of the single-cell quantitation type to use (defaults to the preferred one)" );
        options.addOption( "cta", "cell-type-assignment", true, "Name of the cell type assignment to use (defaults to the preferred one)" );
        options.addOption( "p", "make-preferred", false, "Make the resulting aggregated data the preferred raw data for the experiment" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        qtName = commandLine.getOptionValue( "qt" );
        ctaName = commandLine.getOptionValue( "cta" );
        makePreferred = commandLine.hasOption( "p" );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        log.info( "Splitting single cell data into pseudo-bulks for: " + expressionExperiment );
        QuantitationType qt;
        if ( qtName != null ) {
            qt = entityLocator.locateQuantitationType( expressionExperiment, qtName, SingleCellExpressionDataVector.class );
        } else {
            qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( expressionExperiment )
                    .orElseThrow( () -> new IllegalStateException( expressionExperiment + " does not have a preferred set of single-cell vectors." ) );
        }
        CellTypeAssignment cta;
        if ( ctaName != null ) {
            cta = entityLocator.locateCellTypeAssignment( expressionExperiment, qt, ctaName );
        } else {
            cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( expressionExperiment, qt )
                    .orElseThrow( () -> new IllegalStateException( expressionExperiment + " does not have a preferred cell-type assignment for " + qt + "." ) );
        }

        QuantitationType newQt = helperService.splitAndAggregate( expressionExperiment, qt, cta, makePreferred );

        // create/recreate processed vectors
        if ( newQt.getIsPreferred() ) {
            log.info( "Reprocessing experiment since a new set of raw data vectors was added or replaced..." );
            preprocessorService.process( expressionExperiment );
        }

        addSuccessObject( expressionExperiment );
    }

    /**
     * Simple service to split and aggregate in a transaction.
     */
    @Service
    public static class AggregatorHelperService {

        @Autowired
        private SingleCellExpressionExperimentSplitService singleCellExpressionExperimentSplitService;

        @Autowired
        private SingleCellExpressionExperimentAggregatorService singleCellExpressionExperimentAggregatorService;

        @Transactional
        public QuantitationType splitAndAggregate( ExpressionExperiment expressionExperiment, QuantitationType qt, CellTypeAssignment cta, boolean makePreferred ) {
            List<ExpressionExperimentSubSet> subsets = singleCellExpressionExperimentSplitService.splitByCellType( expressionExperiment, cta );
            log.info( String.format( "Created %d subsets of %s for each cell type:\n%s", subsets.size(), expressionExperiment,
                    subsets.stream().map( ExpressionExperimentSubSet::toString ).collect( Collectors.joining( "\n\t" ) ) ) );

            List<BioAssay> cellBAs = new ArrayList<>();
            for ( ExpressionExperimentSubSet subset : subsets ) {
                subset.getBioAssays().stream()
                        .sorted( Comparator.comparing( BioAssay::getName ) )
                        .forEach( cellBAs::add );
            }

            QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectors( expressionExperiment, qt, cellBAs, makePreferred );
            log.info( "Aggregated single-cell data for " + qt + " into pseudo-bulks with quantitation type " + newQt + "." );
            return newQt;
        }
    }
}
