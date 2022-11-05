/*
 * The Gemma project.
 *
 * Copyright (c) 2009 University of British Columbia
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.*;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneSet</code>.
 *
 * @author kelsey
 * @see    GeneSet
 */
@Repository
public class GeneSetDaoImpl extends AbstractDao<GeneSet> implements GeneSetDao {

    @Autowired
    public GeneSetDaoImpl( SessionFactory sessionFactory ) {
        super( GeneSet.class, sessionFactory );
    }

    @Override
    public int getGeneCount( Long id ) {
        return ( Integer ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count(i) from GeneSet g join g.members i where g.id = :id" )
                .setParameter( "id", id ).uniqueResult();
    }

    @Override
    public Taxon getTaxon( Long id ) {
        // get one gene, check the taxon.
        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( "select g from GeneSet gs join gs.members m join m.gene g where gs.id = :id" )
                .setParameter( "id", id ).setMaxResults( 1 );

        Gene g = ( Gene ) q.uniqueResult();
        return g != null ? g.getTaxon() : null;
    }

    @Override
    public Collection<GeneSet> loadMyGeneSets() {
        return this.loadAll();
    }

    @Override
    public Collection<GeneSet> loadMyGeneSets( Taxon tax ) {
        return this.loadAll( tax );
    }

    @Override
    public Collection<GeneSet> loadMySharedGeneSets() {
        return this.loadAll();
    }

    @Override
    public Collection<GeneSet> loadMySharedGeneSets( Taxon tax ) {
        return this.loadAll( tax );
    }

    @Override
    public DatabaseBackedGeneSetValueObject loadValueObject( GeneSet geneSet ) {
        Object[] row = ( Object[] ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select g, t, count(m) from GeneSet g "
                                + "left join g.members m "
                                + "left join m.gene.taxon t "
                                + "where g = (:geneset) group by g.id" )
                .setParameter( "geneset", geneSet )
                .uniqueResult();
        return fillValueObject( ( GeneSet ) row[0], ( Taxon ) row[1], ( Long ) row[2] );
    }

    @Override
    public Collection<DatabaseBackedGeneSetValueObject> loadValueObjects( Collection<Long> ids ) {
        Collection<DatabaseBackedGeneSetValueObject> result = this.loadValueObjectsLite( ids );

        /*
         * Populate gene members - a bit inefficient
         * inner join is okay here, we only care about ones that have genes.
         */

        for ( GeneSetValueObject res : result ) {
            //noinspection unchecked
            res.setGeneIds( new HashSet<>( this.getSessionFactory().getCurrentSession()
                    .createQuery( "select genes.id from GeneSet g join g.members m join m.gene genes where g.id = :id" )
                    .setParameter( "id", res.getId() ).list() ) );
        }

        return result;
    }

    @Override
    public Collection<DatabaseBackedGeneSetValueObject> loadValueObjectsLite( Collection<Long> ids ) {
        Collection<DatabaseBackedGeneSetValueObject> result = new HashSet<>();

        if ( ids.isEmpty() )
            return result;

        // Left join: includes one that have no members. Caller has to filter them out if they need to.
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery(
                        "select g, t, count(m) from GeneSet g "
                                + "left join g.members m "
                                + "left join m.gene.taxon t "
                                + "where g.id in (:ids) group by g.id" )
                .setParameterList( "ids", ids )
                .list();

        for ( Object[] oa : list ) {
            result.add( this.fillValueObject( ( GeneSet ) oa[0], ( Taxon ) oa[1], ( Long ) oa[2] ) );
        }

        return result;
    }

    @Override
    public Collection<GeneSet> findByGene( Gene gene ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select gs from GeneSet gs inner join gs.members m inner join m.gene g where g = :g" )
                .setParameter( "g", gene ).list();
    }

    @Override
    public Collection<GeneSet> findByName( String name ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select gs from GeneSet gs where gs.name like :name order by gs.name" )
                .setParameter( "name", name + "%" ).list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        StopWatch timer = new StopWatch();
        timer.start();
        if ( StringUtils.isBlank( name ) )
            return new HashSet<>();
        assert taxon != null;
        // slow? would it be faster to just findByName and then restrict taxon?
        List<?> result = this.getSessionFactory().getCurrentSession().createQuery(
                        "select gs from GeneSet gs join gs.members gm join gm.gene g where g.taxon = :taxon and gs.name like :query order by gs.name" )
                .setParameter( "query", name + "%" ).setParameter( "taxon", taxon ).list();
        if ( timer.getTime() > 500 )
            AbstractDao.log
                    .info( "Find geneSets by name took " + timer.getTime() + "ms query=" + name + " taxon=" + taxon );
        //noinspection unchecked
        return ( Collection<GeneSet> ) result;
    }

    @Override
    public Collection<GeneSet> loadAll( Taxon tax ) {
        if ( tax == null )
            return this.loadAll();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct gs from GeneSet gs join gs.members m join m.gene g where g.taxon = :t" )
                .setParameter( "t", tax ).list();
    }

    @Override
    public GeneSet find( GeneSet entity ) {
        return this.findByName( entity.getName() ).iterator().next();
    }

    /**
     * Retrieve taxa for genesets
     *
     * @param  ids
     * @return
     */
    private Map<Long, Taxon> getTaxa( Collection<Long> ids ) {
        // fast
        //noinspection unchecked
        List<Object[]> q = this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct gs.id, t from GeneSet gs join gs.members m"
                                + " join m.gene g join g.taxon t where gs.id in (:ids) group by gs.id" )
                .setParameterList( "ids", ids ).list();

        Map<Long, Taxon> result = new HashMap<>();
        for ( Object[] o : q ) {
            //noinspection RedundantCast // Without casting we get suspicious call warning
            if ( result.containsKey( o[0] ) ) {
                throw new IllegalStateException( "More than one taxon in gene set id= " + o[0] );
            }

            result.put( ( Long ) o[0], ( Taxon ) o[1] );

        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.persistence.service.genome.gene.GeneSetDao#thaw(ubic.gemma.model.genome.gene.GeneSet)
     */
    @Override
    @Transactional(readOnly = true)
    public void thaw( final GeneSet geneSet ) {
        if ( geneSet == null || geneSet.getId() == null ) return;
        getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.NONE ).lock( geneSet );
        Hibernate.initialize( geneSet );
        Hibernate.initialize( geneSet.getMembers() );
        for ( GeneSetMember gsm : geneSet.getMembers() ) {
            Hibernate.initialize( gsm.getGene() );
        }

    }

    private DatabaseBackedGeneSetValueObject fillValueObject( GeneSet geneSet, Taxon taxon, Long membersCount ) {
        DatabaseBackedGeneSetValueObject dvo = new DatabaseBackedGeneSetValueObject();
        dvo.setSize( membersCount.intValue() );
        dvo.setId( geneSet.getId() );
        dvo.setName( geneSet.getName() );
        dvo.setDescription( geneSet.getDescription() );
        if ( taxon != null ) {
            dvo.setTaxon( new TaxonValueObject( taxon ) );
        } else {
            // NPE bug 60 - happens if we have leftover (empty) gene sets for taxa that were removed.
            log.warn( "No taxon found for gene set " + geneSet );
        }
        return dvo;
    }
}
