/*
 * The gemma project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimensionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.List;

/**
 * Hibernate implementation of {@link SingleCellDimensionExperimentDao}. See PERF_PROBE_REPORT_ROUND4
 * finding B1 for the motivation.
 */
@Repository
public class SingleCellDimensionExperimentDaoImpl extends AbstractDao<SingleCellDimensionExperiment>
        implements SingleCellDimensionExperimentDao {

    @Autowired
    public SingleCellDimensionExperimentDaoImpl( SessionFactory sessionFactory ) {
        super( SingleCellDimensionExperiment.class, sessionFactory );
    }

    @Nullable
    @Override
    public SingleCellDimension findDimensionByEEAndQt( ExpressionExperiment ee, QuantitationType qt ) {
        return withoutAutoFlush( session -> ( SingleCellDimension ) session
                .createQuery( "select e.singleCellDimension from SingleCellDimensionExperiment e "
                        + "where e.expressionExperiment = :ee and e.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult() );
    }

    @Nullable
    @Override
    public SingleCellDimensionExperiment findByEEAndQt( ExpressionExperiment ee, QuantitationType qt ) {
        return withoutAutoFlush( session -> ( SingleCellDimensionExperiment ) session
                .createQuery( "from SingleCellDimensionExperiment e "
                        + "where e.expressionExperiment = :ee and e.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult() );
    }

    @Override
    public List<SingleCellDimensionExperiment> findByEE( ExpressionExperiment ee ) {
        //noinspection unchecked
        return withoutAutoFlush( session -> ( List<SingleCellDimensionExperiment> ) session
                .createQuery( "from SingleCellDimensionExperiment e where e.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list() );
    }

    @Override
    public SingleCellDimensionExperiment record( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension scd ) {
        // Use native SQL with the unique-key (EE_FK, QT_FK) for idempotency. Doing this via
        // session.persist on a managed entity left the new row in pending state through the
        // remainder of the calling flow; subsequent autoflushes (eg. the legacy
        // getNumberOfSingleCellDataVectors count in removeSingleCellVectorsAndDimensionIfNecessary)
        // would walk it and choke on transient state elsewhere in the cascade graph. A native
        // INSERT runs immediately, bypasses the Hibernate flush walk, and matches the read pattern
        // the link table is supposed to support.
        return withoutAutoFlush( session -> {
            SingleCellDimensionExperiment existing = ( SingleCellDimensionExperiment ) session
                    .createQuery( "from SingleCellDimensionExperiment e "
                            + "where e.expressionExperiment = :ee and e.quantitationType = :qt" )
                    .setParameter( "ee", ee )
                    .setParameter( "qt", qt )
                    .uniqueResult();
            if ( existing != null ) {
                if ( !existing.getSingleCellDimension().equals( scd ) ) {
                    session.createNativeQuery( "update SINGLE_CELL_DIMENSION_EXPERIMENT "
                                    + "set SINGLE_CELL_DIMENSION_FK = :scdId "
                                    + "where ID = :id" )
                            .setParameter( "scdId", scd.getId() )
                            .setParameter( "id", existing.getId() )
                            .executeUpdate();
                    existing.setSingleCellDimension( scd );
                }
                return existing;
            }
            session.createNativeQuery( "insert into SINGLE_CELL_DIMENSION_EXPERIMENT "
                            + "(EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK, SINGLE_CELL_DIMENSION_FK) "
                            + "values (:eeId, :qtId, :scdId)" )
                    .setParameter( "eeId", ee.getId() )
                    .setParameter( "qtId", qt.getId() )
                    .setParameter( "scdId", scd.getId() )
                    .executeUpdate();
            SingleCellDimensionExperiment row = new SingleCellDimensionExperiment();
            row.setExpressionExperiment( ee );
            row.setQuantitationType( qt );
            row.setSingleCellDimension( scd );
            // Note: not attached to the session — the row exists in the DB via the native insert
            // above. Callers don't need the managed entity (the maintenance hook is fire-and-forget).
            return row;
        } );
    }

    @Override
    public int removeByEEAndQt( ExpressionExperiment ee, QuantitationType qt ) {
        return withoutAutoFlush( session -> session
                .createQuery( "delete from SingleCellDimensionExperiment e "
                        + "where e.expressionExperiment = :ee and e.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .executeUpdate() );
    }

    @Override
    public int removeByEE( ExpressionExperiment ee ) {
        return withoutAutoFlush( session -> session
                .createQuery( "delete from SingleCellDimensionExperiment e "
                        + "where e.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .executeUpdate() );
    }

    @Override
    public int removeBySingleCellDimension( SingleCellDimension scd ) {
        return withoutAutoFlush( session -> session
                .createQuery( "delete from SingleCellDimensionExperiment e "
                        + "where e.singleCellDimension = :scd" )
                .setParameter( "scd", scd )
                .executeUpdate() );
    }

    /**
     * Run the given query with autoflush disabled.
     * <p>
     * The link-table maintenance hooks fire inside flows that already have transient state pending
     * (new SC vectors mid-cascade in {@code replaceSingleCellDataVectors}, etc.). Hibernate's
     * autoflush would otherwise walk that pending state and reject it with TransientObjectException
     * because the SC vector mapping has no cascade-save on its {@code singleCellDimension} field.
     * Our link-table queries don't read any of that pending state, so it's safe to skip the
     * autoflush — the caller's later explicit flush still picks up everything correctly.
     */
    private <T> T withoutAutoFlush( java.util.function.Function<Session, T> work ) {
        Session session = getSessionFactory().getCurrentSession();
        FlushMode prev = session.getHibernateFlushMode();
        try {
            session.setHibernateFlushMode( FlushMode.MANUAL );
            return work.apply( session );
        } finally {
            session.setHibernateFlushMode( prev );
        }
    }
}
