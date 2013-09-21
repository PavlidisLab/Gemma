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

import gemma.gsec.SecurityService;
import gemma.gsec.model.Securable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.AutomatedAnnotationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedAnnotations;
import ubic.gemma.model.expression.bioAssayData.ProcessedDataVectorCache;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.Settings;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.visualization.ExperimentalDesignVisualizationService;

/**
 * Handles creation, serialization and/or marshaling of reports about expression experiments. Reports are stored in
 * ExpressionExperimentValueObjects.
 * 
 * @author jsantos
 * @author paul
 * @author klc
 * @version $Id$
 */
@Component
public class ExpressionExperimentReportServiceImpl implements ExpressionExperimentReportService, InitializingBean {

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    private String EESTATS_CACHE_NAME = "ExpressionExperimentReportsCache";

    /**
     * Batch of classes we can get events for all at once.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends AuditEventType>[] eventTypes = new Class[] { LinkAnalysisEvent.class,
            MissingValueAnalysisEvent.class, ProcessedVectorComputationEvent.class, ValidatedAnnotations.class,
            DifferentialExpressionAnalysisEvent.class, AutomatedAnnotationEvent.class,
            BatchInformationFetchingEvent.class, PCAAnalysisEvent.class };

    @Autowired
    private ExperimentalDesignVisualizationService experimentalDesignVisualizationService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;

    @Autowired
    private ProcessedDataVectorCache processedDataVectorCache;

    @Autowired
    private SecurityService securityService;

    /**
     * Cache to hold stats in memory. This is used to avoid hittinig the disk for reports too often.
     */
    private Cache statsCache;

    @Override
    public void afterPropertiesSet() throws Exception {
        /*
         * Initialize the cache; if it already exists it will not be recreated.
         */
        boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", false );
        boolean diskPersistent = Settings.getBoolean( "gemma.cache.diskpersistent", false ) && !terracottaEnabled;
        int maxElements = 5000;
        boolean eternal = false;
        boolean overFlowToDisk = false;
        int diskExpiryThreadIntervalSeconds = 600;
        int maxElementsOnDisk = 10000;
        int secondsToLive = 300;
        boolean terracottaCoherentReads = false;
        boolean clearOnFlush = false;

        if ( terracottaEnabled ) {
            CacheConfiguration config = new CacheConfiguration( EESTATS_CACHE_NAME, maxElements );
            config.setStatistics( false );
            config.setMemoryStoreEvictionPolicy( MemoryStoreEvictionPolicy.LRU.toString() );
            config.setOverflowToDisk( false );
            config.setEternal( eternal );
            config.setTimeToIdleSeconds( 0 );
            config.setTimeToLiveSeconds( secondsToLive );
            config.setMaxElementsOnDisk( maxElementsOnDisk );
            config.addTerracotta( new TerracottaConfiguration() );
            config.getTerracottaConfiguration().setCoherentReads( terracottaCoherentReads );
            config.clearOnFlush( clearOnFlush );

            config.getTerracottaConfiguration().setClustered( true );
            config.getTerracottaConfiguration().setValueMode( "SERIALIZATION" );
            NonstopConfiguration nonstopConfiguration = new NonstopConfiguration();
            TimeoutBehaviorConfiguration tobc = new TimeoutBehaviorConfiguration();
            tobc.setType( TimeoutBehaviorType.NOOP.getTypeName() );
            nonstopConfiguration.addTimeoutBehavior( tobc );
            config.getTerracottaConfiguration().addNonstop( nonstopConfiguration );
            this.statsCache = new Cache( config );
        } else {
            this.statsCache = new Cache( EESTATS_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU,
                    overFlowToDisk, null, eternal, secondsToLive, 0, diskPersistent, diskExpiryThreadIntervalSeconds,
                    null );
        }

        cacheManager.addCache( statsCache );
        this.statsCache = cacheManager.getCache( EESTATS_CACHE_NAME );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ExpressionExperimentReportService#evictFromCache(java.lang.Long)
     */
    @Override
    public void evictFromCache( Long id ) {
        this.statsCache.remove( id );

        processedDataVectorCache.clearCache( id );
        experimentalDesignVisualizationService.clearCaches( id );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ExpressionExperimentReportService#generateSummaryObject(java.lang.Long)
     */
    @Override
    public ExpressionExperimentValueObject generateSummary( Long id ) {
        assert id != null;
        Collection<Long> ids = new ArrayList<Long>();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> results = generateSummaryObjects( ids );
        if ( results.size() > 0 ) {
            return results.iterator().next();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ExpressionExperimentReportService#generateSummaryObjects()
     */
    @Override
    @Secured({ "GROUP_AGENT" })
    public void generateSummaryObjects() {
        Collection<Long> ids = EntityUtils.getIds( expressionExperimentService.loadAll() );
        Collection<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjects( ids, false );
        getStats( vos );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ExpressionExperimentReportService#generateSummaryObjects(java.util.Collection)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> generateSummaryObjects( Collection<Long> ids ) {

        Collection<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjects( ids, false );
        getStats( vos );

        for ( ExpressionExperimentValueObject vo : vos ) {
            evictFromCache( vo.getId() );
            statsCache.put( new Element( vo.getId(), vo ) );
        }
        return vos;
    }

    /**
     * Populate information about how many annotations there are, and how many factor values there are. Batch is not
     * counted towards the number of factors
     * 
     * @param vos
     */
    @Override
    public void getAnnotationInformation( Collection<ExpressionExperimentValueObject> vos ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Long> ids = new HashSet<Long>();
        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            ids.add( id );
        }

        Map<Long, Integer> annotationCounts = expressionExperimentService.getAnnotationCounts( ids );

        Map<Long, Integer> factorCounts = expressionExperimentService.getPopulatedFactorCountsExcludeBatch( ids );

        for ( ExpressionExperimentValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            eeVo.setNumAnnotations( annotationCounts.get( id ) );
            eeVo.setNumPopulatedFactors( factorCounts.get( id ) );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Fill annotation information: " + timer.getTime() + "ms" );
        }

    }

    /**
     * Fills in event and security information from the database. This will only retrieve the latest event (if any).
     * This is rather slow so should be avoided if the information isn't needed.
     * 
     * @return Map of EE ids to the most recent update.
     */
    @Override
    public Map<Long, Date> getEventInformation( Collection<ExpressionExperimentValueObject> vos ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Long> ids = EntityUtils.getIds( vos );

        Map<Long, Date> results = new HashMap<Long, Date>();

        // do this ahead to avoid round trips - this also filters...
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( ids );

        if ( ees.size() == 0 ) {
            return results;
        }

        Map<Long, ExpressionExperiment> eemap = EntityUtils.getIdMap( ees );

        Collection<Long> troubledEEs = getTroubled( ees );
        Map<Long, Date> lastArrayDesignUpdates = expressionExperimentService.getLastArrayDesignUpdate( ees );
        Collection<Class<? extends AuditEventType>> typesToGet = Arrays.asList( eventTypes );

        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> events = getEvents( ees, typesToGet );

        Map<Auditable, AuditEvent> linkAnalysisEvents = events.get( LinkAnalysisEvent.class );
        Map<Auditable, AuditEvent> missingValueAnalysisEvents = events.get( MissingValueAnalysisEvent.class );
        Map<Auditable, AuditEvent> rankComputationEvents = events.get( ProcessedVectorComputationEvent.class );

        Collection<Long> validatedEEs = getValidated( vos );

        Map<Auditable, AuditEvent> differentialAnalysisEvents = events.get( DifferentialExpressionAnalysisEvent.class );
        Map<Auditable, AuditEvent> autotaggerEvents = events.get( AutomatedAnnotationEvent.class );
        Map<Auditable, AuditEvent> batchFetchEvents = events.get( BatchInformationFetchingEvent.class );
        Map<Auditable, AuditEvent> pcaAnalysisEvents = events.get( PCAAnalysisEvent.class );

        Map<Long, Collection<AuditEvent>> sampleRemovalEvents = getSampleRemovalEvents( ees );

        Map<Securable, Boolean> privacyInfo = securityService.arePrivate( ees );
        Map<Securable, Boolean> sharingInfo = securityService.areShared( ees );

        /*
         * add in the last events of interest for all eeVos This step is remarkably slow.
         */
        for ( ExpressionExperimentValueObject eeVo : vos ) {

            /*
             * Note that in the current incarnation, the last update date is already filled in, so the checks in this
             * loop are superfluous.
             */
            Date mostRecentDate = eeVo.getDateLastUpdated() == null ? new Date( 0 ) : eeVo.getDateLastUpdated();

            Long id = eeVo.getId();

            ExpressionExperiment ee = eemap.get( id );

            if ( linkAnalysisEvents.containsKey( ee ) ) {
                AuditEvent event = linkAnalysisEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateLinkAnalysis( date );

                    eeVo.setLinkAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( missingValueAnalysisEvents.containsKey( ee ) ) {
                AuditEvent event = missingValueAnalysisEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateMissingValueAnalysis( date );

                    eeVo.setMissingValueAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( rankComputationEvents.containsKey( ee ) ) {
                AuditEvent event = rankComputationEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateProcessedDataVectorComputation( date );

                    eeVo.setProcessedDataVectorComputationEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( differentialAnalysisEvents.containsKey( ee ) ) {
                AuditEvent event = differentialAnalysisEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateDifferentialAnalysis( date );

                }
            }

            if ( pcaAnalysisEvents.containsKey( ee ) ) {
                AuditEvent event = pcaAnalysisEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDatePcaAnalysis( date );

                    eeVo.setPcaAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( batchFetchEvents.containsKey( ee ) ) {
                AuditEvent event = batchFetchEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateBatchFetch( date );

                    eeVo.setBatchFetchEventType( event.getEventType().getClass().getSimpleName() );
                }
            }

            if ( lastArrayDesignUpdates.containsKey( id ) ) {
                Date date = lastArrayDesignUpdates.get( id );
                eeVo.setDateArrayDesignLastUpdated( date );
            }

            if ( validatedEEs.contains( ee.getId() ) ) {

                eeVo.setValidated( true );

            }

            if ( autotaggerEvents.containsKey( ee ) ) {
                AuditEvent taggerEvent = autotaggerEvents.get( ee );

                if ( taggerEvent.getDate().after( mostRecentDate ) ) {
                    mostRecentDate = taggerEvent.getDate();
                }

                eeVo.setAutoTagDate( taggerEvent.getDate() );
            }

            if ( privacyInfo.containsKey( ee ) ) {
                eeVo.setIsPublic( !privacyInfo.get( ee ) );
            }

            if ( sharingInfo.containsKey( ee ) ) {
                eeVo.setIsShared( sharingInfo.get( ee ) );
            }

            /*
             * The following are keyed by ID
             */

            if ( sampleRemovalEvents.containsKey( id ) ) {
                Collection<AuditEvent> removalEvents = sampleRemovalEvents.get( id );
                // we find we are getting lazy-load exceptions from this guy.
                eeVo.auditEvents2SampleRemovedFlags( removalEvents );

            }

            if ( troubledEEs.contains( id ) ) {
                Collection<Long> tids = new HashSet<Long>();
                tids.add( id );
                Map<Long, AuditEvent> troublM = expressionExperimentService.getLastTroubleEvent( tids );
                if ( !troublM.isEmpty() ) {
                    eeVo.setTroubleDetails( troublM.get( id ).getDate() + ": " + troublM.get( id ).getNote() );
                    eeVo.setTroubled( true );
                } else {
                    log.warn( "Status was out of date? for EE=" + id );
                }
            }

            if ( validatedEEs.contains( id ) ) {
                eeVo.setValidated( true );
            }

            if ( mostRecentDate.after( new Date( 0 ) ) ) results.put( ee.getId(), mostRecentDate );
        }

        if ( timer.getTime() > 1000 ) log.info( "Retrieving audit events took " + timer.getTime() + "ms" );

        return results;
    }


    @Override
    public Map<Long, Date> getReportInformation( Collection<ExpressionExperimentValueObject> vos ) {
        StopWatch timer = new StopWatch();
        Map<Long, Date> result = new HashMap<Long, Date>();
        timer.start();

        List<Long> ids = new ArrayList<Long>();
        for ( ExpressionExperimentValueObject vo : vos ) {
            ids.add( vo.getId() );
        }

        Collection<ExpressionExperimentValueObject> cachedVos = retrieveSummaryObjects( ids );
        Map<Long, ExpressionExperimentValueObject> id2cachedVo = EntityUtils.getIdMap( cachedVos );

        for ( ExpressionExperimentValueObject eeVo : vos ) {
            ExpressionExperimentValueObject cacheVo = id2cachedVo.get( eeVo.getId() );
            if ( cacheVo != null ) {
                eeVo.setBioMaterialCount( cacheVo.getBioMaterialCount() );
                eeVo.setProcessedExpressionVectorCount( cacheVo.getProcessedExpressionVectorCount() );
                eeVo.setCoexpressionLinkCount( cacheVo.getCoexpressionLinkCount() );
                eeVo.setDateCached( cacheVo.getDateCached() );
                eeVo.setDifferentialExpressionAnalyses( cacheVo.getDifferentialExpressionAnalyses() );

                if ( eeVo.getDateCreated() == null ) {
                    // should be filled in already.
                    log.warn( "Create date was not populated: " + eeVo );
                    eeVo.setDateCreated( cacheVo.getDateCreated() );
                    eeVo.setDateLastUpdated( cacheVo.getDateLastUpdated() );
                }

                if ( eeVo.getDateLastUpdated() != null ) {
                    result.put( eeVo.getId(), eeVo.getDateLastUpdated() );
                } else {
                    result.put( eeVo.getId(), eeVo.getDateCreated() );
                }

            }
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( vos.size() + " EE reports fetched in " + timer.getTime() + "ms" );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ExpressionExperimentReportService#retrieveSummaryObjects(java.util.Collection)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> retrieveSummaryObjects( Collection<Long> ids ) {
        Collection<ExpressionExperimentValueObject> eeValueObjects = new ArrayList<ExpressionExperimentValueObject>();
        Collection<Long> filteredIds = securityFilterExpressionExperimentIds( ids );

        int incache = 0;
        for ( Long id : filteredIds ) {

            Element cachedElement = this.statsCache.get( id );
            if ( cachedElement != null ) {
                incache++;
                Serializable el = cachedElement.getValue();
                assert el instanceof ExpressionExperimentValueObject;

                eeValueObjects.add( ( ExpressionExperimentValueObject ) el );
                continue;
            }

            ExpressionExperimentValueObject valueObject = generateSummary( id );
            eeValueObjects.add( valueObject );
            continue;

        }
        if ( ids.size() > 1 ) {
            log.info( incache + "/" + ids.size() + " reports were found in the cache" );
        }
        return eeValueObjects;
    }

    /**
     * @param ees
     * @param types
     * @return
     */
    private Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getEvents(
            Collection<ExpressionExperiment> ees, Collection<Class<? extends AuditEventType>> types ) {

        return auditEventService.getLastEvents( ees, types );

    }

    /**
     * @param ees
     * @return
     */
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
     * Compute statistics for EEs, that aren't immediately part of the value object.
     * 
     * @param vos
     */
    private void getStats( Collection<ExpressionExperimentValueObject> vos ) {
        log.debug( "Getting stats for " + vos.size() + " value objects." );
        int count = 0;
        for ( ExpressionExperimentValueObject object : vos ) {
            getStats( object );

            if ( ++count % 10 == 0 ) {
                log.debug( "Processed " + count + " reports." );
            }
        }
        log.debug( "Done, processed " + count + " reports" );
    }

    /**
     * Get the stats report for one EE
     * 
     * @param object
     */
    private void getStats( ExpressionExperimentValueObject eeVo ) {
        Long id = eeVo.getId();
        assert id != null;

        eeVo.setDifferentialExpressionAnalyses( differentialExpressionAnalysisService.getAnalysisValueObjects( id ) );

        eeVo.setCoexpressionLinkCount( probe2ProbeCoexpressionService.countLinks( id ) );

        Date timestamp = new Date( System.currentTimeMillis() );
        eeVo.setDateCached( timestamp );
        assert eeVo.getDateCreated() != null;
        assert eeVo.getDateLastUpdated() != null;

    }

    private Collection<Long> getTroubled( Collection<ExpressionExperiment> ees ) {
        Collection<Long> ids = EntityUtils.getIds( ees );
        Collection<Long> untroubled = expressionExperimentService.getUntroubled( ids );
        ids.removeAll( untroubled );
        return ids;
    }

    private Collection<Long> getValidated( Collection<ExpressionExperimentValueObject> vos ) {
        Collection<Long> result = new HashSet<Long>();
        for ( ExpressionExperimentValueObject ee : vos ) {

            if ( ee.getValidated() ) {
                result.add( ee.getId() );
            }
        }
        return result;
    }

    /**
     * @param ids
     * @return
     */
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
