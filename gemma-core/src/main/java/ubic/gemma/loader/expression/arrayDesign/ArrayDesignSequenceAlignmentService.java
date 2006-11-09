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

import ubic.basecode.util.StringUtil;
import ubic.gemma.apps.Blat;
import ubic.gemma.externalDb.GoldenPathQuery;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Aligns sequences from array designs to the genome, using blat, and persists the blat results.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean name="arrayDesignSequenceAlignmentService"
 * @spring.property name="blatResultService" ref="blatResultService"
 * @spring.property name="persisterHelper" ref="persisterHelper" *
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 */
public class ArrayDesignSequenceAlignmentService {

    private static Log log = LogFactory.getLog( ArrayDesignSequenceAlignmentService.class.getName() );

    BlatResultService blatResultService;

    ArrayDesignService arrayDesignService;

    PersisterHelper persisterHelper;

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
            sequence.setLength( length );
            sequence.setIsApproximateLength( false );
            sequence.setDescription( StringUtil.append( sequence.getDescription(),
                    "Length information from GoldenPath annotations.", " -- " ) );
        } else {
            assert result.getQuerySequence().getLength().equals( sequence.getLength() );
        }
    }

    /**
     * @param sequencesToBlat
     * @param blat
     * @param taxon whose database will be queries
     * @return
     */
    private Map<BioSequence, Collection<BlatResult>> getAlignments( Collection<BioSequence> sequencesToBlat, Taxon taxon ) {
        Blat blat = new Blat();
        Map<BioSequence, Collection<BlatResult>> results = new HashMap<BioSequence, Collection<BlatResult>>();

        try {

            // First checck if there are alignment results in the goldenpath
            // datbase.
            GoldenPathQuery gpq = new GoldenPathQuery( taxon );

            Collection<BioSequence> needBlat = new HashSet<BioSequence>();
            int count = 0;
            int totalFound = 0;
            for ( BioSequence sequence : sequencesToBlat ) {
                boolean found = false;
                if ( sequence.getSequenceDatabaseEntry() != null ) {
                    Collection<BlatResult> brs = gpq
                            .findAlignments( sequence.getSequenceDatabaseEntry().getAccession() );

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

                if ( ++count % 200 == 0 && totalFound > 0 ) {
                    log
                            .info( "Alignments in Golden Path database for " + totalFound + "/" + count
                                    + " checked so far." );
                }

                if ( !found ) {
                    needBlat.add( sequence );
                }

            }

            if ( totalFound > 0 ) {
                log.info( "Found " + totalFound + "/" + count + " alignments in Golden Path database" );
            }

            if ( needBlat.size() > 0 ) {
                log.info( "Running blat on " + needBlat.size() + " sequences" );
                Map<BioSequence, Collection<BlatResult>> moreResults = blat.blatQuery( needBlat, taxon );
                results.putAll( moreResults );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
        return results;
    }

    /**
     * @param ad
     * @return
     */
    private Collection<BioSequence> getSequenceMap( ArrayDesign ad ) {
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
    @SuppressWarnings("unchecked")
    private Collection<BlatResult> persistBlatResults( Collection<BioSequence> sequencesToBlat,
            Collection<BlatResult> brs ) {

        for ( BlatResult br : brs ) {
            assert br.getQuerySequence() != null;
            assert br.getQuerySequence().getName() != null;
            Taxon taxon = br.getQuerySequence().getTaxon();
            assert taxon != null;
            br.getTargetChromosome().setTaxon( taxon );
            br.getTargetChromosome().getSequence().setTaxon( taxon );
        }
        log.info( "Persisting " + brs.size() + " BLAT results" );
        return ( Collection<BlatResult> ) persisterHelper.persist( brs );
    }

    /**
     * @param ad
     */
    public Collection<BlatResult> processArrayDesign( ArrayDesign ad ) {
        Taxon taxon = arrayDesignService.getTaxon( ad.getId() );
        Collection<BioSequence> sequencesToBlat = getSequenceMap( ad );

        Collection<BlatResult> allResults = new HashSet<BlatResult>();

        Map<BioSequence, Collection<BlatResult>> results = getAlignments( sequencesToBlat, taxon );

        log.info( "Got BLAT results for " + results.keySet().size() + " query sequences" );

        int noresults = 0;
        for ( BioSequence sequence : sequencesToBlat ) {
            if ( sequence == null ) {
                log.warn( "Null sequence!" );
                continue;
            }
            Collection<BlatResult> brs = results.get( sequence );
            if ( brs == null ) {
                ++noresults;
                continue;
            }
            for ( BlatResult result : brs ) {
                result.setQuerySequence( sequence ); // must do this to replace
                // placeholder instance.
            }
            allResults.addAll( persistBlatResults( sequencesToBlat, brs ) );
        }

        log.info( noresults + "/" + sequencesToBlat.size() + " sequences had no blat results" );

        return allResults;

    }

    /**
     * @param ad
     * @param rawBlatResults, assumed to be from alignments to the genome for the array design (that is, we don't
     *        consider aligning mouse to human)
     * @return persisted BlatResults.
     */
    public Collection<BlatResult> processArrayDesign( ArrayDesign ad, Collection<BlatResult> rawBlatResults ) {
        Collection<BioSequence> sequencesToBlat = getSequenceMap( ad );

        Taxon taxon = arrayDesignService.getTaxon( ad.getId() );
        ExternalDatabase searchedDatabase = Blat.getSearchedGenome( taxon );

        for ( BlatResult result : rawBlatResults ) {
            result.getQuerySequence().setTaxon( taxon );
            result.setSearchedDatabase( searchedDatabase );
            result.getTargetChromosome().setTaxon( taxon );
            result.getTargetChromosome().getSequence().setTaxon( taxon );
        }

        return persistBlatResults( sequencesToBlat, rawBlatResults );
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param blatResultService the blatResultService to set
     */
    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
