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
package ubic.gemma.model.expression.biomaterial;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.biomaterial.BioMaterial
 */
public class BioMaterialDaoImpl extends ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase {

    private static Log log = LogFactory.getLog( BioMaterialDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase#find(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public BioMaterial find( BioMaterial bioMaterial ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( BioMaterial.class );

            if ( bioMaterial.getName() != null ) {
                queryObject.add( Restrictions.eq( "name", bioMaterial.getName() ) );
            }

            if ( bioMaterial.getExternalAccession() != null ) {
                queryObject.createCriteria( "externalAccession" ).add(
                        Restrictions.eq( "accession", bioMaterial.getExternalAccession().getAccession() ) );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + BioMaterial.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
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
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase#findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public BioMaterial findOrCreate( BioMaterial bioMaterial ) {
        if ( bioMaterial.getName() == null && bioMaterial.getExternalAccession() == null ) {
            throw new IllegalArgumentException( "BioMaterial must have a name or accession to use as comparison key" );
        }
        BioMaterial newBioMaterial = this.find( bioMaterial );
        if ( newBioMaterial != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing bioMaterial: " + newBioMaterial );
            return newBioMaterial;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new bioMaterial: " + bioMaterial.getName() );
        return ( BioMaterial ) create( bioMaterial );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from BioMaterialImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase#handleCopy(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected BioMaterial handleCopy( final BioMaterial bioMaterial ) throws Exception {

        BioMaterial newMaterial = ( BioMaterial ) this.getHibernateTemplate().execute(
                new org.springframework.orm.hibernate3.HibernateCallback() {

                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        session.evict( bioMaterial );
                        BioMaterial newMaterial = BioMaterial.Factory.newInstance();
                        newMaterial.setDescription( bioMaterial.getDescription() + " [Created by Gemma]" );
                        newMaterial.setMaterialType( bioMaterial.getMaterialType() );
                        newMaterial.setCharacteristics( bioMaterial.getCharacteristics() );
                        newMaterial.setSourceTaxon( bioMaterial.getSourceTaxon() );

                        newMaterial.setTreatments( bioMaterial.getTreatments() );
                        newMaterial.setFactorValues( bioMaterial.getFactorValues() );

                        newMaterial.setName( "Modeled after " + bioMaterial.getName() );
                        newMaterial = findOrCreate( newMaterial );
                        return newMaterial;
                    }
                }, true );

        return newMaterial;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        Collection<BioMaterial> bs = null;
        final String queryString = "select distinct b from BioMaterialImpl b where b.id in (:ids)";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            bs = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return bs;
    }
}