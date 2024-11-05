package ubic.gemma.core.apps;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentAggregatorService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentSplitService;
import ubic.gemma.persistence.service.expression.experiment.UnsupportedScaleTypeForAggregationException;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class SingleCellDataAggregatorCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private AggregatorHelperService helperService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Nullable
    private String ctaName;
    private boolean makePreferred;

    public SingleCellDataAggregatorCli() {
        super( SingleCellExpressionDataVector.class );
        setUsePreferredQuantitationType();
    }

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
    protected void buildExperimentVectorsOptions( Options options ) {
        options.addOption( "cta", "cell-type-assignment", true, "Name of the cell type assignment to use (defaults to the preferred one)" );
        options.addOption( "p", "make-preferred", false, "Make the resulting aggregated data the preferred raw data for the experiment" );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) {
        ctaName = commandLine.getOptionValue( "cta" );
        makePreferred = commandLine.hasOption( "p" );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment expressionExperiment, QuantitationType qt ) {
        log.info( "Splitting single cell data into pseudo-bulks for: " + expressionExperiment + " and " + qt );

        CellTypeAssignment cta;
        if ( ctaName != null ) {
            cta = entityLocator.locateCellTypeAssignment( expressionExperiment, qt, ctaName );
        } else {
            cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( expressionExperiment, qt )
                    .orElseThrow( () -> new IllegalStateException( expressionExperiment + " does not have a preferred cell-type assignment for " + qt + "." ) );
        }

        QuantitationType newQt;
        try {
            newQt = helperService.splitAndAggregate( expressionExperiment, qt, cta, makePreferred );
            addSuccessObject( expressionExperiment, "Aggregated single-cell data into " + newQt + "." );
        } catch ( UnsupportedScaleTypeForAggregationException e ) {
            addErrorObject( expressionExperiment, String.format( "Aggregation is not support for data of scale type %s, change it first in the GUI %s.",
                    qt.getScale(), entityUrlBuilder.fromHostUrl().entity( expressionExperiment ).web().edit().toUriString() ), e );
            return;
        }

        // create/recreate processed vectors
        if ( newQt.getIsPreferred() ) {
            log.info( "Creating a data file for " + newQt + "..." );
            try ( ExpressionDataFileService.LockedPath lockedFile = expressionDataFileService.writeOrLocateRawExpressionDataFile( expressionExperiment, newQt, true ) ) {
                addSuccessObject( expressionExperiment, "Created a data file for " + newQt + ": " + lockedFile.getPath() );
            } catch ( IOException e ) {
                addErrorObject( expressionExperiment, "Failed to generate a data file for " + newQt + ".", e );
            }

            log.info( "Reprocessing experiment since a new set of raw data vectors was added or replaced..." );
            try {
                preprocessorService.process( expressionExperiment );
                addSuccessObject( expressionExperiment, "Post-processed data from " + newQt + "." );
            } catch ( Exception e ) {
                addErrorObject( expressionExperiment, "Failed to post-process the data from " + newQt + ".", e );
            }
        }
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

        @Autowired
        private EntityUrlBuilder entityUrlBuilder;

        @Transactional
        public QuantitationType splitAndAggregate( ExpressionExperiment expressionExperiment, QuantitationType qt, CellTypeAssignment cta, boolean makePreferred ) {
            List<ExpressionExperimentSubSet> subsets = singleCellExpressionExperimentSplitService.splitByCellType( expressionExperiment, cta );
            int longestSubsetName = subsets.stream().map( ExpressionExperimentSubSet::getName ).mapToInt( String::length ).max().orElse( 0 );
            log.info( String.format( "Created %d subsets of %s for each cell type:\n\t%s", subsets.size(), expressionExperiment,
                    subsets.stream().map( subset -> StringUtils.rightPad( subset.getName(), longestSubsetName ) + "\t" + entityUrlBuilder.fromHostUrl().entity( subset ).web().toUri() ).collect( Collectors.joining( "\n\t" ) ) ) );

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
