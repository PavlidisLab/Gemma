/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author paul
 */
@Repository
public class RawExpressionDataVectorDaoImpl extends DesignElementDataVectorDaoImpl<RawExpressionDataVector>
        implements RawExpressionDataVectorDao {

    @Autowired
    public RawExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super( RawExpressionDataVector.class, sessionFactory );
    }

    @Override
    public ExpressionExperiment addVectors( Long eeId, Collection<RawExpressionDataVector> vectors ) {
        ExpressionExperiment ee = this.getHibernateTemplate().load( ExpressionExperiment.class, eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Experiment with id=" + eeId + " not found" );
        }
        ee.getRawExpressionDataVectors().addAll( vectors );
        this.getHibernateTemplate().update( ee );
        return ee;
    }

    @Override
    public Collection<RawExpressionDataVector> find( ArrayDesign arrayDesign, QuantitationType quantitationType ) {
        final String queryString =
                "select dev from RawExpressionDataVectorImpl dev  inner join fetch dev.bioAssayDimension bd "
                        + " inner join fetch dev.designElement de inner join fetch dev.quantitationType inner join de.arrayDesign ad where ad.id = :adid "
                        + "and dev.quantitationType = :quantitationType ";

        //noinspection unchecked
        return this.getSession().createQuery( queryString ).setParameter( "quantitationType", quantitationType )
                .setParameter( "adid", arrayDesign.getId() ).list();

    }

    @Override
    public Collection<? extends DesignElementDataVector> find( BioAssayDimension bioAssayDimension ) {
        Collection<? extends DesignElementDataVector> results = new HashSet<>();

        //noinspection unchecked
        results.addAll( this.getSession()
                .createQuery( "select d from RawExpressionDataVectorImpl d where d.bioAssayDimension = :bad" )
                .setParameter( "bad", bioAssayDimension ).list() );

        //noinspection unchecked
        results.addAll( this.getSession()
                .createQuery( "select d from ProcessedExpressionDataVectorImpl d where d.bioAssayDimension = :bad" )
                .setParameter( "bad", bioAssayDimension ).list() );
        return results;

    }

    @Override
    public Collection<RawExpressionDataVector> find( Collection<QuantitationType> quantitationTypes ) {
        final String queryString = "select dev from RawExpressionDataVectorImpl dev  where  "
                + "  dev.quantitationType in ( :quantitationTypes) ";
        //noinspection unchecked
        return this.getSession().createQuery( queryString ).setParameterList( "quantitationTypes", quantitationTypes )
                .list();
    }

    @Override
    public Collection<RawExpressionDataVector> find( QuantitationType quantitationType ) {
        return this.findByProperty( "quantitationType", quantitationType );
    }

    @Override
    public RawExpressionDataVector find( RawExpressionDataVector designElementDataVector ) {

        BusinessKey.checkKey( designElementDataVector );

        DetachedCriteria crit = DetachedCriteria.forClass( RawExpressionDataVector.class );

        crit.createCriteria( "designElement" )
                .add( Restrictions.eq( "name", designElementDataVector.getDesignElement().getName() ) )
                .createCriteria( "arrayDesign" ).add( Restrictions
                .eq( "name", designElementDataVector.getDesignElement().getArrayDesign().getName() ) );

        crit.createCriteria( "quantitationType" )
                .add( Restrictions.eq( "name", designElementDataVector.getQuantitationType().getName() ) );

        crit.createCriteria( "expressionExperiment" )
                .add( Restrictions.eq( "name", designElementDataVector.getExpressionExperiment().getName() ) );

        List<?> results = this.getHibernateTemplate().findByCriteria( crit );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + DesignElementDataVector.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( RawExpressionDataVector ) result;

    }

    @Override
    public void removeDataForCompositeSequence( final CompositeSequence compositeSequence ) {
        final String dedvRemovalQuery = "delete RawExpressionDataVectorImpl dedv where dedv.designElement = ?";
        int deleted = getHibernateTemplate().bulkUpdate( dedvRemovalQuery, compositeSequence );
        log.info( "Deleted: " + deleted );
    }

    @Override
    public void removeDataForQuantitationType( final QuantitationType quantitationType ) {
        final String dedvRemovalQuery = "delete from RawExpressionDataVectorImpl as dedv where dedv.quantitationType = ?";
        int deleted = getHibernateTemplate().bulkUpdate( dedvRemovalQuery, quantitationType );
        log.info( "Deleted " + deleted + " data vector elements" );
    }

}
