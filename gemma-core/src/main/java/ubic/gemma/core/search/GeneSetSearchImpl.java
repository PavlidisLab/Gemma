/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.core.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.lucene.LuceneQueryUtils;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.*;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.persistence.service.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author paul
 */
@Component
public class GeneSetSearchImpl implements GeneSetSearch {

    /**
     * Also defined in GeneSearchServiceImpl.
     */
    private static final int MAX_GO_GROUP_SIZE = 200;
    private static final Log log = LogFactory.getLog( GeneSetSearchImpl.class );

    @Autowired
    private Gene2GOAssociationService gene2GoService;
    @Autowired
    private GeneOntologyService geneOntologyService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;
    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    @Autowired
    private TaxonService taxonService;

    @Override
    public Collection<GeneSet> findByGene( Gene gene ) {
        return geneSetService.findByGene( gene );
    }

    @Override
    public GOGroupValueObject findGeneSetValueObjectByGoId( String goId, @Nullable Long taxonId ) {

        // shouldn't need to set the taxon here, should be taken care of when creating the value object
        Taxon taxon;

        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                GeneSetSearchImpl.log.warn( "No such taxon with id=" + taxonId );
            } else {
                GeneSet result = this.findByGoId( goId, taxon );
                if ( result == null ) {
                    GeneSetSearchImpl.log.warn( "No matching gene set found for: " + goId );
                    return null;
                }
                GOGroupValueObject ggvo = geneSetValueObjectHelper.convertToGOValueObject( result, goId, goId );

                ggvo.setTaxon( new TaxonValueObject( taxon ) );

                return ggvo;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public GeneSet findByGoId( String goId, @Nullable Taxon taxon ) {
        OntologyTerm goTerm = geneOntologyService.getTerm( StringUtils.strip( goId ) );

        if ( goTerm == null ) {
            return null;
        }
        // if taxon is null, this returns a geneset with genes from different taxons
        return this.goTermToGeneSet( goTerm, taxon );
    }

    @Override
    public Collection<GeneSet> findByGoTermName( String goTermName, @Nullable Taxon taxon ) throws SearchException {
        return this.findByGoTermName( goTermName, taxon, null, null );
    }

    @Override
    public Collection<GeneSet> findByGoTermName( String goTermName, @Nullable Taxon taxon, @Nullable Integer maxGoTermsProcessed,
            @Nullable Integer maxGeneSetSize ) throws SearchException {
        if ( !geneOntologyService.isOntologyLoaded() ) {
            return Collections.emptySet();
        }
        Collection<OntologySearchResult<OntologyTerm>> matches;
        try {
            matches = this.geneOntologyService.findTerm( StringUtils.strip( goTermName ), 500 );
        } catch ( OntologySearchException e ) {
            try {
                matches = this.geneOntologyService.findTerm( LuceneQueryUtils.escape( StringUtils.strip( goTermName ) ), 500 );
            } catch ( OntologySearchException e1 ) {
                throw new BaseCodeOntologySearchException( e );
            }
        }

        Collection<GeneSet> results = new HashSet<>();

        for ( OntologySearchResult<OntologyTerm> t : matches ) {
            if ( taxon == null ) {
                Collection<GeneSet> sets = this.goTermToGeneSets( t.getResult(), maxGeneSetSize );
                results.addAll( sets );

                // noinspection StatementWithEmptyBody // FIXME should we count each species as one go?
                if ( maxGoTermsProcessed != null && results.size() > maxGoTermsProcessed ) {
                    // return results;
                }
            } else {

                GeneSet converted = this.goTermToGeneSet( t.getResult(), taxon, maxGeneSetSize );
                // converted will be null if its size is more than maxGeneSetSize
                if ( converted != null ) {
                    results.add( converted );

                }
            }

            if ( maxGoTermsProcessed != null && results.size() > maxGoTermsProcessed ) {
                return results;
            }
        }

        return results;

    }

    @Override
    public Collection<GeneSet> findByName( String name ) {
        return geneSetService.findByName( StringUtils.strip( name ) );
    }

    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        return geneSetService.findByName( StringUtils.strip( name ), taxon );
    }

    @Override
    public Collection<GeneSet> findGeneSetsByName( String query, Long taxonId ) throws SearchException {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<>();
        }
        Collection<GeneSet> foundGeneSets;
        Taxon tax;
        tax = taxonService.load( taxonId );

        if ( tax == null ) {
            // throw new IllegalArgumentException( "Can't locate taxon with id=" + taxonId );
            foundGeneSets = this.findByName( query );
        } else {
            foundGeneSets = this.findByName( query, tax );
        }

        foundGeneSets.clear(); // for testing general search

        /*
         * SEARCH GENE ONTOLOGY
         */

        if ( query.toUpperCase().startsWith( "GO" ) ) {
            if ( tax == null ) {
                Collection<GeneSet> goSets = this.findByGoId( query );
                foundGeneSets.addAll( goSets );
            } else {
                GeneSet goSet = this.findByGoId( query, tax );
                if ( goSet != null )
                    foundGeneSets.add( goSet );
            }
        } else {
            foundGeneSets.addAll( this.findByGoTermName( query, tax ) );
        }

        return foundGeneSets;
    }

    @Override
    public Collection<GeneSetValueObject> findByPhenotypeName( String phenotypeQuery, Taxon taxon ) throws SearchException {

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<CharacteristicValueObject> phenotypes = phenotypeAssociationManagerService
                .searchOntologyForPhenotypes( StringUtils.strip( phenotypeQuery ), null );

        Collection<GeneSetValueObject> results = new HashSet<>();

        if ( phenotypes.isEmpty() ) {
            return results;
        }

        if ( timer.getTime() > 200 ) {
            GeneSetSearchImpl.log.info( "Find phenotypes: " + timer.getTime() + "ms" );
        }

        GeneSetSearchImpl.log.debug( " Converting CharacteristicValueObjects collection(size:" + phenotypes.size()
                + ") into GeneSets for  phenotype query " + phenotypeQuery );
        Map<String, CharacteristicValueObject> uris = new HashMap<>();
        for ( CharacteristicValueObject cvo : phenotypes ) {
            uris.put( cvo.getValueUri(), cvo );
        }

        Map<String, Collection<? extends GeneValueObject>> genes = phenotypeAssociationManagerService
                .findCandidateGenesForEach( uris.keySet(), taxon );

        if ( timer.getTime() > 500 ) {
            GeneSetSearchImpl.log.info( "Find phenotype genes done at " + timer.getTime() + "ms" );
        }

        for ( String uri : genes.keySet() ) {

            Collection<? extends GeneValueObject> gvos = genes.get( uri );

            if ( gvos.isEmpty() )
                continue;

            Collection<Long> geneIds = EntityUtils.getIds( gvos );

            GeneSetValueObject transientGeneSet = new GeneSetValueObject();

            transientGeneSet.setName( this.uri2phenoID( uris.get( uri ) ) );
            transientGeneSet.setDescription( uris.get( uri ).getValue() );
            transientGeneSet.setGeneIds( geneIds );

            transientGeneSet.setTaxon( gvos.iterator().next().getTaxon() );

            results.add( transientGeneSet );

        }

        if ( timer.getTime() > 1000 ) {
            GeneSetSearchImpl.log
                    .info( "Loaded " + phenotypes.size() + " phenotype gene sets for query " + phenotypeQuery + " in "
                            + timer.getTime() + "ms" );
        }
        return results;

    }

    private Collection<GeneSet> findByGoId( String query ) {
        OntologyTerm goTerm = geneOntologyService.getTerm( StringUtils.strip( query ) );

        if ( goTerm == null ) {
            return new HashSet<>();
        }
        // if taxon is null, this returns genesets for all taxa
        return this.goTermToGeneSets( goTerm, GeneSetSearchImpl.MAX_GO_GROUP_SIZE );
    }

    @Nullable
    private GeneSet goTermToGeneSet( OntologyTerm term, @Nullable Taxon taxon ) {
        return this.goTermToGeneSet( term, taxon, null );
    }

    /**
     * Convert a GO term to a 'GeneSet', including genes from all child terms. Divide up by taxon.
     */
    @Nullable
    private GeneSet goTermToGeneSet( OntologyTerm term, @Nullable Taxon taxon, @Nullable Integer maxGeneSetSize ) {
        if ( term.getUri() == null )
            return null;

        Collection<OntologyTerm> allMatches = new HashSet<>();
        allMatches.add( term );
        allMatches.addAll( term.getChildren( false, false ) );
        // GeneSetSearchImpl.log.info( term );
        /*
         * Gather up uris
         */
        Collection<String> uris = new HashSet<>();
        for ( OntologyTerm t : allMatches ) {
            if ( t.getUri() != null ) {
                uris.add( t.getUri() );
            }
        }

        Collection<Gene> genes = this.gene2GoService.findByGOTermUris( uris, taxon );

        if ( genes.isEmpty() || ( maxGeneSetSize != null && genes.size() > maxGeneSetSize ) ) {
            return null;
        }

        GeneSet transientGeneSet = GeneSet.Factory.newInstance();
        transientGeneSet.setName( this.uri2goid( term ) );

        if ( term.getLabel() == null ) {
            GeneSetSearchImpl.log.warn( " Label for term " + term.getUri() + " was null" );
        }
        //noinspection StatementWithEmptyBody // FIXME this is an individual or a 'resource', not a 'class', but it's a real GO term. How to get the text.
        if ( term.getLabel() != null && term.getLabel().toUpperCase().startsWith( "GO_" ) ) {
        }

        transientGeneSet.setDescription( term.getLabel() );

        for ( Gene gene : genes ) {
            GeneSetMember gmember = GeneSetMember.Factory.newInstance();
            gmember.setGene( gene );
            transientGeneSet.getMembers().add( gmember );
        }
        return transientGeneSet;
    }

    private Collection<GeneSet> goTermToGeneSets( OntologyTerm term, @Nullable Integer maxGeneSetSize ) {
        if ( term.getUri() == null )
            return Collections.emptySet();

        Collection<OntologyTerm> allMatches = new HashSet<>();
        allMatches.add( term );
        allMatches.addAll( term.getChildren( false, false ) );
        GeneSetSearchImpl.log.info( term );
        /*
         * Gather up uris
         */
        Collection<String> termsToFetch = new HashSet<>();
        for ( OntologyTerm t : allMatches ) {
            if ( t.getUri() != null ) {
                termsToFetch.add( t.getUri() );
            }
        }

        Map<Taxon, Collection<Gene>> genesByTaxon = this.gene2GoService.findByGOTermUrisPerTaxon( termsToFetch );

        Collection<GeneSet> results = new HashSet<>();
        for ( Taxon t : genesByTaxon.keySet() ) {
            Collection<Gene> genes = genesByTaxon.get( t );

            if ( genes.isEmpty() || ( maxGeneSetSize != null && genes.size() > maxGeneSetSize ) ) {
                continue;
            }

            GeneSet transientGeneSet = GeneSet.Factory.newInstance();
            transientGeneSet.setName( this.uri2goid( term ) );
            transientGeneSet.setDescription( term.getLabel() );

            for ( Gene gene : genes ) {
                GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                gmember.setGene( gene );
                transientGeneSet.getMembers().add( gmember );
            }
            results.add( transientGeneSet );
        }
        return results;
    }

    private String uri2goid( OntologyTerm t ) {
        return t.getUri().replaceFirst( ".*/", "" );
    }

    private String uri2phenoID( CharacteristicValueObject t ) {
        return t.getValueUri().replaceFirst( ".*/", "" );
    }

}
