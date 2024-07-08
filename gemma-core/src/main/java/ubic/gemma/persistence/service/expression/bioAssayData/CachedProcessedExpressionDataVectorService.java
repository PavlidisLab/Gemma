package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;

import java.util.Collection;

/**
 * A service that provides cached {@link ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector} in the
 * form of {@link DoubleVectorValueObject}.
 */
public interface CachedProcessedExpressionDataVectorService {

    /**
     * Retrieve processed vectors for a given experiment or subset.
     */
    Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment );

    /**
     * Retrieve processed vectors by genes for a given experiment or subset.
     */
    Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment, Collection<Long> genes );

    /**
     * Retrieve random processed vectors for a given experiment or subset.
     */
    Collection<DoubleVectorValueObject> getRandomProcessedDataArrays( BioAssaySet expressionExperiment, int limit );

    /**
     * Retrieve processed vectors by genes and experiments.
     */
    Collection<DoubleVectorValueObject> getProcessedDataArrays( Collection<? extends BioAssaySet> expressionExperiments, Collection<Long> genes );

    /**
     * Retrieves processed vectors by probes and experiments
     *
     * @param expressionExperiments EEs
     * @param probes composite sequences
     * @return double vector vos
     */
    Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( Collection<? extends BioAssaySet> expressionExperiments, Collection<CompositeSequence> probes );

    Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet ee, Collection<Long> probes );
}
