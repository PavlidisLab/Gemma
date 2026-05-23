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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.HashSet;

import static ubic.gemma.persistence.util.QueryUtils.optimizeIdentifiableParameterList;

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
        // bioAssayDimension/quantitationType are lazy=proxy in the hbm; join fetch both to
        // avoid one follow-up SELECT per vector. (quantitationType is pinned by the parameter
        // but join-fetch initializes the proxy so downstream consumers don't hit it lazily.)
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dev from RawExpressionDataVector dev "
                                + "join fetch dev.bioAssayDimension bd "
                                + "join fetch dev.quantitationType qt "
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

        // bioAssayDimension/quantitationType are lazy=proxy in the hbm; join fetch both so
        // downstream consumers don't fall back to per-row lazy initialization.
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dev from RawExpressionDataVector as dev "
                                // design elements + biological characteristics are already in the session
                                + "join fetch dev.bioAssayDimension "
                                + "join fetch dev.quantitationType "
                                + "where dev.designElement in (:des) and dev.quantitationType = :qt" )
                .setParameterList( "des", optimizeIdentifiableParameterList( designElements ) )
                .setParameter( "qt", quantitationType )
                .list();
    }

    @Override
    public RawExpressionDataVector find( RawExpressionDataVector designElementDataVector ) {

        BusinessKey.checkKey( designElementDataVector );

        return ( RawExpressionDataVector ) getSessionFactory().getCurrentSession()
                .createQuery( "select dev from RawExpressionDataVector dev "
                        + "join dev.designElement de "
                        + "join de.arrayDesign ad "
                        + "join dev.quantitationType qt "
                        + "join dev.expressionExperiment ee "
                        + "where de.name = :deName "
                        + "and ad.name = :adName "
                        + "and qt.name = :qtName "
                        + "and ee.name = :eeName" )
                .setParameter( "deName", designElementDataVector.getDesignElement().getName() )
                .setParameter( "adName", designElementDataVector.getDesignElement().getArrayDesign().getName() )
                .setParameter( "qtName", designElementDataVector.getQuantitationType().getName() )
                .setParameter( "eeName", designElementDataVector.getExpressionExperiment().getName() )
                .uniqueResult();
    }
}
