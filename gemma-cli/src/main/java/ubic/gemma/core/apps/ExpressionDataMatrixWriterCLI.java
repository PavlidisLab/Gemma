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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.util.OptionsUtils;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.core.util.OptionsUtils.addEnumOption;

/**
 * Prints preferred data matrix to a file.
 *
 * @author Paul
 */
public class ExpressionDataMatrixWriterCLI extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionDataFileService fs;

    @Nullable
    private ScaleType scaleType;
    @Nullable
    private Path outputFile;
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
        options.addOption( Option.builder( "o" ).longOpt( "outputFileName" ).desc( "File name. If omitted, the file name will be based on the short name of the experiment." ).argName( "filename" ).hasArg().type( Path.class ).build() );
        options.addOption( "filter", "Filter expression matrix under default parameters" );
        addEnumOption( options, "scaleType", "scale-type", "Scale type to use for the output", ScaleType.class );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        scaleType = OptionsUtils.getEnumOptionValue( commandLine, "scaleType", ScaleType.class );
        outputFile = commandLine.getParsedOptionValue( 'o' );
        filter = commandLine.hasOption( "filter" );
    }

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        if ( outputFile != null ) {
            throw new IllegalArgumentException( "Output file name can only be used for single experiment output" );
        }
        super.processBioAssaySets( expressionExperiments );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        Path fileName;
        if ( outputFile != null ) {
            fileName = outputFile;
        } else {
            fileName = Paths.get( ExpressionDataFileUtils.getDataOutputFilename( ee, filter, ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX ) );
        }
        try ( Writer writer = new OutputStreamWriter( openOutputFile( fileName, isForce() ), StandardCharsets.UTF_8 ) ) {
            int written = fs.writeProcessedExpressionData( ee, filter, scaleType, writer );
            addSuccessObject( ee, "Wrote " + written + " vectors to " + fileName + "." );
        } catch ( IOException | FilteringException e ) {
            throw new RuntimeException( e );
        }
    }

    private OutputStream openOutputFile( Path fileName, boolean overwriteExisting ) throws IOException {
        if ( !overwriteExisting && Files.exists( fileName ) ) {
            throw new RuntimeException( fileName + " already exists, use -force/--force to override." );
        }
        if ( fileName.toString().endsWith( ".gz" ) ) {
            return new GZIPOutputStream( Files.newOutputStream( fileName ) );
        } else {
            return Files.newOutputStream( fileName );
        }
    }
}
