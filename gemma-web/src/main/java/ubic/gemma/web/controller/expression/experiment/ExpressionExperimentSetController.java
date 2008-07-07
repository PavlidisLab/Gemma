/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.web.controller.BaseFormController;

/**
 * For fetching and manipulating ExpressionExperimentSets
 * 
 * @spring.bean id="expressionExperimentSetController"
 * @spring.property name="expressionExperimentSetService" ref="expressionExperimentSetService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentSetController extends BaseFormController {
    private ExpressionExperimentSetService expressionExperimentSetService;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private ExpressionExperimentService expressionExperimentService;
    private PersisterHelper persisterHelper;

    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentValueObject> getExperimentsInSet( Long id ) {
        Collection<Long> eeids = getExperimentIdsInSet( id );
        Collection<ExpressionExperimentValueObject> result = expressionExperimentService.loadValueObjects( eeids );
        populateAnalyses( eeids, result );
        return result;
    }

    /**
     * Fill in information about analyses done on the experiments.
     * 
     * @param result
     */
    @SuppressWarnings("unchecked")
    private void populateAnalyses( Collection<Long> eeids, Collection<ExpressionExperimentValueObject> result ) {
        Map<Long, DifferentialExpressionAnalysis> analysisMap = differentialExpressionAnalysisService
                .findByInvestigationIds( eeids );
        for ( ExpressionExperimentValueObject eevo : result ) {
            if ( !analysisMap.containsKey( eevo.getId() ) ) {
                continue;
            }
            eevo.setDifferentialExpressionAnalysisId( analysisMap.get( eevo.getId() ).getId() );
        }
    }

    /**
     * @param id
     * @return
     */
    public Collection<Long> getExperimentIdsInSet( Long id ) {
        ExpressionExperimentSet eeSet = expressionExperimentSetService.load( id ); // secure
        Collection<BioAssaySet> datasets = eeSet.getExperiments(); // Not secure.
        Collection<Long> eeids = new HashSet<Long>();
        for ( BioAssaySet ee : datasets ) {
            eeids.add( ee.getId() );
        }
        return eeids;
    }

    /**
     * @return all available sets that have at least 2 experiments.
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentSetValueObject> getAvailableExpressionExperimentSets() {
        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.loadAll(); // filtered by security.
        Collection<ExpressionExperimentSetValueObject> results = new HashSet<ExpressionExperimentSetValueObject>();
        for ( ExpressionExperimentSet set : sets ) {

            int size = set.getExperiments().size();
            if ( size < 2 ) continue; // Ignore sets of size = 1 because there are many!

            ExpressionExperimentSetValueObject vo = new ExpressionExperimentSetValueObject();
            vo.setName( set.getName() );
            vo.setId( set.getId() );
            vo.setTaxon( set.getTaxon() );

            vo.getTaxon().toString(); // If I don't do this, won't be populated in the downstream object. This is
            // basically a thaw.

            vo.setDescription( set.getDescription() == null ? "" : set.getDescription() );
            if ( expressionExperimentSetService.getAnalyses( set ).size() > 0 ) {
                vo.setModifiable( false );
            }
            for ( BioAssaySet ee : set.getExperiments() ) {
                vo.getExpressionExperimentIds().add( ee.getId() );
            }

            vo.setNumExperiments( size );
            results.add( vo );
        }
        return results;
    }

    /**
     * @param obj
     */
    @SuppressWarnings("unchecked")
    public void update( ExpressionExperimentSetValueObject obj ) {

        if ( obj.getId() == null ) {
            throw new IllegalArgumentException( "Can only update an existing eeset (passed id=" + obj.getId() + ")" );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        ExpressionExperimentSet toUpdate = expressionExperimentSetService.load( obj.getId() );

        if ( toUpdate == null ) {
            throw new IllegalArgumentException( "No such set with id = " + obj.getId() );
        }

        if ( expressionExperimentSetService.getAnalyses( toUpdate ).size() > 0 ) {
            throw new IllegalArgumentException( "Sorry, can't update this analysis" );
        }

        toUpdate.setName( obj.getName() );
        toUpdate.setDescription( obj.getDescription() );

        Collection<? extends BioAssaySet> datasetsAnalyzed = expressionExperimentService.loadMultiple( obj
                .getExpressionExperimentIds() );
        toUpdate.getExperiments().retainAll( datasetsAnalyzed );
        toUpdate.getExperiments().addAll( datasetsAnalyzed );

        expressionExperimentSetService.update( toUpdate );

        log.info( "Updated " + obj.getName() );

    }

    /**
     * Delete a EEset from the system. NOTE this will fail if it has analyses associated with it.
     * 
     * @param obj
     * @return true if it was deleted.
     */
    public boolean remove( ExpressionExperimentSetValueObject obj ) {
        Long id = obj.getId();
        if ( id == null || id < 0 ) {
            log.warn( "Cannot delete eeset with id=" + id );
            return false;
        }
        ExpressionExperimentSet expressionExperimentSet = expressionExperimentSetService.load( id );
        if ( expressionExperimentSet == null ) {
            log.warn( "No such eeset id=" + id );
            return false;
        }
        try {
            expressionExperimentSetService.delete( expressionExperimentSet );
        } catch ( Exception e ) {
            log.warn( e, e );
            return false;
        }
        return true;
    }

    /**
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    public Long create( ExpressionExperimentSetValueObject obj ) {

        if ( obj.getId() != null ) {
            throw new IllegalArgumentException( "Should not provide an id for 'create': " + obj.getId() );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        ExpressionExperimentSet va = ExpressionExperimentSet.Factory.newInstance();
        va.setName( obj.getName() );
        va.setDescription( obj.getDescription() );

        Collection<? extends BioAssaySet> datasetsAnalyzed = expressionExperimentService.loadMultiple( obj
                .getExpressionExperimentIds() );

        ExpressionExperimentSet eeSet = ExpressionExperimentSet.Factory.newInstance();
        eeSet.getExperiments().addAll( datasetsAnalyzed );

        ExpressionExperimentSet newAnalysis = ( ExpressionExperimentSet ) persisterHelper.persist( va );
        return newAnalysis.getId();
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

}
