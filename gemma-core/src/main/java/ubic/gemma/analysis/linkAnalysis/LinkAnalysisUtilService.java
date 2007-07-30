package ubic.gemma.analysis.linkAnalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
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
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="ppService" ref="probe2ProbeCoexpressionService"
 * @spring.property name="goService" ref="geneOntologyService"
 * @spring.property name="goAssociationService" ref="gene2GOAssociationService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="geneService" ref="geneService"
 */
public class LinkAnalysisUtilService {
    private ExpressionExperimentService eeService;
    private Probe2ProbeCoexpressionService ppService = null;
    private GeneOntologyService goService = null;
    private Gene2GOAssociationService goAssociationService = null;
    private TaxonService taxonService = null;
    private GeneService geneService = null;
    private static Map<Long, Collection<OntologyTerm>> goTermsCache = Collections.synchronizedMap(new HashMap<Long, Collection<OntologyTerm>>());
    
    public Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        taxon = taxonService.find( taxon );
        return taxon;
    }
    private boolean goStatus(){
        int waiting_time = 100000;
        try {
        	while (  !goService.isGeneOntologyLoaded() ){
        			Thread.sleep(500);
        			waiting_time = waiting_time - 500;
        			if(waiting_time <= 0) return false;
        	}
        }catch(Exception e){
        	e.printStackTrace();
        	return false;
        }
    	return true;
    }
    public int computeGOOverlap( Gene gene1, Gene gene2 ) {
    	int res = 0;
    	try{
    		Collection<OntologyTerm> overlap = calculateGoTermOverlap(gene1, gene2);
    		if(overlap != null)
    			res = overlap.size();
    	}catch(Exception e){
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
    	if(goTermsCache.containsKey(gene.getId()))
    		return goTermsCache.get(gene.getId());
    	Collection<OntologyTerm> goTerms = goService.getGOTerms(gene);
    	goTermsCache.put(gene.getId(), goTerms);
    	return goTerms;
    }
    /**
     * @param queryGene1
     * @param queryGene2
     * @returns Collection<OntologyEntries>
     * @throws Exception
    
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 )
            throws Exception {

        if ( queryGene1 == null  || queryGene2 == null) return null;
        
        Collection<OntologyTerm> queryGeneTerms1 = getGOTerms( queryGene1 );
        Collection<OntologyTerm> queryGeneTerms2 = getGOTerms( queryGene2 );

        // nothing to do.
        if ( ( queryGeneTerms1 == null ) || ( queryGeneTerms1.isEmpty() ) ) return null;
        if ( ( queryGeneTerms2 == null ) || ( queryGeneTerms2.isEmpty() ) ) return null;
        

        
        Collection<String> termURI = new HashSet<String>();
        for(OntologyTerm goTerm:queryGeneTerms2){
        	termURI.add(goTerm.getUri());
        }
        Collection<OntologyTerm> overlap = new HashSet<OntologyTerm>();
        for(OntologyTerm goTerm:queryGeneTerms1){
        	if(termURI.contains(goTerm.getUri())) overlap.add(goTerm);
        }
        queryGeneTerms1.retainAll(queryGeneTerms2);
        //return queryGeneTerms1;
//        if(overlap.size() != queryGeneTerms1.size()){
//        	System.err.print("Overlap "+"\t");
//        	for(OntologyTerm goTerm:overlap){
//        		System.err.print(goTerm.getUri() + "\t");
//        	}
//        	System.err.println();
//        	System.err.print("RetainAll "+"\t");
//        	for(OntologyTerm goTerm:queryGeneTerms1){
//        		System.err.print(goTerm.getUri() + "\t");
//        	}
//        	System.err.println();
//        	
//        	System.err.print("QueryTerm2 "+"\t");
//        	for(OntologyTerm goTerm:queryGeneTerms2){
//        		System.err.print(goTerm.getUri() + "\t");
//        	}
//        	System.err.println();
//        	System.exit(0);
//        }
        return overlap;
    }

    public Collection<Gene> loadGenes(Taxon taxon){
    	Collection <Gene> allGenes = geneService.getGenesByTaxon(taxon);
    	Collection <Gene> genes = new HashSet<Gene>();
    	for(Gene gene:allGenes){
    		if(!(gene instanceof PredictedGeneImpl) && !(gene instanceof ProbeAlignedRegionImpl)){
    			genes.add(gene);
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

	public void setEeService(ExpressionExperimentService eeService) {
		this.eeService = eeService;
	}
	public void setGoAssociationService(
			Gene2GOAssociationService goAssociationService) {
		this.goAssociationService = goAssociationService;
	}
	public void setGoService(GeneOntologyService goService) {
		this.goService = goService;
	}
	public void setPpService(Probe2ProbeCoexpressionService ppService) {
		this.ppService = ppService;
	}
	public void setTaxonService(TaxonService taxonService) {
		this.taxonService = taxonService;
	}
    /**
     * @param geneService the geneService to set
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }
}
