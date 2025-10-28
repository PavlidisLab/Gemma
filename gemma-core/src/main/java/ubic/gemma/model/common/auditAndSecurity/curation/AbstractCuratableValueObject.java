package ubic.gemma.model.common.auditAndSecurity.curation;

import gemma.gsec.util.SecurityUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.text.StringEscapeUtils;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

import java.util.Date;

/**
 * Created by tesarst on 07/03/17.
 * Abstract curatable value object that provides variables and methods for data stored in CurationDetails objects on
 * curatable objects.
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@CommonsLog
@Getter
@Setter
public abstract class AbstractCuratableValueObject<C extends Curatable> extends IdentifiableValueObject<C> {

    private static final String TROUBLE_DETAILS_NONE = "No trouble details provided.";

    private Date lastUpdated;
    private boolean troubled = false;
    private AuditEventValueObject lastTroubledEvent;
    private boolean needsAttention = false;
    private AuditEventValueObject lastNeedsAttentionEvent;
    private String curationNote;
    private AuditEventValueObject lastNoteUpdateEvent;

    /**
     * Required when using the implementing classes as a spring beans.
     */
    protected AbstractCuratableValueObject() {
        super();
    }

    protected AbstractCuratableValueObject( Long id ) {
        super( id );
    }

    protected AbstractCuratableValueObject( C curatable ) {
        super( curatable );
        this.lastUpdated = curatable.getCurationDetails().getLastUpdated();
        this.troubled = curatable.getCurationDetails().getTroubled();
        this.lastTroubledEvent = curatable.getCurationDetails().getLastTroubledEvent() != null ? new AuditEventValueObject( curatable.getCurationDetails().getLastTroubledEvent() ) : null;
        this.needsAttention = curatable.getCurationDetails().getNeedsAttention();
        this.lastNeedsAttentionEvent = curatable.getCurationDetails().getLastNeedsAttentionEvent() != null ? new AuditEventValueObject( curatable.getCurationDetails().getLastNeedsAttentionEvent() ) : null;
        if ( SecurityUtil.isUserAdmin() ) {
            this.curationNote = curatable.getCurationDetails().getCurationNote();
            this.lastNoteUpdateEvent = curatable.getCurationDetails().getLastNoteUpdateEvent() != null ? new AuditEventValueObject( curatable.getCurationDetails().getLastNoteUpdateEvent() ) : null;
        }
    }

    /**
     * Copy constructor.
     */
    protected AbstractCuratableValueObject( AbstractCuratableValueObject<C> curatable ) {
        super( curatable );
        this.lastUpdated = curatable.getLastUpdated();
        this.troubled = curatable.getTroubled();
        this.lastTroubledEvent = curatable.getLastTroubledEvent();
        this.needsAttention = curatable.getNeedsAttention();
        this.lastNeedsAttentionEvent = curatable.getLastNeedsAttentionEvent();
        if ( SecurityUtil.isUserAdmin() ) {
            this.curationNote = curatable.getCurationNote();
            this.lastNoteUpdateEvent = curatable.getLastNoteUpdateEvent();
        }
    }

    public boolean getTroubled() {
        return troubled;
    }

    public void setTroubled( Boolean troubled ) {
        this.troubled = troubled;
    }

    public boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( Boolean needsAttention ) {
        this.needsAttention = needsAttention;
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
                log.warn( "Curatable object is troubled, but has no trouble event! Id: " + this.getId() );
            } else {
                details = this.getLastTroubledEvent().toString();
            }
        }

        return htmlEscape ? StringEscapeUtils.escapeHtml4( details ) : details;
    }
}
