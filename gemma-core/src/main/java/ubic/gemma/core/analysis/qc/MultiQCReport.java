package ubic.gemma.core.analysis.qc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class MultiQCReport {

    public static MultiQCReport parse( InputStream is ) throws IOException {
        return new ObjectMapper().readValue( is, MultiQCReport.class );
    }

    public static MultiQCReport parse( Reader reader ) throws IOException {
        return new ObjectMapper().readValue( reader, MultiQCReport.class );
    }

    @JsonProperty("report_data_sources")
    Map<String, Map<String, Map<String, Path>>> dataSources;

    // list of sample name -> stats mappings
    // FastQC reports are not using BioAssay names
    @JsonProperty("report_general_stats_data")
    private List<Map<String, GeneralStats>> generalStatsData;
    @JsonProperty("config_version")
    private String configVersion;

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({ @JsonSubTypes.Type(FastQcGeneralStats.class), @JsonSubTypes.Type(StarGeneralStats.class), @JsonSubTypes.Type(RsemGeneralStats.class) })
    static class GeneralStats {
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    static class FastQcGeneralStats extends GeneralStats {
        private double percent_gc;
        private double avg_sequence_length;
        private double median_sequence_length;
        private double total_sequences;
        private double percent_duplicates;
        private double percent_fails;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    static class StarGeneralStats extends GeneralStats {
        private double total_reads;
        private double avg_input_read_length;
        private double uniquely_mapped;
        private double uniquely_mapped_percent;
        private double avg_mapped_read_length;
        private double num_splices;
        private double num_annotated_splices;
        private double num_GTAG_splices;
        private double num_GCAG_splices;
        private double num_ATAC_splices;
        private double num_noncanonical_splices;
        private double mismatch_rate;
        private double deletion_rate;
        private double deletion_length;
        private double insertion_rate;
        private double insertion_length;
        private double multimapped;
        private double multimapped_percent;
        private double multimapped_toomany;
        private double multimapped_toomany_percent;
        private double unmapped_mismatches_percent;
        private double unmapped_tooshort_percent;
        private double unmapped_other_percent;
        private double unmapped_mismatches;
        private double unmapped_tooshort;
        private double unmapped_other;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    static class RsemGeneralStats extends GeneralStats {
        @JsonProperty("Unalignable")
        private double unalignable;
        @JsonProperty("Alignable")
        private double alignable;
        @JsonProperty("Filtered")
        private double filtered;
        @JsonProperty("Total")
        private double total;
        @JsonProperty("alignable_percent")
        private double alignablePercent;
        @JsonProperty("Unique")
        private double unique;
        @JsonProperty("Multi")
        private double multi;
        @JsonProperty("Uncertain")
        private double uncertain;
    }
}
