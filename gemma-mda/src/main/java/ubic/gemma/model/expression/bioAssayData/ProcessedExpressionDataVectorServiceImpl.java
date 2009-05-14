/**
 * 
 */
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author Paul
 * @version $Id$
 */
public class ProcessedExpressionDataVectorServiceImpl implements ProcessedExpressionDataVectorService {

    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#createProcessedDataVectors(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment)
     */
    public Collection<ProcessedExpressionDataVector> createProcessedDataVectors(
            ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().createProcessedDataVectors( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataMatrices(java.util
     * .Collection)
     */
    public  Collection<DoubleVectorValueObject> getProcessedDataArrays(ExpressionExperiment ee, int limit, boolean fullMap) {
        return this.getProcessedExpressionDataVectorDao().getProcessedDataArrays( ee, limit, fullMap);
    }

    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes ) {
        return processedExpressionDataVectorDao.getProcessedDataArrays( expressionExperiments, genes );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataMatrix(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedDataArrays( expressionExperiment );
    }

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment,
            Collection<Gene> genes ) {
        return processedExpressionDataVectorDao.getProcessedDataArrays( expressionExperiment, genes );
    }

    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, Boolean fullMapping ) {
        return processedExpressionDataVectorDao.getProcessedDataArrays( expressionExperiments, genes, fullMapping );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#getProcessedDataVectors(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedVectors( expressionExperiment );
    }

    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment, int limit ) {
        return this.getProcessedExpressionDataVectorDao().getProcessedVectors( expressionExperiment, limit );
    }
    
    public ProcessedExpressionDataVectorDao getProcessedExpressionDataVectorDao() {
        return processedExpressionDataVectorDao;
    }

    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiments, genes, method );
    }

    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiment, genes, method );
    }

    public Map<DesignElement, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        return processedExpressionDataVectorDao.getRanks( expressionExperiment, method );
    }

    public void setProcessedExpressionDataVectorDao( ProcessedExpressionDataVectorDao processedExpressionDataVectorDao ) {
        this.processedExpressionDataVectorDao = processedExpressionDataVectorDao;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#thaw(java.util.Collection)
     */
    public void thaw( Collection<ProcessedExpressionDataVector> vectors ) {
        this.getProcessedExpressionDataVectorDao().thaw( vectors );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService#update(java.util.Collection)
     */
    public void update( Collection<ProcessedExpressionDataVector> dedvs ) {
        this.getProcessedExpressionDataVectorDao().update( dedvs );

    }

    public Map<ExpressionExperiment, Map<Gene, Map<DesignElement, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> eeCol, Collection<Gene> genes ) {
        return this.getProcessedExpressionDataVectorDao().getRanksByProbe( eeCol, genes );
    }

}
