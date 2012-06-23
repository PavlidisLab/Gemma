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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * Prints preferred data matrix to a file.
 * 
 * @author Paul
 * @version $Id$
 */
public class ExpressionDataMatrixWriterCLI extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        ExpressionDataMatrixWriterCLI cli = new ExpressionDataMatrixWriterCLI();
        Exception exc = cli.doWork( args );
        if ( exc != null ) {
            log.error( exc.getMessage() );
        }
    }

    private String outFileName = null;

    private boolean filter = false;

    private boolean addGeneInfo = false;

    @Override
    public String getShortDesc() {
        return "Prints preferred data matrix to a file.";
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option outputFileOption = OptionBuilder
                .hasArg()
                .withArgName( "outFilePrefix" )
                .withDescription(
                        "File prefix for saving the output (short name will be appended); if not supplied, output to stdout" )
                .withLongOpt( "outFilePrefix" ).create( 'o' );
        addOption( outputFileOption );

        Option geneInfoOption = OptionBuilder.withDescription(
                "Write the gene information.  If not set, the gene information will not be written." ).create( 'g' );
        addOption( geneInfoOption );

        Option filteredOption = OptionBuilder.withDescription( "Filter expression matrix under default parameters" )
                .create( "filter" );
        addOption( filteredOption );

    }

    @Override
    protected Exception doWork( String[] args ) {
        FilterConfig fCon = new FilterConfig();
        Exception err = processCommandLine( "expressionDataMatrixWriterCLI", args );
        if ( err != null ) return err;

        ExpressionDataMatrixService ahs = this.getBean( ExpressionDataMatrixService.class );

        CompositeSequenceService css = this.getBean( CompositeSequenceService.class );

        for ( BioAssaySet ee : expressionExperiments ) {
            ExpressionDataDoubleMatrix dataMatrix;
            if ( filter ) {// filtered expression matrix desired
                dataMatrix = ahs.getFilteredMatrix( ( ExpressionExperiment ) ee, fCon );
            } else {
                dataMatrix = ahs.getProcessedExpressionDataMatrix( ( ExpressionExperiment ) ee );
            }

            int rows = dataMatrix.rows();
            Collection<CompositeSequence> probes = new ArrayList<CompositeSequence>();
            for ( int j = 0; j < rows; j++ ) {
                CompositeSequence probeForRow = dataMatrix.getDesignElementForRow( j );
                probes.add( probeForRow );
            }

            Map<CompositeSequence, Collection<Gene>> genes = css.getGenes( probes );

            Writer writer;
            try {
                MatrixWriter out = new MatrixWriter();

                if ( outFileName == null ) {
                    writer = new PrintWriter( System.out );
                } else {
                    writer = new PrintWriter( outFileName + "_"
                            + ( ( ExpressionExperiment ) ee ).getShortName().replaceAll( "\\s", "" ) + ".txt" );
                }
                out.write( writer, dataMatrix, genes, true, false, addGeneInfo, true );
                writer.flush();
                writer.close();
            } catch ( IOException e ) {
                return e;
            } finally {

            }
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        outFileName = getOptionValue( 'o' );

        addGeneInfo = hasOption( 'g' );
        if ( hasOption( "filter" ) ) {
            filter = true;
        }
    }
}
