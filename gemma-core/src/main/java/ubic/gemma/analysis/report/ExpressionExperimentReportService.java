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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTask;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * Handles creation, serialization and/or marshalling of reports about expression experiments. Reports are stored in
 * ExpressionExperimentValueObjects.
 * 
 * @author jsantos
 * @author paul
 * @spring.bean name="expressionExperimentReportService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 * @spring.property name="auditEventService" ref="auditEventService"
 * @spring.property name="probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 * @spring.property name="securityService" ref="securityService"
 * @version $Id$
 */
public class ExpressionExperimentReportService extends BaseSpacesTask implements ExpressionExperimentReportTask {
    private AuditEventService auditEventService;

    private AuditTrailService auditTrailService;
    private String EE_LINK_SUMMARY = "AllExpressionLinkSummary";
    private String EE_REPORT_DIR = "ExpressionExperimentReports";
    private ExpressionExperimentService expressionExperimentService;
    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private Log log = LogFactory.getLog( this.getClass() );
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    private SecurityService securityService;
    private String taskId = null;

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTask#execute()
     */
    public TaskResult execute( TaskCommand spacesCommand ) {
        this.generateSummaryObjects();
        return null;
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

    /**
     * Fills in event and security information from the database. This will only retrieve the latest event (if any).
     * This is rather slow so should be avoided if the information isn't needed.
     * 
     * @return the filled out value objects
     */
    public void fillEventInformation( Collection<ExpressionExperimentValueObject> vos ) {
        Collection<Long> ids = new ArrayList<Long>();
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;
            ids.add( eeVo.getId() );
        }

        // do this ahead to avoid round trips.
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( ids );

        Map<Long, ExpressionExperiment> eemap = new HashMap<Long, ExpressionExperiment>();
        for ( ExpressionExperiment ee : ees ) {
            eemap.put( ee.getId(), ee );
        }

        if ( ees.size() == 0 ) {
            return;
        }
        // This is substantially faster than expressionExperimentService.getLastLinkAnalysis( ids ).
        Map<Long, AuditEvent> linkAnalysisEvents = getEvents( ees, LinkAnalysisEvent.Factory.newInstance() );
        Map<Long, AuditEvent> missingValueAnalysisEvents = getEvents( ees, MissingValueAnalysisEvent.Factory
                .newInstance() );
        Map<Long, AuditEvent> rankComputationEvents = getEvents( ees, ProcessedVectorComputationEvent.Factory
                .newInstance() );
        Map<Long, AuditEvent> troubleEvents = getEvents( ees, TroubleStatusFlagEvent.Factory.newInstance() );
        Map<Long, AuditEvent> validationEvents = getEvents( ees, ValidatedFlagEvent.Factory.newInstance() );
        Map<Long, AuditEvent> arrayDesignEvents = getEvents( ees, ArrayDesignGeneMappingEvent.Factory.newInstance() );
        Map<Long, AuditEvent> differentialAnalysisEvents = getEvents( ees, DifferentialExpressionAnalysisEvent.Factory
                .newInstance() );
        Map<Long, Collection<AuditEvent>> sampleRemovalEvents = getSampleRemovalEvents( ees );

        Map<Securable, Boolean> privacyInfo = securityService.arePrivate( ees );

        // add in the last events of interest for all eeVos
        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            if ( linkAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = linkAnalysisEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateLinkAnalysis( event.getDate() );
                    eeVo.setLinkAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( missingValueAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = missingValueAnalysisEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateMissingValueAnalysis( ( event.getDate() ) );
                    eeVo.setMissingValueAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( rankComputationEvents.containsKey( id ) ) {
                AuditEvent event = rankComputationEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateProcessedDataVectorComputation( event.getDate() );
                    eeVo.setProcessedDataVectorComputationEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( arrayDesignEvents.containsKey( id ) ) {
                AuditEvent event = arrayDesignEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateArrayDesignLastUpdated( event.getDate() );
                }
            }

            if ( differentialAnalysisEvents.containsKey( id ) ) {
                AuditEvent event = differentialAnalysisEvents.get( id );
                if ( event != null ) {
                    eeVo.setDateDifferentialAnalysis( event.getDate() );
                    eeVo.setDifferentialAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( sampleRemovalEvents.containsKey( id ) ) {
                Collection<AuditEvent> e = sampleRemovalEvents.get( id );
                // we find we are getting lazy-load exceptions from this guy.
                eeVo.setSampleRemovedFlagsFromAuditEvent( e );
            }

            if ( troubleEvents.containsKey( id ) ) {
                AuditEvent trouble = troubleEvents.get( id );
                // we find we are getting lazy-load exceptions from this guy.
                auditEventService.thaw( trouble );
                eeVo.setTroubleFlag( new AuditEventValueObject( trouble ) );
            }
            if ( validationEvents.containsKey( id ) ) {
                AuditEvent validated = validationEvents.get( id );
                auditEventService.thaw( validated );
                eeVo.setValidatedFlag( new AuditEventValueObject( validated ) );
            }

            ExpressionExperiment ee = eemap.get( id );
            if ( privacyInfo.containsKey( ee ) ) {
                eeVo.setIsPublic( !privacyInfo.get( ee ) );
            }
        }
        log.debug( "processed events" );
    }

    /**
     * @return the filled out value objects fills the link statistics from the cache. If it is not in the cache, the
     *         values will be null.
     */
    public void fillLinkStatsFromCache( Collection<ExpressionExperimentValueObject> vos ) {
        StopWatch timer = new StopWatch();
        timer.start();
        for ( ExpressionExperimentValueObject eeVo : vos ) {
            ExpressionExperimentValueObject cacheVo = retrieveValueObject( eeVo.getId() );
            if ( cacheVo != null ) {
                eeVo.setBioMaterialCount( cacheVo.getBioMaterialCount() );
                eeVo.setProcessedExpressionVectorCount( cacheVo.getProcessedExpressionVectorCount() );
                eeVo.setCoexpressionLinkCount( cacheVo.getCoexpressionLinkCount() );
                eeVo.setDateCached( cacheVo.getDateCached() );
                eeVo.setDateCreated( cacheVo.getDateCreated() );
                eeVo.setDateLastUpdated( cacheVo.getDateLastUpdated() );
            }
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Link stats read from cache in " + timer.getTime() + "ms" );
        }
    }

    /**
     * Generate a value object that contain summary information about links, biomaterials, and datavectors
     */
    @SuppressWarnings("unchecked")
    public ExpressionExperimentValueObject generateSummaryObject( Long id ) {
        Collection ids = new ArrayList<Long>();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> results = generateSummaryObjects( ids );
        if ( results.size() > 0 ) {
            return results.iterator().next();
        }
        return null;
    }

    /**
     * Generates a collection of value objects that contain summary information about links, biomaterials, and
     * datavectors. Use of this method should be restricted to admins (see security settings)
     * 
     * @return
     */
    public void generateSummaryObjects() {
        initDirectories( false );
        Collection<ExpressionExperimentValueObject> vos = expressionExperimentService.loadAllValueObjects();
        getStats( vos );
    }

    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and
     * datavectors
     * 
     * @return
     */
    public Collection<ExpressionExperimentValueObject> generateSummaryObjects( Collection<Long> ids ) {
        initDirectories( false );

        Collection<Long> filteredIds = securityFilterExpressionExperimentIds( ids );
        Collection<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjects( filteredIds );
        getStats( vos );
        return vos;
    }

    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @return the probe2ProbeCoexpressionService
     */
    public Probe2ProbeCoexpressionService getProbe2ProbeCoexpressionService() {
        return probe2ProbeCoexpressionService;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.SpacesTask#getTaskId()
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * retrieves a collection of cached value objects containing summary information
     * 
     * @return a collection of cached value objects
     */
    public Collection<ExpressionExperimentValueObject> retrieveSummaryObjects() {
        return retrieveValueObjects();
    }

    /**
     * retrieves a collection of cached value objects containing summary information
     * 
     * @return a collection of cached value objects
     */
    public Collection<ExpressionExperimentValueObject> retrieveSummaryObjects( Collection<Long> ids ) {
        return retrieveValueObjects( ids );
    }

    public void setAuditEventService( AuditEventService auditEventService ) {
        this.auditEventService = auditEventService;
    }

    /**
     * @param auditTrailService the auditTrailService to set
     */
    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param probe2ProbeCoexpressionService the probe2ProbeCoexpressionService to set
     */
    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, AuditEvent> getEvents( Collection<ExpressionExperiment> ees, AuditEventType type ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Map<Long, AuditEvent> result = new HashMap<Long, AuditEvent>();
        Map<Auditable, AuditEvent> events = null;
        if ( type instanceof ArrayDesignAnalysisEvent ) {
            events = expressionExperimentService.getLastArrayDesignUpdate( ees, type.getClass() );

        } else if ( type instanceof TroubleStatusFlagEvent ) {
            // This service unlike the others needs ids not EE objects
            Collection<Long> eeIds = new HashSet<Long>();
            for ( ExpressionExperiment ee : ees ) {
                eeIds.add( ee.getId() );
            }
            timer.stop();
            if ( timer.getTime() > 1000 ) {
                log.info( "Retrieved " + type.getClass().getSimpleName() + " in " + timer.getTime() + "ms" );
            }
            // This service unlike the others returns ids to events
            return expressionExperimentService.getLastTroubleEvent( eeIds );

        } else {
            events = expressionExperimentService.getLastAuditEvent( ees, type );
        }

        for ( Auditable a : events.keySet() ) {
            result.put( ( ( ExpressionExperiment ) a ).getId(), events.get( a ) );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Retrieved " + type.getClass().getSimpleName() + " in " + timer.getTime() + "ms" );
        }
        return result;
    }

    /**
     * @param id
     * @return
     */
    private String getReportPath( long id ) {
        return HOME_DIR + File.separatorChar + EE_REPORT_DIR + File.separatorChar + EE_LINK_SUMMARY + "." + id;
    }

    private Map<Long, Collection<AuditEvent>> getSampleRemovalEvents( Collection<ExpressionExperiment> ees ) {
        Map<Long, Collection<AuditEvent>> result = new HashMap<Long, Collection<AuditEvent>>();
        Map<ExpressionExperiment, Collection<AuditEvent>> rawr = expressionExperimentService
                .getSampleRemovalEvents( ees );
        for ( ExpressionExperiment e : rawr.keySet() ) {
            result.put( e.getId(), rawr.get( e ) );
        }
        return result;
    }

    /**
     * Compute statistics for EEs and serialize them to disk for later retrieval.
     * 
     * @param vos
     */
    private void getStats( Collection<ExpressionExperimentValueObject> vos ) {
        log.info( "Getting stats for " + vos.size() + " value objects." );
        int count = 0;
        for ( ExpressionExperimentValueObject object : vos ) {
            getStats( object );
            // periodic updates.
            if ( ++count % 10 == 0 ) {
                log.info( "Processed " + count + " reports." );
            }
        }
        log.info( "Done, processed " + count + " reports" );
    }

    /**
     * @param object
     */
    private void getStats( ExpressionExperimentValueObject eeVo ) {
        ExpressionExperiment tempEe = expressionExperimentService.load( eeVo.getId() );

        eeVo.setBioMaterialCount( expressionExperimentService.getBioMaterialCount( tempEe ) );
        eeVo
                .setProcessedExpressionVectorCount( expressionExperimentService
                        .getProcessedExpressionVectorCount( tempEe ) );

        long numLinks = probe2ProbeCoexpressionService.countLinks( tempEe ).longValue();
        log.debug( numLinks + " links." );
        eeVo.setCoexpressionLinkCount( numLinks );

        Date timestamp = new Date( System.currentTimeMillis() );
        eeVo.setDateCached( timestamp );

        auditTrailService.thaw( tempEe.getAuditTrail() );
        if ( tempEe.getAuditTrail() != null ) {
            eeVo.setDateCreated( tempEe.getAuditTrail().getCreationEvent().getDate() );
        }
        eeVo.setDateLastUpdated( tempEe.getAuditTrail().getLast().getDate() );

        saveValueObject( eeVo );

        log.debug( "Generated report for " + eeVo.getShortName() );
    }

    // Methods needed to allow this to be used in a space.

    /**
     * Check to see if the top level report storage directory exists. If it doesn't, create it, Check to see if the
     * reports directory exists. If it doesn't, create it.
     * 
     * @param deleteFiles
     */
    private void initDirectories( boolean deleteFiles ) {

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

    /**
     * @return the serialized value object
     */
    private ExpressionExperimentValueObject retrieveValueObject( long id ) {

        Collection<Long> ids = new HashSet<Long>();
        ids.add( id );

        Collection<ExpressionExperimentValueObject> obs = retrieveValueObjects( ids );
        if ( obs.size() == 1 ) {
            return obs.iterator().next();
        }
        return null;
    }

    /**
     * @return the serialized value objects
     */
    @SuppressWarnings("unchecked")
    private Collection<ExpressionExperimentValueObject> retrieveValueObjects() {
        Collection<ExpressionExperimentValueObject> eeValueObjects = null;
        // load all files that start with EE_LINK_SUMMARY
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return ( name.startsWith( EE_LINK_SUMMARY + "." ) );
            }
        };
        File fDir = new File( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        String[] filenames = fDir.list( filter );

        int numWarnings = 0;
        int maxWarnings = 5; // don't put 1000 warnings in the logs!
        for ( String objectFile : filenames ) {
            try {
                FileInputStream fis = new FileInputStream( HOME_DIR + File.separatorChar + EE_REPORT_DIR
                        + File.separatorChar + objectFile );
                ObjectInputStream ois = new ObjectInputStream( fis );
                eeValueObjects = ( Collection<ExpressionExperimentValueObject> ) ois.readObject();
                ois.close();
                fis.close();
            } catch ( IOException e ) {
                if ( numWarnings < maxWarnings ) {
                    log.warn( "Unable to read report object from " + objectFile + ": " + e.getMessage() );
                } else if ( numWarnings == maxWarnings ) {
                    log.warn( "Skipping futher warnings ... reports need refreshing." );
                }
                numWarnings++;
                continue;
            } catch ( ClassNotFoundException e ) {
                if ( numWarnings < maxWarnings ) {
                    log.warn( "Unable to read report object from " + objectFile + ": " + e.getMessage() );
                } else if ( numWarnings == maxWarnings ) {
                    log.warn( "Skipping futher warnings ... reports need refreshing" );
                }
                numWarnings++;
                continue;
            }
        }
        return eeValueObjects;
    }

    /**
     * @return the serialized value objects
     */
    private Collection<ExpressionExperimentValueObject> retrieveValueObjects( Collection<Long> ids ) {
        Collection<ExpressionExperimentValueObject> eeValueObjects = new ArrayList<ExpressionExperimentValueObject>();
        Collection<Long> filteredIds = securityFilterExpressionExperimentIds( ids );

        int numWarnings = 0;
        int maxWarnings = 5; // don't put 1000 warnings in the logs!

        for ( Object object : filteredIds ) {
            Long id = ( Long ) object;
            File f = new File( getReportPath( id ) );
            try {
                if ( f.exists() ) {
                    FileInputStream fis = new FileInputStream( getReportPath( id ) );
                    ObjectInputStream ois = new ObjectInputStream( fis );
                    eeValueObjects.add( ( ExpressionExperimentValueObject ) ois.readObject() );
                    ois.close();
                    fis.close();
                } else {
                    continue;
                }
            } catch ( IOException e ) {
                if ( numWarnings < maxWarnings ) {
                    log.warn( "Unable to read report object for id =" + id + ": " + e.getMessage() );
                } else if ( numWarnings == maxWarnings ) {
                    log.warn( "Skipping futher warnings ... reports need refreshing." );
                }
                numWarnings++;
                continue;
            } catch ( ClassNotFoundException e ) {
                if ( numWarnings < maxWarnings ) {
                    log.warn( "Unable to read report object from " + f + ": " + e.getMessage() );
                } else if ( numWarnings == maxWarnings ) {
                    log.warn( "Skipping futher warnings ... reports need refreshing" );
                }
                numWarnings++;
                continue;
            }
        }
        return eeValueObjects;
    }

    /**
     * @param eeVo Expression Experiment value objects to serialize
     */
    private void saveValueObject( ExpressionExperimentValueObject eeVo ) {
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
            log.warn( e );
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Long> securityFilterExpressionExperimentIds( Collection<Long> ids ) {
        /*
         * Because this method returns the results, we have to screen.
         */
        Collection<ExpressionExperiment> securityScreened = expressionExperimentService.loadMultiple( ids );

        Collection<Long> filteredIds = new HashSet<Long>();
        for ( ExpressionExperiment ee : securityScreened ) {
            filteredIds.add( ee.getId() );
        }
        return filteredIds;
    }

}
