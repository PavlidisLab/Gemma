package ubic.gemma.core.loader.expression.geo.model;

import lombok.Getter;

@Getter
public enum GeoSeriesType {
    EXPRESSION_PROFILING_BY_ARRAY( "Expression profiling by array" ),
    EXPRESSION_PROFILING_BY_HIGH_THROUGHPUT_SEQUENCING( "Expression profiling by high throughput sequencing" ),
    EXPRESSION_PROFILING_BY_MPSS( "Expression profiling by MPSS" ),
    EXPRESSION_PROFILING_BY_RT_PRC( "Expression profiling by RT-PCR" ),
    EXPRESSION_PROFILING_BY_SAGE( "Expression profiling by SAGE" ),
    EXPRESSION_PROFILING_BY_SNP_ARRAY( "Expression profiling by SNP array" ),
    EXPRESSION_PROFILING_BY_TILING_ARRAY( "Expression profiling by genome tiling array" ),
    GENOME_BINDING_PROFILING_BY_HIGH_THROUGHPUT_SEQUENCING( "Genome binding/occupancy profiling by high throughput sequencing" ),
    GENOME_BINDING_PROFILING_BY_TILING_ARRAY( "Genome binding/occupancy profiling by genome tiling array" ),
    GENOME_VARIATION_PROFILING_BY_ARRAY( "Genome variation profiling by array" ),
    GENOME_VARIATION_PROFILING_BY_GENOME_TILING_ARRAY( "Genome variation profiling by genome tiling array" ),
    METHYLATION_PROFILING_BY_GENOME_TILING_ARRAY( "Methylation profiling by genome tiling array" ),
    METHYLATION_PROFILING_BY_HIGH_THROUGHPUT_SEQUENCING( "Methylation profiling by high throughput sequencing" ),
    NON_CODING_RNA_PROFILING_BY_ARRAY( "Non-coding RNA profiling by array" ),
    NON_CODING_RNA_PROFILING_BY_HIGH_THROUGHPUT_SEQUENCING( "Non-coding RNA profiling by high throughput sequencing" ),
    THIRD_PARTY_REANALYSIS( "Third-party reanalysis" ),
    OTHER( "Other" );

    /**
     * Identifier of the series type as it appears in the GEO SOFT files.
     */
    private final String identifier;

    GeoSeriesType( String identifier ) {
        this.identifier = identifier;
    }
}
