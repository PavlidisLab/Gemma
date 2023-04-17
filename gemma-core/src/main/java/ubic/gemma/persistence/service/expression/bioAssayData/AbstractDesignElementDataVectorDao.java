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
import org.hibernate.*;
import org.hibernate.metadata.ClassMetadata;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
        StopWatch timer = new StopWatch();
        timer.start();

        Hibernate.initialize( designElementDataVectors );

        Collection<ExpressionExperiment> ees = new HashSet<>();
        Map<BioAssayDimension, Collection<DesignElementDataVector>> dims = new HashMap<>();
        Collection<CompositeSequence> cs = new HashSet<>();
        for ( DesignElementDataVector vector : designElementDataVectors ) {
            Hibernate.initialize( vector );
            Hibernate.initialize( vector.getQuantitationType() );

            BioAssayDimension bad = vector.getBioAssayDimension();
            if ( !dims.containsKey( bad ) ) {
                dims.put( bad, new HashSet<>() );
            }

            dims.get( bad ).add( vector );
            cs.add( vector.getDesignElement() );
            ees.add( vector.getExpressionExperiment() );
        }

        if ( timer.getTime() > designElementDataVectors.size() ) {
            AbstractDao.log
                    .info( "Thaw phase 1, " + designElementDataVectors.size() + " vectors initialized in " + timer
                            .getTime() + "ms " );
        }
        timer.reset();
        timer.start();

        // lightly thaw the EEs we saw
        for ( ExpressionExperiment ee : ees ) {
            Hibernate.initialize( ee );
        }

        if ( timer.getTime() > 200 ) {
            AbstractDao.log
                    .info( "Thaw phase 2, " + ees.size() + " vector-associated expression experiments in " + timer
                            .getTime() + "ms " );
        }

        timer.reset();
        timer.start();

        // thaw the bioassayDimensions we saw -- usually one, more rarely two.
        for ( BioAssayDimension bad : dims.keySet() ) {

            BioAssayDimension tbad = ( BioAssayDimension ) this.getSessionFactory().getCurrentSession().createQuery(
                            "select distinct bad from BioAssayDimension bad join fetch bad.bioAssays ba join fetch ba.sampleUsed "
                                    + "bm join fetch ba.arrayDesignUsed left join fetch bm.factorValues fetch all properties where bad.id= :bad " )
                    .setParameter( "bad", bad.getId() ).uniqueResult();

            assert tbad != null;
            assert !dims.get( tbad ).isEmpty();

            for ( DesignElementDataVector v : designElementDataVectors ) {
                if ( v.getBioAssayDimension().getId().equals( tbad.getId() ) ) {
                    v.setBioAssayDimension( tbad );
                }
            }
        }

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Thaw phase 3, " + dims.size() + " vector-associated bioassaydimensions in " + timer
                    .getTime() + "ms " );
        }
        timer.reset();
        timer.start();

        // thaw the designelements we saw. SLOW
        long lastTime = 0;
        int count = 0;
        for ( CompositeSequence de : cs ) {
            BioSequence seq = de.getBiologicalCharacteristic();
            if ( seq == null )
                continue;
            Hibernate.initialize( seq );

            if ( ++count % 10000 == 0 ) {
                if ( timer.getTime() - lastTime > 1000 ) {
                    AbstractDao.log.info( "Thawed " + count + " vector-associated probes " + timer.getTime() + " ms" );
                }
                lastTime = timer.getTime();
            }
        }

        timer.stop();
        if ( designElementDataVectors.size() >= 2000 || timer.getTime() > 200 ) {
            AbstractDao.log.info( "Thaw phase 4 " + cs.size() + " vector-associated probes thawed in " + timer.getTime()
                    + "ms" );
        }
    }

    @Override
    public void thaw( T designElementDataVector ) {
        Session session = this.getSessionFactory().getCurrentSession();
        BioSequence seq = designElementDataVector.getDesignElement().getBiologicalCharacteristic();
        if ( seq != null ) {
            Hibernate.initialize( seq );
        }

        ArrayDesign arrayDesign = designElementDataVector.getDesignElement().getArrayDesign();
        Hibernate.initialize( arrayDesign );

        // thaw the bioassays.
        for ( BioAssay ba : designElementDataVector.getBioAssayDimension().getBioAssays() ) {
            ba = ( BioAssay ) session.get( BioAssay.class, ba.getId() );
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getSampleUsed() );
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
