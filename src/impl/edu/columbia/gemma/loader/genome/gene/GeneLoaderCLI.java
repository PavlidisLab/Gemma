package edu.columbia.gemma.loader.genome.gene;

import java.io.IOException;
import java.util.Collection;

//import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

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
import edu.columbia.gemma.expression.designElement.DesignElementService;
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
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneLoaderCLI {
    protected static final Log log = LogFactory.getLog( GeneLoaderCLI.class );

    // FIXME this should use the SDOG (source domain object generator)

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    public static void main( String args[] ) throws IOException {
        GeneLoaderCLI cli = new GeneLoaderCLI();

        try {
            // options stage
            OptionBuilder.withDescription( "Print help for this application" );
            Option helpOpt = OptionBuilder.create( 'h' );

            OptionBuilder.hasArg();
            OptionBuilder.withDescription( "Parse File (requires file arg)" );
            Option parseOpt = OptionBuilder.create( 'p' );

            OptionBuilder.hasArgs();
            OptionBuilder.withDescription( "1) Specify files\n" + "2) Load database with entries from file" );
            Option loadOpt = OptionBuilder.create( 'l' );

            OptionBuilder.withDescription( "Remove genes from database" );
            Option removeOpt = OptionBuilder.create( 'r' );

            Options opt = new Options();
            opt.addOption( helpOpt );
            opt.addOption( parseOpt );
            opt.addOption( loadOpt );
            opt.addOption( removeOpt );

            // parser stage
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse( opt, args );

            NcbiGeneInfoParser geneInfoParser = new NcbiGeneInfoParser();
            // NcbiGene2AccessionParser gene2AccParser = new NcbiGene2AccessionParser();

            // interrogation stage
            if ( cl.hasOption( 'h' ) ) {
                printHelp( opt );

            } else if ( cl.hasOption( 'p' ) ) {
                geneInfoParser.parse( cl.getOptionValue( 'p' ) );
            } else if ( cl.hasOption( 'l' ) ) {

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
                    ( ( Gene ) gene ).setTaxon( ( Taxon ) cli.getMl().persist( ( ( Gene ) gene ).getTaxon() ) );
                    cli.getGenePersister().persist( gene );
                }
                // cli.getGenePersister().persist( geneInfoParser.getResults() );
                // endAS

            } else if ( cl.hasOption( 'r' ) ) {
                cli.getGenePersister().removeAll();
            } else {
                printHelp( opt );
            }
        } catch ( ParseException e ) {
            e.printStackTrace();
        }
    }

    private PersisterHelper ml;
    private GenePersister genePersister;

    public GeneLoaderCLI() {
        BeanFactory ctx = SpringContextUtil.getApplicationContext( false );

        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctx
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( "pavlab", "pavlab" );

        ml = new PersisterHelper();
        ml.setBioMaterialService( ( BioMaterialService ) ctx.getBean( "bioMaterialService" ) );
        ml.setExpressionExperimentService( ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" ) );
        ml.setPersonService( ( PersonService ) ctx.getBean( "personService" ) );
        ml.setOntologyEntryService( ( OntologyEntryService ) ctx.getBean( "ontologyEntryService" ) );
        ml.setArrayDesignService( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) );
        ml.setExternalDatabaseService( ( ExternalDatabaseService ) ctx.getBean( "externalDatabaseService" ) );
        ml.setDesignElementService( ( DesignElementService ) ctx.getBean( "designElementService" ) );
        ml.setProtocolService( ( ProtocolService ) ctx.getBean( "protocolService" ) );
        ml.setHardwareService( ( HardwareService ) ctx.getBean( "hardwareService" ) );
        ml.setSoftwareService( ( SoftwareService ) ctx.getBean( "softwareService" ) );
        ml.setTaxonService( ( TaxonService ) ctx.getBean( "taxonService" ) );
        ml.setBioAssayService( ( BioAssayService ) ctx.getBean( "bioAssayService" ) );
        ml.setQuantitationTypeService( ( QuantitationTypeService ) ctx.getBean( "quantitationTypeService" ) );
        ml.setLocalFileService( ( LocalFileService ) ctx.getBean( "localFileService" ) );
        ml.setCompoundService( ( CompoundService ) ctx.getBean( "compoundService" ) );
        ml.setDatabaseEntryService( ( DatabaseEntryService ) ctx.getBean( "databaseEntryService" ) );
        ml.setContactService( ( ContactService ) ctx.getBean( "contactService" ) );
        ml.setBioSequenceService( ( BioSequenceService ) ctx.getBean( "bioSequenceService" ) );
        ml.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
        ml.setGeneService( ( GeneService ) ctx.getBean( "geneService" ) );
        genePersister = new GenePersister();
        genePersister.setGeneService( ( GeneService ) ctx.getBean( "geneService" ) );

        genePersister.setPersisterHelper( ml );
    }

    /**
     * @param opt
     */
    private static void printHelp( Options opt ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( "Options Tip", opt );
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
    public PersisterHelper getMl() {
        return this.ml;
    }

}
