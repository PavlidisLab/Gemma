package ubic.gemma.model.common.auditAndSecurity.curation;

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.openjena.atlas.logging.Log;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

import java.util.Date;

/**
 * Created by tesarst on 07/03/17.
 * Abstract curatable value object that provides variables and methods for data stored in CurationDetails objects on
 * curatable objects.
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public abstract class AbstractCuratableValueObject<C extends Curatable> extends IdentifiableValueObject<C>
        implements Comparable<AbstractCuratableValueObject<C>> {

    private static final String TROUBLE_DETAILS_NONE = "No trouble details provided.";

    protected Date lastUpdated;
    protected Boolean troubled = false;
    protected AuditEventValueObject lastTroubledEvent;
    protected Boolean needsAttention = false;
    protected AuditEventValueObject lastNeedsAttentionEvent;
    protected String curationNote;
    protected AuditEventValueObject lastNoteUpdateEvent;

    /**
     * A meta information about how many total results are available with the same filters as this
     * object.
     */
    @Deprecated
    private int _totalInQuery;

    /**
     * Required when using the implementing classes as a spring beans.
     */
    public AbstractCuratableValueObject() {
    }

    protected AbstractCuratableValueObject( Long id ) {
        super( id );
    }

    protected AbstractCuratableValueObject( C curatable ) {
        this( curatable.getId(), curatable.getCurationDetails().getLastUpdated(),
                curatable.getCurationDetails().getTroubled(),
                curatable.getCurationDetails().getLastTroubledEvent() != null ?
                        new AuditEventValueObject( curatable.getCurationDetails().getLastTroubledEvent() ) :
                        null, curatable.getCurationDetails().getNeedsAttention(),
                curatable.getCurationDetails().getLastNeedsAttentionEvent() != null ?
                        new AuditEventValueObject( curatable.getCurationDetails().getLastNeedsAttentionEvent() ) :
                        null, curatable.getCurationDetails().getCurationNote(),
                curatable.getCurationDetails().getLastNoteUpdateEvent() != null ?
                        new AuditEventValueObject( curatable.getCurationDetails().getLastNoteUpdateEvent() ) :
                        null );
    }

    protected AbstractCuratableValueObject( Long id, Date lastUpdated, Boolean troubled,
            AuditEventValueObject lastTroubledEvent, Boolean needsAttention,
            AuditEventValueObject lastNeedsAttentionEvent, String curationNote,
            AuditEventValueObject lastNoteUpdateEvent ) {
        super( id );
        this.lastUpdated = lastUpdated;
        this.troubled = troubled;
        this.lastTroubledEvent = lastTroubledEvent;
        this.needsAttention = needsAttention;
        this.lastNeedsAttentionEvent = lastNeedsAttentionEvent;
        if ( SecurityUtil.isUserAdmin() ) {
            this.curationNote = curationNote;
            this.lastNoteUpdateEvent = lastNoteUpdateEvent;
        }
    }

    /**
     * Creates a VO with the given curatable properties
     * Note that the events can still be null, as the value-change events they represent might have not occurred since
     * the curatable object was created, and they are not initialised with a creation event.
     *
     * @param id             the id of the curatable object this VO represents
     * @param curationNote   the curation note attached to the curation details
     * @param lastUpdated    the date the curatable object was last updated
     * @param needsAttention whether the object requires curators attention
     * @param troubled       whether the object is troubled
     * @param troubleEvent   the last event that updated the troubled property
     * @param attentionEvent the last event that updated the needs attention property
     * @param noteEvent      the last event that updated the curation note property
     * @param totalInBatch   the number indicating how many VOs have been returned in the database query along with this one
     */
    protected AbstractCuratableValueObject( Long id, Date lastUpdated, Boolean troubled, AuditEvent troubleEvent,
            Boolean needsAttention, AuditEvent attentionEvent, String curationNote, AuditEvent noteEvent,
            Integer totalInBatch ) {
        this( id, lastUpdated, troubled, troubleEvent == null ? null : new AuditEventValueObject( troubleEvent ),
                needsAttention, attentionEvent == null ? null : new AuditEventValueObject( attentionEvent ),
                curationNote, noteEvent == null ? null : new AuditEventValueObject( noteEvent ) );
        // meta info
        this.set_totalInQuery( totalInBatch != null ? totalInBatch : 0 );
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
    @SuppressWarnings("unused") // Used in front end
    public String getTroubleDetails() {
        return this.getTroubleDetails( true );
    }

    public String getTroubleDetails( boolean htmlEscape ) {
        String details = AbstractCuratableValueObject.TROUBLE_DETAILS_NONE;
        if ( this.getTroubled() ) {
            if ( this.getLastTroubledEvent() == null ) {
                Log.warn( this, "Curatable object is troubled, but has no trouble event! Id: " + this.getId() );
            } else {
                details = this.getLastTroubledEvent().toString();
            }
        }

        return htmlEscape ? StringEscapeUtils.escapeHtml4( details ) : details;
    }

    @Deprecated
    public final int get_totalInQuery() {
        return _totalInQuery;
    }

    @Deprecated
    protected final void set_totalInQuery( int _totalInQuery ) {
        this._totalInQuery = _totalInQuery;
    }

    @Override
    public int compareTo( AbstractCuratableValueObject<C> arg0 ) {
        if ( arg0.getId() == null || this.getId() == null ) {
            return 0;
        }
        return arg0.getId().compareTo( this.getId() );
    }
}
