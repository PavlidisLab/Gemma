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
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author kelsey
 */
public interface FactorValueService extends BaseService<FactorValue>, FilteringVoEnabledService<FactorValue, FactorValueValueObject> {

    @Deprecated
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<FactorValue> findByValue( String valuePrefix, int maxResults );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    FactorValue findOrCreate( FactorValue factorValue );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    FactorValue load( Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    FactorValue loadWithExperimentalFactor( Long id );

    /**
     * Load a {@link FactorValue} with an initialized experimental factor or fail.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    FactorValue loadWithExperimentalFactorOrFail( Long id );

    /**
     * Load a {@link FactorValue} along with its old-style characteristics.
     * @deprecated do not use this, it is only meant for the purpose of migrating old-style characteristics to
     *             statements
     */
    @Nullable
    @Deprecated
    @Secured({ "GROUP_ADMIN" })
    // FIXME: use @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" }), but some FVs have broken ACLs
    FactorValue loadWithOldStyleCharacteristics( Long id, boolean readOnly );

    /**
     * @see FactorValueDao#loadIdsWithNumberOfOldStyleCharacteristics(Set)
     * @deprecated do not use, this is only for migrating old-style characteristics to statements and will be removed
     */
    @Deprecated
    @Secured({ "GROUP_ADMIN" })
    // FIXME: use @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" }), but some FVs have broken ACLs
    Map<Long, Integer> loadIdsWithNumberOfOldStyleCharacteristics( Set<Long> excludedIds );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( FactorValue factorValue );

    /**
     * Create a given statement and add it to the given factor value.
     * @param factorValue the factor value to add the statement to
     * @param statement   the statement to be created and added to the factor value
     * @return the created statement
     * @throws IllegalArgumentException if the statement already exists
     */
    @CheckReturnValue
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    Statement createStatement( FactorValue factorValue, Statement statement );

    /**
     * Create a given statement as per {@link #createStatement(FactorValue, Statement)} if it is transient, otherwise
     * update an existing statement.
     */
    @CheckReturnValue
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    Statement saveStatement( FactorValue fv, Statement statement );

    /**
     * Save a statement ignoring ACLs.
     * <p>
     * This requires the {@code GROUP_ADMIN} authority.
     * @deprecated do not use this, it is meant for FactorValue migration only
     */
    @Deprecated
    @CheckReturnValue
    @Secured({ "GROUP_ADMIN" })
    Statement saveStatementIgnoreAcl( FactorValue fv, Statement statement );

    /**
     * Remove a statement from a factor value.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeStatement( FactorValue fv, Statement c );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void update( Collection<FactorValue> factorValues );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( FactorValue factorValue );

    /**
     * Mark a given factor value as needs attention.
     * @param factorValue a factor value to mark as needs attention
     * @param note note to use for the {@link ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent}
     * @throws IllegalArgumentException if the factor value already needs attention
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void markAsNeedsAttention( FactorValue factorValue, String note );

    /**
     * Clear a needs attention flag on a given factor value.
     * @param factorValue a factor value whose needs flag will be cleared
     * @param note a note to use for the {@link ubic.gemma.model.common.auditAndSecurity.eventType.DoesNotNeedAttentionEvent}
     *             if the dataset does not need attention for any other reason.
     * @throws IllegalArgumentException if the factor value does not need attention
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void clearNeedsAttentionFlag( FactorValue factorValue, String note );
}
