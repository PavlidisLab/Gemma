package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.options.ExpressionDataFileOptionValue;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.getDataOutputFilename;

public class RawExpressionDataWriterCli extends ExpressionExperimentVectorsManipulatingCli<RawExpressionDataVector> {

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Nullable
    private String[] samples;

    private ExpressionDataFileOptionValue destination;

    public RawExpressionDataWriterCli() {
        super( RawExpressionDataVector.class );
        setDefaultToPreferredQuantitationType();
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
        addExpressionDataFileOptions( options, "raw data", true );
        addSingleExperimentOption( options, Option.builder( "samples" ).longOpt( "samples" ).hasArg().valueSeparator( ',' ).desc( "List of sample identifiers to slice. This is incompatible with -standardLocation/--standard-location." ).build() );
        addForceOption( options );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        this.samples = commandLine.getOptionValues( "samples" );
        this.destination = getExpressionDataFileOptionValue( commandLine, true );
        if ( this.destination.isStandardLocation() && this.samples != null ) {
            throw new ParseException( "Cannot use -samples with -standardLocation." );
        }
        if ( this.destination.isStandardLocation() && this.destination.getScaleType() != null ) {
            throw new ParseException( "Cannot use -scaleType with -standardLocation." );
        }
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        int written;
        Path fileName;
        if ( samples != null ) {
            List<BioAssay> assays = Arrays.stream( samples )
                    .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                    .collect( Collectors.toList() );
            if ( destination.isStandardLocation() ) {
                throw new UnsupportedOperationException( "Writing raw data for specific samples to the standard location is not supported." );
            } else if ( destination.isStandardOutput() ) {
                fileName = null;
                try ( Writer writer = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                    written = expressionDataFileService.writeRawExpressionData( ee, assays, qt, destination.getScaleType(), destination.isExcludeSampleIdentifiers(), destination.isUseBioAssayIds(), destination.isUseRawColumnNames(), writer, true );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else {
                fileName = destination.getOutputFile( getDataOutputFilename( ee, assays, qt, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
                try ( Writer writer = openOutputFile( fileName ) ) {
                    written = expressionDataFileService.writeRawExpressionData( ee, assays, qt, destination.getScaleType(), destination.isExcludeSampleIdentifiers(), destination.isUseBioAssayIds(), destination.isUseRawColumnNames(), writer, true );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }
        } else {
            if ( destination.isStandardLocation() ) {
                try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateRawExpressionDataFile( ee, qt, isForce() ) ) {
                    fileName = lockedPath.getPath();
                    written = 0;
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else if ( destination.isStandardOutput() ) {
                fileName = null;
                try ( Writer writer = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                    written = expressionDataFileService.writeRawExpressionData( ee, qt, destination.getScaleType(), destination.isExcludeSampleIdentifiers(), destination.isUseBioAssayIds(), destination.isUseRawColumnNames(), writer, true );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else {
                fileName = destination.getOutputFile( getDataOutputFilename( ee, qt, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
                try ( Writer writer = openOutputFile( fileName ) ) {
                    written = expressionDataFileService.writeRawExpressionData( ee, qt, destination.getScaleType(), destination.isExcludeSampleIdentifiers(), destination.isUseBioAssayIds(), destination.isUseRawColumnNames(), writer, true );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }
        }
        addSuccessObject( ee, qt, String.format( "Wrote%s raw vectors%s for %s.",
                written > 0 ? " " + written : "",
                fileName != null ? " to " + fileName : "",
                samples != null ? " for the following assays: " + String.join( ", ", samples ) : ""
        ) );
    }

    private Writer openOutputFile( Path fileName ) throws IOException {
        if ( fileName.toString().endsWith( ".gz" ) ) {
            return new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( fileName ) ), StandardCharsets.UTF_8 );
        } else {
            return Files.newBufferedWriter( fileName );
        }
    }
}
