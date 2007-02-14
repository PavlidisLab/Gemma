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
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;

/**
 * Goes through the biosequences for array designs in the database and removes duplicates.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BioSequenceCleanupCli extends ArrayDesignSequenceManipulatingCli {

    private boolean justTesting = false;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
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
        CompositeSequenceService css = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );

        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        if ( this.arrayDesignName != null ) {
            ads.add( locateArrayDesign( this.arrayDesignName ) );
        } else {
            ads = this.arrayDesignService.loadAll();
        }
        int i = 0;
        for ( ArrayDesign design : ads ) {
            log.info( design );
            unlazifyArrayDesign( design );
            dups: for ( CompositeSequence cs : design.getCompositeSequences() ) {

                BioSequence anchorSeq = cs.getBiologicalCharacteristic();
                if ( anchorSeq == null ) {
                    continue;
                }
                Collection<BioSequence> seqs = bss.findByName( anchorSeq.getName() );

                // no evidence of duplicates?
                if ( seqs.size() == 1 ) {
                    continue;
                }

                seqs.remove( anchorSeq );

                // ensure this group really does contain all duplicates.
                if ( log.isDebugEnabled() )
                    log.debug( "Examining set of " + seqs.size() + " possible duplicates of " + anchorSeq );

                Collection<BioSequence> notDuplicate = new HashSet<BioSequence>();
                for ( BioSequence candidateForRemoval : seqs ) {
                    if ( log.isDebugEnabled() ) log.debug( "   Examining: " + candidateForRemoval );
                    assert !candidateForRemoval.equals( anchorSeq ) : candidateForRemoval + " equals " + anchorSeq;
                    if ( !this.equals( anchorSeq, candidateForRemoval ) ) {
                        notDuplicate.add( candidateForRemoval );
                    } else {
                        if ( log.isDebugEnabled() )
                            log.debug( "    Duplicate: " + anchorSeq + " " + candidateForRemoval );
                    }
                }

                seqs.removeAll( notDuplicate );

                for ( BioSequence toChange : seqs ) {
                    if ( log.isDebugEnabled() ) log.debug( "Processing " + toChange );

                    assert this.equals( anchorSeq, toChange );

                    /*
                     * Important! This assumes that the only use of a biosequence is as a biologicalcharactersitic; if
                     * that changes this will break.
                     */

                    // all composite sequences for bs2 will be switched to bs1.
                    Collection<CompositeSequence> usingDuplicatedSequence = css.findByBioSequence( toChange );

                    css.thaw( usingDuplicatedSequence );

                    for ( CompositeSequence sequence : usingDuplicatedSequence ) {

                        log.info( "Switching bioseq for " + sequence + " on " + sequence.getArrayDesign() + " from "
                                + toChange + " to " + anchorSeq );
                        if ( !justTesting ) sequence.setBiologicalCharacteristic( anchorSeq );
                        if ( !justTesting ) css.update( sequence );

                    }

                    /*
                     * Remove the other sequence.
                     */
                    log.info( "Deleting unused duplicate sequence " + toChange );
                    if ( !justTesting ) bss.remove( toChange );

                    if ( ++i % 2000 == 0 ) {
                        log.info( "Processed " + i );
                    }
                }
            }
        }

        return null;

    }

    /**
     * Test whether two sequences are effectively equal (ignore the ID)
     * 
     * @param one
     * @param that
     * @return
     */
    private boolean equals( BioSequence one, BioSequence that ) {

        if ( one.getSequenceDatabaseEntry() != null
                && that.getSequenceDatabaseEntry() != null
                && !one.getSequenceDatabaseEntry().getAccession().equals(
                        that.getSequenceDatabaseEntry().getAccession() ) ) return false;

        if ( one.getTaxon() != null && that.getTaxon() != null && !one.getTaxon().equals( that.getTaxon() ) )
            return false;

        if ( one.getName() != null && that.getName() != null && !one.getName().equals( that.getName() ) ) return false;

        if ( one.getSequence() != null && that.getSequence() != null && !one.getSequence().equals( that.getSequence() ) )
            return false;

        return true;
    }

}
