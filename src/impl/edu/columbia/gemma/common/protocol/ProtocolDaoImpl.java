/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.common.protocol;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.protocol.Protocol
 */
public class ProtocolDaoImpl extends edu.columbia.gemma.common.protocol.ProtocolDaoBase {

    private static Log log = LogFactory.getLog( ProtocolDaoImpl.class.getName() );

    @Override
    public Protocol find( Protocol protocol ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Protocol.class );
            queryObject.add( Restrictions.eq( "name", protocol.getName() ) ).add(
                    Restrictions.eq( "description", protocol.getDescription() ) );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + edu.columbia.gemma.common.protocol.Protocol.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( edu.columbia.gemma.common.protocol.Protocol ) results.iterator().next();
                }
            }
            return ( Protocol ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Protocol findOrCreate( Protocol protocol ) {
        if ( protocol == null || protocol.getName() == null ) {
            log.warn( "Protocol was null or had no name : " + protocol );
            return null;
        }
        Protocol newProtocol = find( protocol );
        if ( newProtocol != null ) {
            log.debug( "Found existing protocol: " + protocol );
            return newProtocol;
        }
        log.debug( "Creating new protocol: " + protocol );
        return ( Protocol ) create( protocol );
    }

}