package ubic.gemma.persistence.service.expression.bioAssayData;

import org.openjena.atlas.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author Paul
 */
@Service
public class ProcessedExpressionDataVectorServiceImpl implements ProcessedExpressionDataVectorService {

    public static final int DIFFEX_MIN_NUMBER_OF_RESULTS = 50;
    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;
    @Autowired
    private GeneService geneService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Override
    public void clearCache() {
        this.getProcessedExpressionDataVectorDao().clearCache();
    }

    @Override
    @Transactional
    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().createProcessedDataVectors( expressionExperiment );
    }

    @Override
    @Transactional
    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> vecs ) {
        return this.getProcessedExpressionDataVectorDao().createProcessedDataVectors( ee, vecs );

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<Long> genes ) {
        clearCache(); // uncomment for debugging TEMPORARY FIX FOR 4320

        return processedExpressionDataVectorDao.getProcessedDataArrays( expressionExperiments, genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExperimentExpressionLevelsValueObject> getExpressionLevels( Collection<ExpressionExperiment> ees,
            Collection<Gene> genes ) {
        Collection<DoubleVectorValueObject> vectors = getProcessedDataArrays( ees, EntityUtils.getIds( genes ) );
        Collection<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>( ees.size() );

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
            vos.add( new ExperimentExpressionLevelsValueObject( ee.getId(), vectorsPerGene ) );
        }

        return vos;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExperimentExpressionLevelsValueObject> getExpressionLevelsPca(
            Collection<ExpressionExperiment> ees, int limit, int component ) {
        Collection<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>( ees.size() );

        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            Collection<DoubleVectorValueObject> vectors = svdService.getTopLoadedVectors( ee.getId(), component, limit )
                    .values();
            AddExperimentGeneVectors( vos, ee, vectors );
        }

        return vos;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExperimentExpressionLevelsValueObject> getExpressionLevelsDiffEx(
            Collection<ExpressionExperiment> ees, Long diffExResultSetId, double threshold, int max ) {
        Collection<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>();

        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            Collection<DoubleVectorValueObject> vectors = getDiffExVectors( diffExResultSetId, threshold, max );
            AddExperimentGeneVectors( vos, ee, vectors );
        }

        return vos;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedDataArrays( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment ee, int limit ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedDataArrays( ee, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<? extends BioAssaySet> expressionExperiments,
            Collection<CompositeSequence> compositeSequences ) {

        return this.getProcessedExpressionDataVectorDao()
                .getProcessedDataArraysByProbe( expressionExperiments, compositeSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet ee,
            Collection<Long> probes ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedDataArraysByProbeIds( ee, probes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors(
            ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedVectors( expressionExperiment );
    }

    @Transactional(readOnly = true)
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment,
            int limit ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedVectors( expressionExperiment, limit );
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
        return this.getProcessedExpressionDataVectorDao().getRanksByProbe( eeCol, genes );
    }

    @Override
    @Transactional
    public void removeProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        this.getProcessedExpressionDataVectorDao().removeProcessedDataVectors( expressionExperiment );

    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( Collection<ProcessedExpressionDataVector> vectors ) {
        this.getProcessedExpressionDataVectorDao().thaw( vectors );

    }

    @Override
    @Transactional
    public void update( Collection<ProcessedExpressionDataVector> dedvs ) {
        this.getProcessedExpressionDataVectorDao().update( dedvs );
    }

    @Override
    public void remove( Collection<ProcessedExpressionDataVector> processedExpressionDataVectors ) {
        this.getProcessedExpressionDataVectorDao().remove( processedExpressionDataVectors );
    }

    @Override
    public List<DoubleVectorValueObject> getDiffExVectors( Long resultSetId, Double threshold,
            int maxNumberOfResults ) {

        ExpressionAnalysisResultSet ar = differentialExpressionResultService.loadAnalysisResultSet( resultSetId );
        if ( ar == null ) {
            Log.warn( this.getClass(), "No diff ex result set with ID=" + resultSetId );
            return null;
        }

        differentialExpressionResultService.thawLite( ar );

        BioAssaySet analyzedSet = ar.getAnalysis().getExperimentAnalyzed();

        List<DifferentialExpressionValueObject> ee2probeResults = differentialExpressionResultService
                .findInResultSet( ar, threshold, maxNumberOfResults, DIFFEX_MIN_NUMBER_OF_RESULTS );

        Collection<Long> probes = new HashSet<>();
        // Map<CompositeSequenceId, pValue>
        // using id instead of entity for map key because want to use a value object for retrieval later
        Map<Long, Double> pvalues = new HashMap<>();
        for ( DifferentialExpressionValueObject par : ee2probeResults ) {
            probes.add( par.getProbeId() );
            pvalues.put( par.getProbeId(), par.getP() );
        }

        Collection<DoubleVectorValueObject> processedDataArraysByProbe = getProcessedDataArraysByProbeIds( analyzedSet,
                probes );
        List<DoubleVectorValueObject> dedvs = new ArrayList<>( processedDataArraysByProbe );

        /*
         * Resort
         */
        for ( DoubleVectorValueObject v : dedvs ) {
            v.setPvalue( pvalues.get( v.getDesignElement().getId() ) );
        }

        Collections.sort( dedvs, new Comparator<DoubleVectorValueObject>() {
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

    /**
     * Creates an ExperimentExpressionLevelValueObject for the given experiment and collection of double vector VOs, and
     * adds it to the given vos collection.
     *
     * @param vos     the collection to add the result to.
     * @param ee      the experiment the vectors belong to.
     * @param vectors the vectors to create the new ExperimentExpressionLevelsVO with.
     */
    private void AddExperimentGeneVectors( Collection<ExperimentExpressionLevelsValueObject> vos,
            ExpressionExperiment ee, Collection<DoubleVectorValueObject> vectors ) {
        Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene = new HashMap<>();
        if(vectors == null){
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
        vos.add( new ExperimentExpressionLevelsValueObject( ee.getId(), vectorsPerGene ) );
    }

    private ProcessedExpressionDataVectorDao getProcessedExpressionDataVectorDao() {
        return processedExpressionDataVectorDao;
    }

}
