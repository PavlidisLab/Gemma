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
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;

/**
 * @author paul
 */
public interface ExpressionExperimentSetService
        extends BaseVoEnabledService<ExpressionExperimentSet, ExpressionExperimentSetValueObject> {

    String AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION = "Automatically generated for %s EEs";

    @Secured({ "GROUP_USER" })
    ExpressionExperimentSet create( ExpressionExperimentSet expressionExperimentSet );

    @Secured({ "GROUP_USER" })
    ExpressionExperimentSet createFromValueObject( ExpressionExperimentSetValueObject eesvo );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( ExpressionExperimentSet expressionExperimentSet );

    /**
     * Security is handled within method, when the set is loaded
     */
    void deleteDatabaseEntity( ExpressionExperimentSetValueObject eesvo );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet );

    /**
     * security at DAO level
     */
    Collection<ExpressionExperimentSet> findByName( java.lang.String name );

    /**
     * security at DAO level
     */
    Collection<Long> findIds( BioAssaySet bioAssaySet );

    /**
     * Get the (security-filtered) list of experiments in a set.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> getExperimentsInSet( Long id );

    /**
     * Get the member experiment value objects for the set id; security filtered.
     *
     * @return value objects or an empty set
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id );

    ExpressionExperimentSet initAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon );

    boolean isAutomaticallyGenerated( String experimentSetDescription );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSet> load( Collection<Long> ids );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperimentSet load( java.lang.Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSet> loadAll();

    /**
     * Security at DAO level.
     *
     * @return ExpressionExperimentSets that have more than 1 experiment in them & have a taxon value.
     */
    Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon();

    /**
     * Security filtering is handled by the call to load the set entities
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.loadAllExperimentSetsWithTaxon()
     *
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     * @return ExpressionExperimentSets that have more than 1 experiment in them & have a taxon value.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentSetValueObject> loadAllExperimentSetValueObjects( boolean loadEEIds );

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them. Security at DAO level.
     */
    Collection<ExpressionExperimentSet> loadAllMultiExperimentSets();

    /**
     * @return sets belonging to current user -- only if they have more than one experiment!
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    Collection<ExpressionExperimentSet> loadMySets();

    /**
     * load the user's sets
     *
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentSetValueObject> loadMySetValueObjects( boolean loadEEIds );

    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    Collection<ExpressionExperimentSet> loadMySharedSets();

    /**
     * <p>
     * Load all ExpressionExperimentSets that belong to the given user.
     * </p>
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    java.util.Collection<ExpressionExperimentSet> loadUserSets( ubic.gemma.model.common.auditAndSecurity.User user );

    /**
     * Get a value object for the id param.
     *
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     * @return null if id doesn't match an experiment set
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    ExpressionExperimentSetValueObject loadValueObjectById( Long id, boolean loadEEIds );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( ExpressionExperimentSet expressionExperimentSet );

    ExpressionExperimentSet updateAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon );

    /**
     * Update corresponding entity based on value object
     */
    void updateDatabaseEntity( ExpressionExperimentSetValueObject eesvo );

    /**
     * Updates the database record for the param experiment set value object (permission permitting) with the members
     * specified of the set, not the name or description etc.
     */
    void updateDatabaseEntityMembers( Long groupId, Collection<Long> eeIds );

    /**
     * Updates the database record for the param experiment set value object (permission permitting) with the value
     * object's name and description.
     *
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     */
    ExpressionExperimentSetValueObject updateDatabaseEntityNameDesc( ExpressionExperimentSetValueObject eeSetVO,
            boolean loadEEIds );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    ExpressionExperimentSetValueObject loadValueObjectById( Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentSetValueObject> loadValueObjectsByIds( Collection<Long> eeSetIds,
            boolean loadEEIds );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentSetValueObject> loadValueObjectsByIds( Collection<Long> eeSetIds );
}
