package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.acl.domain.AclSid;
import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import gemma.gsec.util.SecurityUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.Hibernate;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Objects;

@SuppressWarnings({ "unused", "WeakerAccess" }) // used in front end
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpressionExperimentValueObject extends AbstractCuratableValueObject<ExpressionExperiment>
        implements SecureValueObject {

    private static final long serialVersionUID = -6861385216096602508L;
    protected Integer numberOfBioAssays;
    protected String description;
    protected String name;

    private String accession;
    @JsonProperty("numberOfArrayDesigns")
    private Long arrayDesignCount;
    private String batchConfound;
    private String batchEffect;
    @JsonIgnore
    private Integer bioMaterialCount;
    @JsonIgnore
    private Boolean currentUserHasWritePermission = null;
    @JsonIgnore
    private Boolean currentUserIsOwner = null;
    @JsonIgnore
    private Long experimentalDesign;
    private String externalDatabase;
    private String externalUri;
    private GeeqValueObject geeq;
    @JsonIgnore
    private Boolean isPublic = false;
    @JsonIgnore
    private Boolean isShared = false;
    private String metadata;
    @JsonProperty("numberOfProcessedExpressionVectors")
    private Integer processedExpressionVectorCount;
    private String shortName;
    private String source;
    @JsonIgnore
    private Boolean suitableForDEA = true;
    private String taxon;
    private Long taxonId;

    private String technologyType;

    /**
     * Required when using the class as a spring bean.
     */
    protected ExpressionExperimentValueObject() {
    }

    protected ExpressionExperimentValueObject( Long id ) {
        super( id );
    }

    /**
     * Creates a new value object out of given Expression Experiment.
     *
     * @param ee the experiment to convert into a value object.
     */
    public ExpressionExperimentValueObject( ExpressionExperiment ee ) {
        super( ee );
        this.shortName = ee.getShortName();
        this.name = ee.getName();
        this.source = ee.getSource();
        this.description = ee.getDescription();

        // accession
        if ( ee.getAccession() != null && Hibernate.isInitialized( ee.getAccession() ) ) {
            this.accession = ee.getAccession().getAccession();
            this.externalDatabase = ee.getAccession().getExternalDatabase().getName();
            this.externalUri = ee.getAccession().getExternalDatabase().getWebUri();
        }

        // EE
        this.metadata = ee.getMetadata();
        this.processedExpressionVectorCount = ee.getNumberOfDataVectors();

        if ( ee.getTaxon() != null && Hibernate.isInitialized( ee.getTaxon() ) ) {
            this.taxon = ee.getTaxon().getCommonName();
            this.taxonId = ee.getTaxon().getId();
        }

        // Counts
        if ( ee.getBioAssays() != null && Hibernate.isInitialized( ee.getBioAssays() ) ) {
            this.numberOfBioAssays = ee.getBioAssays().size();
        } else {
            // this is a denormalization, so we merely use it as a fallback if bioAssays are not initialized
            this.numberOfBioAssays = ee.getNumberOfSamples();
        }

        // ED
        if ( ee.getExperimentalDesign() != null && Hibernate.isInitialized( ee.getExperimentalDesign() ) ) {
            this.experimentalDesign = ee.getExperimentalDesign().getId();
        }

        // Batch info
        batchEffect = ee.getBatchEffect();
        batchConfound = ee.getBatchConfound();

        // GEEQ: for administrators, create an admin geeq VO. Normal GEEQ VO otherwise.
        if ( ee.getGeeq() != null && Hibernate.isInitialized( ee.getGeeq() ) ) {
            geeq = SecurityUtil.isUserAdmin() ?
                    new GeeqAdminValueObject( ee.getGeeq() ) :
                    new GeeqValueObject( ee.getGeeq() );
        } else {
            geeq = null;
        }
    }

    /**
     * Creates a new {@link ExpressionExperiment} value object with additional information about ownership.
     */
    public ExpressionExperimentValueObject( ExpressionExperiment ee, AclObjectIdentity aoi, AclSid sid ) {
        this( ee );

        // ACL
        boolean[] permissions = EntityUtils.getPermissions( aoi );
        this.setIsPublic( permissions[0] );
        this.setUserCanWrite( permissions[1] );
        this.setIsShared( permissions[2] );

        if ( sid instanceof AclPrincipalSid ) {
            this.setUserOwned( Objects.equals( ( ( AclPrincipalSid ) sid ).getPrincipal(), SecurityUtil.getCurrentUsername() ) );
        } else {
            this.setUserOwned( false );
        }
    }

    protected ExpressionExperimentValueObject( ExpressionExperimentValueObject vo ) {
        super( vo );
        this.name = vo.name;
        this.description = vo.description;
        this.numberOfBioAssays = vo.numberOfBioAssays;
        this.accession = vo.getAccession();
        this.batchConfound = vo.getBatchConfound();
        this.batchEffect = vo.getBatchEffect();
        this.externalDatabase = vo.getExternalDatabase();
        this.externalUri = vo.getExternalUri();
        this.metadata = vo.getMetadata();
        this.shortName = vo.getShortName();
        this.source = vo.getSource();
        this.taxon = vo.getTaxon();
        this.technologyType = vo.getTechnologyType();
        this.taxonId = vo.getTaxonId();
        this.experimentalDesign = vo.getExperimentalDesign();
        this.processedExpressionVectorCount = vo.getProcessedExpressionVectorCount();
        this.arrayDesignCount = vo.getArrayDesignCount();
        this.bioMaterialCount = vo.getBioMaterialCount();
        this.currentUserHasWritePermission = vo.getCurrentUserHasWritePermission();
        this.currentUserIsOwner = vo.getCurrentUserIsOwner();
        this.isPublic = vo.getIsPublic();
        this.isShared = vo.getIsShared();
        this.geeq = vo.getGeeq();
        this.suitableForDEA = vo.getSuitableForDEA();
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

    @Override
    @JsonIgnore
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    @JsonIgnore
    public boolean getIsShared() {
        return this.isShared;
    }

    @Override
    @JsonIgnore
    public Class<? extends Securable> getSecurableClass() {
        return ExpressionExperiment.class;
    }

    @Override
    @JsonIgnore
    public boolean getUserCanWrite() {
        if ( this.currentUserHasWritePermission == null )
            return false;
        return this.currentUserHasWritePermission;
    }

    @Override
    @JsonIgnore
    public boolean getUserOwned() {
        if ( this.currentUserIsOwner == null )
            return false;
        return this.currentUserIsOwner;
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
        this.currentUserHasWritePermission = userCanWrite;
    }

    @Override
    public void setUserOwned( boolean isUserOwned ) {
        this.currentUserIsOwner = isUserOwned;
    }

    @Override
    public String toString() {
        return this.shortName + " (id = " + this.getId() + ")";
    }
}