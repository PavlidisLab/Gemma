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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.biomaterial.BioMaterial
 */
@Repository
public class BioMaterialDaoImpl extends ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase {

    private static Log log = LogFactory.getLog( BioMaterialDaoImpl.class.getName() );

    @Autowired
    public BioMaterialDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase#find(ubic.gemma.model.expression.biomaterial.BioMaterial
     * )
     */
    @Override
    public BioMaterial find( BioMaterial bioMaterial ) {
        log.info( "Start find" );
        Criteria queryObject = super.getSession().createCriteria( BioMaterial.class );

        BusinessKey.addRestrictions( queryObject, bioMaterial );

        java.util.List<?> results = queryObject.list();
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
        log.info( "Done with find" );
        return ( BioMaterial ) result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase#findOrCreate(ubic.gemma.model.expression.biomaterial
     * .BioMaterial)
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
        return create( bioMaterial );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#getExpressionExperiment(java.lang.Long)
     */
    public ExpressionExperiment getExpressionExperiment( Long bioMaterialId ) {
        List<?> result = getHibernateTemplate()
                .findByNamedParam(
                        "select distinct e from ExpressionExperimentImpl e inner join e.bioAssays ba inner join ba.samplesUsed bm where bm.id =:bmid ",
                        "bmid", bioMaterialId );

        if ( result.size() > 1 )
            throw new IllegalStateException( "MOre than one EE for biomaterial with id=" + bioMaterialId );

        if ( result.size() > 0 ) return ( ExpressionExperiment ) result.iterator().next();

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#removeFactor(java.util.Collection,
     * ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    public void removeFactor( Collection<BioMaterial> bioMaterials, ExperimentalFactor experimentalFactor ) {
        for ( BioMaterial bm : bioMaterials ) {
            boolean removed = false;
            for ( Iterator<FactorValue> fIt = bm.getFactorValues().iterator(); fIt.hasNext(); ) {
                if ( fIt.next().getExperimentalFactor().equals( experimentalFactor ) ) {
                    fIt.remove();
                    removed = true;
                }
            }
            if ( removed ) {
                this.update( bm );
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase#handleCopy(ubic.gemma.model.expression.biomaterial
     * .BioMaterial)
     */
    @Override
    protected BioMaterial handleCopy( final BioMaterial bioMaterial ) throws Exception {

        return ( BioMaterial ) this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {

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
                } );

    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from BioMaterialImpl";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDaoBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection<BioMaterial> handleLoad( Collection<Long> ids ) throws Exception {
        Collection<BioMaterial> bs = null;
        final String queryString = "select distinct b from BioMaterialImpl b where b.id in (:ids)";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            bs = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return bs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialDao#thaw(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public void thaw( final BioMaterial bioMaterial ) {
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {

            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {

                session.buildLockRequest( LockOptions.NONE ).lock( bioMaterial );
                Hibernate.initialize( bioMaterial );
                Hibernate.initialize( bioMaterial.getSourceTaxon() );
                Hibernate.initialize( bioMaterial.getBioAssaysUsedIn() );
                Hibernate.initialize( bioMaterial.getTreatments() );
                Hibernate.initialize( bioMaterial.getFactorValues() );
                return null;

            }
        } );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#thaw(java.util.Collection)
     */
    @Override
    public Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials ) {
        if ( bioMaterials.isEmpty() ) return bioMaterials;
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct b from BioMaterialImpl b left join fetch b.sourceTaxon left join fetch b.bioAssaysUsedIn"
                                + " left join fetch b.treatments left join fetch b.factorValues left join fetch b.auditTrail at "
                                + "left join fetch at.events where b.id in (:ids)", "ids",
                        EntityUtils.getIds( bioMaterials ) );
    }

    @Override
    public Collection<BioMaterial> findByFactorValue( FactorValue fv ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select distinct b from BioMaterialImpl b join b.factorValues fv where fv = :f", "f", fv );
    }

    @Override
    public Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment ) {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct bm from ExpressionExperimentImpl e join e.bioAssays b join b.samplesUsed bm where e = :ee",
                        "ee", experiment );
    }
}