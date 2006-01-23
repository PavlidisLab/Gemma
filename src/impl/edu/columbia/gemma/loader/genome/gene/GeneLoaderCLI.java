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

package edu.columbia.gemma.loader.genome.gene;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.common.auditAndSecurity.ContactService;
import edu.columbia.gemma.common.auditAndSecurity.PersonService;
import edu.columbia.gemma.common.description.DatabaseEntryService;
import edu.columbia.gemma.common.description.ExternalDatabaseService;
import edu.columbia.gemma.common.description.LocalFileService;
import edu.columbia.gemma.common.description.OntologyEntryService;
import edu.columbia.gemma.common.protocol.HardwareService;
import edu.columbia.gemma.common.protocol.ProtocolService;
import edu.columbia.gemma.common.protocol.SoftwareService;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeService;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.expression.bioAssay.BioAssayService;
import edu.columbia.gemma.expression.biomaterial.BioMaterialService;
import edu.columbia.gemma.expression.biomaterial.CompoundService;
import edu.columbia.gemma.expression.designElement.CompositeSequenceService;
import edu.columbia.gemma.expression.designElement.ReporterService;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;
import edu.columbia.gemma.expression.experiment.FactorValueService;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonService;
import edu.columbia.gemma.genome.biosequence.BioSequenceService;
import edu.columbia.gemma.genome.gene.GeneService;
import edu.columbia.gemma.loader.genome.gene.ncbi.NcbiGeneConverter;
import edu.columbia.gemma.loader.genome.gene.ncbi.NcbiGeneInfoParser;
import edu.columbia.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;
import edu.columbia.gemma.security.ui.ManualAuthenticationProcessing;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * Command line interface to gene parsing and loading
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneLoaderCLI {
    protected static final Log log = LogFactory.getLog( GeneLoaderCLI.class );
    protected static ManualAuthenticationProcessing manAuthentication = null;
    protected static BeanFactory ctx = null;

    private static final String USAGE = "[-h] [-u <username>] [-p <password>]  [-t <true|false>] [-x <file>] [-l <file>] [-r] ";
    private static final String HEADER = "The Gemma project, Copyright (c) 2006 University of British Columbia";
    private static final String FOOTER = "For more information, see our website at http://www.neurogemma.org";
    private PersisterHelper ph;
    private GenePersister genePersister;
    private static String username = null;
    private static String password = null;

    // FIXME this should use the SDOG (source domain object generator)

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    public static void main( String args[] ) throws IOException {
        GeneLoaderCLI cli = null;

        try {
            /* OPTIONS STAGE */

            /* help */
            OptionBuilder.withDescription( "Print help for this application" );
            Option helpOpt = OptionBuilder.create( 'h' );

            /* username */
            OptionBuilder.hasArgs();
            OptionBuilder.withDescription( "Username" );
            Option usernameOpt = OptionBuilder.create( 'u' );

            /* password */
            OptionBuilder.hasArgs();
            OptionBuilder.withDescription( "Password" );
            Option passwordOpt = OptionBuilder.create( 'p' );

            /* environment (test or prod) */
            OptionBuilder.hasArgs();
            OptionBuilder.withDescription( "Set use of test or production environment" );
            Option testOpt = OptionBuilder.create( 't' );

            /* parse */
            OptionBuilder.hasArg();
            OptionBuilder.withDescription( "Parse File" );
            Option parseOpt = OptionBuilder.create( 'x' );

            /* parse and load */
            OptionBuilder.hasArgs();
            OptionBuilder.withDescription( "1) Specify files\n" + "2) Load database with entries from file" );
            Option loadOpt = OptionBuilder.create( 'l' );

            /* remove */
            OptionBuilder.withDescription( "Remove from database" );
            Option removeOpt = OptionBuilder.create( 'r' );

            Options opt = new Options();
            opt.addOption( helpOpt );
            opt.addOption( usernameOpt );
            opt.addOption( passwordOpt );
            opt.addOption( testOpt );
            opt.addOption( parseOpt );
            opt.addOption( loadOpt );
            opt.addOption( removeOpt );

            /* COMMAND LINE PARSER STAGE */
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse( opt, args );

            /* INTERROGATION STAGE */
            if ( cl.hasOption( 'h' ) ) {
                printHelp( opt );
                System.exit( 0 );

            }

            /* check if using test or production context */
            if ( cl.hasOption( 't' ) ) {
                boolean isTest = Boolean.parseBoolean( cl.getOptionValue( 't' ) );
                if ( isTest )
                    ctx = SpringContextUtil.getApplicationContext( true );
                else
                    ctx = SpringContextUtil.getApplicationContext( false );

                cli = new GeneLoaderCLI();
            }
            // if no ctx is set, default to test environment.
            else {
                ctx = SpringContextUtil.getApplicationContext( true );
                cli = new GeneLoaderCLI();
            }

            /* check username and password. */
            if ( cl.hasOption( 'u' ) ) {
                if ( cl.hasOption( 'p' ) ) {
                    username = cl.getOptionValue( 'u' );
                    password = cl.getOptionValue( 'p' );
                    manAuthentication = ( ManualAuthenticationProcessing ) ctx
                            .getBean( "manualAuthenticationProcessing" );
                    manAuthentication.validateRequest( username, password );
                }
            } else {
                log.error( "Not authenticated.  Make sure you entered a valid username and/or password" );
                System.exit( 0 );
            }

            /* check parse option. */
            if ( cl.hasOption( 'x' ) ) {
                NcbiGeneInfoParser geneInfoParser = new NcbiGeneInfoParser();
                geneInfoParser.parse( cl.getOptionValue( 'x' ) );
            }

            /* check load option. */
            else if ( cl.hasOption( 'l' ) ) {

                NcbiGeneInfoParser geneInfoParser = new NcbiGeneInfoParser();
                String[] filenames = cl.getOptionValues( 'l' );

                for ( int i = 0; i < filenames.length - 1; i++ ) {
                    geneInfoParser.parse( filenames[i] );
                    i++;
                }

                // AS
                geneInfoParser.parse( filenames[filenames.length - 1] );
                Collection<Object> keys = geneInfoParser.getResults();

                NCBIGeneInfo info;
                Object gene;

                NcbiGeneConverter converter = new NcbiGeneConverter();
                for ( Object key : keys ) {
                    info = ( NCBIGeneInfo ) geneInfoParser.get( key );
                    gene = converter.convert( info );

                    ( ( Gene ) gene ).setTaxon( ( Taxon ) cli.getPh().persist( ( ( Gene ) gene ).getTaxon() ) );
                    if ( gene == null ) {
                        System.out.println( "gene null. skipping" );
                    } else {
                        System.out.println( "persisting gene: " + ( ( Gene ) gene ).getNcbiId() );
                        cli.getGenePersister().persist( gene );
                    }
                }
                // cli.getGenePersister().persist( geneInfoParser.getResults() );
                // endAS

            }

            /* check remove option. */
            else if ( cl.hasOption( 'r' ) ) {
                cli.getGenePersister().removeAll();
            }
            /* defaults to print help. */
            else {
                printHelp( opt );
            }
        } catch ( ParseException e ) {
            e.printStackTrace();
        }
    }

    public GeneLoaderCLI() {

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
        genePersister = new GenePersister();
        genePersister.setGeneService( ( GeneService ) ctx.getBean( "geneService" ) );

        genePersister.setPersisterHelper( ph );
    }

    /**
     * @param opt
     */
    private static void printHelp( Options opt ) {
        HelpFormatter h = new HelpFormatter();
        // h.setWidth( 80 );
        h.printHelp( USAGE, HEADER, opt, FOOTER );
    }

    /**
     * @return Returns the genePersister.
     */
    public GenePersister getGenePersister() {
        return this.genePersister;
    }

    /**
     * @return Returns the ml.
     */
    public PersisterHelper getPh() {
        return this.ph;
    }

}
