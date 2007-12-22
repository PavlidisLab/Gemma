/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Prints preferred data matrix to a file.
 * 
 * @author Paul
 * @version $Id$
 */
public class ExpressionDataMatrixWriterCLI extends ExpressionExperimentManipulatingCLI {

    private String outFileName;

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFilePrefix" ).withDescription(
                "File prefix for saving the output" ).withLongOpt( "outFilePrefix" ).create( 'o' );
        addOption( outputFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        outFileName = getOptionValue( 'o' );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        processCommandLine( "expressionDataMatrixWriterCLI", args );

        ExpressionDataMatrixService eeds = ( ExpressionDataMatrixService ) this.getBean( "expressionDataMatrixService" );
        for ( ExpressionExperiment ee : expressionExperiments ) {

            ExpressionDataDoubleMatrix dataMatrix = eeds.getPreferredDataMatrix( ee, true );

            try {
                MatrixWriter out = new MatrixWriter();
                PrintWriter writer = new PrintWriter( outFileName );
                /*
                 * FIXME output the gene information too.
                 */
                out.write( writer, dataMatrix, new HashMap(), true, false );
            } catch ( IOException e ) {
                return e;
            }
        }

        return null;
    }

    public static void main( String[] args ) {
        ExpressionDataMatrixWriterCLI cli = new ExpressionDataMatrixWriterCLI();
        Exception exc = cli.doWork( args );
        if ( exc != null ) {
            log.error( exc.getMessage() );
        }
    }

}
