package ubic.gemma.core.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.payload.SampleMetadataPayload;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleMetadataChangedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

@Service
@Slf4j
public class BioAssayMetadataAuditServiceImpl implements BioAssayMetadataAuditService {

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Audited(value = SampleMetadataChangedEvent.class, messageSpel = "#note")
    public void recordSampleMetadataChange( ExpressionExperiment ee, String note, SampleMetadataPayload payload ) {
        log.debug( "Recording SampleMetadataChangedEvent for {}: {}", ee, note );
    }
}
