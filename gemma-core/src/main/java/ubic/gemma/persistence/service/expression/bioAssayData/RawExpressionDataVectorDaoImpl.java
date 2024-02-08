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
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author paul
 */
@Repository
public class RawExpressionDataVectorDaoImpl extends AbstractDesignElementDataVectorDao<RawExpressionDataVector>
        implements RawExpressionDataVectorDao {

    @Autowired
    public RawExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super( RawExpressionDataVector.class, sessionFactory );
    }

    @Override
    public Collection<RawExpressionDataVector> find( ArrayDesign arrayDesign, QuantitationType quantitationType ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dev from RawExpressionDataVector dev "
                                + "join fetch dev.bioAssayDimension bd "
                                + "join dev.designElement de "
                                + "where de.arrayDesign = :ad and dev.quantitationType = :quantitationType" )
                .setParameter( "quantitationType", quantitationType )
                .setParameter( "ad", arrayDesign )
                .list();
    }

    @Override
    public Collection<RawExpressionDataVector> find( Collection<CompositeSequence> designElements,
            QuantitationType quantitationType ) {
        if ( designElements == null || designElements.size() == 0 )
            return new HashSet<>();

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dev from RawExpressionDataVector as dev "
                                + "join dev.designElement as de "
                                // no need for the fetch jointures since the design elements and biological characteristics are already in the session
                                + "where de in (:des) and dev.quantitationType = :qt" )
                .setParameterList( "des", designElements )
                .setParameter( "qt", quantitationType )
                .list();
    }

    @Override
    public Collection<RawExpressionDataVector> findByExpressionExperiment( ExpressionExperiment ee, QuantitationType quantitationType ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select v from RawExpressionDataVector as v "
                                + "where v.expressionExperiment = :ee and v.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", quantitationType )
                .list();
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
