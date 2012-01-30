/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import org.springframework.stereotype.Service;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService
 */
@Service
public class ExperimentalDesignServiceImpl extends ubic.gemma.model.expression.experiment.ExperimentalDesignServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalDesignServiceBase#handleCreate(ubic.gemma.model.expression
     * .experiment.ExperimentalDesign)
     */
    @Override
    protected ExperimentalDesign handleCreate( ExperimentalDesign experimentalDesign ) throws Exception {
        return this.getExperimentalDesignDao().create( experimentalDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExperimentalDesignServiceBase#handleFind(ubic.gemma.model.expression.
     * experiment.ExperimentalDesign)
     */
    @Override
    protected ExperimentalDesign handleFind( ExperimentalDesign experimentalDesign ) throws Exception {
        return this.getExperimentalDesignDao().find( experimentalDesign );
    }

    @Override
    protected ExperimentalDesign handleFindByName( String name ) throws Exception {
        return this.getExperimentalDesignDao().findByName( name );
    }

    @Override
    protected ExperimentalDesign handleFindOrCreate( ExperimentalDesign experimentalDesign ) throws Exception {
        return this.getExperimentalDesignDao().findOrCreate( experimentalDesign );
    }

    @Override
    protected ExpressionExperiment handleGetExpressionExperiment( ExperimentalDesign experimentalDesign )
            throws Exception {
        return this.getExperimentalDesignDao().getExpressionExperiment( experimentalDesign );
    }

    @Override
    protected ExperimentalDesign handleLoad( Long id ) throws Exception {
        return this.getExperimentalDesignDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#getExperimentalDesigns()
     */
    @Override
    protected java.util.Collection<ExperimentalDesign> handleLoadAll() throws java.lang.Exception {
        return ( Collection<ExperimentalDesign> ) this.getExperimentalDesignDao().loadAll();
    }

    @Override
    protected void handleUpdate( ExperimentalDesign experimentalDesign ) throws Exception {
        this.getExperimentalDesignDao().update( experimentalDesign );
    }

}