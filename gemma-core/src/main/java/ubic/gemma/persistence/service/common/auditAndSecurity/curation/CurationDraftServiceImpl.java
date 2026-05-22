/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDraft;
import ubic.gemma.model.expression.experiment.AgentProposal;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link CurationDraftService} implementation.
 *
 * <p>The service owns the {@code startedAt} / {@code lastEditedAt}
 * timestamps; callers never pass them in. Snapshot capture is done at
 * seed/save time when {@code proposalId} first appears on the row, so
 * subsequent saves of the same draft don't keep rewriting the snapshot —
 * the snapshot is the disposition baseline for the row's lifetime.</p>
 */
@Service
public class CurationDraftServiceImpl implements CurationDraftService {

    private final CurationDraftDao curationDraftDao;
    private final SessionFactory sessionFactory;

    @Autowired
    public CurationDraftServiceImpl( CurationDraftDao curationDraftDao,
            SessionFactory sessionFactory ) {
        this.curationDraftDao = curationDraftDao;
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurationDraft> findForCurator( Long investigationId, User curator ) {
        Assert.notNull( investigationId, "investigationId must not be null." );
        Assert.notNull( curator, "curator must not be null." );
        Investigation inv = loadInvestigation( investigationId );
        if ( inv == null ) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                curationDraftDao.findByInvestigationAndCurator( inv, ( Contact ) curator ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurationDraft> findById( Long draftId ) {
        if ( draftId == null ) return Optional.empty();
        return Optional.ofNullable( curationDraftDao.load( draftId ) );
    }

    @Override
    @Transactional
    public CurationDraft saveOrUpdate( Long investigationId, User curator,
            String payloadJson,
            @Nullable Long proposalId,
            @Nullable String parkedElementsJson ) {
        Assert.notNull( investigationId, "investigationId must not be null." );
        Assert.notNull( curator, "curator must not be null." );
        Assert.notNull( payloadJson, "payloadJson must not be null." );
        Investigation inv = loadInvestigation( investigationId );
        if ( inv == null ) {
            throw new IllegalArgumentException( "No investigation with id " + investigationId );
        }
        CurationDraft draft = curationDraftDao.findByInvestigationAndCurator( inv, ( Contact ) curator );
        Date now = new Date();
        boolean isNew = draft == null;
        if ( isNew ) {
            draft = new CurationDraft();
            draft.setInvestigation( inv );
            draft.setCurator( curator );
            draft.setStartedAt( now );
        }
        draft.setPayloadJson( payloadJson );
        if ( proposalId != null ) {
            // Bind / rebind the proposal. Only refresh the snapshot if the
            // row didn't already carry this proposal id — once captured, the
            // snapshot is the disposition baseline for the draft's lifetime.
            AgentProposal current = draft.getProposal();
            if ( current == null || !proposalId.equals( current.getId() ) ) {
                AgentProposal p = ( AgentProposal ) sessionFactory.getCurrentSession()
                        .get( AgentProposal.class, proposalId );
                if ( p == null ) {
                    throw new IllegalArgumentException( "No AgentProposal with id " + proposalId );
                }
                draft.setProposal( p );
                draft.setProposalSnapshotJson( p.getPayloadJson() );
            }
        }
        if ( parkedElementsJson != null ) {
            draft.setParkedElements( parkedElementsJson );
        }
        draft.setLastEditedAt( now );
        if ( isNew ) {
            return curationDraftDao.create( draft );
        } else {
            curationDraftDao.update( draft );
            return draft;
        }
    }

    @Override
    @Transactional
    public CurationDraft seedFromProposal( Long investigationId, User curator,
            AgentProposal proposal, String initialPayloadJson ) {
        Assert.notNull( investigationId, "investigationId must not be null." );
        Assert.notNull( curator, "curator must not be null." );
        Assert.notNull( proposal, "proposal must not be null." );
        Assert.notNull( initialPayloadJson, "initialPayloadJson must not be null." );
        Investigation inv = loadInvestigation( investigationId );
        if ( inv == null ) {
            throw new IllegalArgumentException( "No investigation with id " + investigationId );
        }
        CurationDraft draft = curationDraftDao.findByInvestigationAndCurator( inv, ( Contact ) curator );
        Date now = new Date();
        boolean isNew = draft == null;
        if ( isNew ) {
            draft = new CurationDraft();
            draft.setInvestigation( inv );
            draft.setCurator( curator );
            draft.setStartedAt( now );
        }
        draft.setPayloadJson( initialPayloadJson );
        draft.setProposal( proposal );
        // Seed call ALWAYS (re-)captures the snapshot — distinct from
        // saveOrUpdate, which preserves a once-captured snapshot.
        draft.setProposalSnapshotJson( proposal.getPayloadJson() );
        draft.setLastEditedAt( now );
        if ( isNew ) {
            return curationDraftDao.create( draft );
        } else {
            curationDraftDao.update( draft );
            return draft;
        }
    }

    @Override
    @Transactional
    public void delete( Long investigationId, User curator ) {
        Assert.notNull( investigationId, "investigationId must not be null." );
        Assert.notNull( curator, "curator must not be null." );
        Investigation inv = loadInvestigation( investigationId );
        if ( inv == null ) return;
        CurationDraft draft = curationDraftDao.findByInvestigationAndCurator( inv, ( Contact ) curator );
        if ( draft != null ) {
            curationDraftDao.remove( draft );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurationDraft> findByCurator( User curator, @Nullable Date since, int offset, int limit ) {
        Assert.notNull( curator, "curator must not be null." );
        return curationDraftDao.findByCurator( ( Contact ) curator, since, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurationDraft> findByProposal( Long proposalId ) {
        Assert.notNull( proposalId, "proposalId must not be null." );
        return curationDraftDao.findByProposal( proposalId );
    }

    @Override
    @Transactional
    public CurationDraft finalize( Long investigationId, User curator ) {
        Assert.notNull( investigationId, "investigationId must not be null." );
        Assert.notNull( curator, "curator must not be null." );
        Investigation inv = loadInvestigation( investigationId );
        if ( inv == null ) {
            throw new IllegalStateException( "No investigation with id " + investigationId );
        }
        CurationDraft draft = curationDraftDao.findByInvestigationAndCurator( inv, ( Contact ) curator );
        if ( draft == null ) {
            throw new IllegalStateException( "No draft exists for investigation "
                    + investigationId + " and curator " + curator.getId() );
        }
        draft.setFinalizedAt( new Date() );
        draft.setLastEditedAt( new Date() );
        curationDraftDao.update( draft );
        return draft;
    }

    @Nullable
    private Investigation loadInvestigation( Long id ) {
        return ( Investigation ) sessionFactory.getCurrentSession()
                .get( Investigation.class, id );
    }
}
