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

import static ubic.gemma.cli.util.OptionsUtils.*;

/**
 * @author poirigui
 */
public class SingleCellDataDeleterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    private static final String
            DELETE_CELL_TYPE_ASSIGNMENT = "deleteCta",
            DELETE_CELL_LEVEL_CHARACTERISTICS = "deleteClc";

    @Autowired
    private DataDeleterService dataDeleterService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    private String ctaIdentifier;
    private String clcIdentifier;

    enum Mode {
        DELETE_ALL,
        DELETE_CELL_TYPE_ASSIGNMENT,
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
        options.addOption( DELETE_CELL_LEVEL_CHARACTERISTICS, "delete-cell-level-characteristics", true, "Delete cell-level characteristics" );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        if ( hasOption( commandLine, DELETE_CELL_TYPE_ASSIGNMENT, requires( toBeUnset( DELETE_CELL_LEVEL_CHARACTERISTICS ) ) ) ) {
            mode = Mode.DELETE_CELL_TYPE_ASSIGNMENT;
            ctaIdentifier = commandLine.getOptionValue( DELETE_CELL_TYPE_ASSIGNMENT );
        } else if ( hasOption( commandLine, DELETE_CELL_LEVEL_CHARACTERISTICS, requires( toBeUnset( DELETE_CELL_TYPE_ASSIGNMENT ) ) ) ) {
            mode = Mode.DELETE_CELL_LEVEL_CHARACTERISTICS;
            clcIdentifier = commandLine.getOptionValue( DELETE_CELL_LEVEL_CHARACTERISTICS );
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
            case DELETE_CELL_TYPE_ASSIGNMENT:
                ee = eeService.thawLite( ee );
                CellTypeAssignment cta = entityLocator.locateCellTypeAssignment( ee, qt, ctaIdentifier );
                singleCellExpressionExperimentService.removeCellTypeAssignment( ee, qt, cta );
                addSuccessObject( ee, qt, "Deleted cell type assignment: " + cta + "." );
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
