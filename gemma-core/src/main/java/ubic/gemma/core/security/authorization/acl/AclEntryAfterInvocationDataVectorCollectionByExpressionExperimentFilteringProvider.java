/*
 * The Gemma project
 *
 * Copyright (c) 2008-2010 University of British Columbia
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
package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationByAssociationCollectionFilteringProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.DataVectorValueObject;

import java.util.List;

/**
 * Filter collections of {@link DataVector} and {@link DataVectorValueObject}s based on the permissions of the
 * associated {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}(s).
 *
 * @author pavlidis (based in part on code from Acegi)
 */
public class AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider
        extends AclEntryAfterInvocationByAssociationCollectionFilteringProvider {

    public AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_DATA_VECTOR_COLLECTION_READ", requirePermission );
    }

    @Override
    protected Class<?> getProcessDomainObjectClass() {
        return DataVector.class;
    }

    @Override
    protected Object getActualDomainObject( Object targetDomainObject ) {
        if ( targetDomainObject instanceof DataVector ) {
            return ( ( DataVector ) targetDomainObject ).getExpressionExperiment();
        } else if ( targetDomainObject instanceof DataVectorValueObject ) {
            // can be either a EE or EE subset VO, both of which are securable VOs
            return ( ( DataVectorValueObject ) targetDomainObject ).getExpressionExperiment();
        }
        throw new IllegalArgumentException( String.format( "Don't know how to find actual domain object for %s.",
                targetDomainObject.getClass().getName() ) );
    }
}
