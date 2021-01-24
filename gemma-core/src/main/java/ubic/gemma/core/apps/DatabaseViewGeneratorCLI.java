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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.core.analysis.report.DatabaseViewGenerator;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;

/**
 * Simple driver of DatabaseViewGenerator. Developed to support NIF and other external data consumers.
 *
 * @author paul
 */
@SuppressWarnings({ "FieldCanBeLocal", "unused" }) // Possible external use
public class DatabaseViewGeneratorCLI extends AbstractCLIContextCLI {

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
    protected void buildOptions() {
        OptionBuilder
                .withDescription( "Will generate a zip file containing a summary of all accessible datasets in gemma" );
        OptionBuilder.withLongOpt( "dataset" );
        Option datasetSummary = OptionBuilder.create( 'd' );

        OptionBuilder.withDescription(
                "Will generate a zip file containing a summary of all the tissues in accessible datasets" );
        OptionBuilder.withLongOpt( "tissue" );
        Option datasetTissueSummary = OptionBuilder.create( 't' );

        OptionBuilder.withDescription(
                "Will generate a zip file containing a summary of all the differential expressed genes in accessible datasets" );
        OptionBuilder.withLongOpt( "diffexpression" );
        Option diffExpSummary = OptionBuilder.create( 'x' );

        OptionBuilder.hasArg();
        OptionBuilder.withArgName( "Limit number of datasets" );
        OptionBuilder.withDescription( "will impose a limit on how many datasets to process" );
        OptionBuilder.withLongOpt( "limit" );
        Option limitOpt = OptionBuilder.create( 'l' );

        this.addOption( datasetSummary );
        this.addOption( datasetTissueSummary );
        this.addOption( diffExpSummary );
        this.addOption( limitOpt );

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
    protected void processOptions() {

        if ( this.hasOption( 'x' ) ) {
            this.generateDiffExpressionSummary = true;
        }

        if ( this.hasOption( 'd' ) ) {
            this.generateDatasetSummary = true;
        }

        if ( this.hasOption( 't' ) ) {
            this.generateTissueSummary = true;
        }

        if ( this.hasOption( 'l' ) ) {
            try {
                this.limit = Integer.parseInt( this.getOptionValue( 'l' ) );
            } catch ( NumberFormatException nfe ) {
                AbstractCLI.log.warn( "Unable to process limit parameter. Processing all available experiments." );
                this.limit = 0;
            }
        }
    }

}
