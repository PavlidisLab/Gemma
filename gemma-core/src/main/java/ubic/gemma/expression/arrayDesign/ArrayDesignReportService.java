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

package ubic.gemma.expression.arrayDesign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.util.ConfigUtils;

/**
 * @author jsantos
 * @spring.bean name="arrayDesignReportService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 * @version $Id$
 */
public class ArrayDesignReportService {
    private Log log = LogFactory.getLog( this.getClass() );

    private String ARRAY_DESIGN_SUMMARY = "AllArrayDesignsSummary";
    private String ARRAY_DESIGN_REPORT_DIR = "ArrayDesignReports";
    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private ArrayDesignService arrayDesignService;
    private AuditTrailService auditTrailService;

    /**
     * @return the arrayDesignService
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    @SuppressWarnings("unchecked")
    public void generateArrayDesignReport() {
        initDirectories();
        generateAllArrayDesignReport();
        Collection<ArrayDesignValueObject> ads = arrayDesignService.loadAllValueObjects();
        for ( ArrayDesignValueObject ad : ads ) {
            generateArrayDesignReport( ad );
        }
    }

    @SuppressWarnings("unchecked")
    public void generateArrayDesignReport( Long id ) {
        Collection<Long> ids = new ArrayList<Long>();
        ids.add( id );
        Collection<ArrayDesignValueObject> adVo = arrayDesignService.loadValueObjects( ids );
        if ( adVo != null && adVo.size() > 0 ) {
            generateArrayDesignReport( adVo.iterator().next() );
        }
    }

    public void generateArrayDesignReport( ArrayDesignValueObject adVo ) {

        ArrayDesign ad = arrayDesignService.load( adVo.getId() );
        if ( ad == null ) return;

        log.info( "Generating report for array design " + ad.getId() + "\n" );

        // obtain time information (for timestamping)
        Date d = new Date( System.currentTimeMillis() );
        String timestamp = DateFormatUtils.format( d, "yyyy.MM.dd HH:mm" );

        long numCsBioSequences = arrayDesignService.numCompositeSequenceWithBioSequences( ad );
        long numCsBlatResults = arrayDesignService.numCompositeSequenceWithBlatResults( ad );
        long numCsGenes = arrayDesignService.numCompositeSequenceWithGenes( ad );
        long numCsPredictedGenes = arrayDesignService.numCompositeSequenceWithPredictedGenes( ad );
        long numCsProbeAlignedRegions = arrayDesignService.numCompositeSequenceWithProbeAlignedRegion( ad );
        long numCsPureGenes = numCsGenes - numCsPredictedGenes - numCsProbeAlignedRegions;
        long numGenes = arrayDesignService.numGenes( ad );

        adVo.setNumProbeSequences( Long.toString( numCsBioSequences ) );
        adVo.setNumProbeAlignments( Long.toString( numCsBlatResults ) );
        adVo.setNumProbesToGenes( Long.toString( numCsGenes ) );
        adVo.setNumProbesToKnownGenes( Long.toString( numCsPureGenes ) );
        adVo.setNumProbesToPredictedGenes( Long.toString( numCsPredictedGenes ) );
        adVo.setNumProbesToProbeAlignedRegions( Long.toString( numCsProbeAlignedRegions ) );
        adVo.setNumGenes( Long.toString( numGenes ) );
        adVo.setDateCached( timestamp );

        // String report = this.generateReportString( numCsBioSequences, numCsBlatResults, numCsGenes, numGenes,
        // numCsPredictedGenes, numCsProbeAlignedRegions, numCsPureGenes );

        // write into file
        /*
         * File f = new File( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY + "." + id );
         * f.delete(); try { f.createNewFile(); Writer writer = new FileWriter( f ); writer.write( report );
         * writer.flush(); writer.close(); } catch ( IOException e ) { // cannot write to file. Just fail gracefully.
         * log.error( "Cannot write to file." ); }
         */

        try {
            // remove file first
            File f = new File( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY + "."
                    + adVo.getId() );
            if ( f.exists() ) {
                f.delete();
            }
            FileOutputStream fos = new FileOutputStream( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/"
                    + ARRAY_DESIGN_SUMMARY + "." + adVo.getId() );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( adVo );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            return;
        }
    }

    public void generateAllArrayDesignReport() {
        log.info( "Generating report for all array designs\n" );

        // obtain time information (for timestamping)
        Date d = new Date( System.currentTimeMillis() );
        String timestamp = DateFormatUtils.format( d, "yyyy.MM.dd HH:mm" );

        long numCsBioSequences = arrayDesignService.numAllCompositeSequenceWithBioSequences();
        long numCsBlatResults = arrayDesignService.numAllCompositeSequenceWithBlatResults();
        long numCsGenes = arrayDesignService.numAllCompositeSequenceWithGenes();
        long numGenes = arrayDesignService.numAllGenes();

        // create a surrogate ArrayDesignValue object to represent the total of all array designs
        ArrayDesignValueObject adVo = new ArrayDesignValueObject();
        adVo.setNumProbeSequences( Long.toString( numCsBioSequences ) );
        adVo.setNumProbeAlignments( Long.toString( numCsBlatResults ) );
        adVo.setNumProbesToGenes( Long.toString( numCsGenes ) );
        adVo.setNumGenes( Long.toString( numGenes ) );
        adVo.setDateCached( timestamp );

        try {
            // remove file first
            File f = new File( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY );
            if ( f.exists() ) {
                f.delete();
            }
            FileOutputStream fos = new FileOutputStream( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/"
                    + ARRAY_DESIGN_SUMMARY );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( adVo );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            // cannot write to file. Just fail gracefully.
            log.error( "Cannot write to file." );
        }
    }

    /**
     * @param id
     * @return
     */
    public String getLastSequenceUpdateEvent( Long id ) {

        ArrayDesign ad = arrayDesignService.load( id );

        if ( ad == null ) return "";

        auditTrailService.thaw( ad );

        String analysisEventString = "";

        List<AuditEvent> sequenceUpdateEvents = new ArrayList<AuditEvent>();

        for ( AuditEvent event : ad.getAuditTrail().getEvents() ) {
            if ( event == null ) continue;
            if ( event.getEventType() != null && event.getEventType() instanceof ArrayDesignSequenceUpdateEvent ) {
                sequenceUpdateEvents.add( event );
            }
        }

        if ( sequenceUpdateEvents.size() == 0 ) {
            return "[None]";
        } else {
            AuditEvent lastSequenceUpdate = sequenceUpdateEvents.get( sequenceUpdateEvents.size() - 1 );
            analysisEventString = DateFormatUtils.format( lastSequenceUpdate.getDate(), "yyyy.MMM.dd hh:mm aa" );
        }

        return analysisEventString;
    }

    /**
     * @param id
     * @return
     */
    public String getLastSequenceAnalysisEvent( Long id ) {

        ArrayDesign ad = arrayDesignService.load( id );

        if ( ad == null ) return "";

        auditTrailService.thaw( ad );

        String analysisEventString = "";

        List<AuditEvent> sequenceAnalysisEvents = new ArrayList<AuditEvent>();

        for ( AuditEvent event : ad.getAuditTrail().getEvents() ) {
            if ( event == null ) continue;
            if ( event.getEventType() != null && event.getEventType() instanceof ArrayDesignSequenceAnalysisEvent ) {
                sequenceAnalysisEvents.add( event );
            }
        }

        if ( sequenceAnalysisEvents.size() == 0 ) {
            return "[None]";
        } else {
            // add the most recent events to the report. fixme check there are events.
            AuditEvent lastEvent = sequenceAnalysisEvents.get( sequenceAnalysisEvents.size() - 1 );
            analysisEventString = DateFormatUtils.format( lastEvent.getDate(), "yyyy.MMM.dd hh:mm aa" );
        }

        return analysisEventString;
    }

    /**
     * @param id
     * @return
     */
    public String getLastGeneMappingEvent( Long id ) {

        ArrayDesign ad = arrayDesignService.load( id );

        if ( ad == null ) return "";

        auditTrailService.thaw( ad );

        String analysisEventString = "";
        List<AuditEvent> geneMappingEvents = new ArrayList<AuditEvent>();

        for ( AuditEvent event : ad.getAuditTrail().getEvents() ) {
            if ( event == null ) continue;
            if ( event.getEventType() != null && event.getEventType() instanceof ArrayDesignGeneMappingEvent ) {
                geneMappingEvents.add( event );
            }
        }

        if ( geneMappingEvents.size() == 0 ) {
            return "[None]";
        } else {

            // add the most recent events to the report. fixme check there are events.
            AuditEvent lastEvent = geneMappingEvents.get( geneMappingEvents.size() - 1 );
            analysisEventString = DateFormatUtils.format( lastEvent.getDate(), "yyyy.MMM.dd hh:mm aa" );
        }

        return analysisEventString;
    }

    /**
     * Get a specific cached summary object
     * 
     * @param id
     * @return arrayDesignValueObject the specified summary object
     */
    public ArrayDesignValueObject getSummaryObject( Long id ) {
        ArrayDesignValueObject adVo = null;
        try {
            File f = new File( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY + "." + id );
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
     * Get the cached summary object that represents all array designs.
     * 
     * @return arrayDesignValueObject the summary object that represents the grand total of all array designs
     */
    public ArrayDesignValueObject getSummaryObject() {
        ArrayDesignValueObject adVo = null;
        try {
            File f = new File( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR + "/" + ARRAY_DESIGN_SUMMARY );
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
     * Fill in the probe summary statistics
     * 
     * @param adVos
     */
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
            }
        }
    }

    /**
     * Fill in event information
     * 
     * @param adVos
     */
    @SuppressWarnings("unchecked")
    public void fillEventInformation( Collection<ArrayDesignValueObject> adVos ) {
        Collection<Long> ids = new ArrayList<Long>();
        for ( Object object : adVos ) {
            ArrayDesignValueObject adVo = ( ArrayDesignValueObject ) object;
            ids.add( adVo.getId() );
        }

        Map<Long, AuditEvent> geneMappingEvents = arrayDesignService.getLastGeneMapping( ids );
        Map<Long, AuditEvent> sequenceUpdateEvents = arrayDesignService.getLastSequenceUpdate( ids );
        Map<Long, AuditEvent> sequenceAnalysisEvents = arrayDesignService.getLastSequenceAnalysis( ids );

        // fill in events for the value objects
        for ( ArrayDesignValueObject adVo : adVos ) {
            // preemptively fill in event dates with None

            Long id = adVo.getId();
            if ( geneMappingEvents.containsKey( id ) ) {
                AuditEvent event = geneMappingEvents.get( id );
                if ( event != null ) {
                    adVo.setLastGeneMapping( event.getDate() );
                }
            }

            if ( sequenceUpdateEvents.containsKey( id ) ) {
                AuditEvent event = sequenceUpdateEvents.get( id );
                if ( event != null ) {
                    adVo.setLastSequenceUpdate( event.getDate() );
                }
            }

            if ( sequenceAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = sequenceAnalysisEvents.get( id );
                if ( event != null ) {
                    adVo.setLastSequenceAnalysis( event.getDate() );
                }
            }
        }
    }

    private void initDirectories() {
        // check to see if the home directory exists. If it doesn't, create it.
        // check to see if the reports directory exists. If it doesn't, create it.
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR );
        File f = new File( HOME_DIR + "/" + ARRAY_DESIGN_REPORT_DIR );
        Collection<File> files = new ArrayList<File>();
        File[] fileArray = f.listFiles();
        for ( File file : fileArray ) {
            files.add( file );
        }
        // clear out all files
        FileTools.deleteFiles( files );
    }

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

}