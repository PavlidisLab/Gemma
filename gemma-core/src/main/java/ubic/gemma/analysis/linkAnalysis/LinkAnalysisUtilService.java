package ubic.gemma.analysis.linkAnalysis;

import java.util.Collection;
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
    
    public Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        taxon = taxonService.find( taxon );
        return taxon;
    }
    public Collection<OntologyTerm> getGoTerms( Gene gene) {
    	return getGoTerms(gene, "");
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
    public Collection<OntologyTerm> getGoTerms( Gene gene, String category ) {
        if(!goStatus()) return null;
        
        Collection<OntologyTerm> annotatedGoEntries = goAssociationService.findByGene( gene );
        Collection<OntologyTerm> allGoEntriesInBP = new HashSet<OntologyTerm>();
        for ( OntologyTerm entry : annotatedGoEntries ) {
            if ( entry.getLabel().toUpperCase().contains( category ) ) {
                Collection<OntologyTerm> parentEntries = goService.getAllParents( entry );
                allGoEntriesInBP.add( entry );
                for ( OntologyTerm parentEntry : parentEntries ) {
//                    if ( goService.asRegularGoId( parentEntry ) != null
//                            && !geneOntologyService.asRegularGoId( parentEntry ).contains( "GO:0008150" ) ) {
//                        allGoEntriesInBP.add( parentEntry );
//                    }
                	if ( goService.asRegularGoId( parentEntry ) != null)
                		allGoEntriesInBP.add( parentEntry );
                }
            }
        }
        return allGoEntriesInBP;
    }
    public int computeGOOverlap( Gene gene1, Gene gene2 ) {
        int res = 0;
        if(!goStatus()) return res;
        Collection<Long> geneIds = new HashSet<Long>();
        geneIds.add( gene2.getId() );
        try{
            Map<Long, Collection<OntologyTerm>> overlapMap = goService.calculateGoTermOverlap(gene1, geneIds );
            if ( overlapMap != null ) {
                Collection<OntologyTerm> overlapGOTerms = overlapMap.get( gene2.getId() );
                if ( overlapGOTerms != null ) res = overlapGOTerms.size();
            }
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
