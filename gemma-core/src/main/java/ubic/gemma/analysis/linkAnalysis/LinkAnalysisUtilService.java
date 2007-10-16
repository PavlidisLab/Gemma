package ubic.gemma.analysis.linkAnalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGeneImpl;
import ubic.gemma.model.genome.ProbeAlignedRegionImpl;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;

/**
 * Providing the function
 * 
 * @spring.bean id="linkAnalysisUtilService"
 * @spring.property name="goService" ref="geneOntologyService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="geneService" ref="geneService"
 */
public class LinkAnalysisUtilService {
    private GeneOntologyService goService = null;
    private TaxonService taxonService = null;
    private GeneService geneService = null;
    private static Map<Long, Collection<OntologyTerm>> goTermsCache = Collections
            .synchronizedMap( new HashMap<Long, Collection<OntologyTerm>>() );

    public Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        taxon = taxonService.find( taxon );
        return taxon;
    }

    public int computeGOOverlap( Gene gene1, Gene gene2 ) {
        int res = 0;
        try {
            Collection<OntologyTerm> overlap = calculateGoTermOverlap( gene1, gene2 );
            if ( overlap != null ) res = overlap.size();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return res;
    }

    public int computeGOOverlap( long id1, long id2 ) {
        Gene gene1 = geneService.load( id1 );
        Gene gene2 = geneService.load( id2 );
        return computeGOOverlap( gene1, gene2 );
    }

    public Collection<OntologyTerm> getGOTerms( Gene gene ) {
        if ( goTermsCache.containsKey( gene.getId() ) ) return goTermsCache.get( gene.getId() );
        Collection<OntologyTerm> goTerms = goService.getGOTerms( gene );
        goTermsCache.put( gene.getId(), goTerms );
        return goTerms;
    }

    /**
     * @param queryGene1
     * @param queryGene2
     * @returns Collection<OntologyEntries>
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 ) throws Exception {

        if ( queryGene1 == null || queryGene2 == null ) return null;

        Collection<OntologyTerm> queryGeneTerms1 = getGOTerms( queryGene1 );
        Collection<OntologyTerm> queryGeneTerms2 = getGOTerms( queryGene2 );

        // nothing to do.
        if ( ( queryGeneTerms1 == null ) || ( queryGeneTerms1.isEmpty() ) ) return null;
        if ( ( queryGeneTerms2 == null ) || ( queryGeneTerms2.isEmpty() ) ) return null;
        Collection<OntologyTerm> overlap = new HashSet<OntologyTerm>( queryGeneTerms1 );
        overlap.retainAll( queryGeneTerms2 );
        return overlap;
    }

    public Collection<Gene> loadGenes( Taxon taxon ) {
        Collection<Gene> allGenes = geneService.getGenesByTaxon( taxon );
        Collection<Gene> genes = new HashSet<Gene>();
        for ( Gene gene : allGenes ) {
            if ( !( gene instanceof PredictedGeneImpl ) && !( gene instanceof ProbeAlignedRegionImpl ) ) {
                genes.add( gene );
            }
        }
        return genes;
    }

    public Gene getGene( String geneName, Taxon taxon ) {
        Gene gene = Gene.Factory.newInstance();
        gene.setOfficialSymbol( geneName.trim() );
        gene.setTaxon( taxon );
        gene = geneService.find( gene );
        return gene;
    }

    public void setGoService( GeneOntologyService goService ) {
        this.goService = goService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @param geneService the geneService to set
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }
}
