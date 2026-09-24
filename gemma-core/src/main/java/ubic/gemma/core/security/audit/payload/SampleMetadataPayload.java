package ubic.gemma.core.security.audit.payload;

import ubic.gemma.core.security.audit.AuditEventPayload;

import java.util.List;

/**
 * What a {@link ubic.gemma.model.common.auditAndSecurity.eventType.SampleMetadataChangedEvent} changed.
 *
 * @param field       the {@link ubic.gemma.model.expression.bioAssay.BioAssay} property that was set —
 *                    {@code libraryStrategy}, {@code librarySelection} or {@code extractedMolecule}
 * @param newValue    the value it was set to, or {@code null} when the field was cleared
 * @param bioAssays   the samples that changed, as labels; samples already holding {@code newValue} are
 *                    not included, so an empty list means the request was a no-op
 */
public record SampleMetadataPayload(
        String field,
        String newValue,
        List<String> bioAssays
) implements AuditEventPayload {
}
