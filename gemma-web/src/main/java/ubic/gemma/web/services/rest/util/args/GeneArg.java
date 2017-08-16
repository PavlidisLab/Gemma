package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Mutable argument type base class for Gene API.
 *
 * @author tesarst
 */
public abstract class GeneArg<T> extends MutableArg<T, Gene, GeneService, GeneValueObject> {

    private static final String ERROR_MSG_DEFAULT = "The identifier was recognised to be an %s, but no Gene with this %s exists.";
    private static final String ERROR_MSG_TAXON = "Gene with given %s does not exist on this taxon";
    private static final String ENSEMBL_ID_REGEX = "(ENSTBE|MGP_BALBcJ_|MGP_PWKPhJ_|ENSMUS|MGP_129S1SvImJ_|"
            + "ENSSHA|ENSPFO|ENSRNO|FB|MGP_NODShiLtJ_|ENSLAF|ENSOAN|MGP_FVBNJ_|ENSDAR|ENSSSC|ENSGGO|ENSAMX|"
            + "ENSXMA|ENSCHO|ENSGAC|ENSDOR|MGP_CASTEiJ_|ENSGMO|ENSTSY|ENSAME|ENSLOC|MGP_LPJ_|ENSCPO|ENSPAN|"
            + "ENSTRU|ENSNLE|ENSPCA|ENSXET|ENSDNO|MGP_AJ_|MGP_DBA2J_|ENSMPU|ENSMOD|ENSVPA|ENS|ENSMMU|ENSOCU|"
            + "MGP_CBAJ_|MGP_NZOHlLtJ_|ENSSCE|ENSOPR|ENSACA|ENSCSA|ENSORL|ENSCSAV|ENSTNI|ENSECA|MGP_C3HHeJ_|"
            + "ENSCEL|ENSFAL|ENSPSI|ENSAPL|ENSCAF|MGP_SPRETEiJ_|ENSLAC|MGP_C57BL6NJ_|ENSSAR|ENSBTA|ENSMIC|"
            + "ENSEEU|ENSTTR|ENSOGA|ENSMLU|ENSSTO|ENSCIN|MGP_WSBEiJ_|ENSMEU|ENSPVA|ENSPMA|ENSPTR|ENSFCA|"
            + "ENSPPY|ENSMGA|ENSOAR|ENSCJA|ENSETE|ENSTGU|MGP_AKRJ_|ENSONI|ENSGAL).*";

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request Gene argument
     * @return instance of appropriate implementation of GeneArg based on the actual property the argument represents.
     */
    @SuppressWarnings("unused")
    public static GeneArg valueOf( final String s ) {
        try {
            return new GeneNcbiIdArg( Integer.parseInt( s.trim() ) );
        } catch ( NumberFormatException e ) {
            if ( s.matches( ENSEMBL_ID_REGEX ) ) {
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
    public Collection<GeneValueObject> getGenesOnTaxon( GeneService geneService, TaxonService taxonService,
            TaxonArg taxonArg ) {
        //noinspection unchecked
        Taxon taxon = ( Taxon ) taxonArg.getPersistentObject( taxonService );
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
    public Object getGeneEvidence( GeneService geneService,
            PhenotypeAssociationManagerService phenotypeAssociationManagerService, Taxon taxon ) {
        Gene gene = this.getPersistentObject( geneService );
        return phenotypeAssociationManagerService
                .findGenesWithEvidence( gene.getOfficialSymbol(), taxon == null ? null : taxon.getId() );
    }

    /**
     * @param service the service used to load the Gene value objects.
     * @return all genes that match the value of the GeneArg.
     */
    public abstract Collection<GeneValueObject> getValueObjects( GeneService service );

    /**
     * Returns all known locations of the gene(s) that this GeneArg represents.
     *
     * @param geneService service that will be used to retrieve the persistent Gene object.
     * @return collection of physical location objects.
     */
    public abstract Collection<PhysicalLocationValueObject> getGeneLocation( GeneService geneService );

    /**
     * Returns all known locations of the gene that this GeneArg represents.
     *
     * @param geneService service that will be used to retrieve the persistent Gene object.
     * @param taxon       the taxon to limit the search to. Can be null.
     * @return collection of physical location objects.
     */
    public abstract Collection<PhysicalLocationValueObject> getGeneLocation( GeneService geneService, Taxon taxon );

    /**
     * Returns GO terms for the gene that this GeneArg represents.
     *
     * @param geneService service that will be used to retrieve the persistent Gene object.
     * @return collection of physical location objects.
     */
    public Collection<GeneOntologyTermValueObject> getGoTerms( GeneService geneService,
            GeneOntologyService geneOntologyService ) {
        Gene gene = this.getPersistentObject( geneService );
        return geneOntologyService.getValueObjects( gene );
    }

    /**
     * @return the name of the identifier that the GeneArg represents.
     */
    abstract String getIdentifierName();

    /**
     * @return the default null cause error message.
     */
    String getDefaultError() {
        return String.format( ERROR_MSG_DEFAULT, getIdentifierName(), getIdentifierName() );
    }

    /**
     * @return the error message for when the null cause is gene not existing on a taxon.
     */
    String getTaxonError() {
        return String.format( ERROR_MSG_TAXON, getIdentifierName() );
    }

    /**
     * Lists Gene Value Objects of all genes that this GeneArg represents, discarding any genes that are not on the
     * given taxon.
     * @param service the service to use to retrieve the Gene Value Objects.
     * @param taxon the taxon to limit the genes search to.
     * @return collection of Gene Value Objects.
     */
    private Collection<GeneValueObject> getValueObjects( GeneService service, Taxon taxon ) {
        Collection<GeneValueObject> genes = this.getValueObjects( service );
        Collection<GeneValueObject> result = new ArrayList<>( genes.size() );
        for ( GeneValueObject vo : genes ) {
            if ( Objects.equals( vo.getTaxonId(), taxon.getId() ) ) {
                result.add( vo );
            }
        }
        if ( result.isEmpty() ) {
            this.nullCause = getTaxonError();
            return null;
        }
        return result;
    }
}
