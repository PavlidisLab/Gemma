package ubic.gemma.core.analysis.preprocess.qc;

import lombok.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Per-sample sequencing QC metrics for one experiment, read out of the RNA-Seq pipeline's MultiQC
 * report and joined to the experiment's {@link ubic.gemma.model.expression.bioAssay.BioAssay}s.
 * <p>
 * These are independent of expression similarity, which is the outlier detector's only input, so
 * they are the second piece of evidence a curator has when judging a low-correlation sample. They
 * are display-only: nothing here feeds
 * {@link ubic.gemma.core.analysis.preprocess.OutlierDetectionService}.
 *
 * @see SequencingQcMetricsService
 */
@Value
public class SequencingQcMetrics {

    /**
     * True when a MultiQC report was found and parsed. When false the rows carry only what the
     * database itself holds (see {@link SampleMetrics#getReadCount()}).
     */
    boolean reportPresent;

    /**
     * Definitions for the metric names appearing in {@link SampleMetrics#getValues()}, in the order
     * MultiQC lists them. A metric present in the data but absent from MultiQC's general-stats
     * headers still gets an entry here, with only its name filled in.
     */
    List<MetricDefinition> metrics;

    /**
     * One entry per {@link ubic.gemma.model.expression.bioAssay.BioAssay} of the experiment, ordered
     * by bioAssay id. Assays the report says nothing about are still present, with an empty
     * {@link SampleMetrics#getValues()}.
     */
    List<SampleMetrics> samples;

    /**
     * Report row keys that could not be matched to any bioAssay of the experiment. Most are SRA run
     * accessions (SRR…), which the FASTQ-level modules key by and which Gemma does not record; a
     * caller cannot resolve them either, so they are reported rather than silently dropped. Sorted,
     * and each key appears once however many modules used it.
     */
    List<String> unmatchedKeys;

    /**
     * A metric column as MultiQC describes it. Every field but {@link #getName()} may be null: the
     * general-stats headers only describe the columns MultiQC chose to display, while the data
     * carries roughly thirty per module.
     */
    @Value
    public static class MetricDefinition {
        /** Key used in {@link SampleMetrics#getValues()}, e.g. {@code uniquely_mapped_percent}. */
        String name;
        /** Short column label, e.g. {@code % Aligned}. */
        @Nullable
        String title;
        /** Longer description, e.g. {@code % Uniquely mapped reads}. */
        @Nullable
        String description;
        /** Module the metric came from, e.g. {@code STAR}, {@code fastqc}. */
        @Nullable
        String namespace;
        /** Unit suffix MultiQC appends when rendering, e.g. {@code %}. */
        @Nullable
        String suffix;
        /** Lower end of MultiQC's plotting range, when it declares one. */
        @Nullable
        Double min;
        /** Upper end of MultiQC's plotting range, when it declares one. */
        @Nullable
        Double max;
        /** True when MultiQC hides this column by default in its own report. */
        boolean hidden;
    }

    /**
     * The metrics resolved for a single bioAssay.
     */
    @Value
    public static class SampleMetrics {

        Long bioAssayId;

        /** The assay's accession (a GSM for GEO data), which is what the report rows are keyed by. */
        @Nullable
        String accession;

        /** The assay's name, for axis labels. */
        @Nullable
        String name;

        /** True when the assay is flagged as an outlier, so the caller need not join a second call. */
        boolean outlier;

        /**
         * Sample-level metrics: the values from report rows whose key IS this assay's accession.
         * Modules are merged into one map; on a name collision between modules the first module
         * MultiQC listed wins.
         */
        Map<String, Double> values;

        /**
         * Rows whose key merely STARTS with this assay's accession — one per sequencing run or
         * per mate of a paired run, which is how the FASTQ-level modules key their output.
         * <p>
         * These are NOT aggregated into {@link #getValues()}. Summarizing them would need a rule
         * per metric (a mean for a rate, a sum for a count) and no such rule is recorded anywhere,
         * so the rows are passed through as they were read.
         */
        List<RunMetrics> runs;

        /**
         * Sequencing depth for the assay. Taken from the report's {@code total_reads} when the
         * report has a sample-level row, and from
         * {@link ubic.gemma.model.expression.bioAssay.BioAssay#getSequenceReadCount()} otherwise.
         * Null when neither has one.
         */
        @Nullable
        Long readCount;

        /**
         * Where {@link #getReadCount()} came from: {@code "report"}, {@code "bioAssay"}, or null
         * when there is no read count.
         */
        @Nullable
        String readCountSource;
    }

    /**
     * One report row below the sample level — a sequencing run, or one mate of a paired run.
     */
    @Value
    public static class RunMetrics {
        /** The report's own row key, e.g. {@code GSM5029427_1} or {@code GSM5029427_SRR13191146_2}. */
        String key;
        Map<String, Double> values;
    }
}
