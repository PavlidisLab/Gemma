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
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Aggregates functionality useful when writing CLIs that need to get an array design from the database and do something
 * with it.
 *
 * @author pavlidis
 */
public abstract class ArrayDesignSequenceManipulatingCli extends AbstractCLIContextCLI {

    ArrayDesignReportService arrayDesignReportService;
    ArrayDesignService arrayDesignService;
    Collection<ArrayDesign> arrayDesignsToProcess = new HashSet<>();

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option arrayDesignOption = OptionBuilder.hasArg().withArgName( "Array design" )
                .withDescription( "Array design name (or short name); or comma-delimited list" ).withLongOpt( "array" )
                .create( 'a' );

        this.addOption( arrayDesignOption );

        Option eeFileListOption = OptionBuilder.hasArg().withArgName( "Array Design list file" ).withDescription(
                "File with list of short names or IDs of designs (one per line; use instead of '-e')" )
                .withLongOpt( "adListFile" ).create( 'f' );
        this.addOption( eeFileListOption );

        this.addDateOption();

        this.addAutoOption();

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

    @Override
    protected void processOptions() {
        super.processOptions();

        arrayDesignReportService = this.getBean( ArrayDesignReportService.class );
        arrayDesignService = this.getBean( ArrayDesignService.class );

        if ( this.hasOption( 'a' ) ) {
            this.arraysFromCliList();
        } else if ( this.hasOption( 'f' ) ) {
            String experimentListFile = this.getOptionValue( 'f' );
            AbstractCLI.log.info( "Reading arrayDesigns list from " + experimentListFile );
            try {
                this.arrayDesignsToProcess = this.readListFile( experimentListFile );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }

        if ( this.hasOption( "mdate" ) ) {
            super.mDate = this.getOptionValue( "mdate" );
            if ( this.hasOption( AbstractCLI.AUTO_OPTION_NAME ) ) {
                throw new IllegalArgumentException( "Please only select one of 'mdate' OR 'auto'" );
            }
        }

        if ( this.hasOption( AbstractCLI.AUTO_OPTION_NAME ) ) {
            this.autoSeek = true;
            if ( this.hasOption( "mdate" ) ) {
                throw new IllegalArgumentException( "Please only select one of 'mdate' OR 'auto'" );
            }
        }

    }

    ArrayDesignReportService getArrayDesignReportService() {
        return arrayDesignReportService;
    }

    ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @return true if the sequences on the given array design would be equivalently treated by analyzing another array
     * design. In the case of subsumption, this only works if the array design has been either analyzed for
     * subsuming status. (the analysis is not done as part of this call).
     */
    boolean isSubsumedOrMerged( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getSubsumingArrayDesign() != null ) {
            AbstractCLI.log.info( arrayDesign + " is subsumed by " + arrayDesign.getSubsumingArrayDesign().getId() );
            return true;
        }

        if ( arrayDesign.getMergedInto() != null ) {
            AbstractCLI.log.info( arrayDesign + " is merged into " + arrayDesign.getMergedInto().getId() );
            return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    boolean shouldRun( Date skipIfLastRunLaterThan, ArrayDesign design, Class<? extends ArrayDesignAnalysisEvent> cls ) {
        if ( design.getTechnologyType().equals( TechnologyType.NONE ) ) {
            AbstractCLI.log.warn( design + " is not a microarray platform, it will not be run" );
            // not really an error, but nice to get notification.
            errorObjects.add( design + ": " + "Skipped because it is not a microarray platform." );
            return false;
        }

        if ( this.hasOption( "force" ) ) return true;

        if ( this.isSubsumedOrMerged( design ) ) {
            AbstractCLI.log.warn( design + " is subsumed or merged into another design, it will not be run; instead process the 'parent' platform" );

            // not really an error, but nice to get notification.
            errorObjects.add( design + ": " + "Skipped because it is subsumed by or merged into another design." );
            return false;
        }

        if ( !this.needToRun( skipIfLastRunLaterThan, design, cls) ) {
            if ( skipIfLastRunLaterThan != null ) {
                AbstractCLI.log.warn( design + " was last run more recently than " + skipIfLastRunLaterThan );
                errorObjects.add( design + ": " + "Skipped because it was last run after " + skipIfLastRunLaterThan );
            } else {
                AbstractCLI.log.warn( design + " seems to be up to date or is not ready to run" );
                errorObjects.add( design + " seems to be up to date or is not ready to run" );
            }
            return false;
        }
        return true;
    }

    /**
     * @param eventClass e.g., ArrayDesignSequenceAnalysisEvent.class
     * @return true if skipIfLastRunLaterThan is null, or there is no record of a previous analysis, or if the last
     * analysis was run before skipIfLastRunLaterThan. false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    boolean needToRun( Date skipIfLastRunLaterThan, ArrayDesign arrayDesign,
            Class<? extends ArrayDesignAnalysisEvent> eventClass ) {

        if ( autoSeek ) {
            return this.needToAutoRun( arrayDesign, eventClass );
        }

        if ( skipIfLastRunLaterThan == null )
            return true;

        List<AuditEvent> events = this.getEvents( arrayDesign, eventClass );
        if ( events.size() == 0 ) {
            return true; // always do it, it's never been done.
        }
        // return true if the last time was older than the limit time.
        AuditEvent lastEvent = events.get( events.size() - 1 );
        return lastEvent.getDate().before( skipIfLastRunLaterThan );

    }

    ArrayDesign thaw( ArrayDesign arrayDesign ) {
        return arrayDesignService.thaw( arrayDesign );
    }

    private void arraysFromCliList() {
        String arrayShortNames = this.getOptionValue( 'a' );
        String[] shortNames = arrayShortNames.split( "," );

        for ( String shortName : shortNames ) {
            if ( StringUtils.isBlank( shortName ) )
                continue;
            ArrayDesign ad = this.locateArrayDesign( shortName, arrayDesignService );
            if ( ad == null ) {
                AbstractCLI.log.warn( shortName + " not found" );
                continue;
            }
            arrayDesignsToProcess.add( ad );
        }
        if ( arrayDesignsToProcess.size() == 0 ) {
            AbstractCLI.log.error( "There were no valid experiments specified" );
            this.bail( ErrorCode.INVALID_OPTION );
        }
    }

    /**
     * @param eventClass if null, then all events are added.
     */
    private List<AuditEvent> getEvents( ArrayDesign arrayDesign,
            Class<? extends ArrayDesignAnalysisEvent> eventClass ) {
        List<AuditEvent> events = new ArrayList<>();

        for ( AuditEvent event : this.auditTrailService.getEvents( arrayDesign ) ) {
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
        if ( !autoSeek ) {
            return false;
        }

        List<AuditEvent> eventsOfCurrentType = this.getEvents( arrayDesign, eventClass );
        List<AuditEvent> allEvents = ( List<AuditEvent> ) arrayDesign.getAuditTrail().getEvents();

        if ( eventsOfCurrentType.size() == 0 ) {
            // it's never been run.
            return true;
        }

        AuditEvent lastEventOfCurrentType = eventsOfCurrentType.get( eventsOfCurrentType.size() - 1 );
        assert lastEventOfCurrentType != null;

        if ( lastEventOfCurrentType.getEventType().getClass().isAssignableFrom( eventClass ) ) {
            // then definitely don't run it. The last event was the same as the one we're trying to renew.
            AbstractCLI.log.debug( "Last event on " + arrayDesign + " was also a " + eventClass + ", skipping." );
            return false;
        }

        for ( AuditEvent currentEvent : allEvents ) {
            if ( currentEvent == null )
                continue;// legacy of ordered-list which could end up with gaps; should not be
            // needed any more

            if ( currentEvent.getEventType() == null || currentEvent.getEventType().getClass().equals( eventClass ) ) {
                continue;
            }

            Class<? extends AuditEventType> currentEventClass = currentEvent.getEventType().getClass();

            // we only care about ArrayDesignAnalysisEvent events.
            if ( !ArrayDesignAnalysisEvent.class.isAssignableFrom( currentEventClass ) ) {
                continue;
            }

            if ( currentEvent.getDate().after( lastEventOfCurrentType.getDate() ) ) {
                AbstractCLI.log
                        .info( arrayDesign + " needs update, last " + eventClass.getSimpleName() + " was before last "
                                + currentEvent.getEventType().getClass().getSimpleName() );
                return true;
            }
            AbstractCLI.log.debug( arrayDesign + " " + eventClass.getSimpleName() + " was after last " + currentEvent
                    .getEventType().getClass().getSimpleName() + " (OK)" );

        }
        AbstractCLI.log.debug( arrayDesign + " does not need an update" );
        return false;
    }

    /**
     * Load expression experiments based on a list of short names or IDs in a file.
     */
    private Set<ArrayDesign> readListFile( String fileName ) throws IOException {
        Set<ArrayDesign> ees = new HashSet<>();
        for ( String eeName : AbstractCLIContextCLI.readListFileToStrings( fileName ) ) {
            ArrayDesign ee = arrayDesignService.findByShortName( eeName );
            if ( ee == null ) {

                try {
                    Long id = Long.parseLong( eeName );
                    ee = arrayDesignService.load( id );
                    if ( ee == null ) {
                        AbstractCLI.log.error( "No ArrayDesign " + eeName + " found" );
                        continue;
                    }
                } catch ( NumberFormatException e ) {
                    AbstractCLI.log.error( "No ArrayDesign " + eeName + " found" );
                    continue;

                }

            }
            ees.add( ee );
        }
        return ees;
    }

    abstract class Consumer implements Runnable {
        private final SecurityContext context;
        private final BlockingQueue<ArrayDesign> queue;

        Consumer( BlockingQueue<ArrayDesign> q, SecurityContext context ) {
            this.queue = q;
            this.context = context;
        }

        @Override
        public void run() {
            SecurityContextHolder.setContext( this.context );
            while ( true ) {
                ArrayDesign ad = queue.poll();
                if ( ad == null ) {
                    break;
                }
                this.consume( ad );
            }
        }

        abstract void consume( ArrayDesign x );
    }

}
