package ubic.gemma.core.analysis.service;

import ubic.gemma.core.security.audit.payload.SampleMetadataPayload;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Records a {@link ubic.gemma.model.common.auditAndSecurity.eventType.SampleMetadataChangedEvent}.
 *
 * <h2>Why a separate bean</h2>
 * The {@code AuditedAspect} does not fire on a self-invoked method, so the audited call has to cross a
 * proxy boundary. This is the same co-bean hop {@link OutlierFlaggingAuditService} uses.
 */
public interface BioAssayMetadataAuditService {

    /**
     * @param ee      the experiment the changed samples belong to
     * @param note    the audit detail string
     * @param payload which field changed, to what, on which samples
     */
    void recordSampleMetadataChange( ExpressionExperiment ee, String note, SampleMetadataPayload payload );
}
