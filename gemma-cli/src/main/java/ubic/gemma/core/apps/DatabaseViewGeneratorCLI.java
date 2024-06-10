/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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
import ubic.gemma.core.analysis.report.DatabaseViewGenerator;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;

/**
 * Simple driver of DatabaseViewGenerator. Developed to support NIF and other external data consumers.
 *
 * @author paul
 */
@SuppressWarnings({ "FieldCanBeLocal", "unused" }) // Possible external use
public class DatabaseViewGeneratorCLI extends AbstractAuthenticatedCLI {

    private boolean generateDatasetSummary = false;
    private boolean generateDiffExpressionSummary = false;
    private boolean generateTissueSummary = false;
    private int limit = 0;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.ANALYSIS;
    }

    @Override
    public String getCommandName() {
        return "dumpForNIF";
    }

    @Override
    protected void buildOptions( Options options ) {
        Option datasetSummary = Option.builder( "d" )
                .longOpt( "dataset" )
                .desc( "Will generate a zip file containing a summary of all accessible datasets in gemma" )
                .build();

        Option datasetTissueSummary = Option.builder( "t" )
                .longOpt( "tissue" )
                .desc( "Will generate a zip file containing a summary of all the tissues in accessible datasets" )
                .build();

        Option diffExpSummary = Option.builder( "x" )
                .longOpt( "diffexpression" )
                .desc( "Will generate a zip file containing a summary of all the differential expressed genes in accessible datasets" )
                .build();

        Option limitOpt = Option.builder( "l" )
                .longOpt( "limit" )
                .hasArg()
                .argName( "Limit number of datasets" )
                .desc( "will impose a limit on how many datasets to process" )
                .build();

        options.addOption( datasetSummary );
        options.addOption( datasetTissueSummary );
        options.addOption( diffExpSummary );
        options.addOption( limitOpt );
    }

    @Override
    protected void doWork() throws Exception {
        DatabaseViewGenerator v = this.getBean( DatabaseViewGenerator.class );
        v.runAll();
    }

    @Override
    public String getShortDesc() {
        return "Generate views of the database in flat files";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

        if ( commandLine.hasOption( 'x' ) ) {
            this.generateDiffExpressionSummary = true;
        }

        if ( commandLine.hasOption( 'd' ) ) {
            this.generateDatasetSummary = true;
        }

        if ( commandLine.hasOption( 't' ) ) {
            this.generateTissueSummary = true;
        }

        if ( commandLine.hasOption( 'l' ) ) {
            try {
                this.limit = Integer.parseInt( commandLine.getOptionValue( 'l' ) );
            } catch ( NumberFormatException nfe ) {
                AbstractCLI.log.warn( "Unable to process limit parameter. Processing all available experiments." );
                this.limit = 0;
            }
        }
    }

}
