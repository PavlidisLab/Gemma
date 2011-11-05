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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.util.StringUtil;
import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.apps.Blat;
import ubic.gemma.externalDb.GoldenPathQuery;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.SequenceBinUtils;

/**
 * Aligns sequences from array designs to the genome, using blat, and persists the blat results.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class ArrayDesignSequenceAlignmentService {

    private static Log log = LogFactory.getLog( ArrayDesignSequenceAlignmentService.class.getName() );

    @Autowired
    BlatResultService blatResultService;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    PersisterHelper persisterHelper;

    @Autowired
    BioSequenceService bioSequenceService;

    @Autowired
    ArrayDesignReportService arrayDesignReportService;

    /**
     * Run blat on all sequences on the array design. For arrays with sequences from multiple taxa, BLAT is run
     * appropriately assuming sequences are available for all the represented taxa.
     * 
     * @param ad
     * @param sensitive if true, blat will be run in a more sensitive mode, if available.
     */
    public Collection<BlatResult> processArrayDesign( ArrayDesign ad, boolean sensitive ) {

        log.info( "Looking for old results to remove..." );
        arrayDesignService.deleteAlignmentData( ad );

        Collection<BlatResult> allResults = new HashSet<BlatResult>();

        if ( sensitive ) log.info( "Running in 'sensitive' mode if possible" );

        Collection<Taxon> taxa = arrayDesignService.getTaxa( ad.getId() );
        for ( Taxon taxon : taxa ) {

            Collection<BioSequence> sequencesToBlat = getSequences( ad, taxon );

            Map<BioSequence, Collection<BlatResult>> results = getAlignments( sequencesToBlat, sensitive, taxon );

            log.info( "Got BLAT results for " + results.keySet().size() + " query sequences" );

            Map<String, BioSequence> nameMap = new HashMap<String, BioSequence>();
            for ( BioSequence bs : results.keySet() ) {
                if ( nameMap.containsKey( bs.getName() ) ) {
                    throw new IllegalStateException(
                            "All distinct sequences on the array must have unique names; found " + bs.getName()
                                    + " more than once." );
                }
                nameMap.put( bs.getName(), bs );
            }

            int noresults = 0;
            int count = 0;
            for ( BioSequence sequence : sequencesToBlat ) {
                if ( sequence == null ) {
                    log.warn( "Null sequence!" );
                    continue;
                }
                Collection<BlatResult> brs = results.get( nameMap.get( sequence.getName() ) );
                if ( brs == null ) {
                    ++noresults;
                    continue;
                }
                for ( BlatResult result : brs ) {
                    result.setQuerySequence( sequence ); // must do this to replace
                    // placeholder instance.
                }
                allResults.addAll( persistBlatResults( brs ) );

                if ( ++count % 2000 == 0 ) {
                    log.info( "Checked results for " + count + " queries, " + allResults.size()
                            + " blat results so far." );
                }

            }

            log.info( noresults + "/" + sequencesToBlat.size() + " sequences had no blat results" );
        }

        arrayDesignReportService.generateArrayDesignReport( ad.getId() );

        return allResults;

    }

    /**
     * @param ad
     * @param taxon (to allow for the possibility of multiple taxa for the array) - if not given, attempt to infer from
     *        ad.
     * @param rawBlatResults , assumed to be from alignments to correct taxon Typically these would have been read in
     *        from a file.
     * @return persisted BlatResults.
     */
    public Collection<BlatResult> processArrayDesign( ArrayDesign ad, Taxon taxon, Collection<BlatResult> rawBlatResults ) {

        log.info( "Looking for old results to remove..." );

        ad = arrayDesignService.thaw( ad );

        arrayDesignService.deleteAlignmentData( ad );
        // Blat file processing can only be run on one taxon at a time
        taxon = validateTaxaForBlatFile( ad, taxon );

        Collection<BioSequence> sequencesToBlat = getSequences( ad );
        sequencesToBlat = bioSequenceService.thaw( sequencesToBlat );

        // if the blat results were loaded from a file, we have to replace the
        // querysequences with the actual ones
        // attached to the array design. We have to do this by name because the
        // sequence name is what the files contain.
        // Note that if there is ambiguity there will be problems!
        Map<String, BioSequence> seqMap = new HashMap<String, BioSequence>();
        for ( BioSequence bioSequence : sequencesToBlat ) {
            seqMap.put( bioSequence.getName(), bioSequence );
        }

        ExternalDatabase searchedDatabase = Blat.getSearchedGenome( taxon );

        Collection<BlatResult> toSkip = new HashSet<BlatResult>();
        for ( BlatResult result : rawBlatResults ) {

            /*
             * If the sequences don't have ids, replace them with the actual sequences associated with the array design.
             */
            if ( result.getQuerySequence().getId() == null ) {
                String querySeqName = result.getQuerySequence().getName();
                BioSequence actualSequence = seqMap.get( querySeqName );
                if ( actualSequence == null ) {
                    log.debug( "Array design does not contain a sequence with name " + querySeqName );
                    toSkip.add( result );
                    continue;
                }
                result.setQuerySequence( actualSequence );
            } else {
                result.getQuerySequence().setTaxon( taxon );
            }

            result.setSearchedDatabase( searchedDatabase );
            result.getTargetChromosome().setTaxon( taxon );
            result.getTargetChromosome().getSequence().setTaxon( taxon );

        }

        if ( toSkip.size() > 0 ) {
            log.warn( toSkip.size() + " blat results were for sequences not on " + ad + "; they will be ignored." );
            rawBlatResults.removeAll( toSkip );
        }

        Map<BioSequence, Collection<BlatResult>> goldenPathAlignments = new HashMap<BioSequence, Collection<BlatResult>>();
        getGoldenPathAlignments( sequencesToBlat, taxon, goldenPathAlignments );
        for ( BioSequence sequence : goldenPathAlignments.keySet() ) {
            rawBlatResults.addAll( goldenPathAlignments.get( sequence ) );
        }

        Collection<BlatResult> results = persistBlatResults( rawBlatResults );

        arrayDesignReportService.generateArrayDesignReport( ad.getId() );

        return results;
    }

    /**
     * If no taxon is supplied then infer it from array. If more than one taxa is on array then stop processing as blat
     * file details should relate to one taxon
     * 
     * @param arrayDesign Array design to process
     * @param taxon Taxon supplied
     */
    public Taxon validateTaxaForBlatFile( ArrayDesign arrayDesign, Taxon taxon ) {

        if ( taxon == null ) {
            Collection<Taxon> taxaOnArray = arrayDesignService.getTaxa( arrayDesign.getId() );
            if ( taxaOnArray != null && taxaOnArray.size() == 1 && taxaOnArray.iterator().next() != null ) {
                return taxaOnArray.iterator().next();
            }
            throw new IllegalArgumentException( ( taxaOnArray == null ? "?" : taxaOnArray.size() )
                    + " taxon found for " + arrayDesign + " specifiy which taxon to run" );

        }
        return taxon;
    }

    /**
     * If necessary, copy the sequence length information over from the blat result to the given sequence. This is often
     * needed when we get the blat results from golden path.
     * 
     * @param sequence that may not have length information
     * @param result used to get length information
     */
    private void copyLengthInformation( BioSequence sequence, BlatResult result ) {
        if ( result.getQuerySequence() != null && result.getQuerySequence().getLength() == null ) {
            long length = result.getQuerySequence().getLength();
            if ( sequence.getLength() == null ) sequence.setLength( length );
            sequence.setIsApproximateLength( false );
            sequence.setDescription( StringUtil.append( sequence.getDescription(),
                    "Length information from GoldenPath annotations.", " -- " ) );
            bioSequenceService.update( sequence );
        }
    }

    /**
     * @param sequencesToBlat
     * @param taxon whose database will be queried
     * @return Map of biosequences to collections of blat results.
     */
    private Map<BioSequence, Collection<BlatResult>> getAlignments( Collection<BioSequence> sequencesToBlat,
            boolean sensitive, Taxon taxon ) {
        Blat blat = new Blat();
        Map<BioSequence, Collection<BlatResult>> results = new HashMap<BioSequence, Collection<BlatResult>>();
        sequencesToBlat = bioSequenceService.thaw( sequencesToBlat );
        try {

            Collection<BioSequence> needBlat = getGoldenPathAlignments( sequencesToBlat, taxon, results );

            if ( needBlat.size() > 0 ) {
                log.info( "Running blat on " + needBlat.size() + " sequences" );
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
     * @param taxon
     * @param results Will be stored here.
     * @return the sequences which ARE NOT found in goldenpath and which therefore DO need blat.
     * @throws SQLException
     */
    private Collection<BioSequence> getGoldenPathAlignments( Collection<BioSequence> sequencesToBlat, Taxon taxon,
            Map<BioSequence, Collection<BlatResult>> results ) {

        GoldenPathQuery gpq = new GoldenPathQuery( taxon );

        Collection<BioSequence> needBlat = new HashSet<BioSequence>();
        int count = 0;
        int totalFound = 0;
        for ( BioSequence sequence : sequencesToBlat ) {
            boolean found = false;
            if ( sequence.getSequenceDatabaseEntry() != null ) {
                Collection<BlatResult> brs = gpq.findAlignments( sequence.getSequenceDatabaseEntry().getAccession() );

                if ( brs != null && brs.size() > 0 ) {
                    for ( BlatResult result : brs ) {
                        copyLengthInformation( sequence, result );
                        result.setQuerySequence( sequence );
                    }
                    results.put( sequence, brs );
                    found = true;
                    totalFound++;
                }
            }

            if ( ++count % 1000 == 0 && totalFound > 0 ) {
                log.info( "Alignments in Golden Path database for " + totalFound + "/" + count + " checked so far." );
            }

            if ( !found ) {
                needBlat.add( sequence );
            }

        }

        if ( totalFound > 0 ) {
            log.info( "Found " + totalFound + "/" + count + " alignments in Golden Path database" );
        }
        return needBlat;
    }

    /**
     * @param ad
     * @return all sequences, across all taxa that might be represented on the array design
     * @see getSequences(ad, taxon) for method to get just the sequences for one taxon.
     */
    public static Collection<BioSequence> getSequences( ArrayDesign ad ) {
        return getSequences( ad, null );
    }

    /**
     * @param ad
     * @param taxon (specified in case array has multiple taxa)
     * @return
     */
    public static Collection<BioSequence> getSequences( ArrayDesign ad, Taxon taxon ) {

        Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
        Collection<BioSequence> sequencesToBlat = new HashSet<BioSequence>();
        int numWithNoBioSequence = 0;
        int numWithNoSequenceData = 0;
        boolean warned = false;
        for ( CompositeSequence cs : compositeSequences ) {
            BioSequence bs = cs.getBiologicalCharacteristic();

            if ( !warned && ( numWithNoBioSequence > 20 || numWithNoSequenceData > 20 ) ) {
                warned = true;
                log.warn( "More than 20 composite sequences don't have sequence information, no more warnings..." );
            }

            if ( bs == null ) {
                ++numWithNoBioSequence;
                if ( !warned ) {
                    log.warn( cs + " had no associated biosequence object" );
                }
                continue;
            }

            if ( bs.getTaxon() == null ) {
                warned = true;
                log.warn( "There is no taxon defined for this biosequence " );
                continue;
            }
            // if the taxon is null that means we want this run for all taxa for that array
            if ( taxon != null && !bs.getTaxon().equals( taxon ) ) {
                continue;
            }

            if ( !warned && ( numWithNoBioSequence > 20 || numWithNoSequenceData > 20 ) ) {
                warned = true;
                log.warn( "More than 20 composite sequences don't have sequence information, no more warnings..." );
            }

            if ( StringUtils.isBlank( bs.getSequence() ) ) {
                ++numWithNoSequenceData;
                if ( !warned ) {
                    log.warn( cs + " had " + bs + " but no sequence, skipping" );
                }
                continue;
            }
            sequencesToBlat.add( bs );

        }
        if ( numWithNoBioSequence > 0 || numWithNoSequenceData > 0 ) {
            log.warn( numWithNoBioSequence + " composite sequences lacked biosequence associations; "
                    + numWithNoSequenceData + " lacked sequence data ( out of " + compositeSequences.size()
                    + " total)." );
        }
        return sequencesToBlat;
    }

    /**
     * @param sequencesToBlat, assumed to be the ones that were analyzed
     * @param brs, assumed to be from alignments to the genome for the array design (that is, we don't consider aligning
     *        mouse to human)
     * @return
     */
    private Collection<BlatResult> persistBlatResults( Collection<BlatResult> brs ) {
        for ( BlatResult br : brs ) {
            assert br.getQuerySequence() != null;
            assert br.getQuerySequence().getName() != null;
            Taxon taxon = br.getQuerySequence().getTaxon();
            assert taxon != null;
            br.getTargetChromosome().setTaxon( taxon );
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

                pl
                        .setBin( SequenceBinUtils.binFromRange( br.getTargetStart().intValue(), br.getTargetEnd()
                                .intValue() ) );
            }

        }
        return ( Collection<BlatResult> ) persisterHelper.persist( brs );
    }

    /**
     * @param design
     */
    public Collection<BlatResult> processArrayDesign( ArrayDesign design ) {
        return this.processArrayDesign( design, false );
    }
}
