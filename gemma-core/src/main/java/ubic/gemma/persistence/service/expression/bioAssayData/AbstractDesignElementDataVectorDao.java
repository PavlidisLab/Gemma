/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static ubic.gemma.persistence.util.QueryUtils.optimizeIdentifiableParameterList;

/**
 * @author pavlidis
 * @see    ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 */
public abstract class AbstractDesignElementDataVectorDao<T extends DesignElementDataVector> extends AbstractDao<T>
        implements DesignElementDataVectorDao<T> {

    protected AbstractDesignElementDataVectorDao( Class<T> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    protected AbstractDesignElementDataVectorDao( Class<T> elementClass, SessionFactory sessionFactory, ClassMetadata classMetadata ) {
        super( elementClass, sessionFactory, classMetadata );
    }

    @Override
    public Collection<T> find( BioAssayDimension bioAssayDimension ) {
        return findByProperty( "bioAssayDimension", bioAssayDimension );
    }

    @Override
    public Collection<T> find( Collection<QuantitationType> quantitationTypes ) {
        return findByPropertyIn( "quantitationType", quantitationTypes );
    }


    @Override
    public void thaw( Collection<T> designElementDataVectors ) {
        if ( designElementDataVectors.isEmpty() ) {
            return;
        }

        StopWatch timer = StopWatch.createStarted();
        StopWatch vTimer = StopWatch.create(),
                eeTimer = StopWatch.create(),
                dimTimer = StopWatch.create();

        // this is generally fast since vectors should be in the session already
        vTimer.start();
        Hibernate.initialize( designElementDataVectors );
        vTimer.stop();

        // collect all the entities to thaw
        Set<ExpressionExperiment> ees = new HashSet<>( designElementDataVectors.size() );
        Set<BioAssayDimension> dims = new HashSet<>( designElementDataVectors.size() );
        for ( DesignElementDataVector vector : designElementDataVectors ) {
            dims.add( vector.getBioAssayDimension() );
            ees.add( vector.getExpressionExperiment() );
        }

        if ( !ees.isEmpty() ) {
            eeTimer.start();
            this.getSessionFactory().getCurrentSession()
                    .createQuery( "select ee from ExpressionExperiment ee where ee in :ees" )
                    .setParameterList( "ees", optimizeIdentifiableParameterList( ees ) )
                    .list();
            eeTimer.stop();
        }

        if ( !dims.isEmpty() ) {
            dimTimer.start();
            this.getSessionFactory().getCurrentSession().createQuery(
                            "select distinct bad from BioAssayDimension bad "
                                    + "left join fetch bad.bioAssays ba "
                                    + "left join fetch ba.sampleUsed bm "
                                    + "left join fetch ba.originalPlatform "
                                    + "left join fetch ba.arrayDesignUsed "
                                    + "left join fetch bm.factorValues fv "
                                    + "left join fetch fv.experimentalFactor "
                                    + "fetch all properties "
                                    + "where bad in :dims" )
                    .setParameterList( "dims", optimizeIdentifiableParameterList( dims ) )
                    .list();
            dimTimer.stop();
        }

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.warn( String.format( "Thawing %d %s took %d ms (vectors: %d ms, ee: %d ms, dims: %d ms)",
                    designElementDataVectors.size(), elementClass.getSimpleName(), timer.getTime(),
                    vTimer.getTime(), eeTimer.getTime(), dimTimer.getTime() ) );
        }
    }

    @Override
    public void thaw( T designElementDataVector ) {
        Hibernate.initialize( designElementDataVector.getExpressionExperiment() );
        Hibernate.initialize( designElementDataVector.getBioAssayDimension() );
        // thaw the bioassays.
        for ( BioAssay ba : designElementDataVector.getBioAssayDimension().getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getSampleUsed() );
            Hibernate.initialize( ba.getSampleUsed().getFactorValues() );
            Hibernate.initialize( ba.getOriginalPlatform() );
        }
    }

    @Override
    public Collection<T> find( QuantitationType quantitationType ) {
        return new HashSet<>( this.findByProperty( "quantitationType", quantitationType ) );
    }

    @Override
    public Collection<T> findByExpressionExperiment( ExpressionExperiment ee ) {
        return findByProperty( "expressionExperiment", ee );
    }
}
