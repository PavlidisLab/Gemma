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
package ubic.gemma.analysis.preprocess.svd;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Performs Singular value decomposition on experiment data to get eigengenes, and does comparison of those PCs to
 * factors recorded in the experimental design.
 * 
 * @author paul
 * @version $Id$
 */
public interface SVDService {

    @Secured( { "GROUP_AGENT" })
    public void svd( Collection<ExpressionExperiment> ees );

    /**
     * @param ee
     */
    @Secured( { "GROUP_AGENT" })
    public SVDValueObject svd( ExpressionExperiment ee );

    /**
     * Compare ExperimentalFactors and BioAssay.processingDates to the PCs.
     * 
     * @param ee
     * @return
     */
    @Secured( { "GROUP_AGENT" })
    SVDValueObject svdFactorAnalysis( ExpressionExperiment ee );

    /**
     * @param id
     * @return
     */
    @Secured( { "GROUP_AGENT" })
    public SVDValueObject retrieveSvd( ExpressionExperiment ee );

    /**
     * Compare ExperimentalFactors and BioAssay.processingDates to the PCs.
     * 
     * @param ee
     * @param svo
     * @return
     */
    @Secured( { "GROUP_AGENT" })
    public SVDValueObject svdFactorAnalysis( ExpressionExperiment ee, SVDValueObject svo );
}