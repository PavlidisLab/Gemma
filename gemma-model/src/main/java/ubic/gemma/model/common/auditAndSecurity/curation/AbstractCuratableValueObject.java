package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;

/**
 * Created by tesarst on 07/03/17.
 */
public abstract class AbstractCuratableValueObject {

    protected Boolean troubled;

    protected AuditEvent troubledEvent;

    protected Boolean needsAttention;

    protected AuditEvent needsAttentionEvent;

    protected String curationNote;

    protected AuditEvent noteEvent;

    public AbstractCuratableValueObject() {
    }

    public AbstractCuratableValueObject( Boolean troubled, AuditEvent troubledEvent, Boolean needsAttention,
            AuditEvent needsAttentionEvent, String curationNote, AuditEvent noteEvent ) {
        this.troubled = troubled;
        this.setTroubledEvent( troubledEvent );
        this.setNeedsAttention( needsAttention );
        this.setNeedsAttentionEvent( needsAttentionEvent );
        this.setCurationNote( curationNote );
        this.setNoteEvent( noteEvent );
    }

    public Boolean getTroubled() {
        return troubled;
    }

    public void setTroubled( Boolean troubled ) {
        this.troubled = troubled;
    }

    public AuditEvent getTroubledEvent() {
        return troubledEvent;
    }

    public void setTroubledEvent( AuditEvent troubledEvent ) {
        this.troubledEvent = troubledEvent;
    }

    public Boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( Boolean needsAttention ) {
        this.needsAttention = needsAttention;
    }

    public AuditEvent getNeedsAttentionEvent() {
        return needsAttentionEvent;
    }

    public void setNeedsAttentionEvent( AuditEvent needsAttentionEvent ) {
        this.needsAttentionEvent = needsAttentionEvent;
    }

    public String getCurationNote() {
        return curationNote;
    }

    public void setCurationNote( String curationNote ) {
        this.curationNote = curationNote;
    }

    public AuditEvent getNoteEvent() {
        return noteEvent;
    }

    public void setNoteEvent( AuditEvent noteEvent ) {
        this.noteEvent = noteEvent;
    }
}
