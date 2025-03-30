/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.cli.util.AbstractAutoSeekingCLI;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.FileUtils;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.util.*;

import static ubic.gemma.cli.util.EntityOptionsUtils.addPlatformOption;

/**
 * Aggregates functionality useful when writing CLIs that need to get an array design from the database and do something
 * with it.
 *
 * @author pavlidis
 */
public abstract class ArrayDesignSequenceManipulatingCli extends AbstractAutoSeekingCLI<ArrayDesign> {

    @Autowired
    private ArrayDesignReportService arrayDesignReportService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    protected AuditTrailService auditTrailService;
    @Autowired
    protected AuditEventService auditEventService;
    @Autowired
    protected EntityLocator entityLocator;

    private Collection<ArrayDesign> arrayDesignsToProcess = new HashSet<>();

    protected ArrayDesignSequenceManipulatingCli() {
        super( ArrayDesign.class );
        setRequireLogin();
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    protected Collection<ArrayDesign> getArrayDesignsToProcess() {
        return arrayDesignsToProcess;
    }

    @Override
    protected void buildOptions( Options options ) {
        addPlatformOption( options, "a", "array", "Platform ID, short name or name; or comma-delimited list of these" );

        Option eeFileListOption = Option.builder( "f" ).hasArg().argName( "File containing platform identifiers" )
                .desc( "File with list of short names or IDs of platforms (one per line; use instead of '-a')" )
                .longOpt( "adListFile" ).build();
        options.addOption( eeFileListOption );

        this.addLimitingDateOption( options );
        this.addAutoOption( options );
        this.addBatchOption( options );
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( 'a' ) ) {
            this.arraysFromCliList( commandLine );
        } else if ( commandLine.hasOption( 'f' ) ) {
            String experimentListFile = commandLine.getOptionValue( 'f' );
            log.info( "Reading arrayDesigns list from " + experimentListFile );
            try {
                this.arrayDesignsToProcess = this.readListFile( experimentListFile );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    protected ArrayDesignReportService getArrayDesignReportService() {
        return arrayDesignReportService;
    }

    protected ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesign the array design to check
     * @return true if the sequences on the given array design would be equivalently treated by analyzing another array
     *         design. In the case of subsumption, this only works if the array design has been either analyzed for
     *         subsuming status. (the analysis is not done as part of this call).
     */
    protected boolean isSubsumedOrMerged( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getSubsumingArrayDesign() != null ) {
            log.info( arrayDesign + " is subsumed by " + arrayDesign.getSubsumingArrayDesign().getId() );
            return true;
        }

        if ( arrayDesign.getMergedInto() != null ) {
            log.info( arrayDesign + " is merged into " + arrayDesign.getMergedInto().getId() );
            return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    protected boolean shouldRun( Date skipIfLastRunLaterThan, ArrayDesign design,
            Class<? extends ArrayDesignAnalysisEvent> cls ) {
        if ( design.getTechnologyType().equals( TechnologyType.GENELIST ) || design.getTechnologyType().equals( TechnologyType.SEQUENCING ) ) {
            log.warn( design + " is not a microarray platform (it doesn't have sequences) so it will not be run" );
            // not really an error, but nice to get notification.
            // TODO: use a warning instead of an error as it will cause a non-zero exit code
            addErrorObject( design, "Skipped because it is not a microarray platform." );
            return false;
        }

        if ( this.isSubsumedOrMerged( design ) ) {
            log.warn( design
                    + " is subsumed or merged into another design, it will not be run; instead process the 'parent' platform" );

            // not really an error, but nice to get notification.
            // TODO: use a warning instead of an error as it will cause a non-zero exit code
            addErrorObject( design, "Skipped because it is subsumed by or merged into another design." );
            return false;
        }

        if ( !this.needToRun( skipIfLastRunLaterThan, design, cls ) ) {
            if ( skipIfLastRunLaterThan != null ) {
                addErrorObject( design, "Skipped because it was last run after " + skipIfLastRunLaterThan );
            } else {
                addErrorObject( design, "Seems to be up to date or is not ready to run" );
            }
            return false;
        }
        return true;
    }

    /**
     * Mergees or subsumees of the platform.
     *
     * @param design a platform
     * @return related platforms
     */
    Collection<ArrayDesign> getRelatedDesigns( ArrayDesign design ) {
        Collection<ArrayDesign> toUpdate = new HashSet<>();
        toUpdate.addAll( design.getMergees() );
        toUpdate.addAll( design.getSubsumedArrayDesigns() );
        return toUpdate;
    }

    /**
     *
     * @param eventClass e.g., ArrayDesignSequenceAnalysisEvent.class
     * @return true if skipIfLastRunLaterThan is null, or there is no record of a previous analysis, or if the last
     *         analysis was run before skipIfLastRunLaterThan. false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    // Better semantics
    protected boolean needToRun( Date skipIfLastRunLaterThan, ArrayDesign arrayDesign,
            Class<? extends ArrayDesignAnalysisEvent> eventClass ) {

        if ( isAutoSeek() ) {
            return this.needToAutoRun( arrayDesign, eventClass );
        }

        if ( skipIfLastRunLaterThan == null )
            return true;

        List<AuditEvent> events = this.getEvents( arrayDesign, eventClass );
        if ( events.isEmpty() ) {
            return true; // always do it, it's never been done.
        }
        // return true if the last time was older than the limit time.
        AuditEvent lastEvent = events.get( events.size() - 1 );
        return lastEvent.getDate().before( skipIfLastRunLaterThan );

    }

    private void arraysFromCliList( CommandLine commandLine ) {
        String arrayShortNames = commandLine.getOptionValue( 'a' );
        String[] shortNames = arrayShortNames.split( "," );

        for ( String shortName : shortNames ) {
            if ( StringUtils.isBlank( shortName ) )
                continue;
            ArrayDesign ad = entityLocator.locateArrayDesign( shortName );
            arrayDesignsToProcess.add( ad );
        }
        if ( arrayDesignsToProcess.isEmpty() ) {
            throw new RuntimeException( "There were no valid platforms specified" );
        }
    }

    /**
     * @param eventClass if null, then all events are added.
     */
    private List<AuditEvent> getEvents( ArrayDesign arrayDesign,
            Class<? extends ArrayDesignAnalysisEvent> eventClass ) {
        List<AuditEvent> events = new ArrayList<>();

        for ( AuditEvent event : this.auditEventService.getEvents( arrayDesign ) ) {
            if ( event == null )
                continue;
            if ( eventClass == null || ( event.getEventType() != null && eventClass
                    .isAssignableFrom( event.getEventType().getClass() ) ) ) {
                events.add( event );
            }
        }
        return events;
    }

    /**
     * Find if the most recent ArrayDesignAnalysisEvent is less recent than the _other_ types of array design events; if
     * so, then we need to refresh it.
     * <ul>
     * <li>If the autoseek option is not turned on, then return false.
     * <li>If the event has never been done, return true.
     * <li>If the last event was of the passed eventClass, then return false.
     * <li>If any other ArrayDesignAnalysisEvent was more recent than the last event of eventClass, return true.
     * <li>Otherwise return false.
     * </ul>
     *
     * @param eventClass The type of event we are considering running on the basis of this call.
     * @return whether the array design needs updating based on the criteria outlined above.
     */
    private boolean needToAutoRun( ArrayDesign arrayDesign, Class<? extends ArrayDesignAnalysisEvent> eventClass ) {
        if ( !isAutoSeek() ) {
            return false;
        }

        List<AuditEvent> eventsOfCurrentType = this.getEvents( arrayDesign, eventClass );
        List<AuditEvent> allEvents = arrayDesign.getAuditTrail().getEvents();

        if ( eventsOfCurrentType.isEmpty() ) {
            // it's never been run.
            return true;
        }

        AuditEvent lastEventOfCurrentType = eventsOfCurrentType.get( eventsOfCurrentType.size() - 1 );
        assert lastEventOfCurrentType != null;

        if ( lastEventOfCurrentType.getEventType() != null && eventClass.isAssignableFrom( lastEventOfCurrentType.getEventType().getClass() ) ) {
            // then definitely don't run it. The last event was the same as the one we're trying to renew.
            log.debug( "Last event on " + arrayDesign + " was also a " + eventClass + ", skipping." );
            return false;
        }

        for ( AuditEvent currentEvent : allEvents ) {

            if ( currentEvent.getEventType() == null || currentEvent.getEventType().getClass().equals( eventClass ) ) {
                // ignore events of the same type.
                log.debug( "Ignoring " + currentEvent );
                continue;
            }

            Class<? extends AuditEventType> currentEventClass = currentEvent.getEventType().getClass();

            // we only care about ArrayDesignAnalysisEvent events.
            if ( !ArrayDesignAnalysisEvent.class.isAssignableFrom( currentEventClass ) ) {
                continue;
            }

            if ( currentEvent.getDate().after( lastEventOfCurrentType.getDate() ) ) {
                log
                        .info( arrayDesign + " needs update, last " + eventClass.getSimpleName() + " was before last "
                                + currentEvent.getEventType().getClass().getSimpleName() );
                return true;
            }
            log.debug( arrayDesign + " " + eventClass.getSimpleName() + " was after last " + currentEvent
                    .getEventType().getClass().getSimpleName() + " (OK)" );

        }
        log.debug( arrayDesign + " does not need an update" );
        return false;
    }

    /**
     * Load expression experiments based on a list of short names or IDs in a file.
     */
    private Set<ArrayDesign> readListFile( String fileName ) throws IOException {
        Set<ArrayDesign> ees = new HashSet<>();
        for ( String eeName : FileUtils.readListFileToStrings( fileName ) ) {
            ArrayDesign ee = arrayDesignService.findByShortName( eeName );
            if ( ee == null ) {

                try {
                    Long id = Long.parseLong( eeName );
                    ee = arrayDesignService.load( id );
                    if ( ee == null ) {
                        addErrorObject( null, "No ArrayDesign found with ID " + eeName );
                        continue;
                    }
                } catch ( NumberFormatException e ) {
                    addErrorObject( null, "No ArrayDesign found with ID " + eeName );
                    continue;
                }

            }
            ees.add( ee );
        }
        return ees;
    }
}
