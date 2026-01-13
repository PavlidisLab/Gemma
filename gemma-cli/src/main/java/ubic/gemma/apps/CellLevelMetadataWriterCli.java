package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.options.CellMetadataFileOptionValue;
import ubic.gemma.core.visualization.cellbrowser.CellBrowserMetadataWriter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.CELL_BROWSER_SC_METADATA_SUFFIX;
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.getMetadataOutputFilename;

public class CellLevelMetadataWriterCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    private CellMetadataFileOptionValue destination;

    public CellLevelMetadataWriterCli() {
        super( SingleCellExpressionDataVector.class );
        setSingleExperimentMode();
        setDefaultToPreferredQuantitationType();
    }

    @Override
    public String getCommandName() {
        return "getSingleCellMetadata";
    }

    @Override
    protected void buildExperimentVectorsOptions( Options options ) {
        addCellMetadataFileOptions( options, "cell-level metadata", false );
        addForceOption( options );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        destination = getCellMetadataFileOptionValue( commandLine, false );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) throws IOException {
        ee = eeService.thawLite( ee );
        SingleCellDimension scd = singleCellExpressionExperimentService.getSingleCellDimensionWithAssaysAndCellLevelCharacteristics( ee, qt );
        if ( scd == null ) {
            addErrorObject( ee, qt, "There is no single-cell dimension associated to the quantitation type." );
            return;
        }
        Path dest;
        if ( destination.isStandardLocation() ) {
            throw new UnsupportedOperationException( "Cell Browser-compatible metadata cannot be written to the standard location." );
        } else if ( destination.isStandardOutput() ) {
            dest = null;
            try ( Writer out = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                CellBrowserMetadataWriter writer = new CellBrowserMetadataWriter();
                writer.setSeparateSampleFromAssayIdentifiers( destination.isSeparateSampleFromAssaysIdentifiers() );
                writer.setUseBioAssayIds( destination.isUseBioAssayIds() );
                writer.setUseRawColumnNames( destination.isUseRawColumnNames() );
                writer.setAutoFlush( true );
                writer.write( ee, scd, out );
            }
        } else {
            dest = destination.getOutputFile( getMetadataOutputFilename( ee, qt, CELL_BROWSER_SC_METADATA_SUFFIX ) );
            try ( Writer out = new OutputStreamWriter( openOutputFile( dest ), StandardCharsets.UTF_8 ) ) {
                CellBrowserMetadataWriter writer = new CellBrowserMetadataWriter();
                writer.setSeparateSampleFromAssayIdentifiers( destination.isSeparateSampleFromAssaysIdentifiers() );
                writer.setUseBioAssayIds( destination.isUseBioAssayIds() );
                writer.setUseRawColumnNames( destination.isUseRawColumnNames() );
                writer.setAutoFlush( true );
                writer.write( ee, scd, out );
            }
        }
        addSuccessObject( ee, qt, "Wrote single-cell metadata to " + ( dest != null ? dest : "the standard output" ) + "." );
    }

    private OutputStream openOutputFile( Path fileName ) throws IOException {
        if ( fileName.toString().endsWith( ".gz" ) ) {
            return new GZIPOutputStream( Files.newOutputStream( fileName ) );
        } else {
            return Files.newOutputStream( fileName );
        }
    }
}
