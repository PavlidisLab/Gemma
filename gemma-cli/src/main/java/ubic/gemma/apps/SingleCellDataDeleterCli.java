package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.DataDeleterService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class SingleCellDataDeleterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    private static final String
            DELETE_CELL_TYPE_ASSIGNMENT = "deleteCta",
            DELETE_ALL_CELL_TYPE_ASSIGNMENT = "deleteCtas",
            DELETE_CELL_LEVEL_CHARACTERISTICS = "deleteClc",
            DELETE_ALL_CELL_LEVEL_CHARACTERISTICS = "deleteClcs";

    @Autowired
    private DataDeleterService dataDeleterService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    private String ctaIdentifier;
    private String clcIdentifier;

    enum Mode {
        DELETE_ALL,
        DELETE_ALL_CELL_TYPE_ASSIGNMENTS,
        DELETE_CELL_TYPE_ASSIGNMENT,
        DELETE_ALL_CELL_LEVEL_CHARACTERISTICS,
        DELETE_CELL_LEVEL_CHARACTERISTICS
    }

    private Mode mode;

    public SingleCellDataDeleterCli() {
        super( SingleCellExpressionDataVector.class );
        setDefaultToPreferredQuantitationType();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "deleteSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Delete single-cell data and any related data files";
    }

    @Override
    protected void buildExperimentVectorsOptions( Options options ) {
        options.addOption( DELETE_CELL_TYPE_ASSIGNMENT, "delete-cell-type-assignment", true, "Delete a cell type assignment." );
        options.addOption( DELETE_ALL_CELL_TYPE_ASSIGNMENT, "delete-all-cell-type-assignment", true, "Delete all cell type assignments." );
        options.addOption( DELETE_CELL_LEVEL_CHARACTERISTICS, "delete-cell-level-characteristics", true, "Delete cell-level characteristics" );
        options.addOption( DELETE_ALL_CELL_LEVEL_CHARACTERISTICS, "delete-all-cell-level-characteristics", true, "Delete all cell-level characteristics" );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( DELETE_CELL_TYPE_ASSIGNMENT ) ) {
            mode = Mode.DELETE_CELL_TYPE_ASSIGNMENT;
            ctaIdentifier = commandLine.getOptionValue( DELETE_CELL_TYPE_ASSIGNMENT );
        } else if ( commandLine.hasOption( DELETE_ALL_CELL_TYPE_ASSIGNMENT ) ) {
            mode = Mode.DELETE_ALL_CELL_TYPE_ASSIGNMENTS;
        } else if ( commandLine.hasOption( DELETE_CELL_LEVEL_CHARACTERISTICS ) ) {
            mode = Mode.DELETE_CELL_LEVEL_CHARACTERISTICS;
            clcIdentifier = commandLine.getOptionValue( DELETE_CELL_LEVEL_CHARACTERISTICS );
        } else if ( commandLine.hasOption( DELETE_ALL_CELL_LEVEL_CHARACTERISTICS ) ) {
            mode = Mode.DELETE_ALL_CELL_LEVEL_CHARACTERISTICS;
        } else {
            mode = Mode.DELETE_ALL;
        }
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        switch ( mode ) {
            case DELETE_ALL:
                dataDeleterService.deleteSingleCellData( ee, qt );
                addSuccessObject( ee, qt, "Deleted single-cell data." );
                break;
            case DELETE_ALL_CELL_TYPE_ASSIGNMENTS:
                ee = eeService.thawLite( ee );
                singleCellExpressionExperimentService.removeAllCellTypeAssignments( ee, qt );
                addSuccessObject( ee, qt, "Deleted all cell type assignments." );
                break;
            case DELETE_CELL_TYPE_ASSIGNMENT:
                ee = eeService.thawLite( ee );
                CellTypeAssignment cta = entityLocator.locateCellTypeAssignment( ee, qt, ctaIdentifier );
                singleCellExpressionExperimentService.removeCellTypeAssignment( ee, qt, cta );
                addSuccessObject( ee, qt, "Deleted cell type assignment: " + cta + "." );
                break;
            case DELETE_ALL_CELL_LEVEL_CHARACTERISTICS:
                singleCellExpressionExperimentService.removeAllCellLevelCharacteristics( ee, qt );
                addSuccessObject( ee, qt, "Deleted all cell-level characteristics." );
                break;
            case DELETE_CELL_LEVEL_CHARACTERISTICS:
                ee = eeService.thawLite( ee );
                CellLevelCharacteristics clc = entityLocator.locateCellLevelCharacteristics( ee, qt, clcIdentifier );
                singleCellExpressionExperimentService.removeCellLevelCharacteristics( ee, qt, clc );
                addSuccessObject( ee, qt, "Deleted cell-level characteristics: " + clc + "." );
                break;
            default:
                throw new UnsupportedOperationException( "Unsupported mode: " + mode + "." );
        }
    }
}
