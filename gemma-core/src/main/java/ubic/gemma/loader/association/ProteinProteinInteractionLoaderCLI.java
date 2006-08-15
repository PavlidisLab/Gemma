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
package ubic.gemma.loader.association;

import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.association.ProteinProteinInteractionDao;
import ubic.gemma.model.genome.gene.GeneProductDao;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Command line interface to retrieve and load literature associations
 * 
 * @author anshu
 * @version $Id$
 */
public class ProteinProteinInteractionLoaderCLI extends AbstractSpringAwareCLI {

    private static final String USAGE = "ppiLoader [options] ";

    // private PersisterHelper mPersister;

    public static void main( String[] args ) {
        ProteinProteinInteractionLoaderCLI p = new ProteinProteinInteractionLoaderCLI();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    public Exception doWork( String args[] ) {

        try {
            ProteinProteinInteractionLoaderCLI cl = new ProteinProteinInteractionLoaderCLI();
            cl.processCommandLine( USAGE, args );

        } catch ( Exception e ) {
            return e;
        }

        return null;

    }

    @Override
    protected void buildOptions() {
        /* load */
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Specify file (requires file arg) and load database" );
        Option loadOpt = OptionBuilder.create( 'l' );

        /* remove */
        OptionBuilder.withDescription( "Remove protein-protein interactions from database" );
        Option removeOpt = OptionBuilder.create( 'r' );

        addOption( loadOpt );
        addOption( removeOpt );

    }

    @Override
    protected void processOptions() {

        super.processOptions();

        GeneProductDao gpDao = ( GeneProductDao ) ctx.getBean( "geneProductDao" );
        ProteinProteinInteractionDao ppiDao = ( ProteinProteinInteractionDao ) ctx
                .getBean( "ProteinProteinInteractionDao" );

        PPIFileParser assocParser = new PPIFileParser( gpDao, ppiDao );

        // interrogation stage
        if ( hasOption( 'l' ) ) {

            String filename = getOptionValue( 'l' );

            try {
                assocParser.parse( filename );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

        } else if ( hasOption( 'r' ) ) {
            assocParser.removeAll();
        }

    }
}
