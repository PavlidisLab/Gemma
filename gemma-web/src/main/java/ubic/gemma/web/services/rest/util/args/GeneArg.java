package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mutable argument type base class for Gene API.
 *
 * @author tesarst
 */
public abstract class GeneArg<T> extends AbstractEntityArg<T, Gene, GeneService> {

    private static final String ERROR_MSG_TAXON = "Gene with given %s does not exist on this taxon";
    private static final String ENSEMBL_ID_REGEX = "(ENSTBE|MGP_BALBcJ_|MGP_PWKPhJ_|ENSMUS|MGP_129S1SvImJ_|"
            + "ENSSHA|ENSPFO|ENSRNO|FB|MGP_NODShiLtJ_|ENSLAF|ENSOAN|MGP_FVBNJ_|ENSDAR|ENSSSC|ENSGGO|ENSAMX|"
            + "ENSXMA|ENSCHO|ENSGAC|ENSDOR|MGP_CASTEiJ_|ENSGMO|ENSTSY|ENSAME|ENSLOC|MGP_LPJ_|ENSCPO|ENSPAN|"
            + "ENSTRU|ENSNLE|ENSPCA|ENSXET|ENSDNO|MGP_AJ_|MGP_DBA2J_|ENSMPU|ENSMOD|ENSVPA|ENS|ENSMMU|ENSOCU|"
            + "MGP_CBAJ_|MGP_NZOHlLtJ_|ENSSCE|ENSOPR|ENSACA|ENSCSA|ENSORL|ENSCSAV|ENSTNI|ENSECA|MGP_C3HHeJ_|"
            + "ENSCEL|ENSFAL|ENSPSI|ENSAPL|ENSCAF|MGP_SPRETEiJ_|ENSLAC|MGP_C57BL6NJ_|ENSSAR|ENSBTA|ENSMIC|"
            + "ENSEEU|ENSTTR|ENSOGA|ENSMLU|ENSSTO|ENSCIN|MGP_WSBEiJ_|ENSMEU|ENSPVA|ENSPMA|ENSPTR|ENSFCA|"
            + "ENSPPY|ENSMGA|ENSOAR|ENSCJA|ENSETE|ENSTGU|MGP_AKRJ_|ENSONI|ENSGAL).*";

    protected GeneArg( T value ) {
        super( Gene.class, value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request Gene argument
     * @return instance of appropriate implementation of GeneArg based on the actual property the argument represents.
     */
    @SuppressWarnings("unused")
    public static GeneArg<?> valueOf( final String s ) {
        try {
            return new GeneNcbiIdArg( Integer.parseInt( s.trim() ) );
        } catch ( NumberFormatException e ) {
            if ( s.matches( GeneArg.ENSEMBL_ID_REGEX ) ) {
                return new GeneEnsemblIdArg( s );
            } else {
                return new GeneSymbolArg( s );
            }
        }
    }

    /**
     * @param geneService service that will be used to retrieve the persistent Gene object.
     * @return a collection of Gene value objects..
     */
    public List<GeneValueObject> getGenesOnTaxon( GeneService geneService, TaxonService taxonService,
            TaxonArg<?> taxonArg ) {
        Taxon taxon = taxonArg.getEntity( taxonService );
        return this.getValueObjects( geneService, taxon );
    }

    /**
     * Searches for gene evidence of the gene that this GeneArg represents, on the given taxon.
     *
     * @param geneService                        service that will be used to retrieve the persistent Gene object.
     * @param phenotypeAssociationManagerService service used to execute the search.
     * @param taxon                              the taxon to limit the search to. Can be null.
     * @return collection of gene evidence VOs matching the criteria, or an error response, if there is an error.
     */
    public List<GeneEvidenceValueObject> getGeneEvidence( GeneService geneService,
            PhenotypeAssociationManagerService phenotypeAssociationManagerService, Taxon taxon ) throws SearchException {
        Gene gene = this.getEntity( geneService );
        return phenotypeAssociationManagerService
                .findGenesWithEvidence( gene.getOfficialSymbol(), taxon == null ? null : taxon.getId() );
    }

    /**
     * @param service the service used to load the Gene value objects.
     * @return all genes that match the value of the GeneArg.
     */
    public abstract List<GeneValueObject> getValueObjects( GeneService service );

    /**
     * Returns all known locations of the gene(s) that this GeneArg represents.
     *
     * @param geneService service that will be used to retrieve the persistent Gene object.
     * @return collection of physical location objects.
     */
    public abstract List<PhysicalLocationValueObject> getGeneLocation( GeneService geneService );

    /**
     * Returns all known locations of the gene that this GeneArg represents.
     *
     * @param geneService service that will be used to retrieve the persistent Gene object.
     * @param taxon       the taxon to limit the search to. Can be null.
     * @return collection of physical location objects.
     */
    public abstract List<PhysicalLocationValueObject> getGeneLocation( GeneService geneService, Taxon taxon );

    /**
     * Returns GO terms for the gene that this GeneArg represents.
     *
     * @param geneService service that will be used to retrieve the persistent Gene object.
     * @return collection of physical location objects.
     */
    public List<GeneOntologyTermValueObject> getGoTerms( GeneService geneService,
            GeneOntologyService geneOntologyService ) {
        Gene gene = this.getEntity( geneService );
        return geneOntologyService.getValueObjects( gene );
    }

    /**
     * @return the name of the identifier that the GeneArg represents.
     */
    abstract String getIdentifierName();

    /**
     * @return the error message for when the null cause is gene not existing on a taxon.
     */
    String getTaxonError() {
        return String.format( GeneArg.ERROR_MSG_TAXON, this.getIdentifierName() );
    }

    /**
     * Lists Gene Value Objects of all genes that this GeneArg represents, discarding any genes that are not on the
     * given taxon.
     *
     * @param service the service to use to retrieve the Gene Value Objects.
     * @param taxon   the taxon to limit the genes search to.
     * @return collection of Gene Value Objects.
     */
    private List<GeneValueObject> getValueObjects( GeneService service, Taxon taxon ) {
        return this.getValueObjects( service ).stream()
                .filter( vo -> Objects.equals( vo.getTaxonId(), taxon.getId() ) )
                .collect( Collectors.toList() );
    }
}
