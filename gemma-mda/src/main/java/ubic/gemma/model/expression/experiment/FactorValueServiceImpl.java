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
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.FactorValueService
 */
@Service
public class FactorValueServiceImpl extends ubic.gemma.model.expression.experiment.FactorValueServiceBase {

    public Collection<FactorValue> create( Collection<FactorValue> fvs ) {
        return ( Collection<FactorValue> ) this.getFactorValueDao().create( fvs );
    }

    public Collection<FactorValue> findByValue( String valuePrefix ) {
        return this.getFactorValueDao().findByValue( valuePrefix );
    }

    @Override
    protected FactorValue handleCreate( FactorValue factorValue ) throws Exception {
        return this.getFactorValueDao().create( factorValue );
    }

    @Override
    protected void handleDelete( FactorValue factorValue ) throws Exception {
        this.getFactorValueDao().remove( factorValue );
    }

    @Override
    protected FactorValue handleFindOrCreate( FactorValue factorValue ) throws Exception {
        return this.getFactorValueDao().findOrCreate( factorValue );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#getAllFactorValues()
     */
    protected java.util.Collection handleGetAllFactorValues() throws java.lang.Exception {
        return this.getFactorValueDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.FactorValueServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected FactorValue handleLoad( Long id ) throws Exception {
        return this.getFactorValueDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.FactorValueServiceBase#handleLoadAll()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getFactorValueDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueService#saveFactorValue(ubic.gemma.model.expression.experiment.FactorValue)
     */
    protected void handleSaveFactorValue( ubic.gemma.model.expression.experiment.FactorValue factorValue )
            throws java.lang.Exception {
        this.getFactorValueDao().create( factorValue );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.FactorValueServiceBase#handleUpdate(java.util.Collection)
     */
    @Override
    protected void handleUpdate( Collection factorValues ) throws Exception {
        this.getFactorValueDao().update( factorValues );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.FactorValueServiceBase#handleUpdate(ubic.gemma.model.expression.experiment
     * .FactorValue)
     */
    @Override
    protected void handleUpdate( FactorValue factorValue ) throws Exception {
        this.getFactorValueDao().update( factorValue );
    }

}