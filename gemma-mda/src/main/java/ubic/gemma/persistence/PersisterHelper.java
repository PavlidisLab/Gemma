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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.StatusService;

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
    StatusService statusService;

    @Autowired
    public PersisterHelper( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.loader.loaderutils.Loader#create(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Object persist( Object entity ) {

        if ( entity instanceof Auditable ) {
            Auditable a = ( Auditable ) entity;
            a.setAuditTrail( persistAuditTrail( a.getAuditTrail() ) );

            a.setStatus( statusService.create() );

            return super.persist( a );
        }
        this.getSession().flush();
        return super.persist( entity );
    }

}
