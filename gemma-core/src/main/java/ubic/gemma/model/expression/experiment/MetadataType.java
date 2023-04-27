package ubic.gemma.model.expression.experiment;

import ubic.gemma.model.expression.bioAssay.BioAssay;

public enum MetadataType {
    /**
     * A sequencing QC report.
     * <p>
     * Example: a FastQC report attached to a specific {@link BioAssay}.
     */
    SEQUENCING_QC_REPORT,
    /**
     * A sequencing alignment report.
     * <p>
     * Example: STAR's Log.final.out file on a {@link BioAssay}
     */
    SEQUENCING_ALIGNMENT_REPORT,
    /**
     * An overall sequencing report.
     * <p>
     * Example: a MultiQC report on a {@link ExpressionExperiment}
     */
    SEQUENCING_OVERALL_REPORT,
}
