package ubic.gemma.cli.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.cli.util.OptionsUtils.addDateOption;

/**
 * Provide auto-seeking capabilities to a CLI.
 * <p>
 * This allows CLIs to process entities that lack certain {@link AuditEvent} or that haven't been updated since a
 * certain date.
 * @param <T> the type of entity being seeked
 */
public abstract class AbstractAutoSeekingCLI<T extends Auditable> extends AbstractAuthenticatedCLI {

    private static final String AUTO_OPTION_NAME = "auto";
    private static final String LIMITING_DATE_OPTION = "mdate";
    protected static final String FORCE_OPTION = "force";

    private final Class<T> entityClass;

    /**
     * Automatically identify which entities to run the tool on. To enable call addAutoOption.
     */
    private boolean autoSeek;

    /**
     * The event type to look for the lack of, when using auto-seek.
     */
    @Nullable
    private Class<? extends AuditEventType> autoSeekEventType;

    /**
     * Date used to identify which entities to run the tool on (e.g., those which were run less recently than mDate). To
     * enable call addLimitingDateOption.
     */
    @Nullable
    private Date limitingDate;

    /**
     * Force entities to be run, regardless of the other auto-seeking options.
     */
    private boolean force = false;

    protected AbstractAutoSeekingCLI( Class<T> entityClass ) {
        this.entityClass = entityClass;
    }

    /**
     * Add the {@code -auto} option.
     * <p>
     * The auto option value can be retrieved with {@link #isAutoSeek()}.
     */
    protected void addAutoOption( Options options ) {
        Assert.state( !options.hasOption( AUTO_OPTION_NAME ), "The -" + AUTO_OPTION_NAME + " option was already added." );
        options.addOption( Option.builder( AUTO_OPTION_NAME )
                .desc( "Attempt to process entities that need processing based on workflow criteria." )
                .build() );
    }

    /**
     * Add the {@code -auto} option for a specific {@link AuditEventType}.
     * <p>
     * The event type can be retrieved with {@link #getAutoSeekEventType()}.
     */
    protected void addAutoOption( Options options, Class<? extends AuditEventType> autoSeekEventType ) {
        addAutoOption( options );
        this.autoSeekEventType = autoSeekEventType;
    }

    /**
     * Add the {@code -mdate} option.
     * <p>
     * The limiting date can be retrieved with {@link #getLimitingDate()}.
     */
    protected void addLimitingDateOption( Options options ) {
        Assert.state( !options.hasOption( LIMITING_DATE_OPTION ), "The -" + LIMITING_DATE_OPTION + " option was already added." );
        addDateOption( options, LIMITING_DATE_OPTION, null, "Constrain to run only on entities with analyses older than the given date. "
                + "For example, to run only on entities that have not been analyzed in the last 10 days, use '-10d'. "
                + "If there is no record of when the analysis was last run, it will be run." );
    }

    protected void addForceOption( Options options ) {
        String desc = "Ignore other reasons for skipping entities (e.g., troubled experiments) and overwrite existing data (see documentation for this tool to see exact behavior if not clear)";
        addForceOption( options, desc );
    }

    protected void addForceOption( Options options, String description ) {
        Assert.state( !force, "Force mode is enabled for this CLI, you cannot add the -force/--force option." );
        Assert.state( !options.hasOption( FORCE_OPTION ), "The -" + FORCE_OPTION + " option was already added." );
        options.addOption( FORCE_OPTION, "force", false, description );
    }

    /**
     * Indicate if auto-seek is enabled.
     */
    protected boolean isAutoSeek() {
        return autoSeek;
    }

    /**
     * Indicate the event to be used for auto-seeking.
     */
    protected Class<? extends AuditEventType> getAutoSeekEventType() {
        return requireNonNull( autoSeekEventType, "This CLI was not configured with a specific event type for auto-seek." );
    }

    /**
     * Obtain the limiting date (i.e. starting date at which entities should be processed).
     */
    @Nullable
    protected Date getLimitingDate() {
        if ( limitingDate != null ) {
            log.info( "Analyses will be run only if last was older than " + limitingDate );
        }
        return limitingDate;
    }

    /**
     * Check if forcing is enabled.
     */
    protected boolean isForce() {
        return force;
    }

    /**
     * Enable the forcing mode.
     */
    protected void setForce() {
        Assert.state( !this.force, "Force mode is already enabled." );
        this.force = true;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( LIMITING_DATE_OPTION ) && commandLine.hasOption( AUTO_OPTION_NAME ) ) {
            throw new IllegalArgumentException( String.format( "Please only select one of -%s or -%s", LIMITING_DATE_OPTION, AUTO_OPTION_NAME ) );
        }

        if ( commandLine.hasOption( LIMITING_DATE_OPTION ) ) {
            this.limitingDate = commandLine.getParsedOptionValue( LIMITING_DATE_OPTION );
        }

        this.autoSeek = commandLine.hasOption( AUTO_OPTION_NAME );

        if ( commandLine.hasOption( FORCE_OPTION ) ) {
            this.force = true;
        }
    }

    protected final void addSuccessObject( T successObject, String message ) {
        addSuccessObject( toBatchObject( successObject ), message );
    }

    protected final void addSuccessObject( T successObject ) {
        addSuccessObject( toBatchObject( successObject ) );
    }

    protected final void addWarningObject( @Nullable T warningObject, String message ) {
        addWarningObject( toBatchObject( warningObject ), message );
    }

    protected final void addWarningObject( @Nullable T warningObject, String message, Throwable throwable ) {
        addWarningObject( toBatchObject( warningObject ), message, throwable );
    }

    protected final void addErrorObject( @Nullable T errorObject, String message, Throwable throwable ) {
        addErrorObject( toBatchObject( errorObject ), message, throwable );
    }

    protected final void addErrorObject( @Nullable T errorObject, String message ) {
        addErrorObject( toBatchObject( errorObject ), message );
    }

    protected final void addErrorObject( @Nullable T errorObject, Exception exception ) {
        addErrorObject( toBatchObject( errorObject ), exception );
    }

    /**
     * Convert the given object to a serializable object for batch processing.
     */
    protected abstract Serializable toBatchObject( @Nullable T object );

    /**
     * Check if the given auditable can be skipped.
     * @param auditable  auditable
     * @param eventClass can be null
     * @return boolean
     */
    protected boolean noNeedToRun( T auditable, @Nullable Class<? extends AuditEventType> eventClass ) {
        if ( force ) {
            return false;
        }

        Date skipIfLastRunLaterThan = this.getLimitingDate();
        List<AuditEvent> events = getApplicationContext()
                .getBean( AuditEventService.class )
                .getEvents( auditable );

        // figure out if we need to run it by date; or if there is no event of the given class; "Fail" type events don't
        // count.
        for ( int j = events.size() - 1; j >= 0; j-- ) {
            AuditEvent event = events.get( j );
            if ( event == null ) {
                continue; // legacy of ordered-list which could end up with gaps; should not be needed any more
            }
            AuditEventType eventType = event.getEventType();
            if ( eventClass != null && eventClass.isInstance( eventType ) && !eventType.getClass().getSimpleName().startsWith( "Fail" ) ) {
                if ( skipIfLastRunLaterThan != null ) {
                    if ( event.getDate().after( skipIfLastRunLaterThan ) ) {
                        log.info( auditable + ": " + " run more recently than " + skipIfLastRunLaterThan );
                        addErrorObject( auditable, "Run more recently than " + skipIfLastRunLaterThan + ", use - " + FORCE_OPTION + "to process anyway." );
                        return true;
                    }
                } else {
                    // it has been run already at some point
                    return true;
                }
            }
        }

        if ( auditable instanceof Curatable ) {
            Curatable curatable = ( Curatable ) auditable;
            if ( curatable.getCurationDetails().getTroubled() ) {
                /*
                 * Always skip if the object is curatable and troubled
                 */
                addErrorObject( auditable, "Has an active troubled flag, use -" + FORCE_OPTION + " to process anyway." );
                return true;
            }
        }

        return false;
    }
}
