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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.List;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * @author pavlidis
 * @see AuditTrailDao
 */
@Repository
public class AuditTrailDaoImpl extends AbstractDao<AuditTrail> implements AuditTrailDao {

    @Autowired
    public AuditTrailDaoImpl( SessionFactory sessionFactory ) {
        super( AuditTrail.class, sessionFactory );
    }

    @Override
    public int removeByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() )
            return 0;
        //noinspection unchecked
        List<Long> aeIds = getSessionFactory().getCurrentSession()
                .createQuery( "select ae.id from AuditTrail at join at.events ae" )
                .list();
        //noinspection unchecked
        List<Long> aetIds = getSessionFactory().getCurrentSession()
                .createQuery( "select aet.id from AuditTrail at join at.events ae join ae.eventType aet" )
                .list();
        if ( !aeIds.isEmpty() ) {
            getSessionFactory().getCurrentSession()
                    .createQuery( "delete from AuditEvent ae where ae.id in :aeIds" )
                    .setParameterList( "aeIds", optimizeParameterList( aeIds ) )
                    .executeUpdate();
        }
        if ( !aetIds.isEmpty() ) {
            getSessionFactory().getCurrentSession()
                    .createQuery( "delete from AuditEventType aet where aet.id in :aetIds" )
                    .setParameterList( "aetIds", optimizeParameterList( aetIds ) )
                    .executeUpdate();
        }
        return getSessionFactory().getCurrentSession()
                .createQuery( "delete from AuditTrail at where at.id in :atIds" )
                .setParameterList( "atIds", optimizeParameterList( ids ) )
                .executeUpdate();
    }
}