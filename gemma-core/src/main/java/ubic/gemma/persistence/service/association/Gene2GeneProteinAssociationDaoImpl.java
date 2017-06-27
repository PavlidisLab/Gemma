/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
package ubic.gemma.persistence.service.association;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.association.Gene2GeneProteinAssociationImpl;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Dao implementation for gene2geneproteinassociations.
 *
 * @author ldonnison
 */

@Repository
public class Gene2GeneProteinAssociationDaoImpl extends Gene2GeneProteinAssociationDaoBase {

    @Autowired
    public Gene2GeneProteinAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public Gene2GeneProteinAssociation create( final Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        Gene2GeneProteinAssociation old = this.find( gene2GeneProteinAssociation );
        if ( old != null ) {
            this.remove( old );
        }
        super.create( gene2GeneProteinAssociation );
        return gene2GeneProteinAssociation;
    }

    @Override
    public Gene2GeneProteinAssociation find( Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            Criteria queryObject = this.getSessionFactory().getCurrentSession()
                    .createCriteria( Gene2GeneProteinAssociation.class );
            // have to have gene 1 and gene 2 there
            BusinessKey.checkKey( gene2GeneProteinAssociation );

            BusinessKey.createQueryObject( queryObject, gene2GeneProteinAssociation );

            //noinspection unchecked
            java.util.List<Gene2GeneProteinAssociation> results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() == 1 ) {
                    result = results.iterator().next();
                } else if ( results.size() > 1 ) {
                    log.error( "Multiple interactions  found for " + gene2GeneProteinAssociation + ":" );

                    Collections.sort( results, new Comparator<Gene2GeneProteinAssociation>() {
                        @Override
                        public int compare( Gene2GeneProteinAssociation arg0, Gene2GeneProteinAssociation arg1 ) {
                            return arg0.getId().compareTo( arg1.getId() );
                        }
                    } );
                    result = results.iterator().next();
                    log.error( "Returning arbitrary gene2GeneProteinAssociation: " + result );
                }
            }
            return ( Gene2GeneProteinAssociation ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Collection<Gene2GeneProteinAssociation> findProteinInteractionsForGene( Gene gene ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "from Gene2GeneProteinAssociationImpl where :gene = firstGene.id or :gene = secondGene.id" )
                .setLong( "gene", gene.getId() ).list();
    }

    @Override
    public void thaw( Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        if ( gene2GeneProteinAssociation == null )
            return;
        if ( gene2GeneProteinAssociation.getId() == null )
            return;

        Session session = this.getSessionFactory().getCurrentSession();

        EntityUtils.attach( session, gene2GeneProteinAssociation, Gene2GeneProteinAssociationImpl.class,
                gene2GeneProteinAssociation.getId() );
        Hibernate.initialize( gene2GeneProteinAssociation );
        Hibernate.initialize( gene2GeneProteinAssociation.getFirstGene() );
        Hibernate.initialize( gene2GeneProteinAssociation.getSecondGene() );

        if ( gene2GeneProteinAssociation.getSecondGene().getTaxon() != null
                && gene2GeneProteinAssociation.getSecondGene().getTaxon().getId() != null ) {
            Hibernate.initialize( gene2GeneProteinAssociation.getSecondGene().getTaxon() );
            Hibernate.initialize( gene2GeneProteinAssociation.getFirstGene().getTaxon() );
        }

        session.evict( gene2GeneProteinAssociation );

    }

}