/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.analysis.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.math.Rank;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.Multifunctionality;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl;

/**
 * Compute gene multifunctionality and store it in the database.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class GeneMultifunctionalityPopulationServiceImpl implements GeneMultifunctionalityPopulationService {

    private static Log log = LogFactory.getLog( GeneMultifunctionalityPopulationServiceImpl.class );

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeneOntologyService goService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private Gene2GOAssociationService gene2GOService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.GeneMultifunctionalityPopulationService#updateMultifunctionality()
     */
    @Override
    public void updateMultifunctionality() {
        for ( Taxon t : taxonService.loadAll() ) {
            log.info( "Processing multifunctionality for " + t );
            updateMultifunctionality( t );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.GeneMultifunctionalityPopulationService#updateMultifunctionality(ubic.gemma.model
     * .genome.Taxon)
     */
    @Override
    public void updateMultifunctionality( Taxon taxon ) {
        Collection<Gene> genes = geneService.loadKnownGenes( taxon );

        if ( genes.isEmpty() ) {
            return;
        }

        Map<Gene, Collection<String>> gomap = fetchGoAnnotations( genes );

        Map<Gene, Multifunctionality> mfs = computeMultifunctionality( gomap );

        /*
         * Persist the results.
         */
        log.info( "Saving multifunctionality for " + genes.size() + " genes" );
        for ( Gene g : genes ) {

            if ( !mfs.containsKey( g ) ) {
                g.setMultifunctionality( null );
            } else {

                Multifunctionality updatedMf = mfs.get( g );

                Multifunctionality oldMf = g.getMultifunctionality();
                if ( oldMf == null ) {
                    g.setMultifunctionality( updatedMf );
                } else {
                    oldMf.setNumGoTerms( updatedMf.getNumGoTerms() );
                    oldMf.setRank( updatedMf.getRank() );
                    oldMf.setScore( updatedMf.getScore() );
                }
            }

            geneService.update( g );
        }

    }

    /**
     * Implementation of multifunctionality computations as described in Gillis and Pavlidis (2011) PLoS ONE 6:2:e17258.
     * 
     * @param gomap
     * @return
     */
    private Map<Gene, Multifunctionality> computeMultifunctionality( Map<Gene, Collection<String>> gomap ) {

        /*
         * See ermineJ Multifunctionality.java for another implementation.
         */

        assert !gomap.isEmpty();

        Map<String, Integer> goGroupSizes = new HashMap<String, Integer>();
        for ( Gene g : gomap.keySet() ) {
            for ( String go : gomap.get( g ) ) {
                if ( !goGroupSizes.containsKey( go ) ) {
                    goGroupSizes.put( go, 1 );
                } else {
                    goGroupSizes.put( go, goGroupSizes.get( go ) + 1 );
                }
            }
        }

        log.info( "Computed GO group sizes" );

        Map<Gene, Double> geneMultifunctionalityScore = new HashMap<Gene, Double>();
        Map<Gene, Multifunctionality> geneMultifunctionality = new HashMap<Gene, Multifunctionality>();
        int numGenes = gomap.size();

        for ( Gene gene : gomap.keySet() ) {

            Multifunctionality mf = Multifunctionality.Factory.newInstance();
            double mfscore = 0.0;
            Collection<String> sets = gomap.get( gene );
            for ( String goset : sets ) {

                assert goGroupSizes.containsKey( goset );
                int inGroup = goGroupSizes.get( goset );
                assert inGroup > 0;

                int outGroup = numGenes - inGroup;

                if ( outGroup == 0 ) {
                    // this doesn't meaningfully contribute to multifunctionality since every gene has it.
                    continue;
                }

                mfscore += 1.0 / ( inGroup * outGroup );
            }
            assert mfscore >= 0.0 && mfscore <= 1.0;

            mf.setNumGoTerms( gomap.get( gene ).size() );
            mf.setScore( mfscore );

            geneMultifunctionalityScore.put( gene, mfscore );
            geneMultifunctionality.put( gene, mf );
        }

        Map<Gene, Double> rawGeneMultifunctionalityRanks = Rank.rankTransform( geneMultifunctionalityScore, true );
        assert numGenes == rawGeneMultifunctionalityRanks.size();
        for ( Gene gene : rawGeneMultifunctionalityRanks.keySet() ) {
            // 1-base the rank before calculating ratio
            double relRank = ( rawGeneMultifunctionalityRanks.get( gene ) + 1 ) / numGenes;
            assert relRank >= 0.0 && relRank <= 1.0;

            // big values are "more multifunctional".
            geneMultifunctionality.get( gene ).setRank( Math.max( 0.0, 1.0 - relRank ) );
        }

        log.info( "Computed multifunctionality" );

        return geneMultifunctionality;
    }

    /**
     * @param genes
     * @return
     */
    private Map<Gene, Collection<String>> fetchGoAnnotations( Collection<Gene> genes ) {

        if ( !goService.isRunning() ) {
            goService.init( true );
        }

        while ( !goService.isReady() ) {
            try {
                Thread.sleep( 2000 );
            } catch ( InterruptedException e ) {
                //
            }
            log.info( "Waiting for GO to load" );
        }

        /*
         * Build the GO 'matrix'.
         */
        int count = 0;
        Map<Gene, Collection<String>> gomap = new HashMap<Gene, Collection<String>>();
        for ( Gene gene : genes ) {
            Collection<VocabCharacteristic> annots = gene2GOService.findByGene( gene );

            Collection<String> termsForGene = new HashSet<String>();

            if ( annots.isEmpty() ) {
                // we just count it as a gene with lowest multifunctionality.
                // continue;
            }

            /*
             * Propagate.
             */

            for ( VocabCharacteristic t : annots ) {

                if ( ontologyService.isObsolete( t.getValueUri() ) ) {
                    log.warn( "Obsolete term annotated to " + gene + " : " + t );
                    continue;
                }

                termsForGene.add( t.getValue() );

                Collection<OntologyTerm> parents = goService.getAllParents( GeneOntologyServiceImpl.getTermForURI( t
                        .getValueUri() ) );

                for ( OntologyTerm p : parents ) {
                    termsForGene.add( p.getTerm() );
                }
            }

            if ( termsForGene.isEmpty() ) {
                // we just count it as a gene with lowest multifunctionality.
                // continue;
            }

            gomap.put( gene, termsForGene );

            if ( ++count % 1000 == 0 ) {
                log.info( "Fetched GO annotations for: " + count + "/" + genes.size() + " genes" );
            }

        }
        return gomap;
    }

}
