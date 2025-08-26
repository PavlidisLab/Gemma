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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author paul
 */
public interface ExpressionExperimentSetService
        extends BaseService<ExpressionExperimentSet>, BaseVoEnabledService<ExpressionExperimentSet, ExpressionExperimentSetValueObject> {

    @Override
    @Secured({ "GROUP_USER" })
    ExpressionExperimentSet create( ExpressionExperimentSet expressionExperimentSet );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSet> load( Collection<Long> ids );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperimentSet load( Long id );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSet> loadAll();

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( ExpressionExperimentSet expressionExperimentSet );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( ExpressionExperimentSet expressionExperimentSet );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSet> find( ExpressionExperiment ee );

    /**
     * security at DAO level
     *
     * @param name name
     * @return collection of ee sets
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSet> findByName( String name );

    /**
     * security at DAO level
     *
     * @param ee BA set
     * @return collection of IDs
     */
    Collection<Long> findIds( ExpressionExperiment ee );

    /**
     * Get the (security-filtered) list of experiments in a set.
     *
     * @param id id
     * @return collection of ees
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> getExperimentsInSet( Long id );

    /**
     * Get the member experiment value objects for the set id; security filtered.
     *
     * @param id id
     * @return value objects or an empty set
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentDetailsValueObject> getExperimentValueObjectsInSet( Long id );

    ExpressionExperimentSet initAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon );

    boolean isAutomaticallyGenerated( String experimentSetDescription );

    /**
     * Security at DAO level.
     *
     * @return ExpressionExperimentSets that have more than 1 experiment in them &amp; have a taxon value.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon();

    /**
     * Security filtering is handled by the call to load the set entities
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.loadAllExperimentSetsWithTaxon()
     *
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     * @return ExpressionExperimentSets that have more than 1 experiment in them &amp; have a taxon value.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentSetValueObject> loadAllExperimentSetValueObjects( boolean loadEEIds );

    /**
     * load the user's sets
     *
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     * @return colelction of EE set VOs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentSetValueObject> loadMySetValueObjects( boolean loadEEIds );

    /**
     * Get a value object for the id param.
     *
     * @param id        ID
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     * @return null if id doesn't match an experiment set
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    ExpressionExperimentSetValueObject loadValueObjectById( Long id, boolean loadEEIds );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    ExpressionExperimentSetValueObject loadValueObjectById( Long id );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentSetValueObject> loadValueObjectsByIds( Collection<Long> eeSetIds );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperimentSet thaw( ExpressionExperimentSet set );

    /**
     * Remove an experiment from all sets it is in.
     * <p>
     * Using this allows one to bypass the requirement of "owning edit rights" on the set as long as the current user
     * has the right to edit the experiment itself.
     * <p>
     * If a set ends-up empty as a result, it is removed as well.
     * @return the number of sets this experiment was removed from
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_EDIT" })
    int removeFromSets( ExpressionExperiment bas );
}
