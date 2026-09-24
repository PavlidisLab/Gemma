/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
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

package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.Hibernate;
import org.springframework.lang.Nullable;

/**
 * Helpers for reading a {@link Contact} the way a person is displayed.
 *
 * @author paul
 */
public final class ContactUtils {

    private ContactUtils() {
    }

    /**
     * The best available human-readable name for a contact: its {@code name}, else the account's
     * {@code userName}.
     * <p>
     * A Gemma {@link User} carries {@code userName} separately from the {@code name} it inherits as a
     * {@link Person}, and the contact name is optional. Anything typed {@code Contact} — {@code Ticket}'s
     * reporter and assignee, a {@code TicketEvent}'s actor — therefore reads null for an account that
     * never filled one in, even though the account has a username. {@code AuditEventValueObject} does
     * not hit this because {@code AuditEvent.performer} is declared {@code User}.
     *
     * @param contact a contact, possibly an uninitialized-but-attached proxy; may be null
     * @return the name, else the username, else null — never blank
     */
    @Nullable
    public static String displayName( @Nullable Contact contact ) {
        if ( contact == null ) {
            return null;
        }
        String name = contact.getName();
        if ( name != null && !name.trim().isEmpty() ) {
            return name.trim();
        }
        // Contact is itself an @Entity, so a LAZY association declared Contact hands back a
        // Contact-typed proxy even when the row is a User, and `instanceof User` is false until it is
        // resolved. Reaching here means getName() already succeeded, so the proxy is initialized and
        // unproxy costs no query.
        Object impl = Hibernate.unproxy( contact );
        if ( impl instanceof User ) {
            String userName = ( ( User ) impl ).getUserName();
            if ( userName != null && !userName.trim().isEmpty() ) {
                return userName.trim();
            }
        }
        return null;
    }
}
