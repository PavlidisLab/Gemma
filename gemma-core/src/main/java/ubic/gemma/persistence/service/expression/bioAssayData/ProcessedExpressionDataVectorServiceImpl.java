package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.datastructure.matrix.QuantitationMismatchException;
import ubic.gemma.core.genome.gene.service.GeneService;
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
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Paul
 */
@Service
@CommonsLog
public class ProcessedExpressionDataVectorServiceImpl
        extends AbstractDesignElementDataVectorService<ProcessedExpressionDataVector>
        implements ProcessedExpressionDataVectorService, InitializingBean {

    private static final int DIFFEX_MIN_NUMBER_OF_RESULTS = 50;

    private final ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @Autowired
    private GeneService geneService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;
    @Autowired
    private ProcessedExpressionDataVectorCreateHelperService helperService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Autowired
    private BeanFactory beanFactory;

    /**
     * For self-referencing transactional methods.
     */
    private ProcessedExpressionDataVectorService self;

    @Autowired
    protected ProcessedExpressionDataVectorServiceImpl( ProcessedExpressionDataVectorDao mainDao ) {
        super( mainDao );
        this.processedExpressionDataVectorDao = mainDao;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        self = beanFactory.getBean( ProcessedExpressionDataVectorService.class );
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void replaceProcessedDataVectors( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> vectors ) {
        try {
            helperService.replaceProcessedDataVectors( ee, vectors );
            auditTrailService.addUpdateEvent( ee, ProcessedVectorComputationEvent.class, String.format( "Replaced processed expression data for %s.", ee ) );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( ee, FailedProcessedVectorComputationEvent.class, "Failed to replace processed expression data vectors.", e );
            throw e;
        }
        self.updateRanks( ee );
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
    public void clearCache() {
        this.processedExpressionDataVectorDao.clearCache();
    }

    @Override
    @Transactional
    public Set<ProcessedExpressionDataVector> createProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        try {
            return createProcessedDataVectors( expressionExperiment, true );
        } catch ( QuantitationMismatchException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional
    public Set<ProcessedExpressionDataVector> createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch ) throws QuantitationMismatchException {
        try {
            Set<ProcessedExpressionDataVector> result = this.processedExpressionDataVectorDao.createProcessedDataVectors( expressionExperiment, ignoreQuantitationMismatch );
            auditTrailService.addUpdateEvent( expressionExperiment, ProcessedVectorComputationEvent.class, String.format( "Created processed expression data for %s.", expressionExperiment ) );
            return result;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedProcessedVectorComputationEvent.class, "Failed to create processed expression data vectors.", e );
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Long> genes ) {
        this.clearCache(); // Fix for 4320
        return processedExpressionDataVectorDao.getProcessedDataArrays( expressionExperiments, genes );
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet ee, Collection<Long> genes ) {
        this.clearCache(); // Fix for 4320
        return processedExpressionDataVectorDao.getProcessedDataArrays( Collections.singleton( ee ), genes );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentExpressionLevelsValueObject> getExpressionLevels( Collection<ExpressionExperiment> ees,
            Collection<Gene> genes, boolean keepGeneNonSpecific, @Nullable String consolidateMode ) {
        Collection<DoubleVectorValueObject> vectors = this.getProcessedDataArrays( ees, EntityUtils.getIds( genes ) );
        List<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>( ees.size() );

        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene = new HashMap<>();
            for ( DoubleVectorValueObject v : vectors ) {
                if ( !v.getExpressionExperiment().getId().equals( ee.getId() ) ) {
                    continue;
                }

                if ( v.getGenes() == null || v.getGenes().isEmpty() ) {
                    continue;
                }

                for ( Gene g : genes ) {
                    if ( v.getGenes().contains( g.getId() ) ) {
                        if ( !vectorsPerGene.containsKey( g ) ) {
                            vectorsPerGene.put( g, new LinkedList<DoubleVectorValueObject>() );
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
    public List<ExperimentExpressionLevelsValueObject> getExpressionLevelsPca(
            Collection<ExpressionExperiment> ees, int limit, int component, boolean keepGeneNonSpecific,
            @Nullable String consolidateMode ) {
        List<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>( ees.size() );

        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            Collection<DoubleVectorValueObject> vectors = svdService.getTopLoadedVectors( ee.getId(), component, limit )
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
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment ) {
        return this.processedExpressionDataVectorDao.getProcessedDataArrays( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment ee, int limit ) {
        return this.processedExpressionDataVectorDao.getProcessedDataArrays( ee, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<ExpressionExperiment> expressionExperiments,
            Collection<CompositeSequence> compositeSequences ) {

        return this.processedExpressionDataVectorDao
                .getProcessedDataArraysByProbe( expressionExperiments, compositeSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet ee,
            Collection<Long> probes ) {
        return this.processedExpressionDataVectorDao.getProcessedDataArraysByProbeIds( ee, probes );
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
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiment, genes, method );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiment, method );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> eeCol, Collection<Gene> genes ) {
        return this.processedExpressionDataVectorDao.getRanksByProbe( eeCol, genes );
    }

    @Override
    @Transactional
    public void removeProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        this.processedExpressionDataVectorDao.removeProcessedDataVectors( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoubleVectorValueObject> getDiffExVectors( Long resultSetId, Double threshold,
            int maxNumberOfResults ) {

        ExpressionAnalysisResultSet ar = expressionAnalysisResultSetService.load( resultSetId );
        if ( ar == null ) {
            log.warn( "No diff ex result set with ID=" + resultSetId );
            return null;
        }

        expressionAnalysisResultSetService.thaw( ar );

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

        Collection<DoubleVectorValueObject> processedDataArraysByProbe = this
                .getProcessedDataArraysByProbeIds( analyzedSet, probes );
        List<DoubleVectorValueObject> dedvs = new ArrayList<>( processedDataArraysByProbe );

        /*
         * Resort
         */
        for ( DoubleVectorValueObject v : dedvs ) {
            v.setPvalue( pvalues.get( v.getDesignElement().getId() ) );
        }

        dedvs.sort( new Comparator<DoubleVectorValueObject>() {
            @Override
            public int compare( DoubleVectorValueObject o1, DoubleVectorValueObject o2 ) {
                if ( o1.getPvalue() == null )
                    return -1;
                if ( o2.getPvalue() == null )
                    return 1;
                return o1.getPvalue().compareTo( o2.getPvalue() );
            }
        } );

        return dedvs;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void computeProcessedExpressionData( ExpressionExperiment ee ) {
        try {
            computeProcessedExpressionData( ee, true );
        } catch ( QuantitationMismatchException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void computeProcessedExpressionData( ExpressionExperiment ee, boolean ignoreQuantitationMismatch ) throws QuantitationMismatchException {
        self.createProcessedDataVectors( ee, ignoreQuantitationMismatch );
        self.updateRanks( ee );
    }

    @Override
    @Transactional
    public void reorderByDesign( ExpressionExperiment ee ) {
        this.helperService.reorderByDesign( ee );
        this.auditTrailService.addUpdateEvent( ee, "Reordered the data vectors by experimental design" );
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
        if ( vectors == null ) {
            return;
        }
        for ( DoubleVectorValueObject v : vectors ) {
            if ( !v.getExpressionExperiment().getId().equals( ee.getId() ) ) {
                continue;
            }

            if ( v.getGenes() == null || v.getGenes().isEmpty() ) {
                if ( !vectorsPerGene.containsKey( null ) ) {
                    vectorsPerGene.put( null, new LinkedList<DoubleVectorValueObject>() );
                }
                vectorsPerGene.get( null ).add( v );
            }

            for ( Long gId : v.getGenes() ) {
                Gene g = geneService.load( gId );
                if ( g != null ) {
                    if ( !vectorsPerGene.containsKey( g ) ) {
                        vectorsPerGene.put( g, new LinkedList<DoubleVectorValueObject>() );
                    }
                    vectorsPerGene.get( g ).add( v );
                }
            }

        }
        vos.add( new ExperimentExpressionLevelsValueObject( ee.getId(), vectorsPerGene, keepGeneNonSpecific,
                consolidateMode ) );
    }
}
