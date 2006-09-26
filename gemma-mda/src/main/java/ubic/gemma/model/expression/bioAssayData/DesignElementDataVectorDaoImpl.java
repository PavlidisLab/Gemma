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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 * @author pavlidis
 * @version $Id$
 */
public class DesignElementDataVectorDaoImpl extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase {

    private static Log log = LogFactory.getLog( DesignElementDataVectorDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     */
    @Override
    public DesignElementDataVector find( DesignElementDataVector designElementDataVector ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( DesignElementDataVector.class );

            queryObject.createCriteria( "designElement" ).add(
                    Restrictions.eq( "name", designElementDataVector.getDesignElement().getName() ) ).createCriteria(
                    "arrayDesign" ).add(
                    Restrictions.eq( "name", designElementDataVector.getDesignElement().getArrayDesign().getName() ) );

            queryObject.createCriteria( "quantitationType" ).add(
                    Restrictions.eq( "name", designElementDataVector.getQuantitationType().getName() ) );

            queryObject.createCriteria( "expressionExperiment" ).add(
                    Restrictions.eq( "name", designElementDataVector.getExpressionExperiment().getName() ) );

            // FIXME - finish filling in criteria so we never use 'equals' on a domain object.

            java.util.List results = queryObject.list();
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
            return ( DesignElementDataVector ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public DesignElementDataVector findOrCreate( DesignElementDataVector designElementDataVector ) {
        if ( designElementDataVector == null || designElementDataVector.getDesignElement() == null
                || designElementDataVector.getExpressionExperiment() == null ) {
            throw new IllegalArgumentException( "DesignElementDataVector did not have complete business key "
                    + designElementDataVector );
        }
        DesignElementDataVector newDesignElementDataVector = find( designElementDataVector );
        if ( newDesignElementDataVector != null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found existing designElementDataVector: " + newDesignElementDataVector );
            return newDesignElementDataVector;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new designElementDataVector: " + designElementDataVector );
        return ( DesignElementDataVector ) create( designElementDataVector );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleQueryByGeneSymbolAndSpecies(java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected Collection handleQueryByGeneSymbolAndSpecies( String geneSymbol, String species ) throws Exception {
        final String queryString = "from DesignElementDataVectorImpl as d inner join d.designElement as de )";
        // + "inner join de.biologicalCharacteristic as bs inner join bs.bioSequence2geneProduct as b2g "
        // + "inner join b2g.geneProduct as gp inner join gp.gene as g "
        // + "inner join g.taxon as t where g.symbol='GRIN1' and t.commonName='mouse' "
        // + "and d.expressionExperiment.id in (1,4,6)";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            // queryObject.setParameter( "id", id );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                log.debug( "size: " + results.size() );
                for ( Object obj : results ) {
                    log.debug( obj );
                }
                // if ( results.size() > 1 ) {
                // throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                // "More than one instance of 'Integer" + "' was found when executing query --> '"
                // + queryString + "'" );
                // } else if ( results.size() == 1 ) {
                // result = results.iterator().next();
                // }
            }

            return ( Collection ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

}