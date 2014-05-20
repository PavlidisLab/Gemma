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
     * @param expressionExperimentSet
     * @return
     */
    @Secured({ "GROUP_USER" })
    public ExpressionExperimentSet create( ExpressionExperimentSet expressionExperimentSet );

    /**
     * @param eesvo
     * @return
     */
    @Secured({ "GROUP_USER" })
    public ExpressionExperimentSet createFromValueObject( ExpressionExperimentSetValueObject eesvo );

    /**
     * @param expressionExperimentSet
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void delete( ExpressionExperimentSet expressionExperimentSet );

    /**
     * Security is handled within method, when the set is loaded
     */
    public void deleteDatabaseEntity( ExpressionExperimentSetValueObject eesvo );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet );

    /**
     * security at DAO level
     */
    public Collection<ExpressionExperimentSet> findByName( java.lang.String name );

    /**
     * security at DAO level
     * 
     * @param bioAssaySet
     * @return
     */
    public Collection<Long> findIds( BioAssaySet bioAssaySet );

    // /**
    // * Get analyses that use this set. Note that if this collection is not empty, modification of the
    // * expressionexperimentset should be disallowed.
    // */
    // public Collection<ExpressionAnalysis> getAnalyses( ExpressionExperimentSet expressionExperimentSet );

    /**
     * Get the (security-filtered) list of experiments in a set.
     * 
     * @param id
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id );

    /**
     * Get the member experiment value objects for the set id; security filtered.
     * 
     * @param ids
     * @return value objects or an empty set
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id );

    public ExpressionExperimentSet initAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon );

    public boolean isAutomaticallyGenerated( String experimentSetDescription );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> load( Collection<Long> ids );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperimentSet load( java.lang.Long id );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> loadAll();

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
    public Collection<ExpressionExperimentSetValueObject> loadAllExperimentSetValueObjects();

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them. Security at DAO level.
     */
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets();

    /**
     * @return sets belonging to current user -- only if they have more than one experiment!
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<ExpressionExperimentSet> loadMySets();

    /**
     * load the user's sets
     * 
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public Collection<ExpressionExperimentSetValueObject> loadMySetValueObjects();

    /**
     * @return
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    public Collection<ExpressionExperimentSet> loadMySharedSets();

    /**
     * <p>
     * Load all ExpressionExperimentSets that belong to the given user.
     * </p>
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> loadUserSets(
            ubic.gemma.model.common.auditAndSecurity.User user );

    /**
     * Get a value object for the id param. The experimentIds are not filled in.
     * 
     * @param id
     * @return null if id doesn't match an experiment set
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    public ExpressionExperimentSetValueObject loadValueObject( Long id );

    /**
     * Get value objects for the given ids. The experimentIds are not filled in.
     * 
     * @param eeSetIds
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public Collection<ExpressionExperimentSetValueObject> loadValueObjects( Collection<Long> eeSetIds );

    /**
     * Security handled at DAO level.
     * 
     * @param expressionExperimentSet
     */
    public void thaw( ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExpressionExperimentSet expressionExperimentSet );

    public ExpressionExperimentSet updateAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon );

    /**
     * Update corresponding entity based on value object
     */
    public void updateDatabaseEntity( ExpressionExperimentSetValueObject eesvo );

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
    public ExpressionExperimentSetValueObject updateDatabaseEntityNameDesc( ExpressionExperimentSetValueObject eeSetVO );

}
