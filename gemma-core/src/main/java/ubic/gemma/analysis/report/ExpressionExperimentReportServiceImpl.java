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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import ubic.basecode.util.FileTools;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.AutomatedAnnotationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedAnnotations;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.EntityUtils;

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
    private static final double CUT_OFF = 0.05;
    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;
    private String EE_LINK_SUMMARY = "AllExpressionLinkSummary";
    private String EE_REPORT_DIR = "ExpressionExperimentReports";

    private String EESTATS_CACHE_NAME = "ExpressionExperimentReportsCache";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;

    @Autowired
    private SecurityService securityService;

    /**
     * Batch of classes we can get events for all at once.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends AuditEventType>[] eventTypes = new Class[] { LinkAnalysisEvent.class,
            MissingValueAnalysisEvent.class, ProcessedVectorComputationEvent.class, ValidatedAnnotations.class,
            DifferentialExpressionAnalysisEvent.class, AutomatedAnnotationEvent.class,
            BatchInformationFetchingEvent.class, PCAAnalysisEvent.class };

    /**
     * Cache to hold stats in memory. This is used to avoid hittinig the disk for reports too often.
     */
    private Cache statsCache;

    public void afterPropertiesSet() throws Exception {
        /*
         * Initialize the cache; if it already exists it will not be recreated.
         */
        boolean terracottaEnabled = ConfigUtils.getBoolean( "gemma.cache.clustered", false );
        boolean diskPersistent = ConfigUtils.getBoolean( "gemma.cache.diskpersistent", false ) && !terracottaEnabled;
        int maxElements = 5000;
        boolean eternal = true;
        boolean overFlowToDisk = false;
        int diskExpiryThreadIntervalSeconds = 600;
        int maxElementsOnDisk = 10000;
        boolean terracottaCoherentReads = false;
        boolean clearOnFlush = false;

        if ( terracottaEnabled ) {
            CacheConfiguration config = new CacheConfiguration( EESTATS_CACHE_NAME, maxElements );
            config.setStatistics( false );
            config.setMemoryStoreEvictionPolicy( MemoryStoreEvictionPolicy.LRU.toString() );
            config.setOverflowToDisk( overFlowToDisk );
            config.setEternal( eternal );
            config.setTimeToIdleSeconds( 0 );
            config.setMaxElementsOnDisk( maxElementsOnDisk );
            config.addTerracotta( new TerracottaConfiguration() );
            config.getTerracottaConfiguration().setCoherentReads( terracottaCoherentReads );
            config.clearOnFlush( clearOnFlush );
            config.setTimeToLiveSeconds( 0 );
            config.getTerracottaConfiguration().setClustered( terracottaEnabled );
            config.getTerracottaConfiguration().setValueMode( "SERIALIZATION" );
            config.getTerracottaConfiguration().addNonstop( new NonstopConfiguration() );
            this.statsCache = new Cache( config );

            // this.statsCache = new Cache( EESTATS_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU,
            // overFlowToDisk, null, eternal, 0, 0, diskPersistent, diskExpiryThreadIntervalSeconds, null, null,
            // maxElementsOnDisk, 10, clearOnFlush, terracottaEnabled, "SERIALIZATION", terracottaCoherentReads );
        } else {
            this.statsCache = new Cache( EESTATS_CACHE_NAME, maxElements, MemoryStoreEvictionPolicy.LRU,
                    overFlowToDisk, null, eternal, 0, 0, diskPersistent, diskExpiryThreadIntervalSeconds, null );
        }

        cacheManager.addCache( statsCache );
        this.statsCache = cacheManager.getCache( EESTATS_CACHE_NAME );

    }

    /**
     * Populate information about how many annotations there are, and how many factor values there are. Batch is not
     * counted towards the number of factors
     * 
     * @param vos
     */
    public void fillAnnotationInformation( Collection<ExpressionExperimentValueObject> vos ) {

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
    public Map<Long, Date> fillEventInformation( Collection<ExpressionExperimentValueObject> vos ) {

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

        Collection<Long> validatedEEs = getValidated( ees );

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
            //
            // if ( validatedAnnotationEvents.containsKey( ee ) ) {
            // AuditEvent validated = validationEvents.get( ee );
            //
            // if ( validated != null ) {
            //
            // // auditEventService.thaw( validated );
            //
            // if ( validated.getDate().after( mostRecentDate ) ) {
            // mostRecentDate = validated.getDate();
            // }
            //
            // eeVo.setValidatedAnnotations( new AuditEventValueObject( validated ) );
            // }
            // }

            if ( autotaggerEvents.containsKey( ee ) ) {
                AuditEvent taggerEvent = autotaggerEvents.get( ee );
                // auditEventService.thaw( taggerEvent );

                if ( taggerEvent.getDate().after( mostRecentDate ) ) {
                    mostRecentDate = taggerEvent.getDate();
                }

                eeVo.setAutoTagDate( taggerEvent.getDate() );
            }

            if ( privacyInfo.containsKey( ee ) ) {
                eeVo.setIsPublic( !privacyInfo.get( ee ) );
            }

            if ( sharingInfo.containsKey( ee ) ) {
                eeVo.setShared( sharingInfo.get( ee ) );
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
                eeVo.setTroubled( true );
            }

            if ( validatedEEs.contains( id ) ) {
                eeVo.setValidated( true );
            }

            if ( mostRecentDate.after( new Date( 0 ) ) ) results.put( ee.getId(), mostRecentDate );
        }

        if ( timer.getTime() > 1000 ) log.info( "Retrieving audit events took " + timer.getTime() + "ms" );

        return results;
    }

    private Collection<Long> getValidated( Collection<ExpressionExperiment> ees ) {
        Collection<Long> result = new HashSet<Long>();
        for ( ExpressionExperiment ee : ees ) {
            if ( ee.getStatus().getValidated() ) {
                result.add( ee.getId() );
            }
        }
        return result;
    }

    private Collection<Long> getTroubled( Collection<ExpressionExperiment> ees ) {
        Collection<Long> ids = EntityUtils.getIds( ees );
        Collection<Long> untroubled = expressionExperimentService.getUntroubled( ids );
        ids.removeAll( untroubled );
        return ids;
    }

    /**
     * Fills in link analysis and other info from the report.
     * 
     * @return map of when the objects were most recently updated (or created)
     */
    public Map<Long, Date> fillReportInformation( Collection<ExpressionExperimentValueObject> vos ) {
        StopWatch timer = new StopWatch();
        Map<Long, Date> result = new HashMap<Long, Date>();
        timer.start();

        List<Long> ids = new ArrayList<Long>();
        for ( ExpressionExperimentValueObject vo : vos ) {
            ids.add( vo.getId() );
        }

        Collection<ExpressionExperimentValueObject> cachedVos = retrieveValueObjects( ids );
        Map<Long, ExpressionExperimentValueObject> id2cachedVo = new HashMap<Long, ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject cachedVo : cachedVos ) {
            id2cachedVo.put( cachedVo.getId(), cachedVo );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Link stats read from cache in " + timer.getTime() + "ms" );
        }
        timer.reset();
        timer.start();

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
            log.info( "Link stats processed from cache in " + timer.getTime() + "ms" );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ExpressionExperimentReportService#generateSummaryObject(java.lang.Long)
     */
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
    @Secured({ "GROUP_AGENT" })
    public void generateSummaryObjects() {
        initDirectories( false );
        Collection<Long> ids = EntityUtils.getIds( expressionExperimentService.loadAll() );
        Collection<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjects( ids );
        updateStats( vos );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ExpressionExperimentReportService#generateSummaryObjects(java.util.Collection)
     */
    public Collection<ExpressionExperimentValueObject> generateSummaryObjects( Collection<Long> ids ) {
        initDirectories( false );

        Collection<Long> filteredIds = securityFilterExpressionExperimentIds( ids );
        Collection<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjects( filteredIds );
        updateStats( vos );
        return vos;
    }

    /**
     * @param eeid
     * @param threshold
     * @return A collection of DifferentialExpressionAnalysisValueObjects
     */
    public Collection<DifferentialExpressionAnalysisValueObject> getDiffExpressedProbes( ExpressionExperiment ee,
            double threshold ) {

        Collection<DifferentialExpressionAnalysisValueObject> summaries = new HashSet<DifferentialExpressionAnalysisValueObject>();
        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService.getAnalyses( ee );

        for ( DifferentialExpressionAnalysis analysis : analyses ) {

            differentialExpressionAnalysisService.thaw( analysis );
            Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();

            DifferentialExpressionAnalysisValueObject avo = new DifferentialExpressionAnalysisValueObject( analysis );

            if ( analysis.getSubsetFactorValue() != null )
                avo.setSubsetFactorValue( new FactorValueValueObject( analysis.getSubsetFactorValue() ) );

            for ( ExpressionAnalysisResultSet par : results ) {

                DifferentialExpressionSummaryValueObject desvo = new DifferentialExpressionSummaryValueObject();
                differentialExpressionResultService.thawLite( par ); // need the thaw for the experimental factor
                desvo.setThreshold( threshold );
                desvo.setExperimentalFactors( par.getExperimentalFactors() );
                desvo.setResultSetId( par.getId() );
                desvo.setAnalysisId( analysis.getId() );
                desvo.setFactorIds( EntityUtils.getIds( par.getExperimentalFactors() ) );

                if ( par.getBaselineGroup() != null ) {
                    desvo.setBaselineGroup( new FactorValueValueObject( par.getBaselineGroup() ) );
                }

                populateHitListSizes( threshold, par, desvo );

                avo.getResultSets().add( desvo );

            }

            summaries.add( avo );
        }
        return summaries;
    }

    /**
     * @param threshold
     * @param par
     * @param desvo
     * @deprecated since the counts are available in the ResultSet->HitListSize.
     */
    private void populateHitListSizes( double threshold, ExpressionAnalysisResultSet par,
            DifferentialExpressionSummaryValueObject desvo ) {

        Integer probesThatMetThreshold = differentialExpressionAnalysisService.countProbesMeetingThreshold( par,
                threshold );
        desvo.setNumberOfDiffExpressedProbes( probesThatMetThreshold );
        Integer upregulatedCount = differentialExpressionAnalysisService.countUpregulated( par, threshold );
        Integer downregulatedCount = differentialExpressionAnalysisService.countDownregulated( par, threshold );

        desvo.setUpregulatedCount( upregulatedCount );
        desvo.setDownregulatedCount( downregulatedCount );

        log.debug( "Probes that met threshold in result set - " + par.getId() + " : " + probesThatMetThreshold );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.report.ExpressionExperimentReportService#retrieveSummaryObjects(java.util.Collection)
     */
    public Collection<ExpressionExperimentValueObject> retrieveSummaryObjects( Collection<Long> ids ) {
        return retrieveValueObjects( ids );
    }

    public void setAuditEventService( AuditEventService auditEventService ) {
        this.auditEventService = auditEventService;
    }

    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    public void setDifferentialExpressionResultService(
            DifferentialExpressionResultService differentialExpressionResultService ) {
        this.differentialExpressionResultService = differentialExpressionResultService;
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

    /**
     * Read information about troubled events for the set of given EEs. These are read from the audit trail.
     * 
     * @param ees
     * @param type
     * @return
     * @deprecated use getStatus().getTroubled() instead.
     */
    @Override
    @Deprecated
    public Map<Long, AuditEvent> getTroubledEvents( Collection<ExpressionExperiment> ees ) {
        return getEvents( ees, TroubleStatusFlagEvent.class );
    }

    /**
     * Read information about an particular type of event for the set of given EEs. These are read from the audit trail.
     * 
     * @param ees
     * @param type
     * @return
     * @deprecated not needed
     */
    @Deprecated
    private Map<Long, AuditEvent> getEvents( Collection<ExpressionExperiment> ees, Class<? extends AuditEventType> type ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Map<Long, AuditEvent> result = new HashMap<Long, AuditEvent>();

        Map<? extends Auditable, AuditEvent> events = null;

        events = auditEventService.getLastEvent( ees, type );

        for ( Auditable a : events.keySet() ) {
            result.put( ( ( ExpressionExperiment ) a ).getId(), events.get( a ) );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Retrieved " + type.getSimpleName() + " in " + timer.getTime() + "ms" );
        }
        return result;
    }

    private Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getEvents(
            Collection<ExpressionExperiment> ees, Collection<Class<? extends AuditEventType>> types ) {

        return auditEventService.getLastEvents( ees, types );

    }

    /**
     * @param id
     * @return
     */
    private String getReportPath( long id ) {
        return HOME_DIR + File.separatorChar + EE_REPORT_DIR + File.separatorChar + EE_LINK_SUMMARY + "." + id;
    }

    // Methods needed to allow this to be used in a space.

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
     * Check to see if the top level report storage directory exists. If it doesn't, create it, Check to see if the
     * reports directory exists. If it doesn't, create it.
     * 
     * @param deleteFiles
     */
    private void initDirectories( boolean deleteFiles ) {

        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        File f = new File( HOME_DIR + File.separatorChar + EE_REPORT_DIR );

        // clear out all files
        if ( deleteFiles ) {
            Collection<File> files = new ArrayList<File>();
            File[] fileArray = f.listFiles();
            for ( File file : fileArray ) {
                files.add( file );
            }
            FileTools.deleteFiles( files );
        }
    }

    /**
     * @return the serialized value objects (either from the disk store or from the in-memory cache).
     */
    private Collection<ExpressionExperimentValueObject> retrieveValueObjects( Collection<Long> ids ) {
        Collection<ExpressionExperimentValueObject> eeValueObjects = new ArrayList<ExpressionExperimentValueObject>();
        Collection<Long> filteredIds = securityFilterExpressionExperimentIds( ids );

        int numWarnings = 0;
        int maxWarnings = 5; // don't put 1000 warnings in the logs!

        for ( Long id : filteredIds ) {

            Element cachedElement = this.statsCache.get( id );
            if ( cachedElement != null ) {
                eeValueObjects.add( ( ExpressionExperimentValueObject ) cachedElement.getValue() );
                continue;
            }

            File f = new File( getReportPath( id ) );

            if ( !f.exists() ) {
                continue;
            }

            try {

                FileInputStream fis = new FileInputStream( getReportPath( id ) );
                ObjectInputStream ois = new ObjectInputStream( fis );

                ExpressionExperimentValueObject valueObject = ( ExpressionExperimentValueObject ) ois.readObject();

                eeValueObjects.add( valueObject );
                statsCache.put( new Element( id, valueObject ) );

                ois.close();
                fis.close();

            } catch ( Exception e ) {
                if ( numWarnings < maxWarnings ) {
                    log.warn( "Unable to read report object for id =" + id + ": " + e.getMessage() );
                } else if ( numWarnings == maxWarnings ) {
                    log.warn( "Skipping futher warnings ... reports need refreshing?" );
                }
                numWarnings++;
                continue;
            }
        }
        return eeValueObjects;
    }

    /**
     * Save the stats report to disk; also update the in-memory cache.
     * 
     * @param eeVo Expression Experiment value objects to serialize
     */
    private void saveValueObject( ExpressionExperimentValueObject eeVo ) {
        try {
            // remove old file first
            File f = new File( getReportPath( eeVo.getId() ) );
            if ( f.exists() ) {
                f.delete();
            }
            FileOutputStream fos = new FileOutputStream( getReportPath( eeVo.getId() ) );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( eeVo );
            oos.flush();
            oos.close();

            statsCache.put( new Element( eeVo.getId(), eeVo ) );
        } catch ( Throwable e ) {
            log.warn( e );
        }
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

    /**
     * Compute statistics for EEs and serialize them to disk for later retrieval.
     * 
     * @param vos
     */
    private void updateStats( Collection<ExpressionExperimentValueObject> vos ) {
        log.info( "Getting stats for " + vos.size() + " value objects." );
        int count = 0;
        for ( ExpressionExperimentValueObject object : vos ) {
            updateStats( object );
            if ( ++count % 10 == 0 ) {
                log.info( "Processed " + count + " reports." );
            }
        }
        log.info( "Done, processed " + count + " reports" );
    }

    /**
     * Update the stats report for one EE
     * 
     * @param object
     */
    private void updateStats( ExpressionExperimentValueObject eeVo ) {
        assert eeVo.getId() != null;
        ExpressionExperiment tempEe = expressionExperimentService.load( eeVo.getId() );

        eeVo.setBioMaterialCount( expressionExperimentService.getBioMaterialCount( tempEe ) );
        eeVo.setProcessedExpressionVectorCount( expressionExperimentService.getProcessedExpressionVectorCount( tempEe ) );

        eeVo.setDifferentialExpressionAnalyses( getDiffExpressedProbes( tempEe, CUT_OFF ) );

        Integer numLinks = probe2ProbeCoexpressionService.countLinks( tempEe );
        log.debug( numLinks + " links." );
        eeVo.setCoexpressionLinkCount( numLinks );

        Date timestamp = new Date( System.currentTimeMillis() );
        eeVo.setDateCached( timestamp );
        eeVo.setDateCreated( tempEe.getStatus().getCreateDate() );
        eeVo.setDateLastUpdated( tempEe.getStatus().getLastUpdateDate() );

        saveValueObject( eeVo );

        log.debug( "Generated report for " + eeVo.getShortName() );
    }

}
