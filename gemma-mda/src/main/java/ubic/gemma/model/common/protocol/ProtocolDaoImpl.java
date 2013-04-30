/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.common.protocol;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.protocol.Protocol
 */
@Repository
public class ProtocolDaoImpl extends ubic.gemma.model.common.protocol.ProtocolDaoBase {

    private static Log log = LogFactory.getLog( ProtocolDaoImpl.class.getName() );

    @Autowired
    public ProtocolDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Protocol find( Protocol protocol ) {
        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( Protocol.class );
        queryObject.add( Restrictions.eq( "name", protocol.getName() ) );

        if ( protocol.getDescription() != null )
            queryObject.add( Restrictions.eq( "description", protocol.getDescription() ) );

        java.util.List<Protocol> results = queryObject.list();
        Protocol result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + ubic.gemma.model.common.protocol.Protocol.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return result;

    }

    @Override
    public Protocol findOrCreate( Protocol protocol ) {
        if ( protocol == null || protocol.getName() == null ) {
            throw new IllegalArgumentException( "Protocol was null or had no name : " + protocol );
        }
        Protocol newProtocol = find( protocol );
        if ( newProtocol != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing protocol: " + newProtocol );
            return newProtocol;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new protocol: " + protocol );
        return create( protocol );
    }

}