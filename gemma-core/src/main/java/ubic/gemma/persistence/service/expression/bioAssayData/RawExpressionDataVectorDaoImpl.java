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
import ubic.gemma.persistence.service.AbstractDao;
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
        ExpressionExperiment ee = ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession().load( ExpressionExperiment.class, eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Experiment with id=" + eeId + " not found" );
        }
        ee.getRawExpressionDataVectors().addAll( vectors );
        this.getSessionFactory().getCurrentSession().update( ee );
        return ee;
    }

    @Override
    public Collection<RawExpressionDataVector> find( BioAssayDimension bioAssayDimension ) {
        //noinspection unchecked
        return new HashSet<>( this.getSessionFactory().getCurrentSession()
                .createQuery( "select d from RawExpressionDataVector d where d.bioAssayDimension = :bad" )
                .setParameter( "bad", bioAssayDimension ).list() );
    }

    @Override
    public Collection<RawExpressionDataVector> find( Collection<QuantitationType> quantitationTypes ) {
        //language=HQL
        final String queryString = "select dev from RawExpressionDataVector dev where  "
                + "  dev.quantitationType in ( :quantitationTypes) ";
        //noinspection unchecked
        return new HashSet<>( this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "quantitationTypes", quantitationTypes ).list() );
    }

    @Override
    public Collection<RawExpressionDataVector> find( ArrayDesign arrayDesign, QuantitationType quantitationType ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select dev from RawExpressionDataVector dev  inner join fetch dev.bioAssayDimension bd "
                        + " inner join fetch dev.designElement de inner join fetch dev.quantitationType inner join de.arrayDesign ad where ad.id = :adid "
                        + "and dev.quantitationType = :quantitationType " )
                .setParameter( "quantitationType", quantitationType ).setParameter( "adid", arrayDesign.getId() )
                .list();

    }

    @Override
    public Collection<RawExpressionDataVector> find( Collection<CompositeSequence> designElements,
            QuantitationType quantitationType ) {
        if ( designElements == null || designElements.size() == 0 )
            return new HashSet<>();

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select dev from RawExpressionDataVector as dev inner join dev.designElement as de "
                        + " where de in (:des) and dev.quantitationType = :qt" )
                .setParameterList( "des", designElements ).setParameter( "qt", quantitationType ).list();
    }

    @Override
    public void removeDataForCompositeSequence( final CompositeSequence compositeSequence ) {
        final String dedvRemovalQuery = "delete RawExpressionDataVector dedv where dedv.designElement = ?";
        int deleted = this.getSessionFactory().getCurrentSession()
                .createQuery( dedvRemovalQuery )
                .setParameter( 0, compositeSequence )
                .executeUpdate();
        AbstractDao.log.info( "Deleted: " + deleted );
    }

    @Override
    public void removeDataForQuantitationType( final QuantitationType quantitationType ) {
        final String dedvRemovalQuery = "delete from RawExpressionDataVector as dedv where dedv.quantitationType = ?";
        int deleted = this.getSessionFactory().getCurrentSession()
                .createQuery( dedvRemovalQuery )
                .setParameter( 0, quantitationType )
                .executeUpdate();
        AbstractDao.log.info( "Deleted " + deleted + " data vector elements" );
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

        return ( RawExpressionDataVector ) crit.getExecutableCriteria( getSessionFactory().getCurrentSession() ).uniqueResult();
    }

}
