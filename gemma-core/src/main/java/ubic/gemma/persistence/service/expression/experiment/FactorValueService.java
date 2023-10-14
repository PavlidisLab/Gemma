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

/**
 * @author kelsey
 */
public interface FactorValueService extends BaseService<FactorValue>, FilteringVoEnabledService<FactorValue, FactorValueValueObject> {

    @Deprecated
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<FactorValue> findByValue( String valuePrefix );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    FactorValue findOrCreate( FactorValue factorValue );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    FactorValue load( Long id );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<FactorValue> loadAll();

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( FactorValue factorValue );

    /**
     * Load a given statement by ID.
     */
    @Nullable
    @Secured({ "GROUP_USER" })
    Statement loadStatement( Long statementId );

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
     * <p>
     * This method has the benefit of only updating the given statement and not the whole {@link FactorValue}.
     */
    @CheckReturnValue
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    Statement saveStatement( FactorValue fv, Statement statement );

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
}
