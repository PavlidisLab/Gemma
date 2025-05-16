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
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class RawExpressionDataWriterCli extends ExpressionExperimentVectorsManipulatingCli<RawExpressionDataVector> {

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Nullable
    private String[] samples;

    @Nullable
    private ScaleType scaleType;

    /**
     * Write the file to standard output.
     */
    private boolean standardOutput;

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
        addSingleExperimentOption( options, Option.builder( "o" ).longOpt( "output-file" ).hasArg().type( Path.class ).build() );
        addSingleExperimentOption( options, "stdout", "stdout", false, "Write to the standard output." );
        addSingleExperimentOption( options, Option.builder( "samples" ).longOpt( "samples" ).hasArg().valueSeparator( ',' ).desc( "List of sample identifiers to slice." ).build() );
        OptionsUtils.addEnumOption( options, "scaleType", "scale-type", "Scale type to use for the data.", ScaleType.class );
        addForceOption( options );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        this.samples = commandLine.getOptionValues( "samples" );
        this.scaleType = OptionsUtils.getEnumOptionValue( commandLine, "scaleType" );
        this.standardOutput = commandLine.hasOption( "stdout" );
        this.outputFile = commandLine.getParsedOptionValue( "o" );
        if ( standardOutput && outputFile != null ) {
            throw new ParseException( "Cannot set both -stdout/--stdout and -o/--output-file" );
        }
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        int written;
        if ( samples != null ) {
            List<BioAssay> assays = Arrays.stream( samples )
                    .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                    .collect( Collectors.toList() );
            try ( Writer writer = openOutputFile( ee, assays, qt, isForce() ) ) {
                written = expressionDataFileService.writeRawExpressionData( ee, assays, qt, scaleType, writer, true );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else {
            try ( Writer writer = openOutputFile( ee, null, qt, isForce() ) ) {
                written = expressionDataFileService.writeRawExpressionData( ee, qt, scaleType, writer, true );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        addSuccessObject( ee, String.format( "Wrote%s raw vectors%s for %s%s.",
                written > 0 ? " " + written : "",
                fileName != null ? " to " + fileName : "",
                qt,
                samples != null ? " for the following assays: " + String.join( ", ", samples ) : ""
        ) );
    }

    @Nullable
    private Path fileName;

    private Writer openOutputFile( ExpressionExperiment ee, @Nullable List<BioAssay> assays, QuantitationType qt, boolean overwriteExisting ) throws IOException {
        if ( standardOutput ) {
            fileName = null;
            return new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 );
        }
        if ( outputFile != null ) {
            fileName = outputFile;
        } else {
            if ( assays != null ) {
                fileName = Paths.get( ExpressionDataFileUtils.getDataOutputFilename( ee, assays, qt, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
            } else {
                fileName = Paths.get( ExpressionDataFileUtils.getDataOutputFilename( ee, qt, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
            }
        }
        if ( !overwriteExisting && Files.exists( fileName ) ) {
            throw new RuntimeException( fileName + " already exists, use -force/--force to override." );
        }
        if ( fileName.toString().endsWith( ".gz" ) ) {
            return new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( fileName ) ), StandardCharsets.UTF_8 );
        } else {
            return Files.newBufferedWriter( fileName );
        }
    }
}
