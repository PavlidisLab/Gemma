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
package ubic.gemma.security.authorization.acl;

import java.util.List;

import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * For this particular AfterInvocationProvider, composite sequence authorization is determined based on the secured
 * array design acl. ie. composite sequence security is determined from an owning array desgin's security.
 * 
 * @author keshav (based in part on code from Acegi)
 * @version $Id: BasicAclEntryAfterInvocationArrayDesignCollectionFilteringProvider.java,v 1.2 2005/08/17 21:46:32
 *          keshav Exp $
 * @see AfterInvocationProvider
 */
public class AclAfterCollectionCompSeqByArrayDesignFilter extends
        ByAssociationFilteringProvider<ArrayDesignImpl, CompositeSequence> {

    private static final String CONFIG_ATTRIBUTE = "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ";

    public AclAfterCollectionCompSeqByArrayDesignFilter( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, CONFIG_ATTRIBUTE, requirePermission );
    }

    @Override
    public String getProcessConfigAttribute() {
        return CONFIG_ATTRIBUTE;
    }

    /**
     * @param targetDomainObject
     * @return
     */
    @Override
    protected ArrayDesignImpl getAssociatedSecurable( Object targetDomainObject ) {

        if ( CompositeSequence.class.isAssignableFrom( targetDomainObject.getClass() ) ) {
            return ( ArrayDesignImpl ) ( ( CompositeSequence ) targetDomainObject ).getArrayDesign();
        }

        throw new IllegalArgumentException( "Don't know how to filter a "
                + targetDomainObject.getClass().getSimpleName() );
    }

}
