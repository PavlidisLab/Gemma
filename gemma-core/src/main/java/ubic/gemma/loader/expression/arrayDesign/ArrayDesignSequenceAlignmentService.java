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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.apps.Blat;
import ubic.gemma.externalDb.GoldenPathQuery;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
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
 * @spring.property name="persisterHelper" ref="persisterHelper"
 */
public class ArrayDesignSequenceAlignmentService {

    private static Log log = LogFactory.getLog( ArrayDesignSequenceAlignmentService.class.getName() );

    BlatResultService blatResultService;

    PersisterHelper persisterHelper;

    /**
     * @param blatResultService the blatResultService to set
     */
    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }

    /**
     * @param ad
     * @param rawBlatResults
     * @param taxon
     * @return persisted BlatResults.
     */
    public Collection<BlatResult> processArrayDesign( ArrayDesign ad, Collection<BlatResult> rawBlatResults, Taxon taxon ) {
        Map<String, BioSequence> sequencesToBlat = getSequenceMap( ad );

        ExternalDatabase searchedDatabase = Blat.getSearchedGenome( taxon );

        for ( BlatResult result : rawBlatResults ) {
            result.setSearchedDatabase( searchedDatabase );
        }

        return persistBlatResults( taxon, sequencesToBlat, rawBlatResults );
    }

    /**
     * @param ad
     */
    public Collection<BlatResult> processArrayDesign( ArrayDesign ad, Taxon taxon ) {
        Map<String, BioSequence> sequencesToBlat = getSequenceMap( ad );

        Collection<BlatResult> allResults = new HashSet<BlatResult>();

        Map<BioSequence, Collection<BlatResult>> results = getAlignments( sequencesToBlat, taxon );

        log.info( "Got BLAT results for " + results.keySet().size() + " query sequences" );

        for ( BioSequence key : results.keySet() ) {
            Collection<BlatResult> brs = results.get( key );
            allResults.addAll( persistBlatResults( taxon, sequencesToBlat, brs ) );
        }

        return allResults;

    }

    @SuppressWarnings("unchecked")
    private Collection<BlatResult> persistBlatResults( Taxon taxon, Map<String, BioSequence> sequencesToBlat,
            Collection<BlatResult> brs ) {

        for ( BlatResult br : brs ) {
            String acc = br.getQuerySequence().getName();
            assert acc != null && sequencesToBlat.containsKey( acc );

            br.getTargetChromosome().setTaxon( taxon );
            br.getTargetChromosome().getSequence().setTaxon( taxon );
            br.setQuerySequence( sequencesToBlat.get( acc ) );

        }
        log.info( "Persisting " + brs.size() + " BLAT results" );
        return ( Collection<BlatResult> ) persisterHelper.persist( brs );
    }

    /**
     * @param sequencesToBlat
     * @param blat
     * @param taxon whose database will be queries
     * @return
     */
    private Map<BioSequence, Collection<BlatResult>> getAlignments( Map<String, BioSequence> sequencesToBlat,
            Taxon taxon ) {
        Blat blat = new Blat();
        Map<BioSequence, Collection<BlatResult>> results = new HashMap<BioSequence, Collection<BlatResult>>();

        try {

            // First checck if there are alignment results in the goldenpath datbase.
            GoldenPathQuery gpq = new GoldenPathQuery( taxon );

            Collection<BioSequence> needBlat = new HashSet<BioSequence>();
            int count = 0;
            int totalFound = 0;
            for ( BioSequence sequence : sequencesToBlat.values() ) {
                boolean found = false;
                if ( sequence.getSequenceDatabaseEntry() != null ) {
                    Collection<BlatResult> brs = gpq
                            .findAlignments( sequence.getSequenceDatabaseEntry().getAccession() );
                    if ( brs != null && brs.size() > 0 ) {
                        for ( BlatResult result : brs ) {
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
    private Map<String, BioSequence> getSequenceMap( ArrayDesign ad ) {
        Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
        Map<String, BioSequence> sequencesToBlat = new HashMap<String, BioSequence>();
        for ( CompositeSequence sequence : compositeSequences ) {
            BioSequence bs = sequence.getBiologicalCharacteristic();
            sequencesToBlat.put( bs.getName(), bs );
        }
        return sequencesToBlat;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
