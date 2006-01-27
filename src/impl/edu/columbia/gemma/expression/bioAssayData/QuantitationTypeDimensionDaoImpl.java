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
package edu.columbia.gemma.expression.bioAssayData;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.quantitationTypeData.QuantitationTypeDimension
 */
public class QuantitationTypeDimensionDaoImpl extends
        edu.columbia.gemma.expression.bioAssayData.QuantitationTypeDimensionDaoBase {
    private static Log log = LogFactory.getLog( QuantitationTypeDimensionDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.quantitationTypeData.QuantitationTypeDimensionDaoBase#find(edu.columbia.gemma.expression.quantitationTypeData.QuantitationTypeDimension)
     */
    @SuppressWarnings("unchecked")
    @Override
    public QuantitationTypeDimension find( QuantitationTypeDimension quantitationTypeDimension ) {
        Criteria queryObject = super.getSession( false ).createCriteria( BioAssayDimension.class );
        if ( StringUtils.isNotBlank( quantitationTypeDimension.getName() ) ) {
            queryObject.add( Restrictions.ilike( "name", quantitationTypeDimension.getName() ) );
        }

        if ( StringUtils.isNotBlank( quantitationTypeDimension.getDescription() ) ) {
            queryObject.add( Restrictions.ilike( "description", quantitationTypeDimension.getDescription() ) );
        }

        queryObject.add( Restrictions.sizeEq( "dimensionQuantitationTypes", quantitationTypeDimension
                .getDimensionQuantitationTypes().size() ) );

        // this will not work with detached bioassays.
        // queryObject.add( Restrictions.in( "quantitationTypes", quantitationTypeDimension.getQuantitationTypes() ) );
        try {
            Collection<String> names = new HashSet<String>();
            for ( QuantitationType quantitationType : ( Collection<QuantitationType> ) quantitationTypeDimension
                    .getDimensionQuantitationTypes() ) {
                names.add( quantitationType.getName() );
            }
            queryObject.createCriteria( "dimensionQuantitationTypes" ).add( Restrictions.in( "name", names ) );
            return ( QuantitationTypeDimension ) queryObject.uniqueResult();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.quantitationTypeData.QuantitationTypeDimensionDaoBase#findOrCreate(edu.columbia.gemma.expression.quantitationTypeData.QuantitationTypeDimension)
     */
    @Override
    public QuantitationTypeDimension findOrCreate( QuantitationTypeDimension quantitationTypeDimension ) {
        if ( quantitationTypeDimension == null || quantitationTypeDimension.getDimensionQuantitationTypes() == null )
            return null;
        QuantitationTypeDimension newQuantitationTypeDimension = find( quantitationTypeDimension );
        if ( newQuantitationTypeDimension != null ) {
            BeanPropertyCompleter.complete( newQuantitationTypeDimension, quantitationTypeDimension );
            return newQuantitationTypeDimension;
        }
        log.debug( "Creating new " + quantitationTypeDimension );
        return ( QuantitationTypeDimension ) create( quantitationTypeDimension );
    }
}