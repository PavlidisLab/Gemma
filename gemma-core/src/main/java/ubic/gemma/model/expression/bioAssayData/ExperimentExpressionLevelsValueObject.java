package ubic.gemma.model.expression.bioAssayData;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.genome.Gene;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject.*;

@SuppressWarnings("unused") // Used in rest api
public class ExperimentExpressionLevelsValueObject {
    public static final String OPT_PICK_MAX = "pickmax";
    public static final String OPT_PICK_VAR = "pickvar";
    public static final String OPT_AVG = "average";

    private static final String ERROR_MSG_GENE_MISSING = "Adding a doubleVectorVO for a gene that is not in the VOs genes list.";
    private static final String GENE_SYMBOL_EMPTY = "not mapped";
    private long datasetId;
    private LinkedList<GeneElementExpressionsValueObject> geneExpressionLevels = new LinkedList<>();

    public ExperimentExpressionLevelsValueObject( long datasetId,
            Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene, boolean keepGeneNonSpecific,
            String conslidationMode ) {
        this.datasetId = datasetId;

        for ( Gene g : vectorsPerGene.keySet() ) {
            if ( g != null ) {
                this.geneExpressionLevels
                        .add( new GeneElementExpressionsValueObject( g.getOfficialSymbol(), g.getNcbiGeneId(),
                                vectorsPerGene.get( g ), keepGeneNonSpecific, conslidationMode ) );
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
    private static final String AVG_PREFIX = "Averaged from";
    private String geneOfficialSymbol;
    private Integer geneNcbiId;
    private List<VectorElementValueObject> elements = new LinkedList<>();

    GeneElementExpressionsValueObject( String geneOfficialSymbol, Integer geneNcbiId,
            List<DoubleVectorValueObject> vectors, boolean keepGeneNonSpecific, String mode ) {
        this.geneOfficialSymbol = geneOfficialSymbol;
        this.geneNcbiId = geneNcbiId;

        if ( vectors == null ) {
            return;
        }

        if ( !keepGeneNonSpecific ) { // Pre process
            List<DoubleVectorValueObject> processed = new LinkedList<>();
            for ( DoubleVectorValueObject vo : vectors ) {
                if ( vo.getGenes().size() == 1 ) { // Only including gene-nonspecific vectors
                    processed.add( vo );
                }
            }
            vectors = processed;
        }

        if ( vectors.size() > 1 && !Strings.isNullOrEmpty( mode ) ) { // Consolidation requested
            switch ( mode ) {
                case ( OPT_PICK_MAX ):
                    elements.add( pickMax( vectors ) );
                    break;
                case ( OPT_PICK_VAR ):
                    elements.add( pickVar( vectors ) );
                    break;
                case ( OPT_AVG ):
                    elements.add( average( vectors ) );
                    break;
            }
        } else { // Add all vectors
            for ( DoubleVectorValueObject vector : vectors ) {
                elements.add( new VectorElementValueObject( vector ) );
            }
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

    private VectorElementValueObject pickMax( List<DoubleVectorValueObject> vectors ) {
        DoubleVectorValueObject max = null;
        Double avgMax = null;
        for ( DoubleVectorValueObject v : vectors ) {
            double avg = getMean( v.getData() );
            if ( max == null || avg > avgMax ) {
                avgMax = avg;
                max = v;
            }
        }
        return new VectorElementValueObject( max );
    }

    private VectorElementValueObject pickVar( List<DoubleVectorValueObject> vectors ) {
        DoubleVectorValueObject max = null;
        Double varMax = null;
        for ( DoubleVectorValueObject v : vectors ) {
            double avg = getVariance( v.getData() );
            if ( max == null || avg > varMax ) {
                varMax = avg;
                max = v;
            }
        }
        return new VectorElementValueObject( max );
    }

    private VectorElementValueObject average( List<DoubleVectorValueObject> vectors ) {
        StringBuilder name = new StringBuilder( AVG_PREFIX );
        Map<String, Double> bioAssayValues = new HashMap<>();

        for ( DoubleVectorValueObject vo : vectors ) {

            for ( int i = 0; i < vo.getBioAssays().size(); i++ ) {
                BioAssayValueObject bvo = vo.getBioAssays().get( i );

                if ( bioAssayValues.containsKey( bvo.getName() ) ) {
                    bioAssayValues.put( bvo.getName(), bioAssayValues.get( bvo.getName() ) + vo.getData()[i] );
                } else {
                    bioAssayValues.put( bvo.getName(), vo.getData()[i] );
                }
            }

            name.append( " " );
            name.append( vo.getDesignElement().getName() );
        }

        for ( Map.Entry<String, Double> entry : bioAssayValues.entrySet() ) {
            entry.setValue( entry.getValue() / vectors.size() );
        }

        return new VectorElementValueObject( name.toString(), bioAssayValues );
    }

    private double getVariance( double[] arr ) {
        double mean = getMean( arr );
        double sum = 0;

        for ( double d : arr ) {
            sum += ( d - mean ) * ( d - mean );
        }

        return sum / ( arr.length - 1 );
    }

    private double getMean( double[] arr ) {
        double sum = 0;

        for ( double d : arr ) {
            sum += d;
        }

        return sum / arr.length;
    }
}

// Used in rest api
@SuppressWarnings("unused")
class VectorElementValueObject {
    private String designElementName;
    private Map<String, Double> bioAssayExpressionLevels = new HashMap<>();

    VectorElementValueObject( DoubleVectorValueObject vector ) {
        this.designElementName = vector.getDesignElement().getName();
        extractProbeLevels( vector );
    }

    public VectorElementValueObject( String designElementName, Map<String, Double> bioAssayValues ) {
        this.designElementName = designElementName;
        for ( Map.Entry<String, Double> entry : bioAssayValues.entrySet() ) {
            bioAssayExpressionLevels.put( entry.getKey(), entry.getValue() );
        }
    }

    public String getDesignElementName() {
        return designElementName;
    }

    public Map<String, Double> getBioAssayExpressionLevels() {
        return bioAssayExpressionLevels;
    }

    private void extractProbeLevels( DoubleVectorValueObject vector ) {

        int i;
        for ( i = 0; i < vector.getData().length; i++ ) {
            double value = vector.getData()[i];
            bioAssayExpressionLevels.put( vector.getBioAssays().get( i ).getName(), value );
        }
    }
}