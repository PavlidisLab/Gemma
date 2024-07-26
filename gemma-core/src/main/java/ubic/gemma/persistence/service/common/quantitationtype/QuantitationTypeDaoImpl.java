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

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
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
import ubic.gemma.persistence.util.BusinessKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

    @Autowired
    public QuantitationTypeDaoImpl( SessionFactory sessionFactory ) {
        super( QuantitationType.class, sessionFactory );
    }

    @Override
    protected QuantitationType findByBusinessKey( QuantitationType quantitationType ) {
        //        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( QuantitationType.class );
        //        BusinessKey.addRestrictions( queryObject, quantitationType );
        //        return ( QuantitationType ) queryObject.uniqueResult();
        /*
         * Using this method doesn't really make sense, since QTs are EE-specific not re-usable outside of the context
         * of replacing data for an EE. However, there are a few exceptions to this - QTs can be associated with other
         * entities, so this might cause problems. At the moment I cannot find any places this method is used, though.
         */
        throw new UnsupportedOperationException( "Searching for quantitationtypes without a qualifier for EE not supported by this DAO" );
    }

    @Override
    public QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType ) {

        // find all QTs for the experiment
        //language=HQL
        final String queryString = "select distinct quantType from ExpressionExperiment ee "
                + "inner join ee.quantitationTypes as quantType where ee  = :ee ";

        //noinspection unchecked
        List<?> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", ee ).list();

        // find all matching QTs; not necessarily for this experiment. This is lazy - we could go through the above to check each for a match.
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( QuantitationType.class );
        BusinessKey.addRestrictions( queryObject, quantitationType );
        Collection<?> qts = queryObject.list();

        // intersect that with the ones the experiment has (again, this is the lazy way to do this)
        list.retainAll( qts );

        if ( list.isEmpty() ) {
            return null;
        }
        if ( list.size() > 1 ) {
            /*
             * Ideally this wouldn't happen. We should use the one that has data attached to it.
             */
            final String q2 = "select distinct q from ProcessedExpressionDataVector v"
                    + " inner join v.quantitationType as q where v.expressionExperiment = :ee ";

            final String q3 = "select distinct q from RawExpressionDataVector v"
                    + " inner join v.quantitationType as q where v.expressionExperiment = :ee ";

            //noinspection unchecked
            List<?> l2 = this.getSessionFactory().getCurrentSession().createQuery( q2 )
                    .setParameter( "ee", ee ).list();

            //noinspection unchecked
            l2.addAll( this.getSessionFactory().getCurrentSession().createQuery( q3 )
                    .setParameter( "ee", ee ).list() );

            list.retainAll( l2 );

            if ( list.size() > 1 ) {

                throw new IllegalStateException( "Experiment has more than one used QT matching criteria: " + StringUtils.join( qts, ";" ) );
            }
        }
        return ( QuantitationType ) list.iterator().next();

    }

    @Override
    public QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType ) {
        String entityName = getSessionFactory().getClassMetadata( dataVectorType ).getEntityName();
        return ( QuantitationType ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct v.quantitationType from " + entityName + " v "
                        + "where v.expressionExperiment = :ee and v.quantitationType.name = :name" )
                .setParameter( "ee", ee )
                .setParameter( "name", name )
                .uniqueResult();
    }

    @Override
    public QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType ) {
        String entityName = getSessionFactory().getClassMetadata( dataVectorType ).getEntityName();
        return ( QuantitationType ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct v.quantitationType from " + entityName + " v "
                        + "where v.expressionExperiment = :ee and v.quantitationType.id = :id" )
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