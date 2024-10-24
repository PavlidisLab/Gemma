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
package ubic.gemma.persistence.service.common.protocol;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.AbstractDao;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>Protocol</code>.
 *
 * @see Protocol
 */
@Repository
public class ProtocolDaoImpl extends AbstractDao<Protocol> implements ProtocolDao {

    @Autowired
    public ProtocolDaoImpl( SessionFactory sessionFactory ) {
        super( Protocol.class, sessionFactory );
    }

    @Override
    public Protocol find( Protocol protocol ) {
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( Protocol.class );
        queryObject.add( Restrictions.eq( "name", protocol.getName() ) );

        if ( protocol.getDescription() != null )
            queryObject.add( Restrictions.eq( "description", protocol.getDescription() ) );

        return ( Protocol ) queryObject.uniqueResult();
    }

    @Override
    public Protocol findByName( String protocolName ) {
        return findOneByProperty( "name", protocolName );
    }
}