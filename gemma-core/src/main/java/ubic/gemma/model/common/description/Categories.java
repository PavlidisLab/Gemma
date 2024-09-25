package ubic.gemma.model.common.description;

import ubic.gemma.core.ontology.OntologyService;

/**
 * Enumeration of commonly used categories for referring to in the code.
 * <p>
 * Entries here have corresponding declarations in {@code EFO.factor.categories.txt} and are also available via
 * {@link OntologyService#getCategoryTerms()} in the form of ontology terms.
 */
public final class Categories {

    public static final Category TREATMENT = new Category( "treatment", "http://www.ebi.ac.uk/efo/EFO_0000727" );
    public static final Category CELL_TYPE = new Category( "cell type", "http://www.ebi.ac.uk/efo/EFO_0000324" );
}
