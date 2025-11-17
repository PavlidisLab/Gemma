package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;

import static ubic.gemma.cli.util.OptionsUtils.getAutoOptionValue;

public class SingleCellDataUpdaterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    private static final String
            REPLACE_CELL_TYPE_FACTOR_OPTION = "replaceCtf",
            KEEP_CELL_TYPE_FACTOR_OPTION = "keepCtf";

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Nullable
    private String preferredCtaIdentifier;
    private boolean clearPreferredCta;

    @Nullable
    private Boolean replaceCellTypeFactor;

    public SingleCellDataUpdaterCli() {
        super( SingleCellExpressionDataVector.class );
        setDefaultToPreferredQuantitationType();
    }

    @Override
    public String getCommandName() {
        return "updateSingleCellData";
    }

    @Override
    protected void buildExperimentVectorsOptions( Options options ) {
        addSingleExperimentOption( options, "preferredCta", "preferred-cell-type-assignment", true, "Change the preferred cell type assignment." );
        options.addOption( "clearPreferredCta", "clear-preferred-cell-type-assignment", false, "Clear the preferred cell type assignment." );
        OptionsUtils.addAutoOption( options,
                REPLACE_CELL_TYPE_FACTOR_OPTION, "replace-cell-type-factor", "Replace the existing cell type factor even if is compatible with the new preferred cell type assignment. If no cell type factor exists, it will be created.",
                KEEP_CELL_TYPE_FACTOR_OPTION, "keep-cell-type-factor", "Keep the existing cell type factor as-is even if it becomes misaligned with the preferred cell type assignment. If no cell type factor exists, it will be created.",
                "The default is to re-create the cell type factor if necessary." );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        this.preferredCtaIdentifier = commandLine.getOptionValue( "preferredCta" );
        this.clearPreferredCta = commandLine.hasOption( "clearPreferredCta" );
        this.replaceCellTypeFactor = getAutoOptionValue( commandLine, REPLACE_CELL_TYPE_FACTOR_OPTION, KEEP_CELL_TYPE_FACTOR_OPTION );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) throws Exception {
        if ( preferredCtaIdentifier != null ) {
            CellTypeAssignment preferredCta = entityLocator.locateCellTypeAssignment( ee, qt, preferredCtaIdentifier );
            singleCellExpressionExperimentService.changePreferredCellTypeAssignment( ee, qt, preferredCta,
                    replaceCellTypeFactor == null || replaceCellTypeFactor,
                    replaceCellTypeFactor != null && replaceCellTypeFactor );
        } else if ( clearPreferredCta ) {
            singleCellExpressionExperimentService.clearPreferredCellTypeAssignment( ee, qt );
        }
    }
}
