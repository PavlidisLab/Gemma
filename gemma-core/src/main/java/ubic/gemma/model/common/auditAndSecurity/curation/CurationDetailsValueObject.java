package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.core.security.util.SecurityUtil;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

import org.springframework.lang.Nullable;
import java.util.Date;

/**
 * Value object exposing {@link CurationDetails} over the REST API.
 * <p>
 * The {@code curationNote} and {@code lastNoteUpdateEvent} fields are admin-only and remain {@code null} for
 * non-administrators.
 */
@Getter
@Setter
public class CurationDetailsValueObject extends IdentifiableValueObject<CurationDetails> {

    @Nullable
    private Date lastUpdated;
    private boolean troubled;
    @Nullable
    private AuditEventValueObject lastTroubledEvent;
    private boolean needsAttention;
    @Nullable
    private AuditEventValueObject lastNeedsAttentionEvent;
    @Nullable
    private String curationNote;
    @Nullable
    private AuditEventValueObject lastNoteUpdateEvent;

    public CurationDetailsValueObject() {
        super();
    }

    public CurationDetailsValueObject( CurationDetails curationDetails ) {
        super( curationDetails );
        this.lastUpdated = curationDetails.getLastUpdated();
        this.troubled = curationDetails.getTroubled();
        this.lastTroubledEvent = AbstractCuratableValueObject.lastEventVo(
                curationDetails::getLastTroubledEvent, curationDetails, "lastTroubledEvent" );
        this.needsAttention = curationDetails.getNeedsAttention();
        this.lastNeedsAttentionEvent = AbstractCuratableValueObject.lastEventVo(
                curationDetails::getLastNeedsAttentionEvent, curationDetails, "lastNeedsAttentionEvent" );
        if ( SecurityUtil.isUserAdmin() ) {
            this.curationNote = curationDetails.getCurationNote();
            this.lastNoteUpdateEvent = AbstractCuratableValueObject.lastEventVo(
                    curationDetails::getLastNoteUpdateEvent, curationDetails, "lastNoteUpdateEvent" );
        }
    }
}