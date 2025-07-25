package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.visualization.cellbrowser.CellBrowserMetadataWriter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class CellLevelMetadataWriterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    private boolean useBioAssayIds;
    private boolean useRawColumnNames;

    public CellLevelMetadataWriterCli() {
        super( SingleCellExpressionDataVector.class );
        setSingleExperimentMode();
        setUsePreferredQuantitationType();
    }

    @Override
    public String getCommandName() {
        return "getSingleCellMetadata";
    }

    @Override
    protected void buildExperimentVectorsOptions( Options options ) {
        options.addOption( "useBioAssayIds", "use-bioassay-ids", false, "Use BioAssay IDs instead of their names." );
        options.addOption( "useRawColumnNames", "use-raw-column-names", false, "Use raw names for the columns, otherwise R-friendly names are used." );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) {
        useBioAssayIds = commandLine.hasOption( "useBioAssayIds" );
        useRawColumnNames = commandLine.hasOption( "useRawColumnNames" );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) throws IOException {
        ee = eeService.thawLite( ee );
        SingleCellDimension scd = singleCellExpressionExperimentService.getSingleCellDimensionWithAssaysAndCellLevelCharacteristics( ee, qt );
        if ( scd == null ) {
            addErrorObject( ee, "There is no single-cell dimension associated to " + qt + "." );
            return;
        }
        try ( Writer out = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
            CellBrowserMetadataWriter writer = new CellBrowserMetadataWriter();
            writer.setUseBioAssayIds( useBioAssayIds );
            writer.setUseRawColumnNames( useRawColumnNames );
            writer.setAutoFlush( true );
            writer.write( ee, scd, out );
        }
    }
}
