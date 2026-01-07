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
package ubic.gemma.persistence.persister;

import org.springframework.stereotype.Service;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.Auditable;

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
 *
 * @author pavlidis
 * @author keshav
 */
@Service
public class PersisterHelperImpl extends RelationshipPersister implements PersisterHelper {

    @Override
    protected <T extends Identifiable> T doPersist( T entity, Caches caches ) {
        if ( entity instanceof Auditable ) {
            Auditable auditable = ( Auditable ) entity;
            if ( auditable.getAuditTrail().getId() == null ) {
                auditable.setAuditTrail( persistAuditTrail( auditable.getAuditTrail() ) );
            }
        }
        return super.doPersist( entity, caches );
    }

}
