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
package ubic.gemma.persistence.service.analysis.expression.pca;

import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.List;

/**
 * @author paul
 */
public interface PrincipalComponentAnalysisDao extends BaseDao<PrincipalComponentAnalysis> {

    Collection<PrincipalComponentAnalysis> findByExperiment( ExpressionExperiment ee );

    boolean existsByExperiment( ExpressionExperiment ee );

    List<ProbeLoading> getTopLoadedProbes( ExpressionExperiment ee, int component, int count );

    void removeForExperiment( ExpressionExperiment ee );
}
