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

import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.bioMaterialData.BioMaterialDimension
 */
public class BioMaterialDimensionDaoImpl extends edu.columbia.gemma.expression.bioAssayData.BioMaterialDimensionDaoBase {

    private static Log log = LogFactory.getLog( BioMaterialDimensionDaoImpl.class.getName() );

    @Override
    public BioMaterialDimension find( BioMaterialDimension bioMaterialDimension ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( BioMaterialDimension.class );

            if ( StringUtils.isNotBlank( bioMaterialDimension.getName() ) ) {
                queryObject.add( Restrictions.eq( "name", bioMaterialDimension.getName() ) );
            }

            if ( StringUtils.isNotBlank( bioMaterialDimension.getDescription() ) ) {
                queryObject.add( Restrictions.eq( "description", bioMaterialDimension.getDescription() ) );
            }

            queryObject.add( Restrictions.sizeEq( "bioMaterials", bioMaterialDimension.getBioMaterials().size() ) );

            // this will not work with detached bioassays.
            // queryObject.add( Restrictions.in( "bioMaterials", bioMaterialDimension.getBioMaterials() ) );

            // FIXME this isn't fail-safe, and also doesn't distinguish between dimensions that differ only in the
            // ordering.
            Collection<String> names = new HashSet<String>();
            for ( BioMaterial bioMaterial : bioMaterialDimension.getBioMaterials() ) {
                names.add( bioMaterial.getName() );
            }
            queryObject.createCriteria( "bioMaterials" ).add( Restrictions.in( "name", names ) );
            return ( BioMaterialDimension ) queryObject.uniqueResult();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public BioMaterialDimension findOrCreate( BioMaterialDimension bioMaterialDimension ) {
        if ( bioMaterialDimension == null || bioMaterialDimension.getBioMaterials() == null ) return null;
        BioMaterialDimension newBioMaterialDimension = find( bioMaterialDimension );
        if ( newBioMaterialDimension != null ) {
            BeanPropertyCompleter.complete( newBioMaterialDimension, bioMaterialDimension );
            return newBioMaterialDimension;
        }
        log.debug( "Creating new " + bioMaterialDimension );
        return ( BioMaterialDimension ) create( bioMaterialDimension );
    }
}