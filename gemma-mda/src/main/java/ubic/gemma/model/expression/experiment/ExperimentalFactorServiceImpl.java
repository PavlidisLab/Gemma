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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService
 */
@Service
public class ExperimentalFactorServiceImpl extends ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase {

    @Override
    public Collection<ExperimentalFactor> create( Collection<ExperimentalFactor> factors ) {
        return ( Collection<ExperimentalFactor> ) this.getExperimentalFactorDao().create( factors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#load(java.util.Collection)
     */
    @Override
    public Collection<ExperimentalFactor> load( Collection<Long> ids ) {
        return ( Collection<ExperimentalFactor> ) this.getExperimentalFactorDao().load( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleCreate(ubic.gemma.model.expression
     * .experiment.ExperimentalFactor)
     */
    @Override
    protected ExperimentalFactor handleCreate( ExperimentalFactor experimentalFactor ) {
        return this.getExperimentalFactorDao().create( experimentalFactor );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleDelete(ubic.gemma.model.expression
     * .experiment.ExperimentalFactor)
     */
    @Override
    protected void handleDelete( ExperimentalFactor experimentalFactor ) {

        /*
         * First, check to see if there are any diff results that use this factor.
         */
        Collection<DifferentialExpressionAnalysis> analyses = getDifferentialExpressionAnalysisDao().findByFactor(
                experimentalFactor );
        for ( DifferentialExpressionAnalysis a : analyses ) {
            getDifferentialExpressionAnalysisDao().remove( a );
        }

        this.getExperimentalFactorDao().remove( experimentalFactor );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleFindOrcreate(ubic.gemma.model.expression
     * .experiment.ExperimentalFactor)
     */
    @Override
    protected ExperimentalFactor handleFind( ExperimentalFactor experimentalFactor ) {
        return this.getExperimentalFactorDao().find( experimentalFactor );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleFindOrcreate(ubic.gemma.model.expression
     * .experiment.ExperimentalFactor)
     */
    @Override
    protected ExperimentalFactor handleFindOrCreate( ExperimentalFactor experimentalFactor ) {
        return this.getExperimentalFactorDao().findOrCreate( experimentalFactor );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected ExperimentalFactor handleLoad( Long id ) {
        return this.getExperimentalFactorDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#getAllExperimentalFactors()
     */
    @Override
    protected java.util.Collection<ExperimentalFactor> handleLoadAll() {
        return ( Collection<ExperimentalFactor> ) this.getExperimentalFactorDao().loadAll();
    }

    @Override
    protected void handleUpdate( ExperimentalFactor experimentalFactor ) {
        this.getExperimentalFactorDao().update( experimentalFactor );
    }

}