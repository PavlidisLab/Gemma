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

package ubic.gemma.core.analysis.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.math.Rank;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.Multifunctionality;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.*;

/**
 * Compute gene multifunctionality and store it in the database.
 *
 * @author paul
 */
@Component
public class GeneMultifunctionalityPopulationServiceImpl implements GeneMultifunctionalityPopulationService {

    private static final Log log = LogFactory.getLog( GeneMultifunctionalityPopulationServiceImpl.class );

    @Autowired
    private Gene2GOAssociationService gene2GOService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeneOntologyService goService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private TaxonService taxonService;

    @Override
    public void updateMultifunctionality() {
        for ( Taxon t : taxonService.loadAll() ) {
            GeneMultifunctionalityPopulationServiceImpl.log.info( "Processing multifunctionality for " + t );
            doUpdateMultifunctionality( t );
        }
        ExternalDatabase ed = externalDatabaseService.findByNameWithAuditTrail( ExternalDatabases.MULTIFUNCTIONALITY );
        if ( ed != null ) {
            externalDatabaseService.updateReleaseLastUpdated( ed, "Updated multifunctionality scores for all taxa.", new Date() );
        } else {
            log.warn( String.format( "No external database with name %s.", ExternalDatabases.MULTIFUNCTIONALITY ) );
        }
    }

    @Override
    public void updateMultifunctionality( Taxon t ) {
        doUpdateMultifunctionality( t );
        ExternalDatabase ed = externalDatabaseService.findByNameWithAuditTrail( ExternalDatabases.MULTIFUNCTIONALITY );
        if ( ed != null ) {
            externalDatabaseService.updateReleaseLastUpdated( ed, String.format( "Updated multifunctionality scores for %s.", t ), new Date() );
        } else {
            log.warn( String.format( "No external database with name %s.", ExternalDatabases.MULTIFUNCTIONALITY ) );
        }
    }

    private void doUpdateMultifunctionality( Taxon taxon ) {
        Collection<Gene> genes = geneService.loadAll( taxon );

        if ( genes.isEmpty() ) {
            GeneMultifunctionalityPopulationServiceImpl.log.warn( "No genes found for " + taxon );
            return;
        }

        Map<Gene, Set<String>> gomap = this.fetchGoAnnotations( genes );

        Map<Gene, Multifunctionality> mfs = this.computeMultifunctionality( gomap );

        GeneMultifunctionalityPopulationServiceImpl.log
                .info( "Saving multifunctionality for " + genes.size() + " genes" );

        Collection<Gene> batch = new HashSet<>();

        int batchSize = 200;
        int i = 0;
        for ( Gene g : genes ) {
            batch.add( g );

            if ( batch.size() == batchSize ) {
                this.saveBatch( batch, mfs );
                batch.clear();
            }

            if ( ++i % 1000 == 0 ) {
                GeneMultifunctionalityPopulationServiceImpl.log.info( "Updated " + i + " genes/" + genes.size() );
            }
        }

        if ( !batch.isEmpty() ) {
            this.saveBatch( batch, mfs );
        }

        GeneMultifunctionalityPopulationServiceImpl.log.info( "Done" );
    }

    /**
     * Implementation of multifunctionality computations as described in Gillis and Pavlidis (2011) PLoS ONE 6:2:e17258.
     *
     * @param  gomap gomap
     * @return map
     */
    private Map<Gene, Multifunctionality> computeMultifunctionality( Map<Gene, Set<String>> gomap ) {

        /*
         * See ermineJ Multifunctionality.java for another implementation.
         */

        assert !gomap.isEmpty();

        Map<String, Integer> goGroupSizes = new HashMap<>();
        for ( Gene g : gomap.keySet() ) {
            for ( String go : gomap.get( g ) ) {
                if ( !goGroupSizes.containsKey( go ) ) {
                    goGroupSizes.put( go, 1 );
                } else {
                    goGroupSizes.put( go, goGroupSizes.get( go ) + 1 );
                }
            }
        }

        GeneMultifunctionalityPopulationServiceImpl.log.info( "Computed GO group sizes" );

        Map<Gene, Double> geneMultifunctionalityScore = new HashMap<>();
        Map<Gene, Multifunctionality> geneMultifunctionality = new HashMap<>();
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

        GeneMultifunctionalityPopulationServiceImpl.log.info( "Computed multifunctionality" );

        return geneMultifunctionality;
    }

    private Map<Gene, Set<String>> fetchGoAnnotations( Collection<Gene> genes ) {
        try {
            OntologyUtils.ensureInitialized( goService );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            return Collections.emptyMap();
        }

        /*
         * Build the GO 'matrix'.
         */
        int count = 0;
        Map<Gene, Set<String>> gomap = new HashMap<>();
        for ( Gene gene : genes ) {
            Collection<Characteristic> annots = gene2GOService.findByGene( gene );

            Set<String> termsForGene = new HashSet<>();

            //noinspection StatementWithEmptyBody // TODO we just count it as a gene with lowest multifunctionality
            if ( annots.isEmpty() ) {
                // continue;
            }

            /*
             * Propagate.
             */
            Set<OntologyTerm> terms = new HashSet<>( annots.size() );
            for ( Characteristic t : annots ) {
                if ( t.getValueUri() == null ) {
                    GeneMultifunctionalityPopulationServiceImpl.log
                            .warn( "Free-text term annotated to " + gene + " : " + t );
                    continue;
                }
                OntologyTerm term = goService.getTerm( t.getValueUri() );
                if ( term == null || term.isObsolete() ) {
                    GeneMultifunctionalityPopulationServiceImpl.log
                            .warn( "Obsolete term annotated to " + gene + " : " + t );
                    continue;
                }
                terms.add( term );
                termsForGene.add( term.getLabel() );
            }

            // add all the parents
            Set<OntologyTerm> parents = goService.getParents( terms, true, false );
            for ( OntologyTerm p : parents ) {
                if ( p.isObsolete() ) {
                    continue;
                }
                termsForGene.add( p.getLabel() );
            }

            //noinspection StatementWithEmptyBody // TODO we just count it as a gene with lowest multifunctionality
            if ( termsForGene.isEmpty() ) {
                // continue;
            }

            gomap.put( gene, termsForGene );

            if ( ++count % 1000 == 0 ) {
                GeneMultifunctionalityPopulationServiceImpl.log
                        .info( "Fetched GO annotations for: " + count + "/" + genes.size() + " genes" );
            }

        }
        return gomap;
    }

    private void saveBatch( Collection<Gene> genes, Map<Gene, Multifunctionality> mfs ) {
        genes = geneService.thawLite( genes );
        for ( Gene g : genes ) {
            if ( !mfs.containsKey( g ) ) {
                g.setMultifunctionality( null );
            } else {

                Multifunctionality updatedMf = mfs.get( g );

                Multifunctionality oldMf = g.getMultifunctionality();

                if ( oldMf == null ) {
                    g.setMultifunctionality( updatedMf );
                } else {
                    oldMf = g.getMultifunctionality();
                    oldMf.setNumGoTerms( updatedMf.getNumGoTerms() );
                    oldMf.setRank( updatedMf.getRank() );
                    oldMf.setScore( updatedMf.getScore() );
                }
            }
        }

        geneService.update( genes );
    }

}
