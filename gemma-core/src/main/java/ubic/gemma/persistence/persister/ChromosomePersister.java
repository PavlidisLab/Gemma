package ubic.gemma.persistence.persister;

import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;

public interface ChromosomePersister extends Persister<Chromosome> {
    Chromosome persistChromosome( Chromosome chromosome, Taxon t );
}
