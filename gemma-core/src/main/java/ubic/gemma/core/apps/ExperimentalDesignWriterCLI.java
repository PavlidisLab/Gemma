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

import org.apache.commons.cli.Option;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.datastructure.matrix.ExperimentalDesignWriter;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Writes out the experimental design for a given experiment. This can be directly read into R.
 *
 * @author keshav
 */
public class ExperimentalDesignWriterCLI extends ExpressionExperimentManipulatingCLI {

    private String outFileName;

    public static void main( String[] args ) {
        ExperimentalDesignWriterCLI cli = new ExperimentalDesignWriterCLI();
        Exception exc = cli.doWork( args );
        if ( exc != null ) {
            AbstractCLI.log.error( exc.getMessage() );
        }
    }

    @Override
    public String getCommandName() {
        return "printExperimentalDesign";
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception e = super.processCommandLine( args );
        if ( e != null )
            return e;

        for ( BioAssaySet ee : expressionExperiments ) {

            if ( ee instanceof ExpressionExperiment ) {
                ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();

                try (PrintWriter writer = new PrintWriter(
                        outFileName + "_" + FileTools.cleanForFileName( ( ( ExpressionExperiment ) ee ).getShortName() )
                                + ".txt" )) {

                    edWriter.write( writer, ( ExpressionExperiment ) ee, true );
                    writer.flush();
                } catch ( IOException exception ) {
                    return exception;
                }
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }
        }

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Prints experimental design to a file in a R-friendly format";
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option outputFileOption = Option.builder( "o" ).hasArg().required().argName( "outFilePrefix" )
                .desc( "File prefix for saving the output (short name will be appended)" )
                .longOpt( "outFilePrefix" ).build();
        this.addOption( outputFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        outFileName = this.getOptionValue( 'o' );
    }
}
