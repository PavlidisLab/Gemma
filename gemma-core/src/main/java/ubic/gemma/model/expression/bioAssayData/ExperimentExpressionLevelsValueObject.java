package ubic.gemma.model.expression.bioAssayData;

import org.openjena.atlas.logging.Log;
import ubic.gemma.model.genome.Gene;

import java.util.*;

@SuppressWarnings("unused") // Used in rest api
public class ExperimentExpressionLevelsValueObject {

    private static final String ERROR_MSG_GENE_MISSING = "Adding a doubleVectorVO for a gene that is not in the VOs genes list.";
    private static final String GENE_SYMBOL_EMPTY = "not mapped";
    private long datasetId;
    private LinkedList<GeneExpressionLevelsValueObject> geneExpressionLevels = new LinkedList<>();

    public ExperimentExpressionLevelsValueObject( long datasetId,
            Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene ) {
        this.datasetId = datasetId;
        for ( Map.Entry<Gene, List<DoubleVectorValueObject>> entry : vectorsPerGene.entrySet() ) {

            List<DoubleVectorValueObject> vectors = entry.getValue();
            for ( DoubleVectorValueObject vector : vectors ) {
                Gene gene = entry.getKey();
                if(gene != null){
                    if (!vector.getGenes().contains( gene.getId() ) ) {
                        Log.warn( this.getClass(), ERROR_MSG_GENE_MISSING );
                    }
                    geneExpressionLevels
                            .add( new GeneExpressionLevelsValueObject( gene.getNcbiGeneId(), gene.getOfficialSymbol(),
                                    vector ) );
                }else{
                    geneExpressionLevels
                            .add( new GeneExpressionLevelsValueObject( null, GENE_SYMBOL_EMPTY,
                                    vector ) );
                }
            }
        }
    }

    public long getDatasetId() {
        return datasetId;
    }

    public LinkedList<GeneExpressionLevelsValueObject> getGeneExpressionLevels() {
        return geneExpressionLevels;
    }
}

// Used in rest api
@SuppressWarnings("unused")
class GeneExpressionLevelsValueObject {
    private String designElementName;
    private String geneOfficialSymbol;
    private Boolean geneSpecific;
    private Integer geneNcbiId;
    private Double averageExpression;
    private Double maximumExpression = -Double.MAX_VALUE;
    private Map<String, Double> expressionLevelsPerProbe = new HashMap<>();

    GeneExpressionLevelsValueObject( Integer geneNcbiId, String geneOfficialSymbol, DoubleVectorValueObject vector ) {
        this.geneNcbiId = geneNcbiId;
        this.geneOfficialSymbol = geneOfficialSymbol;
        this.designElementName = vector.getDesignElement().getName();
        this.geneSpecific = vector.getGenes().size() == 1;
        extractProbeLevels( vector );
    }

    private void extractProbeLevels( DoubleVectorValueObject vector ) {
        int i;
        double total = 0;
        for ( i = 0; i < vector.getData().length; i++ ) {
            double value = vector.getData()[i];
            expressionLevelsPerProbe.put( vector.getBioAssays().get( i ).getName(), value );

            if ( value > maximumExpression ) {
                maximumExpression = value;
            }
            total += value;
        }
        averageExpression = total / i;
    }

    public Boolean getGeneSpecific() {
        return geneSpecific;
    }

    public String getDesignElementName() {
        return designElementName;
    }

    public Integer getGeneNcbiId() {
        return geneNcbiId;
    }

    public String getGeneOfficialSymbol() {
        return geneOfficialSymbol;
    }

    public Double getAverageExpression() {
        return averageExpression;
    }

    public Double getMaximumExpression() {
        return maximumExpression;
    }

    public Map<String, Double> getExpressionLevelsPerProbe() {
        return expressionLevelsPerProbe;
    }
}