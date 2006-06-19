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
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.util.BeanPropertyCompleter;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimension
 */
public class BioAssayDimensionDaoImpl extends ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDaoBase {

    private static Log log = LogFactory.getLog( BioAssayDimensionDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDaoBase#find(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    public BioAssayDimension find( BioAssayDimension bioAssayDimension ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( BioAssayDimension.class );

            if ( StringUtils.isNotBlank( bioAssayDimension.getName() ) ) {
                queryObject.add( Restrictions.eq( "name", bioAssayDimension.getName() ) );
            }

            if ( StringUtils.isNotBlank( bioAssayDimension.getDescription() ) ) {
                queryObject.add( Restrictions.eq( "description", bioAssayDimension.getDescription() ) );
            }

            queryObject.add( Restrictions.sizeEq( "dimensionBioAssays", bioAssayDimension.getBioAssays()
                    .size() ) );

            // this will not work with detached bioassays.
            // queryObject.add( Restrictions.in( "bioAssays", bioAssayDimension.getBioAssays() ) );

            // FIXME this isn't fail-safe, and also doesn't distinguish between dimensions that differ only in the
            // ordering.
            Collection<String> names = new HashSet<String>();
            for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
                names.add( bioAssay.getName() );
            }
            queryObject.createCriteria( "dimensionBioAssays" ).add( Restrictions.in( "name", names ) );
            return ( BioAssayDimension ) queryObject.uniqueResult();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDaoBase#findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    public BioAssayDimension findOrCreate( BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null || bioAssayDimension.getBioAssays() == null ) return null;
        BioAssayDimension newBioAssayDimension = find( bioAssayDimension );
        if ( newBioAssayDimension != null ) {
            BeanPropertyCompleter.complete( newBioAssayDimension, bioAssayDimension );
            return newBioAssayDimension;
        }
        log.debug( "Creating new " + bioAssayDimension );
        return ( BioAssayDimension ) create( bioAssayDimension );
    }
}