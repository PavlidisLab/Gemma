/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.expression.biomaterial;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.biomaterial.BioMaterial
 */
public class BioMaterialDaoImpl extends edu.columbia.gemma.expression.biomaterial.BioMaterialDaoBase {

    private static Log log = LogFactory.getLog( BioMaterialDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.biomaterial.BioMaterialDaoBase#find(edu.columbia.gemma.expression.biomaterial.BioMaterial)
     */
    @Override
    public BioMaterial find( BioMaterial bioMaterial ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( BioMaterial.class );

            if ( bioMaterial.getName() != null ) {
                queryObject.add( Restrictions.eq( "name", bioMaterial.getName() ) );
            }

            /*
             * This syntax allows you to look at an association.
             */
            // if ( bioMaterial.getExternalAccession() != null ) {
            // queryObject.createCriteria( "externalAccession" ).add(
            // Restrictions.eq( "accession", bioMaterial.getExternalAccession().getAccession() ) );
            // }
            // but this is easier:
            if ( bioMaterial.getExternalAccession() != null ) {
                queryObject.add( Restrictions.eq( "externalAccession", bioMaterial.getExternalAccession() ) );
            }
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + BioMaterial.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( BioMaterial ) results.iterator().next();
                }
            }
            return ( BioMaterial ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.biomaterial.BioMaterialDaoBase#findOrCreate(edu.columbia.gemma.expression.biomaterial.BioMaterial)
     */
    @Override
    public BioMaterial findOrCreate( BioMaterial bioMaterial ) {
        if ( bioMaterial.getName() == null && bioMaterial.getExternalAccession() == null ) {
            log.debug( "BioMaterial must have a name or accession to use as comparison key" );
            return null;
        }
        BioMaterial newBioMaterial = this.find( bioMaterial );
        if ( newBioMaterial != null ) {
            log.debug( "Found existing bioMaterial: " + newBioMaterial );
            BeanPropertyCompleter.complete( newBioMaterial, bioMaterial );
            return newBioMaterial;
        }
        log.debug( "Creating new bioMaterial: " + bioMaterial.getName() );
        return ( BioMaterial ) create( bioMaterial );
    }

}