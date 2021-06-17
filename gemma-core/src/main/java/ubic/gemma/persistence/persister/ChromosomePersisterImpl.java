package ubic.gemma.persistence.persister;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.genome.ChromosomeDao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChromosomePersisterImpl extends AbstractPersister<Chromosome> implements ChromosomePersister {

    private final Map<Object, Chromosome> seenChromosomes = new HashMap<>();

    @Autowired
    private ChromosomeDao chromosomeDao;

    @Autowired
    private Persister<BioSequence> bioSequencePersister;

    @Autowired
    private Persister<ExternalDatabase> externalDatabasePersister;

    @Autowired
    private Persister<Taxon> taxonPersister;

    @Autowired
    public ChromosomePersisterImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public Chromosome persist( Chromosome entity ) {
        return this.persistChromosome( entity, null );
    }

    @Override
    @Transactional
    public Chromosome persistChromosome( Chromosome chromosome, Taxon t ) {
        if ( chromosome == null )
            return null;
        if ( !this.isTransient( chromosome ) )
            return chromosome;

        Taxon ct = t;
        if ( ct == null ) {
            ct = chromosome.getTaxon();
        }

        // note that we can't use the native hashcode method because we need to ignore the ID.
        int key = chromosome.getName().hashCode();
        if ( ct.getNcbiId() != null ) {
            key += ct.getNcbiId().hashCode();
        } else if ( ct.getCommonName() != null ) {
            key += ct.getCommonName().hashCode();
        } else if ( ct.getScientificName() != null ) {
            key += ct.getScientificName().hashCode();
        }

        if ( seenChromosomes.containsKey( key ) ) {
            return seenChromosomes.get( key );
        }

        Collection<Chromosome> chroms = chromosomeDao.find( chromosome.getName(), ct );

        if ( chroms == null || chroms.isEmpty() ) {

            // no point in doing this if it already exists.
            try {
                FieldUtils.writeField( chromosome, "taxon", taxonPersister.persist( ct ), true );
                if ( chromosome.getSequence() != null ) {
                    // cascade should do?
                    FieldUtils.writeField( chromosome, "sequence", bioSequencePersister.persist( chromosome.getSequence() ), true );
                }
                if ( chromosome.getAssemblyDatabase() != null ) {
                    FieldUtils.writeField( chromosome, "assemblyDatabase",
                            externalDatabasePersister.persist( chromosome.getAssemblyDatabase() ), true );
                }
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
            chromosome = chromosomeDao.create( chromosome );
        } else if ( chroms.size() == 1 ) {
            chromosome = chroms.iterator().next();
        } else {
            throw new IllegalArgumentException( "Non-unique chromosome name  " + chromosome.getName() + " on " + ct );
        }

        seenChromosomes.put( key, chromosome );
        if ( chromosome == null || chromosome.getId() == null )
            throw new IllegalStateException( "Failed to get a persistent chromosome instance" );
        return chromosome;

    }

}
