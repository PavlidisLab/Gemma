/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.common.quantitationtype;

import org.hibernate.NonUniqueResultException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractCriteriaFilteringVoEnabledDao;

import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * {@link QuantitationType}.
 * </p>
 *
 * @see ubic.gemma.model.common.quantitationtype.QuantitationType
 */
@Repository
public class QuantitationTypeDaoImpl extends AbstractCriteriaFilteringVoEnabledDao<QuantitationType, QuantitationTypeValueObject>
        implements QuantitationTypeDao {

    private final Set<Class<? extends DataVector>> vectorTypes;

    @Autowired
    public QuantitationTypeDaoImpl( SessionFactory sessionFactory ) {
        super( QuantitationType.class, sessionFactory );
        //noinspection unchecked
        vectorTypes = sessionFactory.getAllClassMetadata().values().stream()
                .filter( cm -> DataVector.class.isAssignableFrom( cm.getMappedClass() ) )
                .map( cm -> ( Class<? extends DataVector> ) cm.getMappedClass() )
                .collect( Collectors.toSet() );
    }


    @Override
    public Collection<Class<? extends DataVector>> getVectorTypes() {
        return vectorTypes;
    }

    @Override
    public QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType ) {
        String entityName = getSessionFactory().getClassMetadata( dataVectorType ).getEntityName();
        return ( QuantitationType ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select v.quantitationType from " + entityName + " v "
                        + "where v.expressionExperiment = :ee and v.quantitationType.name = :name "
                        + "group by v.quantitationType" )
                .setParameter( "ee", ee )
                .setParameter( "name", name )
                .uniqueResult();
    }

    @Override
    public QuantitationType loadById( Long id, ExpressionExperiment ee ) {
        Set<QuantitationType> found = vectorTypes.stream()
                .map( vt -> loadByIdAndVectorType( id, ee, vt ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        if ( found.size() == 1 ) {
            return found.iterator().next();
        } else if ( found.size() > 1 ) {
            throw new NonUniqueResultException( found.size() );
        } else {
            return null;
        }
    }

    @Override
    public QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType ) {
        String entityName = getSessionFactory().getClassMetadata( dataVectorType ).getEntityName();
        return ( QuantitationType ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select v.quantitationType from " + entityName + " v "
                        + "where v.expressionExperiment = :ee and v.quantitationType.id = :id "
                        + "group by v.quantitationType" )
                .setParameter( "ee", ee )
                .setParameter( "id", id )
                .uniqueResult();
    }

    @Override
    public List<QuantitationType> loadByDescription( String description ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select q from QuantitationType q where q.description like :description" )
                .setParameter( "description", description )
                .list();
    }

    @Override
    protected QuantitationTypeValueObject doLoadValueObject( QuantitationType entity ) {
        return new QuantitationTypeValueObject( entity );
    }

    /**
     * Load {@link QuantitationTypeValueObject} in the context of an associated expression experiment.
     * <p>
     * The resulting VO has a few more fields filled which would be otherwise hidden from JSON serialization.
     * @see QuantitationTypeValueObject#QuantitationTypeValueObject(QuantitationType, ExpressionExperiment, Class)
     */
    @Override
    public List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment ee ) {
        List<QuantitationTypeValueObject> vos = loadValueObjects( qts );
        populateVectorType( vos, ee );
        return vos;
    }

    private void populateVectorType( Collection<QuantitationTypeValueObject> quantitationTypeValueObjects, ExpressionExperiment ee ) {
        if ( quantitationTypeValueObjects.isEmpty() )
            return;

        Set<Long> ids = quantitationTypeValueObjects.stream()
                .map( QuantitationTypeValueObject::getId )
                .collect( Collectors.toSet() );

        // here the order matters if there is more than one matching vector type, so try to organize types in decreasing
        // desirability
        List<Class<? extends DesignElementDataVector>> vectorTypes = new ArrayList<Class<? extends DesignElementDataVector>>() {{
            add( ProcessedExpressionDataVector.class );
            add( RawExpressionDataVector.class );
        }};

        MultiValueMap<Long, Class<? extends DesignElementDataVector>> vectorTypeById = new LinkedMultiValueMap<>();
        for ( Class<? extends DesignElementDataVector> vectorType : vectorTypes ) {
            //noinspection unchecked
            List<Long> qtIds = getSessionFactory().getCurrentSession()
                    .createQuery( "select distinct v.quantitationType.id from " + vectorType.getName() + " v where v.expressionExperiment = :ee and v.quantitationType.id in :ids" )
                    .setParameter( "ee", ee )
                    .setParameterList( "ids", optimizeParameterList( ids ) )
                    .list();
            qtIds.forEach( id -> vectorTypeById.add( id, vectorType ) );
        }

        for ( QuantitationTypeValueObject vo : quantitationTypeValueObjects ) {
            vo.setExpressionExperimentId( ee.getId() );
            List<Class<? extends DesignElementDataVector>> vts = vectorTypeById.get( vo.getId() );
            if ( vts != null ) {
                if ( vts.size() > 1 ) {
                    log.warn( String.format( "%s is associated to multiple vector types in %s: %s.", vo, ee, vts ) );
                }
                vo.setVectorType( vts.iterator().next().getName() );
            } else {
                // this is generally not a problem since the QuantitationType might be generic (i.e. representing a data transformation)
                log.info( String.format( "%s is not associated to any process/raw vectors of %s.", vo, ee ) );
            }
        }
    }
}