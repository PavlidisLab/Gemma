package ubic.gemma.core.ontology;

import lombok.Value;
import org.springframework.util.Assert;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.expression.experiment.StatementValueObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class FactorValueOntologyUtils {

    private static final String URI_PREFIX = "http://gemma.msl.ubc.ca/ont/TGFVO/";

    /**
     * Obtain a suitable ontology ID for a given factor value ID.
     */
    public static String getUri( Long factorValueId ) {
        return URI_PREFIX + factorValueId;
    }

    /**
     * Obtain a suitable ontology ID for a given factor value.
     */
    public static String getUri( FactorValue factorValue ) {
        Assert.notNull( factorValue.getId() );
        return getUri( factorValue.getId() );
    }

    /**
     * Extract a factor value ID from a factor value URI.
     */
    @Nullable
    public static Long parseUri( String uri ) {
        if ( !uri.startsWith( FactorValueOntologyUtils.URI_PREFIX ) ) {
            return null;
        }
        String t = uri.substring( FactorValueOntologyUtils.URI_PREFIX.length() );
        String[] pieces = t.split( "/", 2 );
        try {
            return Long.parseLong( pieces[0] );
        } catch ( NumberFormatException e ) {
            return null;
        }
    }

    /**
     * Check if a URI refers to an annotation of a factor value.
     */
    public static boolean isAnnotationUri( String uri ) {
        if ( !uri.startsWith( FactorValueOntologyUtils.URI_PREFIX ) ) {
            return false;
        }
        String t = uri.substring( FactorValueOntologyUtils.URI_PREFIX.length() );
        String[] pieces = t.split( "/", 2 );
        try {
            if ( pieces.length == 2 ) {
                Long.parseLong( pieces[0] );
                Long.parseLong( pieces[1] );
                return true;
            } else {
                return false;
            }
        } catch ( NumberFormatException e ) {
            return false;
        }
    }

    @FunctionalInterface
    public interface StatementVisitor<U, E extends Throwable> {
        void accept( StatementValueObject v, U u ) throws E;
    }

    /**
     * Visit the characteristics of a FactorValue and generate their annotation IDs.
     * <p>
     * Characteristics also include subject-only statements.
     */
    public static <E extends Throwable> void visitCharacteristics( Long factorValueId, Collection<StatementValueObject> statements, StatementVisitor<String, E> visitor ) throws E {
        long nextAvailableId = 1L;
        for ( StatementValueObject svo : new TreeSet<>( statements ) ) {
            visitor.accept( svo, getAnnotationId( factorValueId, nextAvailableId++ ) );
            if ( svo.getObject() != null ) {
                nextAvailableId++;
            }
            if ( svo.getSecondObject() != null ) {
                nextAvailableId++;
            }
        }
    }

    @Value
    public static class AnnotationIds {
        String subjectId;
        @Nullable
        String objectId;
        @Nullable
        String secondObjectId;
    }

    /**
     * Visit the statements of a FactorValue and generate their annotation IDs.
     */
    public static <E extends Throwable> void visitStatements( Long factorValueId, Collection<StatementValueObject> statements, StatementVisitor<AnnotationIds, E> visitor ) throws E {
        long nextAvailableId = 1L;
        for ( StatementValueObject svo : new TreeSet<>( statements ) ) {
            String subjectId = getAnnotationId( factorValueId, nextAvailableId++ );
            String objectId = null, secondObjectId = null;
            if ( svo.getObject() != null ) {
                objectId = getAnnotationId( factorValueId, nextAvailableId++ );
            }
            if ( svo.getSecondObject() != null ) {
                secondObjectId = getAnnotationId( factorValueId, nextAvailableId++ );
            }
            if ( objectId != null || secondObjectId != null ) {
                visitor.accept( svo, new AnnotationIds( subjectId, objectId, secondObjectId ) );
            }
        }
    }

    @Value
    public static class Annotation {
        String label;
        @Nullable
        String uri;
    }

    /**
     * Create a mapping of annotation IDs to annotations for a FactorValue.
     */
    public static Map<String, Annotation> getAnnotationsById( FactorValue fv ) {
        Assert.notNull( fv.getId() );
        Map<String, Annotation> result = new HashMap<>();
        long nextAvailableId = 1L;
        for ( Statement s : new TreeSet<>( fv.getCharacteristics() ) ) {
            result.put( getAnnotationId( fv.getId(), nextAvailableId++ ), new Annotation( s.getSubject(), s.getSubjectUri() ) );
            if ( s.getObject() != null ) {
                result.put( getAnnotationId( fv.getId(), nextAvailableId++ ), new Annotation( s.getObject(), s.getObjectUri() ) );
            }
            if ( s.getSecondObject() != null ) {
                result.put( getAnnotationId( fv.getId(), nextAvailableId++ ), new Annotation( s.getSecondObject(), s.getSecondObjectUri() ) );
            }
        }
        return result;
    }

    private static String getAnnotationId( Long factorValueId, Long id ) {
        return URI_PREFIX + factorValueId + "/" + id;
    }
}
