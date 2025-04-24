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
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
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

import static ubic.gemma.cli.util.OptionsUtils.addEnumOption;

/**
 * Prints preferred data matrix to a file.
 *
 * @author Paul
 */
public class ExpressionDataMatrixWriterCLI extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionDataFileService fs;

    @Nullable
    private String[] samples;
    @Nullable
    private ScaleType scaleType;
    @Nullable
    private Path outputFile;
    /**
     * Write the file to standard output.
     */
    private boolean standardOutput;
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
        addSingleExperimentOption( options, Option.builder( "o" ).longOpt( "outputFileName" ).desc( "File name. If omitted, the file name will be based on the short name of the experiment." ).argName( "filename" ).hasArg().type( Path.class ).build() );
        addSingleExperimentOption( options, "stdout", "stdout", false, "Write to the standard output." );
        addSingleExperimentOption( options, Option.builder( "samples" ).longOpt( "samples" ).hasArg().valueSeparator( ',' ).desc( "List of sample identifiers to slice." ).build() );
        options.addOption( "filter", "Filter expression matrix under default parameters" );
        addEnumOption( options, "scaleType", "scale-type", "Scale type to use for the output", ScaleType.class );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        samples = commandLine.getOptionValues( "samples" );
        scaleType = OptionsUtils.getEnumOptionValue( commandLine, "scaleType" );
        standardOutput = commandLine.hasOption( "stdout" );
        outputFile = commandLine.getParsedOptionValue( 'o' );
        if ( standardOutput && outputFile != null ) {
            throw new ParseException( "Cannot set both -stdout/--stdout and -o/--outputFileName" );
        }
        filter = commandLine.hasOption( "filter" );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        int written;
        if ( samples != null ) {
            QuantitationType qt = eeService.getProcessedQuantitationType( ee )
                    .orElseThrow( () -> new IllegalStateException( ee + " does not have processed vectors." ) );
            List<BioAssay> assays = Arrays.stream( samples )
                    .map( sampleId -> entityLocator.locateBioAssay( ee, qt, sampleId ) )
                    .collect( Collectors.toList() );
            try ( Writer writer = openOutputFile( ee, assays, filter, isForce() ) ) {
                written = fs.writeProcessedExpressionData( ee, assays, filter, scaleType, writer, true );
            } catch ( IOException | FilteringException e ) {
                throw new RuntimeException( e );
            }
        } else {
            try ( Writer writer = openOutputFile( ee, null, filter, isForce() ) ) {
                written = fs.writeProcessedExpressionData( ee, filter, scaleType, writer, true );
            } catch ( IOException | FilteringException e ) {
                throw new RuntimeException( e );
            }
        }
        addSuccessObject( ee, String.format( "Wrote%s processed vectors%s%s.",
                written > 0 ? " " + written : "",
                fileName != null ? " to " + fileName : "",
                samples != null ? " for the following assays: " + String.join( ", ", samples ) : ""
        ) );
    }

    @Nullable
    private Path fileName;

    private Writer openOutputFile( ExpressionExperiment ee, @Nullable List<BioAssay> assays, boolean filter, boolean overwriteExisting ) throws IOException {
        if ( standardOutput ) {
            fileName = null;
            return new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 );
        }
        if ( outputFile != null ) {
            fileName = outputFile;
        } else {
            if ( assays != null ) {
                fileName = Paths.get( ExpressionDataFileUtils.getDataOutputFilename( ee, assays, filter, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
            } else {
                fileName = Paths.get( ExpressionDataFileUtils.getDataOutputFilename( ee, filter, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
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
