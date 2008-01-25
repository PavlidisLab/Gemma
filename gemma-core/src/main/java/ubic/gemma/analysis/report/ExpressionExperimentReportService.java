/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.basecode.util.FileTools;
import ubic.gemma.grid.javaspaces.SpacesCommand;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTask;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.RankComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author jsantos
 * @spring.bean name="expressionExperimentReportService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 * @spring.property name="probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 */
public class ExpressionExperimentReportService implements ExpressionExperimentReportTask, InitializingBean {
    private Log log = LogFactory.getLog( this.getClass() );

    private String EE_LINK_SUMMARY = "AllExpressionLinkSummary";
    private String EE_REPORT_DIR = "ExpressionExperimentReports";
    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private ExpressionExperimentService expressionExperimentService;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    private AuditTrailService auditTrailService;
    private String taskId = null;

    /**
     * @param auditTrailService the auditTrailService to set
     */
    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * @return the probe2ProbeCoexpressionService
     */
    public Probe2ProbeCoexpressionService getProbe2ProbeCoexpressionService() {
        return probe2ProbeCoexpressionService;
    }

    /**
     * @param probe2ProbeCoexpressionService the probe2ProbeCoexpressionService to set
     */
    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and
     * datavectors
     */
    public void generateSummaryObjects() {
        initDirectories( false );
        // first, load all expression experiment value objects
        // this will have no stats filled in

        // for each expression experiment, load in stats
        Collection vos = expressionExperimentService.loadAllValueObjects();
        getStats( vos );

        // save the collection
        saveValueObjects( vos );
    }

    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and
     * datavectors
     */
    public void generateSummaryObjects( Collection ids ) {
        initDirectories( false );
        // first, load all expression experiment value objects
        // this will have no stats filled in

        // for each expression experiment, load in stats
        Collection vos = expressionExperimentService.loadValueObjects( ids );
        getStats( vos );

        // save the collection

        saveValueObjects( vos );
    }

    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and
     * datavectors
     */
    @SuppressWarnings("unchecked")
    public void generateSummaryObject( Long id ) {
        Collection ids = new ArrayList<Long>();
        ids.add( id );

        generateSummaryObjects( ids );
    }

    /**
     * @param vos
     */
    private void getStats( Collection vos ) {
        log.info( "Getting stats for " + vos.size() + " value objects." );
        String timestamp = DateFormatUtils.format( new Date( System.currentTimeMillis() ), "yyyy.MM.dd HH:mm" );
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;
            ExpressionExperiment tempEe = expressionExperimentService.load( eeVo.getId() );

            eeVo.setBioMaterialCount( expressionExperimentService.getBioMaterialCount( tempEe ) );
            eeVo.setPreferredDesignElementDataVectorCount( expressionExperimentService
                    .getPreferredDesignElementDataVectorCount( tempEe ) );

            eeVo.setCoexpressionLinkCount( probe2ProbeCoexpressionService.countLinks( tempEe ).longValue() );

            eeVo.setDateCached( timestamp );

            auditTrailService.thaw( tempEe.getAuditTrail() );
            if ( tempEe.getAuditTrail() != null ) {
                eeVo.setDateCreated( tempEe.getAuditTrail().getCreationEvent().getDate().toString() );
            }
            eeVo.setDateLastUpdated( tempEe.getAuditTrail().getLast().getDate() );
            log.info( "Generated report for " + eeVo.getShortName() );

        }
        log.info( "Stats completed." );
    }

    /**
     * retrieves a collection of cached value objects containing summary information
     * 
     * @return a collection of cached value objects
     */
    public Collection retrieveSummaryObjects() {
        return retrieveValueObjects();
    }

    /**
     * retrieves a collection of cached value objects containing summary information
     * 
     * @return a collection of cached value objects
     */
    public Collection retrieveSummaryObjects( Collection ids ) {
        return retrieveValueObjects( ids );
    }

    /**
     * @param eeValueObjects the collection of Expression Experiment value objects to serialize
     * @return true if successful, false otherwise serialize value objects
     */
    private boolean saveValueObjects( Collection eeValueObjects ) {
        for ( Object object : eeValueObjects ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;

            try {
                // remove file first
                File f = new File( getReportPath( eeVo.getId() ) );
                if ( f.exists() ) {
                    f.delete();
                }
                FileOutputStream fos = new FileOutputStream( getReportPath( eeVo.getId() ) );
                ObjectOutputStream oos = new ObjectOutputStream( fos );
                oos.writeObject( eeVo );
                oos.flush();
                oos.close();
            } catch ( Throwable e ) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the filled out value objects fills the link statistics from the cache. If it is not in the cache, the
     *         values will be null.
     */
    public void fillLinkStatsFromCache( Collection vos ) {
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;
            ExpressionExperimentValueObject cacheVo = retrieveValueObject( eeVo.getId() );
            if ( cacheVo != null ) {
                eeVo.setBioMaterialCount( cacheVo.getBioMaterialCount() );
                eeVo.setPreferredDesignElementDataVectorCount( cacheVo.getPreferredDesignElementDataVectorCount() );
                eeVo.setCoexpressionLinkCount( cacheVo.getCoexpressionLinkCount() );
                eeVo.setDateCached( cacheVo.getDateCached() );
                eeVo.setDateCreated( cacheVo.getDateCreated() );
                eeVo.setDateLastUpdated( cacheVo.getDateLastUpdated() );
            }
        }
    }

    /**
     * fills in event information from the database. This will only retrieve the latest event (if any).
     * 
     * @return the filled out value objects
     */
    @SuppressWarnings("unchecked")
    public void fillEventInformation( Collection<ExpressionExperimentValueObject> vos ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> ids = new ArrayList<Long>();
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;
            ids.add( eeVo.getId() );
        }

        // do this ahead to avoid round trips.
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( ids );

        watch.split();
        log.info( "Load ees in " + watch.getSplitTime() + "ms (wall time)" );
        watch.unsplit();

        // This is substantially faster than expressionExperimentService.getLastLinkAnalysis( ids ).
        Map<Long, AuditEvent> linkAnalysisEvents = getEvents( ees, LinkAnalysisEvent.Factory.newInstance() );
        Map<Long, AuditEvent> missingValueAnalysisEvents = getEvents( ees, MissingValueAnalysisEvent.Factory
                .newInstance() );
        Map<Long, AuditEvent> rankComputationEvents = getEvents( ees, RankComputationEvent.Factory.newInstance() );
        Map<Long, AuditEvent> troubleEvents = getEvents( ees, TroubleStatusFlagEvent.Factory.newInstance() );
        Map<Long, AuditEvent> validationEvents = getEvents( ees, ValidatedFlagEvent.Factory.newInstance() );
        Map<Long, AuditEvent> arrayDesignEvents = getEvents( ees, ArrayDesignGeneMappingEvent.Factory.newInstance() );

        watch.split();
        log.info( "Retrieval of event information done after " + watch.getSplitTime() + "ms (wall time)" );
        watch.unsplit();

        // add in the last events of interest for all eeVos
        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            if ( linkAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = linkAnalysisEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateLinkAnalysis( event.getDate() );
                    eeVo.setLinkAnalysisEventType( event.getEventType() );
                }
            }

            if ( missingValueAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = missingValueAnalysisEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateMissingValueAnalysis( ( event.getDate() ) );
                    eeVo.setMissingValueAnalysisEventType( event.getEventType() );
                }
            }

            if ( rankComputationEvents.containsKey( id ) ) {
                AuditEvent event = rankComputationEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateRankComputation( event.getDate() );
                    eeVo.setRankComputationEventType( event.getEventType() );
                }
            }

            if ( arrayDesignEvents.containsKey( id ) ) {
                AuditEvent event = arrayDesignEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateArrayDesignLastUpdated( event.getDate() );
                }
            }

            eeVo.setTroubleFlag( troubleEvents.get( id ) );
            eeVo.setValidatedFlag( validationEvents.get( id ) );
        }
    }

    /**
     * Populate information about how many annotations there are, and how many factor values there are.
     * 
     * @param vos
     */
    @SuppressWarnings("unchecked")
    public void fillAnnotationInformation( Collection<ExpressionExperimentValueObject> vos ) {
        Collection<Long> ids = new HashSet<Long>();
        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            ids.add( id );
        }

        Map<Long, Integer> annotationCounts = expressionExperimentService.getAnnotationCounts( ids );

        Map<Long, Integer> factorCounts = expressionExperimentService.getPopulatedFactorCounts( ids );

        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            eeVo.setNumAnnotations( annotationCounts.get( id ) );
            eeVo.setNumPopulatedFactors( factorCounts.get( id ) );
        }

    }

    @SuppressWarnings("unchecked")
    private Map<Long, AuditEvent> getEvents( Collection<ExpressionExperiment> ees, AuditEventType type ) {
        StopWatch watch = new StopWatch();
        watch.start();
        Map<Long, AuditEvent> result = new HashMap<Long, AuditEvent>();
        Map<Auditable, AuditEvent> events = null;
        if ( type instanceof ArrayDesignAnalysisEvent ) {
            events = expressionExperimentService.getLastArrayDesignUpdate( ees, type.getClass() );
        } else {
            events = expressionExperimentService.getLastAuditEvent( ees, type );
        }

        for ( Auditable a : events.keySet() ) {
            result.put( ( ( ExpressionExperiment ) a ).getId(), events.get( a ) );
        }
        watch.split();
        log.info( "Retrieval of events of type " + type.getClass().getSimpleName() + " done after "
                + watch.getSplitTime() + "ms (wall time)" );
        return result;
    }

    /**
     * @return the serialized value objects
     */
    private Collection retrieveValueObjects() {
        Collection eeValueObjects = null;
        // load all files that start with EE_LINK_SUMMARY
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return ( name.startsWith( EE_LINK_SUMMARY + "." ) );
            }
        };
        File fDir = new File( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        String[] filenames = fDir.list( filter );
        for ( String objectFile : filenames ) {
            try {
                FileInputStream fis = new FileInputStream( HOME_DIR + File.separatorChar + EE_REPORT_DIR
                        + File.separatorChar + objectFile );
                ObjectInputStream ois = new ObjectInputStream( fis );
                eeValueObjects = ( Collection ) ois.readObject();
                ois.close();
                fis.close();
            } catch ( Throwable e ) {
                return null;
            }
        }
        return eeValueObjects;
    }

    /**
     * @return the serialized value objects
     */
    @SuppressWarnings("unchecked")
    private Collection retrieveValueObjects( Collection ids ) {
        Collection eeValueObjects = new ArrayList<ExpressionExperiment>();

        for ( Object object : ids ) {
            Long id = ( Long ) object;

            try {
                File f = new File( getReportPath( id ) );
                if ( f.exists() ) {
                    FileInputStream fis = new FileInputStream( getReportPath( id ) );
                    ObjectInputStream ois = new ObjectInputStream( fis );
                    eeValueObjects.add( ois.readObject() );
                    ois.close();
                    fis.close();
                } else {
                    continue;
                }
            } catch ( Throwable e ) {
                return null;
            }
        }
        return eeValueObjects;
    }

    /**
     * @return the serialized value object
     */
    private ExpressionExperimentValueObject retrieveValueObject( long id ) {

        ExpressionExperimentValueObject eeVo = null;
        try {
            File f = new File( getReportPath( id ) );
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( getReportPath( id ) );
                ObjectInputStream ois = new ObjectInputStream( fis );
                eeVo = ( ExpressionExperimentValueObject ) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch ( Throwable e ) {
            return null;
        }
        return eeVo;
    }

    /**
     * @param id
     * @return
     */
    private String getReportPath( long id ) {
        return HOME_DIR + File.separatorChar + EE_REPORT_DIR + File.separatorChar + EE_LINK_SUMMARY + "." + id;
    }

    private void initDirectories( boolean deleteFiles ) {
        // check to see if the home directory exists. If it doesn't, create it.
        // check to see if the reports directory exists. If it doesn't, create it.
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        File f = new File( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        Collection<File> files = new ArrayList<File>();
        File[] fileArray = f.listFiles();
        for ( File file : fileArray ) {
            files.add( file );
        }
        // clear out all files
        if ( deleteFiles ) {
            FileTools.deleteFiles( files );
        }
    }

    // Methods needed to allow this to be used in a space.

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTask#execute()
     */
    public SpacesResult execute( SpacesCommand spacesCommand ) {
        this.generateSummaryObjects();
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#getTaskId()
     */
    public String getTaskId() {
        return taskId;
    }

}
