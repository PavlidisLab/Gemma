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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;

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
        Option justTestingOption = OptionBuilder.withLongOpt( "tryout" ).withDescription(
                "Set to run without any database modifications" ).create( 'f' );
        addOption( justTestingOption );

        Option sequenceNameList = OptionBuilder.hasArg().withArgName( "file" ).withDescription(
                "File with list of biosequence ids to check." ).create( 'b' );
        addOption( sequenceNameList );
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

    BioSequenceService bss;
    CompositeSequenceService css;
    BlatResultService blatResultService;
    BlatAssociationService blatAssociationService;
    private String file = null;

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            this.justTesting = true;
            log.info( "TEST MODE: NO DATABASE UPDATES WILL BE PERFORMED" );
        }

        if ( this.hasOption( 'b' ) ) {
            this.file = ( String ) getOptionValue( 'b' );
        }

        bss = ( BioSequenceService ) this.getBean( "bioSequenceService" );
        css = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );
        blatResultService = ( BlatResultService ) this.getBean( "blatResultService" );
        blatAssociationService = ( BlatAssociationService ) this.getBean( "blatAssociationService" );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Sequence cleanup", args );
        if ( err != null ) return err;

        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        if ( this.arrayDesignName != null ) {
            ads.add( locateArrayDesign( this.arrayDesignName ) );
        } else if ( file != null ) {
            try {
                InputStream is = new FileInputStream( file );
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

                String id = null;
                Collection<Long> ids = new HashSet<Long>();
                while ( ( id = br.readLine() ) != null ) {

                    if ( StringUtils.isBlank( id ) ) {
                        continue;
                    }
                    ids.add( Long.parseLong( id ) );
                }

                Collection bioSequences = bss.loadMultiple( ids );
                bss.thaw( bioSequences );
                processSequences( bioSequences );
                return null;
            } catch ( Exception e ) {
                return e;
            }
        } else {
            ads = this.arrayDesignService.loadAll();
        }

        int i = 0;
        for ( ArrayDesign design : ads ) {
            log.info( design );
            unlazifyArrayDesign( design );

            Collection<BioSequence> bioSequences = new HashSet<BioSequence>();

            for ( CompositeSequence cs : design.getCompositeSequences() ) {
                if ( cs == null ) continue;
                if ( cs.getBiologicalCharacteristic() == null ) continue;
                bioSequences.add( cs.getBiologicalCharacteristic() );
            }

            processSequences( bioSequences );

            // ///////////////////////////////
            // Second phase: make sure composite sequences don't refer to sequences that have duplicates based on name,
            // using stricter equality criteria.
            for ( CompositeSequence cs : design.getCompositeSequences() ) {

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

                this.bss.thaw( seqs );

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
                    if ( !this.equals( anchorSeq, toChange ) ) {
                        throw new IllegalStateException( "Sequences weren't equal " + anchorSeq + " and " + toChange );
                    }
                    switchAndDeleteExtra( anchorSeq, toChange );

                    if ( ++i % 2000 == 0 ) {
                        log.info( "Processed " + i );
                    }
                }
            }
        }

        return null;

    }

    /**
     * @param bioSequences
     */
    @SuppressWarnings("unchecked")
    private void processSequences( Collection<BioSequence> bioSequences ) {
        // ///////////////////////////////
        // First stage: fix biosequences that lack database entries, when there is one for another essentially
        // identical sequence (and the name is the same as the accession)
        for ( BioSequence sequence : bioSequences ) {

            Collection<BioSequence> reps = bss.findByName( sequence.getName() );

            if ( reps.size() == 1 ) continue;

            bss.thaw( reps );

            // pass 1: find an anchor.
            BioSequence anchor = null;
            for ( BioSequence possibleAnchor : reps ) {
                if ( possibleAnchor.getSequenceDatabaseEntry() != null
                        && possibleAnchor.getSequenceDatabaseEntry().getAccession().equals( possibleAnchor.getName() ) ) {
                    anchor = possibleAnchor;
                }

            }

            if ( anchor == null ) continue;
            reps.remove( anchor );

            log.info( "Examining duplicates of " + anchor );

            for ( BioSequence rep : reps ) {
                if ( rep.getSequenceDatabaseEntry() != null ) {

                    if ( rep.getSequenceDatabaseEntry().getAccession().equals(
                            anchor.getSequenceDatabaseEntry().getAccession() ) ) {
                        log.warn( anchor + " and " + rep + " have equivalent database entries for accession" );

                        // they might have different names, but we don't care. One of them has to go.

                    } else {
                        log.warn( anchor + " and " + rep + " have distinct database entries for accession: skipping" );
                        continue;
                    }

                }

                log.info( sequence + " has a potential replica: " + rep );

                // only do this if they have the same name
                // if ( !rep.getName().equals( anchor.getName() ) ) {
                // log.warn( rep + " and " + anchor + " have different names, skipping" );
                // continue;
                // }

                switchAndDeleteExtra( anchor, rep );

            }
        }
    }

    @SuppressWarnings("unchecked")
    private void switchAndDeleteExtra( BioSequence keeper, BioSequence toRemove ) {

        // all composite sequences for bs2 will be switched to bs1.
        Collection<CompositeSequence> usingDuplicatedSequence = css.findByBioSequence( toRemove );

        css.thaw( usingDuplicatedSequence );

        for ( CompositeSequence sequence : usingDuplicatedSequence ) {

            log.info( "Switching bioseq for " + sequence + " on " + sequence.getArrayDesign() + " from " + toRemove
                    + " to " + keeper );
            if ( !justTesting ) sequence.setBiologicalCharacteristic( keeper );
            if ( !justTesting ) css.update( sequence );

        }

        Collection<BlatResult> blatResults = blatResultService.findByBioSequence( toRemove );

        for ( BlatResult br : blatResults ) {
            if ( !justTesting ) br.setQuerySequence( keeper );
            if ( !justTesting ) blatResultService.update( br );
        }

        Collection<BlatAssociation> bs2gps = blatAssociationService.find( toRemove );

        for ( BlatAssociation bs2gp : bs2gps ) {
            if ( !justTesting ) bs2gp.setBioSequence( keeper );
            if ( !justTesting ) blatAssociationService.update( bs2gp );
        }

        /*
         * Remove the other sequence.
         */
        log.info( "Deleting unused duplicate sequence " + toRemove );
        if ( !justTesting ) bss.remove( toRemove );
    }

    /**
     * Test whether two sequences are effectively equal (ignore the ID)
     * 
     * @param one
     * @param that
     * @return
     */
    private boolean equals( BioSequence one, BioSequence that ) {

        bss.thaw( one );
        bss.thaw( that );

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
