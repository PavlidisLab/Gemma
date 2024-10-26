package ubic.gemma.persistence.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Taxon;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * This builder allows for generating URLs for entities in Gemma Web and REST.
 * @author poirigui
 */
@Component
public class EntityUrlBuilder {

    private final String hostUrl;

    @Autowired
    public EntityUrlBuilder( @Value("${gemma.hosturl}") String hostUrl ) {
        Assert.isTrue( !hostUrl.endsWith( "/" ), "The context path must not end with '/'." );
        this.hostUrl = hostUrl;
    }

    /**
     * Obtain an {@link EntityUrlChooser} for generating a URL relative to the host URL.
     * <p>
     * Use this for absolute URLs.
     */
    public EntityUrlChooser fromHostUrl() {
        return new EntityUrlChooser( hostUrl );
    }

    /**
     * Obtain an {@link EntityUrlChooser} for generating a URL relative to the context path.
     * <p>
     * Use this for relative URLs.
     */
    public EntityUrlChooser fromContextPath( String contextPath ) {
        Assert.isTrue( !contextPath.endsWith( "/" ), "The context path must not end with '/'." );
        return new EntityUrlChooser( contextPath );
    }

    /**
     * Allows for choosing a specific entity.
     */
    public static class EntityUrlChooser {

        private final String baseUrl;

        public EntityUrlChooser( String baseUrl ) {
            this.baseUrl = baseUrl;
        }

        public <T extends Identifiable> AllEntitiesUrl<T> all( Class<T> entityType ) {
            return new AllEntitiesUrl<>( baseUrl, entityType );
        }

        public ExpressionExperimentUrl entity( ExpressionExperiment entity ) {
            return new ExpressionExperimentUrl( baseUrl, entity );
        }

        public ArrayDesignUrl entity( ArrayDesign entity ) {
            return new ArrayDesignUrl( baseUrl, entity );
        }

        public <T extends Identifiable> EntityUrl<T> entity( T entity ) {
            return new EntityUrl<>( baseUrl, entity );
        }
    }

    /**
     * Represents a URL for an {@link Identifiable} entity.
     */
    public static class EntityUrl<T extends Identifiable> {

        protected final String baseUrl;
        protected final T entity;

        private EntityUrl( String baseUrl, T entity ) {
            Assert.notNull( entity.getId(), "Entity must have an ID." );
            this.baseUrl = baseUrl;
            this.entity = entity;
        }

        /**
         * Generate a URL for Gemma Web.
         */
        public WebEntityUrl<T> web() {
            return new WebEntityUrl<>( baseUrl, entity );
        }

        /**
         * Generate a URL for the REST API.
         */
        public RestEntityUrl<T> rest() {
            return new RestEntityUrl<>( baseUrl, entity );
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

    public static class ExpressionExperimentUrl extends EntityUrl<ExpressionExperiment> {

        private ExpressionExperimentUrl( String baseUrl, ExpressionExperiment entity ) {
            super( baseUrl, entity );
        }

        @Override
        public ExpressionExperimentWebUrl web() {
            return new ExpressionExperimentWebUrl( baseUrl, entity );
        }
    }

    /**
     * Generate a URL for Gemma Web.
     */
    public static class WebEntityUrl<U extends Identifiable> extends EntityUrl<U> {

        protected final String entityPath;

        private WebEntityUrl( String baseUrl, U entity ) {
            super( baseUrl, entity );
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
            } else if ( entity instanceof BioMaterial ) {
                this.entityPath = "/bioMaterial/showBioMaterial.html?id=";
            } else {
                throw new UnsupportedOperationException( "Cannot generate a Web URL for entities of type " + entity.getClass() + "." );
            }
        }

        public URI toUri() {
            return URI.create( baseUrl + entityPath + entity.getId() );
        }
    }

    public static class ExpressionExperimentWebUrl extends WebEntityUrl<ExpressionExperiment> {

        private boolean byShortName = false;
        private boolean edit = false;

        private ExpressionExperimentWebUrl( String baseUrl, ExpressionExperiment entity ) {
            super( baseUrl, entity );
        }

        public ExperimentalDesignWebUrl design() {
            Assert.notNull( entity.getExperimentalDesign(), entity + " does not have an experimental design." );
            return new ExperimentalDesignWebUrl( baseUrl, entity, entity.getExperimentalDesign() );
        }

        public ExpressionExperimentWebUrl edit() {
            edit = true;
            return this;
        }

        @Override
        public URI toUri() {
            if ( edit ) {
                return URI.create( baseUrl + "/expressionExperiment/editExpressionExperiment.html?id=" + entity.getId() );
            } else if ( byShortName ) {
                return URI.create( baseUrl + "/expressionExperiment/showExpressionExperiment.html?shortName=" + urlEncode( entity.getShortName() ) );
            } else {
                return super.toUri();
            }
        }

        public ExpressionExperimentWebUrl byShortName() {
            Assert.isTrue( StringUtils.isNotBlank( entity.getShortName() ) );
            byShortName = true;
            return this;
        }
    }

    public static class ExperimentalDesignWebUrl extends WebEntityUrl<ExperimentalDesign> {

        private final ExpressionExperiment experiment;

        private ExperimentalDesignWebUrl( String baseUrl, ExpressionExperiment ee, ExperimentalDesign entity ) {
            super( baseUrl, entity );
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
    public static class RestEntityUrl<T extends Identifiable> extends EntityUrl<T> {

        private final String entityPath;

        private RestEntityUrl( String baseUrl, T entity ) {
            super( baseUrl, entity );
            if ( entity instanceof ExpressionExperiment ) {
                this.entityPath = "/datasets";
            } else if ( entity instanceof ArrayDesign ) {
                this.entityPath = "/platforms";
            } else if ( entity instanceof Taxon ) {
                this.entityPath = "/taxa";
            } else if ( entity instanceof ExpressionAnalysisResultSet ) {
                this.entityPath = "/resultSets";
            } else {
                throw new UnsupportedOperationException( "Cannot generate a REST URL for entities of type " + entity.getClass() + "." );
            }
        }

        public URI toUri() {
            return URI.create( baseUrl + "/rest/v2" + entityPath + "/" + entity.getId() );
        }
    }

    public static class AllEntitiesUrl<T extends Identifiable> {

        protected final String baseUrl;
        private final Class<T> entityType;

        private AllEntitiesUrl( String baseUrl, Class<T> entityType ) {
            this.baseUrl = baseUrl;
            this.entityType = entityType;
        }

        public AllWebEntitiesUrl<T> web() {
            return new AllWebEntitiesUrl<>( baseUrl, entityType );
        }

        public AllRestEntitiesUrl<T> rest() {
            return new AllRestEntitiesUrl<>( baseUrl, entityType );
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

    public static class AllWebEntitiesUrl<T extends Identifiable> extends AllEntitiesUrl<T> {

        private final String entityPath;

        private AllWebEntitiesUrl( String baseUrl, Class<T> entityType ) {
            super( baseUrl, entityType );
            if ( ExpressionExperiment.class.isAssignableFrom( entityType ) ) {
                this.entityPath = "/expressionExperiment/showAllExpressionExperiments.html";
            } else if ( ArrayDesign.class.isAssignableFrom( entityType ) ) {
                this.entityPath = "/arrays/showAllArrayDesigns.html";
            } else {
                throw new UnsupportedOperationException( "Cannot generate a Web URL for entities of type " + entityType + "." );
            }
        }

        @Override
        public URI toUri() {
            return URI.create( baseUrl + entityPath );
        }
    }

    public static class AllRestEntitiesUrl<T extends Identifiable> extends AllEntitiesUrl<T> {

        private final String entityPath;

        private AllRestEntitiesUrl( String baseUrl, Class<T> entityType ) {
            super( baseUrl, entityType );
            if ( ExpressionExperiment.class.isAssignableFrom( entityType ) ) {
                this.entityPath = "/datasets";
            } else if ( ArrayDesign.class.isAssignableFrom( entityType ) ) {
                this.entityPath = "/platforms";
            } else if ( Taxon.class.isAssignableFrom( entityType ) ) {
                this.entityPath = "/taxa";
            } else if ( ExpressionAnalysisResultSet.class.isAssignableFrom( entityType ) ) {
                this.entityPath = "/resultSets";
            } else {
                throw new UnsupportedOperationException( "Cannot generate a REST URL for entities of type " + entityType + "." );
            }
        }

        @Override
        public URI toUri() {
            return URI.create( baseUrl + "/rest/v2" + entityPath );
        }
    }

    private static String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }

    public static class ArrayDesignUrl extends WebEntityUrl<ArrayDesign> {

        public ArrayDesignUrl( String baseUrl, ArrayDesign entity ) {
            super( baseUrl, entity );
        }

        @Override
        public ArrayDesignWebUrl web() {
            return new ArrayDesignWebUrl( baseUrl, entity );
        }
    }

    public static class ArrayDesignWebUrl extends WebEntityUrl<ArrayDesign> {

        private boolean byShortName = false;

        public ArrayDesignWebUrl( String baseUrl, ArrayDesign entity ) {
            super( baseUrl, entity );
        }

        public ArrayDesignWebUrl byShortName() {
            Assert.isTrue( StringUtils.isNotBlank( entity.getShortName() ) );
            this.byShortName = true;
            return this;
        }

        @Override
        public URI toUri() {
            if ( byShortName ) {
                return URI.create( baseUrl + "/arrays/showArrayDesign.html?shortName=" + urlEncode( entity.getShortName() ) );
            } else {
                return super.toUri();
            }
        }
    }
}