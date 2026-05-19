/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.simple;

import ubic.gemma.core.ontology.basecode.model.OntologyProperty;
import ubic.gemma.core.ontology.basecode.model.OntologyResource;
import ubic.gemma.core.ontology.basecode.model.OntologyStatement;

import java.util.Objects;

/**
 * @author poirigui
 */
public class OntologyStatementSimple implements OntologyStatement {

    private final OntologyResource subject;
    private final OntologyProperty predicate;
    private final OntologyResource object;

    public OntologyStatementSimple( OntologyResource subject, OntologyProperty predicate, OntologyResource object ) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    @Override
    public OntologyResource getSubject() {
        return subject;
    }

    @Override
    public OntologyProperty getPredicate() {
        return predicate;
    }

    @Override
    public OntologyResource getObject() {
        return object;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof OntologyStatement ) ) {
            return false;
        }
        return Objects.equals( subject, ( ( OntologyStatement ) obj ).getSubject() )
            && Objects.equals( predicate, ( ( OntologyStatement ) obj ).getPredicate() )
            && Objects.equals( object, ( ( OntologyStatement ) obj ).getObject() );
    }

    @Override
    public int hashCode() {
        return Objects.hash( subject, predicate, object );
    }
}
