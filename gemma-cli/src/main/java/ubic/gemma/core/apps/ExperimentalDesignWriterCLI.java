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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.file.PathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.ExperimentalDesignWriter;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Writes out the experimental design for a given experiment. This can be directly read into R.
 *
 * @author keshav
 */
public class ExperimentalDesignWriterCLI extends ExpressionExperimentManipulatingCLI {

    private static final String
            STANDARD_LOCATION_OPTION = "standardLocation",
            OUT_FILE_PREFIX_OPTION = "o";

    private boolean standardLocation;
    @Nullable
    private String outFileNamePrefix;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

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
        options.addOption( STANDARD_LOCATION_OPTION, "standard-location", false, "Write the experimental design to the standard location." );
        options.addOption( OUT_FILE_PREFIX_OPTION, "outFilePrefix", true, "File prefix for saving the output (short name will be appended)" );
        addForceOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        standardLocation = commandLine.hasOption( STANDARD_LOCATION_OPTION );
        outFileNamePrefix = commandLine.getOptionValue( OUT_FILE_PREFIX_OPTION );
    }

    @Override
    protected void doWork() throws Exception {
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( !( ee instanceof ExpressionExperiment ) ) {
                addErrorObject( ee, new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" ) );
                continue;
            }
            try {
                processExpressionExperiment( ( ExpressionExperiment ) ee );
            } catch ( Exception e ) {
                addErrorObject( ee, e );
            }
        }
    }

    private void processExpressionExperiment( ExpressionExperiment ee ) throws IOException {
        if ( standardLocation ) {
            expressionDataFileService.writeOrLocateDesignFile( ee, force );
        } else {
            ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();
            Path file = Paths.get( getFilename( ee ) );
            if ( Files.exists( file ) && !force ) {
                throw new IllegalStateException( "File already exists: " + file + ", use -force to override." );
            }
            PathUtils.createParentDirectories( file );
            try ( Writer writer = Files.newBufferedWriter( file ) ) {
                edWriter.write( writer, ee, true );
                writer.flush();
            }
        }
    }

    private String getFilename( ExpressionExperiment ee ) {
        if ( outFileNamePrefix != null ) {
            return outFileNamePrefix + "_" + FileTools.cleanForFileName( ee.getShortName() ) + ".txt";
        } else {
            return FileTools.cleanForFileName( ee.getShortName() ) + ".txt";
        }
    }
}
