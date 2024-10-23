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
import org.springframework.beans.factory.annotation.Value;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.datastructure.matrix.io.ExperimentalDesignWriter;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Writes out the experimental design for a given experiment. This can be directly read into R.
 *
 * @author keshav
 */
public class ExperimentalDesignWriterCLI extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private BuildInfo buildInfo;

    @Value("${gemma.hosturl}")
    private String gemmaHostUrl;

    private String outFileName;

    @Override
    public String getCommandName() {
        return "printExperimentalDesign";
    }

    @Override
    public String getShortDesc() {
        return "Prints experimental design to a file in a R-friendly format";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        Option outputFileOption = Option.builder( "o" ).hasArg().required().argName( "outFilePrefix" )
                .desc( "File prefix for saving the output (short name will be appended)" )
                .longOpt( "outFilePrefix" ).build();
        options.addOption( outputFileOption );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        outFileName = commandLine.getOptionValue( 'o' );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter( gemmaHostUrl, buildInfo );
        try ( PrintWriter writer = new PrintWriter( outFileName + "_" + FileTools.cleanForFileName( ee.getShortName() ) + ".txt" ) ) {
            edWriter.write( writer, ee, true );
            writer.flush();
        } catch ( IOException exception ) {
            throw new RuntimeException( exception );
        }
    }
}
