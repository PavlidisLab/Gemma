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
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailDao;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

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
    public int getGeneCount( Long id ) {
        return ( Integer ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count(i) from GeneSet g join g.members i where g.id = :id" )
                .setParameter( "id", id ).uniqueResult();
    }

    @Override
    public Taxon getTaxon( GeneSet geneSet ) {
        // get one gene, check the taxon.
        return ( Taxon ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select g.taxon from GeneSet gs join gs.members m join m.gene g where gs = :gs" )
                .setParameter( "gs", geneSet )
                .setMaxResults( 1 )
                .uniqueResult();
    }

    @Override
    public List<Taxon> getTaxa( GeneSet geneSet ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select t from GeneSet gs join gs.members m join m.gene g join g.taxon t where gs = :gs group by t" )
                .setParameter( "gs", geneSet )
                .list();
    }

    @Override
    public DatabaseBackedGeneSetValueObject loadValueObject( GeneSet geneSet ) {
        return loadValueObjectById( geneSet.getId() );
    }

    @Override
    public DatabaseBackedGeneSetValueObject loadValueObjectById( Long id ) {
        DatabaseBackedGeneSetValueObject vo = loadValueObjectByIdLite( id );
        if ( vo != null ) {
            fillGeneIds( Collections.singletonList( vo ) );
        }
        return vo;
    }

    @Override
    public DatabaseBackedGeneSetValueObject loadValueObjectByIdLite( Long id ) {
        Object[] row = ( Object[] ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select g, t, count(m) from GeneSet g "
                                + "left join g.members m "
                                + "left join m.gene.taxon t "
                                + "where g.id = :id "
                                + "group by g.id" )
                .setParameter( "id", id )
                .uniqueResult();
        if ( row != null ) {
            return new DatabaseBackedGeneSetValueObject( ( GeneSet ) row[0], ( Taxon ) row[1], ( Long ) row[2] );
        } else {
            return null;
        }
    }

    @Override
    public List<DatabaseBackedGeneSetValueObject> loadValueObjects( Collection<GeneSet> entities ) {
        return loadValueObjectsByIds( entities.stream().map( GeneSet::getId ).collect( Collectors.toSet() ) );
    }

    @Override
    public List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        List<DatabaseBackedGeneSetValueObject> vos = loadValueObjectsByIdsLite( ids );
        fillGeneIds( vos );
        return vos;
    }

    @Override
    public List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIdsLite( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        List<Object[]> result = this.getSessionFactory().getCurrentSession().createQuery(
                        "select g, t, count(m) from GeneSet g "
                                + "left join g.members m "
                                + "left join m.gene.taxon t "
                                + "where g.id in :ids "
                                + "group by g.id" )
                .setParameterList( "ids", optimizeParameterList( ids ) )
                .list();
        return fillValueObjects( result );
    }

    @Override
    public List<DatabaseBackedGeneSetValueObject> loadAllValueObjects() {
        //noinspection unchecked
        List<Object[]> result = this.getSessionFactory().getCurrentSession().createQuery(
                        "select g, t, count(m) from GeneSet g "
                                + "left join g.members m "
                                + "left join m.gene.taxon t "
                                + "group by g.id" )
                .list();
        List<DatabaseBackedGeneSetValueObject> vos = fillValueObjects( result );
        fillGeneIds( vos );
        return vos;
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
        return findByName( name, null );
    }

    @Override
    public Collection<GeneSet> findByName( String name, @Nullable Taxon taxon ) {
        StopWatch timer = StopWatch.createStarted();
        if ( StringUtils.isBlank( name ) )
            return new HashSet<>();
        // slow? would it be faster to just findByNameAndVectorType and then restrict taxon?
        Query query = this.getSessionFactory().getCurrentSession().createQuery(
                        "select gs from GeneSet gs join gs.members gm join gm.gene g "
                                + "where gs.name like :query "
                                + ( taxon != null ? "and g.taxon = :taxon " : "" )
                                + "order by gs.name" )
                .setParameter( "query", name + "%" );
        if ( taxon != null ) {
            query.setParameter( "taxon", taxon );
        }
        //noinspection unchecked
        List<GeneSet> result = query.list();
        if ( timer.getTime() > 500 )
            log.info( "Find geneSets by name took " + timer.getTime() + "ms query=" + name + " taxon=" + taxon );
        return result;
    }

    @Override
    public Collection<GeneSet> loadAll( @Nullable Taxon tax ) {
        if ( tax == null )
            return this.loadAll();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select gs from GeneSet gs "
                        + "join gs.members m "
                        + "join m.gene g "
                        + "where g.taxon = :t "
                        + "group by gs" )
                .setParameter( "t", tax ).list();
    }

    @Override
    public GeneSet find( GeneSet entity ) {
        return this.findByName( entity.getName() ).iterator().next();
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.persistence.service.genome.gene.GeneSetDao#thaw(ubic.gemma.model.genome.gene.GeneSet)
     */
    @Override
    public void thaw( final GeneSet geneSet ) {
        Hibernate.initialize( geneSet );
        Hibernate.initialize( geneSet.getMembers() );
        for ( GeneSetMember gsm : geneSet.getMembers() ) {
            Hibernate.initialize( gsm.getGene() );
        }
    }

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Override
    public int removeAll() {
        //noinspection unchecked
        List<Long> atIds = getSessionFactory().getCurrentSession()
                .createQuery( "select at.id from GeneSet gs join gs.auditTrail at" )
                .list();
        int removedMembers = getSessionFactory().getCurrentSession()
                .createQuery( "delete from GeneSetMember gsm" )
                .executeUpdate();
        // FIXME: use HQL delete from Characteristic
        int removedCharacteristics = getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete from CHARACTERISTIC where GENE_SET_FK is not null" )
                .executeUpdate();
        //noinspection SqlWithoutWhere
        int removedLiteratureSourcesAssociations = getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete from GENE_SETS2LITERATURE_SOURCES" )
                .executeUpdate();
        int removedGeneSet = getSessionFactory().getCurrentSession().createQuery( "delete from GeneSet" )
                .executeUpdate();
        auditTrailDao.removeByIds( atIds );
        log.debug( String.format( "Removed all %d GeneSet. %d GeneSetMember, %d Characteristic and %d BibliographicReference associations were removed in cascade.",
                removedGeneSet, removedMembers, removedCharacteristics, removedLiteratureSourcesAssociations ) );
        return removedGeneSet;
    }

    @Override
    public final void update( Collection<GeneSet> entities ) {
        super.update( entities );
    }

    @Override
    public final void update( GeneSet entity ) {
        super.update( entity );
    }

    @Override
    public final void remove( Collection<GeneSet> entities ) {
        super.remove( entities );
    }

    @Override
    public final void remove( GeneSet entity ) {
        super.remove( entity );
    }

    private List<DatabaseBackedGeneSetValueObject> fillValueObjects( List<Object[]> result ) {
        return result.stream()
                .map( row -> new DatabaseBackedGeneSetValueObject( ( GeneSet ) row[0], ( Taxon ) row[1], ( Long ) row[2] ) )
                .collect( Collectors.toList() );
    }

    private void fillGeneIds( List<DatabaseBackedGeneSetValueObject> result ) {
        if ( result.isEmpty() ) {
            return;
        }
        Set<Long> ids = result.stream().map( DatabaseBackedGeneSetValueObject::getId ).collect( Collectors.toSet() );
        //noinspection unchecked
        List<Object[]> r = getSessionFactory().getCurrentSession()
                .createQuery( "select g.id, genes.id from GeneSet g join g.members m join m.gene genes where g.id in :ids" )
                .setParameterList( "ids", optimizeParameterList( ids ) )
                .list();
        Map<Long, Set<Long>> geneIdsByGeneSetId = r.stream()
                .collect( Collectors.groupingBy( row -> ( Long ) row[0], Collectors.mapping( row -> ( Long ) row[1], Collectors.toSet() ) ) );
        /*
         * Populate gene members - a bit inefficient
         * inner join is okay here, we only care about ones that have genes.
         */
        for ( GeneSetValueObject res : result ) {
            res.setGeneIds( geneIdsByGeneSetId.getOrDefault( res.getId(), Collections.emptySet() ) );
        }
    }
}
