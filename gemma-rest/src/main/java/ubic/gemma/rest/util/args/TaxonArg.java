package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.util.MalformedArgException;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;

/**
 * Mutable argument type base class for Taxon API
 *
 * @author tesarst
 */
@Schema(oneOf = { TaxonIdArg.class, TaxonNcbiIdArg.class, TaxonNameArg.class })
public abstract class TaxonArg<T> extends AbstractEntityArg<T, Taxon, TaxonService> {

    /**
     * Minimum value to be considered an NCBI ID, lower values will be considered a regular gemma Taxon ID.
     */
    private static final Long MIN_NCBI_ID = 999L;

    protected TaxonArg( T arg ) {
        super( Taxon.class, arg );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request taxon argument
     * @return instance of appropriate implementation of TaxonArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static TaxonArg<?> valueOf( final String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "Taxon identifier cannot be null or empty.", null );
        }
        try {
            long id = Long.parseLong( s.trim() );
            return id > TaxonArg.MIN_NCBI_ID ? new TaxonNcbiIdArg( ( int ) id ) : new TaxonIdArg( id );
        } catch ( NumberFormatException e ) {
            return new TaxonNameArg( s );
        }
    }

    /**
     * Lists Genes overlapping a location on a specific chromosome on a taxon that this TaxonArg represents.
     *
     * @param taxonService      the service that will be used to retrieve the persistent Taxon object.
     * @param chromosomeService the service that will be used to find the Chromosome object.
     * @param geneService       the service that will be used to retrieve the Gene VOs
     * @param chromosomeName    name of the chromosome to look on
     * @param start             the start nucleotide denoting the location to look for genes at.
     * @param size              the size (in nucleotides) of the location from the 'start' nucleotide.
     * @return collection of Gene VOs overlapping the location defined by the 'start' and 'size' parameters.
     * @throws NotFoundException if the taxon cannot retrieved as per {@link #getEntity(TaxonService)}
     */
    public List<GeneValueObject> getGenesOnChromosome( TaxonService taxonService,
            ChromosomeService chromosomeService, GeneService geneService, String chromosomeName, long start,
            int size ) throws NotFoundException {
        // Taxon argument
        Taxon taxon = this.getEntity( taxonService );

        //Chromosome argument
        Collection<Chromosome> chromosomes = chromosomeService.find( chromosomeName, taxon );
        if ( chromosomes.isEmpty() ) {
            throw new NotFoundException( "Chromosome " + chromosomeName + " not found for taxon " + taxon.getScientificName() );
        }
        Chromosome chromosome = chromosomes.iterator().next();

        // Setup chromosome location
        PhysicalLocation region = PhysicalLocation.Factory.newInstance( chromosome );
        region.setNucleotide( start );
        region.setNucleotideLength( size );
        // region.setStrand( strand );

        List<GeneValueObject> GVOs = geneService.loadValueObjects( geneService.find( region ) );
        if ( GVOs == null ) {
            throw new NotFoundException(
                    "No genes found on chromosome " + chromosomeName + " between positions " + start + " and " + start
                            + size + "." );
        }
        return GVOs;
    }
}