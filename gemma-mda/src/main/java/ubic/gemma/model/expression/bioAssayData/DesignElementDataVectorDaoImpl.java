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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.BusinessKey;

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
            BusinessKey.checkKey( designElementDataVector );

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

        DesignElementDataVector existing = find( designElementDataVector );
        if ( existing != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing designElementDataVector: " + existing );
            return existing;
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
    protected Collection handleQueryByGeneSymbolAndSpecies( String geneOfficialSymbol, String species,
            Collection expressionExperiments ) throws Exception {

        String expressionExperimentIds = parenthesis( expressionExperiments );
        final String queryString = "from DesignElementDataVectorImpl as d " // get DesignElementDataVectorImpl
                + "inner join d.designElement as de " // where de.name='probe_5'";
                + "inner join de.biologicalCharacteristic as bs " // where bs.name='test_bs'";
                + "inner join bs.bioSequence2GeneProduct as b2g "// where b2g.score=1.5";
                + "inner join b2g.geneProduct as gp inner join gp.gene as g "
                + "inner join g.taxon as t where g.officialSymbol = :geneOfficialSymbol and t.commonName = :species "
                + "and d.expressionExperiment.id in " + expressionExperimentIds;

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "geneOfficialSymbol", geneOfficialSymbol );
            queryObject.setParameter( "species", species );
            // queryObject.setParameter( "expressionExperiments", get ids of each expression experiment );
            java.util.List results = queryObject.list();

            if ( results != null ) {
                log.debug( "size: " + results.size() );
                for ( Object obj : results ) {
                    log.debug( obj );
                }
            }

            return ( Collection ) results;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * Places objects in the collection in a list surrounded by parenthesis and separated by commas. Useful for placing
     * this String representation of the list in a sql "in" statement. ie. "... in stringReturnedFromThisMethod" will
     * look like "... in (a,b,c)"
     * 
     * @param objects
     * @return String
     */
    private String parenthesis( Collection objects ) {
        // TODO refactor me into a utilities class and use reflection to determine
        // the parameters that should live in the parenthesis.
        String expressionExperimentIds = "";

        Object[] objs = objects.toArray();
        for ( int i = 0; i < objs.length; i++ ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) objs[i];

            if ( StringUtils.isEmpty( expressionExperimentIds ) ) {
                expressionExperimentIds = "(";
            } else {
                expressionExperimentIds = expressionExperimentIds + ee.getId().toString();
                expressionExperimentIds = expressionExperimentIds + ",";
            }
        }

        expressionExperimentIds = expressionExperimentIds + ( ( ExpressionExperiment ) objs[objs.length - 1] ).getId()
                + ")";

        return expressionExperimentIds;
    }
}