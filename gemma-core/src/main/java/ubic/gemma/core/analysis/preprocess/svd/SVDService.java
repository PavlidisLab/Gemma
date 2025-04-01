/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess.svd;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Performs Singular value decomposition on experiment data to get eigengenes, and does comparison of those PCs to
 * factors recorded in the experimental design.
 *
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public interface SVDService {

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Map<ProbeLoading, DoubleVectorValueObject> getTopLoadedVectors( ExpressionExperiment ee, int component, int count );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    boolean hasPca( ExpressionExperiment ee );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SVDResult getSvd( ExpressionExperiment ee );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    SVDResult svd( ExpressionExperiment ee ) throws SVDException;

    /**
     * Compare ExperimentalFactors and BioAssay.processingDates to the PCs.
     *
     * @param ee the experiment
     * @return SVD VO
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SVDResult getSvdFactorAnalysis( ExpressionExperiment ee );

    /**
     * Compare ExperimentalFactors and BioAssay.processingDates to the PCs.
     *
     * @param pca PCA
     * @return SVD VO
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SVDResult getSvdFactorAnalysis( PrincipalComponentAnalysis pca );

    /**
     * @param experimentalFactors to consider
     * @param importanceThreshold threshold for pvalue of association with factor. Suggested value might be 0.01.
     * @param ee                  the expression experiment
     * @return factors which are "significantly" associated with one of the first three PCs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Set<ExperimentalFactor> getImportantFactors( ExpressionExperiment ee,
            Collection<ExperimentalFactor> experimentalFactors, Double importanceThreshold );
}