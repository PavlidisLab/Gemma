package ubic.gemma.persistence.util;

import org.springframework.util.Assert;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.net.URI;

/**
 * Represents a URL for an {@link Identifiable} entity.
 * @author poirigui
 */
public class EntityUrl<T extends Identifiable> {

    public static <T extends Identifiable> EntityUrl<T> of( String baseUrl, T entity ) {
        Assert.notNull( entity.getId(), "Entity must have an ID." );
        return new EntityUrl<>( baseUrl, entity );
    }

    private final String baseUrl;

    private final T entity;

    private EntityUrl( String baseUrl, T entity ) {
        this.baseUrl = baseUrl;
        this.entity = entity;
    }

    public WebEntityUrl web() {
        return new WebEntityUrl();
    }

    /**
     * Generate an URL for the REST API.
     */
    public RestEntityUrl rest() {
        return new RestEntityUrl();
    }

    /**
     * Generate a URL for Gemma Web.
     */
    public class WebEntityUrl {

        private final String entityPath;

        private WebEntityUrl() {
            if ( entity instanceof ExpressionExperiment ) {
                this.entityPath = "/expressionExperiment/showExpressionExperiment.html?id=";
            } else if ( entity instanceof ArrayDesign ) {
                this.entityPath = "/arrays/showArrayDesign.html?id=";
            } else {
                throw new UnsupportedOperationException( "Cannot generate Web URL for entities of type " + entity.getClass() + "." );
            }
        }

        public URI toUri() {
            return URI.create( baseUrl + entityPath + entity.getId() );
        }

        public String toUriString() {
            return toUri().toString();
        }

        @Override
        public String toString() {
            return toUriString();
        }
    }

    /**
     * Generate a URL for Gemma REST.
     */
    public class RestEntityUrl {

        private final String entityPath;

        private RestEntityUrl() {
            if ( entity instanceof ExpressionExperiment ) {
                this.entityPath = "/datasets";
            } else if ( entity instanceof ArrayDesign ) {
                this.entityPath = "/platforms";
            } else if ( entity instanceof Taxon ) {
                this.entityPath = "/taxa";
            } else if ( entity instanceof ExpressionAnalysisResultSet ) {
                this.entityPath = "/resultSet";
            } else {
                throw new UnsupportedOperationException( "Cannot generate WEb URL for entities of type " + entity.getClass() + "." );
            }
        }

        public URI toUri() {
            return URI.create( baseUrl + "/rest/v2" + entityPath + "/" + entity.getId() );
        }

        public String toUriString() {
            return toUri().toString();
        }

        @Override
        public String toString() {
            return toUriString();
        }
    }

    public URI toUri() {
        return web().toUri();
    }

    public String toUriString() {
        return toUri().toString();
    }

    @Override
    public String toString() {
        return toUriString();
    }
}
