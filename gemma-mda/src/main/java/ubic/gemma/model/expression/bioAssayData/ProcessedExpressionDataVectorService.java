/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author Paul
 * @version $Id$
 */
public interface ProcessedExpressionDataVectorService {

    @Secured({ "GROUP_ADMIN" })
    public void clearCache();

    /**
     * Populate the processed data for the given experiment. For two-channel studies, the missing value information
     * should already have been computed.
     * 
     * @param expressionExperiment
     * @return updated expressionExperiment
     */
    @Secured({ "GROUP_USER" })
    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment expressionExperiment );

    /**
     * @param bioassaySets - expressionExperiments or expressionExperimentSubSets
     * @param genes
     * @return vectors, which will be subsetted if the bioassayset is a subset.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( Collection<? extends BioAssaySet> bioassaySets,
            Collection<Long> genes );

    /**
     * Note: currently only used in tests
     * 
     * @param expressionExperiment
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiments
     * @param limit (null limit = default hibernate limit).
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment ee, int limit );

    /**
     * Retrieves DEDV's by probes and experiments
     * 
     * @param expressionExperiments
     * @param compositeSequences
     * @param fullMap
     * @return DVVOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<CompositeSequence> compositeSequences );

    /**
     * @param expressionExperiment
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiments
     * @param genes
     * @param method
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_AFTER_MAP_READ", "ACL_SECURABLE_COLLECTION_READ" })
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method );

    /**
     * @param expressionExperiment
     * @param genes
     * @param method
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method );

    /**
     * @param expressionExperiment
     * @param method
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method );

    /**
     * Retrieve expression level information for genes in experiments.
     * 
     * @param expressionExperiments
     * @param genes
     * @return A map of experiment -> gene -> probe -> array of doubles holding the 1) mean and 2) max expression rank.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> eeCol, Collection<Gene> pars );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void removeProcessedDataVectors( final ExpressionExperiment expressionExperiment );

    /**
     * @param vectors
     */
    public void thaw( Collection<ProcessedExpressionDataVector> vectors );

    /**
     * Updates a collection of ProcessedExpressionDataVectors
     */
    @Secured({ "GROUP_USER" })
    public void update( java.util.Collection<ProcessedExpressionDataVector> dedvs );

}
