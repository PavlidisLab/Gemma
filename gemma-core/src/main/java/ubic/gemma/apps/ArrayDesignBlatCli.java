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
package ubic.gemma.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * Command line interface to run blat on the sequences for a microarray; the results are persisted in the DB. You must
 * start the BLAT server first before using this.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignBlatCli extends ArrayDesignSequenceManipulatingCli {
    ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;

    String blatResultFile = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option blatResultOption = OptionBuilder.hasArg().withArgName( "PSL file" ).withDescription(
                "Blat result file in PSL format (if supplied, BLAT will not be run)" ).withLongOpt( "blatfile" )
                .create( 'b' );

        addOption( blatResultOption );
    }

    public static void main( String[] args ) {
        ArrayDesignBlatCli p = new ArrayDesignBlatCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design sequence BLAT - only works if server is already started!",
                args );
        if ( err != null ) return err;

        Taxon taxon = taxonService.findByCommonName( commonName );

        if ( taxon == null ) {
            log.error( "No taxon " + commonName + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }

        ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

        unlazifyArrayDesign( arrayDesign );
        Collection<BlatResult> persistedResults;
        try {
            if ( this.blatResultFile != null ) {
                File f = new File( blatResultFile );
                if ( !f.canRead() ) {
                    log.error( "Cannot read from " + blatResultFile );
                    bail( ErrorCode.INVALID_OPTION );
                }

                log.info( "Reading blat results in from " + f.getAbsolutePath() );
                BlatResultParser parser = new BlatResultParser();
                parser.setTaxon( taxon );
                parser.parse( f );
                Collection<BlatResult> blatResults = parser.getResults();

                if ( blatResults == null || blatResults.size() == 0 ) {
                    throw new IllegalStateException( "No blat results in file!" );
                }

                log.info( "Got " + blatResults.size() + " blat records" );
                persistedResults = arrayDesignSequenceAlignmentService.processArrayDesign( arrayDesign, blatResults,
                        taxon );
            } else {
                persistedResults = arrayDesignSequenceAlignmentService.processArrayDesign( arrayDesign, taxon );
            }
        } catch ( FileNotFoundException e ) {
            return e;
        } catch ( IOException e ) {
            return e;
        }

        log.info( "Persisted " + persistedResults.size() + " results" );

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 'b' ) ) {
            this.blatResultFile = this.getOptionValue( 'b' );
        }

        arrayDesignSequenceAlignmentService = ( ArrayDesignSequenceAlignmentService ) this
                .getBean( "arrayDesignSequenceAlignmentService" );

    }

}
