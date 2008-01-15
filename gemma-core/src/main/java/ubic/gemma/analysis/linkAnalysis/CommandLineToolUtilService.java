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
 * Provides some commonly used methods that CLIs can use. In some cases you can subclass one of the specialized CLI
 * classes.
 * 
 * @spring.bean id="commandLineToolUtilService"
 * @spring.property name="goService" ref="geneOntologyService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="geneService" ref="geneService"
 * @deprecated This entire class is not needed badly. Most of the methods simply delegate to methods that are found
 *             elsewhere.
 */
public class CommandLineToolUtilService {
    private GeneOntologyService goService = null;
    private GeneService geneService = null;
    private TaxonService taxonService;

    /**
     * @param name common name of a taxon.
     * @return
     */
    public Taxon getTaxon( String name ) {
        return this.taxonService.findByCommonName( name );
    }

    /**
     * @param gene1
     * @param gene2
     * @return
     */
    public int computeGOOverlap( Gene gene1, Gene gene2 ) {
        return goService.calculateGoTermOverlap( gene1, gene2 ).size();
    }

    /**
     * @param gene
     * @return
     * @deprecated duplicates functionality found elsewhere.
     */
    public Collection<OntologyTerm> getGOTerms( Gene gene ) {
        return goService.getGOTerms( gene );
    }

    /**
     * @param taxon
     * @return Known genes (not predicted, not probe aligned regions)
     */
    @SuppressWarnings("unchecked")
    public Collection<Gene> loadKnownGenes( Taxon taxon ) {
        Collection<Gene> allGenes = geneService.getGenesByTaxon( taxon );
        Collection<Gene> genes = new HashSet<Gene>();
        for ( Gene gene : allGenes ) {
            if ( !( gene instanceof PredictedGeneImpl ) && !( gene instanceof ProbeAlignedRegionImpl ) ) {
                genes.add( gene );
            }
        }
        return genes;
    }

    /**
     * Load a gene by symbol
     * 
     * @param symbol official symbol of the gene
     * @param taxon
     * @return
     */
    public Gene getGene( String symbol, Taxon taxon ) {
        Gene gene = Gene.Factory.newInstance();
        gene.setOfficialSymbol( symbol.trim() );
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
