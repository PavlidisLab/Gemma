package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.*;

@Service
public class TaxonArgService extends AbstractEntityArgService<Taxon, TaxonService> {

    private final ChromosomeService chromosomeService;
    private final GeneService geneService;

    @Autowired
    public TaxonArgService( TaxonService service, ChromosomeService chromosomeService, GeneService geneService ) {
        super( service );
        this.chromosomeService = chromosomeService;
        this.geneService = geneService;
    }

    @Override
    public <A> Filters getFilters( AbstractEntityArg<A, Taxon, TaxonService> entityArg ) throws BadRequestException {
        if ( entityArg instanceof TaxonNameArg ) {
            return Filters.by(
                    service.getFilter( "commonName", String.class, Filter.Operator.eq, ( String ) entityArg.getValue() ),
                    service.getFilter( "scientificName", String.class, Filter.Operator.eq, ( String ) entityArg.getValue() ) );
        } else {
            return super.getFilters( entityArg );
        }
    }

    @Override
    protected Map<String, List<String>> getArgsByPropertyName( AbstractEntityArrayArg<Taxon, TaxonService> entitiesArg ) {
        Map<String, List<String>> argsByPropertyName = new HashMap<>();
        for ( String v : entitiesArg.getValue() ) {
            AbstractEntityArg<?, Taxon, TaxonService> arg = entityArgValueOf( entitiesArg.getEntityArgClass(), v );
            if ( arg instanceof TaxonNameArg ) {
                argsByPropertyName.computeIfAbsent( "commonName", ( k ) -> new ArrayList<>() ).add( v );
                argsByPropertyName.computeIfAbsent( "scientificName", ( k ) -> new ArrayList<>() ).add( v );
            } else {
                argsByPropertyName.computeIfAbsent( arg.getPropertyName(), ( k ) -> new ArrayList<>() ).add( v );
            }
        }
        return argsByPropertyName;
    }

    /**
     * Lists Genes overlapping a location on a specific chromosome on a taxon that this TaxonArg represents.
     *
     * @param chromosomeName name of the chromosome to look on
     * @param strand         the strand that the gene has to have which is either '+' or '-', or null to ignore
     * @param start          the start nucleotide denoting the location to look for genes at.
     * @param size           the size (in nucleotides) of the location from the 'start' nucleotide.
     * @return collection of Gene VOs overlapping the location defined by the 'start' and 'size' parameters.
     * @throws NotFoundException if the taxon cannot retrieved
     */
    public List<GeneValueObject> getGenesOnChromosome( TaxonArg<?> arg, String chromosomeName, @Nullable String strand, long start, int size ) throws NotFoundException {
        // Taxon argument
        Taxon taxon = this.getEntity( arg );

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
        region.setStrand( strand );

        List<GeneValueObject> GVOs = geneService.loadValueObjects( geneService.find( region ) );
        if ( GVOs == null ) {
            throw new NotFoundException(
                    "No genes found on chromosome " + chromosomeName + " between positions " + start + " and " + start
                            + size + "." );
        }
        return GVOs;
    }
}