package ubic.gemma.model.expression.bioAssayData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.genome.Gene;

import org.springframework.lang.Nullable;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused") // Used in rest api
@Getter
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
        this( datasetId, vectorsPerGene, keepGeneNonSpecific, conslidationMode, Collections.emptyMap() );
    }

    /**
     * Variant that carries per-gene differential-expression statistics for the contrast represented by the
     * result-set used to build the response. See {@code geneOfficialName} accessors on
     * {@link GeneElementExpressionsValueObject} for the exposed fields.
     */
    public ExperimentExpressionLevelsValueObject( long datasetId,
            Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene, boolean keepGeneNonSpecific,
            @Nullable String conslidationMode, Map<Gene, GeneDiffExStats> diffExStatsPerGene ) {
        this.datasetId = datasetId;

        for ( Map.Entry<Gene, List<DoubleVectorValueObject>> vpgEntry : vectorsPerGene.entrySet() ) {
            Gene g = vpgEntry.getKey();
            if ( g != null ) {
                GeneDiffExStats stats = diffExStatsPerGene.get( g );
                this.geneExpressionLevels
                        .add( new GeneElementExpressionsValueObject( g.getOfficialSymbol(), g.getOfficialName(),
                                g.getNcbiGeneId(), g.getEnsemblId(),
                                stats != null ? stats.correctedPvalue : null,
                                stats != null ? stats.pvalue : null,
                                stats != null ? stats.log2FoldChange : null,
                                vpgEntry.getValue(), keepGeneNonSpecific, conslidationMode ) );
            }
        }
    }

    /**
     * Per-gene differential-expression statistics carried alongside expression levels for the
     * {@code /datasets/{dataset}/expressions/differential} endpoint.
     * <p>
     * When a gene maps to multiple probes in the result set, the producer should pick the row with the most
     * significant (smallest) corrected p-value — consistent with how the endpoint ranks its top-N.
     */
    public static class GeneDiffExStats {
        @Nullable
        public final Double correctedPvalue;
        @Nullable
        public final Double pvalue;
        @Nullable
        public final Double log2FoldChange;

        public GeneDiffExStats( @Nullable Double correctedPvalue, @Nullable Double pvalue, @Nullable Double log2FoldChange ) {
            this.correctedPvalue = correctedPvalue;
            this.pvalue = pvalue;
            this.log2FoldChange = log2FoldChange;
        }
    }

    // Used in rest api
    @SuppressWarnings("unused")
    @Getter
    public static class GeneElementExpressionsValueObject implements Serializable {
        private static final String AVG_PREFIX = "Averaged from";
        private static final String MSG_ERR_VECS_MAX = "Can not compute max from null or 1 element vector collection";
        private static final String MSG_ERR_VECS_VAR = "Can not compute var from null or 1 element vector collection";
        private String geneOfficialSymbol;
        @Nullable
        @Schema(description = "Long descriptive gene name. May be null if unknown.")
        private String geneOfficialName;
        private Integer geneNcbiId;
        @Nullable
        @Schema(description = "Ensembl accession for the gene. May be null if the gene has no Ensembl mapping.")
        private String geneEnsemblId;
        /**
         * FDR-corrected p-value for the contrast represented by the result-set used to populate this VO.
         * Only set for the {@code /datasets/{id}/expressions/differential} endpoint; null otherwise.
         */
        @Nullable
        @Schema(description = "FDR-corrected p-value for the contrast represented by the result-set. Only set for the differential-expression endpoint.")
        private Double correctedPvalue;
        /**
         * Uncorrected p-value for the contrast represented by the result-set used to populate this VO.
         * Only set for the {@code /datasets/{id}/expressions/differential} endpoint; null otherwise.
         */
        @Nullable
        @Schema(description = "Uncorrected p-value for the contrast represented by the result-set. Only set for the differential-expression endpoint.")
        private Double pvalue;
        /**
         * Log2 fold change for the primary contrast of the represented result-set. For multi-contrast result
         * sets the producer picks the contrast on the gene's most-significant row (smallest corrected p-value).
         * Only set for the {@code /datasets/{id}/expressions/differential} endpoint; null otherwise.
         */
        @Nullable
        @Schema(description = "Log2 fold change for the primary contrast on the gene's most-significant probe row. Only set for the differential-expression endpoint.")
        private Double log2FoldChange;
        /**
         * Exposed via {@link #getVectors()} (legacy getter name).
         */
        @Getter(AccessLevel.NONE)
        private List<VectorElementValueObject> elements = new LinkedList<>();

        public GeneElementExpressionsValueObject() {
            super();
        }

        public GeneElementExpressionsValueObject( String geneOfficialSymbol, Integer geneNcbiId,
                List<DoubleVectorValueObject> vectors, boolean keepGeneNonSpecific, @Nullable String mode ) {
            this( geneOfficialSymbol, null, geneNcbiId, null, null, null, null, vectors, keepGeneNonSpecific, mode );
        }

        public GeneElementExpressionsValueObject( String geneOfficialSymbol, @Nullable String geneOfficialName,
                Integer geneNcbiId, @Nullable String geneEnsemblId,
                @Nullable Double correctedPvalue, @Nullable Double pvalue, @Nullable Double log2FoldChange,
                List<DoubleVectorValueObject> vectors, boolean keepGeneNonSpecific, @Nullable String mode ) {
            this.geneOfficialSymbol = geneOfficialSymbol;
            this.geneOfficialName = geneOfficialName;
            this.geneNcbiId = geneNcbiId;
            this.geneEnsemblId = geneEnsemblId;
            this.correctedPvalue = correctedPvalue;
            this.pvalue = pvalue;
            this.log2FoldChange = log2FoldChange;

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
    @Getter
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

        private void extractProbeLevels( DoubleVectorValueObject vector ) {

            int i;
            for ( i = 0; i < vector.getData().length; i++ ) {
                double value = vector.getData()[i];
                bioAssayExpressionLevels.put( vector.getBioAssays().get( i ).getName(), value );
            }
        }
    }
}
