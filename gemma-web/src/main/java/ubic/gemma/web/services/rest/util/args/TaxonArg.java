package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Mutable argument type base class for Taxon API
 *
 * @author tesarst
 */
public abstract class TaxonArg<T> extends MutableArg<T, Taxon, TaxonService, TaxonValueObject> {

    /**
     * Minimum value to be considered an NCBI ID, lower values will be considered a regular gemma Taxon ID.
     */
    private static final Long MIN_NCBI_ID = 999L;

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request taxon argument
     * @return instance of appropriate implementation of TaxonArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static TaxonArg valueOf( final String s ) {
        try {
            Long id = Long.parseLong( s.trim() );
            return id < MIN_NCBI_ID ? new TaxonIdArg( id ) : new TaxonNcbiIdArg( id );
        } catch ( NumberFormatException e ) {
            return new TaxonStringArg( s );
        }
    }

    /**
     * Lists datasets on the taxon that this TaxonArg represents.
     *
     * @param expressionExperimentService the service that will be used to retrieve the EEVOs.
     * @param taxonService                the service that will be used to retrieve the persistent Taxon object.
     * @param filters                     see ExpressionExperimentDaoImpl#loadValueObjectsPreFilter
     * @param offset                      see ExpressionExperimentDaoImpl#loadValueObjectsPreFilter
     * @param limit                       see ExpressionExperimentDaoImpl#loadValueObjectsPreFilter
     * @param sort                        see ExpressionExperimentDaoImpl#loadValueObjectsPreFilter
     * @param sortAsc                     see ExpressionExperimentDaoImpl#loadValueObjectsPreFilter
     * @return a collection of EEVOs matching the input parameters.
     */
    public Collection<ExpressionExperimentValueObject> getTaxonDatasets(
            ExpressionExperimentService expressionExperimentService, TaxonService taxonService,
            ArrayList<ObjectFilter[]> filters, int offset, int limit, String sort, boolean sortAsc ) {
        if ( filters == null ) {
            filters = new ArrayList<>( 1 );
        }
        filters.add( new ObjectFilter[] {
                new ObjectFilter( "id", this.getPersistentObject( taxonService ).getId(), ObjectFilter.is,
                        ObjectFilter.DAO_TAXON_ALIAS ) } );

        return expressionExperimentService.loadValueObjectsPreFilter( offset, limit, sort, sortAsc, filters );
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
     */
    public Collection<GeneValueObject> getGenesOnChromosome( TaxonService taxonService,
            ChromosomeService chromosomeService, GeneService geneService, String chromosomeName, long start,
            int size ) {
        // Taxon argument
        Taxon taxon = getPersistentObject( taxonService );

        //Chromosome argument
        Collection<Chromosome> chromosomes = chromosomeService.find( chromosomeName, taxon );
        if ( chromosomes.isEmpty() ) {
            WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                    "Chromosome " + chromosomeName + " not found for taxon " + taxon.getScientificName() );
            throw new GemmaApiException( errorBody );
        }
        Chromosome chromosome = chromosomes.iterator().next();

        // Setup chromosome location
        PhysicalLocation region = PhysicalLocation.Factory.newInstance( chromosome );
        region.setNucleotide( start );
        region.setNucleotideLength( size );
        // region.setStrand( strand );

        Collection<GeneValueObject> GVOs = geneService.loadValueObjects( geneService.find( region ) );
        if ( GVOs == null ) {
            WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                    "No genes found on chromosome " + chromosomeName + " between positions " + start + " and " + start
                            + size + "." );
            throw new GemmaApiException( errorBody );
        }
        return GVOs;
    }
}
