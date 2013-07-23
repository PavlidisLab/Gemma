/*
 * The Gemma project
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
package ubic.gemma.persistence;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.StatusDao;

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
@Service
public class PersisterHelper extends RelationshipPersister {

    @Autowired
    private StatusDao statusDao;

    @Autowired
    public PersisterHelper( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.loader.loaderutils.Loader#create(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Object persist( Object entity ) {

        try {

            this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.COMMIT );

            if ( entity instanceof Auditable ) {
                Auditable auditable = ( Auditable ) entity;

                if ( auditable.getAuditTrail() == null ) auditable.setAuditTrail( AuditTrail.Factory.newInstance() );

                auditable.setAuditTrail( persistAuditTrail( auditable.getAuditTrail() ) );
                auditable.setStatus( statusDao.create() );
            }

            Object persisted = super.persist( entity );

            return persisted;
        } finally {
            this.getSession().setFlushMode( FlushMode.AUTO );
        }
    }

}
