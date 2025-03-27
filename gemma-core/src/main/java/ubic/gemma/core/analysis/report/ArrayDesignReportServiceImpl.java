/*
 * The Gemma project.
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

package ubic.gemma.core.analysis.report;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.*;
import java.util.*;

/**
 * @author jsantos
 */
@Component("arrayDesignReportService")
public class ArrayDesignReportServiceImpl implements ArrayDesignReportService {

    private final static Log log = LogFactory.getLog( ArrayDesignReportServiceImpl.class );

    private final static String ARRAY_DESIGN_REPORT_DIR = "ArrayDesignReports";
    private final static String ARRAY_DESIGN_REPORT_FILE_NAME_PREFIX = "ArrayDesignReport";
    // For all array designs
    private final static String ARRAY_DESIGN_SUMMARY = "AllArrayDesignsSummary";

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AuditEventService auditEventService;

    @Value("${gemma.appdata.home}")
    private String appdataHome;

    /**
     * Batch of classes we can get events for all at once.
     */
    private static final List<Class<? extends AuditEventType>> eventTypes = Arrays.asList(
            ArrayDesignSequenceUpdateEvent.class, ArrayDesignSequenceAnalysisEvent.class,
            ArrayDesignGeneMappingEvent.class, ArrayDesignRepeatAnalysisEvent.class );

    @Override
    public void generateAllArrayDesignReport() {
        ArrayDesignReportServiceImpl.log.info( "Generating report summarizing all platforms ... " );

        // obtain time information (for timestamp)
        Date d = new Date( System.currentTimeMillis() );
        String timestamp = DateFormatUtils.format( d, "yyyy.MM.dd HH:mm" );

        long numCsBioSequences = arrayDesignService.numAllCompositeSequenceWithBioSequences();
        long numCsBlatResults = arrayDesignService.numAllCompositeSequenceWithBlatResults();
        long numCsGenes = arrayDesignService.numAllCompositeSequenceWithGenes();
        long numGenes = arrayDesignService.numAllGenes();

        // create a surrogate ArrayDesignValue object to represent the total of all platforms
        ArrayDesignValueObject adVo = new ArrayDesignValueObject( -1L );
        adVo.setNumProbeSequences( Long.toString( numCsBioSequences ) );
        adVo.setNumProbeAlignments( Long.toString( numCsBlatResults ) );
        adVo.setNumProbesToGenes( Long.toString( numCsGenes ) );
        adVo.setNumGenes( Long.toString( numGenes ) );
        adVo.setDateCached( timestamp );

        // remove file first
        File f = new File( appdataHome + File.separatorChar
                + ArrayDesignReportServiceImpl.ARRAY_DESIGN_REPORT_DIR + File.separatorChar
                + ArrayDesignReportServiceImpl.ARRAY_DESIGN_SUMMARY );
        if ( f.exists() ) {
            if ( !f.canWrite() || !f.delete() ) {
                ArrayDesignReportServiceImpl.log.warn( "Cannot write to file." );
                return;
            }
        }
        try ( FileOutputStream fos = new FileOutputStream( appdataHome + File.separatorChar
                + ArrayDesignReportServiceImpl.ARRAY_DESIGN_REPORT_DIR + File.separatorChar
                + ArrayDesignReportServiceImpl.ARRAY_DESIGN_SUMMARY );
                ObjectOutputStream oos = new ObjectOutputStream( fos ) ) {
            oos.writeObject( adVo );
        } catch ( Throwable e ) {
            // cannot write to file. Just fail gracefully.
            ArrayDesignReportServiceImpl.log.error( "Cannot write to file." );
        }
        ArrayDesignReportServiceImpl.log.info( "Done making reports" );
    }

    @Override
    @Secured({ "GROUP_AGENT" })
    public void generateArrayDesignReport() {
        this.initDirectories();

        Collection<ArrayDesignValueObject> ads = arrayDesignService.loadAllValueObjects();
        ArrayDesignReportServiceImpl.log.info( "Creating reports for " + ads.size() + " platforms" );
        for ( ArrayDesignValueObject ad : ads ) {
            this.generateArrayDesignReport( ad );
        }

        ArrayDesignReportServiceImpl.log.info( "Generating global report" );
        this.generateAllArrayDesignReport();
    }

    @Override
    public void generateArrayDesignReport( ArrayDesignValueObject adVo ) {

        ArrayDesign ad = arrayDesignService.load( adVo.getId() );
        if ( ad == null )
            return;

        // obtain time information (for timestamp)
        Date d = new Date( System.currentTimeMillis() );
        String timestamp = DateFormatUtils.format( d, "yyyy.MM.dd HH:mm" );

        long numProbes = arrayDesignService.getCompositeSequenceCount( ad );
        long numCsBioSequences = arrayDesignService.numCompositeSequenceWithBioSequences( ad );
        long numCsBlatResults = arrayDesignService.numCompositeSequenceWithBlatResults( ad );
        long numCsGenes = arrayDesignService.numCompositeSequenceWithGenes( ad );
        long numGenes = arrayDesignService.numGenes( ad );

        adVo.setDesignElementCount( ( int ) numProbes );
        adVo.setNumProbeSequences( Long.toString( numCsBioSequences ) );
        adVo.setNumProbeAlignments( Long.toString( numCsBlatResults ) );
        adVo.setNumProbesToGenes( Long.toString( numCsGenes ) );
        adVo.setNumGenes( Long.toString( numGenes ) );
        adVo.setDateCached( timestamp );

        // check the directory exists.
        String reportDir = appdataHome + File.separatorChar
                + ArrayDesignReportServiceImpl.ARRAY_DESIGN_REPORT_DIR;
        File reportDirF = new File( reportDir );
        try {
            FileUtils.forceMkdir( reportDirF );
        } catch ( IOException e ) {
            log.error( "Failed to create report parent directory: " + reportDirF );
            return;
        }

        String reportFileName =
                reportDir + File.separatorChar + ArrayDesignReportServiceImpl.ARRAY_DESIGN_REPORT_FILE_NAME_PREFIX + "."
                        + adVo.getId();
        File f = new File( reportFileName );

        if ( f.exists() ) {
            if ( !f.canWrite() || !f.delete() ) {
                ArrayDesignReportServiceImpl.log
                        .warn( "Report exists but cannot overwrite, leaving the old one in place: " + reportFileName );
                return;
            }
        }

        try ( FileOutputStream fos = new FileOutputStream( reportFileName );
                ObjectOutputStream oos = new ObjectOutputStream( fos ) ) {
            oos.writeObject( adVo );
        } catch ( Throwable e ) {
            ArrayDesignReportServiceImpl.log.error( "Cannot write to file: " + reportFileName, e );
            return;
        }
        ArrayDesignReportServiceImpl.log.info( "Generated report for " + ad );
    }

    @Override
    public ArrayDesignValueObject generateArrayDesignReport( Long id ) {
        Collection<ArrayDesignValueObject> adVo = arrayDesignService
                .loadValueObjectsByIds( Collections.singleton( id ) );
        if ( adVo != null && !adVo.isEmpty() ) {
            this.generateArrayDesignReport( adVo.iterator().next() );
            return this.getSummaryObject( id );
        }
        ArrayDesignReportServiceImpl.log.warn( "No value objects return for requested platforms" );
        return null;
    }

    /**
     * Get a specific cached summary object
     *
     * @return arrayDesignValueObject the specified summary object
     */
    @Override
    public ArrayDesignValueObject getSummaryObject( Long id ) {
        ArrayDesignValueObject adVo = null;
        File f = new File(
                appdataHome + "/" + ArrayDesignReportServiceImpl.ARRAY_DESIGN_REPORT_DIR
                        + File.separatorChar + ArrayDesignReportServiceImpl.ARRAY_DESIGN_REPORT_FILE_NAME_PREFIX + "."
                        + id );
        if ( f.exists() ) {
            try ( FileInputStream fis = new FileInputStream( f ); ObjectInputStream ois = new ObjectInputStream( fis ) ) {

                adVo = ( ArrayDesignValueObject ) ois.readObject();

            } catch ( Throwable e ) {
                return null;
            }
        }
        return adVo;
    }

    /**
     * Get the cached summary object that represents all platforms.
     *
     * @return arrayDesignValueObject the summary object that represents the grand total of all array designs
     */
    @Override
    public ArrayDesignValueObject getSummaryObject() {
        ArrayDesignValueObject adVo = null;
        File f = new File( appdataHome + File.separatorChar
                + ArrayDesignReportServiceImpl.ARRAY_DESIGN_REPORT_DIR + File.separatorChar
                + ArrayDesignReportServiceImpl.ARRAY_DESIGN_SUMMARY );
        if ( f.exists() ) {
            try ( FileInputStream fis = new FileInputStream( f ); ObjectInputStream ois = new ObjectInputStream( fis ) ) {
                adVo = ( ArrayDesignValueObject ) ois.readObject();
            } catch ( Throwable e ) {
                return null;
            }
        }
        return adVo;
    }

    /**
     * Get the cached summary objects
     *
     * @return arrayDesignValueObjects the specified summary object
     */
    @Override
    public Collection<ArrayDesignValueObject> getSummaryObject( Collection<Long> ids ) {
        Collection<ArrayDesignValueObject> adVos = new ArrayList<>();
        for ( Long id : ids ) {
            ArrayDesignValueObject adVo = this.getSummaryObject( id );
            if ( adVo != null ) {
                adVos.add( this.getSummaryObject( id ) );
            }
        }
        return adVos;
    }

    /**
     * Fill in event information
     */
    @Override
    public void fillEventInformation( Collection<ArrayDesignValueObject> adVos ) {

        if ( adVos == null || adVos.isEmpty() )
            return;

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> ids = new ArrayList<>();
        for ( ArrayDesignValueObject adVo : adVos ) {
            Long id = adVo.getId();
            if ( id == null )
                continue;
            ids.add( id );
        }

        if ( ids.isEmpty() )
            return;

        Collection<ArrayDesign> arrayDesigns = arrayDesignService.load( ids );

        Map<Long, ArrayDesign> idMap = EntityUtils.getIdMap( arrayDesigns );

        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> events = auditEventService
                .getLastEvents( arrayDesigns, eventTypes );

        Map<Auditable, AuditEvent> geneMappingEvents = events.get( ArrayDesignGeneMappingEvent.class );
        Map<Auditable, AuditEvent> sequenceUpdateEvents = events.get( ArrayDesignSequenceUpdateEvent.class );
        Map<Auditable, AuditEvent> sequenceAnalysisEvents = events.get( ArrayDesignSequenceAnalysisEvent.class );
        Map<Auditable, AuditEvent> repeatAnalysisEvents = events.get( ArrayDesignRepeatAnalysisEvent.class );
        Map<Auditable, AuditEvent> creationEvents = auditEventService.getCreateEvents( arrayDesigns );

        for ( ArrayDesignValueObject adVo : adVos ) {

            Long id = adVo.getId();
            ArrayDesign ad = idMap.get( id );

            if ( geneMappingEvents.containsKey( ad ) ) {
                AuditEvent event = geneMappingEvents.get( ad );
                if ( event != null ) {
                    adVo.setLastGeneMapping( event.getDate() );
                }
            }

            if ( sequenceUpdateEvents.containsKey( ad ) ) {
                AuditEvent event = sequenceUpdateEvents.get( ad );
                if ( event != null ) {
                    adVo.setLastSequenceUpdate( event.getDate() );
                }
            }

            if ( sequenceAnalysisEvents.containsKey( ad ) ) {
                AuditEvent event = sequenceAnalysisEvents.get( ad );
                if ( event != null ) {
                    adVo.setLastSequenceAnalysis( event.getDate() );
                }
            }
            if ( repeatAnalysisEvents.containsKey( ad ) ) {
                AuditEvent event = repeatAnalysisEvents.get( ad );
                if ( event != null ) {
                    adVo.setLastRepeatMask( event.getDate() );
                }
            }

            if ( creationEvents.containsKey( ad ) ) {
                AuditEvent event = creationEvents.get( ad );
                if ( event != null ) {
                    adVo.setCreateDate( event.getDate() );
                }
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            ArrayDesignReportServiceImpl.log.info( "Added event information in " + watch.getTime() + "ms" );

    }

    @Override
    public void fillInSubsumptionInfo( Collection<ArrayDesignValueObject> valueObjects ) {
        Collection<Long> ids = new ArrayList<>();
        for ( ArrayDesignValueObject adVo : valueObjects ) {
            if ( adVo == null )
                continue;
            ids.add( adVo.getId() );
        }
        Map<Long, Boolean> isSubsumed = arrayDesignService.isSubsumed( ids );
        Map<Long, Boolean> hasSubsumees = arrayDesignService.isSubsumer( ids );
        Map<Long, Boolean> isMergee = arrayDesignService.isMergee( ids );
        Map<Long, Boolean> isMerged = arrayDesignService.isMerged( ids );

        for ( ArrayDesignValueObject adVo : valueObjects ) {
            Long id = adVo.getId();
            if ( isSubsumed.containsKey( id ) ) {
                adVo.setIsSubsumed( isSubsumed.get( id ) );
            }
            if ( hasSubsumees.containsKey( id ) ) {
                adVo.setIsSubsumer( hasSubsumees.get( id ) );
            }
            if ( isMergee.containsKey( id ) ) {
                adVo.setIsMergee( isMergee.get( id ) );
            }
            if ( isMerged.containsKey( id ) ) {
                adVo.setIsMerged( isMerged.get( id ) );
            }
        }

    }

    /**
     * Fill in the probe summary statistics
     */
    @Override
    public void fillInValueObjects( Collection<ArrayDesignValueObject> adVos ) {
        for ( ArrayDesignValueObject origVo : adVos ) {
            if ( origVo == null )
                continue;
            ArrayDesignValueObject cachedVo = this.getSummaryObject( origVo.getId() );
            if ( cachedVo != null ) {
                origVo.setNumProbeSequences( cachedVo.getNumProbeSequences() );
                origVo.setNumProbeAlignments( cachedVo.getNumProbeAlignments() );
                origVo.setNumProbesToGenes( cachedVo.getNumProbesToGenes() );
                origVo.setNumGenes( cachedVo.getNumGenes() );
                origVo.setDateCached( cachedVo.getDateCached() );
                origVo.setDesignElementCount( cachedVo.getDesignElementCount() );
            }
        }
    }

    @Override
    public String getLastSequenceUpdateEvent( Long id ) {
        return this.getLastEvent( id, ArrayDesignSequenceUpdateEvent.class );
    }

    @Override
    public String getLastSequenceAnalysisEvent( Long id ) {
        return this.getLastEvent( id, ArrayDesignSequenceAnalysisEvent.class );
    }

    @Override
    public String getLastRepeatMaskEvent( Long id ) {
        return this.getLastEvent( id, ArrayDesignRepeatAnalysisEvent.class );
    }

    @Override
    public String getLastGeneMappingEvent( Long id ) {
        return this.getLastEvent( id, ArrayDesignGeneMappingEvent.class );
    }

    @Override
    public String getCreateDate( Long id ) {
        ArrayDesign ad = arrayDesignService.load( id );

        if ( ad == null )
            return "";

        List<AuditEvent> events = auditEventService.getEvents( ad );
        AuditEvent lastEvent = events.get( 0 );
        return DateFormatUtils.format( lastEvent.getDate(), "yyyy.MMM.dd hh:mm aa" );
    }

    private String getLastEvent( Long id, Class<? extends AuditEventType> eventType ) {
        ArrayDesign ad = arrayDesignService.load( id );

        if ( ad == null )
            return "";

        List<AuditEvent> events2 = auditEventService.getEvents( ad );

        String analysisEventString;
        List<AuditEvent> events = new ArrayList<>();

        for ( AuditEvent event : events2 ) {
            if ( event == null )
                continue; // legacy of ordered-list which could end up with gaps; should not be needed
            // any more
            if ( event.getEventType() != null && eventType.isAssignableFrom( event.getEventType().getClass() ) ) {
                events.add( event );
            }
        }

        if ( events.isEmpty() ) {
            return "[None]";
        }

        // add the most recent events to the report. There should always be at least one creation event.
        AuditEvent lastEvent = events.get( events.size() - 1 );
        analysisEventString = DateFormatUtils.format( lastEvent.getDate(), "yyyy.MMM.dd hh:mm aa" );

        return analysisEventString;
    }

    private void initDirectories() {
        FileTools.createDir( appdataHome );
        FileTools.createDir( appdataHome + File.separator
                + ArrayDesignReportServiceImpl.ARRAY_DESIGN_REPORT_DIR );
    }

}