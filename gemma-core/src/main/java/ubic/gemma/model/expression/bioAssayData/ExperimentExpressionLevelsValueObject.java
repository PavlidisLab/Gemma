package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.genome.Gene;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused") // Used in rest api
public class ExperimentExpressionLevelsValueObject {

    private static final String ERROR_MSG_GENE_MISSING = "Adding a doubleVectorVO for a gene that is not in the VOs genes list.";
    private static final String GENE_SYMBOL_EMPTY = "not mapped";
    private long datasetId;
    private LinkedList<GeneElementExpressionsValueObject> geneExpressionLevels = new LinkedList<>();

    public ExperimentExpressionLevelsValueObject( long datasetId,
            Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene, boolean keepGeneNonSpecific ) {
        this.datasetId = datasetId;

        for ( Gene g : vectorsPerGene.keySet() ) {
            if ( g != null ) {
                this.geneExpressionLevels
                        .add( new GeneElementExpressionsValueObject( g.getOfficialSymbol(), g.getNcbiGeneId(),
                                vectorsPerGene.get( g ), keepGeneNonSpecific ) );
            }
        }
    }

    public long getDatasetId() {
        return datasetId;
    }

    public LinkedList<GeneElementExpressionsValueObject> getGeneExpressionLevels() {
        return geneExpressionLevels;
    }
}

// Used in rest api
@SuppressWarnings("unused")
class GeneElementExpressionsValueObject {
    private String geneOfficialSymbol;
    private Integer geneNcbiId;
    private List<VectorElementValueObject> elements = new LinkedList<>();

    GeneElementExpressionsValueObject( String geneOfficialSymbol, Integer geneNcbiId,
            List<DoubleVectorValueObject> vectors, boolean keepGeneNonSpecific ) {
        this.geneOfficialSymbol = geneOfficialSymbol;
        this.geneNcbiId = geneNcbiId;
        for ( DoubleVectorValueObject vector : vectors ) {
            if ( vector.getGenes().size() != 1 && !keepGeneNonSpecific ) {
                continue;
            }
            elements.add( new VectorElementValueObject( vector ) );
        }
    }

    public String getGeneOfficialSymbol() {
        return geneOfficialSymbol;
    }

    public Integer getGeneNcbiId() {
        return geneNcbiId;
    }

    public List<VectorElementValueObject> getVectors() {
        return elements;
    }
}

// Used in rest api
@SuppressWarnings("unused")
class VectorElementValueObject {
    private String designElementName;
    private Map<String, Double> sampleExpressionLevels = new HashMap<>();

    VectorElementValueObject( DoubleVectorValueObject vector ) {
        this.designElementName = vector.getDesignElement().getName();
        extractProbeLevels( vector );
    }

    public String getDesignElementName() {
        return designElementName;
    }

    public Map<String, Double> getSampleExpressionLevels() {
        return sampleExpressionLevels;
    }

    private void extractProbeLevels( DoubleVectorValueObject vector ) {

        int i;
        for ( i = 0; i < vector.getData().length; i++ ) {
            double value = vector.getData()[i];
            sampleExpressionLevels.put( vector.getBioAssays().get( i ).getName(), value );
        }
    }
}