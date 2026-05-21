/*
 * The gemma-core project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.security.acl;

import ubic.gemma.core.security.model.Securable;
import ubic.gemma.core.security.model.SecuredChild;
import ubic.gemma.core.security.model.SecuredNotChild;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Renovations Phase 3: drive ACL maintenance directly off Hibernate's per-entity insert and
 * delete events instead of {@code @AfterReturning} AOP advice on DAO methods.
 * <p>
 * The pre-renovation {@link BaseAclAdvice} fired after a DAO method returned, then walked the
 * entity graph via reflection to discover newly-cascaded Securables that needed ACLs. That
 * model:
 * <ul>
 *   <li>Fired at the wrong time — child ids weren't assigned yet on Hibernate 6 because
 *       {@code session.merge()} defers cascaded inserts until flush, so the walk saw transient
 *       children with id=null (worked around in Phase 2 with {@code resolveManaged} +
 *       force-persist hacks).</li>
 *   <li>Double-bookkept the cascade: Hibernate already knows which entities are about to
 *       insert, and we re-discovered them by parsing persister metadata.</li>
 *   <li>Tied ACL maintenance to DAO method naming conventions
 *       ({@code create*}, {@code update*}, {@code save*}, {@code remove*}) and a deprecated
 *       Spring AOP pointcut layer.</li>
 * </ul>
 * <p>
 * Hibernate fires {@link PostInsertEvent} for each entity actually inserted during flush
 * (managed graph, id assigned, in-transaction), and {@link PostDeleteEvent} for each delete.
 * We filter by {@link Securable} and delegate to the existing {@link BaseAclAdvice} ACL
 * maintenance methods, which encapsulate the gsec-specific rules (base ACEs, parent inheritance,
 * user/group special cases) and were already package-friendly here.
 *
 * <h3>Parent-ACL discovery and the parent stash</h3>
 *
 * Many SecuredChildren have no @ManyToOne back to their security owner — e.g. BioAssay declares
 * {@code SecuredChild<ExpressionExperiment>} but the FK lives on the EE side
 * ({@code @OneToMany ee.bioAssays}). The pre-renovation advice discovered the parent purely
 * from cascade-walk context (the recursive traversal "knew" the parent because it had just
 * descended from it). A listener fires per-entity in isolation and has no walk context, so
 * the only sources of truth available at child-insert time are:
 * <ol>
 *   <li>{@code SecuredChild.getSecurityOwner()} — populated explicitly on some persistence
 *       paths (e.g. {@code ExpressionExperimentServiceImpl.addFactor}) but routinely null on
 *       others (e.g. {@code ExpressionPersister});</li>
 *   <li>persister metadata on the child — useless when the FK lives on the parent side or
 *       points to a non-securable;</li>
 *   <li>state captured when the <em>parent</em>'s PostInsertEvent fires — the parent's
 *       in-memory cascade collections still hold the (transient, id-less) child references
 *       at that moment, which we can stash by {@link System#identityHashCode} keyed lookup.</li>
 * </ol>
 * <p>
 * This listener uses (1) → (3) in order: try {@code getSecurityOwner} first (cheap, exact when
 * populated); on null, consult a thread-local parent stash populated when the parent's own
 * PostInsertEvent fired earlier in the same flush. Hibernate's default insert ordering
 * (hibernate.order_inserts=true + @OneToMany cascade) inserts parents before their cascaded
 * children, so the stash is reliably populated by the time the child's event fires.
 * <p>
 * Stash entries are popped on read (one-shot). The stash also has a hard size cap to guard
 * against thread-local leaks if a flush is interrupted before entries drain. The stash is
 * intentionally scoped to a thread rather than to a Session — Hibernate 6 removed
 * {@code PostFlushEventListener}, so there's no clean per-flush teardown hook; pop-on-read
 * plus the size cap is the pragmatic substitute.
 */
public class AclEventListener implements PostInsertEventListener, PostDeleteEventListener {

    private static final Log log = LogFactory.getLog( AclEventListener.class );

    /**
     * Upper bound on the parent stash before we assume something has leaked and clear it.
     * Sized to comfortably exceed the largest plausible single-flush cascade in Gemma
     * (a single EE persist may cascade thousands of BioAssays + BioMaterials + FactorValues).
     */
    private static final int STASH_MAX = 100_000;

    private static final ThreadLocal<Map<Integer, ObjectIdentity>> STASH =
            ThreadLocal.withInitial( HashMap::new );

    private final BaseAclAdvice aclAdvice;
    private final SessionFactory sessionFactory;

    public AclEventListener( BaseAclAdvice aclAdvice, SessionFactory sessionFactory ) {
        this.aclAdvice = aclAdvice;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void onPostInsert( PostInsertEvent event ) {
        Object entity = event.getEntity();
        if ( !( entity instanceof Securable ) ) {
            return;
        }
        if ( aclAdvice.canSkipAclCheck( entity ) ) {
            return;
        }
        Securable s = ( Securable ) entity;

        Acl parentAcl = resolveParentAcl( s );

        try {
            aclAdvice.addOrUpdateAcl( null, s, parentAcl );
        } catch ( RuntimeException ex ) {
            log.error( "Failed to add/update ACL for inserted entity " + s + ": " + ex, ex );
            throw ex;
        }

        // Once this entity's own ACL exists, populate the stash with the OIDs of its
        // cascade-reachable Securable children (in-memory, still-transient) so each child's
        // own PostInsertEvent later in this flush can recover the parent OID even when the
        // child has no FK back to this Securable.
        //
        // Chain semantics: most SecuredChildren (ED/EF/FV) declare ExpressionExperiment
        // directly as their security owner so they inherit straight from EE, not from
        // intermediate SecuredChild ancestors. Propagate this entity's OWN parent OID
        // downward when this entity is itself a SecuredChild — so e.g. ED stashes EF with
        // EE's OID, matching what locateSecuredParent (the immediate getSecurityOwner)
        // would return for EF directly. The two-hop case (ExpressionAnalysisResultSet ->
        // DifferentialExpressionAnalysis -> ExpressionExperiment) is handled by
        // locateParentAcl, which beats stash lookup and resolves ResultSet's parent
        // directly via getSecurityOwner() to DEA.
        ObjectIdentity childParentOid;
        if ( s instanceof SecuredChild ) {
            childParentOid = parentAcl != null ? parentAcl.getObjectIdentity() : null;
        } else {
            childParentOid = aclAdvice.makeObjectIdentity( s );
        }
        if ( childParentOid != null ) {
            stashChildren( s, childParentOid, new HashSet<>() );
        }
    }

    @Override
    public void onPostDelete( PostDeleteEvent event ) {
        Object entity = event.getEntity();
        if ( !( entity instanceof Securable ) ) {
            return;
        }
        Securable s = ( Securable ) entity;
        try {
            aclAdvice.deleteAcl( s );
        } catch ( RuntimeException ex ) {
            log.error( "Failed to delete ACL for removed entity " + s + ": " + ex, ex );
            throw ex;
        }
    }

    @Override
    public boolean requiresPostCommitHandling( EntityPersister persister ) {
        // false = fire at flush, in the same transaction; ACL ops roll back with the rest of
        // the tx on failure. true would defer to post-commit, which is wrong for our semantics.
        return false;
    }

    /**
     * Look up the parent ACL for a Securable using, in order:
     * <ol>
     *   <li>explicit {@code getSecurityOwner()} — set by some persistence paths (e.g.
     *       {@code ExpressionExperimentServiceImpl.addFactor});</li>
     *   <li>thread-local parent stash populated when this entity's parent fired
     *       PostInsertEvent earlier in the same flush (covers cascade-insert flows);</li>
     *   <li>persister-metadata back-reference walk — for entities inserted independently of
     *       their parent (typical of update flows: parent is already persisted, child is being
     *       added via a managed-side merge with no parent insert event firing). Walks the
     *       child's @ManyToOne associations; prefers a SecuredChild target over a
     *       SecuredNotChild target so we end up in the right security domain (e.g. an EF's @ManyToOne
     *       to ED is followed back through ED's existing parent ACL to find EE, not to an
     *       ArrayDesign root).</li>
     * </ol>
     * Returns null if none yields a parent — {@link BaseAclAdvice#addOrUpdateAcl} treats null
     * parent as "create a root ACL with base ACEs," which is the correct fallback for an
     * actual root Securable.
     */
    private Acl resolveParentAcl( Securable s ) {
        if ( !( s instanceof SecuredChild ) ) {
            return null;
        }
        Acl parentAcl = null;
        try {
            parentAcl = aclAdvice.locateParentAcl( ( SecuredChild ) s );
        } catch ( RuntimeException ex ) {
            if ( log.isDebugEnabled() ) {
                log.debug( "Could not locate parent ACL via getSecurityOwner for SecuredChild " + s + ": " + ex );
            }
        }
        if ( parentAcl != null ) {
            // If the entity also has a stash entry (from being added to multiple parent
            // collections), drop it to keep the stash from accumulating.
            STASH.get().remove( System.identityHashCode( s ) );
            return parentAcl;
        }
        ObjectIdentity stashed = STASH.get().remove( System.identityHashCode( s ) );
        if ( stashed != null ) {
            try {
                return aclAdvice.getAclService().readAclById( stashed );
            } catch ( NotFoundException nfe ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( "Stashed parent OID " + stashed + " has no ACL for child " + s );
                }
            }
        }
        return discoverParentViaBackRef( s );
    }

    /**
     * Walk the child's persister-declared @ManyToOne (EntityType) properties to find a
     * back-reference to a Securable parent. Prefers SecuredChild candidates over
     * SecuredNotChild ones so that, given multiple Securable back-refs, we follow the one that
     * lives in the same security domain rather than landing on an unrelated ACL root (e.g.
     * BA.sampleUsed → BM → EE is preferred over BA.arrayDesignUsed → AD which is its own root).
     * <p>
     * Returns the chain-flat top: if the back-ref's ACL has a parent (SecuredChild case),
     * returns the topmost ancestor's ACL; otherwise returns the back-ref's own ACL
     * (SecuredNotChild root case). Mirrors the chain-flattening semantics of
     * {@link BaseAclAdvice#chooseParentForAssociations}.
     */
    private Acl discoverParentViaBackRef( Securable s ) {
        SessionFactoryImplementor sfi;
        try {
            sfi = sessionFactory.unwrap( SessionFactoryImplementor.class );
        } catch ( RuntimeException ex ) {
            return null;
        }
        EntityPersister persister = sfi.getMappingMetamodel()
                .getEntityDescriptor( Hibernate.getClass( s ) );
        if ( persister == null ) {
            return null;
        }
        Type[] types = persister.getPropertyTypes();
        Acl preferredCandidate = null;
        Acl fallbackCandidate = null;
        for ( int i = 0; i < types.length; i++ ) {
            if ( !( types[i] instanceof EntityType ) ) {
                continue;
            }
            Object value;
            try {
                value = persister.getPropertyValue( s, i );
            } catch ( RuntimeException ex ) {
                continue;
            }
            if ( !( value instanceof Securable ) ) {
                continue;
            }
            Securable target = ( Securable ) value;
            if ( target.getId() == null ) {
                continue;
            }
            Acl targetAcl;
            try {
                targetAcl = aclAdvice.getAclService().readAclById( aclAdvice.makeObjectIdentity( target ) );
            } catch ( NotFoundException nfe ) {
                continue;
            } catch ( RuntimeException ex ) {
                continue;
            }
            Acl chainTop = topmost( targetAcl );
            if ( target instanceof SecuredChild ) {
                if ( preferredCandidate == null ) {
                    preferredCandidate = chainTop;
                }
            } else if ( target instanceof SecuredNotChild ) {
                if ( fallbackCandidate == null ) {
                    fallbackCandidate = chainTop;
                }
            }
        }
        return preferredCandidate != null ? preferredCandidate : fallbackCandidate;
    }

    private static Acl topmost( Acl acl ) {
        Acl cur = acl;
        while ( cur.getParentAcl() != null ) {
            cur = cur.getParentAcl();
        }
        return cur;
    }

    /**
     * Walk {@code parent}'s persister-declared collection properties; for each Securable
     * element that isn't itself a {@link SecuredNotChild} (i.e. isn't its own ACL root), stash
     * {@code System.identityHashCode(child) → childParentOid}. Called after the parent's own
     * ACL has been created so the stashed OID is guaranteed to resolve when each child's
     * PostInsertEvent later fires.
     * <p>
     * {@code childParentOid} is the OID that children should adopt as their parent ACL — for a
     * top-level Securable {@code parent} this is {@code parent}'s own OID; for a SecuredChild
     * {@code parent} this is propagated downward (the chain-flattening described in
     * {@link #onPostInsert}).
     * <p>
     * Why {@link System#identityHashCode}: at this point in flush the children are still
     * transient (id=null) and their {@code equals/hashCode} is whatever the entity defines —
     * which may dereference a not-yet-loaded field or be content-based (so two distinct
     * transient instances with identical fields would collide). identityHashCode is reference
     * identity, stable for the lifetime of the object, and zero collision risk during a single
     * flush.
     */
    private void stashChildren( Securable parent, ObjectIdentity childParentOid, Set<Integer> visited ) {
        // Cycle guard: a parent's object graph may contain references back to itself or to a
        // shared descendant (e.g. BM.sourceBioMaterial pointing to another BM, two BAs sharing
        // a sampleUsed). Track entities we've already walked in this top-level call so the
        // retroactive recursion terminates.
        if ( !visited.add( System.identityHashCode( parent ) ) ) {
            return;
        }
        SessionFactoryImplementor sfi;
        try {
            sfi = sessionFactory.unwrap( SessionFactoryImplementor.class );
        } catch ( RuntimeException ex ) {
            log.warn( "Could not unwrap SessionFactoryImplementor; skipping parent-stash walk: " + ex );
            return;
        }
        EntityPersister persister = sfi.getMappingMetamodel()
                .getEntityDescriptor( Hibernate.getClass( parent ) );
        if ( persister == null ) {
            return;
        }
        Type[] types = persister.getPropertyTypes();
        Map<Integer, ObjectIdentity> stash = STASH.get();
        if ( stash.size() >= STASH_MAX ) {
            log.warn( "ParentAclStash exceeded " + STASH_MAX + " entries; clearing as a leak guard. "
                    + "Indicates a flush was interrupted without draining stashed children." );
            stash.clear();
        }
        CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
        for ( int i = 0; i < types.length; i++ ) {
            Type t = types[i];
            boolean isCollection = t instanceof CollectionType;
            boolean isEntity = t instanceof EntityType;
            if ( !isCollection && !isEntity ) {
                continue;
            }
            // Walk every Collection/Entity association, regardless of cascade. The cascade flag
            // gates whether a TRANSIENT child should be stashed for its future PostInsertEvent
            // (only cascading ones will actually be inserted by this transaction's flush). For
            // ALREADY-PERSISTED children — typical when FK ordering or independent persistence
            // ran the child's PostInsertEvent before its parent's — we walk regardless, because
            // those orphan ACLs need retroactive parent fixup. The SecuredNotChild check inside
            // {@link #handleChild} prevents accidentally claiming separate ACL roots.
            boolean cascades = cascadesPersist( cascadeStyles[i] );
            Object value;
            try {
                value = persister.getPropertyValue( parent, i );
            } catch ( RuntimeException ex ) {
                continue;
            }
            if ( value == null ) {
                continue;
            }
            if ( isCollection ) {
                if ( !( value instanceof Collection ) ) {
                    continue;
                }
                for ( Object child : ( Collection<?> ) value ) {
                    handleChild( child, childParentOid, stash, cascades, visited );
                }
            } else {
                handleChild( value, childParentOid, stash, cascades, visited );
            }
        }
    }

    private void handleChild( Object child, ObjectIdentity childParentOid,
                              Map<Integer, ObjectIdentity> stash, boolean parentCascades,
                              Set<Integer> visited ) {
        if ( !( child instanceof Securable ) ) {
            return;
        }
        if ( child instanceof SecuredNotChild ) {
            // SecuredNotChild is its own ACL root — don't claim it as our descendant.
            return;
        }
        Securable cs = ( Securable ) child;
        if ( cs.getId() == null ) {
            // Transient. Only stash if this association cascades — otherwise the child won't
            // be inserted by this flush and the stash entry would never be consumed (leaks
            // until the size cap clears them).
            if ( parentCascades ) {
                stash.put( System.identityHashCode( cs ), childParentOid );
            }
            return;
        }
        // Already persisted. Either it inserted before us due to FK ordering (e.g. EE →
        // @ManyToOne ED, cascade-all, ED's row has to exist first), or it was persisted
        // independently and is now being referenced via a non-cascading @ManyToOne (e.g.
        // BioMaterial referenced from BA.sampleUsed, persisted earlier by ExpressionPersister).
        // Reconcile its parent to {@code childParentOid} (the top-level security owner):
        // <ul>
        //   <li>If currently orphan → assign via {@link BaseAclAdvice#addOrUpdateAcl}
        //       (which routes to maybeSetParentACL).</li>
        //   <li>If currently parented to something OTHER than the top → force-flatten by
        //       directly setting the parent on the MutableAcl. This happens when a child's
        //       own PostInsertEvent ran an earlier back-ref discovery that picked up an
        //       intermediate ancestor (e.g. EF.persister.experimentalDesign → ED) before EE
        //       was even inserted; the back-ref ACL was then still orphan, so chain-flat
        //       resolution couldn't see EE.</li>
        //   <li>If currently parented to the top → no-op (cycle break for shared descendants).</li>
        // </ul>
        // Recursion always proceeds so deep subtrees get reconciled; the visited set in
        // stashChildren is what actually stops cycles.
        try {
            Acl existing = aclAdvice.getAclService().readAclById( aclAdvice.makeObjectIdentity( cs ) );
            Acl currentParent = existing.getParentAcl();
            if ( currentParent == null ) {
                Acl parentAcl = aclAdvice.getAclService().readAclById( childParentOid );
                aclAdvice.addOrUpdateAcl( null, cs, parentAcl );
            } else if ( !childParentOid.equals( currentParent.getObjectIdentity() )
                    && existing instanceof MutableAcl ) {
                MutableAcl macl = ( MutableAcl ) existing;
                Acl newParent = aclAdvice.getAclService().readAclById( childParentOid );
                macl.setParent( newParent );
                macl.setEntriesInheriting( true );
                aclAdvice.getAclService().updateAcl( macl );
            }
        } catch ( RuntimeException ex ) {
            log.warn( "Could not reconcile parent ACL on already-persisted child "
                    + cs + " (parent OID " + childParentOid + "): " + ex );
            return;
        }
        // Recurse with the SAME OID so the whole subtree adopts the propagated security
        // owner as parent. For the ED/EF/FV chain this naturally hits EE; for the two-hop
        // DEA -> ResultSet case, ResultSet's own PostInsert hits locateParentAcl first
        // (which now returns DEA directly), so the stash entry is just a fallback.
        stashChildren( cs, childParentOid, visited );
    }

    private static boolean cascadesPersist( CascadeStyle cs ) {
        return cs != null
                && ( cs.doCascade( CascadingActions.PERSIST )
                        || cs.doCascade( CascadingActions.PERSIST_ON_FLUSH ) );
    }
}
