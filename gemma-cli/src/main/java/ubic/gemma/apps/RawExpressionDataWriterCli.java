package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

public class RawExpressionDataWriterCli extends ExpressionExperimentVectorsManipulatingCli<RawExpressionDataVector> {

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Nullable
    private ScaleType scaleType;

    @Nullable
    private Path outputFile;

    public RawExpressionDataWriterCli() {
        super( RawExpressionDataVector.class );
        setUsePreferredQuantitationType();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "getRawDataMatrix";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Write raw data matrix to a; gene information is included if available.";
    }

    @Override
    protected void buildExperimentVectorsOptions( Options options ) {
        options.addOption( Option.builder( "o" ).longOpt( "output-file" ).hasArg().type( Path.class ).build() );
        OptionsUtils.addEnumOption( options, "scaleType", "scale-type", "Scale type to use for the data.", ScaleType.class );
        addForceOption( options );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        this.scaleType = OptionsUtils.getEnumOptionValue( commandLine, "scaleType" );
        this.outputFile = commandLine.getParsedOptionValue( "o" );
    }

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        if ( outputFile != null ) {
            throw new IllegalArgumentException( "The -o/--output-file option cannot be used with multiple experiments." );
        }
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        Path f;
        if ( outputFile != null ) {
            f = outputFile;
        } else {
            f = Paths.get( ExpressionDataFileUtils.getDataOutputFilename( ee, qt, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
        }
        if ( !isForce() && Files.exists( f ) ) {
            throw new RuntimeException( "Output file " + f + " already exists, use -force to overwrite." );
        }
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( f ) ), StandardCharsets.UTF_8 ) ) {
            int written = expressionDataFileService.writeRawExpressionData( ee, qt, scaleType, writer, true );
            addSuccessObject( ee, "Wrote " + written + " vectors for " + qt + " to " + f + "." );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
