/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.*;

/**
 * @author Paul
 * @see ExperimentalDesignImporter
 */
public class ExperimentalDesignImportCli extends AbstractCLIContextCLI {

    private ExpressionExperiment expressionExperiment;
    private InputStream inputStream;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    public String getCommandName() {
        return "importDesign";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        Option expOption = Option.builder( "e" ).required().hasArg().argName( "Expression experiment name" )
                .desc(
                        "Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
                                + "and if this option is omitted, the tool will be applied to all expression experiments." )
                .longOpt( "experiment" ).build();

        this.addOption( expOption );

        Option designFileOption = Option.builder( "f" ).required().hasArg().argName( "Design file" )
                .desc( "Experimental design description file" ).longOpt( "designFile" ).build();
        this.addOption( designFileOption );
    }

    @Override
    protected void doWork() throws Exception {
        ExperimentalFactorOntologyService mos = this.getBean( OntologyService.class )
                .getExperimentalFactorOntologyService();
        mos.startInitializationThread( true, false ); // note will *not* re-index
        while ( !mos.isOntologyLoaded() ) {
            try {
                Thread.sleep( 5000 );
            } catch ( InterruptedException e1 ) {
                //
            }
            AbstractCLI.log.info( "Waiting for EFO to load" );
        }

        ExperimentalDesignImporter edImp = this.getBean( ExperimentalDesignImporter.class );
        ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );
        expressionExperiment = ees.thawBioAssays( expressionExperiment );
        edImp.importDesign( expressionExperiment, inputStream );
    }

    @Override
    public String getShortDesc() {
        return "Import an experimental design";
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        String shortName = this.getOptionValue( 'e' );
        this.expressionExperiment = this.locateExpressionExperiment( shortName );
        if ( this.expressionExperiment == null ) {
            throw new IllegalArgumentException( shortName + " not found" );
        }

        File f = new File( this.getOptionValue( 'f' ) );
        if ( !f.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from " + f );
        }

        try {
            inputStream = new FileInputStream( f );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param shortName short name of the experiment to find.
     * @return experiment with the given short name, if it exists. Bails otherwise with {@link
     *         ubic.gemma.core.util.AbstractCLI.ErrorCode#INVALID_OPTION}.
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // Possible external use
    protected ExpressionExperiment locateExpressionExperiment( String shortName ) {

        if ( shortName == null ) {
            addErrorObject( null, "Expression experiment short name must be provided" );
            return null;
        }
        ExpressionExperimentService eeService = this.getBean( ExpressionExperimentService.class );
        ExpressionExperiment experiment = eeService.findByShortName( shortName );

        if ( experiment == null ) {
            throw new RuntimeException( "No experiment " + shortName + " found" );
        }
        return experiment;
    }

}
