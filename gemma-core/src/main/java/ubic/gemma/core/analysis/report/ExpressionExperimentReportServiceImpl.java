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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.experiment.BatchEffectType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectUtils.getBatchEffectStatistics;
import static ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectUtils.getBatchEffectType;

/**
 * Handles creation, serialization and/or marshaling of reports about expression experiments. Reports are stored in
 * ExpressionExperimentValueObjects.
 *
 * @author jsantos
 * @author paul
 * @author klc
 */
@Service("expressionExperimentReportService")
public class ExpressionExperimentReportServiceImpl implements ExpressionExperimentReportService, InitializingBean {

    private static final Log log = LogFactory.getLog( ExpressionExperimentReportServiceImpl.class );

    private static final String NOTE_UPDATED_CONFOUND = "Updated batch confound";
    private static final String NOTE_UPDATED_EFFECT = "Updated batch effect";
    private static final String EESTATS_CACHE_NAME = "ExpressionExperimentReportsCache";
    /**
     * Batch of classes we can get events for all at once.
     */
    private static final List<Class<? extends AuditEventType>> eventTypes = Arrays.asList(
            LinkAnalysisEvent.class, MissingValueAnalysisEvent.class, ProcessedVectorComputationEvent.class,
            DifferentialExpressionAnalysisEvent.class, BatchInformationFetchingEvent.class,
            PCAAnalysisEvent.class, BatchInformationMissingEvent.class );

    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

    /**
     * Cache to hold stats in memory. This is used to avoid hittinig the disk for reports too often.
     */
    private Cache statsCache;

    @Override
    public void afterPropertiesSet() {
        this.statsCache = requireNonNull( cacheManager.getCache( ExpressionExperimentReportServiceImpl.EESTATS_CACHE_NAME ) );
    }

    @Override
    public void evictFromCache( Long id ) {
        this.statsCache.evict( id );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentDetailsValueObject generateSummary( Long id ) {
        assert id != null;
        Collection<Long> ids = Collections.singletonList( id );
        Collection<ExpressionExperimentDetailsValueObject> vos = expressionExperimentService
                .loadDetailsValueObjectsByIds( ids );
        ExpressionExperimentDetailsValueObject vo = vos.iterator().next();
        if ( vo == null ) {
            return null;
        }
        this.getStats( vo );
        return vo;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Collection<ExpressionExperimentDetailsValueObject> generateSummaryObjects() {
        Collection<Long> ids = IdentifiableUtils.getIds( expressionExperimentService.loadAll() );
        Collection<ExpressionExperimentDetailsValueObject> vos = expressionExperimentService
                .loadDetailsValueObjectsByIds( ids );
        this.getStats( vos );
        return vos;
    }

    /**
     * Populate information about how many annotations there are, and how many factor values there are. Batch is not
     * counted towards the number of factors
     */
    @Override
    @Transactional(readOnly = true)
    public void getAnnotationInformation( Collection<ExpressionExperimentDetailsValueObject> vos ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Long> ids = new HashSet<>();
        for ( ExpressionExperimentDetailsValueObject eeVo : vos ) {
            Long id = eeVo.getId();
            ids.add( id );
        }

        Map<Long, Long> annotationCounts = expressionExperimentService.getAnnotationCountsByIds( ids );

        Map<Long, Long> factorCounts = expressionExperimentService.getPopulatedFactorCountsExcludeBatch( ids );

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
    @Transactional(readOnly = true)
    public void populateEventInformation( Collection<ExpressionExperimentDetailsValueObject> vos ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Long> ids = IdentifiableUtils.getIds( vos );

        // do this ahead to avoid round trips - this also filters...
        Collection<ExpressionExperiment> ees = expressionExperimentService.load( ids );

        if ( ees.isEmpty() ) {
            return;
        }

        Map<Long, ExpressionExperiment> eeMap = IdentifiableUtils.getIdMap( ees );
        Map<Long, Date> lastArrayDesignUpdates = expressionExperimentService.getLastArrayDesignUpdate( ees );

        Map<Class<? extends AuditEventType>, Map<ExpressionExperiment, AuditEvent>> events = auditEventService.getLastEvents( ees, eventTypes );

        Map<ExpressionExperiment, AuditEvent> linkAnalysisEvents = events.get( LinkAnalysisEvent.class );
        Map<ExpressionExperiment, AuditEvent> missingValueAnalysisEvents = events.get( MissingValueAnalysisEvent.class );
        Map<ExpressionExperiment, AuditEvent> rankComputationEvents = events.get( ProcessedVectorComputationEvent.class );

        Map<ExpressionExperiment, AuditEvent> differentialAnalysisEvents = events.get( DifferentialExpressionAnalysisEvent.class );
        Map<ExpressionExperiment, AuditEvent> batchFetchEvents = events.get( BatchInformationFetchingEvent.class );
        Map<ExpressionExperiment, AuditEvent> batchMissingEvents = events.get( BatchInformationMissingEvent.class );
        Map<ExpressionExperiment, AuditEvent> pcaAnalysisEvents = events.get( PCAAnalysisEvent.class );

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
                    if ( event.getEventType() != null ) {
                        eeVo.setLinkAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                    }
                }
            }

            if ( missingValueAnalysisEvents.containsKey( ee ) ) {
                AuditEvent event = missingValueAnalysisEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateMissingValueAnalysis( date );
                    if ( event.getEventType() != null ) {
                        eeVo.setMissingValueAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                    }
                }
            }

            if ( rankComputationEvents.containsKey( ee ) ) {
                AuditEvent event = rankComputationEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateProcessedDataVectorComputation( date );
                    if ( event.getEventType() != null ) {
                        eeVo.setProcessedDataVectorComputationEventType( event.getEventType().getClass().getSimpleName() );
                    }
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
                    if ( event.getEventType() != null ) {
                        eeVo.setPcaAnalysisEventType( event.getEventType().getClass().getSimpleName() );
                    }
                }
            }

            if ( batchFetchEvents.containsKey( ee ) ) {
                AuditEvent event = batchFetchEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateBatchFetch( date );
                    if ( event.getEventType() != null ) {
                        eeVo.setBatchFetchEventType( event.getEventType().getClass().getSimpleName() );
                    }
                }
            } else if ( batchMissingEvents.containsKey( ee ) ) { // we use date.
                AuditEvent event = batchMissingEvents.get( ee );
                if ( event != null ) {
                    Date date = event.getDate();
                    eeVo.setDateBatchFetch( date );
                    if ( event.getEventType() != null ) {
                        eeVo.setBatchFetchEventType( event.getEventType().getClass().getSimpleName() );
                    }
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
    @Transactional(readOnly = true)
    public void populateReportInformation( Collection<ExpressionExperimentDetailsValueObject> vos ) {
        StopWatch timer = new StopWatch();
        timer.start();

        List<Long> ids = new ArrayList<>();
        for ( ExpressionExperimentValueObject vo : vos ) {
            ids.add( vo.getId() );
        }

        Collection<ExpressionExperimentDetailsValueObject> cachedVos = this.retrieveSummaryObjects( ids );
        Map<Long, ExpressionExperimentDetailsValueObject> id2cachedVo = IdentifiableUtils.getIdMap( cachedVos );

        for ( ExpressionExperimentDetailsValueObject eeVo : vos ) {
            ExpressionExperimentDetailsValueObject cacheVo = id2cachedVo.get( eeVo.getId() );
            if ( cacheVo != null ) {
                eeVo.setBioMaterialCount( cacheVo.getBioMaterialCount() );
                eeVo.setProcessedExpressionVectorCount( cacheVo.getProcessedExpressionVectorCount() );
                // eeVo.setCoexpressionLinkCount( cacheVo.getCoexpressionLinkCount() ); // not used
                eeVo.setHasCoexpressionAnalysis( cacheVo.getHasCoexpressionAnalysis() );
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
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentDetailsValueObject> retrieveSummaryObjects( Collection<Long> ids ) {
        Collection<ExpressionExperimentDetailsValueObject> eeValueObjects = new ArrayList<>();
        Collection<Long> filteredIds = this.securityFilterExpressionExperimentIds( ids );

        int incache = 0;
        for ( Long id : filteredIds ) {

            Cache.ValueWrapper cachedElement = this.statsCache.get( id );
            if ( cachedElement != null ) {
                incache++;
                Object el = cachedElement.get();
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
    @Transactional
    public void recalculateExperimentBatchInfo( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thaw( ee );
        BatchEffectDetails details = expressionExperimentBatchInformationService.getBatchEffectDetails( ee );
        BatchEffectType effect = getBatchEffectType( details );
        String effectStatistics = getBatchEffectStatistics( details );
        String effectSummary = effectStatistics != null ? effectStatistics : effect.name();
        String confound = expressionExperimentBatchInformationService.getBatchConfoundAsHtmlString( ee );
        String confoundSummary = confound != null ? confound : escapeHtml4( "<no confound>" );

        if ( !Objects.equals( confound, ee.getBatchConfound() ) ) {
            ee.setBatchConfound( confound );
            auditTrailService.addUpdateEvent( ee, BatchProblemsUpdateEvent.class,
                    ExpressionExperimentReportServiceImpl.NOTE_UPDATED_CONFOUND, confoundSummary );
            log.info( "New batch confound for " + ee + ": " + confoundSummary );
        }

        if ( !Objects.equals( effect, ee.getBatchEffect() ) || !Objects.equals( effectStatistics, ee.getBatchEffectStatistics() ) ) {
            ee.setBatchEffect( effect );
            ee.setBatchEffectStatistics( effectStatistics );
            auditTrailService.addUpdateEvent( ee, BatchProblemsUpdateEvent.class,
                    ExpressionExperimentReportServiceImpl.NOTE_UPDATED_EFFECT, effectSummary );
            log.info( "New batch effect for " + ee + ": " + effectSummary );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentDataProcessingReport generateDataProcessingReport( ExpressionExperiment ee ) {
        Map<Class<? extends AuditEventType>, AuditEvent> latestEvents = auditEventService.getLastEvents( ee, Arrays.asList(
                SingleCellSubSetsCreatedEvent.class,
                ProcessedVectorComputationEvent.class ) );
        List<AuditEvent> dataAddedEvents = auditEventService.getEvents( ee, DataAddedEvent.class );

        // FIXME
        ExpressionExperimentDataProcessingReport.Details odd = null;
        ExpressionExperimentDataProcessingReport.Details ad = null;
        for ( int i = dataAddedEvents.size() - 1; i >= 0; i-- ) {
            AuditEvent e = dataAddedEvents.get( i );
            if ( e.getEventType() instanceof SingleCellDataAddedEvent ) {
                //
                odd = new ExpressionExperimentDataProcessingReport.Details( e.getDetail(), e.getDate() );
            } else if ( e.getEventType() instanceof AggregatedSingleDataAddedEvent ) {
                ad = new ExpressionExperimentDataProcessingReport.Details( e.getDetail(), e.getDate() );
            } else if ( odd == null && e.getNote() != null && e.getNote().matches( "Added \\d+ vectors for %s with dimension .+\\." ) ) {
                odd = new ExpressionExperimentDataProcessingReport.Details( e.getDetail(), e.getDate() );
            } else if ( ad == null && e.getNote() != null && e.getNote().matches( "Created \\d+ aggregated raw vectors for .+\\." ) ) {
                ad = new ExpressionExperimentDataProcessingReport.Details( e.getDetail(), e.getDate() );
            } else {
                log.info( e.getNote() );
            }
        }

        ExpressionExperimentDataProcessingReport.Details sd;
        if ( latestEvents.containsKey( SingleCellSubSetsCreatedEvent.class ) ) {
            AuditEvent se = latestEvents.get( SingleCellSubSetsCreatedEvent.class );
            sd = new ExpressionExperimentDataProcessingReport.Details( se.getDetail(), se.getDate() );
        } else {
            sd = null;
        }
        ExpressionExperimentDataProcessingReport.Details pd;
        if ( latestEvents.containsKey( ProcessedVectorComputationEvent.class ) ) {
            AuditEvent pe = latestEvents.get( ProcessedVectorComputationEvent.class );
            pd = new ExpressionExperimentDataProcessingReport.Details( pe.getDetail(), pe.getDate() );
        } else {
            pd = null;
        }
        return ExpressionExperimentDataProcessingReport.builder()
                .originalData( odd )
                .subsetting( sd )
                .aggregation( ad )
                .preprocessing( pd )
                .build();
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

        Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> analysis = differentialExpressionAnalysisService
                .getAnalysesByExperiment( Collections.singleton( id ) );
        if ( analysis != null && analysis.containsKey( eeVo ) ) {
            eeVo.setDifferentialExpressionAnalyses( analysis.get( eeVo ) );
        }

        Date timestamp = new Date( System.currentTimeMillis() );
        eeVo.setDateCached( timestamp );

        // update cached detailed VOs
        this.evictFromCache( eeVo.getId() );
        statsCache.put( eeVo.getId(), eeVo );
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
