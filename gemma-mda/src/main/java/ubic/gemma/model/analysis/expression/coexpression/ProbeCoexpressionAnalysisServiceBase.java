/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.analysis.expression.coexpression;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for
 * <code>ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService
 */
public abstract class ProbeCoexpressionAnalysisServiceBase extends
        ubic.gemma.model.analysis.AnalysisServiceImpl<ProbeCoexpressionAnalysis> implements
        ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService {

    @Autowired
    private ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao probeCoexpressionAnalysisDao;

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService#create(ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis)
     */
    @Override
    public ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis create(
            final ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis probeCoexpressionAnalysis ) {
        return this.handleCreate( probeCoexpressionAnalysis );

    }

    /**
     * Sets the reference to <code>probeCoexpressionAnalysis</code>'s DAO.
     */
    public void setProbeCoexpressionAnalysisDao(
            ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao probeCoexpressionAnalysisDao ) {
        this.probeCoexpressionAnalysisDao = probeCoexpressionAnalysisDao;
    }

    /**
     * Gets the reference to <code>probeCoexpressionAnalysis</code>'s DAO.
     */
    protected ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao getProbeCoexpressionAnalysisDao() {
        return this.probeCoexpressionAnalysisDao;
    }

    /**
     * Performs the core logic for
     * {@link #create(ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis)}
     */
    protected abstract ProbeCoexpressionAnalysis handleCreate( ProbeCoexpressionAnalysis probeCoexpressionAnalysis );

}