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
package ubic.gemma.persistence.service.common.description;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.PublicationAssociation;
import ubic.gemma.model.common.description.PublicationAssociationRole;
import ubic.gemma.model.common.description.PublicationAssociationSource;
import ubic.gemma.model.common.description.PublicationAssociationStatus;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link PublicationAssociationService}.
 */
@Service("publicationAssociationService")
public class PublicationAssociationServiceImpl implements PublicationAssociationService {

    private static final Log log = LogFactory.getLog( PublicationAssociationServiceImpl.class );

    private final PublicationAssociationDao publicationAssociationDao;

    @Autowired
    public PublicationAssociationServiceImpl( PublicationAssociationDao publicationAssociationDao ) {
        this.publicationAssociationDao = publicationAssociationDao;
    }

    @Override
    @Transactional
    public List<PublicationAssociation> reconcile( Investigation investigation,
            @Nullable PublicationAssertion primary,
            Collection<PublicationAssertion> otherRelevant,
            @Nullable Collection<PublicationAssertion> rejected ) {
        Assert.notNull( investigation, "An investigation is required." );
        Assert.notNull( otherRelevant, "The other-relevant assertions must not be null (use an empty collection)." );

        // Null means "not speaking to rejections", which is not the same as "clear them" -- see the
        // interface javadoc. Kept as a flag rather than by substituting an empty collection, because
        // the two differ precisely in what the retraction sweep below is allowed to touch.
        boolean replacingRejections = rejected != null;
        Collection<PublicationAssertion> rejectedOrNone = rejected != null ? rejected : Collections.emptyList();

        // Desired end state, keyed by publication id. LinkedHashMap so the primary is processed first
        // and a duplicate entry in otherRelevant cannot demote it.
        Map<Long, DesiredAssertion> desired = new LinkedHashMap<>();
        if ( primary != null ) {
            desired.put( requiredPublicationId( primary ),
                    new DesiredAssertion( primary, PublicationAssociationStatus.ACCEPTED, PublicationAssociationRole.PRIMARY ) );
        }
        for ( PublicationAssertion a : otherRelevant ) {
            desired.putIfAbsent( requiredPublicationId( a ),
                    new DesiredAssertion( a, PublicationAssociationStatus.ACCEPTED, PublicationAssociationRole.OTHER_RELEVANT ) );
        }
        for ( PublicationAssertion a : rejectedOrNone ) {
            Long pubId = requiredPublicationId( a );
            DesiredAssertion clash = desired.get( pubId );
            if ( clash != null ) {
                // Deliberately not resolved by precedence: the caller has asked for the same paper to
                // be both the dataset's publication and not, and there is no reading of that which is
                // more likely to be what they meant.
                throw new IllegalArgumentException( "Publication " + describe( a.getPublication() )
                        + " is given both as a publication of " + describe( investigation )
                        + " and as rejected for it. Pick one." );
            }
            desired.put( pubId, new DesiredAssertion( a, PublicationAssociationStatus.REJECTED, null ) );
        }

        Map<Long, PublicationAssociation> held = new HashMap<>();
        for ( PublicationAssociation existing : publicationAssociationDao.findByInvestigation( investigation, null ) ) {
            held.put( existing.getPublication().getId(), existing );
        }

        // Refuse the whole reconcile before writing anything if any acceptance is blocked. The method
        // is transactional so a throw would roll back regardless, but failing up front keeps the
        // exception's message about the caller's request rather than about however far we got.
        for ( DesiredAssertion d : desired.values() ) {
            if ( d.status != PublicationAssociationStatus.ACCEPTED ) {
                continue;
            }
            PublicationAssociation blocking = blockingRejection( held.get( d.assertion.getPublication().getId() ),
                    d.assertion.getSource() );
            if ( blocking != null ) {
                throw new PublicationAssociationConflictException( conflictMessage( investigation, blocking, d.assertion ), blocking );
            }
        }

        Date now = new Date();
        String actor = currentActor();
        List<PublicationAssociation> out = new ArrayList<>();

        for ( DesiredAssertion d : desired.values() ) {
            PublicationAssociation row = held.get( d.assertion.getPublication().getId() );
            if ( row == null ) {
                row = new PublicationAssociation();
                row.setInvestigation( investigation );
                row.setPublication( d.assertion.getPublication() );
                apply( row, d, now, actor );
                out.add( publicationAssociationDao.create( row ) );
            } else {
                apply( row, d, now, actor );
                publicationAssociationDao.update( row );
                out.add( row );
            }
        }

        // Anything previously asserted and not named in this reconcile is retracted, not silently
        // demoted: an ACCEPTED row whose link has just been removed would be a claim about a link that
        // no longer exists, which is exactly the kind of drift this table is meant to end.
        //
        // 🛑 A standing REJECTED row is only swept when the caller passed a rejected collection, i.e.
        // actually said something about rejections. Silence is not a retraction. The caller's accepted
        // sets came from a read that showed it every accepted row and no rejected one -- the GET hides
        // them unless asked -- so its omissions are informative about one half and meaningless about
        // the other. Sweeping both cost GSE227854 its curator ruling on any ordinary write-back, and
        // with the ruling gone the GEO refresh re-installs the wrong paper unopposed.
        int retracted = 0;
        int rejectionsKept = 0;
        for ( Map.Entry<Long, PublicationAssociation> e : held.entrySet() ) {
            if ( desired.containsKey( e.getKey() ) ) {
                continue;
            }
            if ( !replacingRejections && e.getValue().getStatus() == PublicationAssociationStatus.REJECTED ) {
                rejectionsKept++;
                continue;
            }
            publicationAssociationDao.remove( e.getValue() );
            retracted++;
        }

        if ( !desired.isEmpty() || retracted > 0 ) {
            log.info( "Publication assertions for " + describe( investigation ) + ": " + desired.size()
                    + " standing (" + rejectedOrNone.size() + " rejected), " + retracted + " retracted"
                    + ( rejectionsKept > 0 ? ", " + rejectionsKept + " standing rejection(s) left untouched" : "" )
                    + "." );
        }
        return out;
    }

    @Override
    @Transactional
    public PublicationAssociation assertAccepted( Investigation investigation, PublicationAssertion assertion,
            PublicationAssociationRole role ) {
        Assert.notNull( investigation, "An investigation is required." );
        Assert.notNull( role, "An accepted assertion needs a role." );
        requiredPublicationId( assertion );
        return upsert( investigation, new DesiredAssertion( assertion, PublicationAssociationStatus.ACCEPTED, role ) );
    }

    @Override
    @Transactional
    public PublicationAssociation assertRejected( Investigation investigation, PublicationAssertion assertion ) {
        Assert.notNull( investigation, "An investigation is required." );
        requiredPublicationId( assertion );
        Long pubId = assertion.getPublication().getId();
        // A rejection recorded against a live link would leave the two halves contradicting each
        // other, and this service does not own the links, so it cannot fix that itself.
        if ( investigation.getPrimaryPublication() != null
                && pubId.equals( investigation.getPrimaryPublication().getId() ) ) {
            throw new IllegalStateException( "Cannot reject " + describe( assertion.getPublication() ) + " for "
                    + describe( investigation ) + ": it is currently the primary publication. Remove the link and"
                    + " record the rejection together, through ExpressionExperimentService.updatePublications." );
        }
        for ( BibliographicReference other : investigation.getOtherRelevantPublications() ) {
            if ( pubId.equals( other.getId() ) ) {
                throw new IllegalStateException( "Cannot reject " + describe( assertion.getPublication() ) + " for "
                        + describe( investigation ) + ": it is currently an other-relevant publication. Remove the link"
                        + " and record the rejection together, through"
                        + " ExpressionExperimentService.updatePublications." );
            }
        }
        return upsert( investigation, new DesiredAssertion( assertion, PublicationAssociationStatus.REJECTED, null ) );
    }

    /**
     * Create or update the single row for one desired assertion, enforcing precedence. Shared by the
     * two incremental entry points; {@link #reconcile} does its own batched version so it can refuse
     * the whole request before writing any of it.
     */
    private PublicationAssociation upsert( Investigation investigation, DesiredAssertion d ) {
        PublicationAssociation row = publicationAssociationDao
                .findByInvestigationAndPublication( investigation, d.assertion.getPublication() );
        if ( d.status == PublicationAssociationStatus.ACCEPTED ) {
            PublicationAssociation blocking = blockingRejection( row, d.assertion.getSource() );
            if ( blocking != null ) {
                throw new PublicationAssociationConflictException(
                        conflictMessage( investigation, blocking, d.assertion ), blocking );
            }
        }
        Date now = new Date();
        String actor = currentActor();
        if ( row == null ) {
            row = new PublicationAssociation();
            row.setInvestigation( investigation );
            row.setPublication( d.assertion.getPublication() );
            apply( row, d, now, actor );
            return publicationAssociationDao.create( row );
        }
        apply( row, d, now, actor );
        publicationAssociationDao.update( row );
        return row;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public PublicationAssociation findBlockingRejection( Investigation investigation,
            BibliographicReference publication, PublicationAssociationSource source ) {
        Assert.notNull( investigation, "An investigation is required." );
        Assert.notNull( publication, "A publication is required." );
        Assert.notNull( source, "A source is required — precedence cannot be evaluated without one." );
        return blockingRejection( publicationAssociationDao.findByInvestigationAndPublication( investigation, publication ), source );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public PublicationAssociation find( Investigation investigation, BibliographicReference publication ) {
        return publicationAssociationDao.findByInvestigationAndPublication( investigation, publication );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicationAssociation> findByInvestigation( Investigation investigation,
            @Nullable PublicationAssociationStatus statusFilter ) {
        return publicationAssociationDao.findByInvestigation( investigation, statusFilter );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PublicationAssociation> findByPublications( Investigation investigation,
            Collection<BibliographicReference> publications ) {
        Map<Long, PublicationAssociation> byPublicationId = new HashMap<>();
        for ( PublicationAssociation pa : publicationAssociationDao.findByInvestigationAndPublications( investigation, publications ) ) {
            byPublicationId.put( pa.getPublication().getId(), pa );
        }
        return byPublicationId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicationAssociation> findRejections( BibliographicReference publication ) {
        return publicationAssociationDao.findRejectionsByPublication( publication );
    }

    @Override
    @Transactional
    public int rebindPublication( BibliographicReference from, BibliographicReference to ) {
        Assert.notNull( from, "A source publication is required." );
        Assert.notNull( to, "A target publication is required." );
        Assert.isTrue( !from.equals( to ), "Cannot rebind a publication onto itself." );
        int moved = publicationAssociationDao.rebindPublication( from, to );
        if ( moved > 0 ) {
            log.info( "Moved " + moved + " publication assertion(s) from " + describe( from ) + " to " + describe( to ) + "." );
        }
        return moved;
    }

    /**
     * The rule, in one place: a standing rejection blocks an acceptance unless the incoming source
     * outranks the one that made it. Equal rank passes — two curators in sequence, or a re-import
     * correcting an earlier import, should behave the way anyone would expect.
     */
    @Nullable
    private PublicationAssociation blockingRejection( @Nullable PublicationAssociation held,
            PublicationAssociationSource incoming ) {
        if ( held == null || held.getStatus() != PublicationAssociationStatus.REJECTED ) {
            return null;
        }
        return incoming.outranks( held.getSource() ) ? null : held;
    }

    /**
     * Write the desired state onto a row.
     * <p>
     * Status and role always follow the caller — they describe the link as it now is. The provenance
     * (source, who, when, evidence) is rewritten only when something actually happened: the row is
     * new, or the decision changed, or the incoming claim states a basis. And then only if the
     * incoming source outranks the recorded one.
     * <p>
     * Both halves of that guard earn their place. Without the "something happened" half, any
     * set-replace that merely re-sends the current list — a curator committing an unrelated section,
     * a client PUTting back what it just read — would relabel a link GEO supplied as one a curator
     * asserted, quietly promoting it from rank 30 to 40 and erasing the fact that GEO is where it came
     * from. Without the rank half, a nightly re-import would keep replacing a curator's reasoning with
     * its own boilerplate while leaving the conclusion unchanged, which reads as agreement and is not.
     */
    private void apply( PublicationAssociation row, DesiredAssertion d, Date now, @Nullable String actor ) {
        PublicationAssertion a = d.assertion;
        boolean isNew = row.getId() == null;
        boolean decisionChanged = !isNew && ( row.getStatus() != d.status || row.getRole() != d.role );

        row.setStatus( d.status );
        row.setRole( d.role );

        if ( isNew ) {
            row.setSource( a.getSource() );
            row.setAssertedAt( now );
            row.setAssertedBy( a.getAssertedBy() != null ? a.getAssertedBy() : actor );
            row.setEvidence( a.getEvidence() );
            row.setSupportingEvidence( a.getSupportingEvidence() );
            row.setEvidenceCode( a.getEvidenceCode() );
            row.setConfidence( a.getConfidence() );
            return;
        }

        if ( !a.getSource().outranks( row.getSource() ) || !( decisionChanged || a.hasEvidence() ) ) {
            return;
        }
        row.setSource( a.getSource() );
        row.setAssertedAt( now );
        row.setAssertedBy( a.getAssertedBy() != null ? a.getAssertedBy() : actor );
        if ( a.hasEvidence() ) {
            row.setEvidence( a.getEvidence() );
            row.setSupportingEvidence( a.getSupportingEvidence() );
            row.setEvidenceCode( a.getEvidenceCode() );
            row.setConfidence( a.getConfidence() );
        }
    }

    private String conflictMessage( Investigation investigation, PublicationAssociation blocking,
            PublicationAssertion incoming ) {
        return "Cannot attach publication " + describe( blocking.getPublication() ) + " to "
                + describe( investigation ) + ": it was rejected by "
                + blocking.getSource().getDbValue()
                + ( blocking.getAssertedBy() != null ? " (" + blocking.getAssertedBy() + ")" : "" )
                + " on " + blocking.getAssertedAt()
                + ( blocking.getEvidence() != null ? " — " + blocking.getEvidence() : "" )
                + ". " + incoming.getSource().getDbValue() + " does not outrank that.";
    }

    private Long requiredPublicationId( PublicationAssertion a ) {
        Assert.notNull( a.getPublication(), "Every assertion needs a publication." );
        Assert.notNull( a.getPublication().getId(),
                "Publication references must be persistent before they can be asserted; resolve the identifier first." );
        Assert.notNull( a.getSource(), "Every assertion needs a source — precedence cannot be evaluated without one." );
        return a.getPublication().getId();
    }

    @Nullable
    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private String describe( @Nullable Investigation investigation ) {
        if ( investigation == null ) {
            return "an unknown experiment";
        }
        // Short name lives on ExpressionExperiment, not on Investigation, and it is the only label a
        // reader of these messages recognises ("GSE227854", not "id=27929").
        if ( investigation instanceof ExpressionExperiment
                && ( ( ExpressionExperiment ) investigation ).getShortName() != null ) {
            return ( ( ExpressionExperiment ) investigation ).getShortName() + " (id=" + investigation.getId() + ")";
        }
        return "experiment id=" + investigation.getId();
    }

    private String describe( @Nullable BibliographicReference ref ) {
        if ( ref == null ) {
            return "an unknown publication";
        }
        return ref.getPubAccession() != null && ref.getPubAccession().getAccession() != null
                ? "PubMed " + ref.getPubAccession().getAccession() + " (id=" + ref.getId() + ")"
                : "id=" + ref.getId();
    }

    /**
     * One entry of the requested end state: the incoming claim plus the slot it is being put in.
     */
    private static final class DesiredAssertion {
        private final PublicationAssertion assertion;
        private final PublicationAssociationStatus status;
        @Nullable
        private final PublicationAssociationRole role;

        private DesiredAssertion( PublicationAssertion assertion, PublicationAssociationStatus status,
                @Nullable PublicationAssociationRole role ) {
            this.assertion = assertion;
            this.status = status;
            this.role = role;
        }
    }
}
