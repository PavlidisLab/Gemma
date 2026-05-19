/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.providers;

import ubic.gemma.core.ontology.basecode.model.OntologyIndividual;
import ubic.gemma.core.ontology.basecode.model.OntologyResource;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.basecode.search.OntologySearchException;
import ubic.gemma.core.ontology.basecode.search.OntologySearchResult;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

public interface OntologyService extends AutoCloseable {

    /**
     * Obtain the name of this ontology if available.
     */
    @Nullable
    String getName();

    /**
     * Obtain a description of this ontology if available.
     */
    @Nullable
    String getDescription();

    /**
     * Check if this ontology will process imports.
     * <p>
     * If disabled, ontologies imported by this ontology will not be loaded.
     */
    boolean getProcessImports();

    /**
     * Allow of forbid this ontology to process imports.
     * <p>
     * Changes are applicable only if the service is re-initialized.
     */
    void setProcessImports( boolean processImports );

    /**
     * List of URI prefixes to load from the ontology.
     * <p>
     * Changes are applicable only if the service is re-initialized.
     */
    void setAllowedUriPrefixes( String... uriPrefixes );

    /**
     * Clear the list of allowed URI prefixes.
     * <p>
     * Changes are applicable only if the service is re-initialized.
     */
    void clearAllowedUriPrefixes();

    enum LanguageLevel {
        /**
         * The full OWL language.
         */
        FULL,
        /**
         * OWL-DL
         */
        DL,
        /**
         * OWL/Lite
         */
        LITE
    }


    /**
     * Obtain the OWL language level supported by this ontology.
     * <p>
     * The default is to use {@link LanguageLevel#FULL}.
     */
    LanguageLevel getLanguageLevel();

    /**
     * Set the OWL language level supported by this ontology.
     * <p>
     * Changes are applicable only if the service is re-initialized.
     */
    void setLanguageLevel( LanguageLevel languageLevel );

    enum InferenceMode {
        /**
         * No inference is supported, only the axioms defined in the ontology are considered.
         */
        NONE,
        /**
         * Only basic inference is supported for {@code rdfs:subClassOf} and {@code rdfs:subPropertyOf}.
         * <p>
         * This is the fastest inference mode.
         */
        TRANSITIVE,
        /**
         * Very limited inference.
         */
        MICRO,
        /**
         * Limited inference.
         */
        MINI,
        /**
         * Complete inference.
         * <p>
         * This is the slowest inference mode.
         */
        FULL
    }

    /**
     * Obtain the inference mode used for this ontology.
     * <p>
     * The default is {@link InferenceMode#TRANSITIVE}.
     */
    InferenceMode getInferenceMode();

    /**
     * Set the inference mode used for this ontology.
     * <p>
     * Changes are applicable only if the service is re-initialized.
     */
    void setInferenceMode( InferenceMode inferenceMode );

    /**
     * Check if this ontology has full-text search enabled.
     * <p>
     * This is necessary for finding term using full-text queries. If enabled, an index will be generated in during
     * initialization by {@link #initialize(boolean, boolean)}.
     * <p>
     * Search is enabled by default.
     *
     * @see #findTerm(String, int, boolean)
     * @see #findIndividuals(String, int, boolean)
     * @see #findResources(String, int, boolean)
     */
    boolean isSearchEnabled();

    /**
     * Enable or disable search for this ontology.
     * <p>
     * Changes are only applicable if the service is re-initialized.
     */
    void setSearchEnabled( boolean searchEnabled );

    /**
     * Obtain the words that should be excluded from stemming.
     * <p>
     * By default, all words are subject to stemming. The exact implementation of stemming depends on the actual search
     * implementation.
     */
    Set<String> getExcludedWordsFromStemming();

    /**
     * Set words that should be excluded from stemming when searching.
     */
    void setExcludedWordsFromStemming( Set<String> excludedWordsFromStemming );

    /**
     * Obtain the URIs used as additional properties when inferring parents and children.
     * <p>
     * The default is to use <a href="http://purl.obolibrary.org/obo/BFO_0000050">part of</a>, <a href="http://www.obofoundry.org/ro/ro.owl#proper_part_of">proper part of</a>
     * and all of their sub-properties if inference is enabled.
     *
     * @see #getParents(Collection, boolean, boolean, boolean)
     * @see #getChildren(Collection, boolean, boolean, boolean)
     */
    Set<String> getAdditionalPropertyUris();

    /**
     * Set the URIs to be used as additional properties when inferring parents and children.
     * <p>
     * Changes are applicable only if the service is re-initialized.
     */
    void setAdditionalPropertyUris( Set<String> additionalPropertyUris );

    /**
     * Initialize this ontology service.
     *
     * @param forceLoad     Force loading of ontology, even if it is already loaded
     * @param forceIndexing If forceLoad is also true, indexing will be performed. If you know the index is up-to-date,
     *                      there's no need to do it again. Normally indexing is only done if there is no index, or if
     *                      the ontology has changed since last loaded.
     */
    void initialize( boolean forceLoad, boolean forceIndexing );

    /**
     * Initialize this ontology service from a stream.
     * <p>
     * Note that when this method of initialization is used, the ontology cache is not created on-disk.
     */
    void initialize( InputStream stream, boolean forceIndexing );

    /**
     * Looks for any individuals that match the given search string.
     * <p>
     * Obsolete terms are filtered out.
     */
    default Collection<OntologySearchResult<OntologyIndividual>> findIndividuals( String search, int maxResults ) throws OntologySearchException {
        return findIndividuals( search, maxResults, false );
    }

    /**
     * Looks for any individuals that match the given search string.
     *
     * @param search        search query
     * @param keepObsoletes retain obsolete terms
     */
    Collection<OntologySearchResult<OntologyIndividual>> findIndividuals( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException;

    /**
     * Looks for any resources (terms or individuals) that match the given search string
     * <p>
     * Obsolete terms are filtered out.
     *
     * @return results, or an empty collection if the results are empty OR the ontology is not available to be
     * searched.
     */
    default Collection<OntologySearchResult<OntologyResource>> findResources( String searchString, int maxResults ) throws OntologySearchException {
        return findResources( searchString, maxResults, false );
    }

    /**
     * Looks for any resources (terms or individuals) that match the given search string
     *
     * @param search        search query
     * @param keepObsoletes retain obsolete terms
     */
    Collection<OntologySearchResult<OntologyResource>> findResources( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException;

    /**
     * Looks for any terms that match the given search string.
     * <p>
     * Obsolete terms are filtered out.
     */
    default Collection<OntologySearchResult<OntologyTerm>> findTerm( String search, int maxResults ) throws OntologySearchException {
        return findTerm( search, maxResults, false );
    }


    /**
     * Looks for any terms that match the given search string.
     *
     * @param search        search query
     * @param keepObsoletes retain obsolete terms
     */
    Collection<OntologySearchResult<OntologyTerm>> findTerm( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException;

    /**
     * Find a term using an alternative ID.
     */
    @Nullable
    OntologyTerm findUsingAlternativeId( String alternativeId );

    /**
     * Obtain all the resource URIs in this ontology.
     */
    Set<String> getAllURIs();

    /**
     * Looks through both Terms and Individuals for a OntologyResource that has a uri matching the uri given. If no
     * OntologyTerm is found only then will ontologyIndividuals be searched. returns null if nothing is found.
     */
    @Nullable
    OntologyResource getResource( String uri );

    /**
     * Looks for a OntologyTerm that has the match in URI given
     */
    @Nullable
    OntologyTerm getTerm( String uri );

    /**
     * Obtain all the individuals for a given term URI.
     */
    Collection<OntologyIndividual> getTermIndividuals( String uri );

    /**
     * Obtain all the parents of a given set of terms, excluding obsolete terms.
     *
     * @see #getParents(Collection, boolean, boolean, boolean)
     */
    default Set<OntologyTerm> getParents( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return getParents( terms, direct, includeAdditionalProperties, false );
    }

    /**
     * Obtain all the parents of a given set of terms.
     *
     * @param terms                       set of terms whose parents are retrieved
     * @param direct                      only retain direct parents
     * @param includeAdditionalProperties also include parents matched via additional properties
     * @param keepObsoletes               retain obsolete terms
     * @return a set of parent terms
     */
    Set<OntologyTerm> getParents( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes );

    /**
     * Obtain all the children of a given set of terms, excluding obsolete terms.
     *
     * @see #getChildren(Collection, boolean, boolean, boolean)
     */
    default Set<OntologyTerm> getChildren( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return getChildren( terms, direct, includeAdditionalProperties, false );
    }

    /**
     * Obtain all the children of a given set of terms.
     *
     * @param terms                       set of terms whose children are retrieved
     * @param direct                      only retain direct children
     * @param includeAdditionalProperties also include children matched via additional properties
     * @param keepObsoletes               retain obsolete terms
     * @return a set of child terms
     */
    Set<OntologyTerm> getChildren( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes );

    /**
     * Check if this ontology is enabled.
     */
    boolean isEnabled();

    /**
     * Used for determining if the Ontology has finished loading into memory. Although calls like getParents,
     * getChildren will still work (its much faster once the ontologies have been preloaded into memory.)
     */
    boolean isOntologyLoaded();

    /**
     * Start the initialization thread.
     * <p>
     * If the initialization thread is already running, this method does nothing. If the initialization thread
     * previously completed, the ontology will be reinitialized.
     *
     * @param forceLoad     Force loading of ontology, even if it is already loaded
     * @param forceIndexing If forceLoad is also true, indexing will be performed. If you know the index is
     *                      up to date, there's no need to do it again. Normally indexing is only done if there is no
     *                      index, or if the ontology has changed since last loaded.
     */
    void startInitializationThread( boolean forceLoad, boolean forceIndexing );

    /**
     * Check if the initialization thread is alive.
     */
    boolean isInitializationThreadAlive();

    /**
     * Check if the initialization thread is cancelled.
     */
    boolean isInitializationThreadCancelled();

    /**
     * Cancel a running initialization thread.
     */
    void cancelInitializationThread();

    /**
     * Wait for the initialization thread to finish.
     */
    void waitForInitializationThread() throws InterruptedException;

    /**
     * Index the ontology for performing full-text searches.
     *
     * @param force if true, perform indexing even if an index already exists
     * @see #findIndividuals(String, int) (String)
     * @see #findTerm(String, int)
     * @see #findResources(String, int)
     */
    void index( boolean force );

    /**
     * For testing! Overrides normal way of loading the ontology. This does not index the ontology unless 'force' is
     * true (if there is an existing index, it will be used)
     *
     * @param is         input stream from which the ontology model is loaded
     * @param forceIndex initialize the index. Otherwise it will only be initialized if it doesn't exist.
     * @deprecated use {@link #initialize(InputStream, boolean)} instead and possibly {@link #cancelInitializationThread()}
     * prior to get any running initialization thread out of the way
     */
    @Deprecated
    void loadTermsInNameSpace( InputStream is, boolean forceIndex );
}