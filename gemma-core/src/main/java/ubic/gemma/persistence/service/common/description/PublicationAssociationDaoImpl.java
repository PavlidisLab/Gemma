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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.PublicationAssociation;
import ubic.gemma.model.common.description.PublicationAssociationStatus;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Hibernate implementation of {@link PublicationAssociationDao}.
 */
@Repository
public class PublicationAssociationDaoImpl extends AbstractDao<PublicationAssociation>
        implements PublicationAssociationDao {

    /**
     * Accepted rows first, then rejected, then by id. Ordering on the enum's ordinal would make the
     * declaration order of {@link PublicationAssociationStatus} a wire contract, so the comparison is
     * spelled out instead.
     */
    private static final String STATUS_THEN_ID_ORDER =
            " order by case when pa.status = ubic.gemma.model.common.description.PublicationAssociationStatus.ACCEPTED"
                    + " then 0 else 1 end, pa.id";

    /**
     * The publication is fetch-joined, not left lazy.
     * <p>
     * gemma-rest has no open-session-in-view, so a caller that walks {@code pa.getPublication()} after
     * the service's read-only transaction has closed gets a {@code LazyInitializationException} — and
     * the read path does exactly that, building a value object per rejected publication. The reference
     * is one row and every caller of these two queries wants it, so joining it is both cheaper than a
     * query per row and the only shape that is safe to hand out detached. {@code pubAccession} rides
     * along without asking: it is mapped {@code EAGER} with {@code FetchMode.JOIN}.
     */
    private static final String WITH_PUBLICATION = " join fetch pa.publication";

    @Autowired
    public PublicationAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( PublicationAssociation.class, sessionFactory );
    }

    @Nullable
    @Override
    public PublicationAssociation findByInvestigationAndPublication( Investigation investigation,
            BibliographicReference publication ) {
        return ( PublicationAssociation ) getSessionFactory().getCurrentSession()
                .createQuery( "from PublicationAssociation pa"
                        + " where pa.investigation = :inv and pa.publication = :pub" )
                .setParameter( "inv", investigation )
                .setParameter( "pub", publication )
                .uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PublicationAssociation> findByInvestigation( Investigation investigation,
            @Nullable PublicationAssociationStatus statusFilter ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from PublicationAssociation pa" + WITH_PUBLICATION
                        + " where pa.investigation = :inv"
                        + " and ( :status is null or pa.status = :status )"
                        + STATUS_THEN_ID_ORDER )
                .setParameter( "inv", investigation )
                .setParameter( "status", statusFilter )
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PublicationAssociation> findByInvestigationAndPublications( Investigation investigation,
            Collection<BibliographicReference> publications ) {
        if ( publications.isEmpty() ) {
            return Collections.emptyList();
        }
        return getSessionFactory().getCurrentSession()
                .createQuery( "from PublicationAssociation pa" + WITH_PUBLICATION
                        + " where pa.investigation = :inv and pa.publication in :pubs"
                        + STATUS_THEN_ID_ORDER )
                .setParameter( "inv", investigation )
                .setParameterList( "pubs", publications )
                .list();
    }

    @Override
    public int rebindPublication( BibliographicReference from, BibliographicReference to ) {
        // Drop first, move second. Doing it the other way round runs the update straight into the
        // unique key on (investigation, publication) for any investigation that asserts something
        // about both references.
        //
        // Selected then deleted by id rather than in one statement: MySQL refuses a DELETE whose
        // subquery names the table being deleted from ("You can't specify target table ... for update
        // in FROM clause"), and the set is at most a handful of rows.
        @SuppressWarnings("unchecked")
        List<Long> redundant = getSessionFactory().getCurrentSession()
                .createQuery( "select pa.id from PublicationAssociation pa where pa.publication = :from"
                        + " and exists (select 1 from PublicationAssociation keep"
                        + " where keep.publication = :to and keep.investigation = pa.investigation)" )
                .setParameter( "from", from )
                .setParameter( "to", to )
                .list();
        if ( !redundant.isEmpty() ) {
            getSessionFactory().getCurrentSession()
                    .createQuery( "delete from PublicationAssociation pa where pa.id in :ids" )
                    .setParameterList( "ids", redundant )
                    .executeUpdate();
        }
        return getSessionFactory().getCurrentSession()
                .createQuery( "update PublicationAssociation pa set pa.publication = :to where pa.publication = :from" )
                .setParameter( "from", from )
                .setParameter( "to", to )
                .executeUpdate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PublicationAssociation> findRejectionsByPublication( BibliographicReference publication ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from PublicationAssociation pa"
                        + " where pa.publication = :pub and pa.status = :status"
                        + " order by pa.assertedAt desc, pa.id desc" )
                .setParameter( "pub", publication )
                .setParameter( "status", PublicationAssociationStatus.REJECTED )
                .list();
    }
}
