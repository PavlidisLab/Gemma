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
package ubic.gemma.model.analysis.expression;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public interface ExpressionExperimentSetService {

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ExpressionExperimentSet create( ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void delete( ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> findByName( java.lang.String name );

    /**
     * Get analyses that use this set. Note that if this collection is not empty, modification of the
     * expressionexperimentset should be disallowed.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionAnalysis> getAnalyses( ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperimentSet load( java.lang.Long id );

    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> load( Collection<Long> ids );

    /**
     * Get the security-filtered list of experiments in a set. It is possible for the return to be empty even if the set
     * is not (due to security filters). Use this insead of expressionExperimentSet.getExperiments.
     * 
     * @param id
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> loadAll();
    
    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets();

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon();

    /**
     * @return sets belonging to current user -- only if they have more than one experiment!
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<ExpressionExperimentSet> loadMySets();

    /**
     * <p>
     * Load all ExpressionExperimentSets that belong to the given user.
     * </p>
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperimentSet> loadUserSets(
            ubic.gemma.model.common.auditAndSecurity.User user );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExpressionExperimentSet expressionExperimentSet );

    /**
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    public Collection<ExpressionExperimentSet> loadMySharedSets();

    /**
     * @param expressionExperimentSet
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public void thaw( ExpressionExperimentSet expressionExperimentSet );
    

    /**
     * Checks if the EE set can be exposed to the front end
     * 
     * For example:
     * some experiment sets were implicitly created when analyses were run (bug 2323)
     * these sets were created before the taxon field was added, so in many (all?) cases they do not have a taxon value
     * these sets should not be exposed to the front end since they are mostly useless will just add clutter
     * also, not having a taxon causes serious problems in our taxon-centric handling of sets
     * in the case of these EE sets, this method would return false
     * 
     * @param expressionExperimentSet
     * @return false if the set should not be shown on the front end (for example, if the set has a null taxon),
     * true otherwise
     */
    public boolean isValidForFrontEnd( ExpressionExperimentSet expressionExperimentSet );

    /**
     * Returns only the experiment sets that are valid for the front end
     * uses ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.isValidForFrontEnd(ExpressionExperimentSet)
     * @param eeSets
     * @return
     */
    public Collection<ExpressionExperimentSet> validateForFrontEnd( Collection<ExpressionExperimentSet> eeSets );

}
