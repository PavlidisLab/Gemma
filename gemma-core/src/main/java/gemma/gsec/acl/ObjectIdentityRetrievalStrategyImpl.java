/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

package gemma.gsec.acl;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.util.Assert;

/**
 * Customized to know how to deal with SecureValueObject, makes it easier to share code in SecurityService; and doesn't
 * use the default ObjectIdentityImpl.
 *
 * @author Paul
 * @version $Id: ValueObjectAwareIdentityRetrievalStrategyImpl.java,v 1.3 2013/09/14 16:56:02 paul Exp $
 */
public class ObjectIdentityRetrievalStrategyImpl implements ObjectIdentityRetrievalStrategy {

    @Override
    public ObjectIdentity getObjectIdentity( Object domainObject ) {
        Assert.notNull( domainObject, "The domain object must be non-null." );
        Assert.isInstanceOf( Securable.class, domainObject, "The domain object must implement the " + Securable.class.getName() + " interface." );
        ObjectIdentity oi;
        if ( domainObject instanceof SecureValueObject ) {
            SecureValueObject svo = ( SecureValueObject ) domainObject;
            oi = new AclObjectIdentity( svo.getSecurableClass(), svo.getId() );
        } else {
            oi = new AclObjectIdentity( ( Securable ) domainObject );
        }
        if ( oi.getType() != null && oi.getType().contains( "UserGroup" ) ) {
            System.out.println( "OID-FOR " + domainObject.getClass().getName() + " -> " + oi );
        }
        return oi;
    }
}
