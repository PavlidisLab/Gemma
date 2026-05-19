package ubic.gemma.core.security.gsec.acl.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AclObjectIdentity}, with particular focus on equals/hashCode behaviour
 * during Hibernate-driven instantiation where fields are populated incrementally by setters.
 */
public class AclObjectIdentityTest {

    @Test
    public void equalsHandlesUninitializedTypeAndIdentifier() {
        // Hibernate 6 bytecode-enhanced setters may call equals via setParentObject before
        // type / identifier are populated on a fresh instance. Must not NPE.
        AclObjectIdentity blank = new AclObjectIdentity();
        AclObjectIdentity other = new AclObjectIdentity( "com.example.Foo", 1L );

        // No NPE in either direction.
        assertFalse( blank.equals( other ) );
        assertFalse( other.equals( blank ) );

        // Two uninitialized instances are equal under Objects.equals semantics (both null fields).
        AclObjectIdentity blank2 = new AclObjectIdentity();
        assertTrue( blank.equals( blank2 ) );
        assertEquals( blank.hashCode(), blank2.hashCode() );
    }

    @Test
    public void setParentObjectDoesNotNPEOnUninitializedInstance() {
        // Reproduces the Hibernate-6 path: instance freshly constructed via no-arg ctor,
        // setParentObject called before setType/setIdentifier. Previously NPE'd inside equals().
        AclObjectIdentity fresh = new AclObjectIdentity();
        AclObjectIdentity parent = new AclObjectIdentity( "com.example.Foo", 42L );
        fresh.setParentObject( parent );
        assertEquals( parent, fresh.getParentObject() );
    }

    @Test
    public void equalsByTypeAndIdentifierWhenPopulated() {
        AclObjectIdentity a = new AclObjectIdentity( "com.example.Foo", 1L );
        AclObjectIdentity b = new AclObjectIdentity( "com.example.Foo", 1L );
        AclObjectIdentity c = new AclObjectIdentity( "com.example.Foo", 2L );
        AclObjectIdentity d = new AclObjectIdentity( "com.example.Bar", 1L );

        assertEquals( a, b );
        assertEquals( a.hashCode(), b.hashCode() );
        assertNotEquals( a, c );
        assertNotEquals( a, d );
    }
}
