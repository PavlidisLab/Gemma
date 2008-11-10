/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
package ubic.gemma.model.common;

import ubic.gemma.util.EntityUtils;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.Securable
 */
public class SecurableDaoImpl<T extends Securable> extends ubic.gemma.model.common.SecurableDaoBase<T> {

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.SecurableDaoBase#getAclObjectIdentityId(ubic.gemma.model.common.Securable)
     */
    public Long getAclObjectIdentityId( Securable target ) {

        String objectIdentity = createObjectIdentityFromObject( target );

        String queryString = "SELECT id FROM acl_object_identity WHERE object_identity = ?";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setParameter( 0, objectIdentity );
            Integer result = ( Integer ) queryObject.uniqueResult();

            Long longId = null;
            if ( result != null ) {
                longId = new Long( result );
            }

            return longId;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.SecurableDao#getAclObjectIdentityParent(ubic.gemma.model.common.Securable)
     */
    public Integer getAclObjectIdentityParentId( Securable target ) {

        String objectIdentity = createObjectIdentityFromObject( target );

        String queryString = "select parent_object from acl_object_identity where object_identity = ?";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setParameter( 0, objectIdentity );
            Integer result = ( Integer ) queryObject.uniqueResult();

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * Creates the object_identity to be used in the acl_object_identity table.
     * 
     * @param target
     * @return
     */
    private String createObjectIdentityFromObject( Securable target ) {

        Securable implementation = ( Securable ) EntityUtils.getImplementationForProxy( target );
        Long id = implementation.getId();

        return implementation.getClass().getName() + ":" + id;
    }
}