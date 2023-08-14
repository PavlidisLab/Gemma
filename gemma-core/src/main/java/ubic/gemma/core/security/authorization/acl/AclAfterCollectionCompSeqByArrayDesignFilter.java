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
package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationByAssociationCollectionFilteringProvider;
import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.List;

/**
 * For this particular AfterInvocationProvider, composite sequence authorization is determined based on the secured
 * array design acl. ie. composite sequence security is determined from an owning array desgin's security.
 *
 * @author keshav (based in part on code from Acegi)
 * @see AfterInvocationProvider
 */
public class AclAfterCollectionCompSeqByArrayDesignFilter extends AclEntryAfterInvocationByAssociationCollectionFilteringProvider {

    public AclAfterCollectionCompSeqByArrayDesignFilter( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ", requirePermission );
    }

    @Override
    protected Class<?> getProcessDomainObjectClass() {
        return CompositeSequence.class;
    }

    @Override
    protected Object getActualDomainObject( Object targetDomainObject ) {
        if ( targetDomainObject instanceof CompositeSequence ) {
            return ( ( CompositeSequence ) targetDomainObject ).getArrayDesign();
        }
        throw new IllegalArgumentException(
                "Don't know how to filter a " + targetDomainObject.getClass().getSimpleName() );
    }
}
