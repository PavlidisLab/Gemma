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

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.protocol.Hardware
 */
public class HardwareDaoImpl extends edu.columbia.gemma.common.protocol.HardwareDaoBase {

    @Override
    public Hardware find( Hardware hardware ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Hardware.class );
            queryObject.add( Restrictions.eq( "name", hardware.getName() ) ).add(
                    Restrictions.eq( "make", hardware.getMake() ) )
                    .add( Restrictions.eq( "model", hardware.getModel() ) );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + edu.columbia.gemma.common.protocol.Hardware.class.getName()
                                    + "' was found when executing query" );
                } else if ( results.size() == 1 ) {
                    result = ( edu.columbia.gemma.common.protocol.Hardware ) results.iterator().next();
                }
            }
            return ( Hardware ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Hardware findOrCreate( Hardware hardware ) {
        if ( hardware == null || hardware.getMake() == null || hardware.getModel() == null ) return null;
        Hardware newHardware = find( hardware );
        if ( newHardware != null ) {
            BeanPropertyCompleter.complete( newHardware, hardware );
            return newHardware;
        }
        return ( Hardware ) create( hardware );
    }

}