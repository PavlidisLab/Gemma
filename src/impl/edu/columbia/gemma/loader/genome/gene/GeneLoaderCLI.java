package edu.columbia.gemma.loader.genome.gene;

import java.io.IOException;

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

import edu.columbia.gemma.common.auditAndSecurity.ContactDao;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.description.DatabaseEntryDao;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.LocalFileDao;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.common.protocol.HardwareDao;
import edu.columbia.gemma.common.protocol.ProtocolDao;
import edu.columbia.gemma.common.protocol.SoftwareDao;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.expression.bioAssay.BioAssayDao;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.biomaterial.CompoundDao;
import edu.columbia.gemma.expression.designElement.DesignElementDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.expression.experiment.FactorValueDao;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.genome.biosequence.BioSequenceDao;
import edu.columbia.gemma.loader.genome.gene.ncbi.NcbiGeneInfoParser;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;
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

                geneInfoParser.parse( filenames[filenames.length - 1] );
                cli.getGenePersister().persist( geneInfoParser.getResults() );

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
        ml = new PersisterHelper();
        ml.setBioMaterialDao( ( BioMaterialDao ) ctx.getBean( "bioMaterialDao" ) );
        ml.setExpressionExperimentDao( ( ExpressionExperimentDao ) ctx.getBean( "expressionExperimentDao" ) );
        ml.setPersonDao( ( PersonDao ) ctx.getBean( "personDao" ) );
        ml.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );
        ml.setArrayDesignDao( ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" ) );
        ml.setExternalDatabaseDao( ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" ) );
        ml.setDesignElementDao( ( DesignElementDao ) ctx.getBean( "designElementDao" ) );
        ml.setProtocolDao( ( ProtocolDao ) ctx.getBean( "protocolDao" ) );
        ml.setHardwareDao( ( HardwareDao ) ctx.getBean( "hardwareDao" ) );
        ml.setSoftwareDao( ( SoftwareDao ) ctx.getBean( "softwareDao" ) );
        ml.setTaxonDao( ( TaxonDao ) ctx.getBean( "taxonDao" ) );
        ml.setBioAssayDao( ( BioAssayDao ) ctx.getBean( "bioAssayDao" ) );
        ml.setQuantitationTypeDao( ( QuantitationTypeDao ) ctx.getBean( "quantitationTypeDao" ) );
        ml.setLocalFileDao( ( LocalFileDao ) ctx.getBean( "localFileDao" ) );
        ml.setCompoundDao( ( CompoundDao ) ctx.getBean( "compoundDao" ) );
        ml.setDatabaseEntryDao( ( DatabaseEntryDao ) ctx.getBean( "databaseEntryDao" ) );
        ml.setContactDao( ( ContactDao ) ctx.getBean( "contactDao" ) );
        ml.setBioSequenceDao( ( BioSequenceDao ) ctx.getBean( "bioSequenceDao" ) );
        ml.setFactorValueDao( ( FactorValueDao ) ctx.getBean( "factorValueDao" ) );
        genePersister = new GenePersister();
        genePersister.setGeneDao( ( GeneDao ) ctx.getBean( "geneDao" ) );

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
