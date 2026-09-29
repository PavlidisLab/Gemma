package ubic.gemma.model.common.auditAndSecurity.eventType;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A curator changed upstream-derived metadata on one or more of an experiment's samples —
 * {@code libraryStrategy}, {@code librarySelection} or {@code extractedMolecule}.
 *
 * <h2>Why this exists</h2>
 * Those three columns are written by {@code GeoConverterImpl} at import and by nothing else, so before
 * this event the only way to correct one was direct SQL, which emits no audit event at all. The event is
 * against the {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}, not the sample:
 * {@link ubic.gemma.model.expression.bioAssay.BioAssay} is a
 * {@link ubic.gemma.model.common.auditAndSecurity.SecuredChild}, not an
 * {@link ubic.gemma.model.common.auditAndSecurity.Auditable}. The samples that changed are named in the
 * typed payload rather than the detail string.
 */
@Entity
@DiscriminatorValue("SampleMetadataChangedEvent")
public class SampleMetadataChangedEvent extends ExpressionExperimentAnalysisEvent {

}
