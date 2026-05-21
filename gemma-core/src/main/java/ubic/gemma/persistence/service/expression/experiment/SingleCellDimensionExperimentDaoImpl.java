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
        return ( SingleCellDimension ) getSessionFactory().getCurrentSession()
                .createQuery( "select e.singleCellDimension from SingleCellDimensionExperiment e "
                        + "where e.expressionExperiment = :ee and e.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult();
    }

    @Nullable
    @Override
    public SingleCellDimensionExperiment findByEEAndQt( ExpressionExperiment ee, QuantitationType qt ) {
        return ( SingleCellDimensionExperiment ) getSessionFactory().getCurrentSession()
                .createQuery( "from SingleCellDimensionExperiment e "
                        + "where e.expressionExperiment = :ee and e.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult();
    }

    @Override
    public List<SingleCellDimensionExperiment> findByEE( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "from SingleCellDimensionExperiment e where e.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public SingleCellDimensionExperiment record( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension scd ) {
        SingleCellDimensionExperiment existing = findByEEAndQt( ee, qt );
        if ( existing != null ) {
            if ( !existing.getSingleCellDimension().equals( scd ) ) {
                existing.setSingleCellDimension( scd );
                getSessionFactory().getCurrentSession().update( existing );
            }
            return existing;
        }
        SingleCellDimensionExperiment row = new SingleCellDimensionExperiment();
        row.setExpressionExperiment( ee );
        row.setQuantitationType( qt );
        row.setSingleCellDimension( scd );
        getSessionFactory().getCurrentSession().persist( row );
        return row;
    }

    @Override
    public int removeByEEAndQt( ExpressionExperiment ee, QuantitationType qt ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "delete from SingleCellDimensionExperiment e "
                        + "where e.expressionExperiment = :ee and e.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .executeUpdate();
    }

    @Override
    public int removeByEE( ExpressionExperiment ee ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "delete from SingleCellDimensionExperiment e "
                        + "where e.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .executeUpdate();
    }

    @Override
    public int removeBySingleCellDimension( SingleCellDimension scd ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "delete from SingleCellDimensionExperiment e "
                        + "where e.singleCellDimension = :scd" )
                .setParameter( "scd", scd )
                .executeUpdate();
    }
}
