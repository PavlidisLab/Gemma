/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.expression.experiment;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * @author paul
 * @version $Id$
 */
public interface ExpressionExperimentSetService {

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public ExpressionExperimentSet create( ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void delete( ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> findByName( java.lang.String name );

    /**
     * Get analyses that use this set. Note that if this collection is not empty, modification of the
     * expressionexperimentset should be disallowed.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionAnalysis> getAnalyses( ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperimentSet load( java.lang.Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> load( Collection<Long> ids );

    /**
     * Get the security-filtered list of experiments in a set. It is possible for the return to be empty even if the set
     * is not (due to security filters). Use this insead of expressionExperimentSet.getExperiments.
     * 
     * @param id
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> loadAll();

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets();

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them & have a taxon value.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon();
    
    /**
     * Security filtering is handled by the call to load the set entities
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.loadAllExperimentSetsWithTaxon()
     * @return ExpressionExperimentSets that have more than 1 experiment in them & have a taxon value.
     */
    public Collection<ExpressionExperimentSetValueObject> loadAllExperimentSetValueObjectsWithTaxon();

    /**
     * @return sets belonging to current user -- only if they have more than one experiment!
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<ExpressionExperimentSet> loadMySets();

    /**
     * <p>
     * Load all ExpressionExperimentSets that belong to the given user.
     * </p>
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> loadUserSets(
            ubic.gemma.model.common.auditAndSecurity.User user );

    /**
     * 
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExpressionExperimentSet expressionExperimentSet );

    /**
     * @return
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    public Collection<ExpressionExperimentSet> loadMySharedSets();

    /**
     * @param expressionExperimentSet
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public void thaw( ExpressionExperimentSet expressionExperimentSet );

    /**
     * Checks if the EE set can be exposed to the front end For example: some experiment sets were implicitly created
     * when analyses were run (bug 2323) these sets were created before the taxon field was added, so in many (all?)
     * cases they do not have a taxon value these sets should not be exposed to the front end since they are mostly
     * useless will just add clutter also, not having a taxon causes serious problems in our taxon-centric handling of
     * sets in the case of these EE sets, this method would return false
     * 
     * @param expressionExperimentSet
     * @return false if the set should not be shown on the front end (for example, if the set has a null taxon), true
     *         otherwise
     */
    public boolean isValidForFrontEnd( ExpressionExperimentSet expressionExperimentSet );

    /**
     * Returns only the experiment sets that are valid for the front end uses
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.isValidForFrontEnd(ExpressionExperimentSet)
     * 
     * @param eeSets
     * @return
     */
    public Collection<ExpressionExperimentSet> validateForFrontEnd( Collection<ExpressionExperimentSet> eeSets );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet );

    /**
     * Load a light-weight representation of the value objects for the given IDs (the IDs must be filtered for security
     * _first_). The sizes of the set, but not the set members, are loaded.
     * 
     * @param ids
     * @return
     */
    public Collection<ExpressionExperimentSetValueObject> loadLightValueObjects( Collection<Long> ids );

    /**
     * No security filtering is done here, assuming that if the user could load the experimentSet entity, they have access to it
     * @param set an expressionExperimentSet entity to create a value object for
     * @return
     */    
    public ExpressionExperimentSetValueObject convertToValueObject( ExpressionExperimentSet set );
    
    /**
     * Get a value object for the id param
     * @param id
     * @return null if id doesn't match an experiment set
     */
    public ExpressionExperimentSetValueObject getValueObject( Long id );
    
    /**
     * Get a value object for the id param
     * @param id
     * @return null if id doesn't match an experiment set
     */    
    public Collection<ExpressionExperimentSetValueObject> getValueObjects( Collection<ExpressionExperimentSet> sets );
    /**
     * Get a value objects for the ids
     * @param ids
     * @return value objects or an empty set
     */    
    public Collection<ExpressionExperimentSetValueObject> getValueObjectsFromIds( Collection<Long> ids );

    /**
     * Get the member experiment value objects for the set id
     * @param ids
     * @return value objects or an empty set
     */        
    public Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id );

    /**
     * load the user's sets
     * @return
     */
    public Collection<ExpressionExperimentSetValueObject> loadMySetValueObjects();

    /**
     * Updates the database record for the param experiment set value object 
     * (permission permitting) with the members specified of the set, not the 
     * name or description etc.
     * @param groupId
     * @param eeIds
     * @return
     */
    public String updateMembers( Long groupId, Collection<Long> eeIds );
    
    /**
     * Updates the database record for the param experiment set value object 
     * (permission permitting) with the value object's name and description.
     * @param eeSetVO
     * @return
     */
    public DatabaseBackedExpressionExperimentSetValueObject updateNameDesc(
            DatabaseBackedExpressionExperimentSetValueObject eeSetVO );
    
    /**
     * create an entity in the database based on the value object parameter
     * 
     * @param eesvo
     * @return value object converted from the newly created entity
     */
    public ExpressionExperimentSetValueObject create( ExpressionExperimentSetValueObject eesvo );
    
    /**
     * Security is handled within method, when the set is loaded
     */
    public void delete( ExpressionExperimentSetValueObject eesvo );

    /**
     * Update corresponding entity based on value object
     */
    public void update( ExpressionExperimentSetValueObject eesvo );
}
