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
package ubic.gemma.model.analysis.expression;

import org.springframework.security.access.annotation.Secured;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public interface PrincipalComponentAnalysisService {

    /**
     * @param ee
     * @param u
     * @param vToStore
     * @param bad
     * @param numLoadingsToStore
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    PrincipalComponentAnalysis create( ExpressionExperiment ee, DoubleMatrix<CompositeSequence, Integer> u,
            double[] ds, DoubleMatrix<Integer, Integer> vToStore, BioAssayDimension bad, int numLoadingsToStore );

    /**
     * @param ee
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeForExperiment( ExpressionExperiment ee );

    /**
     * @param ee
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    PrincipalComponentAnalysis loadForExperiment( ExpressionExperiment ee );

}
