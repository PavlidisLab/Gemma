/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.search;

import ubic.gemma.core.ontology.basecode.model.OntologyResource;

import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a search result from an ontology.
 *
 * @author poirigui
 */
public class OntologySearchResult<T extends OntologyResource> implements Comparator<OntologySearchResult<?>> {

    private final T result;
    private final double score;

    public OntologySearchResult( T result, double score ) {
        this.result = result;
        this.score = score;
    }

    public T getResult() {
        return result;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int hashCode() {
        return Objects.hash( result );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof OntologySearchResult ) ) {
            return false;
        }
        return Objects.equals( result, ( ( OntologySearchResult<?> ) obj ).result );
    }

    @Override
    public int compare( OntologySearchResult<?> searchResult, OntologySearchResult<?> t1 ) {
        return Double.compare( searchResult.score, t1.score );
    }
}
