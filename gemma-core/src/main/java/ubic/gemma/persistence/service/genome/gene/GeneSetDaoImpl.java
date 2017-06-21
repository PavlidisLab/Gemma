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
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.*;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneSet</code>.
 *
 * @author kelsey
 * @see GeneSet
 */
@Repository
public class GeneSetDaoImpl extends AbstractDao<GeneSet> implements GeneSetDao {

    @Autowired
    public GeneSetDaoImpl( SessionFactory sessionFactory ) {
        super( GeneSet.class, sessionFactory );
    }

    @Override
    public Collection<GeneSet> findByGene( Gene gene ) {
        //noinspection unchecked
        return this.getSession()
                .createQuery( "select gs from GeneSet gs inner join gs.members m inner join m.gene g where g = :g" )
                .setParameter( "g", gene ).list();
    }

    @Override
    public Collection<GeneSet> findByName( String name ) {
        //noinspection unchecked
        return this.getSession()
                .createQuery( "select gs from GeneSet gs where gs.name like :name order by gs.name" )
                .setParameter( "name", name + "%" ).list();
    }

    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        StopWatch timer = new StopWatch();
        timer.start();
        if ( StringUtils.isBlank( name ) )
            return new HashSet<>();
        assert taxon != null;
        // slow? would it be faster to just findByName and then restrict taxon?
        List result = this.getSession().createQuery(
                "select gs from GeneSet gs join gs.members gm join gm.gene g where g.taxon = :taxon and gs.name like :query order by gs.name" )
                .setParameter( "query", name + "%" ).setParameter( "taxon", taxon ).list();
        if ( timer.getTime() > 500 )
            log.info( "Find geneSets by name took " + timer.getTime() + "ms query=" + name + " taxon=" + taxon );
        //noinspection unchecked
        return ( Collection<GeneSet> ) result;
    }

    @Override
    public Collection<GeneSet> loadAll( Taxon tax ) {
        if ( tax == null )
            return this.loadAll();
        //noinspection unchecked
        return this.getSession().createQuery(
                "select distinct gs from GeneSet gs join gs.members m join m.gene g where g.taxon = :t" )
                .setParameter( "t", tax ).list();
    }

    @Override
    public Collection<GeneSet> loadMyGeneSets() {
        return loadAll();
    }

    @Override
    public Collection<GeneSet> loadMyGeneSets( Taxon tax ) {
        return loadAll( tax );
    }

    @Override
    public Collection<GeneSet> loadMySharedGeneSets() {
        return loadAll();
    }

    @Override
    public Collection<GeneSet> loadMySharedGeneSets( Taxon tax ) {
        return loadAll( tax );
    }

    @Override
    public Collection<DatabaseBackedGeneSetValueObject> loadValueObjects( Collection<Long> ids ) {
        Collection<DatabaseBackedGeneSetValueObject> result = this.loadValueObjectsLite( ids );

        /*
         * Populate gene members - a bit inefficient
         * inner join is okay here, we only care about ones that have genes.
         */

        for ( GeneSetValueObject res : result ) {
            res.setGeneIds( new HashSet<Long>() );
            //noinspection unchecked
            res.getGeneIds().addAll( this.getSession().createQuery(
                    "select genes.id from GeneSet g join g.members m join m.gene genes where g.id = :id" )
                    .setParameter( "id", res.getId() ).list() );
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
        List<Object[]> list = this.getSession().createQuery(
                "select g.id, g.description, count(m), g.name from GeneSet g"
                        + " left join g.members m where g.id in (:ids) group by g.id" ).setParameterList( "ids", ids )
                .list();

        Map<Long, Taxon> taxa = getTaxa( ids );

        for ( Object[] oa : list ) {

            DatabaseBackedGeneSetValueObject dvo = new DatabaseBackedGeneSetValueObject();
            dvo.setDescription( ( String ) oa[1] );
            dvo.setId( ( Long ) oa[0] );
            dvo.setSize( ( ( Long ) oa[2] ).intValue() );
            dvo.setName( ( String ) oa[3] );

            Taxon t = taxa.get( dvo.getId() );
            dvo.setTaxonId( t.getId() );
            dvo.setTaxonName( t.getCommonName() );
            result.add( dvo );
        }

        return result;
    }

    @Override
    public int getGeneCount( Long id ) {
        return ( Integer ) this.getSession()
                .createQuery( "select count(i) from GeneSet g join g.members i where g.id = :id" )
                .setParameter( "id", id ).uniqueResult();
    }

    @Override
    public Taxon getTaxon( Long id ) {
        // get one gene, check the taxon.
        Query q = this.getSession()
                .createQuery( "select g from GeneSet gs join gs.members m join m.gene g where gs.id = :id" )
                .setParameter( "id", id ).setMaxResults( 1 );

        Gene g = ( Gene ) q.uniqueResult();
        return g.getTaxon();
    }

    private Map<Long, Taxon> getTaxa( Collection<Long> ids ) {
        // fast
        Query q = this.getSession().createQuery(
                "select distinct gs.id, t from GeneSet gs join gs.members m"
                        + " join m.gene g join g.taxon t where gs.id in (:ids) group by gs.id" )
                .setParameterList( "ids", ids );

        Map<Long, Taxon> result = new HashMap<>();
        for ( Object o : q.list() ) {
            Object[] oa = ( Object[] ) o;

            if ( result.containsKey( oa[0] ) ) {
                throw new IllegalStateException( "More than one taxon in gene set id= " + oa[0] );
            }

            result.put( ( Long ) oa[0], ( Taxon ) oa[1] );

        }

        return result;
    }

    @Override
    public void thaw( GeneSet entity ) {
    }

    @Override
    public GeneSet find( GeneSet entity ) {
        return this.findByName( entity.getName() ).iterator().next();
    }
}
