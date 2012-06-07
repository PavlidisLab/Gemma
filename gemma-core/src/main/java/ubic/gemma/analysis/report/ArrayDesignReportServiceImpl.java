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

package ubic.gemma.analysis.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.EntityUtils;

/**
 * @author jsantos
 * @version $Id$
 */
@Component
public class ArrayDesignReportServiceImpl implements ArrayDesignReportService {
    private String ARRAY_DESIGN_REPORT_DIR = "ArrayDesignReports";

    private String ARRAY_DESIGN_REPORT_FILE_NAME_PREFIX = "ArrayDesignReport";

    // For all array designs
    private String ARRAY_DESIGN_SUMMARY = "AllArrayDesignsSummary";

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AuditEventService auditEventService;

    /**
     * Batch of classes we can get events for all at once.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends AuditEventType>[] eventTypes = new Class[] { ArrayDesignSequenceUpdateEvent.class,
            ArrayDesignSequenceAnalysisEvent.class, ArrayDesignGeneMappingEvent.class,
            ArrayDesignRepeatAnalysisEvent.class };
    /*
     * ArrayDesignProbeRenamingEvent.class,ArrayDesignMergeEvent.class, ArrayDesignAnnotationFileEvent.class,
     * ArrayDesignSubsumeCheckEvent.class
     */

    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );

    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * Fill in event information
     * 
     * @param adVos
     */
    @Override
    public void fillEventInformation( Collection<ArrayDesignValueObject> adVos ) {

        if ( adVos == null || adVos.size() == 0 ) return;

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> ids = new ArrayList<Long>();
        for ( Object object : adVos ) {
            ArrayDesignValueObject adVo = ( ArrayDesignValueObject ) object;
            Long id = adVo.getId();
            if ( id == null ) continue;
            ids.add( id );
        }

        if ( ids.size() == 0 ) return;

        Collection<Class<? extends AuditEventType>> typesToGet = Arrays.asList( eventTypes );

        Collection<ArrayDesign> arrayDesigns = arrayDesignService.loadMultiple( ids );

        Map<Long, ArrayDesign> idMap = EntityUtils.getIdMap( arrayDesigns );

        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> events = auditEventService.getLastEvents(
                arrayDesigns, typesToGet );

        Map<Auditable, AuditEvent> geneMappingEvents = events.get( ArrayDesignGeneMappingEvent.class );
        Map<Auditable, AuditEvent> sequenceUpdateEvents = events.get( ArrayDesignSequenceUpdateEvent.class );
        Map<Auditable, AuditEvent> sequenceAnalysisEvents = events.get( ArrayDesignSequenceAnalysisEvent.class );
        Map<Auditable, AuditEvent> repeatAnalysisEvents = events.get( ArrayDesignRepeatAnalysisEvent.class );

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

        }

        watch.stop();
        if ( watch.getTime() > 1000 ) log.info( "Added event information in " + watch.getTime() + "ms" );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ArrayDesignReportService#fillInSubsumptionInfo(java.util.Collection)
     */
    @Override
    public void fillInSubsumptionInfo( Collection<ArrayDesignValueObject> valueObjects ) {
        Collection<Long> ids = new ArrayList<Long>();
        for ( Object object : valueObjects ) {
            ArrayDesignValueObject adVo = ( ArrayDesignValueObject ) object;
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
     * 
     * @param adVos
     */
    @Override
    public void fillInValueObjects( Collection<ArrayDesignValueObject> adVos ) {
        for ( ArrayDesignValueObject origVo : adVos ) {
            ArrayDesignValueObject cachedVo = getSummaryObject( origVo.getId() );
            if ( cachedVo != null ) {
                origVo.setNumProbeSequences( cachedVo.getNumProbeSequences() );
                origVo.setNumProbeAlignments( cachedVo.getNumProbeAlignments() );
                origVo.setNumProbesToGenes( cachedVo.getNumProbesToGenes() );
                origVo.setNumProbesToKnownGenes( cachedVo.getNumProbesToKnownGenes() );
                origVo.setNumProbesToPredictedGenes( cachedVo.getNumProbesToPredictedGenes() );
                origVo.setNumProbesToProbeAlignedRegions( cachedVo.getNumProbesToProbeAlignedRegions() );
                origVo.setNumGenes( cachedVo.getNumGenes() );
                origVo.setDateCached( cachedVo.getDateCached() );
                origVo.setDesignElementCount( cachedVo.getDesignElementCount() );
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ArrayDesignReportService#generateAllArrayDesignReport()
     */
    @Override
    public void generateAllArrayDesignReport() {
        log.info( "Generating report summarizing all platforms ... " );

        // obtain time information (for timestamping)
        Date d = new Date( System.currentTimeMillis() );
        String timestamp = DateFormatUtils.format( d, "yyyy.MM.dd HH:mm" );

        long numCsBioSequences = arrayDesignService.numAllCompositeSequenceWithBioSequences();
        long numCsBlatResults = arrayDesignService.numAllCompositeSequenceWithBlatResults();
        long numCsGenes = arrayDesignService.numAllCompositeSequenceWithGenes();
        long numGenes = arrayDesignService.numAllGenes();

        // create a surrogate ArrayDesignValue object to represent the total of all platforms
        ArrayDesignValueObject adVo = new ArrayDesignValueObject();
        adVo.setNumProbeSequences( Long.toString( numCsBioSequences ) );
        adVo.setNumProbeAlignments( Long.toString( numCsBlatResults ) );
        adVo.setNumProbesToGenes( Long.toString( numCsGenes ) );
        adVo.setNumGenes( Long.toString( numGenes ) );
        adVo.setDateCached( timestamp );

        try {
            // remove file first
            File f = new File( HOME_DIR + File.separatorChar + ARRAY_DESIGN_REPORT_DIR + File.separatorChar
                    + ARRAY_DESIGN_SUMMARY );
            if ( f.exists() ) {
                if ( !f.canWrite() || !f.delete() ) {
                    log.warn( "Cannot write to file." );
                    return;
                }
            }
            FileOutputStream fos = new FileOutputStream( HOME_DIR + File.separatorChar + ARRAY_DESIGN_REPORT_DIR
                    + File.separatorChar + ARRAY_DESIGN_SUMMARY );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( adVo );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            // cannot write to file. Just fail gracefully.
            log.error( "Cannot write to file." );
        }
        log.info( "Done making reports" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ArrayDesignReportService#generateArrayDesignReport()
     */
    @Override
    @Secured({ "GROUP_AGENT" })
    public void generateArrayDesignReport() {
        initDirectories();
        log.info( "Generating global report" );
        generateAllArrayDesignReport();
        Collection<ArrayDesignValueObject> ads = arrayDesignService.loadAllValueObjects();
        log.info( "Creating reports for " + ads.size() + " platforms" );
        for ( ArrayDesignValueObject ad : ads ) {
            generateArrayDesignReport( ad );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.report.ArrayDesignReportService#generateArrayDesignReport(ubic.gemma.model.expression.arrayDesign
     * .ArrayDesignValueObject)
     */
    @Override
    public void generateArrayDesignReport( ArrayDesignValueObject adVo ) {

        ArrayDesign ad = arrayDesignService.load( adVo.getId() );
        if ( ad == null ) return;

        // obtain time information (for timestamping)
        Date d = new Date( System.currentTimeMillis() );
        String timestamp = DateFormatUtils.format( d, "yyyy.MM.dd HH:mm" );

        long numProbes = arrayDesignService.getCompositeSequenceCount( ad );
        long numCsBioSequences = arrayDesignService.numCompositeSequenceWithBioSequences( ad );
        long numCsBlatResults = arrayDesignService.numCompositeSequenceWithBlatResults( ad );
        long numCsGenes = arrayDesignService.numCompositeSequenceWithGenes( ad );
        long numGenes = arrayDesignService.numGenes( ad );

        adVo.setDesignElementCount( numProbes );
        adVo.setNumProbeSequences( Long.toString( numCsBioSequences ) );
        adVo.setNumProbeAlignments( Long.toString( numCsBlatResults ) );
        adVo.setNumProbesToGenes( Long.toString( numCsGenes ) );
        adVo.setNumGenes( Long.toString( numGenes ) );
        adVo.setDateCached( timestamp );

        // check the directory exists.
        String reportDir = HOME_DIR + File.separatorChar + ARRAY_DESIGN_REPORT_DIR;
        File reportDirF = new File( reportDir );
        if ( !reportDirF.exists() ) {
            reportDirF.mkdirs();
        }

        String reportFileName = reportDir + File.separatorChar + ARRAY_DESIGN_REPORT_FILE_NAME_PREFIX + "."
                + adVo.getId();

        try {
            // remove old file first (possible todo: don't do this until after new file is okayed - maybe this delete
            // isn't needed, just clobber.)

            File f = new File( reportFileName );

            if ( f.exists() ) {
                if ( !f.canWrite() || !f.delete() ) {
                    log.error( "Report exists but cannot overwrite, leaving the old one in place: " + reportFileName );
                    return;
                }
            }
            FileOutputStream fos = new FileOutputStream( reportFileName );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( adVo );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            log.error( "Cannot write to file: " + reportFileName );
            return;
        }
        log.info( "Generated report for " + ad );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ArrayDesignReportService#generateArrayDesignReport(java.lang.Long)
     */
    @Override
    public ArrayDesignValueObject generateArrayDesignReport( Long id ) {
        Collection<Long> ids = new ArrayList<Long>();
        ids.add( id );
        Collection<ArrayDesignValueObject> adVo = arrayDesignService.loadValueObjects( ids );
        if ( adVo != null && adVo.size() > 0 ) {
            generateArrayDesignReport( adVo.iterator().next() );
            return getSummaryObject( id );
        }
        log.warn( "No value objects return for requested platforms" );
        return null;

    }

    /**
     * @param id
     * @return
     */
    @Override
    public String getLastGeneMappingEvent( Long id ) {
        return getLastEvent( id, ArrayDesignGeneMappingEvent.class );

    }

    /**
     * @param id
     * @return
     */
    @Override
    public String getLastRepeatMaskEvent( Long id ) {
        return getLastEvent( id, ArrayDesignRepeatAnalysisEvent.class );
    }

    /**
     * @param id
     * @return
     */
    @Override
    public String getLastSequenceAnalysisEvent( Long id ) {
        return getLastEvent( id, ArrayDesignSequenceAnalysisEvent.class );
    }

    /**
     * @param id
     * @return
     */
    @Override
    public String getLastSequenceUpdateEvent( Long id ) {
        return getLastEvent( id, ArrayDesignSequenceUpdateEvent.class );
    }

    /**
     * Get the cached summary object that represents all platforms.
     * 
     * @return arrayDesignValueObject the summary object that represents the grand total of all array designs
     */
    @Override
    public ArrayDesignValueObject getSummaryObject() {
        ArrayDesignValueObject adVo = null;
        try {
            File f = new File( HOME_DIR + File.separatorChar + ARRAY_DESIGN_REPORT_DIR + File.separatorChar
                    + ARRAY_DESIGN_SUMMARY );
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( f );
                ObjectInputStream ois = new ObjectInputStream( fis );
                adVo = ( ArrayDesignValueObject ) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch ( Throwable e ) {
            return null;
        }
        return adVo;
    }

    /**
     * Get the cached summary objects
     * 
     * @param id
     * @return arrayDesignValueObjects the specified summary object
     */
    public Collection<ArrayDesignValueObject> getSummaryObject( Collection<Long> ids ) {
        Collection<ArrayDesignValueObject> adVos = new ArrayList<ArrayDesignValueObject>();
        for ( Long id : ids ) {
            ArrayDesignValueObject adVo = getSummaryObject( id );
            if ( adVo != null ) {
                adVos.add( getSummaryObject( id ) );
            }
        }

        return adVos;
    }

    /**
     * Get a specific cached summary object
     * 
     * @param id
     * @return arrayDesignValueObject the specified summary object
     */
    @Override
    public ArrayDesignValueObject getSummaryObject( Long id ) {
        ArrayDesignValueObject adVo = null;
        try {
            File f = new File( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + File.separatorChar
                    + ARRAY_DESIGN_REPORT_FILE_NAME_PREFIX + "." + id );
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( f );
                ObjectInputStream ois = new ObjectInputStream( fis );
                adVo = ( ArrayDesignValueObject ) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch ( Throwable e ) {
            return null;
        }
        return adVo;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * FIXME this could be refactored and used elsewhere. This is similar to code in the AuditableService/Dao.
     * 
     * @param id
     * @param eventType
     * @return
     */
    private String getLastEvent( Long id, Class<? extends AuditEventType> eventType ) {
        ArrayDesign ad = arrayDesignService.load( id );

        if ( ad == null ) return "";

        List<AuditEvent> events2 = auditEventService.getEvents( ad );

        String analysisEventString = "";
        List<AuditEvent> events = new ArrayList<AuditEvent>();

        for ( AuditEvent event : events2 ) {
            if ( event == null ) continue; // legacy of ordered-list which could end up with gaps; should not be needed
                                           // any more
            if ( event.getEventType() != null && eventType.isAssignableFrom( event.getEventType().getClass() ) ) {
                events.add( event );
            }
        }

        if ( events.size() == 0 ) {
            return "[None]";
        }

        // add the most recent events to the report. fixme check there are events.
        AuditEvent lastEvent = events.get( events.size() - 1 );
        analysisEventString = DateFormatUtils.format( lastEvent.getDate(), "yyyy.MMM.dd hh:mm aa" );

        return analysisEventString;
    }

    private void initDirectories() {
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separator + ARRAY_DESIGN_REPORT_DIR );
    }

}