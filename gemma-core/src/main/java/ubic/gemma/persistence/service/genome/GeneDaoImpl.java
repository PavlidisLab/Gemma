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
package ubic.gemma.persistence.service.genome;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractQueryFilteringVoEnabledDao;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>Gene</code>.
 *
 * @see Gene
 */
@Repository
public class GeneDaoImpl extends AbstractQueryFilteringVoEnabledDao<Gene, GeneValueObject> implements GeneDao, InitializingBean {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RESULTS = 100;

    @Autowired
    public GeneDaoImpl( SessionFactory sessionFactory ) {
        super( GeneDao.OBJECT_ALIAS, Gene.class, sessionFactory );
    }

    @Override
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return this.findByPosition( physicalLocation.getChromosome(), physicalLocation.getNucleotide(),
                physicalLocation.getNucleotide() + physicalLocation.getNucleotideLength(),
                physicalLocation.getStrand() );
    }

    @Override
    public Gene findByAccession( String accession, @Nullable ExternalDatabase source ) {
        Collection<Gene> genes = new HashSet<>();
        final String accessionQuery = "select g from Gene g inner join g.accessions a where a.accession = :accession";
        final String externalDbQuery = accessionQuery + " and a.externalDatabase = :source";

        if ( source == null ) {
            //noinspection unchecked
            genes = this.getSessionFactory().getCurrentSession()
                    .createQuery( accessionQuery )
                    .setParameter( "accession", accession )
                    .list();
            if ( genes.isEmpty() ) {
                try {
                    return this.findByNcbiId( Integer.parseInt( accession ) );
                } catch ( NumberFormatException e ) {
                    // it's not an NCBIid
                }
            }
        } else {
            if ( source.getName().equalsIgnoreCase( "NCBI" ) ) {
                try {
                    return this.findByNcbiId( Integer.parseInt( accession ) );
                } catch ( NumberFormatException e ) {
                    // it's not an NCBIid
                }
            } else {
                //noinspection unchecked
                genes = this.getSessionFactory().getCurrentSession()
                        .createQuery( externalDbQuery )
                        .setParameter( "accession", accession )
                        .setParameter( "source", source )
                        .list();
            }
        }
        if ( genes.size() > 0 ) {
            return genes.iterator().next();
        }
        return null;

    }

    /**
     * Gets all the genes referred to by the alias defined by the search string.
     *
     * @return Collection
     */
    @Override
    public Collection<Gene> findByAlias( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct g from Gene as g inner join g.aliases als where als.alias = :search" )
                .setParameter( "search", search ).list();
    }

    @Override
    public Gene findByEnsemblId( String id ) {
        return ( Gene ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.ensemblId = :id" ).setParameter( "id", id ).uniqueResult();
    }

    @Override
    public Gene findByNcbiId( Integer ncbiId ) {
        return ( Gene ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.ncbiGeneId = :n" ).setParameter( "n", ncbiId ).uniqueResult();
    }

    @Override
    public Collection<Gene> findByOfficialSymbol( String officialSymbol ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.officialSymbol=:officialSymbol order by g.officialName" )
                .setParameter( "officialSymbol", officialSymbol ).list();
    }

    @Override
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return this.findByProperty( "officialName", officialName );
    }

    @Override
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.officialName like :officialName order by g.officialName" )
                .setParameter( "officialName", officialName ).setMaxResults( GeneDaoImpl.MAX_RESULTS ).list();
    }

    @Override
    public Gene findByOfficialSymbol( String symbol, Taxon taxon ) {
        return ( Gene ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct g from Gene as g where g.officialSymbol = :symbol and g.taxon = :taxon" )
                .setParameter( "symbol", symbol ).setParameter( "taxon", taxon ).uniqueResult();
    }

    @Override
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene g where g.officialSymbol like :officialSymbol order by g.officialSymbol" )
                .setParameter( "officialSymbol", officialSymbol ).setMaxResults( GeneDaoImpl.MAX_RESULTS ).list();
    }

    @Override
    public Map<String, Gene> findByOfficialSymbols( Collection<String> query, Long taxonId ) {
        Map<String, Gene> result = new HashMap<>();
        //language=HQL
        final String queryString = "select g from Gene as g join fetch g.taxon t where g.officialSymbol in (:symbols) and t.id = :taxonId";

        for ( Collection<String> batch : new BatchIterator<>( query, GeneDaoImpl.BATCH_SIZE ) ) {
            //noinspection unchecked
            List<Gene> results = this.getSessionFactory().getCurrentSession()
                    .createQuery( queryString )
                    .setParameterList( "symbols", batch )
                    .setParameter( "taxonId", taxonId )
                    .list();
            for ( Gene g : results ) {
                result.put( g.getOfficialSymbol().toLowerCase(), g );
            }
        }
        return result;
    }

    @Override
    public Map<Integer, Gene> findByNcbiIds( Collection<Integer> ncbiIds ) {
        Map<Integer, Gene> result = new HashMap<>();
        //language=HQL
        final String queryString = "from Gene g where g.ncbiGeneId in (:ncbi)";

        for ( Collection<Integer> batch : new BatchIterator<>( ncbiIds, GeneDaoImpl.BATCH_SIZE ) ) {
            //noinspection unchecked
            List<Gene> results = this.getSessionFactory().getCurrentSession()
                    .createQuery( queryString )
                    .setParameterList( "ncbi", batch )
                    .list();
            for ( Gene g : results ) {
                result.put( g.getNcbiGeneId(), g );
            }
        }
        return result;
    }

    @Override
    public Collection<Gene> findByPhysicalLocation( final PhysicalLocation location ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Gene as gene where gene.physicalLocation = :location" )
                .setParameter( "location", location ).list();
    }

    /**
     * Gets a count of the CompositeSequences related to the gene identified by the given id.
     *
     * @return Collection
     */
    @Override
    public long getCompositeSequenceCountById( long id ) {
        //language=HQL
        final String queryString =
                "select count(distinct cs) from Gene as gene inner join gene.products gp,  BioSequence2GeneProduct"
                        + " as bs2gp, CompositeSequence as cs where gp=bs2gp.geneProduct "
                        + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        return ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "id", id )
                .uniqueResult();
    }

    @Override
    public Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select distinct cs from Gene as gene inner join gene.products gp,  BioSequence2GeneProduct"
                        + " as bs2gp, CompositeSequence as cs where gp=bs2gp.geneProduct "
                        + " and cs.biologicalCharacteristic=bs2gp.bioSequence "
                        + " and gene = :gene and cs.arrayDesign = :arrayDesign ";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "arrayDesign", arrayDesign )
                .setParameter( "gene", gene )
                .list();
    }

    /**
     * Gets all the CompositeSequences related to the gene identified by the given id.
     *
     * @return Collection
     */
    @Override
    public Collection<CompositeSequence> getCompositeSequencesById( long id ) {
        //language=HQL
        final String queryString =
                "select distinct cs from Gene as gene  inner join gene.products as gp, BioSequence2GeneProduct "
                        + " as bs2gp , CompositeSequence as cs where gp=bs2gp.geneProduct "
                        + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "id", id )
                .list();
    }

    @Override
    public Collection<Gene> getGenesByTaxon( Taxon taxon ) {
        //language=HQL
        final String queryString = "select gene from Gene as gene where gene.taxon = :taxon ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public Collection<Gene> getMicroRnaByTaxon( Taxon taxon ) {
        //language=HQL
        final String queryString = "select gene from Gene as gene where gene.taxon = :taxon"
                + " and (gene.description like '%micro RNA or sno RNA' OR gene.description = 'miRNA')";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "taxon", taxon ).list();
    }

    @Override
    public int getPlatformCountById( Long id ) {
        //language=HQL
        final String queryString =
                "select count(distinct cs.arrayDesign) from Gene as gene inner join gene.products gp,  BioSequence2GeneProduct"
                        + " as bs2gp, CompositeSequence as cs where gp=bs2gp.geneProduct "
                        + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        return ( ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "id", id )
                .uniqueResult() ).intValue();
    }

    @Override
    public Collection<Gene> loadKnownGenes( Taxon taxon ) {
        //language=HQL
        final String queryString = "select gene from Gene as gene fetch all properties where gene.taxon = :taxon";

        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public List<Gene> loadThawed( Collection<Long> ids ) {
        List<Gene> result = new ArrayList<>( ids.size() );

        if ( ids.isEmpty() )
            return result;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> batch : new BatchIterator<>( ids, GeneDaoImpl.BATCH_SIZE ) ) {
            result.addAll( this.doLoadThawedLite( batch ) );
        }
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.debug( "Load+thawRawAndProcessed " + result.size() + " genes: " + timer.getTime() + "ms" );
        }
        return result;
    }

    @Override
    public Collection<Gene> loadThawedLiter( Collection<Long> ids ) {
        Collection<Gene> result = new HashSet<>();

        if ( ids.isEmpty() )
            return result;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> batch : new BatchIterator<>( ids, GeneDaoImpl.BATCH_SIZE ) ) {
            result.addAll( this.doLoadThawedLiter( batch ) );
        }
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.debug( "Load+thawRawAndProcessed " + result.size() + " genes: " + timer.getTime() + "ms" );
        }
        return result;
    }

    @Override
    public Gene thaw( final Gene gene ) {
        if ( gene.getId() == null )
            return gene;
        return ( Gene ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct g from Gene g " + " left join fetch g.aliases left join fetch g.accessions acc"
                                + " left join fetch acc.externalDatabase left join fetch g.products gp "
                                + " left join fetch gp.accessions gpacc left join fetch gpacc.externalDatabase left join"
                                + " fetch gp.physicalLocation gppl left join fetch gppl.chromosome chr left join fetch chr.taxon "
                                + " left join fetch g.taxon t left join fetch t.externalDatabase "
                                + " left join fetch g.multifunctionality left join fetch g.phenotypeAssociations "
                                + " where g.id=:gid" )
                .setParameter( "gid", gene.getId() )
                .uniqueResult();
    }

    /**
     * Only thaw the Aliases, very light version
     */
    @Override
    public Gene thawAliases( final Gene gene ) {
        if ( gene.getId() == null )
            return gene;
        return ( Gene ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct g from Gene g left join fetch g.aliases left join fetch g.accessions acc where g.id=:gid" )
                .setParameter( "gid", gene.getId() )
                .uniqueResult();
    }

    @Override
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        if ( genes.isEmpty() )
            return new HashSet<>();

        Collection<Gene> result = new HashSet<>();
        Collection<Gene> batch = new HashSet<>();

        for ( Gene g : genes ) {
            batch.add( g );
            if ( batch.size() == GeneDaoImpl.BATCH_SIZE ) {
                result.addAll( this.loadThawed( EntityUtils.getIds( batch ) ) );
                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {
            result.addAll( this.loadThawed( EntityUtils.getIds( batch ) ) );
        }

        return result;
    }

    @Override
    public Gene thawLite( final Gene gene ) {
        return this.thaw( gene );
    }

    @Override
    public Gene thawLiter( final Gene gene ) {
        if ( gene.getId() == null )
            return gene;

        return ( Gene ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct g from Gene g left join fetch g.taxon where g.id=:gid" )
                .setParameter( "gid", gene.getId() )
                .uniqueResult();
    }

    @Override
    public void remove( Gene gene ) {
        // remove associations
        this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from BioSequence2GeneProduct ba where ba.geneProduct in (select gp from GeneProduct gp where gp.gene = :g)" )
                .setParameter( "g", gene )
                .executeUpdate();
        this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from GeneSetMember gm where gm.gene = :g" )
                .setParameter( "g", gene )
                .executeUpdate();
        this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from Gene2GOAssociation g2g where g2g.gene = :g" )
                .setParameter( "g", gene )
                .executeUpdate();
        super.remove( gene );
    }

    @Override
    public Gene find( Gene gene ) {

        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( Gene.class );

        BusinessKey.checkKey( gene );

        BusinessKey.createQueryObject( queryObject, gene );

        //noinspection unchecked,unchecked
        List<Gene> results = queryObject.list();
        Gene result;

        if ( results.isEmpty() ) {

            return null;

        } else if ( results.size() > 1 ) {

            /*
             * As a side-effect, we remove relics. This is a bit ugly, but takes care of the problem! It was put in
             * place to help in the cleanup of duplicated genes. But this can happen fairly routinely when NCBI
             * information changes in messy ways.
             *
             * FIXME this can fail because 'find' methods are read-only; it will be okay if it is a nested call from a
             * read-write method.
             */
            Collection<Gene> toDelete = new HashSet<>();
            for ( Gene foundGene : results ) {
                if ( StringUtils.isBlank( foundGene.getPreviousNcbiId() ) )
                    continue;
                // Note hack we used to allow multiple previous ids.
                for ( String previousId : StringUtils.split( foundGene.getPreviousNcbiId(), "," ) ) {
                    try {
                        if ( gene.getNcbiGeneId().equals( Integer.parseInt( previousId ) ) ) {
                            toDelete.add( foundGene );
                        }
                    } catch ( NumberFormatException e ) {
                        // no action
                    }
                }
            }

            if ( !toDelete.isEmpty() ) {
                assert toDelete.size() < results.size(); // it shouldn't be everything!
                AbstractDao.log.warn(
                        "Deleting gene(s) that use a deprecated NCBI ID: " + StringUtils.join( toDelete, " | " ) );
                this.remove( toDelete ); // WARNING this might fail due to constraints.
            }
            results.removeAll( toDelete );

            for ( Gene foundGene : results ) {
                if ( foundGene.getNcbiGeneId() != null && gene.getNcbiGeneId() != null && foundGene.getNcbiGeneId()
                        .equals( gene.getNcbiGeneId() ) ) {
                    return foundGene;
                }
            }

            /*
             * This should be quite a rare situation if the database is kept tidy.
             */
            if ( results.size() > 1 ) {
                AbstractDao.log.error( "Multiple genes found for " + gene + ":" );
                this.debug( results );
                results.sort( Comparator.comparing( Describable::getId ) );
                result = results.iterator().next();
                AbstractDao.log.error( "Returning arbitrary gene: " + result );
            } else {
                result = results.get( 0 );
            }

        } else {
            result = results.get( 0 );
        }

        return result;
    }

    @Override
    protected GeneValueObject doLoadValueObject( Gene entity ) {
        Hibernate.initialize( entity.getMultifunctionality() );
        return new GeneValueObject( entity );
    }

    /**
     * @param filters         see {@link FilterQueryUtils#formRestrictionClause(Filters)} filters argument for
     *                        description.
     * @return a Hibernated Query object ready to be used for TaxonVO retrieval.
     */
    @Override
    protected Query getFilteringQuery( @Nullable Filters filters, @Nullable Sort sort ) {

        //noinspection JpaQlInspection // the constants for aliases is messing with the inspector
        String queryString = "select gene "
                + "from Gene as gene " // gene
                + "left join fetch gene.multifunctionality " // multifunctionality, if available
                + "left join fetch gene.taxon as taxon "// taxon
                + "where gene.id is not null"; // needed to use formRestrictionCause()

        queryString += FilterQueryUtils.formRestrictionClause( filters );
        queryString += FilterQueryUtils.formOrderByClause( sort );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        FilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    protected Query getFilteringCountQuery( @Nullable Filters filters ) {
        //noinspection JpaQlInspection // the constants for aliases is messing with the inspector
        String queryString = "select count(gene) from Gene as gene " // gene
                + "left join gene.multifunctionality " // multifunctionality, if available
                + "left join gene.taxon as taxon "// taxon
                + "where gene.id is not null"; // needed to use formRestrictionCause()

        queryString += FilterQueryUtils.formRestrictionClause( filters );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        FilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    protected void postProcessValueObjects( List<GeneValueObject> geneValueObjects ) {
        fillAliases( geneValueObjects );
        fillAccessions( geneValueObjects );
        fillMultifunctionalityRank( geneValueObjects );
    }

    private Collection<Gene> doLoadThawedLite( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select g from Gene g left join fetch g.aliases left join fetch g.accessions acc "
                        + "join fetch g.taxon t left join fetch g.products gp left join fetch g.multifunctionality "
                        + "where g.id in (:gIds)" ).setParameterList( "gIds", ids ).list();
    }

    private Collection<Gene> doLoadThawedLiter( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select g from Gene g join fetch g.taxon t where g.id in (:gIds)" )
                .setParameterList( "gIds", ids )
                .list();
    }

    /**
     * Returns genes in the region.
     */
    private Collection<Gene> findByPosition( Chromosome chromosome, final Long targetStart, final Long targetEnd,
            @Nullable final String strand ) {

        // the 'fetch'es are so we don't get lazy loads (typical applications of this method)
        //language=none // Prevents unresolvable missing value warnings.
        String query = "select distinct g from Gene as g "
                + "inner join fetch g.products prod  inner join fetch prod.physicalLocation pl inner join fetch pl.chromosome "
                + "where ((pl.nucleotide >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end) "
                + "OR (pl.nucleotide <= :start AND (pl.nucleotide + pl.nucleotideLength) >= :end) OR "
                + "(pl.nucleotide >= :start AND pl.nucleotide <= :end) "
                + "OR  ((pl.nucleotide + pl.nucleotideLength) >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end )) "
                + "and pl.chromosome = :chromosome and " + SequenceBinUtils.addBinToQuery( "pl", targetStart,
                targetEnd );

        if ( strand != null ) {
            //noinspection unchecked
            return getSessionFactory().getCurrentSession().createQuery( query + " and pl.strand = :strand" )
                    .setParameter( "chromosome", chromosome )
                    .setParameter( "start", targetStart )
                    .setParameter( "end", targetEnd )
                    .setParameter( "strand", strand )
                    .list();
        } else {
            //noinspection unchecked
            return getSessionFactory().getCurrentSession().createQuery( query )
                    .setParameter( "chromosome", chromosome )
                    .setParameter( "start", targetStart )
                    .setParameter( "end", targetEnd )
                    .list();
        }
    }

    private void debug( List<Gene> results ) {

        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Gene g : results ) {
            buf.append( g ).append( "\n" );
        }
        AbstractDao.log.error( buf );

    }

    private void fillAliases( List<GeneValueObject> geneValueObjects ) {
        if ( geneValueObjects.isEmpty() ) {
            return;
        }
        //noinspection unchecked
        List<Object[]> results = getSessionFactory().getCurrentSession()
                .createQuery( "select g.id, a.alias from Gene g join g.aliases a where g.id in :ids" )
                .setParameterList( "ids", geneValueObjects.stream().map( GeneValueObject::getId ).collect( Collectors.toSet() ) )
                .list();
        Map<Long, List<String>> aliasByGeneId = results.stream()
                .collect( Collectors.groupingBy(
                        row -> ( Long ) row[0],
                        Collectors.mapping( row -> ( String ) row[1], Collectors.toList() ) ) );
        for ( GeneValueObject gvo : geneValueObjects ) {
            List<String> aliases = aliasByGeneId.get( gvo.getId() );
            if ( aliases != null ) {
                gvo.setAliases( new TreeSet<>( aliases ) );
            } else {
                gvo.setAliases( Collections.emptySortedSet() );
            }
        }
    }

    private void fillAccessions( List<GeneValueObject> geneValueObjects ) {
        if ( geneValueObjects.isEmpty() ) {
            return;
        }
        //noinspection unchecked
        List<Object[]> results = getSessionFactory().getCurrentSession()
                .createQuery( "select g.id, a from Gene g join g.accessions a where g.id in :ids" )
                .setParameterList( "ids", geneValueObjects.stream().map( GeneValueObject::getId ).collect( Collectors.toSet() ) )
                .list();
        Map<Long, List<DatabaseEntry>> accessionsByGeneId = results.stream()
                .collect( Collectors.groupingBy(
                        row -> ( Long ) row[0],
                        Collectors.mapping( row -> ( DatabaseEntry ) row[1], Collectors.toList() ) ) );
        for ( GeneValueObject gvo : geneValueObjects ) {
            List<DatabaseEntry> accessions = accessionsByGeneId.get( gvo.getId() );
            if ( accessions != null ) {
                gvo.setAccessions( accessions.stream()
                        .map( DatabaseEntryValueObject::new )
                        .collect( Collectors.toSet() ) );
            } else {
                gvo.setAccessions( Collections.emptySet() );
            }
        }
    }

    /**
     * Fill multifuctionality ranks.
     * <p>
     * Usually, if genes are loaded via {@link #loadValueObject(Identifiable)}-family of functions, this is unnecessary
     * because of the {@code join fetch}, but in the search service, entities are retrieved via other methods that do
     * always retrieve multifunctionality scores eagerly.
     */
    private void fillMultifunctionalityRank( List<GeneValueObject> geneValueObjects ) {
        // only fill ranks that are null
        Set<Long> ids = geneValueObjects.stream()
                .filter( gvo -> gvo.getMultifunctionalityRank() == null )
                .map( GeneValueObject::getId )
                .collect( Collectors.toSet() );
        if ( ids.isEmpty() ) {
            return;
        }
        //noinspection unchecked
        List<Object[]> results = getSessionFactory().getCurrentSession()
                .createQuery( "select g.id, g.multifunctionality.rank from Gene g where g.id in :ids" )
                .setParameterList( "ids", ids )
                .list();
        Map<Long, Double> result = results.stream()
                .collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Double ) row[1] ) );
        for ( GeneValueObject gvo : geneValueObjects ) {
            Double rank = result.get( gvo.getId() );
            if ( rank != null ) {
                gvo.setMultifunctionalityRank( rank );
            }
        }
    }
}