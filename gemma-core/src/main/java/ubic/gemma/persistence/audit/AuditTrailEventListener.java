/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.audit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Auditable;

/**
 * Phase 3 persister retirement (roadmap step 2): a Hibernate {@link PersistEventListener}
 * that guarantees every {@link Auditable} entity has a non-null {@link AuditTrail} attached
 * before {@code session.persist} hands it off to the cascade machinery. Replaces the
 * explicit priming in {@code PersisterHelperImpl.doPersist}, which previously called
 * {@code AuditTrailDao.create} ahead of the chain.
 * <p>
 * Once this listener is in place, removing the per-Auditable priming from the persister
 * chain becomes safe — the listener fires before any cascade, and every Auditable arriving
 * at the listener gets a fresh AuditTrail if one is somehow missing. In practice
 * {@code AbstractAuditable} initialises the field on construction so this defensive branch
 * is rarely exercised; the listener exists to document the invariant and to provide a
 * single chokepoint for the audit-trail lifecycle now that the persister hierarchy is
 * being dismantled.
 * <p>
 * HBM cascade declarations ({@code cascade="all"} on every Auditable's auditTrail
 * many-to-one — see Investigation.hbm.xml, ArrayDesign.hbm.xml, ExternalDatabase.hbm.xml,
 * GeneSet.hbm.xml, UserGroup.hbm.xml, ExpressionExperimentSet.hbm.xml) take it from there:
 * Hibernate persists the AuditTrail ahead of its parent, satisfying the
 * {@code AUDIT_TRAIL_FK NOT NULL} constraint on the parent table.
 *
 * @author phase3-agent
 * @see ubic.gemma.persistence.audit.AuditTrailEventListenerConfig
 */
public class AuditTrailEventListener implements PersistEventListener {

    private static final Log log = LogFactory.getLog( AuditTrailEventListener.class );

    @Override
    public void onPersist( PersistEvent event ) {
        ensureAuditTrail( event.getObject() );
    }

    @Override
    public void onPersist( PersistEvent event, PersistContext createdAlready ) {
        ensureAuditTrail( event.getObject() );
    }

    private void ensureAuditTrail( Object entity ) {
        if ( entity instanceof Auditable ) {
            Auditable auditable = ( Auditable ) entity;
            if ( auditable.getAuditTrail() == null ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( String.format(
                            "Auditable %s arrived at persist without an AuditTrail; initialising one defensively.",
                            entity.getClass().getSimpleName() ) );
                }
                auditable.setAuditTrail( new AuditTrail() );
            }
            // The HBM cascade="all" on the auditTrail many-to-one walks into the
            // AuditTrail and persists it ahead of the parent. No explicit priming
            // (session.persist of the AuditTrail) is needed here — doing so would
            // duplicate the cascade.
        }
    }
}
