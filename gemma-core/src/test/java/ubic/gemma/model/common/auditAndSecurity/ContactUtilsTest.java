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
 */
package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ContactUtils#displayName} — the name/username fallback the ticket surfaces read a person
 * through, including the proxy case that makes the plain {@code instanceof User} test useless.
 */
class ContactUtilsTest {

    @Test
    void displayName_prefersTheContactName() {
        User u = User.Factory.newInstance( "administrator" );
        u.setName( "admin" );

        assertThat( ContactUtils.displayName( u ) ).isEqualTo( "admin" );
    }

    @Test
    void displayName_fallsBackToTheUsername_whenTheContactNameWasNeverFilledIn() {
        User u = User.Factory.newInstance( "administrator" );

        assertThat( ContactUtils.displayName( u ) ).isEqualTo( "administrator" );
    }

    @Test
    void displayName_treatsABlankContactNameAsAbsent() {
        User u = User.Factory.newInstance( "administrator" );
        u.setName( "   " );

        assertThat( ContactUtils.displayName( u ) ).isEqualTo( "administrator" );
    }

    /**
     * Ticket's reporter/assignee and TicketEvent's actor are all declared {@code Contact}, which is
     * itself an {@code @Entity} — so Hibernate hands back a Contact-typed proxy for a User row and
     * {@code contact instanceof User} is false. Without the unproxy this returns null.
     */
    @Test
    void displayName_resolvesAContactTypedProxyOverAUserRow() {
        User u = User.Factory.newInstance( "administrator" );
        Contact proxied = new ContactProxy( u );

        assertThat( proxied ).isNotInstanceOf( User.class ); // the trap, stated
        assertThat( ContactUtils.displayName( proxied ) ).isEqualTo( "administrator" );
    }

    @Test
    void displayName_isNullForAPlainContactWithNoName() {
        Contact c = Contact.Factory.newInstance();

        assertThat( ContactUtils.displayName( c ) ).isNull();
    }

    @Test
    void displayName_isNullForANullContact() {
        assertThat( ContactUtils.displayName( null ) ).isNull();
    }

    /**
     * A Mockito mock cannot stand in for a proxy here: {@code Hibernate.unproxy} goes through
     * {@code asHibernateProxy()}, a default method a mock stubs to null, so the unfixed code would
     * pass. This is a real subclass with a mocked {@link LazyInitializer}.
     */
    private static class ContactProxy extends Contact implements HibernateProxy {

        private final LazyInitializer li;

        private ContactProxy( Object target ) {
            this.li = mock( LazyInitializer.class );
            when( li.getImplementation() ).thenReturn( target );
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return li;
        }

        @Override
        public Object writeReplace() {
            return this;
        }
    }
}
