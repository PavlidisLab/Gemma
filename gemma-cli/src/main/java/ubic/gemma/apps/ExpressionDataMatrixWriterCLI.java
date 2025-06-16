/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
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

import static ubic.gemma.cli.util.OptionsUtils.addEnumOption;
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.getDataOutputFilename;

/**
 * Prints preferred data matrix to a file.
 *
 * @author Paul
 */
public class ExpressionDataMatrixWriterCLI extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionDataFileService fs;

    @Autowired
    private FileLockManager fileLockManager;

    @Nullable
    private String[] samples;
    @Nullable
    private ScaleType scaleType;
    private ExpressionDataFileResult result;
    private boolean filter;

    @Override
    public String getCommandName() {
        return "getDataMatrix";
    }

    @Override
    public String getShortDesc() {
        return "Write processed data matrix to a file; gene information is included if available.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        addExpressionDataFileOptions( options, "processed data", true, true );
        addSingleExperimentOption( options, Option.builder( "samples" ).longOpt( "samples" ).hasArg().valueSeparator( ',' ).desc( "List of sample identifiers to slice." ).build() );
        options.addOption( "filter", "Filter expression matrix under default parameters" );
        addEnumOption( options, "scaleType", "scale-type", "Scale type to use for the output. This is incompatible with -standardLocation/--standard-location.", ScaleType.class );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        result = getExpressionDataFileResult( commandLine );
        samples = commandLine.getOptionValues( "samples" );
        if ( result.isStandardLocation() && samples != null ) {
            throw new ParseException( "Cannot specify samples when writing to standard location." );
        }
        scaleType = OptionsUtils.getEnumOptionValue( commandLine, "scaleType" );
        if ( result.isStandardLocation() && scaleType != null ) {
            throw new ParseException( "Cannot specify scale type when writing to standard location." );
        }
        filter = commandLine.hasOption( "filter" );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        int written;
        Path fileName;
        if ( result.isStandardLocation() ) {
            try ( LockedPath lockedPath = fs.writeOrLocateProcessedDataFile( ee, filter, isForce() )
                    .orElseThrow( () -> new IllegalStateException( ee + " does not have processed vectors." ) ) ) {
                fileName = lockedPath.getPath();
                written = 0;
            } catch ( FilteringException | IOException e ) {
                throw new RuntimeException( e );
            }
        } else {
            if ( samples != null ) {
                QuantitationType qt = eeService.getProcessedQuantitationType( ee )
                        .orElseThrow( () -> new IllegalStateException( ee + " does not have processed vectors." ) );
                List<BioAssay> assays = Arrays.stream( samples )
                        .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                        .collect( Collectors.toList() );
                fileName = result.getOutputFile( getDataOutputFilename( ee, assays, filter, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
                try ( Writer writer = openOutputFile( fileName ) ) {
                    written = fs.writeProcessedExpressionData( ee, assays, filter, scaleType, writer, true );
                } catch ( IOException | FilteringException e ) {
                    throw new RuntimeException( e );
                }
            } else {
                fileName = result.getOutputFile( getDataOutputFilename( ee, filter, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
                try ( Writer writer = openOutputFile( fileName ) ) {
                    written = fs.writeProcessedExpressionData( ee, filter, scaleType, writer, true );
                } catch ( IOException | FilteringException e ) {
                    throw new RuntimeException( e );
                }
            }
        }
        addSuccessObject( ee, String.format( "Wrote%s processed vectors%s%s.",
                written > 0 ? " " + written : "",
                fileName != null ? " to " + fileName : "",
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
