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

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService
 */
@Service
public class ExperimentalFactorServiceImpl extends ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase {

    public Collection<ExperimentalFactor> create( Collection<ExperimentalFactor> factors ) {
        return ( Collection<ExperimentalFactor> ) this.getExperimentalFactorDao().create( factors );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleCreate(ubic.gemma.model.expression
     * .experiment.ExperimentalFactor)
     */
    @Override
    protected ExperimentalFactor handleCreate( ExperimentalFactor experimentalFactor ) throws Exception {
        return this.getExperimentalFactorDao().create( experimentalFactor );
    }

    @Override
    protected void handleDelete( ExperimentalFactor experimentalFactor ) throws Exception {
        this.getExperimentalFactorDao().remove( experimentalFactor );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleFindOrcreate(ubic.gemma.model.expression
     * .experiment.ExperimentalFactor)
     */
    @Override
    protected ExperimentalFactor handleFind( ExperimentalFactor experimentalFactor ) throws Exception {
        return this.getExperimentalFactorDao().find( experimentalFactor );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleFindOrcreate(ubic.gemma.model.expression
     * .experiment.ExperimentalFactor)
     */
    @Override
    protected ExperimentalFactor handleFindOrCreate( ExperimentalFactor experimentalFactor ) throws Exception {
        return this.getExperimentalFactorDao().findOrCreate( experimentalFactor );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected ExperimentalFactor handleLoad( Long id ) throws Exception {
        return this.getExperimentalFactorDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalFactorService#getAllExperimentalFactors()
     */
    @Override
    protected java.util.Collection<ExperimentalFactor> handleLoadAll() throws java.lang.Exception {
        return ( Collection<ExperimentalFactor> ) this.getExperimentalFactorDao().loadAll();
    }

    @Override
    protected void handleUpdate( ExperimentalFactor experimentalFactor ) throws Exception {
        this.getExperimentalFactorDao().update( experimentalFactor );
    }

}