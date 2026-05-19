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

import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

public class AbstractDelegatingOntologyService implements OntologyService {

    private final OntologyService delegate;

    protected AbstractDelegatingOntologyService( OntologyService delegate ) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public boolean getProcessImports() {
        return delegate.getProcessImports();
    }

    @Override
    public void setProcessImports( boolean processImports ) {
        delegate.setProcessImports( processImports );
    }

    @Override
    public void setAllowedUriPrefixes( String... uriPrefixes ) {
        delegate.setAllowedUriPrefixes( uriPrefixes );
    }

    @Override
    public void clearAllowedUriPrefixes() {
        delegate.clearAllowedUriPrefixes();
    }

    @Override
    public LanguageLevel getLanguageLevel() {
        return delegate.getLanguageLevel();
    }

    @Override
    public void setLanguageLevel( LanguageLevel languageLevel ) {
        delegate.setLanguageLevel( languageLevel );
    }

    @Override
    public InferenceMode getInferenceMode() {
        return delegate.getInferenceMode();
    }

    @Override
    public void setInferenceMode( InferenceMode inferenceMode ) {
        delegate.setInferenceMode( inferenceMode );
    }

    @Override
    public boolean isSearchEnabled() {
        return delegate.isSearchEnabled();
    }

    @Override
    public void setSearchEnabled( boolean searchEnabled ) {
        delegate.setSearchEnabled( searchEnabled );
    }

    @Override
    public Set<String> getExcludedWordsFromStemming() {
        return delegate.getExcludedWordsFromStemming();
    }

    @Override
    public void setExcludedWordsFromStemming( Set<String> excludedWordsFromStemming ) {
        delegate.setExcludedWordsFromStemming( excludedWordsFromStemming );
    }

    @Override
    public Set<String> getAdditionalPropertyUris() {
        return delegate.getAdditionalPropertyUris();
    }

    @Override
    public void setAdditionalPropertyUris( Set<String> additionalPropertyUris ) {
        delegate.setAdditionalPropertyUris( additionalPropertyUris );
    }

    @Override
    public void initialize( boolean forceLoad, boolean forceIndexing ) {
        delegate.initialize( forceLoad, forceIndexing );
    }

    @Override
    public void initialize( InputStream stream, boolean forceIndexing ) {
        delegate.initialize( stream, forceIndexing );
    }

    @Override
    public Collection<OntologySearchResult<OntologyIndividual>> findIndividuals( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException {
        return delegate.findIndividuals( search, maxResults, keepObsoletes );
    }

    @Override
    public Collection<OntologySearchResult<OntologyResource>> findResources( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException {
        return delegate.findResources( search, maxResults, keepObsoletes );
    }

    @Override
    public Collection<OntologySearchResult<OntologyTerm>> findTerm( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException {
        return delegate.findTerm( search, maxResults, keepObsoletes );
    }

    @Override
    public OntologyTerm findUsingAlternativeId( String alternativeId ) {
        return delegate.findUsingAlternativeId( alternativeId );
    }

    @Override
    public Set<String> getAllURIs() {
        return delegate.getAllURIs();
    }

    @Override
    public OntologyResource getResource( String uri ) {
        return delegate.getResource( uri );
    }

    @Override
    public OntologyTerm getTerm( String uri ) {
        return delegate.getTerm( uri );
    }

    @Override
    public Collection<OntologyIndividual> getTermIndividuals( String uri ) {
        return delegate.getTermIndividuals( uri );
    }

    @Override
    public Set<OntologyTerm> getParents( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return delegate.getParents( terms, direct, includeAdditionalProperties );
    }

    @Override
    public Set<OntologyTerm> getChildren( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return delegate.getChildren( terms, direct, includeAdditionalProperties );
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public boolean isOntologyLoaded() {
        return delegate.isOntologyLoaded();
    }

    @Override
    public void startInitializationThread( boolean forceLoad, boolean forceIndexing ) {
        delegate.startInitializationThread( forceLoad, forceIndexing );
    }

    @Override
    public boolean isInitializationThreadAlive() {
        return delegate.isInitializationThreadAlive();
    }

    @Override
    public boolean isInitializationThreadCancelled() {
        return delegate.isInitializationThreadCancelled();
    }

    @Override
    public void cancelInitializationThread() {
        delegate.cancelInitializationThread();
    }

    @Override
    public void waitForInitializationThread() throws InterruptedException {
        delegate.waitForInitializationThread();
    }

    @Override
    public void index( boolean force ) {
        delegate.index( force );
    }

    @Override
    @Deprecated
    public void loadTermsInNameSpace( InputStream is, boolean forceIndex ) {
        delegate.loadTermsInNameSpace( is, forceIndex );
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
