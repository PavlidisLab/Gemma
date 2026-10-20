package ubic.gemma.model.common.auditAndSecurity.curation;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * <p>
 * {@code curationPending} is not read from {@link CurationDetails} at all — it comes from the curation lock and
 * is supplied by the caller, so it is {@code null} unless the reading path had the lock in hand.
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

    /**
     * Whether curation of the dataset is under way right now.
     * <p>
     * Administrators only, like {@code curationNote} beside it. Derived from the curation lock: true exactly
     * while an unexpired claim exists. It names nobody, and it lapses with the lease rather than waiting on a
     * sign-off, so a curator who only relabels does not leave it stuck on.
     */
    @Nullable
    @Schema(description = "Administrators only; null for everyone else. True while someone holds an unexpired "
            + "curation lock on the dataset: curation is under way, so treat what you read as provisional. It "
            + "says nothing about who is curating — no holder, run or agent name is exposed here at any "
            + "authorization level — and it clears itself when the lease lapses. Also null when the reading "
            + "path did not consult the lock.")
    private Boolean curationPending;

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

    /**
     * As {@link #CurationDetailsValueObject(CurationDetails)}, plus the curation-lock state the caller already
     * holds.
     *
     * @param curationPending whether an unexpired curation lock exists on the dataset, or {@code null} when the
     *                        caller did not look. Kept only for administrators.
     */
    public CurationDetailsValueObject( CurationDetails curationDetails, @Nullable Boolean curationPending ) {
        this( curationDetails );
        if ( SecurityUtil.isUserAdmin() ) {
            this.curationPending = curationPending;
        }
    }
}