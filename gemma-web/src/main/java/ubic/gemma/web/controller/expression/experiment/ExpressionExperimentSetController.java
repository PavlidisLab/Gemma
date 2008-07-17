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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
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
 * @spring.property name="taxonService" ref="taxonService"
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentSetController extends BaseFormController {
    private ExpressionExperimentSetService expressionExperimentSetService;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private ExpressionExperimentService expressionExperimentService;
    private PersisterHelper persisterHelper;
    private TaxonService taxonService;

    /**
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    public Long create( ExpressionExperimentSetValueObject obj ) {

        if ( obj.getId() != null && obj.getId() >= 0 ) {
            throw new IllegalArgumentException( "Should not provide an id for 'create': " + obj.getId() );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        /*
         * Sanity check.
         */
        if ( expressionExperimentService.findByName( obj.getName() ) != null ) {
            throw new IllegalArgumentException( "Sorry, there is already a set with that name (" + obj.getName() + ")" );
        }

        ExpressionExperimentSet newSet = ExpressionExperimentSet.Factory.newInstance();
        newSet.setName( obj.getName() );
        newSet.setDescription( obj.getDescription() );
        newSet.setTaxon( taxonService.load( obj.getTaxonId() ) );

        if ( newSet.getTaxon() == null ) {
            throw new IllegalArgumentException( "No such taxon with id=" + obj.getTaxonId() );
        }

        Collection<? extends BioAssaySet> datasetsAnalyzed = expressionExperimentService.loadMultiple( obj
                .getExpressionExperimentIds() );

        newSet.getExperiments().addAll( datasetsAnalyzed );

        if ( newSet.getExperiments().size() < 2 ) {
            throw new IllegalArgumentException( "Attempt to create an ExpressionExperimentSet with only "
                    + newSet.getExperiments().size() + ", must have at least 2" );
        }
        ExpressionExperimentSet newAnalysis = ( ExpressionExperimentSet ) persisterHelper.persist( newSet );
        return newAnalysis.getId();
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
            vo.setTaxonId( set.getTaxon().getId() );
            vo.setTaxonName( set.getTaxon().getCommonName() ); // If I don't do this, won't be populated in the
            // downstream object. This is
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

    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentValueObject> getExperimentsInSet( Long id ) {
        Collection<Long> eeids = getExperimentIdsInSet( id );
        Collection<ExpressionExperimentValueObject> result = expressionExperimentService.loadValueObjects( eeids );
        populateAnalyses( eeids, result );
        return result;
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
            throw new IllegalArgumentException( "Cannot delete eeset with id=" + id );
        }
        ExpressionExperimentSet toDelete = expressionExperimentSetService.load( id );
        if ( toDelete == null ) {
            throw new IllegalArgumentException( "No such eeset id=" + id );
        }

        if ( expressionExperimentSetService.getAnalyses( toDelete ).size() > 0 ) {
            throw new IllegalArgumentException( "Sorry, can't delete this set, it is associated with active analyses." );
        }

        try {
            expressionExperimentSetService.delete( toDelete );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return true;
    }

    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
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

        boolean needUpdate = updateExperimentsInSet( obj, toUpdate );

        /*
         * Allow updating of the name & description.
         */
        if ( !obj.getName().equals( toUpdate.getName() ) ) {
            toUpdate.setName( obj.getName() );
            needUpdate = true;
        }

        if ( !obj.getDescription().equals( toUpdate.getDescription() ) ) {
            toUpdate.setDescription( obj.getDescription() );
            needUpdate = true;
        }

        if ( needUpdate ) {
            if ( toUpdate.getExperiments().size() < 2 ) {
                throw new IllegalArgumentException( "Attempt to update an ExpressionExperimentSet so it has only "
                        + toUpdate.getExperiments().size() + ", must have at least 2" );
            }
            expressionExperimentSetService.update( toUpdate );
            log.info( "Updated " + obj.getName() );
        } else {
            log.info( "No changes found for " + obj.getName() );
        }

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
     * Check if the user has requested a change in membership; if so, check if the set can be safely modified.
     * 
     * @param obj
     * @param toUpdate
     * @return true if the set of experiments has changed, false otherwise.
     * @throws IllegalArgumentException if the set cannot be modified becasue it is is associated with an analysis
     *         object.
     */
    @SuppressWarnings("unchecked")
    private boolean updateExperimentsInSet( ExpressionExperimentSetValueObject obj, ExpressionExperimentSet toUpdate ) {

        Collection<Long> idsInExistingSet = this.getExperimentIdsInSet( obj.getId() );
        boolean membersAreTheSame = idsInExistingSet.containsAll( obj.getExpressionExperimentIds() )
                && obj.getExpressionExperimentIds().containsAll( idsInExistingSet );
        /*
         * If there is an existing analysis, we have to disallow alteration of the set. Warn the user if they are
         * attempting to do this.
         */
        if ( !membersAreTheSame && expressionExperimentSetService.getAnalyses( toUpdate ).size() > 0 ) {
            throw new IllegalArgumentException(
                    "Sorry, you can't update members of this set, it is associated with active analyses." );
        }

        if ( membersAreTheSame ) {
            return false;
        } else {
            Collection<? extends BioAssaySet> datasetsAnalyzed = expressionExperimentService.loadMultiple( obj
                    .getExpressionExperimentIds() );
            toUpdate.getExperiments().retainAll( datasetsAnalyzed );
            toUpdate.getExperiments().addAll( datasetsAnalyzed );
            /*
             * Check that all the datasets match the given taxon.
             */
            for ( BioAssaySet ee : toUpdate.getExperiments() ) {
                Taxon t = expressionExperimentService.getTaxon( ee.getId() );
                if ( !t.equals( toUpdate.getTaxon() ) ) {
                    throw new IllegalArgumentException( "You cannot add a " + t.getCommonName() + " dataset to a "
                            + toUpdate.getTaxon().getCommonName() + " set" );
                }
            }
            return true;
        }
    }

}
