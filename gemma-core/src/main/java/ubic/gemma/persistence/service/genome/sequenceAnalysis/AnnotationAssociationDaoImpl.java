/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.sql.Connection;
import java.util.*;

/**
 * @author paul
 */
@Repository
public class AnnotationAssociationDaoImpl extends AbstractDao<AnnotationAssociation>
        implements AnnotationAssociationDao {

    @Autowired
    public AnnotationAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( AnnotationAssociation.class, sessionFactory );
    }

    @Override
    public Collection<AnnotationAssociation> find( BioSequence bioSequence ) {
        BusinessKey.checkValidKey( bioSequence );
        Criteria queryObject = this.getSessionFactory().getCurrentSession()
                .createCriteria( AnnotationAssociation.class );
        BusinessKey.attachCriteria( queryObject, bioSequence, "bioSequence" );

        //noinspection unchecked
        return queryObject.list();
    }

    @Override
    public Collection<AnnotationAssociation> find( Gene gene ) {
        if ( gene.getProducts().size() == 0 ) {
            throw new IllegalArgumentException( "Gene has no products" );
        }

        Collection<AnnotationAssociation> result = new HashSet<>();

        for ( GeneProduct geneProduct : gene.getProducts() ) {

            BusinessKey.checkValidKey( geneProduct );
            Criteria queryObject = this.getSessionFactory().getCurrentSession()
                    .createCriteria( AnnotationAssociation.class );
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
    public void thaw( final AnnotationAssociation annotationAssociation ) {
        if ( annotationAssociation == null )
            return;
        if ( annotationAssociation.getId() == null )
            return;
        HibernateTemplate template = this.getHibernateTemplate();
        template.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                AnnotationAssociationDaoImpl.this.thawAssociation( session, annotationAssociation );
                return null;
            }
        } );
    }

    @Override
    public void thaw( final Collection<AnnotationAssociation> anCollection ) {
        if ( anCollection == null )
            return;
        HibernateTemplate template = this.getHibernateTemplate();
        template.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( Object object : anCollection ) {
                    AnnotationAssociation blatAssociation = ( AnnotationAssociation ) object;
                    if ( blatAssociation.getId() == null )
                        continue;
                    AnnotationAssociationDaoImpl.this.thawAssociation( session, blatAssociation );
                }

                return null;
            }

        } );

    }

    @Override
    public Collection<AnnotationAssociation> find( Collection<GeneProduct> gps ) {
        if ( gps.isEmpty() )
            //noinspection unchecked
            return Collections.EMPTY_SET;
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select b from AnnotationAssociation b join b.geneProduct gp where gp in (:gps)" )
                .setParameterList( "gps", gps ).list();
    }

    private void thawAssociation( org.hibernate.Session session, AnnotationAssociation association ) {
        session.update( association );
        session.update( association.getBioSequence() );
        session.update( association.getGeneProduct() );
        session.update( association.getGeneProduct().getGene() );
        session.update( association.getGeneProduct().getGene().getPhysicalLocation() );
        Hibernate.initialize( association.getGeneProduct().getGene().getProducts() );
        session.update( association.getBioSequence() );
        //noinspection ResultOfMethodCallIgnored called so that the collection is initialised.
        association.getBioSequence().getSequenceDatabaseEntry();
    }

}
