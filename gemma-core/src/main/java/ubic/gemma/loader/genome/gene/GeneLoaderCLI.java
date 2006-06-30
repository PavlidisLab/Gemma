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
package ubic.gemma.loader.genome.gene;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.loader.genome.gene.ncbi.NcbiGeneConverter;
import ubic.gemma.loader.genome.gene.ncbi.NcbiGeneInfoParser;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.loader.util.AbstractSpringAwareCLI;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Command line interface to gene parsing and loading
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class GeneLoaderCLI extends AbstractSpringAwareCLI {
    private GenePersister genePersister;

    // FIXME this should use the SDOG (source domain object generator)

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    @SuppressWarnings("static-access")
    public static void main( String args[] ) throws IOException {
        GeneLoaderCLI cli = new GeneLoaderCLI();

        /* COMMAND LINE PARSER STAGE */
        cli.initCommandParse( "GeneLoaderCLI", args );

        /* check parse option. */
        if ( cli.hasOption( 'x' ) ) {
            NcbiGeneInfoParser geneInfoParser = new NcbiGeneInfoParser();
            geneInfoParser.parse( cli.getOptionValue( 'x' ) );
        }

        /* check load option. */
        else if ( cli.hasOption( 'l' ) ) {

            NcbiGeneInfoParser geneInfoParser = new NcbiGeneInfoParser();
            String[] filenames = cli.getOptionValues( 'l' );

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

                ( ( Gene ) gene ).setTaxon( ( Taxon ) cli.getPersisterHelper().persist( ( ( Gene ) gene ).getTaxon() ) );
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
        else if ( cli.hasOption( 'r' ) ) {
            cli.getGenePersister().removeAll();
        }
        /* defaults to print help. */
        else {
            cli.printHelp( "GeneLoaderCLI" );
        }

    }

    public GeneLoaderCLI() {
        super();
        genePersister = new GenePersister();
        genePersister.setGeneService( ( GeneService ) ctx.getBean( "geneService" ) );
        genePersister.setPersisterHelper( this.getPersisterHelper() );
    }

    /**
     * @return Returns the genePersister.
     */
    public GenePersister getGenePersister() {
        return this.genePersister;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.AbstractSpringAwareCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        /* parse */
        Option parseOption = OptionBuilder.hasArg().withLongOpt( "input" ).withDescription( "File to parse" ).create(
                'x' );

        /* parse and load */
        Option loadOption = OptionBuilder.hasArg().withDescription(
                "1: Specify files or 2: Load database with entries from file" ).create( 'l' );

        Option removeOption = OptionBuilder.withDescription( "Remove from database" ).create( 'r' );

        options.addOption( parseOption );
        options.addOption( loadOption );
        options.addOption( removeOption );

    }

}
