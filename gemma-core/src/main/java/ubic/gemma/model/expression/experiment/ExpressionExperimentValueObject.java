package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclPrincipalSid;
import ubic.gemma.core.security.acl.domain.AclSid;
import ubic.gemma.core.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignReferenceValueObject;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.core.loader.util.ExternalDatabaseUtils;
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.util.ModelUtils;
import ubic.gemma.persistence.util.SecurityUtils;

import org.springframework.lang.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "unused", "WeakerAccess" }) // used in front end
@Getter
@Setter
public class ExpressionExperimentValueObject extends AbstractCuratableValueObject<ExpressionExperiment> implements BioAssaySetValueObject {

    private static final long serialVersionUID = -6861385216096602508L;
    protected Integer numberOfBioAssays;
    protected String description;
    protected String name;

    // TODO: migrate this to a DatabaseEntryValueObject and remove the individual external fields below. See
    //       <a href="https://github.com/PavlidisLab/Gemma/issues/450">#450</a> for details.
    /**
     * @see DatabaseEntryValueObject#getAccession()
     */
    @Nullable
    private String accession;
    /**
     * @see DatabaseEntryValueObject#getUri()
     */
    @Nullable
    private String externalUri;
    /**
     * @see DatabaseEntryValueObject#getLabel()
     */
    @Nullable
    private String externalLabel;
    /**
     * @see ExternalDatabaseValueObject#getName()
     */
    @Nullable
    private String externalDatabase;
    /**
     * @see ExternalDatabaseValueObject#getUri()
     */
    @Nullable
    private String externalDatabaseUri;

    /**
     * PubMed ID of the primary publication, when it is indexed by PubMed. Mutually exclusive with
     * {@link #doi}: the primary publication carries a single accession, so a PubMed-indexed paper
     * populates this and a preprint (bioRxiv/arXiv/CrossRef DOI) populates {@link #doi}.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String pubmedId;
    /**
     * DOI of the primary publication, when it is a preprint or otherwise identified by a DOI rather
     * than a PubMed ID. See {@link #pubmedId}.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String doi;

    @JsonProperty("numberOfArrayDesigns")
    private Long arrayDesignCount;

    /**
     * The platforms this dataset's assays were run on, as accession + full name.
     * <p>
     * A list because a dataset may use more than one; {@link #arrayDesignCount} is the size of this
     * list and is kept because clients read it. Populated on every filtered read out of the same
     * query that produces the count — the join to the platform was already being paid for.
     */
    @Schema(description = "Platforms the dataset's assays were run on, as id + shortName + name. Empty when the dataset has no assays.")
    private List<ArrayDesignReferenceValueObject> platforms;

    /**
     * The platforms this dataset's assays were run on BEFORE a platform switch, when one happened.
     * <p>
     * Empty for the great majority of datasets. A no-op switch — an original platform that is also
     * a platform in use — is left out, so a non-empty list here means the dataset really was moved.
     */
    @Schema(description = "Platforms the assays were originally run on, when the dataset was switched to another platform. Empty when there was no switch; a switch to the same platform is not reported.")
    private List<ArrayDesignReferenceValueObject> originalPlatforms;

    /**
     * When the dataset was created in Gemma — loaded, not published.
     * <p>
     * Read from the {@code C} audit event, which is the only record of it: there is no creation
     * column on the dataset (a {@code CURATION_DETAILS.CREATED} backfill was proposed and deferred,
     * 2026-08-21). Measured universal — 200 of 200 sampled datasets carry the event — but null is
     * still possible and means the event is missing, never "created just now".
     * <p>
     * 🛑 <b>Not filterable or sortable.</b> {@code AbstractCuratableDao} unregisters
     * {@code auditTrail.*} from the dataset filter surface, so this is a projection for display.
     * Filtering on it is what the deferred migration was for.
     */
    @Schema(description = "When the dataset was loaded into Gemma, from its creation audit event. Null when that event is missing. Display only — not filterable or sortable.")
    private Date dateCreated;
    private String batchConfound;
    /**
     * Batch effect type. See {@link BatchEffectType} enum for possible values.
     */
    @Schema(implementation = BatchEffectType.class)
    private String batchEffect;
    /**
     * Summary statistics of a batch effect is present.
     */
    @Nullable
    private String batchEffectStatistics;
    @JsonIgnore
    private Integer bioMaterialCount;
    @JsonIgnore
    private Long experimentalDesign;
    private GeeqValueObject geeq;
    private String metadata;
    @JsonProperty("numberOfProcessedExpressionVectors")
    private Integer processedExpressionVectorCount;
    private String shortName;
    private String source;
    @JsonIgnore
    private Boolean suitableForDEA = true;

    // these are populated by gsec
    private boolean isPublic = false;
    @JsonIgnore
    private boolean isShared = false;
    @JsonIgnore
    private boolean userCanWrite = false;
    @JsonIgnore
    private boolean userOwned = false;

    /**
     * FIXME: this should be named simply "taxon", but that field is already taken for Gemma Web, see {@link #getTaxon()}.
     */
    @Nullable
    @JsonProperty("taxon")
    private TaxonValueObject taxonObject;

    private String technologyType;

    /**
     * Whether this is a single-cell experiment, i.e. it has a preferred single-cell quantitation type.
     * <p>
     * That is the same condition every single-cell route resolves against
     * ({@code getPreferredSingleCellQuantitationType}), so a dataset this reports true for is one
     * {@code /cellTypeAssignment} and {@code /singleCellDimension} can actually serve. 546 datasets on prod —
     * 29 more than {@code SINGLE_CELL_DIMENSION_EXPERIMENT} indexes, which is why the flag and not that table
     * decides this.
     * <p>
     * It is here rather than on the details VO because {@code technologyType} does not answer the question —
     * eid 79038 is single-cell and reads {@code GENELIST}, the generic-platform placeholder — which left
     * clients inferring modality from a regex over platform and assay strings, blind to a dataset annotated
     * with none of the expected words and fooled by a title that merely mentions single cell (uib, 2026-09-03).
     */
    private boolean isSingleCell;

    /**
     * Total number of cells, or {@code null} when this is not a single-cell experiment or the count has not
     * been computed. Denormalized on the experiment itself, so it costs nothing to serve.
     * <p>
     * 🛑 Not a substitute for {@link #isSingleCell}: 63 of the 546 single-cell datasets on prod have no count.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer numberOfCells;

    /**
     * Number of cell IDs in the preferred single-cell dimension, or {@code null} when there is none.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer numberOfCellIds;

    /**
     * The other parts of the study this dataset was split off from, empty when it was not split.
     * <p>
     * Gemma splits an experiment by a factor — usually organism part for single-cell data — and names each
     * part {@code Split part N of: … [organism part = …]}. That title tells a reader siblings exist and gives
     * them no way to reach one: 52 of 100 sampled single-cell datasets are split parts over 32 parent studies,
     * and neither the curation UI nor the browser could follow the link, because the field lived on a VO only
     * {@code /experiment-sets/{id}/datasets} serves (uib, 2026-09-03).
     * <p>
     * References rather than whole VOs: a sibling is rendered as a name and a link, and the previous full-VO
     * form cost a {@code loadValueObjectsByIds} per split experiment.
     */
    private List<ExpressionExperimentReferenceValueObject> otherParts = new ArrayList<>();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<CharacteristicValueObject> characteristics;

    @Nullable
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "nothing originates it: the copy constructor propagates it but no constructor, setter call site, HQL projection or alias transformer ever sets a value, and there is no MIN_PVALUE column — so it would serialize a permanent null")
    private Double minPvalue;

    /**
     * Required when using the class as a spring bean.
     */
    public ExpressionExperimentValueObject() {
        super();
    }

    public ExpressionExperimentValueObject( Long id ) {
        super( id );
    }

    /**
     * Creates a new value object out of given Expression Experiment.
     *
     * @param ee              the experiment to convert into a value object.
     * @param ignoreDesign    exclude the experimental design from serialization
     * @param ignoreAccession exclude accession from serialization
     */
    public ExpressionExperimentValueObject( ExpressionExperiment ee, boolean ignoreDesign, boolean ignoreAccession ) {
        this( ee, ignoreDesign, ignoreAccession, false );
    }

    /**
     * Variant that skips reading the three {@code last*Event} associations off
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails} when
     * {@code skipEvents=true}. Use this from a transformer that batch-hydrates the events
     * post-fetch and calls
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject#applyLastEventTriple(ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject.LastEventTriple)}
     * to fill them in once a per-page prefetch is available.
     */
    public ExpressionExperimentValueObject( ExpressionExperiment ee, boolean ignoreDesign, boolean ignoreAccession, boolean skipEvents ) {
        super( ee, skipEvents );
        this.shortName = ee.getShortName();
        this.name = ee.getName();
        this.source = ee.getSource();
        this.description = ee.getDescription();

        // accession
        if ( !ignoreAccession && ee.getAccession() != null && ModelUtils.isInitialized( ee.getAccession() ) ) {
            this.accession = ee.getAccession().getAccession();
            this.externalUri = ExternalDatabaseUtils.getUri( ee.getAccession() );
            this.externalLabel = ExternalDatabaseUtils.getLabel( ee.getAccession() );
            this.externalDatabase = ee.getAccession().getExternalDatabase().getName();
            this.externalDatabaseUri = ExternalDatabaseUtils.getUri( ee.getAccession().getExternalDatabase() );
        }

        // primary-publication identifier: the pubAccession carries either a PubMed ID or a preprint
        // DOI, discriminated by its external database. Guarded like accession so an uninitialized
        // (lazy) primaryPublication is never forced.
        BibliographicReference primaryPublication = ee.getPrimaryPublication();
        if ( primaryPublication != null && ModelUtils.isInitialized( primaryPublication ) ) {
            DatabaseEntry pubAccession = primaryPublication.getPubAccession();
            if ( pubAccession != null && ModelUtils.isInitialized( pubAccession )
                    && pubAccession.getExternalDatabase() != null ) {
                String pubDb = pubAccession.getExternalDatabase().getName();
                if ( ExternalDatabases.PUBMED.equals( pubDb ) ) {
                    this.pubmedId = pubAccession.getAccession();
                } else if ( ExternalDatabases.DOI.equals( pubDb )
                        || ExternalDatabases.BIORXIV.equals( pubDb )
                        || ExternalDatabases.ARXIV.equals( pubDb ) ) {
                    this.doi = pubAccession.getAccession();
                }
            }
        }

        // EE
        this.metadata = ee.getMetadata();
        this.processedExpressionVectorCount = ee.getNumberOfDataVectors();

        if ( ee.getTaxon() != null ) {
            this.taxonObject = new TaxonValueObject( ee.getTaxon() );
        }

        // Denormalized on the experiment; isSingleCell and numberOfCellIds need a query and are filled in by
        // ExpressionExperimentDaoImpl#populateSingleCellInfo, one batched query per page.
        this.numberOfCells = ee.getNumberOfCells();

        // Counts
        if ( ModelUtils.isInitialized( ee.getBioAssays() ) ) {
            this.numberOfBioAssays = ee.getBioAssays().size();
        } else {
            // this is a denormalization, so we merely use it as a fallback if bioAssays are not initialized
            this.numberOfBioAssays = ee.getNumberOfSamples();
        }

        // ED
        if ( !ignoreDesign && ee.getExperimentalDesign() != null && ModelUtils.isInitialized( ee.getExperimentalDesign() ) ) {
            this.experimentalDesign = ee.getExperimentalDesign().getId();
        }

        // Batch info
        if ( ee.getBatchEffect() != null ) {
            batchEffect = ee.getBatchEffect().name();
        }
        batchEffectStatistics = ee.getBatchEffectStatistics();
        batchConfound = ee.getBatchConfound();

        // GEEQ: for administrators, create an admin geeq VO. Normal GEEQ VO otherwise.
        if ( ee.getGeeq() != null && ModelUtils.isInitialized( ee.getGeeq() ) ) {
            geeq = SecurityUtil.isUserAdmin() ?
                    new GeeqAdminValueObject( ee.getGeeq() ) :
                    new GeeqValueObject( ee.getGeeq() );
        } else {
            geeq = null;
        }

        if ( ModelUtils.isInitialized( ee.getCharacteristics() ) ) {
            characteristics = ee.getCharacteristics().stream()
                    .map( CharacteristicValueObject::new )
                    .collect( Collectors.toSet() );
        }
    }

    public ExpressionExperimentValueObject( ExpressionExperiment ee ) {
        this( ee, false, false );
    }

    /**
     * Creates a new {@link ExpressionExperiment} value object with additional information about ownership.
     */
    public ExpressionExperimentValueObject( ExpressionExperiment ee, AclObjectIdentity aoi, AclSid sid ) {
        this( ee );
        populateAclInfo( this, aoi, sid );
    }

    /**
     * Apply ACL-derived flags (isPublic, userCanWrite, isShared, userOwned) onto an existing VO.
     * <p>
     * Extracted from the {@code (ExpressionExperiment, AclObjectIdentity, AclSid)} constructor so
     * that the EXISTS-rewritten filtering query path can post-fetch ACL info and inject it onto a
     * VO that was constructed without the {@code aoi}/{@code sid} pair available at projection time.
     * Public so DAOs in other packages (e.g. {@code ExpressionExperimentDaoImpl}) can reach it.
     */
    public static void populateAclInfo( ExpressionExperimentValueObject vo,
            @org.springframework.lang.Nullable AclObjectIdentity aoi,
            @org.springframework.lang.Nullable AclSid sid ) {
        if ( aoi != null ) {
            boolean[] permissions = SecurityUtils.getPermissions( aoi );
            vo.setIsPublic( permissions[0] );
            vo.setUserCanWrite( permissions[1] );
            vo.setIsShared( permissions[2] );
        }
        String ownerName = ubic.gemma.core.security.acl.domain.Sids.principalName( sid == null ? null : sid.toSid() );
        vo.setUserOwned( ownerName != null && Objects.equals( ownerName, SecurityUtil.getCurrentUsername() ) );
    }

    public ExpressionExperimentValueObject( ExpressionExperimentIdAndShortName ee ) {
        this( ee.getId() );
        this.shortName = ee.getShortName();
    }

    protected ExpressionExperimentValueObject( ExpressionExperimentValueObject vo ) {
        super( vo );
        this.name = vo.name;
        this.description = vo.description;
        this.numberOfBioAssays = vo.numberOfBioAssays;
        this.accession = vo.getAccession();
        this.batchConfound = vo.getBatchConfound();
        this.batchEffect = vo.getBatchEffect();
        this.batchEffectStatistics = vo.getBatchEffectStatistics();
        this.externalDatabase = vo.getExternalDatabase();
        this.externalDatabaseUri = vo.getExternalDatabaseUri();
        this.externalUri = vo.getExternalUri();
        this.externalLabel = vo.getExternalLabel();
        this.metadata = vo.getMetadata();
        this.shortName = vo.getShortName();
        this.source = vo.getSource();
        this.taxonObject = vo.getTaxonObject();
        this.technologyType = vo.getTechnologyType();
        this.experimentalDesign = vo.getExperimentalDesign();
        this.processedExpressionVectorCount = vo.getProcessedExpressionVectorCount();
        this.arrayDesignCount = vo.getArrayDesignCount();
        this.platforms = vo.getPlatforms();
        this.originalPlatforms = vo.getOriginalPlatforms();
        this.dateCreated = vo.getDateCreated();
        this.bioMaterialCount = vo.getBioMaterialCount();
        this.userCanWrite = vo.getUserCanWrite();
        this.userOwned = vo.getUserOwned();
        this.isPublic = vo.getIsPublic();
        this.isShared = vo.getIsShared();
        this.geeq = vo.getGeeq();
        this.suitableForDEA = vo.getSuitableForDEA();
        this.characteristics = vo.getCharacteristics();
        this.minPvalue = vo.getMinPvalue();
    }

    /**
     * Obtain the number of {@link ubic.gemma.model.expression.bioAssay.BioAssay} in this experiment.
     *
     * @deprecated use {@link #getNumberOfBioAssays()} instead.
     */
    @Deprecated
    public int getBioAssayCount() {
        return numberOfBioAssays;
    }

    /**
     * Lombok would name these {@code isSingleCell} / {@code setSingleCell}; the wire name and the convention
     * these flags follow in this class is {@code getIsX} / {@code setIsX} (see {@link #getIsPublic()}).
     */
    public boolean getIsSingleCell() {
        return this.isSingleCell;
    }

    public void setIsSingleCell( boolean isSingleCell ) {
        this.isSingleCell = isSingleCell;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    @JsonIgnore
    public boolean getIsShared() {
        return this.isShared;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "flattened common name; taxonObject already serializes")
    public String getTaxon() {
        return taxonObject == null ? null : taxonObject.getCommonName();
    }

    /**
     * @deprecated use {@link #getTaxonObject()} instead
     */
    @Deprecated
    public Long getTaxonId() {
        return taxonObject == null ? null : taxonObject.getId();
    }

    @Override
    @JsonIgnore
    public Class<? extends Securable> getSecurableClass() {
        return ExpressionExperiment.class;
    }

    @Override
    @JsonIgnore
    public boolean getUserCanWrite() {
        return this.userCanWrite;
    }

    @Override
    @JsonIgnore
    public boolean getUserOwned() {
        return this.userOwned;
    }

    @Override
    public void setIsPublic( boolean b ) {
        this.isPublic = b;
    }

    @Override
    public void setIsShared( boolean b ) {
        this.isShared = b;
    }

    @Override
    public void setUserCanWrite( boolean userCanWrite ) {
        this.userCanWrite = userCanWrite;
    }

    @Override
    public void setUserOwned( boolean isUserOwned ) {
        this.userOwned = isUserOwned;
    }

    @WithheldFromApi(value = Reason.CALLER_IDENTITY,
            comment = "per-principal permission on a response cached by URL")
    public boolean getCurrentUserHasWritePermission() {
        return userCanWrite;
    }

    @WithheldFromApi(value = Reason.CALLER_IDENTITY,
            comment = "per-principal ownership on a response cached by URL")
    public boolean getCurrentUserIsOwner() {
        return userOwned;
    }

    @Override
    public String toString() {
        return this.shortName + " (id = " + this.getId() + ")";
    }
}