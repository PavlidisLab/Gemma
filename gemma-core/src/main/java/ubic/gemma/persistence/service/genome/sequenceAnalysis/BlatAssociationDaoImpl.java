/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>BlatAssociation</code>.
 * </p>
 *
 * @see BlatAssociation
 */
@Repository
public class BlatAssociationDaoImpl extends AbstractDao<BlatAssociation> implements BlatAssociationDao {

    @Autowired
    public BlatAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( BlatAssociation.class, sessionFactory );
    }

    @Override
    public Collection<BlatAssociation> find( Collection<GeneProduct> gps ) {
        //noinspection unchecked
        return gps.isEmpty() ?
                Collections.emptySet() :
                this.getSession()
                        .createQuery( "select b from BlatAssociation b join b.geneProduct gp where gp in (:gps)" )
                        .setParameter( "gps", gps ).list();
    }

    @Override
    public Collection<BlatAssociation> find( BioSequence bioSequence ) {
        BusinessKey.checkValidKey( bioSequence );
        Criteria queryObject = super.getSession().createCriteria( BlatAssociation.class );
        BusinessKey.attachCriteria( queryObject, bioSequence, "bioSequence" );
        //noinspection unchecked
        return queryObject.list();
    }

    @Override
    public Collection<BlatAssociation> find( Gene gene ) {

        if ( gene.getProducts().size() == 0 ) {
            throw new IllegalArgumentException( "Gene has no products" );
        }

        Collection<BlatAssociation> result = new HashSet<BlatAssociation>();

        for ( GeneProduct geneProduct : gene.getProducts() ) {

            BusinessKey.checkValidKey( geneProduct );
            Criteria queryObject = this.getSession().createCriteria( BlatAssociation.class );
            Criteria innerQuery = queryObject.createCriteria( "geneProduct" );
            if ( StringUtils.isNotBlank( geneProduct.getNcbiGi() ) ) {
                innerQuery.add( Restrictions.eq( "ncbiGi", geneProduct.getNcbiGi() ) );
            }
            if ( StringUtils.isNotBlank( geneProduct.getName() ) ) {
                innerQuery.add( Restrictions.eq( "name", geneProduct.getName() ) );
            }

            //noinspection unchecked
            result.addAll( queryObject.list() );
        }

        return result;

    }

    @Override
    public void thaw( final BlatAssociation blatAssociation ) {
        if ( blatAssociation == null )
            return;
        if ( blatAssociation.getId() == null )
            return;
        HibernateTemplate template = this.getHibernateTemplate();
        template.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                thawBlatAssociation( session, blatAssociation );
                return null;
            }
        } );
    }

    @Override
    public void thaw( final Collection<BlatAssociation> blatAssociations ) {
        if ( blatAssociations == null )
            return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( Object object : blatAssociations ) {
                    BlatAssociation blatAssociation = ( BlatAssociation ) object;
                    if ( ( blatAssociation ).getId() == null )
                        continue;
                    thawBlatAssociation( session, blatAssociation );
                    session.evict( blatAssociation );
                }
                return null;
            }

        } );
    }

    private void thawBlatAssociation( org.hibernate.Session session, BlatAssociation blatAssociation ) {
        session.buildLockRequest( LockOptions.NONE ).lock( blatAssociation );
        Hibernate.initialize( blatAssociation.getBioSequence() );
        Hibernate.initialize( blatAssociation.getGeneProduct() );
        Hibernate.initialize( blatAssociation.getBlatResult() );
        Hibernate.initialize( blatAssociation.getBlatResult().getTargetChromosome() );
    }

}