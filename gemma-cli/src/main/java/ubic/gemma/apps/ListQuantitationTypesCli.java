package ubic.gemma.apps;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author poirigui
 */
public class ListQuantitationTypesCli extends ExpressionExperimentVectorsManipulatingCli<DataVector> {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    public ListQuantitationTypesCli() {
        super( DataVector.class );
        setUseReferencesIfPossible();
        setDefaultToAll();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "listQuantitationTypes";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "List the available quantitation types for an experiment.";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        getCliContext().getOutputStream().println( formatExperiment( expressionExperiment ) );
        super.processExpressionExperiment( expressionExperiment );
        getCliContext().getOutputStream().println();
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        getCliContext().getOutputStream().println( "\t" + qt );
        Class<? extends DataVector> vectorType = quantitationTypeService.getDataVectorType( qt );
        if ( vectorType != null ) {
            getCliContext().getOutputStream().println( "\t\tVector Type: " + vectorType.getSimpleName() );
        }
        if ( StringUtils.isNotBlank( qt.getDescription() ) ) {
            if ( qt.getDescription().contains( "\n" ) ) {
                getCliContext().getOutputStream().println( "\t\tDescription:\n\t\t" + qt.getDescription().replaceAll( "\n", "\n\t\t" ) );
            } else {
                getCliContext().getOutputStream().println( "\t\tDescription: " + qt.getDescription() );
            }
        }
        BioAssayDimension dimension;
        SingleCellDimension scd;
        SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig initializationConfig = SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig.builder()
                .includeBioAssays( true )
                .includeCtas( true )
                .includeClcs( true )
                .includeProtocol( true )
                .includeCharacteristics( true )
                .includeIndices( false )
                .build();
        if ( ( dimension = eeService.getBioAssayDimension( ee, qt ) ) != null ) {
            getCliContext().getOutputStream().println( "\t\t" + dimension );
        } else if ( ( scd = singleCellExpressionExperimentService.getSingleCellDimensionWithoutCellIds( ee, qt, initializationConfig ) ) != null ) {
            getCliContext().getOutputStream().println( "\t\t" + scd );
            try ( Stream<String> cellIds = singleCellExpressionExperimentService.streamCellIds( ee, qt, true ) ) {
                if ( cellIds != null ) {
                    getCliContext().getOutputStream().println( "\t\tCell IDs: " + cellIds.limit( 10 ).collect( Collectors.joining( ", " ) ) + ", ..." );
                }
            }
            if ( !scd.getCellTypeAssignments().isEmpty() ) {
                getCliContext().getOutputStream().println( "\t\tCell Type Assignments:" );
                for ( CellTypeAssignment cta : scd.getCellTypeAssignments() ) {
                    getCliContext().getOutputStream().println( "\t\t\t" + cta );
                    if ( cta.getProtocol() != null ) {
                        getCliContext().getOutputStream().println( "\t\t\t\tProtocol: " + cta.getProtocol().getName() );
                    }
                    getCliContext().getOutputStream().printf( "\t\t\t\tCell Types: %s, ...%n",
                            requireNonNull( singleCellExpressionExperimentService.streamCellTypes( ee, cta, true ) )
                                    .limit( 10 )
                                    .map( c -> c != null ? c.getValue() : "<unassigned>" )
                                    .collect( Collectors.joining( ", " ) ) );
                }
            }
            if ( !scd.getCellLevelCharacteristics().isEmpty() ) {
                getCliContext().getOutputStream().println( "\t\tCell-level Characteristics:" );
                for ( CellLevelCharacteristics clc : scd.getCellLevelCharacteristics() ) {
                    getCliContext().getOutputStream().println( "\t\t\t" + clc );
                    Category cat = singleCellExpressionExperimentService.getCellLevelCharacteristicsCategory( ee, clc );
                    if ( cat != null && cat.getCategory() != null ) {
                        getCliContext().getOutputStream().println( "\t\t\t\tCategory: " + cat.getCategory() );
                    } else {
                        getCliContext().getOutputStream().println( "\t\t\t\tCategory: <unassigned>" );
                    }
                    if ( clc.getNumberOfCharacteristics() <= 100 ) {
                        getCliContext().getOutputStream().printf( "\t\t\t\tCharacteristics: %s, ...%n",
                                requireNonNull( singleCellExpressionExperimentService.streamCellLevelCharacteristics( ee, clc, true ) )
                                        .limit( 10 )
                                        .map( c -> c != null ? c.getValue() : "<unassigned>" )
                                        .collect( Collectors.joining( ", " ) ) );

                    } else {
                        getCliContext().getOutputStream().printf( "\t\t\t\tToo many characteristics (%d), ignoring.%n", clc.getNumberOfCharacteristics() );
                    }
                }
            }
        }
    }
}
