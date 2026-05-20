/*
 * The gemma-core project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.acl;

import ubic.gemma.core.security.model.Securable;
import ubic.gemma.core.security.model.SecuredChild;
import ubic.gemma.core.security.model.SecuredNotChild;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Nullable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fast unit tests for {@link AclEventListener}. Runs entirely in-memory: no Spring context,
 * no Hibernate flush, no JDBC, no database. The trigger surface
 * ({@link PostInsertEvent}/{@link PostDeleteEvent}) is constructed by hand and the persister
 * metadata (Hibernate Type[], property values) is supplied via a stub registry, so each test
 * runs in single-digit milliseconds.
 * <p>
 * Coverage matrix:
 * <ul>
 *   <li>plain root Securable insert → ACL with no parent;</li>
 *   <li>SecuredChild with explicit {@code getSecurityOwner} → parent resolved via existing
 *       ACL on owner;</li>
 *   <li>parent-then-child cascade order → stash populated by parent insert, consumed by child;</li>
 *   <li>chain-flat semantics (Root → Middle (SecuredChild) → Leaf (SecuredChild) all share
 *       Root as parent ACL);</li>
 *   <li>back-ref discovery prefers {@link SecuredChild} candidate over {@link SecuredNotChild};</li>
 *   <li>force-flatten reconciles a child that was earlier parented to an intermediate that
 *       later got its own ACL parent set;</li>
 *   <li>cycle guard: stashChildren visits each entity at most once per top-level call even
 *       when collections cross-reference;</li>
 *   <li>{@link SecuredNotChild} is excluded from stashing + back-ref discovery (kept as its
 *       own ACL root);</li>
 *   <li>delete event → ACL removed (with cascade-children removal);</li>
 *   <li>non-Securable entities are ignored;</li>
 *   <li>{@link AclEventListener#requiresPostCommitHandling} returns false (in-tx semantics).</li>
 * </ul>
 */
public class AclEventListenerTest {

    /**
     * In-memory ACL backend. Asserted against directly in most tests.
     */
    private InMemoryAclService aclService;

    /**
     * Minimal {@link BaseAclAdvice} concrete impl exposing the listener's collaborator surface.
     */
    private TestAclAdvice aclAdvice;

    /**
     * The listener under test.
     */
    private AclEventListener listener;

    /**
     * Stub persister registry: Mockito-backed {@link SessionFactoryImplementor} →
     * {@link MappingMetamodel} → {@link EntityPersister} keyed by entity class. Each test
     * registers persisters for the entity classes it touches; the registry lookups happen
     * inside {@link AclEventListener#stashChildren} / {@code discoverParentViaBackRef}.
     */
    private StubPersisterRegistry persisters;

    @BeforeEach
    public void setUp() {
        // Owner sid + admin authority — addOrUpdateAcl checks SecurityContext for both.
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "test-user", "x", "GROUP_ADMIN" );
        auth.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( auth );

        aclService = new InMemoryAclService();
        persisters = new StubPersisterRegistry();
        aclAdvice = new TestAclAdvice( aclService, persisters.sessionFactory );
        listener = new AclEventListener( aclAdvice, persisters.sessionFactory );
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
        // Reset listener thread-local stash so a leftover entry from one test can't taint the next.
        // (We can't reach STASH directly; fire a no-op insert on a non-Securable to flush nothing,
        //  but actually a clean wipe is needed — exposing this would mean test-only API. The
        //  simplest robust answer is one-listener-per-test which we already do via @Before.)
    }

    // -------------------------------------------------------------------------------------
    // Smoke
    // -------------------------------------------------------------------------------------

    @Test
    public void requiresPostCommitHandling_isFalse_soAclOpsRunInTx() {
        assertThat( listener.requiresPostCommitHandling( null ) ).isFalse();
    }

    @Test
    public void nonSecurableEntity_isIgnored() {
        firePostInsert( "I am not a Securable", 42L );
        assertThat( aclService.size() ).isZero();
    }

    @Test
    public void rootSecurable_insertCreatesAclWithoutParent() {
        Root root = new Root( 1L );
        persisters.register( Root.class );
        firePostInsert( root, root.id );

        Acl acl = aclService.readAclById( oid( Root.class, 1L ) );
        assertThat( acl.getParentAcl() ).isNull();
        assertThat( acl.getEntries() ).isNotEmpty();
    }

    @Test
    public void securedChild_withSecurityOwner_inheritsFromOwnerAcl() {
        Root parent = new Root( 1L );
        persisters.register( Root.class );
        firePostInsert( parent, parent.id );

        ChildWithOwner child = new ChildWithOwner( 2L, parent );
        persisters.register( ChildWithOwner.class );
        firePostInsert( child, child.id );

        Acl childAcl = aclService.readAclById( oid( ChildWithOwner.class, 2L ) );
        assertThat( childAcl.getParentAcl() ).isNotNull();
        assertThat( childAcl.getParentAcl().getObjectIdentity() ).isEqualTo( oid( Root.class, 1L ) );
        assertThat( childAcl.isEntriesInheriting() ).isTrue();
    }

    // -------------------------------------------------------------------------------------
    // Parent stash
    // -------------------------------------------------------------------------------------

    @Test
    public void parentStash_isPopulatedByParentInsert_andConsumedByChildInsert() {
        Root parent = new Root( 1L );
        // Child has no getSecurityOwner — must rely on stash. Starts transient (id=null) so
        // when Root.PostInsert walks the cascade collection, the listener takes the
        // stash-the-transient-child branch (not the retroactive fixup branch).
        ChildNoOwner child = new ChildNoOwner( null );
        parent.children.add( child );

        persisters.register( Root.class )
                .withCollectionProperty( "children", CascadeStyles.PERSIST, p -> ( ( Root ) p ).children );
        persisters.register( ChildNoOwner.class );

        // Parent inserts first; this populates the stash with {ihc(child) → Root#1 OID}.
        firePostInsert( parent, parent.id );
        // Hibernate assigns child's id between cascade-pending and child PostInsert.
        child.id = 2L;
        firePostInsert( child, child.id );

        Acl childAcl = aclService.readAclById( oid( ChildNoOwner.class, 2L ) );
        assertThat( childAcl.getParentAcl().getObjectIdentity() )
                .isEqualTo( oid( Root.class, 1L ) );
    }

    @Test
    public void chainFlatSemantics_grandchildParentsToRootNotImmediateAncestor() {
        // Root → Middle (SecuredChild, no FK back) → Leaf (SecuredChild, no FK back).
        // Both middle and leaf transient at Root.PostInsert time, getting ids in cascade order
        // before their own PostInserts — same as real Hibernate cascade-flush ordering.
        Root root = new Root( 1L );
        ChildNoOwner middle = new ChildNoOwner( null );
        ChildNoOwner leaf = new ChildNoOwner( null );
        root.children.add( middle );
        middle.children.add( leaf );

        persisters.register( Root.class )
                .withCollectionProperty( "children", CascadeStyles.PERSIST, p -> ( ( Root ) p ).children );
        persisters.register( ChildNoOwner.class )
                .withCollectionProperty( "children", CascadeStyles.PERSIST, p -> ( ( ChildNoOwner ) p ).children );

        firePostInsert( root, root.id );
        middle.id = 2L;
        firePostInsert( middle, middle.id );
        leaf.id = 3L;
        firePostInsert( leaf, leaf.id );

        Acl leafAcl = aclService.readAclById( oid( ChildNoOwner.class, 3L ) );
        // Chain-flat: leaf's parent is Root, not Middle. Matches BaseAclAdvice.chooseParentForAssociations.
        assertThat( leafAcl.getParentAcl().getObjectIdentity() ).isEqualTo( oid( Root.class, 1L ) );
    }

    // -------------------------------------------------------------------------------------
    // Back-ref discovery
    // -------------------------------------------------------------------------------------

    @Test
    public void backRefDiscovery_prefersSecuredChildOverSecuredNotChild() {
        // Setup: a Root exists. A NotChildRoot also exists (its own ACL root). A late-added
        // child has ManyToOne refs to BOTH but should pick the SecuredChild path (which
        // chains to Root) over the SecuredNotChild path (an unrelated root).
        Root root = new Root( 1L );
        NotChildRoot notChild = new NotChildRoot( 99L );
        persisters.register( Root.class );
        persisters.register( NotChildRoot.class );
        firePostInsert( root, root.id );
        firePostInsert( notChild, notChild.id );

        // A SecuredChild that lives under Root, ALSO with a back-ref to the NotChildRoot.
        ChildWithOwner middleChild = new ChildWithOwner( 2L, root );
        persisters.register( ChildWithOwner.class );
        firePostInsert( middleChild, middleChild.id );

        // The "late" entity has two ManyToOne references: one to NotChildRoot, one to middleChild.
        // The listener falls back to back-ref discovery (no stash, no getSecurityOwner) and
        // should follow middleChild → Root, not NotChildRoot.
        TwoParentChild late = new TwoParentChild( 5L, notChild, middleChild );
        persisters.register( TwoParentChild.class )
                .withEntityProperty( "notChildRef", null, p -> ( ( TwoParentChild ) p ).notChildRef )
                .withEntityProperty( "securedChildRef", null, p -> ( ( TwoParentChild ) p ).securedChildRef );

        firePostInsert( late, late.id );

        Acl lateAcl = aclService.readAclById( oid( TwoParentChild.class, 5L ) );
        assertThat( lateAcl.getParentAcl().getObjectIdentity() )
                .as( "back-ref discovery should follow SecuredChild → chain-flat top (Root), not the SecuredNotChild root" )
                .isEqualTo( oid( Root.class, 1L ) );
    }

    @Test
    public void securedNotChildBackRef_isUsedAsParent_whenNoSecuredChildCandidate() {
        // If only a SecuredNotChild back-ref is available, use it (rare but allowed).
        NotChildRoot root = new NotChildRoot( 99L );
        persisters.register( NotChildRoot.class );
        firePostInsert( root, root.id );

        OneNotChildParent child = new OneNotChildParent( 5L, root );
        persisters.register( OneNotChildParent.class )
                .withEntityProperty( "notChildRef", null, p -> ( ( OneNotChildParent ) p ).notChildRef );

        firePostInsert( child, child.id );

        Acl childAcl = aclService.readAclById( oid( OneNotChildParent.class, 5L ) );
        assertThat( childAcl.getParentAcl().getObjectIdentity() )
                .isEqualTo( oid( NotChildRoot.class, 99L ) );
    }

    // -------------------------------------------------------------------------------------
    // Force-flatten reconciliation
    // -------------------------------------------------------------------------------------

    @Test
    public void forceFlatten_reconcilesChildThatWasParentedToIntermediate() {
        // Reproduces the FK-ordering scenario: Middle inserts before Root (FK target), gets an
        // orphan ACL. Leaf inserts after Middle, back-ref discovery picks orphan Middle as its
        // parent. Then Root inserts and its stash walk reaches Middle → fixes Middle.parent
        // and recurses into Middle.children → finds Leaf parented to Middle, force-flattens
        // Leaf.parent = Root.
        ChildNoOwner middle = new ChildNoOwner( 1L );
        ChildWithBackRef leaf = new ChildWithBackRef( 2L, middle );
        middle.children.add( leaf );

        persisters.register( ChildNoOwner.class )
                .withCollectionProperty( "children", CascadeStyles.PERSIST, p -> ( ( ChildNoOwner ) p ).children );
        persisters.register( ChildWithBackRef.class )
                .withEntityProperty( "middleRef", null, p -> ( ( ChildWithBackRef ) p ).middleRef );

        firePostInsert( middle, middle.id );
        firePostInsert( leaf, leaf.id );

        // At this point: middle is orphan; leaf was parented to middle via back-ref discovery.
        assertThat( aclService.readAclById( oid( ChildNoOwner.class, 1L ) ).getParentAcl() ).isNull();
        assertThat( aclService.readAclById( oid( ChildWithBackRef.class, 2L ) ).getParentAcl()
                .getObjectIdentity() ).isEqualTo( oid( ChildNoOwner.class, 1L ) );

        // Now Root inserts with middle in its cascade collection.
        Root root = new Root( 100L );
        root.children.add( middle );
        persisters.register( Root.class )
                .withCollectionProperty( "children", CascadeStyles.PERSIST, p -> ( ( Root ) p ).children );

        firePostInsert( root, root.id );

        // Middle.parent gets set to Root, then recursion into middle.children should
        // force-flatten leaf.parent from middle (now wrong) to Root (the chain-flat top).
        assertThat( aclService.readAclById( oid( ChildNoOwner.class, 1L ) ).getParentAcl()
                .getObjectIdentity() ).isEqualTo( oid( Root.class, 100L ) );
        assertThat( aclService.readAclById( oid( ChildWithBackRef.class, 2L ) ).getParentAcl()
                .getObjectIdentity() )
                .as( "leaf should be force-flattened to chain-flat top after Root's stash walk" )
                .isEqualTo( oid( Root.class, 100L ) );
    }

    // -------------------------------------------------------------------------------------
    // Cycle guard
    // -------------------------------------------------------------------------------------

    @Test
    public void cycleGuard_handlesSelfReferentialBackRef() {
        // Diamond / self-ref: child has back-ref to itself's class via "self" association.
        // Without the visited set, recursive fixup would loop forever.
        SelfRefChild a = new SelfRefChild( 1L, null );
        a.self = a; // self-reference

        persisters.register( SelfRefChild.class )
                .withEntityProperty( "self", null, p -> ( ( SelfRefChild ) p ).self );

        Root root = new Root( 100L );
        root.children.add( a );
        persisters.register( Root.class )
                .withCollectionProperty( "children", CascadeStyles.PERSIST, p -> ( ( Root ) p ).children );

        // Pre-persist child (orphan), then insert root — recursion into child should not infinite-loop.
        firePostInsert( a, a.id );
        firePostInsert( root, root.id );

        // No StackOverflowError; child ends up parented to root.
        assertThat( aclService.readAclById( oid( SelfRefChild.class, 1L ) ).getParentAcl()
                .getObjectIdentity() ).isEqualTo( oid( Root.class, 100L ) );
    }

    // -------------------------------------------------------------------------------------
    // SecuredNotChild exclusion
    // -------------------------------------------------------------------------------------

    @Test
    public void securedNotChildInCascadeCollection_isNotClaimedAsChild() {
        Root root = new Root( 1L );
        NotChildRoot independent = new NotChildRoot( 99L );
        root.notChildren.add( independent );

        persisters.register( Root.class )
                .withCollectionProperty( "notChildren", CascadeStyles.PERSIST,
                        p -> ( ( Root ) p ).notChildren );
        persisters.register( NotChildRoot.class );

        firePostInsert( root, root.id );
        firePostInsert( independent, independent.id );

        // The NotChildRoot must have an ACL with no parent — it's its own root.
        Acl ncAcl = aclService.readAclById( oid( NotChildRoot.class, 99L ) );
        assertThat( ncAcl.getParentAcl() ).isNull();
    }

    // -------------------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------------------

    @Test
    public void postDelete_removesAcl() {
        Root root = new Root( 1L );
        persisters.register( Root.class );
        firePostInsert( root, root.id );
        assertThat( aclService.peek( oid( Root.class, 1L ) ) ).isNotNull();

        firePostDelete( root, root.id );

        assertThatThrownBy( () -> aclService.readAclById( oid( Root.class, 1L ) ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void postDelete_cascadesToChildAcls() {
        // Root + child via stash; deleting Root should also remove the child ACL via
        // InMemoryAclService's deleteChildren=true behaviour.
        Root root = new Root( 1L );
        ChildNoOwner child = new ChildNoOwner( null );
        root.children.add( child );

        persisters.register( Root.class )
                .withCollectionProperty( "children", CascadeStyles.PERSIST, p -> ( ( Root ) p ).children );
        persisters.register( ChildNoOwner.class );

        firePostInsert( root, root.id );
        child.id = 2L;
        firePostInsert( child, child.id );
        assertThat( aclService.peek( oid( ChildNoOwner.class, 2L ) ) ).isNotNull();
        assertThat( aclService.peek( oid( ChildNoOwner.class, 2L ) ).getParentAcl() ).isNotNull();

        firePostDelete( root, root.id );

        assertThatThrownBy( () -> aclService.readAclById( oid( ChildNoOwner.class, 2L ) ) )
                .isInstanceOf( NotFoundException.class );
    }

    // -------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------

    private void firePostInsert( Object entity, @Nullable Long id ) {
        EntityPersister persister = persisters.persisterFor( entity.getClass() );
        PostInsertEvent event = mock( PostInsertEvent.class );
        when( event.getEntity() ).thenReturn( entity );
        when( event.getId() ).thenReturn( id );
        when( event.getPersister() ).thenReturn( persister );
        listener.onPostInsert( event );
    }

    private void firePostDelete( Object entity, @Nullable Long id ) {
        EntityPersister persister = persisters.persisterFor( entity.getClass() );
        PostDeleteEvent event = mock( PostDeleteEvent.class );
        when( event.getEntity() ).thenReturn( entity );
        when( event.getId() ).thenReturn( id );
        when( event.getPersister() ).thenReturn( persister );
        listener.onPostDelete( event );
    }

    private static ObjectIdentity oid( Class<?> clazz, Long id ) {
        return new org.springframework.security.acls.domain.ObjectIdentityImpl( clazz.getName(), id );
    }

    // -------------------------------------------------------------------------------------
    // Concrete BaseAclAdvice for tests
    // -------------------------------------------------------------------------------------

    /**
     * Minimal {@link BaseAclAdvice} concrete subclass. The base class is abstract over four
     * gsec/Gemma extension points (user / group identification); none of them are exercised
     * by the listener algorithm under test (those only fire for User/UserGroup entities,
     * which the test fixtures don't model), so we return safe defaults.
     */
    public static class TestAclAdvice extends BaseAclAdvice {
        TestAclAdvice( ubic.gemma.core.security.acl.domain.AclService aclService, SessionFactory sessionFactory ) {
            super( aclService, sessionFactory, new ObjectIdentityRetrievalStrategyImpl() );
        }

        @Override
        protected boolean currentUserIsAdmin() {
            return true;
        }

        @Override
        protected boolean currentUserIsAnonymous() {
            return false;
        }

        @Override
        protected boolean currentUserIsRunningAsAdmin() {
            return false;
        }

        @Override
        protected GrantedAuthority getUserGroupGrantedAuthority( Securable object ) {
            return new SimpleGrantedAuthority( "GROUP_TEST" );
        }

        @Override
        protected String getUserName( Securable object ) {
            return "test-user";
        }

        @Override
        protected boolean objectIsUser( Securable object ) {
            return false;
        }

        @Override
        protected boolean objectIsUserGroup( Securable object ) {
            return false;
        }
    }

    /**
     * Hand-rolled Spring {@link ObjectIdentityRetrievalStrategyImpl} replacement that mirrors
     * the production strategy: {@code (entity.class.name, entity.id)}. We don't need the
     * proxy-unwrapping logic of {@code ubic.gemma.core.security.acl.domain.AclObjectIdentity} here because
     * test entities are never wrapped in Hibernate proxies.
     */
    // (intentionally just reusing Spring's strategy via TestAclAdvice's super call above.)

    // -------------------------------------------------------------------------------------
    // Persister registry stub
    // -------------------------------------------------------------------------------------

    /**
     * Mockito-backed substitute for {@link SessionFactoryImplementor} →
     * {@link MappingMetamodel} → {@link EntityPersister}. Each registered entity class gets
     * an EntityPersister whose {@code getPropertyTypes()} / {@code getPropertyValue} /
     * {@code getPropertyCascadeStyles} answer based on the per-entity setup the test provides.
     */
    public static class StubPersisterRegistry {
        final SessionFactory sessionFactory;
        final Map<Class<?>, PersisterBuilder> byClass = new HashMap<>();

        StubPersisterRegistry() {
            // Hand-rolled Proxy-based stubs. Mockito can't mock the Hibernate SPI interfaces
            // here (NPE on an annotation lookup deep in its bytecode generator); Proxy works
            // because we only implement the small slice of the API the listener actually calls.
            MappingMetamodelImplementor mmm = ( MappingMetamodelImplementor ) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { MappingMetamodelImplementor.class },
                    ( p, method, args ) -> {
                        if ( "getEntityDescriptor".equals( method.getName() )
                                && args != null && args.length == 1 && args[0] instanceof Class<?> ) {
                            PersisterBuilder b = byClass.get( args[0] );
                            return b == null ? null : b.build();
                        }
                        return defaultValueFor( method.getReturnType() );
                    } );
            SessionFactoryImplementor sfi = ( SessionFactoryImplementor ) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { SessionFactoryImplementor.class },
                    ( p, method, args ) -> {
                        switch ( method.getName() ) {
                            case "getMappingMetamodel": return mmm;
                            case "unwrap": return p;
                            default: return defaultValueFor( method.getReturnType() );
                        }
                    } );
            this.sessionFactory = ( SessionFactory ) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { SessionFactory.class },
                    ( p, method, args ) -> {
                        if ( "unwrap".equals( method.getName() ) ) {
                            return sfi;
                        }
                        return defaultValueFor( method.getReturnType() );
                    } );
        }

        private static Object defaultValueFor( Class<?> returnType ) {
            if ( returnType == boolean.class ) return false;
            if ( returnType == byte.class ) return ( byte ) 0;
            if ( returnType == short.class ) return ( short ) 0;
            if ( returnType == int.class ) return 0;
            if ( returnType == long.class ) return 0L;
            if ( returnType == float.class ) return 0f;
            if ( returnType == double.class ) return 0d;
            if ( returnType == char.class ) return ( char ) 0;
            return null;
        }

        PersisterBuilder register( Class<?> entityClass ) {
            return byClass.computeIfAbsent( entityClass, c -> new PersisterBuilder( c ) );
        }

        @Nullable
        EntityPersister persisterFor( Class<?> entityClass ) {
            PersisterBuilder b = byClass.get( entityClass );
            return b == null ? null : b.persister;
        }
    }

    /**
     * Fluent builder for a per-entity-class {@link EntityPersister} mock. Holds a list of
     * (Hibernate Type, CascadeStyle, value-extractor) tuples — one per declared property the
     * listener should see. {@link #wire()} (called lazily on first use) hands the assembled
     * Type[] and CascadeStyle[] back through Mockito stubs and routes
     * {@code getPropertyValue(entity, i)} to the right extractor.
     */
    public static class PersisterBuilder {
        final EntityPersister persister = mock( EntityPersister.class );
        final List<Type> types = new ArrayList<>();
        final List<CascadeStyle> cascades = new ArrayList<>();
        final List<java.util.function.Function<Object, Object>> extractors = new ArrayList<>();
        boolean wired = false;

        PersisterBuilder( Class<?> entityClass ) {
            // entityClass currently unused — could be surfaced via persister.getMappedClass() if
            // a test wants to assert class lookup, but Mockito returns null by default which is fine.
        }

        PersisterBuilder withCollectionProperty( String name, CascadeStyle cascade,
                                                  java.util.function.Function<Object, Object> extractor ) {
            CollectionType ct = mock( CollectionType.class );
            return withType( ct, cascade, extractor );
        }

        PersisterBuilder withEntityProperty( String name, @Nullable CascadeStyle cascade,
                                              java.util.function.Function<Object, Object> extractor ) {
            EntityType et = mock( EntityType.class );
            return withType( et, cascade, extractor );
        }

        private PersisterBuilder withType( Type t, @Nullable CascadeStyle cascade,
                                            java.util.function.Function<Object, Object> extractor ) {
            wired = false;
            types.add( t );
            cascades.add( cascade != null ? cascade : CascadeStyles.NONE );
            extractors.add( extractor );
            return this;
        }

        EntityPersister build() {
            if ( !wired ) {
                wire();
                wired = true;
            }
            return persister;
        }

        @SuppressWarnings("unchecked")
        private void wire() {
            when( persister.getPropertyTypes() ).thenReturn( types.toArray( new Type[0] ) );
            when( persister.getPropertyCascadeStyles() ).thenReturn( cascades.toArray( new CascadeStyle[0] ) );
            // getPropertyValue(entity, int) — route by index.
            when( persister.getPropertyValue( any(), any( Integer.class ) ) ).thenAnswer( inv -> {
                Object entity = inv.getArgument( 0 );
                int i = ( Integer ) inv.getArgument( 1 );
                return extractors.get( i ).apply( entity );
            } );
        }

    }

    // -------------------------------------------------------------------------------------
    // Test entity types
    // -------------------------------------------------------------------------------------

    /**
     * Plain top-level Securable. The tests use it as the EE-equivalent: holds two cascade
     * collections, one of Securable children and one of SecuredNotChild children.
     */
    public static class Root implements Securable {
        Long id;
        final List<Object> children = new ArrayList<>();
        final List<Object> notChildren = new ArrayList<>();

        Root( Long id ) { this.id = id; }
        @Override public Long getId() { return id; }
    }

    /**
     * SecuredChild that exposes a populated {@link #getSecurityOwner()} — covers the
     * locateParentAcl path (used when entity-author wired the back-ref explicitly).
     */
    public static class ChildWithOwner implements SecuredChild {
        Long id;
        final Securable owner;

        ChildWithOwner( Long id, Securable owner ) { this.id = id; this.owner = owner; }
        @Override public Long getId() { return id; }
        @Override public Securable getSecurityOwner() { return owner; }
    }

    /**
     * SecuredChild without a populated {@code getSecurityOwner} — the realistic case for
     * BioAssay / FactorValue persisted via ExpressionPersister. Forces the listener to rely
     * on stash / back-ref discovery. Also itself a parent: holds a children collection so we
     * can build the chain-flat test case.
     */
    public static class ChildNoOwner implements SecuredChild {
        Long id;
        final List<Object> children = new ArrayList<>();

        ChildNoOwner( Long id ) { this.id = id; }
        @Override public Long getId() { return id; }
        @Override @Nullable public Securable getSecurityOwner() { return null; }
    }

    /**
     * SecuredChild with an explicit ManyToOne back-ref to a {@link ChildNoOwner} — exercises
     * the force-flatten reconciliation when the back-ref ACL gets its own parent set late.
     */
    public static class ChildWithBackRef implements SecuredChild {
        Long id;
        final Securable middleRef;

        ChildWithBackRef( Long id, Securable middleRef ) { this.id = id; this.middleRef = middleRef; }
        @Override public Long getId() { return id; }
        @Override @Nullable public Securable getSecurityOwner() { return null; }
    }

    /**
     * SecuredChild with two ManyToOne back-refs: one to a {@link SecuredNotChild} root and
     * one to another {@link SecuredChild}. The listener's back-ref discovery should prefer
     * the SecuredChild path.
     */
    public static class TwoParentChild implements SecuredChild {
        Long id;
        final Securable notChildRef;
        final Securable securedChildRef;

        TwoParentChild( Long id, Securable notChildRef, Securable securedChildRef ) {
            this.id = id;
            this.notChildRef = notChildRef;
            this.securedChildRef = securedChildRef;
        }
        @Override public Long getId() { return id; }
        @Override @Nullable public Securable getSecurityOwner() { return null; }
    }

    /**
     * SecuredChild with only a SecuredNotChild back-ref. Exercises the fallback path of
     * back-ref discovery when no SecuredChild candidate is available.
     */
    public static class OneNotChildParent implements SecuredChild {
        Long id;
        final Securable notChildRef;

        OneNotChildParent( Long id, Securable notChildRef ) {
            this.id = id;
            this.notChildRef = notChildRef;
        }
        @Override public Long getId() { return id; }
        @Override @Nullable public Securable getSecurityOwner() { return null; }
    }

    /**
     * SecuredChild that references another instance of its own class — covers the cycle
     * guard in the recursive fixup walk.
     */
    public static class SelfRefChild implements SecuredChild {
        Long id;
        Securable self;

        SelfRefChild( Long id, @Nullable Securable self ) { this.id = id; this.self = self; }
        @Override public Long getId() { return id; }
        @Override @Nullable public Securable getSecurityOwner() { return null; }
    }

    /**
     * SecuredNotChild — its own ACL root. Must not be claimed as a descendant by any Root or
     * other parent's stash walk.
     */
    public static class NotChildRoot implements SecuredNotChild {
        Long id;

        NotChildRoot( Long id ) { this.id = id; }
        @Override public Long getId() { return id; }
    }
}
