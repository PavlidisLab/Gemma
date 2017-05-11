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
package ubic.gemma.persistence.service.expression.experiment;

import java.util.Collection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * @author pavlidis
 * @see ExperimentalFactorService
 */
@Service
public class ExperimentalFactorServiceImpl
        extends ExperimentalFactorServiceBase {

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<ExperimentalFactor> load( Collection<Long> ids ) {
        return ( Collection<ExperimentalFactor> ) this.getExperimentalFactorDao().load( ids );
    }

    @Override
    protected ExperimentalFactor handleCreate( ExperimentalFactor experimentalFactor ) {
        return this.getExperimentalFactorDao().create( experimentalFactor );
    }

    @Override
    protected void handleDelete( ExperimentalFactor experimentalFactor ) {

        /*
         * First, check to see if there are any diff results that use this factor.
         */
        Collection<DifferentialExpressionAnalysis> analyses = getDifferentialExpressionAnalysisDao()
                .findByFactor( experimentalFactor );
        for ( DifferentialExpressionAnalysis a : analyses ) {
            getDifferentialExpressionAnalysisDao().remove( a );
        }

        this.getExperimentalFactorDao().remove( experimentalFactor );

    }

    @Override
    protected ExperimentalFactor handleFind( ExperimentalFactor experimentalFactor ) {
        return this.getExperimentalFactorDao().find( experimentalFactor );
    }

    @Override
    protected ExperimentalFactor handleFindOrCreate( ExperimentalFactor experimentalFactor ) {
        return this.getExperimentalFactorDao().findOrCreate( experimentalFactor );
    }

    @Override
    protected ExperimentalFactor handleLoad( Long id ) {
        return this.getExperimentalFactorDao().load( id );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Collection<ExperimentalFactor> handleLoadAll() {
        return ( Collection<ExperimentalFactor> ) this.getExperimentalFactorDao().loadAll();
    }

    @Override
    protected void handleUpdate( ExperimentalFactor experimentalFactor ) {
        this.getExperimentalFactorDao().update( experimentalFactor );
    }

}