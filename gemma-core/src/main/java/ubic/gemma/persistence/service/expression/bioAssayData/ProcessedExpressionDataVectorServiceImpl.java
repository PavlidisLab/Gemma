package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author Paul
 */
@Service
public class ProcessedExpressionDataVectorServiceImpl implements ProcessedExpressionDataVectorService {

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;
    @Autowired
    private GeneService geneService;
    @Autowired
    private SVDService svdService;

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
        Collection<DoubleVectorValueObject> vectors = getProcessedDataArrays( ees, EntityUtils.getIdsFast( genes ) );
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
    public Collection<ExperimentExpressionLevelsValueObject> getExpressionLevelsPca( Collection<ExpressionExperiment> ees,
            int threshold, int component ) {
        Collection<ExperimentExpressionLevelsValueObject> vos = new ArrayList<>( ees.size() );

        // Adapted from DEDV controller
        for ( ExpressionExperiment ee : ees ) {
            Map<Gene, List<DoubleVectorValueObject>> vectorsPerGene = new HashMap<>();
            Collection<DoubleVectorValueObject> vectors =  svdService.getTopLoadedVectors( ee.getId(), component, threshold ).values();
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

    private ProcessedExpressionDataVectorDao getProcessedExpressionDataVectorDao() {
        return processedExpressionDataVectorDao;
    }

    public void setProcessedExpressionDataVectorDao(
            ProcessedExpressionDataVectorDao processedExpressionDataVectorDao ) {
        this.processedExpressionDataVectorDao = processedExpressionDataVectorDao;
    }

}
