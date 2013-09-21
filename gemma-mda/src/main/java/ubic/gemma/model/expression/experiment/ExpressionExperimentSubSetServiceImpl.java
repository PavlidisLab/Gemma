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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService
 */
@Service
public class ExpressionExperimentSubSetServiceImpl extends ExpressionExperimentSubSetServiceBase {

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao;

    @Autowired
    private ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    @Autowired
    private ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao;

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#saveExpressionExperimentSubSet(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    protected ExpressionExperimentSubSet handleCreate(
            ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        return this.getExpressionExperimentSubSetDao().create( expressionExperimentSubSet );
    }

    /**
     * Loads one subset, given an id
     * 
     * @return ExpressionExperimentSubSet
     */
    @Override
    protected ExpressionExperimentSubSet handleLoad( Long id ) {
        return this.getExpressionExperimentSubSetDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#getAllExpressionExperimentSubSets()
     */
    @Override
    protected Collection<ExpressionExperimentSubSet> handleLoadAll() {
        return ( Collection<ExpressionExperimentSubSet> ) this.getExpressionExperimentSubSetDao().loadAll();
    }

    @Override
    public ExpressionExperimentSubSet findOrCreate( ExpressionExperimentSubSet entity ) {
        return this.getExpressionExperimentSubSetDao().findOrCreate( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet find( ExpressionExperimentSubSet entity ) {
        return this.getExpressionExperimentSubSetDao().find( entity );
    }

    @Override
    @Transactional
    public void delete( ExpressionExperimentSubSet entity ) {
        this.handleDelete( entity );
    }

    /**
     * doesn't include removal of sample coexpression matrices, PCA, probe2probe coexpression links, or adjusting
     * experiment set members
     * 
     * @param subset
     * @throws Exception
     */
    protected void handleDelete( ExpressionExperimentSubSet subset ) {

        if ( subset == null ) {
            throw new IllegalArgumentException( "ExperimentSubSet cannot be null" );
        }

        /*
         * If we remove the experiment from the set, analyses that used the set have to cope with this. For G2G,the data
         * sets are stored in order of IDs, but the actual ids are not stored (we refer back to the eeset), so coping
         * will not be possible (at best we can mark it as troubled). If there is no analysis object using the set, it's
         * okay. There are ways around this but it's messy, so for now we just refuse to delete such experiments.
         */
        Collection<GeneCoexpressionAnalysis> g2gAnalyses = this.geneCoexpressionAnalysisDao
                .findByInvestigation( subset );

        if ( g2gAnalyses.size() > 0 ) {
            throw new IllegalArgumentException( "Sorry, you can't delete subset: " + subset
                    + "; it is part of at least one coexpression meta analysis: "
                    + g2gAnalyses.iterator().next().getName() );
        }

        // Remove differential expression analyses
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.differentialExpressionAnalysisDao
                .findByInvestigation( subset );
        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {
            Long toDelete = de.getId();
            this.differentialExpressionAnalysisDao.remove( toDelete );
        }

        // Remove probe2probe links
        this.probe2ProbeCoexpressionDao.deleteLinks( subset );

        this.expressionExperimentSubSetDao.remove( subset );
    }

}