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
package ubic.gemma.expression.experiment.service;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.expression.experiment.DatabaseBackedExpressionExperimentSetValueObject;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @version $Id$
 */
public interface ExpressionExperimentSetService {

    public static String AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION = "Automatically generated for %s EEs";

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
     * security at DAO level
     */
    public Collection<ExpressionExperimentSet> findByName( java.lang.String name );

    /**
     * Get analyses that use this set. Note that if this collection is not empty, modification of the
     * expressionexperimentset should be disallowed. Security at DAO level.
     */
    public Collection<ExpressionAnalysis> getAnalyses( ExpressionExperimentSet expressionExperimentSet );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperimentSet load( java.lang.Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> load( Collection<Long> ids );

    /**
     * Get the security-filtered list of experiments in a set. It is possible for the return to be empty even if the set
     * is not (due to security filters). Use this insead of expressionExperimentSet.getExperiments. Security at DAO
     * level.
     * 
     * @param id
     * @return
     */
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> loadAll();

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them. Security at DAO level.
     */
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets();

    /**
     * Security at DAO level.
     * 
     * @return ExpressionExperimentSets that have more than 1 experiment in them & have a taxon value.
     */
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon();

    /**
     * Security filtering is handled by the call to load the set entities
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.loadAllExperimentSetsWithTaxon()
     * 
     * @return ExpressionExperimentSets that have more than 1 experiment in them & have a taxon value.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
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
     * Security handled at DAO level.
     * 
     * @param expressionExperimentSet
     */
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
     * Get a value object for the id param
     * 
     * @param id
     * @return null if id doesn't match an experiment set
     */
    public DatabaseBackedExpressionExperimentSetValueObject getValueObject( Long id );

    /**
     * Get a value objects for the ids
     * 
     * @param ids
     * @return value objects or an empty set
     */
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> getValueObjectsFromIds( Collection<Long> ids );

    /**
     * Get the member experiment value objects for the set id
     * 
     * @param ids
     * @return value objects or an empty set
     */
    public Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id );

    /**
     * load the user's sets
     * 
     * @return
     */
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> loadMySetValueObjects();

    /**
     * Updates the database record for the param experiment set value object (permission permitting) with the members
     * specified of the set, not the name or description etc.
     * 
     * @param groupId
     * @param eeIds
     * @return
     */
    public String updateDatabaseEntityMembers( Long groupId, Collection<Long> eeIds );

    /**
     * Updates the database record for the param experiment set value object (permission permitting) with the value
     * object's name and description.
     * 
     * @param eeSetVO
     * @return
     */
    public DatabaseBackedExpressionExperimentSetValueObject updateDatabaseEntityNameDesc(
            DatabaseBackedExpressionExperimentSetValueObject eeSetVO );

    /**
     * create an entity in the database based on the value object parameter
     * 
     * @param eesvo
     * @return value object converted from the newly created entity
     */
    public DatabaseBackedExpressionExperimentSetValueObject createDatabaseEntity(
            ExpressionExperimentSetValueObject eesvo );

    /**
     * Security is handled within method, when the set is loaded
     */
    public void deleteDatabaseEntity( DatabaseBackedExpressionExperimentSetValueObject eesvo );

    /**
     * Update corresponding entity based on value object
     */
    public void updateDatabaseEntity( DatabaseBackedExpressionExperimentSetValueObject eesvo );

    public Collection<Long> getExperimentIdsInSet( Long id );

    /**
     * security at DAO level
     * 
     * @param bioAssaySet
     * @return
     */
    public Collection<Long> findIds( BioAssaySet bioAssaySet );

    public Collection<DatabaseBackedExpressionExperimentSetValueObject> getLightValueObjectsFromIds(
            Collection<Long> ids );

    public boolean isAutomaticallyGenerated( String experimentSetDescription );

    public ExpressionExperimentSet initAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon );

    public ExpressionExperimentSet updateAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon );
}
