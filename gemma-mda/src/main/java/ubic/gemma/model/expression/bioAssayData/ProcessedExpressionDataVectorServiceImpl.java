package ubic.gemma.model.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.Map;

/**
 * @author Paul
 */
@Service
public class ProcessedExpressionDataVectorServiceImpl implements ProcessedExpressionDataVectorService {

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

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

    private ProcessedExpressionDataVectorDao getProcessedExpressionDataVectorDao() {
        return processedExpressionDataVectorDao;
    }

    public void setProcessedExpressionDataVectorDao(
            ProcessedExpressionDataVectorDao processedExpressionDataVectorDao ) {
        this.processedExpressionDataVectorDao = processedExpressionDataVectorDao;
    }

}
