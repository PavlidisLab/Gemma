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
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.*;

/**
 * @author pavlidis
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
@Repository
public class GeneProductDaoImpl extends GeneProductDaoBase {

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
        super( sessionFactory );
    }

    @Override
    public GeneProduct find( GeneProduct geneProduct ) {
        try {
            Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( GeneProduct.class )
                    .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY );

            BusinessKey.checkValidKey( geneProduct );

            BusinessKey.createQueryObject( queryObject, geneProduct );

            log.debug( queryObject );

            List<GeneProduct> results = queryObject.list();
            Object result = null;
            if ( results.size() > 1 ) {

                /*
                 * At this point we can trust that the genes are from the same taxon. This kind of confusion should
                 * reduce with cruft-reduction.
                 */
                Collections.sort( results, c ); // we tend to want to keep the one with the lowest ID
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
                        } else if ( candidateMatch.getPhysicalLocation() != null
                                && geneProduct.getPhysicalLocation() != null && candidateMatch.getPhysicalLocation()
                                .nearlyEquals( geneProduct.getPhysicalLocation() ) ) {
                            keeper = candidateMatch;
                            numFound++;
                        }
                    }

                    if ( numFound == 1 ) {
                        // not so bad, we figured out a match.
                        log.warn( "Multiple gene products match " + geneProduct + ", but only one for the right gene ("
                                + gene + "), returning " + keeper );
                        debug( results );
                        return keeper;
                    }

                    if ( numFound == 0 ) {
                        log.error( "Multiple gene products match " + geneProduct + ", but none with " + gene );
                        debug( results );
                        log.error( "Returning arbitrary match " + results.iterator().next() );
                        return results.iterator().next();
                    }

                    if ( numFound > 1 ) {
                        log.error( "Multiple gene products match " + geneProduct + ", and matches " + numFound
                                + " genes" );
                        debug( results );
                        log.error( "Returning arbitrary match " + results.iterator().next() );
                        return results.iterator().next();
                    }
                }

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
            if ( result == null )
                return null;
            log.debug( "Found: " + result );
            return ( GeneProduct ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public GeneProduct findOrCreate( GeneProduct geneProduct ) {
        GeneProduct existingGeneProduct = this.find( geneProduct );
        if ( existingGeneProduct != null ) {
            return existingGeneProduct;
        }
        if ( log.isDebugEnabled() )
            log.debug( "Creating new geneProduct: " + geneProduct.getName() );
        return create( geneProduct );
    }

    @Override
    protected Collection<Gene> handleGetGenesByName( String search ) throws Exception {
        Collection<Gene> genes;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp where  gp.name = :search";
        try {
            org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }

    @Override
    protected Collection<Gene> handleGetGenesByNcbiId( String search ) throws Exception {
        Collection<Gene> genes;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp where gp.ncbiGi = :search";
        try {
            org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }

    private void debug( Collection<?> results ) {

        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Object o : results ) {
            buf.append( o ).append( "\n" );
        }
        log.error( buf );

    }

    @Override
    public void thaw( GeneProduct existing ) {

        Hibernate.initialize( existing.getGene() );
        Hibernate.initialize( existing.getGene().getTaxon() );
        Hibernate.initialize( existing.getPhysicalLocation() );
        Hibernate.initialize( existing.getPhysicalLocation().getChromosome() );
        Hibernate.initialize( existing.getPhysicalLocation().getChromosome().getTaxon() );
        Hibernate.initialize( existing.getAccessions() );

    }

    @Override
    public Collection<GeneProduct> findByName( String name, Taxon taxon ) {
        //noinspection unchecked
        return this.getSession().createQuery(
                "select distinct gp from GeneProductImpl gp left join fetch gp.gene g left join fetch g.taxon "
                        + "left join fetch gp.physicalLocation pl left join fetch gp.accessions"
                        + " left join fetch pl.chromosome ch left join fetch ch.taxon left join fetch g.aliases "
                        + "where gp.name = :name and g.taxon = :taxon" ).setParameter( "name", name )
                .setParameter( "taxon", taxon ).list();
    }

    @Override
    public GeneProductValueObject loadValueObject( GeneProduct entity ) {
        return new GeneProductValueObject( entity );
    }

    @Override
    public Collection<GeneProductValueObject> loadValueObjects( Collection<GeneProduct> entities ) {
        Collection<GeneProductValueObject> vos = new LinkedHashSet<>();
        for ( GeneProduct e : entities ) {
            vos.add( this.loadValueObject( e ) );
        }
        return vos;
    }
}