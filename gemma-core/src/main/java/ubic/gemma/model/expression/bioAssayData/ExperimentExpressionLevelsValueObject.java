package ubic.gemma.model.expression.bioAssayData;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject.*;

@SuppressWarnings("unused") // Used in rest api
public class ExperimentExpressionLevelsValueObject implements Serializable {
    public static final String OPT_PICK_MAX = "pickmax";
    public static final String OPT_PICK_VAR = "pickvar";
    public static final String OPT_AVG = "average";

    private static final String ERROR_MSG_GENE_MISSING = "Adding a doubleVectorVO for a gene that is not in the VOs genes list.";
    private static final String GENE_SYMBOL_EMPTY = "not mapped";
    private long datasetId;
    private LinkedList<GeneElementExpressionsValueObject> geneExpressionLevels = new LinkedList<>();

    public ExperimentExpressionLevelsValueObject() {
        super();
    }

    public ExperimentExpressionLevelsValueObject( long datasetId,
            Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene, boolean keepGeneNonSpecific,
            @Nullable String conslidationMode ) {
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

    // Used in rest api
    @SuppressWarnings("unused")
    public static class GeneElementExpressionsValueObject implements Serializable {
        private static final String AVG_PREFIX = "Averaged from";
        private static final String MSG_ERR_VECS_MAX = "Can not compute max from null or 1 element vector collection";
        private static final String MSG_ERR_VECS_VAR = "Can not compute var from null or 1 element vector collection";
        private String geneOfficialSymbol;
        private Integer geneNcbiId;
        private List<VectorElementValueObject> elements = new LinkedList<>();

        public GeneElementExpressionsValueObject() {
            super();
        }

        public GeneElementExpressionsValueObject( String geneOfficialSymbol, Integer geneNcbiId,
                List<DoubleVectorValueObject> vectors, boolean keepGeneNonSpecific, @Nullable String mode ) {
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

            if ( vectors.size() > 1 && !StringUtils.isEmpty( mode ) ) { // Consolidation requested
                switch ( mode ) {
                    case ( OPT_PICK_MAX ):
                        elements.add( this.pickMax( vectors ) );
                        break;
                    case ( OPT_PICK_VAR ):
                        elements.add( this.pickVar( vectors ) );
                        break;
                    case ( OPT_AVG ):
                        elements.add( this.average( vectors ) );
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
            if ( vectors == null || vectors.size() <= 1 ) {
                throw new IllegalArgumentException( GeneElementExpressionsValueObject.MSG_ERR_VECS_MAX );
            }
            DoubleVectorValueObject max = null;
            Double avgMax = null;
            for ( DoubleVectorValueObject v : vectors ) {
                double avg = this.getMean( v.getData() );
                if ( max == null || avg > avgMax ) {
                    avgMax = avg;
                    max = v;
                }
            }
            return new VectorElementValueObject( max );
        }

        private VectorElementValueObject pickVar( List<DoubleVectorValueObject> vectors ) {
            if ( vectors == null || vectors.size() <= 1 ) {
                throw new IllegalArgumentException( GeneElementExpressionsValueObject.MSG_ERR_VECS_VAR );
            }
            DoubleVectorValueObject max = null;
            Double varMax = null;
            for ( DoubleVectorValueObject v : vectors ) {
                double avg = this.getVariance( v.getData() );
                if ( max == null || avg > varMax ) {
                    varMax = avg;
                    max = v;
                }
            }
            return new VectorElementValueObject( max );
        }

        private VectorElementValueObject average( List<DoubleVectorValueObject> vectors ) {
            StringBuilder name = new StringBuilder( GeneElementExpressionsValueObject.AVG_PREFIX );
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
            double mean = this.getMean( arr );
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

    @SuppressWarnings("unused")
    // Used in rest api
    public static class VectorElementValueObject implements Serializable {
        private String designElementName;
        private Map<String, Double> bioAssayExpressionLevels = new HashMap<>();

        public VectorElementValueObject() {
            super();
        }

        public VectorElementValueObject( DoubleVectorValueObject vector ) {
            this.designElementName = vector.getDesignElement().getName();
            this.extractProbeLevels( vector );
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
}