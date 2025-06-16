package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.LockedPath;
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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.getDataOutputFilename;

public class RawExpressionDataWriterCli extends ExpressionExperimentVectorsManipulatingCli<RawExpressionDataVector> {

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private FileLockManager fileLockManager;

    @Nullable
    private String[] samples;

    @Nullable
    private ScaleType scaleType;

    private ExpressionDataFileResult result;

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
        addExpressionDataFileOptions( options, "raw data", true, true );
        addSingleExperimentOption( options, Option.builder( "samples" ).longOpt( "samples" ).hasArg().valueSeparator( ',' ).desc( "List of sample identifiers to slice. This is incompatible with -standardLocation/--standard-location." ).build() );
        OptionsUtils.addEnumOption( options, "scaleType", "scale-type", "Scale type to use for the data. This is incompatible with -standardLocation/--standard-location.", ScaleType.class );
        addForceOption( options );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        this.result = getExpressionDataFileResult( commandLine );
        this.samples = commandLine.getOptionValues( "samples" );
        this.scaleType = OptionsUtils.getEnumOptionValue( commandLine, "scaleType" );
        if ( this.result.isStandardLocation() && this.samples != null ) {
            throw new ParseException( "Cannot use -samples with -standardLocation." );
        }
        if ( this.result.isStandardLocation() && this.scaleType != null ) {
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
            if ( result.isStandardLocation() ) {
                throw new UnsupportedOperationException( "Writing raw data for specific samples to the standard location is not supported." );
            } else if ( result.isStandardOutput() ) {
                fileName = null;
                try ( Writer writer = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                    written = expressionDataFileService.writeRawExpressionData( ee, assays, qt, scaleType, writer, true );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else {
                fileName = result.getOutputFile( getDataOutputFilename( ee, assays, qt, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
                try ( Writer writer = openOutputFile( fileName ) ) {
                    written = expressionDataFileService.writeRawExpressionData( ee, assays, qt, scaleType, writer, true );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }
        } else {
            if ( result.isStandardLocation() ) {
                try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateRawExpressionDataFile( ee, qt, isForce() ) ) {
                    fileName = lockedPath.getPath();
                    written = 0;
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else if ( result.isStandardOutput() ) {
                fileName = null;
                try ( Writer writer = new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) ) {
                    written = expressionDataFileService.writeRawExpressionData( ee, qt, scaleType, writer, true );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else {
                fileName = result.getOutputFile( getDataOutputFilename( ee, qt, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
                try ( Writer writer = openOutputFile( fileName ) ) {
                    written = expressionDataFileService.writeRawExpressionData( ee, qt, scaleType, writer, true );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }
        }
        addSuccessObject( ee, String.format( "Wrote%s raw vectors%s for %s%s.",
                written > 0 ? " " + written : "",
                fileName != null ? " to " + fileName : "",
                qt,
                samples != null ? " for the following assays: " + String.join( ", ", samples ) : ""
        ) );
    }

    private Writer openOutputFile( Path fileName ) throws IOException {
        if ( fileName.toString().endsWith( ".gz" ) ) {
            return new OutputStreamWriter( new GZIPOutputStream( fileLockManager.newOutputStream( fileName ) ), StandardCharsets.UTF_8 );
        } else {
            return fileLockManager.newBufferedWriter( fileName );
        }
    }
}
