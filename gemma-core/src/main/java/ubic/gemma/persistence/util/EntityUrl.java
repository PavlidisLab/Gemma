package ubic.gemma.persistence.util;

import org.springframework.util.Assert;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Taxon;

import java.net.URI;

/**
 * Represents a URL for an {@link Identifiable} entity.
 * @author poirigui
 */
public class EntityUrl<T extends Identifiable> {

    public static EntityUrlChooser of( String baseUrl ) {
        return new EntityUrlChooser( baseUrl );
    }

    protected final String baseUrl;

    protected final T entity;

    private EntityUrl( String baseUrl, T entity ) {
        Assert.notNull( entity.getId(), "Entity must have an ID." );
        this.baseUrl = baseUrl;
        this.entity = entity;
    }

    public WebEntityUrl<T> web() {
        return new WebEntityUrl<T>( entity );
    }

    /**
     * Generate an URL for the REST API.
     */
    public RestEntityUrl rest() {
        return new RestEntityUrl();
    }

    public static class EntityUrlChooser {

        private final String baseUrl;

        public EntityUrlChooser( String baseUrl ) {
            this.baseUrl = baseUrl;
        }

        public ExpressionExperimentUrl entity( ExpressionExperiment entity ) {
            return new ExpressionExperimentUrl( baseUrl, entity );
        }

        public <T extends Identifiable> EntityUrl<T> entity( T entity ) {
            return new EntityUrl<>( baseUrl, entity );
        }
    }

    public static class ExpressionExperimentUrl extends EntityUrl<ExpressionExperiment> {

        private ExpressionExperimentUrl( String baseUrl, ExpressionExperiment entity ) {
            super( baseUrl, entity );
        }

        @Override
        public ExpressionExperimentWebUrl web() {
            return new ExpressionExperimentWebUrl( entity );
        }
    }

    /**
     * Generate a URL for Gemma Web.
     */
    public class WebEntityUrl<U extends Identifiable> {

        protected final U entity;
        private final String entityPath;

        private WebEntityUrl( U entity ) {
            this.entity = entity;
            if ( entity instanceof ExpressionExperiment ) {
                this.entityPath = "/expressionExperiment/showExpressionExperiment.html?id=";
            } else if ( entity instanceof ArrayDesign ) {
                this.entityPath = "/arrays/showArrayDesign.html?id=";
            } else if ( entity instanceof ExperimentalDesign ) {
                this.entityPath = "/experimentalDesign/showExperimentalDesign.html?id=";
            } else if ( entity instanceof ExpressionExperimentSubSet ) {
                this.entityPath = "/expressionExperiment/showExpressionExperimentSubSet.html?id=";
            } else if ( entity instanceof BioAssay ) {
                this.entityPath = "/bioAssay/showBioAssay.html?id=";
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

    public class ExpressionExperimentWebUrl extends WebEntityUrl<ExpressionExperiment> {

        private boolean edit = false;

        private ExpressionExperimentWebUrl( ExpressionExperiment entity ) {
            super( entity );
        }

        public ExperimentalDesignWebUrl design() {
            Assert.notNull( entity.getExperimentalDesign(), entity + " does not have an experimental design." );
            return new ExperimentalDesignWebUrl( entity, entity.getExperimentalDesign() );
        }

        public ExpressionExperimentWebUrl edit() {
            edit = true;
            return this;
        }

        @Override
        public URI toUri() {
            if ( edit ) {
                return URI.create( baseUrl + "/expressionExperiment/editExpressionExperiment.html?id=" + entity.getId() );
            } else {
                return super.toUri();
            }
        }
    }

    public class ExperimentalDesignWebUrl extends WebEntityUrl<ExperimentalDesign> {

        private final ExpressionExperiment experiment;

        private ExperimentalDesignWebUrl( ExpressionExperiment ee, ExperimentalDesign entity ) {
            super( entity );
            this.experiment = ee;
        }

        @Override
        public URI toUri() {
            return URI.create( baseUrl + "/experimentalDesign/showExperimentalDesign.html?eeid=" + experiment.getId() );
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
