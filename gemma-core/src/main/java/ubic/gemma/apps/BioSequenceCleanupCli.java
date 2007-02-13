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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Goes through the biosequences in the database and removes duplicates.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BioSequenceCleanupCli extends AbstractSpringAwareCLI {

    private boolean justTesting = true;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option justTestingOption = OptionBuilder.withArgName( "tryout mode" ).withDescription(
                "Set to run without any database modifications" ).create( 'f' );
        addOption( justTestingOption );
    }

    public static void main( String[] args ) {
        BioSequenceCleanupCli p = new BioSequenceCleanupCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            this.justTesting = true;
            log.info( "TEST MODE: NO DATABASE UPDATES WILL BE PERFORMED" );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Sequence cleanup", args );
        if ( err != null ) return err;

        BioSequenceService bss = ( BioSequenceService ) this.getBean( "bioSequenceService" );
        ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        CompositeSequenceService css = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );

        Collection<ArrayDesign> ads = adService.loadAll();
        int i = 0;
        for ( ArrayDesign design : ads ) {
            log.info( design );
            adService.thaw( design );
            for ( CompositeSequence cs : design.getCompositeSequences() ) {

                BioSequence bs = cs.getBiologicalCharacteristic();
                Collection<BioSequence> seqs = bss.findByName( bs.getName() );
                if ( seqs.size() == 2 ) {
                    // make sure they really are duplicates
                    Iterator<BioSequence> it = seqs.iterator();
                    BioSequence bs1 = it.next();
                    BioSequence bs2 = it.next();

                    if ( !bs1.equals( bs2 ) ) continue;

                    if ( log.isDebugEnabled() ) log.debug( "Duplicates: " + bs1 + " " + bs2 );

                    /*
                     * Important! This assumes that the only use of a biosequence is as a biologicalcharactersitic; if
                     * that changes this will break.
                     */
                    // then load all composite sequences for this.
                    Collection<CompositeSequence> havingDupSeqs = css.findByBioSequenceName( bs.getName() );

                    if ( havingDupSeqs.size() == 1 ) {
                        /*
                         * Remove the other sequence.
                         */
                        log.info( "Deleting unused duplicate sequence " + bs2 );
                        if ( !justTesting ) bss.remove( bs2 );
                    } else {
                        /*
                         * Switch references from bs2 to bs1.
                         */
                        for ( CompositeSequence sequence : havingDupSeqs ) {

                            if ( sequence.getBiologicalCharacteristic().equals( bs1 ) ) {
                                // no problem.
                            } else if ( sequence.getBiologicalCharacteristic().equals( bs2 ) ) {
                                log.info( "Switching bioseq for " + sequence + " from " + bs2 + " to " + bs1 );
                                if ( !justTesting ) sequence.setBiologicalCharacteristic( bs1 );
                                if ( !justTesting ) css.update( sequence );
                            } else {
                                log.warn( "Hmm, " + sequence + " doesn't have seq " + bs1 + " or " + bs2
                                        + " but name matches: " + sequence.getBiologicalCharacteristic() );
                            }
                        }

                        log.info( "Deleting duplicate sequence " + bs2 );
                        if ( !justTesting ) bss.remove( bs2 );
                    }

                } else if ( seqs.size() > 2 ) {
                    log.warn( seqs.size() - 1 + " potential duplicates of " + bs
                            + ", this program can only handle pairs" );
                }

                if ( ++i % 2000 == 0 ) {
                    log.info( "Processed " + i );
                }
            }

        }

        return null;

    }
}
