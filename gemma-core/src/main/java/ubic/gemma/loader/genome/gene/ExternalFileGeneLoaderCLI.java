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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import ubic.gemma.util.AbstractCLIContextCLI;

import java.io.IOException;

/**
 * CLI for loading genes from a non NCBI files. A taxon and gene file should be supplied as command line arguments. File
 * should be in tab delimited format containing gene symbol, gene name, uniprot id in that order.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class ExternalFileGeneLoaderCLI extends AbstractCLIContextCLI {

    private ExternalFileGeneLoaderService loader;
    private String directGeneInputFileName = null;
    private String taxonName;

    public ExternalFileGeneLoaderCLI() {
        super();
    }

    public static void main( String[] args ) {
        // super constructor calls build options
        ExternalFileGeneLoaderCLI p = new ExternalFileGeneLoaderCLI();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "ExternalFileGeneLoader", args );
        if ( err != null ) return err;
        processGeneList();
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.AbstractSpringAwareCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option directGene = OptionBuilder
                .withDescription(
                        "Import genes from a file rather than NCBI. You must provide the taxon option and gene file name including full path details" )
                .hasArg().withArgName( "file" ).create( "f" );
        addOption( directGene );

        Option taxonNameOption = OptionBuilder.hasArg()
                .withDescription( "Taxon common name e.g. 'salmonoid' does not have to be a species " ).create( "t" );
        addOption( taxonNameOption );

        requireLogin();
    }

    /**
     * This method is called at the end of processCommandLine
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'f' ) ) {
            directGeneInputFileName = getOptionValue( 'f' );
            if ( directGeneInputFileName == null ) {
                throw new IllegalArgumentException( "No gene input file provided " + directGeneInputFileName );
            }
        }
        if ( hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
            if ( taxonName == null ) {
                throw new IllegalArgumentException( "No taxon name supplied " );
            }
        }

    }

    /**
     * Main entry point to service class which reads a gene file and persists the genes in that file.
     */
    public void processGeneList() {

        loader = this.getBean( ExternalFileGeneLoaderService.class );

        try {
            int count = loader.load( directGeneInputFileName, taxonName );
            System.out.println( count + " genes loaded successfully " );
        } catch ( IOException e ) {
            System.out.println( "File could not be read: " + e.getMessage() );
            throw new RuntimeException( e );
        } catch ( IllegalArgumentException e ) {
            System.out
                    .println( "One of the programme arguments were incorrect check gene file is in specified location and taxon is in system."
                            + e.getMessage() );
            throw new RuntimeException( e );
        } catch ( Exception e ) {
            System.out.println( "Gene file persisting error: " + e.getMessage() );
            throw new RuntimeException( e );
        }

    }

}
