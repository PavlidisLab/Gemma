/**
 * 
 */
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author Paul
 * @version $Id$
 */
@Service
public class ProcessedExpressionDataVectorServiceImpl implements ProcessedExpressionDataVectorService {

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @Override
    public void clearCache() {
        this.getProcessedExpressionDataVectorDao().clearCache();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#createProcessedDataVectors(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().createProcessedDataVectors( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataArrays(java.util
     * .Collection, java.util.Collection, java.lang.Boolean)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( Collection<? extends BioAssaySet> bioassaySets,
            Collection<Gene> genes, Boolean fullMapping ) {
        return processedExpressionDataVectorDao.getProcessedDataArrays( bioassaySets, genes, fullMapping );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataArrays(java.util
     * .Collection, java.util.Collection)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes ) {
        return processedExpressionDataVectorDao.getProcessedDataArrays( expressionExperiments, genes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataMatrix(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedDataArrays( expressionExperiment );
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment,
            Collection<Gene> genes ) {
        return processedExpressionDataVectorDao.getProcessedDataArrays( expressionExperiment, genes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataMatrices(java.util
     * .Collection)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment ee, int limit,
            boolean fullMap ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedDataArrays( ee, limit, fullMap );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataArraysByProbe(java
     * .util.Collection, java.util.Collection, boolean)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<CompositeSequence> compositeSequences,
            boolean fullMap ) {

        return this.getProcessedExpressionDataVectorDao().getProcessedDataArraysByProbe( expressionExperiments,
                compositeSequences, fullMap );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataVectors(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedVectors( expressionExperiment );
    }

    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors(
            ExpressionExperiment expressionExperiment, int limit ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedVectors( expressionExperiment, limit );
    }

    public ProcessedExpressionDataVectorDao getProcessedExpressionDataVectorDao() {
        return processedExpressionDataVectorDao;
    }

    @Override
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiments, genes, method );
    }

    @Override
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiment, genes, method );
    }

    @Override
    public Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiment, method );
    }

    @Override
    public Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> eeCol, Collection<Gene> genes ) {
        return this.getProcessedExpressionDataVectorDao().getRanksByProbe( eeCol, genes );
    }

    @Override
    public void removeProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        this.getProcessedExpressionDataVectorDao().removeProcessedDataVectors( expressionExperiment );

    }

    public void setProcessedExpressionDataVectorDao( ProcessedExpressionDataVectorDao processedExpressionDataVectorDao ) {
        this.processedExpressionDataVectorDao = processedExpressionDataVectorDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#thaw(java.util.Collection)
     */
    @Override
    public void thaw( Collection<ProcessedExpressionDataVector> vectors ) {
        this.getProcessedExpressionDataVectorDao().thaw( vectors );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#update(java.util.Collection)
     */
    @Override
    public void update( Collection<ProcessedExpressionDataVector> dedvs ) {
        this.getProcessedExpressionDataVectorDao().update( dedvs );

    }

}
