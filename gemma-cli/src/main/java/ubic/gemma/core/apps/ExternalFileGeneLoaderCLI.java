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

package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.genome.gene.ExternalFileGeneLoaderService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.CLI;

import java.io.IOException;

/**
 * CLI for loading genes from a non NCBI files. A taxon and gene file should be supplied as command line arguments. File
 * should be in tab delimited format containing gene symbol, gene name, uniprot id in that order.
 *
 * @author ldonnison
 */
public class ExternalFileGeneLoaderCLI extends AbstractAuthenticatedCLI {

    @Autowired
    private ExternalFileGeneLoaderService loader;

    private String directGeneInputFileName = null;
    private String taxonName;

    public ExternalFileGeneLoaderCLI() {
        setRequireLogin();
    }

    @Override
    public String getCommandName() {
        return "loadGenesFromFile";
    }

    @Override
    public String getShortDesc() {
        return "loading genes from a non-NCBI files; only used for species like salmon";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.SYSTEM;
    }

    /**
     * This method is called at the end of processCommandLine
     */
    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'f' ) ) {
            directGeneInputFileName = commandLine.getOptionValue( 'f' );
            if ( directGeneInputFileName == null ) {
                throw new IllegalArgumentException( "No gene input file provided " );
            }
        }
        if ( commandLine.hasOption( 't' ) ) {
            this.taxonName = commandLine.getOptionValue( 't' );
            if ( taxonName == null ) {
                throw new IllegalArgumentException( "No taxon name supplied " );
            }
        }

    }

    @Override
    protected void buildOptions( Options options ) {
        Option directGene = Option.builder( "f" )
                .desc( "Tab delimited format containing gene symbol, gene name, uniprot id in that order" )
                .hasArg().argName( "file" ).build();
        options.addOption( directGene );

        Option taxonNameOption = Option.builder( "t" ).hasArg()
                .desc( "Taxon common name e.g. 'salmonoid'; does not have to be a species " ).build();
        options.addOption( taxonNameOption );
    }

    /**
     * Main entry point to service class which reads a gene file and persists the genes in that file.
     */
    @Override
    protected void doAuthenticatedWork() throws Exception {
        try {
            int count = loader.load( directGeneInputFileName, taxonName );
            System.out.println( count + " genes loaded successfully " );
        } catch ( IOException e ) {
            System.out.println( "File could not be read: " + e.getMessage() );
            throw new RuntimeException( e );
        } catch ( IllegalArgumentException e ) {
            System.out.println(
                    "One of the programme arguments were incorrect check gene file is in specified location and taxon is in system."
                            + e.getMessage() );
            throw new RuntimeException( e );
        } catch ( Exception e ) {
            System.out.println( "Gene file persisting error: " + e.getMessage() );
            throw new RuntimeException( e );
        }
    }
}
