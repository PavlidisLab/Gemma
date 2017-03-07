package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;

/**
 * Created by tesarst on 07/03/17.
 */
public abstract class AbstractCuratableValueObject {

    protected Boolean troubled;

    protected AuditEvent lastTroubledEvent;

    protected Boolean needsAttention;

    protected AuditEvent lastNeedsAttentionEvent;

    protected String curationNote;

    protected AuditEvent lastCurationNoteEvent;

    public AbstractCuratableValueObject() {
    }

    public AbstractCuratableValueObject( Boolean troubled, AuditEvent lastTroubledEvent, Boolean needsAttention,
            AuditEvent lastNeedsAttentionEvent, String curationNote, AuditEvent lastCurationNoteEvent ) {
        this.troubled = troubled;
        this.setLastTroubledEvent( lastTroubledEvent );
        this.setNeedsAttention( needsAttention );
        this.setLastNeedsAttentionEvent( lastNeedsAttentionEvent );
        this.setCurationNote( curationNote );
        this.setLastCurationNoteEvent( lastCurationNoteEvent );
    }

    public Boolean getTroubled() {
        return troubled;
    }

    public void setTroubled( Boolean troubled ) {
        this.troubled = troubled;
    }

    public AuditEvent getLastTroubledEvent() {
        return lastTroubledEvent;
    }

    public void setLastTroubledEvent( AuditEvent lastTroubledEvent ) {
        this.lastTroubledEvent = lastTroubledEvent;
    }

    public Boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( Boolean needsAttention ) {
        this.needsAttention = needsAttention;
    }

    public AuditEvent getLastNeedsAttentionEvent() {
        return lastNeedsAttentionEvent;
    }

    public void setLastNeedsAttentionEvent( AuditEvent lastNeedsAttentionEvent ) {
        this.lastNeedsAttentionEvent = lastNeedsAttentionEvent;
    }

    public String getCurationNote() {
        return curationNote;
    }

    public void setCurationNote( String curationNote ) {
        this.curationNote = curationNote;
    }

    public AuditEvent getLastCurationNoteEvent() {
        return lastCurationNoteEvent;
    }

    public void setLastCurationNoteEvent( AuditEvent lastCurationNoteEvent ) {
        this.lastCurationNoteEvent = lastCurationNoteEvent;
    }
}
