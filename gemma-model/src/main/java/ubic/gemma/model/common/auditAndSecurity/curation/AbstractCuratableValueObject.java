package ubic.gemma.model.common.auditAndSecurity.curation;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openjena.atlas.logging.Log;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

import java.util.Date;

/**
 * Created by tesarst on 07/03/17.
 * Abstract curatable value object that provides variables and methods for data stored in CurationDetails objects on
 * curatable objects.
 */
public abstract class AbstractCuratableValueObject {

    private static final String TROUBLE_DETAILS_NONE = "Not troubled";

    protected Long id;

    protected Date lastUpdated;

    protected Boolean troubled = false;

    protected AuditEventValueObject lastTroubledEvent;

    protected Boolean needsAttention = false;

    protected AuditEventValueObject lastNeedsAttentionEvent;

    protected String curationNote;

    protected AuditEventValueObject lastNoteUpdateEvent;

    public AbstractCuratableValueObject() {
    }

    public AbstractCuratableValueObject( Long id, Date lastUpdated, Boolean troubled,
            AuditEventValueObject lastTroubledEvent, Boolean needsAttention,
            AuditEventValueObject lastNeedsAttentionEvent, String curationNote,
            AuditEventValueObject lastNoteUpdateEvent ) {
        this.id = id;
        this.lastUpdated = lastUpdated;
        this.troubled = troubled;
        this.lastTroubledEvent = lastTroubledEvent;
        this.needsAttention = needsAttention;
        this.lastNeedsAttentionEvent = lastNeedsAttentionEvent;
        this.curationNote = curationNote;
        this.lastNoteUpdateEvent = lastNoteUpdateEvent;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }

    public Boolean getTroubled() {
        return troubled;
    }

    public void setTroubled( Boolean troubled ) {
        this.troubled = troubled;
    }

    public AuditEventValueObject getLastTroubledEvent() {
        return lastTroubledEvent;
    }

    public void setLastTroubledEvent( AuditEventValueObject lastTroubledEvent ) {
        this.lastTroubledEvent = lastTroubledEvent;
    }

    public Boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( Boolean needsAttention ) {
        this.needsAttention = needsAttention;
    }

    public AuditEventValueObject getLastNeedsAttentionEvent() {
        return lastNeedsAttentionEvent;
    }

    public void setLastNeedsAttentionEvent( AuditEventValueObject lastNeedsAttentionEvent ) {
        this.lastNeedsAttentionEvent = lastNeedsAttentionEvent;
    }

    public String getCurationNote() {
        return curationNote;
    }

    public void setCurationNote( String curationNote ) {
        this.curationNote = curationNote;
    }

    public AuditEventValueObject getLastNoteUpdateEvent() {
        return lastNoteUpdateEvent;
    }

    public void setLastNoteUpdateEvent( AuditEventValueObject lastNoteUpdateEvent ) {
        this.lastNoteUpdateEvent = lastNoteUpdateEvent;
    }

    /**
     * @return a string describing the current trouble of this object. In this case, only the trouble of the Expression
     * Experiment are described. If you also need to include the Array Design trouble info, use
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject}
     */
    public String getTroubleDetails() {
        return this.getTroubleDetails( true );
    }

    public String getTroubleDetails( boolean htmlEscape ) {
        String details = TROUBLE_DETAILS_NONE;
        if ( this.getTroubled() ) {
            if ( this.getLastTroubledEvent() == null ) {
                Log.warn( this, "Curatable object is troubled, but has no trouble event! Id: " + this.getId() );
            } else {
                details = this.getLastTroubledEvent().toString();
            }
        }

        return htmlEscape ? StringEscapeUtils.escapeHtml4( details ) : details;
    }
}
