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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.Blat;
import ubic.gemma.core.analysis.sequence.ProbeMapUtils;
import ubic.gemma.core.analysis.sequence.SequenceBinUtils;
import ubic.gemma.core.analysis.sequence.ShellDelegatingBlat;
import ubic.gemma.core.goldenpath.GoldenPathQuery;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Aligns sequences from array designs to the genome, using blat, and persists the blat results.
 * Note: to avoid having very long transactions, this does not run transactionally at the level of a platform. Thus it
 * is possible for it to die with a platform half-processed. But if we don't do this the transactions are too big and
 * cause various deadlocking problems.
 *
 * @author pavlidis
 */
@Component

public class ArrayDesignSequenceAlignmentServiceImpl implements ArrayDesignSequenceAlignmentService {

    private static final Log log = LogFactory.getLog( ArrayDesignSequenceAlignmentServiceImpl.class.getName() );

    private final ArrayDesignReportService arrayDesignReportService;
    private final ArrayDesignService arrayDesignService;
    private final BioSequenceService bioSequenceService;
    private final Persister persisterHelper;

    @Autowired
    public ArrayDesignSequenceAlignmentServiceImpl( ArrayDesignReportService arrayDesignReportService,
            ArrayDesignService arrayDesignService, BioSequenceService bioSequenceService, Persister persisterHelper ) {
        this.arrayDesignReportService = arrayDesignReportService;
        this.arrayDesignService = arrayDesignService;
        this.bioSequenceService = bioSequenceService;
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param ad platform
     * @return all sequences, across all taxa that might be represented on the array design
     */
    public static Collection<BioSequence> getSequences( ArrayDesign ad ) {
        return ArrayDesignSequenceAlignmentServiceImpl.getSequences( ad, null );
    }

    /**
     * @param ad    platform
     * @param taxon (specified in case array has multiple taxa)
     * @return bio sequences
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static Collection<BioSequence> getSequences( ArrayDesign ad, Taxon taxon ) {

        Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
        Collection<BioSequence> sequencesToBlat = new HashSet<>();
        int numWithNoBioSequence = 0;
        int numWithNoSequenceData = 0;
        boolean warned = false;
        for ( CompositeSequence cs : compositeSequences ) {
            BioSequence bs = cs.getBiologicalCharacteristic();

            if ( !warned && ( numWithNoBioSequence > 20 || numWithNoSequenceData > 20 ) ) {
                warned = true;
                ArrayDesignSequenceAlignmentServiceImpl.log
                        .warn( "More than 20 composite sequences don't have sequence information, no more warnings..." );
            }

            if ( bs == null ) {
                ++numWithNoBioSequence;
                if ( !warned ) {
                    ArrayDesignSequenceAlignmentServiceImpl.log.warn( cs + " had no associated biosequence object" );
                }
                continue;
            }

            if ( bs.getTaxon() == null ) {
                warned = true;
                ArrayDesignSequenceAlignmentServiceImpl.log.warn( "There is no taxon defined for this biosequence " );
                continue;
            }
            // if the taxon is null that means we want this run for all taxa for that array
            if ( taxon != null && !bs.getTaxon().equals( taxon ) ) {
                continue;
            }

            //noinspection ConstantConditions // Better readability
            if ( !warned && ( numWithNoBioSequence > 20 || numWithNoSequenceData > 20 ) ) {
                warned = true;
                ArrayDesignSequenceAlignmentServiceImpl.log
                        .warn( "More than 20 composite sequences don't have sequence information, no more warnings..." );
            }

            if ( StringUtils.isBlank( bs.getSequence() ) ) {
                ++numWithNoSequenceData;
                if ( !warned ) {
                    ArrayDesignSequenceAlignmentServiceImpl.log
                            .warn( cs + " had " + bs + " but no sequence, skipping" );
                }
                continue;
            }
            sequencesToBlat.add( bs );

        }
        if ( numWithNoBioSequence > 0 || numWithNoSequenceData > 0 ) {
            ArrayDesignSequenceAlignmentServiceImpl.log
                    .warn( numWithNoBioSequence + " composite sequences lacked biosequence associations; "
                            + numWithNoSequenceData + " lacked sequence data ( out of " + compositeSequences.size()
                            + " total)." );
        }
        return sequencesToBlat;
    }

    @Override
    public Collection<BlatResult> processArrayDesign( ArrayDesign design, boolean sensitive ) {
        return this.processArrayDesign( design, sensitive, null );
    }

    @Override
    public Collection<BlatResult> processArrayDesign( ArrayDesign ad, Taxon taxon,
            Collection<BlatResult> rawBlatResults ) {

        ArrayDesignSequenceAlignmentServiceImpl.log.info( "Looking for old results to remove..." );

        ad = arrayDesignService.thaw( ad );

        arrayDesignService.deleteAlignmentData( ad );
        // Blat file processing can only be run on one taxon at a time
        taxon = this.validateTaxaForBlatFile( ad, taxon );

        Collection<BioSequence> sequencesToBlat = ArrayDesignSequenceAlignmentServiceImpl.getSequences( ad );
        sequencesToBlat = bioSequenceService.thaw( sequencesToBlat );

        // if the blat results were loaded from a file, we have to replace the
        // query sequences with the actual ones
        // attached to the array design. We have to do this by name because the
        // sequence name is what the files contain.
        // Note that if there is ambiguity there will be problems!
        Map<String, BioSequence> seqMap = new HashMap<>();
        for ( BioSequence bioSequence : sequencesToBlat ) {
            seqMap.put( bioSequence.getName(), bioSequence );
        }

        ExternalDatabase searchedDatabase = ShellDelegatingBlat.getSearchedGenome( taxon );

        Collection<BlatResult> toSkip = new HashSet<>();
        for ( BlatResult result : rawBlatResults ) {

            /*
             * If the sequences don't have ids, replace them with the actual sequences associated with the array design.
             */
            if ( result.getQuerySequence().getId() == null ) {
                String querySeqName = result.getQuerySequence().getName();
                BioSequence actualSequence = seqMap.get( querySeqName );
                if ( actualSequence == null ) {
                    ArrayDesignSequenceAlignmentServiceImpl.log
                            .debug( "Array design does not contain a sequence with name " + querySeqName );
                    toSkip.add( result );
                    continue;
                }
                result.setQuerySequence( actualSequence );
            } else {
                result.getQuerySequence().setTaxon( taxon );
            }

            result.setSearchedDatabase( searchedDatabase );
            try {
                FieldUtils.writeField( result.getTargetChromosome(), "taxon", taxon, true );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
            result.getTargetChromosome().getSequence().setTaxon( taxon );

        }

        if ( toSkip.size() > 0 ) {
            ArrayDesignSequenceAlignmentServiceImpl.log
                    .warn( toSkip.size() + " blat results were for sequences not on " + ad
                            + "; they will be ignored." );
            rawBlatResults.removeAll( toSkip );
        }

        Map<BioSequence, Collection<BlatResult>> goldenPathAlignments = new HashMap<>();
        this.getGoldenPathAlignments( sequencesToBlat, taxon, goldenPathAlignments );
        for ( BioSequence sequence : goldenPathAlignments.keySet() ) {
            rawBlatResults.addAll( goldenPathAlignments.get( sequence ) );
        }

        Collection<BlatResult> results = this.persistBlatResults( rawBlatResults );

        arrayDesignReportService.generateArrayDesignReport( ad.getId() );

        return results;
    }

    @Override
    public Taxon validateTaxaForBlatFile( ArrayDesign arrayDesign, Taxon taxon ) {

        if ( taxon == null ) {
            Collection<Taxon> taxaOnArray = arrayDesignService.getTaxa( arrayDesign );
            if ( taxaOnArray != null && taxaOnArray.size() == 1 && taxaOnArray.iterator().next() != null ) {
                return taxaOnArray.iterator().next();
            }
            throw new IllegalArgumentException(
                    ( taxaOnArray == null ? "?" : taxaOnArray.size() ) + " taxon found for " + arrayDesign
                            + " specify which taxon to run" );

        }
        return taxon;
    }

    @Override
    public Collection<BlatResult> processArrayDesign( ArrayDesign design ) {
        return this.processArrayDesign( design, false, null );
    }

    @Override
    public Collection<BlatResult> processArrayDesign( ArrayDesign design, Blat blat ) {
        return this.processArrayDesign( design, false, blat );
    }

    /**
     * If necessary, copy the sequence length information over from the blat result to the given sequence. This is often
     * needed when we get the blat results from golden path.
     *
     * @param sequence that may not have length information
     * @param result   used to get length information
     */
    private void copyLengthInformation( BioSequence sequence, BlatResult result ) {
        if ( result.getQuerySequence() != null && result.getQuerySequence().getLength() == null ) {
            long length = result.getQuerySequence().getLength();
            if ( sequence.getLength() == null )
                sequence.setLength( length );
            sequence.setIsApproximateLength( false );
            sequence.setDescription( StringUtil
                    .append( sequence.getDescription(), "Length information from GoldenPath annotations.", " -- " ) );
            bioSequenceService.update( sequence );
        }
    }

    /**
     * @param taxon whose database will be queried
     * @return Map of biosequences to collections of blat results.
     */
    private Map<BioSequence, Collection<BlatResult>> getAlignments( Collection<BioSequence> sequencesToBlat,
            boolean sensitive, Taxon taxon, Blat blat ) {

        Map<BioSequence, Collection<BlatResult>> results = new HashMap<>();
        sequencesToBlat = bioSequenceService.thaw( sequencesToBlat );
        try {

            Collection<BioSequence> needBlat = this.getGoldenPathAlignments( sequencesToBlat, taxon, results );

            if ( needBlat.size() > 0 ) {
                ArrayDesignSequenceAlignmentServiceImpl.log.info( "Running blat on " + needBlat.size() + " sequences" );
                Map<BioSequence, Collection<BlatResult>> moreResults = blat.blatQuery( needBlat, sensitive, taxon );
                results.putAll( moreResults );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return results;
    }

    /**
     * Check if there are alignment results in the goldenpath database, in which case we do not reanalyze the sequences.
     *
     * @param sequencesToBlat The full set of sequences that need analysis.
     * @param results         Will be stored here.
     * @return the sequences which ARE NOT found in goldenpath and which therefore DO need blat.
     */
    private Collection<BioSequence> getGoldenPathAlignments( Collection<BioSequence> sequencesToBlat, Taxon taxon,
            Map<BioSequence, Collection<BlatResult>> results ) {

        try ( GoldenPathQuery gpq = new GoldenPathQuery( taxon ) ) {
            Collection<BioSequence> needBlat = new HashSet<>();
            int count = 0;
            int totalFound = 0;
            for ( BioSequence sequence : sequencesToBlat ) {
                boolean found = false;
                if ( sequence.getSequenceDatabaseEntry() != null ) {
                    Collection<BlatResult> brs = gpq.findAlignments( sequence.getSequenceDatabaseEntry().getAccession() );

                    if ( brs != null && brs.size() > 0 ) {
                        for ( BlatResult result : brs ) {
                            this.copyLengthInformation( sequence, result );
                            result.setQuerySequence( sequence );
                        }
                        results.put( sequence, brs );
                        found = true;
                        totalFound++;
                    }
                }

                if ( ++count % 1000 == 0 && totalFound > 0 ) {
                    ArrayDesignSequenceAlignmentServiceImpl.log
                            .info( "Alignments in Golden Path database for " + totalFound + "/" + count
                                    + " checked so far." );
                }

                if ( !found ) {
                    needBlat.add( sequence );
                }

            }

            if ( totalFound > 0 ) {
                ArrayDesignSequenceAlignmentServiceImpl.log
                        .info( "Found " + totalFound + "/" + count + " alignments in Golden Path database" );
            }
            return needBlat;
        }
    }

    /**
     * @param brs, assumed to be from alignments to the genome for the array design (that is, we don't consider aligning
     *             mouse to human)
     */
    @SuppressWarnings("unchecked")
    private Collection<BlatResult> persistBlatResults( Collection<BlatResult> brs ) {

        Collection<Integer> seen = new HashSet<>();
        int duplicates = 0;
        for ( BlatResult br : brs ) {

            Integer hash = ProbeMapUtils.hashBlatResult( br );
            if ( seen.contains( hash ) ) {
                duplicates++;
                continue;
            }
            seen.add( hash );

            assert br.getQuerySequence() != null;
            assert br.getQuerySequence().getName() != null;
            Taxon taxon = br.getQuerySequence().getTaxon();
            assert taxon != null;

            try {
                FieldUtils.writeField( br.getTargetChromosome(), "taxon", taxon, true );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
            br.getTargetChromosome().getSequence().setTaxon( taxon );

            PhysicalLocation pl = br.getTargetAlignedRegion();
            if ( pl == null ) {
                pl = PhysicalLocation.Factory.newInstance();
                pl.setChromosome( br.getTargetChromosome() );
                pl.setNucleotide( br.getTargetStart() );
                assert br.getTargetEnd() != null && br.getTargetStart() != null;
                pl.setNucleotideLength( br.getTargetEnd().intValue() - br.getTargetStart().intValue() );
                pl.setStrand( br.getStrand() );
                br.setTargetAlignedRegion( pl );
                pl.setBin(
                        SequenceBinUtils.binFromRange( br.getTargetStart().intValue(), br.getTargetEnd().intValue() ) );
            }

        }

        if ( duplicates > 0 ) {
            ArrayDesignSequenceAlignmentServiceImpl.log.info( duplicates + " duplicate BLAT hits skipped" );
        }

        return ( Collection<BlatResult> ) persisterHelper.persist( brs );
    }

    private Collection<BlatResult> processArrayDesign( ArrayDesign ad, boolean sensitive, Blat blat ) {

        if ( blat == null )
            blat = new ShellDelegatingBlat();

        Collection<BlatResult> allResults = new HashSet<>();

        if ( sensitive )
            ArrayDesignSequenceAlignmentServiceImpl.log.info( "Running in 'sensitive' mode if possible" );

        Collection<Taxon> taxa = arrayDesignService.getTaxa( ad );
        boolean first = true;
        for ( Taxon taxon : taxa ) {

            Collection<BioSequence> sequencesToBlat = ArrayDesignSequenceAlignmentServiceImpl.getSequences( ad, taxon );

            Map<BioSequence, Collection<BlatResult>> results = this
                    .getAlignments( sequencesToBlat, sensitive, taxon, blat );

            ArrayDesignSequenceAlignmentServiceImpl.log
                    .info( "Got BLAT results for " + results.keySet().size() + " query sequences" );

            Map<String, BioSequence> nameMap = new HashMap<>();
            for ( BioSequence bs : results.keySet() ) {
                if ( nameMap.containsKey( bs.getName() ) ) {
                    throw new IllegalStateException(
                            "All distinct sequences on the array must have unique names; found " + bs.getName()
                                    + " more than once." );
                }
                nameMap.put( bs.getName(), bs );
            }

            int noResults = 0;
            int count = 0;

            // We only remove the results here, after we have at least one set of blat results.
            if ( first ) {
                ArrayDesignSequenceAlignmentServiceImpl.log.info( "Looking for old results to remove..." );
                arrayDesignService.deleteAlignmentData( ad );
            }

            for ( BioSequence sequence : sequencesToBlat ) {
                if ( sequence == null ) {
                    ArrayDesignSequenceAlignmentServiceImpl.log.warn( "Null sequence!" );
                    continue;
                }
                Collection<BlatResult> brs = results.get( nameMap.get( sequence.getName() ) );
                if ( brs == null ) {
                    ++noResults;
                    continue;
                }
                for ( BlatResult result : brs ) {
                    result.setQuerySequence( sequence ); // must do this to replace
                    // placeholder instance.
                }
                allResults.addAll( this.persistBlatResults( brs ) );

                if ( ++count % 2000 == 0 ) {
                    ArrayDesignSequenceAlignmentServiceImpl.log
                            .info( "Checked results for " + count + " queries, " + allResults.size()
                                    + " blat results so far." );
                }

            }

            ArrayDesignSequenceAlignmentServiceImpl.log
                    .info( noResults + "/" + sequencesToBlat.size() + " sequences had no blat results" );
            first = false;
        }

        arrayDesignReportService.generateArrayDesignReport( ad.getId() );

        return allResults;

    }
}
