package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Paul
 */
@Service
@CommonsLog
public class ProcessedExpressionDataVectorServiceImpl
        extends AbstractDesignElementDataVectorService<ProcessedExpressionDataVector>
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
    private AuditTrailService auditTrailService;
    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private CachedProcessedExpressionDataVectorService cachedProcessedExpressionDataVectorService;

    @Autowired
    protected ProcessedExpressionDataVectorServiceImpl( ProcessedExpressionDataVectorDao mainDao ) {
        super( mainDao );
        this.processedExpressionDataVectorDao = mainDao;
    }

    @Override
    @Transactional(rollbackFor = { QuantitationTypeConversionException.class })
    public int createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean updateRanks ) throws QuantitationTypeConversionException {
        try {
            return createProcessedDataVectors( expressionExperiment, true, true );
        } catch ( QuantitationTypeDetectionException e ) {
            // never happening
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional(rollbackFor = { QuantitationTypeDetectionException.class, QuantitationTypeConversionException.class })
    public int createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean updateRanks, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        int created;
        try {
            created = this.processedExpressionDataVectorDao.createProcessedDataVectors( expressionExperiment, ignoreQuantitationMismatch );
            auditTrailService.addUpdateEvent( expressionExperiment, ProcessedVectorComputationEvent.class, String.format( "Created processed expression data for %s.", expressionExperiment ) );
        } catch ( Exception e ) {
            // Note: addUpdateEvent with an exception uses REQUIRES_NEW, which will create an audit event that cannot be
            //       rolled back
            auditTrailService.addUpdateEvent( expressionExperiment, FailedProcessedVectorComputationEvent.class, "Failed to create processed expression data vectors.", e );
            throw e;
        }
        if ( updateRanks ) {
            updateRanks( expressionExperiment );
        }
        return created;
    }

    @Override
    @Transactional
    public int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors, boolean updateRanks ) {
        int replaced;
        try {
            replaced = expressionExperimentService.replaceProcessedDataVectors( ee, vectors );
            auditTrailService.addUpdateEvent( ee, ProcessedVectorComputationEvent.class, String.format( "Replaced processed expression data for %s.", ee ) );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( ee, FailedProcessedVectorComputationEvent.class, "Failed to replace processed expression data vectors.", e );
            throw e;
        }
        if ( updateRanks ) {
            updateRanks( ee );
        }
        return replaced;
    }

    @Override
    @Transactional
    public void reorderByDesign( ExpressionExperiment ee ) {
        this.helperService.reorderByDesign( ee );
        this.auditTrailService.addUpdateEvent( ee, "Reordered the data vectors by experimental design" );
    }

    @Override
    @Transactional
    public void updateRanks( ExpressionExperiment ee ) {
        try {
            helperService.updateRanks( ee );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( ee, FailedProcessedVectorComputationEvent.class, "Failed to update ranks for expression data vectors.", e );
            throw e;
        }
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

        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            Collection<DoubleVectorValueObject> vectors = this.getDiffExVectors( diffExResultSetId, threshold, max );
            this.addExperimentGeneVectors( vos, ee, vectors, keepGeneNonSpecific, consolidateMode );
        }

        return vos;
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
                .findInResultSet( ar, threshold, maxNumberOfResults,
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
        List<DoubleVectorValueObject> dedvs = new ArrayList<>( processedDataArraysByProbe );

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
                Gene g = geneService.load( gId );
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
}
