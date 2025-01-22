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
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.*;

import static ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils.visitBioMaterials;

/**
 * @author pavlidis
 * @see    ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 */
public abstract class AbstractDesignElementDataVectorDao<T extends BulkExpressionDataVector> extends AbstractDao<T>
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
        // using tree sets to avoid initializing via hashCode()
        Set<ExpressionExperiment> ees = new TreeSet<>( Comparator.comparing( Identifiable::getId ) );
        Set<BioAssayDimension> dims = new TreeSet<>( Comparator.comparing( Identifiable::getId ) );
        for ( T vector : designElementDataVectors ) {
            thawDesignElement( vector.getDesignElement() );
            dims.add( vector.getBioAssayDimension() );
            ees.add( vector.getExpressionExperiment() );
        }

        if ( !ees.isEmpty() ) {
            eeTimer.start();
            ees.forEach( Hibernate::initialize );
            eeTimer.stop();
        }

        if ( !dims.isEmpty() ) {
            dimTimer.start();
            // recursively initialize biomaterials
            dims.forEach( this::thawBioAssayDimension );
            dimTimer.stop();
        }

        if ( timer.getTime() > 1000 ) {
            log.warn( String.format( "Thawing %d %s took %d ms (vectors: %d ms, ee: %d ms, dims: %d ms)",
                    designElementDataVectors.size(), getElementClass().getSimpleName(), timer.getTime(),
                    vTimer.getTime(), eeTimer.getTime(), dimTimer.getTime() ) );
        }
    }

    @Override
    public void thaw( T designElementDataVector ) {
        Hibernate.initialize( designElementDataVector.getExpressionExperiment() );
        thawDesignElement( designElementDataVector.getDesignElement() );
        thawBioAssayDimension( designElementDataVector.getBioAssayDimension() );
    }

    private void thawDesignElement( CompositeSequence designElement ) {
        Hibernate.initialize( designElement.getBiologicalCharacteristic() );
    }

    private void thawBioAssayDimension( BioAssayDimension dim ) {
        dim.getBioAssays().forEach( this::thawBioAssay );
    }

    private void thawBioAssay(BioAssay ba) {
        Hibernate.initialize( ba.getArrayDesignUsed() );
        Hibernate.initialize( ba.getArrayDesignUsed().getDesignProvider() );
        if ( ba.getOriginalPlatform() != null ) {
            Hibernate.initialize( ba.getOriginalPlatform() );
            Hibernate.initialize( ba.getOriginalPlatform().getDesignProvider() );
        }
        visitBioMaterials( ba.getSampleUsed(), bm -> {
            Hibernate.initialize( bm );
            Hibernate.initialize( bm.getFactorValues() );
            for ( FactorValue fv : bm.getFactorValues() ) {
                Hibernate.initialize( fv.getExperimentalFactor() );
            }
            Hibernate.initialize( bm.getTreatments() );
        } );
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
