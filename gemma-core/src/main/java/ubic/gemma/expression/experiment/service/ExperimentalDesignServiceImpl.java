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
package ubic.gemma.expression.experiment.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService
 */
@Service
public class ExperimentalDesignServiceImpl extends ExperimentalDesignServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExperimentalDesignServiceBase#handleFind(ubic.gemma.model.expression.
     * experiment.ExperimentalDesign)
     */
    @Override
    protected ExperimentalDesign handleFind( ExperimentalDesign experimentalDesign ) {
        return this.getExperimentalDesignDao().find( experimentalDesign );
    }

    @Override
    protected ExperimentalDesign handleFindOrCreate( ExperimentalDesign experimentalDesign ) {
        return this.getExperimentalDesignDao().findOrCreate( experimentalDesign );
    }

    @Override
    protected ExpressionExperiment handleGetExpressionExperiment( ExperimentalDesign experimentalDesign ) {
        return this.getExperimentalDesignDao().getExpressionExperiment( experimentalDesign );
    }

    @Override
    protected ExperimentalDesign handleLoad( Long id ) {
        return this.getExperimentalDesignDao().load( id );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExperimentalDesignService#getExperimentalDesigns()
     */
    @Override
    protected java.util.Collection<ExperimentalDesign> handleLoadAll() {
        return ( Collection<ExperimentalDesign> ) this.getExperimentalDesignDao().loadAll();
    }

    @Override
    protected void handleUpdate( ExperimentalDesign experimentalDesign ) {
        this.getExperimentalDesignDao().update( experimentalDesign );
    }

}