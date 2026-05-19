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

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
 * <p>
 * Phase 3 persister retirement (roadmap step 2): the audit-trail priming that used to live
 * in {@code doPersist} has moved to
 * {@link ubic.gemma.persistence.audit.AuditTrailEventListener}, a Hibernate
 * {@code PERSIST} event listener that runs ahead of cascade. Every {@code session.persist}
 * of an {@code Auditable} now flows through that single chokepoint, so this class can
 * inherit {@link RelationshipPersister#doPersist} unchanged. This is one of the prerequisites
 * for deleting the entire persister chain (roadmap step 8).
 *
 * @author pavlidis
 * @author keshav
 */
@Service
public class PersisterHelperImpl extends RelationshipPersister implements PersisterHelper {

    // doPersist is fully inherited from RelationshipPersister now that the audit-trail
    // priming has moved to AuditTrailEventListener.

}
