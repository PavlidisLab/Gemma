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

import org.apache.commons.lang3.StringUtils;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;

/**
 * Prints preferred data matrix to a file.
 *
 * @author Paul
 */
public class ExpressionDataMatrixWriterCLI extends ExpressionExperimentManipulatingCLI {

    private boolean filter = false;
    private String outFileName = null;

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( "o", "outputFileName",
                "File name. If omitted, the file name will be based on the short name of the experiment.", "filename" );
        super.addOption( "filter", "Filter expression matrix under default parameters" );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        outFileName = this.getOptionValue( 'o' );
        if ( this.hasOption( "filter" ) ) {
            filter = true;
        }
    }

    @Override
    public String getCommandName() {
        return "getDataMatrix";
    }

    @Override
    protected void doWork() throws Exception {
        ExpressionDataFileService fs = this.getBean( ExpressionDataFileService.class );

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
                fs.writeDataFile( ( ExpressionExperiment ) ee, filter, fileName, false );
            } catch ( IOException e ) {
                this.errorObjects.add( ee + ": " + e );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Prints preferred data matrix to a file; gene information is included if available.";
    }
}
