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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.io.ExperimentalDesignWriter;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;

import static ubic.gemma.cli.util.OptionsUtils.formatOption;

/**
 * Writes out the experimental design for a given experiment. This can be directly read into R.
 *
 * @author keshav
 */
public class ExperimentalDesignWriterCLI extends ExpressionExperimentManipulatingCLI {

    private static final String
            STANDARD_LOCATION_OPTION = "standardLocation",
            OUT_FILE_PREFIX_OPTION = "o",
            USE_RAW_COLUMN_NAMES_OPTION = "useRawColumnNames";

    @Autowired
    private BuildInfo buildInfo;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    private boolean standardLocation;
    private boolean useRawColumnNames;
    @Nullable
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
    protected void buildExperimentOptions( Options options ) {
        options.addOption( STANDARD_LOCATION_OPTION, "standard-location", false, "Write the experimental design to the standard location." );
        options.addOption( OUT_FILE_PREFIX_OPTION, "outFilePrefix", true, "File prefix for saving the output (short name will be appended). This option is incompatible with " + formatOption( options, STANDARD_LOCATION_OPTION ) + "." );
        options.addOption( USE_RAW_COLUMN_NAMES_OPTION, "use-raw-column-names", false, "Use raw names for the columns, otherwise R-friendly names are used. This option is incompatible with " + formatOption( options, STANDARD_LOCATION_OPTION ) + "." );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        standardLocation = commandLine.hasOption( STANDARD_LOCATION_OPTION );
        outFileName = commandLine.getOptionValue( OUT_FILE_PREFIX_OPTION );
        useRawColumnNames = commandLine.hasOption( USE_RAW_COLUMN_NAMES_OPTION );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        ee = eeService.thawLite( ee );
        if ( standardLocation ) {
            try {
                expressionDataFileService.writeOrLocateDesignFile( ee, isForce() );
            } catch ( IOException e ) {
                addErrorObject( ee, e );
            }
        } else {
            try ( PrintWriter writer = new PrintWriter( ( outFileName != null ? outFileName + "_" : "" ) + FileTools.cleanForFileName( ee.getShortName() ) + ".txt" ) ) {
                ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter( entityUrlBuilder, buildInfo, true );
                edWriter.setUseRawColumnNames( useRawColumnNames );
                edWriter.write( ee, true, writer );
            } catch ( IOException e ) {
                addErrorObject( ee, e );
            }
        }
    }
}
