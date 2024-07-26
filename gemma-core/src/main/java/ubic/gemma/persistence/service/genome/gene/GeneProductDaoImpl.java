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
package ubic.gemma.persistence.service.genome.gene;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.*;

/**
 * @author pavlidis
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
@Repository
public class GeneProductDaoImpl extends AbstractVoEnabledDao<GeneProduct, GeneProductValueObject>
        implements GeneProductDao {

    private static final Comparator<GeneProduct> c;

    static {
        c = new Comparator<GeneProduct>() {
            @Override
            public int compare( GeneProduct arg0, GeneProduct arg1 ) {
                return arg0.getId().compareTo( arg1.getId() );
            }
        };
    }

    @Autowired
    public GeneProductDaoImpl( SessionFactory sessionFactory ) {
        super( GeneProduct.class, sessionFactory );
    }

    @Override
    public GeneProduct findByNcbiId( String ncbiId ) {
        return ( GeneProduct ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from GeneProduct g where g.ncbiGi = :ncbiId" ).setParameter( "ncbiId", ncbiId )
                .uniqueResult();
    }

    @Override
    public Collection<Gene> getGenesByName( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct gene from Gene as gene inner join gene.products gp where  gp.name = :search" )
                .setString( "search", search ).list();
    }

    @Override
    public Collection<Gene> getGenesByNcbiId( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct gene from Gene as gene inner join gene.products gp where gp.ncbiGi = :search" )
                .setString( "search", search ).list();
    }

    @Override
    public Collection<GeneProduct> findByName( String name, Taxon taxon ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct gp from GeneProduct gp left join fetch gp.gene g left join fetch g.taxon "
                                + "left join fetch gp.physicalLocation pl left join fetch gp.accessions"
                                + " left join fetch pl.chromosome ch left join fetch ch.taxon left join fetch g.aliases "
                                + "where gp.name = :name and g.taxon = :taxon" ).setParameter( "name", name )
                .setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public GeneProduct thaw( GeneProduct existing ) {
        return ( GeneProduct ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct gp from GeneProduct gp left join fetch gp.gene g left join fetch g.taxon "
                                + "left join fetch gp.physicalLocation pl left join fetch gp.accessions left join fetch pl.chromosome ch left join fetch ch.taxon "
                                + "left join fetch g.aliases  where gp = :gp" )
                .setParameter( "gp", existing )
                .uniqueResult();
    }

    @Override
    protected GeneProduct findByBusinessKey( GeneProduct geneProduct ) {
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( GeneProduct.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY );

        BusinessKey.checkValidKey( geneProduct );

        BusinessKey.createQueryObject( queryObject, geneProduct );

        //noinspection unchecked
        List<GeneProduct> results = queryObject.list();
        GeneProduct result = null;
        if ( results.size() > 1 ) {

            /*
             * At this point we can trust that the genes are from the same taxon. This kind of confusion should
             * reduce with cruft-reduction.
             */
            results.sort( GeneProductDaoImpl.c ); // we tend to want to keep the one with the lowest ID
            Gene gene = geneProduct.getGene();
            if ( gene != null ) {
                GeneProduct keeper = null;
                int numFound = 0;
                for ( Object object : results ) {
                    GeneProduct candidateMatch = ( GeneProduct ) object;

                    Gene candidateGene = candidateMatch.getGene();
                    if ( candidateGene.getOfficialSymbol().equals( gene.getOfficialSymbol() ) && candidateGene
                            .getTaxon().equals( gene.getTaxon() ) ) {
                        keeper = candidateMatch;
                        numFound++;
                    }
                }

                if ( numFound == 1 ) {
                    // not so bad, we figured out a match.
                    AbstractDao.log.warn( "Multiple gene products match " + geneProduct
                            + ", but only one for the right gene (" + gene + "), returning " + keeper );
                    this.debug( results );
                    return keeper;
                }

                if ( numFound == 0 ) {
                    AbstractDao.log
                            .error( "Multiple gene products match " + geneProduct + ", but none with " + gene );
                    this.debug( results );
                    AbstractDao.log.error( "Returning arbitrary match " + results.iterator().next() );
                    return results.iterator().next();
                }

                if ( numFound > 1 ) {
                    AbstractDao.log
                            .error( "Multiple gene products match " + geneProduct + ", and matches " + numFound
                                    + " genes" );
                    this.debug( results );
                    AbstractDao.log.error( "Returning arbitrary match " + results.iterator().next() );
                    return results.iterator().next();
                }
            }

        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        if ( result == null )
            return null;
        AbstractDao.log.debug( "Found: " + result );
        return result;
    }

    @Override
    protected GeneProductValueObject doLoadValueObject( GeneProduct entity ) {
        return new GeneProductValueObject( entity );
    }

    private void debug( Collection<?> results ) {

        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Object o : results ) {
            buf.append( o ).append( "\n" );
        }
        AbstractDao.log.error( buf );

    }
}