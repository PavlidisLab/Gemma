package ubic.gemma.core.analysis.preprocess.qc;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.util.Optional;

/**
 * Reads the per-sample sequencing QC metrics the RNA-Seq pipeline's MultiQC report carries, and
 * joins them to the experiment's bioAssays.
 * <p>
 * Read depth is also held on {@link ubic.gemma.model.expression.bioAssay.BioAssay} itself, but
 * mapping rate, duplication, GC and mismatch rate are only in the report — the schema has nowhere
 * to put them — so the report has to be read either way.
 *
 * @author gembro
 * @see SequencingQcMetrics
 */
public interface SequencingQcMetricsService {

    /**
     * Obtain the sequencing QC metrics for an experiment.
     * <p>
     * The experiment's bioAssays must already be thawed
     * ({@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService#thawBioAssays(ExpressionExperiment)}):
     * this reads their accessions and read counts.
     *
     * @return the metrics, or {@link Optional#empty()} when the experiment has neither a MultiQC
     * report nor a read count on any assay, i.e. when there is nothing to show.
     * @throws IOException if the report exists but cannot be read
     */
    Optional<SequencingQcMetrics> getSequencingQcMetrics( ExpressionExperiment ee ) throws IOException;
}
