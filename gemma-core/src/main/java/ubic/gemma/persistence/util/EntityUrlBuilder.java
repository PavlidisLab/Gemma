package ubic.gemma.persistence.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This builder allows for generating URLs for entities in Gemma Web and REST.
 * @author poirigui
 */
public class EntityUrlBuilder {

    private final String hostUrl;

    private boolean webByDefault = false;
    private boolean restByDefault = false;

    public EntityUrlBuilder( String hostUrl ) {
        Assert.isTrue( !hostUrl.endsWith( "/" ), "The context path must not end with '/'." );
        this.hostUrl = hostUrl;
    }

    public void setWebByDefault() {
        Assert.state( !restByDefault, "Cannot set both Web and REST by default." );
        this.webByDefault = true;
    }

    public void setRestByDefault() {
        Assert.state( !webByDefault, "Cannot set both Web and REST by default." );
        this.restByDefault = true;
    }

    /**
     * Obtain an {@link EntityUrlChooser} for generating a URL relative to the host URL.
     * <p>
     * Use this for absolute URLs.
     */
    public EntityUrlChooser fromHostUrl() {
        return fromBaseUrl( hostUrl );
    }

    /**
     * Obtain an {@link EntityUrlChooser} for generating a URL relative to a base URL.
     * <p>
     * Use this for relative URLs.
     */
    public EntityUrlChooser fromBaseUrl( String baseUrl ) {
        return new EntityUrlChooser( baseUrl );
    }

    /**
     * Allows for choosing a specific entity.
     */
    public class EntityUrlChooser {

        private final String baseUrl;

        public EntityUrlChooser( String baseUrl ) {
            Assert.isTrue( !baseUrl.endsWith( "/" ), "The base URL must not end with '/'." );
            this.baseUrl = baseUrl;
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

        public <T extends Identifiable> EntityUrl<T> entity( Class<T> entityType, Long id ) {
            T entity = BeanUtils.instantiate( entityType );
            if ( entity instanceof AbstractIdentifiable ) {
                ( ( AbstractIdentifiable ) entity ).setId( id );
            } else {
                throw new UnsupportedOperationException();
            }
            return new EntityUrl<>( baseUrl, entity );
        }

        public <T extends Identifiable> AllEntitiesUrl<T> all( Class<T> entityType ) {
            return new AllEntitiesUrl<>( baseUrl, entityType );
        }

        public <T extends Identifiable> SomeEntitiesUrl<T> some( Collection<T> entities ) {
            return new SomeEntitiesUrl<>( baseUrl, entities );
        }

        public <T extends Identifiable> SomeEntitiesUrl<T> some( Class<T> entityType, Collection<Long> ids ) {
            return new SomeEntitiesUrl<>( baseUrl, entityType, ids );
        }
    }

    /**
     * Represents a URL for an {@link Identifiable} entity.
     */
    public class EntityUrl<T extends Identifiable> {

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

        /**
         * Generate a URL for ontologies served by Gemma.
         */
        public OntologyEntityUrl<T> ont() {
            return new OntologyEntityUrl<>( baseUrl, entity );
        }

        public URI toUri() {
            return webByDefault ? web().toUri() : restByDefault ? rest().toUri() : raiseNoDefault();
        }

        public String toUriString() {
            return toUri().toString();
        }

        @Override
        public String toString() {
            return toUriString();
        }
    }

    public class ExpressionExperimentUrl extends EntityUrl<ExpressionExperiment> {

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
    public class WebEntityUrl<U extends Identifiable> extends EntityUrl<U> {

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
            } else if ( entity instanceof Taxon ) {
                this.entityPath = "/taxon/showTaxon.html?id=";
            } else if ( entity instanceof Gene ) {
                this.entityPath = "/gene/showGene.html?id=";
            } else if ( entity instanceof CompositeSequence ) {
                this.entityPath = "/compositeSequence/show.html?id=";
            } else {
                throw new UnsupportedOperationException( "Cannot generate a Web URL for entities of type " + entity.getClass() + "." );
            }
        }

        public URI toUri() {
            return URI.create( baseUrl + entityPath + entity.getId() );
        }
    }

    public class ExpressionExperimentWebUrl extends WebEntityUrl<ExpressionExperiment> {

        private boolean byShortName = false;

        private String entityPath = "/expressionExperiment/showExpressionExperiment.html";
        private String additionalQuery = "";

        private ExpressionExperimentWebUrl( String baseUrl, ExpressionExperiment entity ) {
            super( baseUrl, entity );
        }

        public ExpressionExperimentWebUrl byShortName() {
            Assert.isTrue( StringUtils.isNotBlank( entity.getShortName() ) );
            byShortName = true;
            return this;
        }

        public ExperimentalDesignWebUrl design() {
            Assert.notNull( entity.getExperimentalDesign(), entity + " does not have an experimental design." );
            return new ExperimentalDesignWebUrl( baseUrl, entity, entity.getExperimentalDesign() );
        }

        public ExpressionExperimentWebUrl edit() {
            entityPath = "/expressionExperiment/editExpressionExperiment.html";
            additionalQuery = "";
            return this;
        }

        public ExpressionExperimentWebUrl bioAssays() {
            entityPath = "/expressionExperiment/showBioAssaysFromExpressionExperiment.html";
            additionalQuery = "";
            return this;
        }

        public ExpressionExperimentWebUrl bioMaterials() {
            entityPath = "/expressionExperiment/showBioMaterialsFromExpressionExperiment.html";
            additionalQuery = "";
            return this;
        }

        public ExpressionExperimentWebUrl showSingleCellExpressionData( QuantitationType quantitationType, CompositeSequence designElement, @Nullable List<BioAssay> assays, @Nullable CellLevelCharacteristics cellLevelCharacteristics, @Nullable Characteristic focusedCharacteristic ) {
            Assert.state( !byShortName, "Single-cell box plots cannot be visualized by short name." );
            entityPath = "/expressionExperiment/showSingleCellExpressionData.html";
            additionalQuery = "&quantitationType=" + quantitationType.getId();
            additionalQuery += "&designElement=" + designElement.getId();
            if ( assays != null ) {
                for ( BioAssay assay : assays ) {
                    additionalQuery += "&assays=" + assay.getId();
                }
            }
            if ( cellLevelCharacteristics instanceof CellTypeAssignment ) {
                additionalQuery += "&cellTypeAssignment=" + cellLevelCharacteristics.getId();
            } else if ( cellLevelCharacteristics != null ) {
                additionalQuery += "&cellLevelCharacteristics=" + cellLevelCharacteristics.getId();
            }
            if ( focusedCharacteristic != null ) {
                additionalQuery += "&focusedCharacteristic=" + focusedCharacteristic.getId();
            }
            return this;
        }

        @Override
        public URI toUri() {
            if ( byShortName ) {
                return URI.create( baseUrl + entityPath + "?shortName=" + urlEncode( entity.getShortName() ) + additionalQuery );
            } else {
                return URI.create( baseUrl + entityPath + "?id=" + entity.getId() + additionalQuery );
            }
        }
    }

    public class ExperimentalDesignWebUrl extends WebEntityUrl<ExperimentalDesign> {

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
    public class RestEntityUrl<T extends Identifiable> extends EntityUrl<T> {

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

    public class AllEntitiesUrl<T extends Identifiable> {

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
            return webByDefault ? web().toUri() : restByDefault ? rest().toUri() : raiseNoDefault();
        }

        public String toUriString() {
            return toUri().toString();
        }

        @Override
        public String toString() {
            return toUriString();
        }
    }

    public class AllWebEntitiesUrl<T extends Identifiable> extends AllEntitiesUrl<T> {

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

    public class AllRestEntitiesUrl<T extends Identifiable> extends AllEntitiesUrl<T> {

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

    public class SomeEntitiesUrl<T extends Identifiable> {

        protected final String baseUrl;
        protected final Class<T> entityType;
        protected final Collection<Long> ids;

        private SomeEntitiesUrl( String baseUrl, Class<T> entityType, Collection<Long> ids ) {
            this.baseUrl = baseUrl;
            this.entityType = entityType;
            this.ids = ids;
        }

        private SomeEntitiesUrl( String baseUrl, Collection<T> entities ) {
            this( baseUrl, selectCommonType( entities ), entities.stream().map( Identifiable::getId ).collect( Collectors.toSet() ) );
        }

        public SomeWebEntitiesUrl<T> web() {
            return new SomeWebEntitiesUrl<>( baseUrl, entityType, ids );
        }

        public SomeRestEntitiesUrl<T> rest() {
            return new SomeRestEntitiesUrl<>( baseUrl, entityType, ids );
        }

        public URI toUri() {
            return webByDefault ? web().toUri() : restByDefault ? rest().toUri() : raiseNoDefault();
        }

        public String toUriString() {
            return toUri().toString();
        }

        @Override
        public String toString() {
            return toUriString();
        }
    }

    public class SomeWebEntitiesUrl<T extends Identifiable> extends SomeEntitiesUrl<T> {

        private final String entityPath;

        private SomeWebEntitiesUrl( String baseUrl, Class<T> entityType, Collection<Long> ids ) {
            super( baseUrl, entityType, ids );
            if ( ExpressionExperiment.class.isAssignableFrom( entityType ) ) {
                this.entityPath = "/expressionExperiment/showAllExpressionExperiments.html?id=";
            } else if ( ArrayDesign.class.isAssignableFrom( entityType ) ) {
                this.entityPath = "/arrays/showAllArrayDesigns.html?id=";
            } else {
                throw new UnsupportedOperationException( "Cannot generate a Web URL for entities of type " + entityType + "." );
            }
        }

        public URI toUri() {
            return URI.create( baseUrl + entityPath + StringUtils.join( ids, "," ) );
        }
    }

    public class SomeRestEntitiesUrl<T extends Identifiable> extends SomeEntitiesUrl<T> {

        public SomeRestEntitiesUrl( String baseUrl, Class<T> entityType, Collection<Long> ids ) {
            super( baseUrl, entityType, ids );
            throw new UnsupportedOperationException( "Cannot generate a REST URL for entities of type " + entityType + "." );
        }
    }

    public class ArrayDesignUrl extends WebEntityUrl<ArrayDesign> {

        public ArrayDesignUrl( String baseUrl, ArrayDesign entity ) {
            super( baseUrl, entity );
        }

        @Override
        public ArrayDesignWebUrl web() {
            return new ArrayDesignWebUrl( baseUrl, entity );
        }
    }

    public class ArrayDesignWebUrl extends WebEntityUrl<ArrayDesign> {

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

    public class OntologyEntityUrl<T extends Identifiable> extends EntityUrl<T> {

        /**
         * Applies to any ontologies served by Gemma, including TGEMO and TGFVO.
         */
        private static final String GEMMA_ONTOLOGY_PREFIX = "http://gemma.msl.ubc.ca/ont";

        private final String entityPath;

        private OntologyEntityUrl( String baseUrl, T entity ) {
            super( baseUrl, entity );
            if ( entity instanceof FactorValue ) {
                entityPath = "/ont/TGFVO/" + entity.getId();
            } else if ( entity instanceof Characteristic ) {
                Characteristic c = ( Characteristic ) entity;
                if ( c.getValueUri() != null && c.getValueUri().startsWith( GEMMA_ONTOLOGY_PREFIX ) ) {
                    entityPath = "/ont" + c.getValueUri().substring( GEMMA_ONTOLOGY_PREFIX.length() );
                } else if ( c.getCategoryUri() != null && c.getCategoryUri().startsWith( GEMMA_ONTOLOGY_PREFIX ) ) {
                    entityPath = "/ont" + c.getCategoryUri().substring( GEMMA_ONTOLOGY_PREFIX.length() );
                } else {
                    throw new UnsupportedOperationException( "Cannot generate an ontology URL for entities of type " + entity.getClass() + "." );
                }
            } else {
                throw new UnsupportedOperationException( "Cannot generate an ontology URL for entities of type " + entity.getClass() + "." );
            }
        }

        public URI toUri() {
            return URI.create( baseUrl + entityPath );
        }
    }

    private URI raiseNoDefault() {
        throw new IllegalStateException( "There is no default way to generate an URL." );
    }

    private <T> Class<T> selectCommonType( Collection<?> elements ) {
        // FIXME: do better...
        //noinspection unchecked
        return ( Class<T> ) elements.iterator().next().getClass();
    }

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}