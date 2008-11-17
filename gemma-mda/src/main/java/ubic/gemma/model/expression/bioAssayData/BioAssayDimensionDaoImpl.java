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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimension
 */
public class BioAssayDimensionDaoImpl extends ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDaoBase {

    private static Log log = LogFactory.getLog( BioAssayDimensionDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDaoBase#find(ubic.gemma.model.expression.bioAssayData
     * .BioAssayDimension)
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

            queryObject.add( Restrictions.sizeEq( "bioAssays", bioAssayDimension.getBioAssays().size() ) );

            Collection<String> names = new HashSet<String>();
            assert bioAssayDimension.getBioAssays().size() > 0;
            for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
                names.add( bioAssay.getName() );
            }
            queryObject.createCriteria( "bioAssays" ).add( Restrictions.in( "name", names ) );
            
            BioAssayDimension candidate = ( BioAssayDimension ) queryObject.uniqueResult();

            if ( candidate == null ) return null;

            // Now check that the bioassays and order are exactly the same.
            Collection<BioAssay> desiredBioAssays = bioAssayDimension.getBioAssays();
            Collection<BioAssay> candidateBioAssays = candidate.getBioAssays();

            assert desiredBioAssays.size() == candidateBioAssays.size();

            Iterator<BioAssay> dit = desiredBioAssays.iterator();
            Iterator<BioAssay> cit = candidateBioAssays.iterator();

            while ( dit.hasNext() ) {
                BioAssay d = dit.next();
                BioAssay c = cit.next();
                if ( !c.equals( d ) ) return null;
            }

            return candidate;

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.model.expression.bioAssayData.BioAssayDimensionDaoBase#findOrCreate(ubic.gemma.model.expression.
     * bioAssayData.BioAssayDimension)
     */
    @Override
    public BioAssayDimension findOrCreate( BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null || bioAssayDimension.getBioAssays() == null )
            throw new IllegalArgumentException();
        BioAssayDimension existingBioAssayDimension = find( bioAssayDimension );
        if ( existingBioAssayDimension != null ) {
            return existingBioAssayDimension;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new " + bioAssayDimension );
        return create( bioAssayDimension );
    }
}