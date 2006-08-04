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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

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

    public static void main( String[] args ) {
        GeneLoaderCLI p = new GeneLoaderCLI();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {

        try {
            /* COMMAND LINE PARSER STAGE */
            Exception err = processCommandLine( "GeneLoaderCLI", args );

            if ( err != null ) return err;

            /* check parse option. */
            if ( hasOption( 'x' ) ) {
                NcbiGeneInfoParser geneInfoParser = new NcbiGeneInfoParser();
                geneInfoParser.parse( getOptionValue( 'x' ) );
            } else if ( hasOption( 'l' ) ) {
                /* check load option. */
                NcbiGeneInfoParser geneInfoParser = new NcbiGeneInfoParser();
                String[] filenames = getOptionValues( 'l' );

                for ( int i = 0; i < filenames.length - 1; i++ ) {
                    geneInfoParser.parse( filenames[i] );
                    i++;
                }

                // AS
                geneInfoParser.parse( filenames[filenames.length - 1] );
                Collection<NCBIGeneInfo> keys = geneInfoParser.getResults();

                NcbiGeneConverter converter = new NcbiGeneConverter();
                for ( NCBIGeneInfo info : keys ) {
                    Gene gene = converter.convert( info );

                    gene.setTaxon( ( Taxon ) getPersisterHelper().persist( gene.getTaxon() ) );
                    if ( gene == null ) {
                        log.warn( "gene null. skipping" );
                    } else {
                        log.debug( "persisting gene: " + gene.getNcbiId() );
                        getGenePersister().persist( gene );
                    }
                }
                // cli.getGenePersister().persist( geneInfoParser.getResults() );
                // endAS

            } else if ( hasOption( 'r' ) ) { /* check remove option. */
                getGenePersister().removeAll();
            } else { /* defaults to print help. */
                printHelp( "GeneLoaderCLI" );
            }

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return null;
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

        addOption( parseOption );
        addOption( loadOption );
        addOption( removeOption );

    }

}
