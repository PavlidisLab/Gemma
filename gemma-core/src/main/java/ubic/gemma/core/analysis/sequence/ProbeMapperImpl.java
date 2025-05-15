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
package ubic.gemma.core.analysis.sequence;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysis;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;
import ubic.gemma.persistence.service.genome.ChromosomeUtils;

import java.io.IOException;
import java.util.*;

/**
 * Provides methods for mapping sequences to genes and gene products. Some methods accept a configuration object that
 * allows threshold etc. to be modified.
 *
 * @author pavlidis
 */
@Component
public class ProbeMapperImpl implements ProbeMapper {

    private static final int MAX_WARNINGS = 10;
    private final Log log = LogFactory.getLog( ProbeMapperImpl.class.getName() );
    private final ThreePrimeDistanceMethod threePrimeMethod = ThreePrimeDistanceMethod.RIGHT;

    @Override
    public Map<String, Collection<BlatAssociation>> processBlatResults( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<BlatResult> blatResults ) {
        return this.processBlatResults( goldenPathDb, blatResults, new ProbeMapperConfig() );
    }

    @Override
    public Map<String, Collection<BlatAssociation>> processBlatResults( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<BlatResult> blatResults, ProbeMapperConfig config ) {

        if ( log.isDebugEnabled() ) {
            log.debug( blatResults.size() + " Blat results to map " );
        }

        assert goldenPathDb != null;
        Map<String, Collection<BlatAssociation>> allRes = new HashMap<>();
        int count = 0;

        Map<BioSequence, Collection<BlatResult>> biosequenceToBlatResults = this
                .groupBlatResultsByBioSequence( blatResults );

        assert !biosequenceToBlatResults.isEmpty();

        // Do them one sequence at a time.
        for ( BioSequence sequence : biosequenceToBlatResults.keySet() ) {

            Collection<BlatResult> blatResultsForSequence = biosequenceToBlatResults.get( sequence );

            if ( log.isDebugEnabled() ) {
                log.debug( blatResultsForSequence.size() + " Blat results for " + sequence );
            }

            assert !blatResultsForSequence.isEmpty();

            Collection<BlatResult> trimmedBlatResultsForSequence = this
                    .trimNonCanonicalChromosomeHits( blatResultsForSequence, config );

            /*
             * Screen out likely repeats, where cross-hybridization is a problem.
             */
            Double fractionRepeats = sequence.getFractionRepeats();
            if ( fractionRepeats != null && fractionRepeats > config.getMaximumRepeatFraction()
                    && trimmedBlatResultsForSequence.size() >= config.getNonSpecificSiteCountThreshold() ) {
                if ( config.getWarnings() < ProbeMapperImpl.MAX_WARNINGS ) {
                    log.info( "Skipped " + sequence + " due to repeat content (" + fractionRepeats + ")" );
                    if ( config.getWarnings() == ProbeMapperImpl.MAX_WARNINGS )
                        log.info( "Further non-mappings will not be logged" );

                    config.incrementWarnings();
                }
                continue;
            }

            /*
             * Filter out sequences that have many high-quality alignments to the genome, even if it isn't a repeat. It
             * just seems like a good idea to reject probes that have too many alignment sites, even if only one of them
             * is to the site of a known gene.
             */
            if ( trimmedBlatResultsForSequence.size() >= config.getNonRepeatNonSpecificSiteCountThreshold() ) {
                if ( config.getWarnings() < ProbeMapperImpl.MAX_WARNINGS ) {
                    log.info( "Skipped " + sequence + " due to non-specificity (" + trimmedBlatResultsForSequence.size()
                            + " hits)" );
                    if ( config.getWarnings() == ProbeMapperImpl.MAX_WARNINGS )
                        log.info( "Further non-mappings will not be logged" );

                    config.incrementWarnings();
                }
                continue;
            }

            /*
             * Sanity check to make sure user is paying attention to what they are putting in.
             */
            if ( trimmedBlatResultsForSequence.size() > 25 ) {
                log.warn( sequence + " has " + trimmedBlatResultsForSequence.size()
                        + " blat associations (after trimming non-canonical chromosome hits)" );
            }

            Collection<BlatAssociation> blatAssociationsForSequence = new HashSet<>();
            int skipped = 0;
            // map each blat result.
            for ( BlatResult blatResult : trimmedBlatResultsForSequence ) {

                if ( blatResult.getQuerySequence() == null || ( blatResult.getQuerySequence().getLength() == null
                        && StringUtils.isBlank( blatResult.getQuerySequence().getSequence() ) ) ) {
                    log.warn( "Blat result had no sequence: " + blatResult );
                    continue;
                }

                assert BlatAssociationScorer.score( blatResult ) >= 0 && BlatAssociationScorer.score( blatResult ) <= 1.0 : "Score was " + BlatAssociationScorer.score( blatResult );
                assert BlatAssociationScorer.identity( blatResult ) >= 0 && BlatAssociationScorer.identity( blatResult ) <= 1.0 :
                        "Identity was " + BlatAssociationScorer.identity( blatResult );

                if ( BlatAssociationScorer.score( blatResult ) < config.getBlatScoreThreshold() || BlatAssociationScorer.identity( blatResult ) < config
                        .getIdentityThreshold() ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "Result for " + sequence + " skipped with score=" + BlatAssociationScorer.score( blatResult ) + " identity="
                                + BlatAssociationScorer.identity( blatResult ) );
                    skipped++;
                    continue;
                }

                if ( blatResult.getQuerySequence().getTaxon() == null ) {
                    Taxon taxon = goldenPathDb.getTaxon();
                    assert taxon != null;
                    blatResult.getQuerySequence().setTaxon( taxon );
                }

                // here's the key line! Find gene products that map to the given blat result.
                Collection<BlatAssociation> resultsForOneBlatResult = this
                        .processBlatResult( goldenPathDb, blatResult, config );

                if ( resultsForOneBlatResult != null && resultsForOneBlatResult.size() > 0 ) {

                    // would be very unlikely.
                    if ( resultsForOneBlatResult.size() > 100 ) {
                        log.warn( blatResult + " for " + sequence + " has " + resultsForOneBlatResult.size()
                                + " blat associations" );
                    }

                    blatAssociationsForSequence.addAll( resultsForOneBlatResult );
                }

                // there are rarely this many, but it does happen.
                if ( ++count % 100 == 0 && log.isDebugEnabled() )
                    log.debug( "Annotations computed for " + count + " blat results for " + sequence );

            } // end of iteration over results for this sequence.

            // Another important step: fill in the specificity, remove duplicates
            if ( !blatAssociationsForSequence.isEmpty() ) {
                BlatAssociationScorer.scoreResults( blatAssociationsForSequence );
                // Remove hits not meeting criteria
                blatAssociationsForSequence = this.filterOnScores( blatAssociationsForSequence, config );
            }

            // it might be empty now...
            if ( blatAssociationsForSequence.isEmpty() ) {
                if ( config.getWarnings() < ProbeMapperImpl.MAX_WARNINGS ) {
                    log.info( "No mappings for " + sequence + "; " + trimmedBlatResultsForSequence.size()
                            + " individual blat results checked; " + skipped
                            + " had identity or score below threshold, rest had no mapping" );
                    if ( config.getWarnings() == ProbeMapperImpl.MAX_WARNINGS )
                        log.info( "Further non-mappings will not be logged" );
                }
                config.incrementWarnings();
                continue;
            } else if ( log.isDebugEnabled() ) {
                log.debug( blatAssociationsForSequence.size() + " associations for " + sequence );
            }

            if ( skipped > 0 ) {
                if ( log.isDebugEnabled() )
                    log.debug( "Skipped " + skipped + "/" + blatResults.size()
                            + " individual blat results that didn't meet criteria" );
            }

            String queryName = sequence.getName();
            assert StringUtils.isNotBlank( queryName );
            if ( !allRes.containsKey( queryName ) ) {
                allRes.put( queryName, blatAssociationsForSequence );
            }
            allRes.get( queryName ).addAll( blatAssociationsForSequence );

            // if ( log.isDebugEnabled() ) {
            // log.info( blatAssociationsForSequence.size() + " associations for " + sequence
            // + " after redundancy reduction and score filtering; "
            // + ( scored - blatAssociationsForSequence.size() ) + " not used" );
            // }

        } // end of iteration over sequences

        return allRes;
    }

    @Override
    public Map<String, Collection<BlatAssociation>> processGbId( GoldenPathSequenceAnalysis goldenPathDb,
            String genbankId ) {

        log.debug( "Entering processGbId with " + genbankId );

        Collection<BlatResult> blatResults = goldenPathDb.findSequenceLocations( genbankId );

        if ( blatResults == null || blatResults.size() == 0 ) {
            log.warn( "No results obtained for " + genbankId );
        }

        return this.processBlatResults( goldenPathDb, blatResults );

    }

    @Override
    public Map<String, Collection<BlatAssociation>> processGbIds( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<String[]> genbankIds ) {
        Map<String, Collection<BlatAssociation>> allRes = new HashMap<>();
        int count = 0;
        for ( String[] genbankIdAr : genbankIds ) {

            if ( genbankIdAr == null || genbankIdAr.length == 0 ) {
                continue;
            }

            if ( genbankIdAr.length > 1 ) {
                throw new IllegalArgumentException( "Input file must have just one genbank identifier per line" );
            }

            String genbankId = genbankIdAr[0];

            Map<String, Collection<BlatAssociation>> res = this.processGbId( goldenPathDb, genbankId );
            allRes.putAll( res );

            count++;
            if ( count % 100 == 0 )
                log.info( "Annotations computed for " + count + " genbank identifiers" );
        }
        log.info( "Annotations computed for " + count + " genbank identifiers" );
        return allRes;
    }

    @Override
    public Collection<BlatAssociation> processSequence( GoldenPathSequenceAnalysis goldenPath, BioSequence sequence ) {

        Blat b = new ShellDelegatingBlat();
        b.setBlatScoreThreshold( ( new ProbeMapperConfig() ).getBlatScoreThreshold() );
        Collection<BlatResult> results;
        try {
            results = b.blatQuery( sequence, goldenPath.getTaxon(), false );
        } catch ( IOException e ) {
            throw new RuntimeException( "Error running blat", e );
        }
        Map<String, Collection<BlatAssociation>> allRes = this.processBlatResults( goldenPath, results );
        assert allRes.keySet().size() == 1;
        return allRes.values().iterator().next();
    }

    @Override
    public Map<String, Collection<BlatAssociation>> processSequences( GoldenPathSequenceAnalysis goldenpath,
            Collection<BioSequence> sequences, ProbeMapperConfig config ) {
        Blat b = new ShellDelegatingBlat();
        b.setBlatScoreThreshold( config.getBlatScoreThreshold() );

        try {
            Map<BioSequence, List<BlatResult>> results = b.blatQuery( sequences, goldenpath.getTaxon() );
            List<BlatResult> blatRes = new ArrayList<>();
            for ( List<BlatResult> coll : results.values() ) {
                blatRes.addAll( coll );
            }
            return this.processBlatResults( goldenpath, blatRes );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * It is assume that strand should only be used if the sequence type is AFFY_{PROBE,COLLAPSED,TARGET} or OLIGO. In
     * all other cases (ESTs etc) the strand is ignored.
     *
     * @return boolean indicating, essentially, if the sequence on the array is double-stranded.
     */
    private boolean determineStrandTreatment( BlatResult blatResult ) {
        boolean ignoreStrand = true;

        SequenceType type = blatResult.getQuerySequence().getType();
        if ( type == null ) {
            return true;
        }
        if ( type.equals( SequenceType.OLIGO ) ) {
            ignoreStrand = false;
        } else if ( type.equals( SequenceType.AFFY_COLLAPSED ) ) {
            ignoreStrand = false;
        } else if ( type.equals( SequenceType.AFFY_PROBE ) ) {
            ignoreStrand = false;
        } else if ( type.equals( SequenceType.AFFY_TARGET ) ) {
            ignoreStrand = false;
        }
        return ignoreStrand;
    }

    /**
     * FIXME possibly implement checking the score, not just the exon overlap. As it stands, the scoring has already
     * done its work by removing redundant blat hits, and very weak hits are also already removed. I'm not convinced
     * more filtering on score is needed here.
     *
     * @param blatAssociationsForSequence associations for one sequence.
     * @return filtered collection
     */
    private Collection<BlatAssociation> filterOnScores( Collection<BlatAssociation> blatAssociationsForSequence,
            ProbeMapperConfig config ) {

        double minimumExonOverlapFraction = config.getMinimumExonOverlapFraction();
        if ( minimumExonOverlapFraction == 0 )
            return blatAssociationsForSequence;

        Collection<BlatAssociation> result = new HashSet<>();

        for ( BlatAssociation ba : blatAssociationsForSequence ) {
            if ( BlatAssociationScorer.computeOverlapFraction( ba ) < minimumExonOverlapFraction ) {
                log.debug( "Result failed to meet exon overlap threshold" );
                continue;
            }
            result.add( ba );
        }

        return result;
    }

    /**
     * group results together by BioSequence
     */
    private Map<BioSequence, Collection<BlatResult>> groupBlatResultsByBioSequence(
            Collection<BlatResult> blatResults ) {

        Map<BioSequence, Collection<BlatResult>> biosequenceToBlatResults = new HashMap<>();

        for ( BlatResult blatResult : blatResults ) {
            if ( !biosequenceToBlatResults.containsKey( blatResult.getQuerySequence() ) ) {
                biosequenceToBlatResults.put( blatResult.getQuerySequence(), new HashSet<BlatResult>() );
            }
            biosequenceToBlatResults.get( blatResult.getQuerySequence() ).add( blatResult );
        }
        return biosequenceToBlatResults;
    }

    /**
     * Process a single BlatResult, identifying gene products it maps to.
     *
     * @return BlatAssociations between the queried biosequence and one or more gene products.
     */
    private Collection<BlatAssociation> processBlatResult( GoldenPathSequenceAnalysis goldenPathDb,
            BlatResult blatResult, ProbeMapperConfig config ) {
        assert blatResult.getTargetChromosome() != null : "Chromosome not filled in for blat result";

        boolean ignoreStrand = this.determineStrandTreatment( blatResult );

        String strand = ignoreStrand ? null : blatResult.getStrand();

        Collection<BlatAssociation> blatAssociations = goldenPathDb
                .findAssociations( blatResult.getTargetChromosome().getName(), blatResult.getTargetStart(),
                        blatResult.getTargetEnd(), blatResult.getTargetStarts(), blatResult.getBlockSizes(), strand,
                        threePrimeMethod, config );

        if ( blatAssociations != null && blatAssociations.size() > 0 ) {
            for ( BlatAssociation association : blatAssociations ) {
                association.setBlatResult( blatResult );
                association.setBioSequence( blatResult.getQuerySequence() );
            }
            return blatAssociations;
        }

        return new HashSet<>();
    }

    /**
     * Potentially trim out alignments to "non-canonical" chromosomes.
     * <ol>
     * <li>If there is just one hit, keep it.
     * <li>If there are multiple hits, but all are to non-canonical chromosomes, keep them for now.
     * <li>Otherwise keep only the ones to canonical chromosomes.
     * </ol>
     *
     * @return trimmed results.
     */
    private Collection<BlatResult> trimNonCanonicalChromosomeHits( Collection<BlatResult> blatResultsForSequence,
            ProbeMapperConfig config ) {

        Collection<BlatResult> trimmedBlatResultsForSequence = blatResultsForSequence;

        if ( config.isTrimNonCanonicalChromosomehits() && blatResultsForSequence.size() > 1 ) {

            Collection<BlatResult> toKeep = new HashSet<>();
            for ( BlatResult ba : blatResultsForSequence ) {
                if ( ChromosomeUtils.isCanonical( ba.getTargetChromosome() ) ) {
                    toKeep.add( ba );
                }
            }
            if ( toKeep.size() > 0 && toKeep.size() < blatResultsForSequence.size() ) {
                trimmedBlatResultsForSequence = toKeep;
            }
        }

        assert !trimmedBlatResultsForSequence.isEmpty();

        return trimmedBlatResultsForSequence;
    }

}
