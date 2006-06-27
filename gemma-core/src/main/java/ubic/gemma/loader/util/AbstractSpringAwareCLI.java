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
package ubic.gemma.loader.util;

import java.io.IOException;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.auditAndSecurity.PersonService;
import ubic.gemma.model.common.description.DatabaseEntryService;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFileService;
import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.model.common.protocol.HardwareService;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.common.protocol.SoftwareService;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.biomaterial.CompoundService;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.ReporterService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.security.authentication.ManualAuthenticationProcessing;
import ubic.gemma.util.SpringContextUtil;

/**
 * Subclass this to create command line interface (CLI) tools that need a Spring context. A standard set of CLI options
 * are provided to manage authentication.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractSpringAwareCLI {

    private static final String HEADER = "Options:";
    private static final String FOOTER = "The Gemma project, Copyright (c) 2006 University of British Columbia\n"
            + "For more information, visit http://www.neurogemma.org/";

    protected static Options options = new Options();
    protected static CommandLine commandLine;

    protected static final Log log = LogFactory.getLog( AbstractSpringAwareCLI.class );
    protected static BeanFactory ctx = null;
    PersisterHelper ph = null;

    public AbstractSpringAwareCLI() {
        this.buildStandardOptions();
        this.buildOptions();
    }

    @SuppressWarnings("static-access")
    private void buildStandardOptions() {
        Option helpOpt = new Option( "h", "help", false, "Print this message" );
        Option testOpt = new Option( "testing", false, "Use the test environment" );

        Option usernameOpt = OptionBuilder.withArgName( "user" ).isRequired().withLongOpt( "user" ).hasArg()
                .withDescription( "User name for accessing the system" ).create( 'u' );

        Option passwordOpt = OptionBuilder.withArgName( "passwd" ).isRequired().withLongOpt( "password" ).hasArg()
                .withDescription( "Password for accessing the system" ).create( 'p' );

        options.addOption( helpOpt );
        options.addOption( usernameOpt );
        options.addOption( passwordOpt );
        options.addOption( testOpt );

    }

    protected abstract void buildOptions();

    /**
     * @return
     */
    protected PersisterHelper getPersisterHelper() {
        if ( ph != null ) {
            return ph;
        }

        assert ctx != null : "Spring context was not initialized";
        ph = new PersisterHelper();
        ph.setBioMaterialService( ( BioMaterialService ) ctx.getBean( "bioMaterialService" ) );
        ph
                .setExpressionExperimentService( ( ExpressionExperimentService ) ctx
                        .getBean( "expressionExperimentService" ) );
        ph.setPersonService( ( PersonService ) ctx.getBean( "personService" ) );
        ph.setOntologyEntryService( ( OntologyEntryService ) ctx.getBean( "ontologyEntryService" ) );
        ph.setArrayDesignService( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) );
        ph.setExternalDatabaseService( ( ExternalDatabaseService ) ctx.getBean( "externalDatabaseService" ) );
        ph.setReporterService( ( ReporterService ) ctx.getBean( "reporterService" ) );
        ph.setCompositeSequenceService( ( CompositeSequenceService ) ctx.getBean( "compositeSequenceService" ) );
        ph.setProtocolService( ( ProtocolService ) ctx.getBean( "protocolService" ) );
        ph.setHardwareService( ( HardwareService ) ctx.getBean( "hardwareService" ) );
        ph.setSoftwareService( ( SoftwareService ) ctx.getBean( "softwareService" ) );
        ph.setTaxonService( ( TaxonService ) ctx.getBean( "taxonService" ) );
        ph.setBioAssayService( ( BioAssayService ) ctx.getBean( "bioAssayService" ) );
        ph.setQuantitationTypeService( ( QuantitationTypeService ) ctx.getBean( "quantitationTypeService" ) );
        ph.setLocalFileService( ( LocalFileService ) ctx.getBean( "localFileService" ) );
        ph.setCompoundService( ( CompoundService ) ctx.getBean( "compoundService" ) );
        ph.setDatabaseEntryService( ( DatabaseEntryService ) ctx.getBean( "databaseEntryService" ) );
        ph.setContactService( ( ContactService ) ctx.getBean( "contactService" ) );
        ph.setBioSequenceService( ( BioSequenceService ) ctx.getBean( "bioSequenceService" ) );
        ph.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
        ph.setGeneService( ( GeneService ) ctx.getBean( "geneService" ) );
        return ph;
    }

    /** check username and password. */
    private static void authenticate() {

        if ( commandLine.hasOption( 'u' ) ) {
            if ( commandLine.hasOption( 'p' ) ) {
                String username = commandLine.getOptionValue( 'u' );
                String password = commandLine.getOptionValue( 'p' );
                ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctx
                        .getBean( "manualAuthenticationProcessing" );
                manAuthentication.validateRequest( username, password );
            }
        } else {
            log.error( "Not authenticated. Make sure you entered a valid username and/or password" );
            System.exit( 0 );
        }

    }

    /** check if using test or production context */
    private static void setTestOrProduction() {

        if ( commandLine.hasOption( "testing" ) ) {
            ctx = SpringContextUtil.getApplicationContext( true );
        } else {
            ctx = SpringContextUtil.getApplicationContext( false );
        }

    }

    /**
     * This must be called in your main method.
     * 
     * @param args
     * @throws ParseException
     */
    protected static void initCommandParse( String commandName, String[] args ) {
        /* COMMAND LINE PARSER STAGE */
        BasicParser parser = new BasicParser();

        if ( args == null ) {
            printHelp( commandName );
            System.exit( 0 );
        }

        try {
            commandLine = parser.parse( options, args );
        } catch ( ParseException e ) {

            if ( e instanceof MissingOptionException ) {
                System.out.println( "Required option(s) were not supplied: " + e.getMessage() );
            } else if ( e instanceof AlreadySelectedException ) {
                System.out.println( "The option(s) " + e.getMessage() + " were already selected" );
            } else if ( e instanceof MissingArgumentException ) {
                System.out.println( "Missing argument(s) " + e.getMessage() );
            } else if ( e instanceof UnrecognizedOptionException ) {
                System.out.println( "Unrecognized option " + e.getMessage() );
            } else {
                e.printStackTrace();
            }

            printHelp( commandName );

            System.exit( 0 );
        }

        /* INTERROGATION STAGE */
        if ( commandLine.hasOption( 'h' ) ) {
            printHelp( commandName );
            System.exit( 0 );
        }

        setTestOrProduction();
        authenticate();

    }

    /**
     * @param command The name of the command as used at the command line.
     */
    protected static void printHelp( String command ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( command, HEADER, options, FOOTER );
    }

}
