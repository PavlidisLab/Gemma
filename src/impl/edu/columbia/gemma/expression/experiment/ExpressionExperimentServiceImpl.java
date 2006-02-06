/*
 * The Gemma project.
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
package edu.columbia.gemma.expression.experiment;

import java.util.Collection;

import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.description.DatabaseEntry;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService
 */
public class ExpressionExperimentServiceImpl extends
        edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase {

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase#handleFindByInvestigator(edu.columbia.gemma.common.auditAndSecurity.Contact)
     */
    @Override
    protected Collection handleFindByInvestigator( Contact investigator ) throws Exception {
        return this.getExpressionExperimentDao().findByInvestigator( investigator );
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase#handleFindByAccession(edu.columbia.gemma.common.description.DatabaseEntry)
     */
    @Override
    protected ExpressionExperiment handleFindByAccession( DatabaseEntry accession ) throws Exception {
        return this.getExpressionExperimentDao().findByAccession( accession );
    }

    @Override
    protected ExpressionExperiment handleFindByName( String name ) throws Exception {
        return this.getExpressionExperimentDao().findByName( name );
    }

    @Override
    protected ExpressionExperiment handleFindOrCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().findOrCreate( expressionExperiment );
    }

    @Override
    protected void handleUpdate( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().update( expressionExperiment );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase#handleGetAllExpressionExperiments()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getExpressionExperimentDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase#handleDelete(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected void handleDelete( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().remove( expressionExperiment );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase#handleRead(java.lang.Long)
     */
    @Override
    protected ExpressionExperiment handleRead( Long id ) throws Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase#handleCreate(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().create( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase#handleFind(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleFind( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().find( expressionExperiment );
    }
}