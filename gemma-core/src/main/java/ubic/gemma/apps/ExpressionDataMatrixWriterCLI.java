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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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

    @Override
    public String getShortDesc() {
        return "Prints preferred data matrix to a file; gene information is included if available.";
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option outputFileOption = OptionBuilder
                .hasArg()
                .withArgName( "outFileName" )
                .withDescription(
                        "File name. If omitted, the file name will be based on the short name of the experiment." )
                .withLongOpt( "outFileName" ).create( 'o' );
        addOption( outputFileOption );

        Option filteredOption = OptionBuilder.withDescription( "Filter expression matrix under default parameters" )
                .create( "filter" );
        addOption( filteredOption );
    }

    private ExpressionDataFileService fs;

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "expressionDataMatrixWriterCLI", args );
        if ( err != null ) return err;

        fs = this.getBean( ExpressionDataFileService.class );

        if ( expressionExperiments.size() > 1 && StringUtils.isNotBlank( outFileName ) ) {
            throw new IllegalArgumentException( "Output file name can only be used for single experiment output" );
        }
        for ( BioAssaySet ee : expressionExperiments ) {

            String fileName;
            if ( StringUtils.isNotBlank( outFileName ) ) {
                fileName = outFileName;
            } else {
                fileName = FileTools.cleanForFileName( ( ( ExpressionExperiment ) ee ).getShortName() ) + ".txt";
            }

            try {
                fs.writeDataFile( ( ExpressionExperiment ) ee, filter, fileName );
            } catch ( IOException e ) {
                this.errorObjects.add( ee + ": " + e );
            }
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        outFileName = getOptionValue( 'o' );
        if ( hasOption( "filter" ) ) {
            filter = true;
        }
    }
}
