package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.AuditedOnError;
import ubic.gemma.core.security.audit.payload.ProcessedVectorComputationPayload;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.VectorsReorderedEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.Slice;

import org.springframework.lang.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Paul
 */
@Service
@Slf4j
public class ProcessedExpressionDataVectorServiceImpl
        extends AbstractBulkExpressionDataVectorService<ProcessedExpressionDataVector>
        implements ProcessedExpressionDataVectorService {

    private static final int DIFFEX_MIN_NUMBER_OF_RESULTS = 50;

    private final ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @Autowired
    private GeneService geneService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;
    @Autowired
    private ProcessedExpressionDataVectorHelperService helperService;
    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private CachedProcessedExpressionDataVectorService cachedProcessedExpressionDataVectorService;
    @Autowired
    private ProcessedExpressionDataVectorCreationHelperService processedExpressionDataVectorCreationHelperService;
    @Autowired
    private ProcessedExpressionDataVectorAuditService processedVectorAuditService;

    @Autowired
    protected ProcessedExpressionDataVectorServiceImpl( ProcessedExpressionDataVectorDao mainDao ) {
        super( mainDao );
        this.processedExpressionDataVectorDao = mainDao;
    }

    @Override
    @Transactional(rollbackFor = { QuantitationTypeConversionException.class })
    public QuantitationType createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean updateRanks ) throws QuantitationTypeConversionException {
        try {
            return createProcessedDataVectors( expressionExperiment, updateRanks, true );
        } catch ( QuantitationTypeDetectionException e ) {
            // never happening
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional(rollbackFor = { QuantitationTypeDetectionException.class, QuantitationTypeConversionException.class })
    @AuditedOnError(value = FailedProcessedVectorComputationEvent.class, message = "Failed to create processed expression data vectors.")
    public QuantitationType createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean updateRanks, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        QuantitationType qt;
        ProcessedExpressionDataVectorCreationSummary summary = new ProcessedExpressionDataVectorCreationSummary();
        qt = this.processedExpressionDataVectorCreationHelperService.createProcessedDataVectors( expressionExperiment, ignoreQuantitationMismatch, summary );
        // Phase C bucket 2f: typed payload via the AuditedAspect. The audit row
        // is emitted by the @Audited annotation on
        // ProcessedExpressionDataVectorAuditService#recordProcessedVectorComputation
        // — calling through a co-bean is required because Spring AOP cannot
        // intercept self-invocations on this class.
        ProcessedVectorComputationPayload payload = new ProcessedVectorComputationPayload(
                summary.getRawQuantitationType() != null ? summary.getRawQuantitationType().toString() : null,
                qt != null ? qt.toString() : null,
                summary.getNumberOfMaskedMissingValues(),
                summary.getNumberOfMaskedOutliers(),
                summary.isQuantileNormalized(),
                StringUtils.isNotBlank( summary.getComment() ) ? summary.getComment() : null );
        processedVectorAuditService.recordProcessedVectorComputation( expressionExperiment, payload );
        if ( updateRanks ) {
            updateRanks( expressionExperiment );
        }
        // cached vectors are no-longer valid
        cachedProcessedExpressionDataVectorService.evict( expressionExperiment );
        return qt;
    }

    @Override
    @Transactional
    @Audited(value = ProcessedVectorComputationEvent.class, message = "Replaced processed expression data.")
    @AuditedOnError(value = FailedProcessedVectorComputationEvent.class, message = "Failed to replace processed expression data vectors.")
    public int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors, boolean updateRanks ) {
        // Success audit event written by @Audited on this method via AuditedAspect.
        // Failure audit event written by @AuditedOnError (REQUIRES_NEW, stack trace in DETAIL).
        int replaced = expressionExperimentService.replaceProcessedDataVectors( ee, vectors );
        if ( updateRanks ) {
            updateRanks( ee );
        }
        cachedProcessedExpressionDataVectorService.evict( ee );
        return replaced;
    }

    @Override
    @Transactional
    @Audited(value = ProcessedVectorComputationEvent.class, message = "Removed processed expression data.")
    @AuditedOnError(value = FailedProcessedVectorComputationEvent.class, message = "Failed to remove processed expression data vectors.")
    public int removeProcessedDataVectors( ExpressionExperiment ee ) {
        // Success audit event written by @Audited on this method via AuditedAspect.
        // Failure audit event written by @AuditedOnError (REQUIRES_NEW, stack trace in DETAIL).
        int removed = expressionExperimentService.removeProcessedDataVectors( ee );
        cachedProcessedExpressionDataVectorService.evict( ee );
        return removed;
    }

    @Override
    @Transactional
    @Audited(value = VectorsReorderedEvent.class, message = "Reordered the data vectors by experimental design")
    public void reorderByDesign( ExpressionExperiment ee ) {
        this.helperService.reorderByDesign( ee );
        cachedProcessedExpressionDataVectorService.evict( ee );
    }

    @Override
    @Transactional
    @AuditedOnError(value = FailedProcessedVectorComputationEvent.class, message = "Failed to update ranks for expression data vectors.")
    public void updateRanks( ExpressionExperiment ee ) {
        helperService.updateRanks( ee );
        cachedProcessedExpressionDataVectorService.evict( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentExpressionLevelsValueObject> getExpressionLevels( Collection<ExpressionExperiment> ees,
            Collection<Gene> genes, boolean keepGeneNonSpecific, @Nullable String consolidateMode ) {
        Map<Long, List<DoubleVectorValueObject>> vectorsByExperiment = cachedProcessedExpressionDataVectorService.getProcessedDataArrays( ees, IdentifiableUtils.getIds( genes ) )
                .stream()
                .collect( Collectors.groupingBy( vector -> vector.getExpressionExperiment().getId(), Collectors.toList() ) );
        List<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>( ees.size() );
        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene = new HashMap<>();
            List<DoubleVectorValueObject> vectors = vectorsByExperiment.get( ee.getId() );
            if ( vectors == null ) {
                continue;
            }
            for ( DoubleVectorValueObject v : vectors ) {
                if ( v.getGenes() == null || v.getGenes().isEmpty() ) {
                    continue;
                }
                for ( Gene g : genes ) {
                    if ( v.getGenes().contains( g.getId() ) ) {
                        if ( !vectorsPerGene.containsKey( g ) ) {
                            vectorsPerGene.put( g, new LinkedList<>() );
                        }
                        vectorsPerGene.get( g ).add( v );
                    }
                }
            }
            vos.add( new ExperimentExpressionLevelsValueObject( ee.getId(), vectorsPerGene, keepGeneNonSpecific,
                    consolidateMode ) );
        }

        return vos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentExpressionLevelsValueObject> getExpressionLevelsByIds( Collection<Long> eeIds, Collection<Gene> genes, boolean keepGeneNonSpecific, @Nullable String consolidateMode ) {
        return getExpressionLevels( expressionExperimentService.loadReferences( eeIds ), genes, keepGeneNonSpecific, consolidateMode );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentExpressionLevelsValueObject> getExpressionLevelsPca(
            Collection<ExpressionExperiment> ees, int limit, int component, boolean keepGeneNonSpecific,
            @Nullable String consolidateMode ) {
        List<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>( ees.size() );

        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            Collection<DoubleVectorValueObject> vectors = svdService.getTopLoadedVectors( ee, component, limit )
                    .values();
            this.addExperimentGeneVectors( vos, ee, vectors, keepGeneNonSpecific, consolidateMode );
        }

        return vos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentExpressionLevelsValueObject> getExpressionLevelsDiffEx(
            Collection<ExpressionExperiment> ees, Long diffExResultSetId, double threshold, int max,
            boolean keepGeneNonSpecific, @Nullable String consolidateMode ) {
        List<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>( ees.size() );

        // Compute the diff-ex stats map once (probeId -> DEVO) so we can enrich the per-gene VO without
        // a second round-trip; this is the same data the endpoint already used to rank its top-N.
        Map<Long, DifferentialExpressionValueObject> statsByProbeId = this.getDiffExStatsByProbeId( diffExResultSetId, threshold, max );

        // The vector retrieval is loop-invariant — the result set fixes the analyzed BioAssaySet, so the
        // vectors are identical on every iteration. Hoist out of the loop to avoid re-thawing the result
        // set, re-running findByResultSet, and re-fetching the DEDV vectors N times.
        Collection<DoubleVectorValueObject> vectors = this.getDiffExVectors( diffExResultSetId, threshold, max );

        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            this.addExperimentGeneVectorsWithDiffExStats( vos, ee, vectors, keepGeneNonSpecific, consolidateMode, statsByProbeId );
        }

        return vos;
    }

    /**
     * Mirror of the per-probe top-hits query used by {@link #getDiffExVectors}, but returning the DEVOs keyed
     * by probe id so callers can pull corrected p-value / log2-fold-change for the contrast represented by
     * the result-set.
     */
    private Map<Long, DifferentialExpressionValueObject> getDiffExStatsByProbeId( Long resultSetId, double threshold, int max ) {
        ExpressionAnalysisResultSet ar = expressionAnalysisResultSetService.load( resultSetId );
        if ( ar == null ) {
            return Collections.emptyMap();
        }
        ar = expressionAnalysisResultSetService.thaw( ar );
        List<DifferentialExpressionValueObject> rows = differentialExpressionResultService
                .findByResultSet( ar, threshold, max, DIFFEX_MIN_NUMBER_OF_RESULTS );
        Map<Long, DifferentialExpressionValueObject> byProbe = new HashMap<>( rows.size() );
        for ( DifferentialExpressionValueObject r : rows ) {
            // If the same probe appears twice (shouldn't happen for a single result set), keep the more-
            // significant row — same tie-break the endpoint uses for ranking.
            DifferentialExpressionValueObject prev = byProbe.get( r.getProbeId() );
            if ( prev == null || isMoreSignificant( r, prev ) ) {
                byProbe.put( r.getProbeId(), r );
            }
        }
        return byProbe;
    }

    private static boolean isMoreSignificant( DifferentialExpressionValueObject a, DifferentialExpressionValueObject b ) {
        Double ac = a.getCorrP();
        Double bc = b.getCorrP();
        if ( ac == null ) return false;
        if ( bc == null ) return true;
        return ac < bc;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( Collection<ExpressionExperiment> expressionExperiments, Collection<Long> genes ) {
        return cachedProcessedExpressionDataVectorService.getProcessedDataArrays( expressionExperiments, genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet bioAssaySet, Collection<Long> genes ) {
        return cachedProcessedExpressionDataVectorService.getProcessedDataArrays( bioAssaySet, genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment ) {
        return this.cachedProcessedExpressionDataVectorService.getProcessedDataArrays( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getRandomProcessedDataArrays( ExpressionExperiment ee, int limit ) {
        return this.cachedProcessedExpressionDataVectorService.getRandomProcessedDataArrays( ee, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( ExpressionExperiment ee, Collection<CompositeSequence> compositeSequences ) {
        return cachedProcessedExpressionDataVectorService.getProcessedDataArraysByProbe( ee, compositeSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( Collection<ExpressionExperiment> expressionExperiments, Collection<CompositeSequence> compositeSequences ) {
        return cachedProcessedExpressionDataVectorService.getProcessedDataArraysByProbe( expressionExperiments, compositeSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors(
            ExpressionExperiment expressionExperiment ) {
        return this.processedExpressionDataVectorDao.getProcessedVectors( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, int offset, int limit ) {
        return new Slice<>( this.processedExpressionDataVectorDao.getProcessedVectors( expressionExperiment, dimension, offset, limit ), null, offset, limit, null );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<CompositeSequence> getProcessedDataVectorsDesignElements( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, int offset, int limit ) {
        return new Slice<>( this.processedExpressionDataVectorDao.getProcessedVectorsDesignElements( expressionExperiment, dimension, offset, limit ), null, offset, limit, null );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectorsAndThaw( ExpressionExperiment expressionExperiment ) {
        Collection<ProcessedExpressionDataVector> vectors = this.processedExpressionDataVectorDao.getProcessedVectors( expressionExperiment );
        processedExpressionDataVectorDao.thaw( vectors );
        return vectors;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiments, genes, method );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoubleVectorValueObject> getDiffExVectors( Long resultSetId, double threshold, int maxNumberOfResults ) {
        ExpressionAnalysisResultSet ar = expressionAnalysisResultSetService.load( resultSetId );
        if ( ar == null ) {
            log.warn( "No diff ex result set with ID=" + resultSetId );
            return Collections.emptyList();
        }

        ar = expressionAnalysisResultSetService.thaw( ar );

        BioAssaySet analyzedSet = ar.getAnalysis().getExperimentAnalyzed();

        List<DifferentialExpressionValueObject> ee2probeResults = differentialExpressionResultService
                .findByResultSet( ar, threshold, maxNumberOfResults,
                        ProcessedExpressionDataVectorServiceImpl.DIFFEX_MIN_NUMBER_OF_RESULTS );

        Collection<Long> probes = new HashSet<>();
        // Map<CompositeSequenceId, pValue>
        // using id instead of entity for map key because want to use a value object for retrieval later
        Map<Long, Double> pvalues = new HashMap<>();
        for ( DifferentialExpressionValueObject par : ee2probeResults ) {
            probes.add( par.getProbeId() );
            pvalues.put( par.getProbeId(), par.getP() );
        }

        Collection<DoubleVectorValueObject> processedDataArraysByProbe = cachedProcessedExpressionDataVectorService.getProcessedDataArraysByProbeIds( analyzedSet, probes );
        // create a deep copy because we're going to modify it (with p-values)
        List<DoubleVectorValueObject> dedvs = processedDataArraysByProbe.stream()
                .map( DoubleVectorValueObject::copy )
                .collect( Collectors.toList() );

        /*
         * Resort
         */
        for ( DoubleVectorValueObject v : dedvs ) {
            v.setPvalue( pvalues.get( v.getDesignElement().getId() ) );
        }

        dedvs.sort( Comparator.comparing( DoubleVectorValueObject::getPvalue,
                Comparator.nullsLast( Comparator.naturalOrder() ) ) );

        return dedvs;
    }

    @Override
    public void evictFromCache( ExpressionExperiment ee ) {
        cachedProcessedExpressionDataVectorService.evict( ee );
    }

    /**
     * Creates an ExperimentExpressionLevelValueObject for the given experiment and collection of double vector VOs, and
     * adds it to the given vos collection.
     *
     * @param vos     the collection to add the result to.
     * @param ee      the experiment the vectors belong to.
     * @param vectors the vectors to create the new ExperimentExpressionLevelsVO with.
     */
    private void addExperimentGeneVectors( Collection<ExperimentExpressionLevelsValueObject> vos,
            ExpressionExperiment ee, Collection<DoubleVectorValueObject> vectors, boolean keepGeneNonSpecific,
            @Nullable String consolidateMode ) {
        // Pre-collect all referenced gene ids and resolve in one batch instead of calling
        // geneService.load(gId) per (vector, gene) — for PCA + gene-level expression endpoints
        // that is 100-300 redundant single-row loads per EE.
        Set<Long> geneIds = new HashSet<>();
        for ( DoubleVectorValueObject v : vectors ) {
            if ( !v.getExpressionExperiment().getId().equals( ee.getId() ) ) {
                continue;
            }
            if ( v.getGenes() != null ) {
                geneIds.addAll( v.getGenes() );
            }
        }
        Map<Long, Gene> genesById = new HashMap<>( geneIds.size() );
        for ( Gene g : geneService.load( geneIds ) ) {
            genesById.put( g.getId(), g );
        }

        Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene = new HashMap<>();
        for ( DoubleVectorValueObject v : vectors ) {
            if ( !v.getExpressionExperiment().getId().equals( ee.getId() ) ) {
                continue;
            }

            if ( v.getGenes() == null || v.getGenes().isEmpty() ) {
                if ( !vectorsPerGene.containsKey( null ) ) {
                    vectorsPerGene.put( null, new LinkedList<>() );
                }
                vectorsPerGene.get( null ).add( v );
            }

            for ( Long gId : v.getGenes() ) {
                Gene g = genesById.get( gId );
                if ( g != null ) {
                    if ( !vectorsPerGene.containsKey( g ) ) {
                        vectorsPerGene.put( g, new LinkedList<>() );
                    }
                    vectorsPerGene.get( g ).add( v );
                }
            }

        }
        vos.add( new ExperimentExpressionLevelsValueObject( ee.getId(), vectorsPerGene, keepGeneNonSpecific,
                consolidateMode ) );
    }

    /**
     * Like {@link #addExperimentGeneVectors} but also computes per-gene diff-ex stats (corrected p-value,
     * uncorrected p-value, log2 fold change) for the result-set whose ranking produced these vectors. For
     * genes whose probes span more than one result row, picks the most-significant row (smallest corrected
     * p-value) — the same selection rule the endpoint uses to rank its top-N.
     */
    private void addExperimentGeneVectorsWithDiffExStats( Collection<ExperimentExpressionLevelsValueObject> vos,
            ExpressionExperiment ee, Collection<DoubleVectorValueObject> vectors, boolean keepGeneNonSpecific,
            @Nullable String consolidateMode, Map<Long, DifferentialExpressionValueObject> statsByProbeId ) {
        // Batch-resolve referenced genes once; mirror of the addExperimentGeneVectors hoist.
        Set<Long> geneIds = new HashSet<>();
        for ( DoubleVectorValueObject v : vectors ) {
            if ( !v.getExpressionExperiment().getId().equals( ee.getId() ) ) {
                continue;
            }
            if ( v.getGenes() != null ) {
                geneIds.addAll( v.getGenes() );
            }
        }
        Map<Long, Gene> genesById = new HashMap<>( geneIds.size() );
        for ( Gene g : geneService.load( geneIds ) ) {
            genesById.put( g.getId(), g );
        }

        Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene = new HashMap<>();
        Map<Gene, DifferentialExpressionValueObject> bestStatsPerGene = new HashMap<>();
        for ( DoubleVectorValueObject v : vectors ) {
            if ( !v.getExpressionExperiment().getId().equals( ee.getId() ) ) {
                continue;
            }

            DifferentialExpressionValueObject probeStats = statsByProbeId.get( v.getDesignElement().getId() );

            if ( v.getGenes() == null || v.getGenes().isEmpty() ) {
                if ( !vectorsPerGene.containsKey( null ) ) {
                    vectorsPerGene.put( null, new LinkedList<>() );
                }
                vectorsPerGene.get( null ).add( v );
            }

            for ( Long gId : v.getGenes() ) {
                Gene g = genesById.get( gId );
                if ( g != null ) {
                    if ( !vectorsPerGene.containsKey( g ) ) {
                        vectorsPerGene.put( g, new LinkedList<>() );
                    }
                    vectorsPerGene.get( g ).add( v );

                    if ( probeStats != null ) {
                        DifferentialExpressionValueObject prev = bestStatsPerGene.get( g );
                        if ( prev == null || isMoreSignificant( probeStats, prev ) ) {
                            bestStatsPerGene.put( g, probeStats );
                        }
                    }
                }
            }
        }

        Map<Gene, ExperimentExpressionLevelsValueObject.GeneDiffExStats> diffExStatsPerGene = new HashMap<>( bestStatsPerGene.size() );
        for ( Map.Entry<Gene, DifferentialExpressionValueObject> entry : bestStatsPerGene.entrySet() ) {
            diffExStatsPerGene.put( entry.getKey(), buildGeneDiffExStats( entry.getValue() ) );
        }

        vos.add( new ExperimentExpressionLevelsValueObject( ee.getId(), vectorsPerGene, keepGeneNonSpecific,
                consolidateMode, diffExStatsPerGene ) );
    }

    /**
     * Build the gene-level diff-ex statistics. The log2 fold change is the contrast coefficient on the
     * picked row; for a single-contrast result set there is exactly one contrast, for a multi-contrast
     * result set we pick the contrast with the smallest uncorrected p-value (most significant on that row).
     */
    private static ExperimentExpressionLevelsValueObject.GeneDiffExStats buildGeneDiffExStats( DifferentialExpressionValueObject row ) {
        Double log2FoldChange = null;
        if ( row.getContrasts() != null && row.getContrasts().getContrasts() != null
                && !row.getContrasts().getContrasts().isEmpty() ) {
            ubic.gemma.model.analysis.expression.diff.ContrastVO best = null;
            for ( ubic.gemma.model.analysis.expression.diff.ContrastVO c : row.getContrasts().getContrasts() ) {
                if ( c.getLogFoldChange() == null ) continue;
                if ( best == null ) { best = c; continue; }
                Double bp = best.getPvalue();
                Double cp = c.getPvalue();
                if ( bp == null && cp != null ) { best = c; continue; }
                if ( bp != null && cp != null && cp < bp ) best = c;
            }
            if ( best != null ) log2FoldChange = best.getLogFoldChange();
        }
        return new ExperimentExpressionLevelsValueObject.GeneDiffExStats( row.getCorrP(), row.getP(), log2FoldChange );
    }
}
