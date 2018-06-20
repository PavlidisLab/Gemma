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
package ubic.gemma.core.analysis.report;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import ubic.gemma.core.visualization.ExperimentalDesignVisualizationService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedDataVectorCache;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.CacheUtils;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Settings;

import java.util.*;

/**
 * Handles creation, serialization and/or marshaling of reports about expression experiments. Reports are stored in
 * ExpressionExperimentValueObjects.
 *
 * @author jsantos
 * @author paul
 * @author klc
 */
@Service
public class ExpressionExperimentReportServiceImpl implements ExpressionExperimentReportService, InitializingBean {

    private static final String NOTE_UPDATED_CONFOUND = "Updated batch confound";
    private static final String NOTE_UPDATED_EFFECT = "Updated batch effect";
    private static final String EESTATS_CACHE_NAME = "ExpressionExperimentReportsCache";
    private final Log log = LogFactory.getLog( this.getClass() );
    /**
     * Batch of classes we can get events for all at once.
     */
    @SuppressWarnings("unchecked")
    private final Class<? extends AuditEventType>[] eventTypes = new Class[] { LinkAnalysisEvent.class,
            MissingValueAnalysisEvent.class, ProcessedVectorComputationEvent.class,
            DifferentialExpressionAnalysisEvent.class, BatchInformationFetchingEvent.class, PCAAnalysisEvent.class };

    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExperimentalDesignVisualizationService experimentalDesignVisualizationService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ProcessedDataVectorCache processedDataVectorCache;

    /**
     * Cache to hold stats in memory. This is used to avoid hittinig the disk for reports too often.
     */
    private Cache statsCache;

    @Override
    public void afterPropertiesSet() {
        boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", false );
        boolean diskPersistent = Settings.getBoolean( "gemma.cache.diskpersistent", false ) && !terracottaEnabled;

        this.statsCache = CacheUtils
                .createOrLoadCache( cacheManager, ExpressionExperimentReportServiceImpl.EESTATS_CACHE_NAME,
                        terracottaEnabled, 5000, false, false, 0, 300, diskPersistent );

    }

    @Override
    public void evictFromCache( Long id ) {
        this.statsCache.remove( id );

        processedDataVectorCache.clearCache( id );
        experimentalDesignVisualizationService.clearCaches( id );

    }

    @Override
    public ExpressionExperimentDetailsValueObject generateSummary( Long id ) {
        assert id != null;
        Collection<Long> ids = Collections.singletonList( id );
        Collection<ExpressionExperimentDetailsValueObject> results = this.generateSummaryObjects( ids );
        if ( results.size() > 0 ) {
            return results.iterator().next();
        }
        return null;
    }

    @Override
    @Secured({ "GROUP_AGENT" })
    public void generateSummaryObjects() {
        Collection<Long> ids = EntityUtils.getIds( expressionExperimentService.loadAll() );
        Collection<ExpressionExperimentDetailsValueObject> vos = expressionExperimentService
                .loadDetailsValueObjects( null, false, ids, null, 0, 0 );
        this.getStats( vos );
    }

    /**
     * Populate information about how many annotations there are, and how many factor values there are. Batch is not
     * counted towards the number of factors
     */
    @Override
    public void getAnnotationInformation( Collection<ExpressionExperimentDetailsValueObject> vos ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Long> ids = new HashSet<>();
        for ( ExpressionExperimentDetailsValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            ids.add( id );
        }

        Map<Long, Integer> annotationCounts = expressionExperimentService.getAnnotationCounts( ids );

        Map<Long, Integer> factorCounts = expressionExperimentService.getPopulatedFactorCountsExcludeBatch( ids );

        for ( ExpressionExperimentDetailsValueObject eeVo : vos ) {
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
     */
    @Override
    public void populateEventInformation( Collection<ExpressionExperimentDetailsValueObject> vos ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Long> ids = EntityUtils.getIds( vos );

        // do this ahead to avoid round trips - this also filters...
        Collection<ExpressionExperiment> ees = expressionExperimentService.load( ids );

        if ( ees.size() == 0 ) {
            return;
        }

        Map<Long, ExpressionExperiment> eeMap = EntityUtils.getIdMap( ees );
        Map<Long, Date> lastArrayDesignUpdates = expressionExperimentService.getLastArrayDesignUpdate( ees );
        Collection<Class<? extends AuditEventType>> typesToGet = Arrays.asList( eventTypes );

        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> events = this.getEvents( ees, typesToGet );

        Map<Auditable, AuditEvent> linkAnalysisEvents = events.get( LinkAnalysisEvent.class );
        Map<Auditable, AuditEvent> missingValueAnalysisEvents = events.get( MissingValueAnalysisEvent.class );
        Map<Auditable, AuditEvent> rankComputationEvents = events.get( ProcessedVectorComputationEvent.class );

        Map<Auditable, AuditEvent> differentialAnalysisEvents = events.get( DifferentialExpressionAnalysisEvent.class );
        Map<Auditable, AuditEvent> batchFetchEvents = events.get( BatchInformationFetchingEvent.class );
        Map<Auditable, AuditEvent> pcaAnalysisEvents = events.get( PCAAnalysisEvent.class );

        Map<Long, Collection<AuditEvent>> sampleRemovalEvents = this.getSampleRemovalEvents( ees );

        /*
         * add in the last events of interest for all eeVos This step is remarkably slow.
         */
        for ( ExpressionExperimentDetailsValueObject eeVo : vos ) {

            Long id = eeVo.getId();

            ExpressionExperiment ee = eeMap.get( id );

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

            if ( sampleRemovalEvents.containsKey( id ) ) {
                Collection<AuditEvent> removalEvents = sampleRemovalEvents.get( id );
                // we find we are getting lazy-load exceptions from this guy.
                eeVo.auditEvents2SampleRemovedFlags( removalEvents );

            }
        }

        if ( timer.getTime() > 1000 )
            log.info( "Retrieving audit events took " + timer.getTime() + "ms" );
    }

    @Override
    public void populateReportInformation( Collection<ExpressionExperimentDetailsValueObject> vos ) {
        StopWatch timer = new StopWatch();
        timer.start();

        List<Long> ids = new ArrayList<>();
        for ( ExpressionExperimentValueObject vo : vos ) {
            ids.add( vo.getId() );
        }

        Collection<ExpressionExperimentDetailsValueObject> cachedVos = this.retrieveSummaryObjects( ids );
        Map<Long, ExpressionExperimentDetailsValueObject> id2cachedVo = EntityUtils.getIdMap( cachedVos );

        for ( ExpressionExperimentDetailsValueObject eeVo : vos ) {
            ExpressionExperimentDetailsValueObject cacheVo = id2cachedVo.get( eeVo.getId() );
            if ( cacheVo != null ) {
                eeVo.setBioMaterialCount( cacheVo.getBioMaterialCount() );
                eeVo.setProcessedExpressionVectorCount( cacheVo.getProcessedExpressionVectorCount() );
                eeVo.setCoexpressionLinkCount( cacheVo.getCoexpressionLinkCount() );
                eeVo.setDateCached( cacheVo.getDateCached() );
                eeVo.setDifferentialExpressionAnalyses( cacheVo.getDifferentialExpressionAnalyses() );
                eeVo.setLastUpdated( cacheVo.getLastUpdated() );
            }
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( vos.size() + " EE reports fetched in " + timer.getTime() + "ms" );
        }

    }

    @Override
    public Collection<ExpressionExperimentDetailsValueObject> retrieveSummaryObjects( Collection<Long> ids ) {
        Collection<ExpressionExperimentDetailsValueObject> eeValueObjects = new ArrayList<>();
        Collection<Long> filteredIds = this.securityFilterExpressionExperimentIds( ids );

        int incache = 0;
        for ( Long id : filteredIds ) {

            Element cachedElement = this.statsCache.get( id );
            if ( cachedElement != null ) {
                incache++;
                Object el = cachedElement.getObjectValue();
                assert el instanceof ExpressionExperimentDetailsValueObject;

                eeValueObjects.add( ( ExpressionExperimentDetailsValueObject ) el );
                continue;
            }

            ExpressionExperimentDetailsValueObject valueObject = this.generateSummary( id );
            eeValueObjects.add( valueObject );
        }
        if ( ids.size() > 1 ) {
            log.info( incache + "/" + ids.size() + " reports were found in the cache" );
        }
        return eeValueObjects;
    }

    @Override
    @Secured({ "GROUP_AGENT" })
    public void recalculateBatchInfo() {
        log.info( "Started batch info recalculation task." );
        Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.HOUR_OF_DAY, -24 ); // All EEs updated in the last day

        Collection<ExpressionExperiment> ees = this.expressionExperimentService.findUpdatedAfter( calendar.getTime() );
        log.info( "Will be checking " + ees.size() + " experiments" );
        for ( ExpressionExperiment ee : ees ) {
            recalculateExperimentBatchInfo( ee );
        }
        log.info( "Finished batch info recalculation task." );
    }

    @Override
    @Secured({ "GROUP_AGENT" })
    public void recalculateExperimentBatchInfo( ExpressionExperiment ee ) {
        String confound = expressionExperimentService.getBatchConfound( ee );
        String effect = expressionExperimentService.getBatchEffectDescription( ee );
        boolean update = false;

        if ( !Objects.equals( confound, ee.getBatchConfound() ) ) {
            ee.setBatchConfound( confound );
            auditTrailService.addUpdateEvent( ee, BatchProblemsUpdateEvent.Factory.newInstance(),
                    ExpressionExperimentReportServiceImpl.NOTE_UPDATED_CONFOUND, confound );
            update = true;
        }

        if ( !Objects.equals( effect, ee.getBatchEffect() ) ) {
            auditTrailService.addUpdateEvent( ee, BatchProblemsUpdateEvent.Factory.newInstance(),
                    ExpressionExperimentReportServiceImpl.NOTE_UPDATED_EFFECT, effect );
            ee.setBatchEffect( effect );
            update = true;
        }

        if ( update ) {
            log.info( "New batch info for experiment " + ee.getShortName() + " id:" + ee.getId() );
            expressionExperimentService.update( ee );
        }
    }

    private Collection<ExpressionExperimentDetailsValueObject> generateSummaryObjects( Collection<Long> ids ) {

        Collection<ExpressionExperimentDetailsValueObject> vos = expressionExperimentService
                .loadDetailsValueObjects( null, false, ids, null, 0, 0 );
        this.getStats( vos );

        for ( ExpressionExperimentValueObject vo : vos ) {
            this.evictFromCache( vo.getId() );
            statsCache.put( new Element( vo.getId(), vo ) );
        }
        return vos;
    }

    private Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getEvents(
            Collection<ExpressionExperiment> ees, Collection<Class<? extends AuditEventType>> types ) {

        return auditEventService.getLastEvents( ees, types );

    }

    private Map<Long, Collection<AuditEvent>> getSampleRemovalEvents( Collection<ExpressionExperiment> ees ) {
        Map<Long, Collection<AuditEvent>> result = new HashMap<>();
        Map<ExpressionExperiment, Collection<AuditEvent>> rawr = expressionExperimentService
                .getSampleRemovalEvents( ees );
        for ( ExpressionExperiment e : rawr.keySet() ) {
            result.put( e.getId(), rawr.get( e ) );
        }
        return result;
    }

    /**
     * Compute statistics for EEs, that aren't immediately part of the value object.
     */
    private void getStats( Collection<ExpressionExperimentDetailsValueObject> vos ) {
        log.debug( "Getting stats for " + vos.size() + " value objects." );
        int count = 0;
        for ( ExpressionExperimentDetailsValueObject object : vos ) {
            this.getStats( object );

            if ( ++count % 10 == 0 ) {
                log.debug( "Processed " + count + " reports." );
            }
        }
        log.debug( "Done, processed " + count + " reports" );
    }

    /**
     * Get the stats report for one EE
     */
    private void getStats( ExpressionExperimentDetailsValueObject eeVo ) {
        Long id = eeVo.getId();
        assert id != null;

        Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> analysis = differentialExpressionAnalysisService
                .getAnalysesByExperiment( Collections.singleton( id ) );
        if ( analysis != null && analysis.containsKey( eeVo ) ) {
            eeVo.setDifferentialExpressionAnalyses( analysis.get( eeVo ) );
        }

        Date timestamp = new Date( System.currentTimeMillis() );
        eeVo.setDateCached( timestamp );
        assert eeVo.getLastUpdated() != null;

    }

    private Collection<Long> securityFilterExpressionExperimentIds( Collection<Long> ids ) {
        /*
         * Because this method returns the results, we have to screen.
         */
        Collection<ExpressionExperiment> securityScreened = expressionExperimentService.load( ids );

        Collection<Long> filteredIds = new HashSet<>();
        for ( ExpressionExperiment ee : securityScreened ) {
            filteredIds.add( ee.getId() );
        }
        return filteredIds;
    }

}
