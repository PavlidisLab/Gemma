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
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

/**
 * Goes through the biosequences for array designs in the database and removes duplicates.
 * <p>
 * Moved from GemmaAnalysis as this is a database maintenance tool
 *
 * @author pavlidis
 */
public class BioSequenceCleanupCli extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private BlatAssociationService blatAssociationService;
    @Autowired
    private BlatResultService blatResultService;
    @Autowired
    private BioSequenceService bss;
    @Autowired
    private CompositeSequenceService css;

    private String file = null;
    private boolean justTesting = false;

    @Override
    public String getCommandName() {
        return "seqCleanup";
    }

    @Override
    public String getShortDesc() {
        return "Examines biosequences for array designs in the database and removes duplicates.";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );

        Option justTestingOption = Option.builder( "dryrun" )
                .desc( "Set to run without any database modifications" )
                .build();
        options.addOption( justTestingOption );

        Option sequenceNameList = Option.builder( "b" )
                .longOpt( "file" )
                .desc( "File with list of biosequence ids to check; default: check all on provided platforms" )
                .build();
        options.addOption( sequenceNameList );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( "dryrun" ) ) {
            this.justTesting = true;
            log.info( "TEST MODE: NO DATABASE UPDATES WILL BE PERFORMED" );
        }

        if ( commandLine.hasOption( 'b' ) ) {
            this.file = commandLine.getOptionValue( 'b' );
        }
    }

    @Override
    protected void doAuthenticatedWork() {

        Collection<ArrayDesign> ads;
        if ( !this.getArrayDesignsToProcess().isEmpty() ) {
            ads = new HashSet<>( this.getArrayDesignsToProcess() );
        } else if ( file != null ) {
            try ( InputStream is = new FileInputStream( file );
                    BufferedReader br = new BufferedReader( new InputStreamReader( is ) ); ) {

                String id = null;
                Collection<Long> ids = new HashSet<>();
                while ( ( id = br.readLine() ) != null ) {
                    if ( StringUtils.isBlank( id ) ) {
                        continue;
                    }
                    ids.add( Long.parseLong( id ) );
                }

                Collection<BioSequence> bioSequences = bss.load( ids );
                bioSequences = bss.thaw( bioSequences );
                processSequences( bioSequences );
                return;
            } catch ( Exception e ) {
                return;
            }
        } else {
            ads = this.getArrayDesignService().loadAll();
        }

        ads = getArrayDesignService().thaw( ads );
        for ( ArrayDesign design : ads ) {
            log.info( design );

            Collection<BioSequence> bioSequences = new HashSet<>();

            for ( CompositeSequence cs : design.getCompositeSequences() ) {
                if ( cs == null ) continue;
                if ( cs.getBiologicalCharacteristic() == null ) continue;
                bioSequences.add( cs.getBiologicalCharacteristic() );
            }

            processSequences( bioSequences ); // fast.

            log.info( "Phase II starting" );

            // ///////////////////////////////
            // Second phase: make sure composite sequences don't refer to sequences that have duplicates based on name,
            // using stricter equality criteria.
            int i = 0;
            for ( CompositeSequence cs : design.getCompositeSequences() ) {

                if ( ++i % 500 == 0 ) {
                    log.info( "Processing: " + i + "/" + bioSequences.size() + " sequences" );
                }

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

                seqs = this.bss.thaw( seqs );

                // ensure this group really does contain all duplicates.
                if ( log.isDebugEnabled() )
                    log.debug( "Examining set of " + seqs.size() + " possible duplicates of " + anchorSeq );

                Collection<BioSequence> notDuplicate = new HashSet<>();
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

                }
            }
        }
    }

    /**
     * Test whether two sequences are effectively equal (ignore the ID)
     *
     */
    private boolean equals( BioSequence one, BioSequence that ) {

        one = bss.thaw( one );
        that = bss.thaw( that );

        if ( one.getSequenceDatabaseEntry() != null
                && that.getSequenceDatabaseEntry() != null
                && !one.getSequenceDatabaseEntry().getAccession()
                .equals( that.getSequenceDatabaseEntry().getAccession() ) )
            return false;

        if ( one.getTaxon() != null && that.getTaxon() != null && !one.getTaxon().equals( that.getTaxon() ) )
            return false;

        if ( one.getName() != null && that.getName() != null && !one.getName().equals( that.getName() ) ) return false;

        if ( one.getSequence() != null && that.getSequence() != null && !one.getSequence().equals( that.getSequence() ) )
            return false;

        return true;
    }

    /**
     */
    private void processSequences( Collection<BioSequence> bioSequences ) {
        // ///////////////////////////////
        // First stage: fix biosequences that lack database entries, when there is one for another essentially
        // identical sequence (and the name is the same as the accession)
        int i = 0;
        int possdups = 0;
        int skipped = 0;
        for ( BioSequence sequence : bioSequences ) {

            Collection<BioSequence> reps = bss.findByName( sequence.getName() );
            i++;

            if ( reps.size() == 1 ) continue; // no duplicates

            possdups++;

            if ( i % 500 == 0 ) {
                log.info( "Examined " + i + " sequences, found " + possdups + " cases of possible duplicates so far; " + skipped
                        + " cases were skipped" );
            }

            reps = bss.thaw( reps );

            // pass 1: find an anchor. Ideally it has the actual sequence filled in
            BioSequence anchor = null;
            for ( BioSequence possibleAnchor : reps ) {
                if ( possibleAnchor.getSequenceDatabaseEntry() != null
                        && possibleAnchor.getSequenceDatabaseEntry().getAccession().equals( possibleAnchor.getName() ) ) {

                    if ( StringUtils.isNotBlank( possibleAnchor.getSequence() ) ) {
                        anchor = possibleAnchor;
                    } else if ( anchor == null ) {
                        anchor = possibleAnchor;
                    }

                }

            }

            if ( anchor == null ) {
                log.warn( "\t No anchoring sequence was found for " + sequence + ", skipping" );
                skipped++;
                continue;
            }

            if ( StringUtils.isBlank( anchor.getSequence() ) ) {
                log.warn( "\tNone of the possible duplicates have a sequence filled in, skipping" );
                skipped++;
                continue;
            }

            reps.remove( anchor );

            log.info( "*** Examining duplicates of " + anchor );
            for ( BioSequence rep : reps ) {
                if ( rep.getSequenceDatabaseEntry() != null ) {

                    if ( rep.getSequenceDatabaseEntry().getAccession()
                            .equals( anchor.getSequenceDatabaseEntry().getAccession() ) ) {
                        log.warn( "\t" + anchor + " and " + rep + " have equivalent database entries for accession" );

                        // they might have different names, but we don't care. One of them has to go.

                    } else {
                        log.warn( "\t" + anchor + " and " + rep + " have distinct database entries for accession: skipping" );
                        skipped++;
                        continue;
                    }

                }

                log.info( "\t" + sequence + " has a potential duplicate: " + rep );

                switchAndDeleteExtra( anchor, rep );

            }
        }

    }

    /**
     */
    private void switchAndDeleteExtra( BioSequence keeper, BioSequence toRemove ) {

        // all composite sequences for bs2 will be switched to bs1.
        Collection<CompositeSequence> usingDuplicatedSequence = css.findByBioSequence( toRemove );

        usingDuplicatedSequence = css.thaw( usingDuplicatedSequence );

        for ( CompositeSequence sequence : usingDuplicatedSequence ) {

            log.info( "\tSwitching bioseq for " + sequence + " on " + sequence.getArrayDesign() + " from " + toRemove
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
            if ( !justTesting ) blatAssociationService.remove( bs2gp );
        }

        /*
         * This will fail with platforms that use AnnotationAssocations. That's easily fixed, but I prefer it this way
         * because those aren't real sequences.
         */

        /*
         * Remove the other sequence.
         */
        log.info( "\tDeleting unused duplicate sequence " + toRemove );
        if ( !justTesting ) {
            bss.remove( toRemove );
        }
    }
}
