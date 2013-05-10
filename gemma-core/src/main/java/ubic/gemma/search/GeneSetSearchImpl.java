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

package ubic.gemma.search;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.GOGroupValueObject;
import ubic.gemma.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.genome.gene.service.GeneSetService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl;
import ubic.gemma.util.EntityUtils;

/**
 * @author paul
 * @version $Id$
 */
@Component
public class GeneSetSearchImpl implements GeneSetSearch {

    private static Log log = LogFactory.getLog( GeneSetSearchImpl.class );

    @Autowired
    private Gene2GOAssociationService gene2GoService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private GeneSetService geneSetService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<GeneSet> findByGene( Gene gene ) {
        return geneSetService.findByGene( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findGeneSetValueObjectByGoId(java.lang.String, java.lang.Long)
     */
    @Override
    public GOGroupValueObject findGeneSetValueObjectByGoId( String goId, Long taxonId ) {

        // shouldn't need to set the taxon here, should be taken care of when creating the value object
        Taxon taxon = null;

        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                log.warn( "No such taxon with id=" + taxonId );
            } else {
                GeneSet result = findByGoId( goId, taxonService.load( taxonId ) );
                if ( result == null ) {
                    log.warn( "No matching gene set found for: " + goId );
                    return null;
                }
                GOGroupValueObject ggvo = geneSetValueObjectHelper.convertToGOValueObject( result, goId, goId );

                ggvo.setTaxonId( taxon.getId() );
                ggvo.setTaxonName( taxon.getCommonName() );

                return ggvo;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findByGoId(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public GeneSet findByGoId( String goId, Taxon taxon ) {
        OntologyTerm goTerm = GeneOntologyServiceImpl.getTermForId( StringUtils.strip( goId ) );

        if ( goTerm == null ) {
            return null;
        }
        // if taxon is null, this returns a geneset with genes from different taxons
        return goTermToGeneSet( goTerm, taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findByGoTermName(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon ) {
        return findByGoTermName( goTermName, taxon, null, null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findByGoTermName(java.lang.String, ubic.gemma.model.genome.Taxon,
     * java.lang.Integer)
     */
    @Override
    public Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon, Integer maxGoTermsProcessed,
            Integer maxGeneSetSize ) {
        Collection<? extends OntologyResource> matches = this.geneOntologyService.findTerm( StringUtils
                .strip( goTermName ) );

        Collection<GeneSet> results = new HashSet<GeneSet>();

        Integer termsProcessed = 0;

        for ( OntologyResource t : matches ) {
            GeneSet converted = goTermToGeneSet( t, taxon, maxGeneSetSize );
            // converted will be null if its size is more than maxGeneSetSize
            if ( converted != null ) {
                results.add( converted );

                if ( maxGoTermsProcessed != null ) {
                    termsProcessed++;
                    if ( termsProcessed > maxGoTermsProcessed ) {
                        return results;
                    }
                }
            }
        }

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findByGoTermName(java.lang.String, ubic.gemma.model.genome.Taxon,
     * java.lang.Integer)
     */
    @Override
    public Collection<GeneSetValueObject> findByPhenotypeName( String phenotypeQuery, Taxon taxon ) {

        Collection<CharacteristicValueObject> phenotypes = phenotypeAssociationManagerService
                .searchOntologyForPhenotypes( StringUtils.strip( phenotypeQuery ), null );

        Collection<GeneSetValueObject> results = new HashSet<GeneSetValueObject>();

        StopWatch timer = new StopWatch();
        timer.start();
        log.debug( " Converting CharacteristicValueObjects collection(size:" + phenotypes.size()
                + ") into GeneSets for  phenotype query " + phenotypeQuery );
        int convertedCount = 0;
        for ( CharacteristicValueObject cvo : phenotypes ) {
            GeneSetValueObject converted = phenotypeAssociationToGeneSet( cvo, taxon );
            if ( converted != null ) {
                convertedCount++;
                results.add( converted );
            }
        }
        log.info( "added " + convertedCount + " results" );

        if ( timer.getTime() > 1000 ) {
            log.info( "Converted CharacteristicValueObjects collection(size:" + phenotypes.size()
                    + ") into GeneSets for  phenotype query " + phenotypeQuery + " in " + timer.getTime() + "ms" );
        }
        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findByName(java.lang.String)
     */
    @Override
    public Collection<GeneSet> findByName( String name ) {
        return geneSetService.findByName( StringUtils.strip( name ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findByName(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        return geneSetService.findByName( StringUtils.strip( name ), taxon );
    }

    private GeneSet goTermToGeneSet( OntologyResource term, Taxon taxon ) {
        return goTermToGeneSet( term, taxon, null );

    }

    /**
     * Convert a GO term to a 'GeneSet', including genes from all child terms.
     */
    private GeneSet goTermToGeneSet( OntologyResource term, Taxon taxon, Integer maxGeneSetSize ) {
        if ( term == null ) return null;
        if ( term.getUri() == null ) return null;

        Collection<OntologyResource> allMatches = new HashSet<OntologyResource>();

        if ( term instanceof OntologyTerm ) {
            allMatches.addAll( this.geneOntologyService.getAllChildren( ( OntologyTerm ) term ) );
        }
        allMatches.add( term );

        Collection<Gene> genes = new HashSet<Gene>();

        for ( OntologyResource t : allMatches ) {
            String goId = uri2goid( t );
            /*
             * This is a slow step. We might want to defer it. Getting a count would be faster
             */
            if ( taxon != null ) {
                genes.addAll( this.gene2GoService.findByGOTerm( goId, taxon ) );
            } else {
                genes.addAll( this.gene2GoService.findByGOTerm( goId ) );
            }

            if ( maxGeneSetSize != null && genes.size() > maxGeneSetSize ) {
                return null;
            }
        }

        GeneSet transientGeneSet = GeneSet.Factory.newInstance();
        transientGeneSet.setName( uri2goid( term ) );

        if ( term.getLabel().toUpperCase().startsWith( "GO_" ) ) {
            // hm, this is an individual or a 'resource', not a 'class', but it's a real GO term. How to get the text.
        }

        transientGeneSet.setDescription( term.getLabel() );

        for ( Gene gene : genes ) {
            GeneSetMember gmember = GeneSetMember.Factory.newInstance();
            gmember.setGene( gene );
            transientGeneSet.getMembers().add( gmember );
        }
        return transientGeneSet;
    }

    /**
     * Convert a phenotype association to a 'GeneSet', including genes from all child phenotypes.
     */
    private GeneSetValueObject phenotypeAssociationToGeneSet( CharacteristicValueObject term, Taxon taxon ) {
        if ( term == null ) return null;
        // for each phenotype, get all genes
        Set<String> URIs = new HashSet<String>();
        URIs.add( term.getValueUri() );
        Collection<GeneValueObject> gvos = phenotypeAssociationManagerService.findCandidateGenes( URIs, taxon );
        Collection<Long> geneIds = EntityUtils.getIds( gvos );

        GeneSetValueObject transientGeneSet = new GeneSetValueObject();

        transientGeneSet.setName( uri2phenoID( term ) );
        transientGeneSet.setDescription( term.getValue() );
        transientGeneSet.setGeneIds( geneIds );
        if ( !gvos.isEmpty() ) {
            transientGeneSet.setTaxonId( gvos.iterator().next().getTaxonId() );
            transientGeneSet.setTaxonName( gvos.iterator().next().getTaxonCommonName() );
        }

        return transientGeneSet;
    }

    private String uri2phenoID( CharacteristicValueObject t ) {
        return t.getValueUri().replaceFirst( ".*#", "" );
    }

    private String uri2goid( OntologyResource t ) {
        return t.getUri().replaceFirst( ".*/GO#", "" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.search.GeneSetSearch#findGeneSetsByName(java.lang.String, java.lang.Long)
     */
    @Override
    public Collection<GeneSet> findGeneSetsByName( String query, Long taxonId ) {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<GeneSet>();
        }
        Collection<GeneSet> foundGeneSets = null;
        Taxon tax = null;
        tax = taxonService.load( taxonId );

        if ( tax == null ) {
            // throw new IllegalArgumentException( "Can't locate taxon with id=" + taxonId );
            foundGeneSets = findByName( query );
        } else {
            foundGeneSets = findByName( query, tax );
        }

        foundGeneSets.clear(); // for testing general search

        /*
         * SEARCH GENE ONTOLOGY
         */

        if ( query.toUpperCase().startsWith( "GO" ) ) {
            GeneSet goSet = findByGoId( query, tax );
            if ( goSet != null ) foundGeneSets.add( goSet );
        } else {
            foundGeneSets.addAll( findByGoTermName( query, tax ) );
        }

        return foundGeneSets;
    }

}
