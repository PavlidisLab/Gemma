package ubic.gemma.model.coexpression;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.model.genome.Gene;

/**
 * @author luke The CommonCoexpressionValueObject 
 */
public class CommonCoexpressionValueObject {
    
//    private String geneName;
//    private Long geneId;
//    private String geneOfficialName;
//    private String geneType;
    
    private Gene gene;
    private Collection<QueryGeneCoexpressionDataPair> commonCoexpressionData;

    /**
     * @param gene
     * @param coexpressed
     */
    public CommonCoexpressionValueObject( Gene gene ) {
        this.gene = gene;
        
        commonCoexpressionData = new ArrayList<QueryGeneCoexpressionDataPair>();
    }
    
    public void add(QueryGeneCoexpressionDataPair coexpressed) {
        commonCoexpressionData.add( coexpressed );
    }

    /**
     * @return the geneId
     */
    public Long getGeneId() {
        return gene.getId();
    }

    /**
     * @return the geneName
     */
    public String getGeneName() {
        return gene.getName();
    }

    /**
     * @return the geneOfficialName
     */
    public String getGeneOfficialName() {
        return gene.getOfficialName();
    }
    
    /**
     * @return the geneType
     */
    public String getGeneType() {
        return Gene.class.getName();
    }
    
    /**
     * @return the collection of CoexpressionCollectionValueObjects representing the query genes this gene was coexpressed with
     */
    public Collection<QueryGeneCoexpressionDataPair> getCoexpressedQueryGenes() {
        return commonCoexpressionData;
    }
}
