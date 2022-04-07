package ubic.gemma.model.expression.experiment;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.acl.domain.AclSid;
import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import gemma.gsec.util.SecurityUtil;
import org.hibernate.Hibernate;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Objects;

@SuppressWarnings({ "unused", "WeakerAccess" }) // used in front end
public class ExpressionExperimentValueObject extends AbstractCuratableValueObject<ExpressionExperiment>
        implements SecureValueObject {

    private static final long serialVersionUID = -6861385216096602508L;
    protected Integer bioAssayCount;
    protected String description;
    protected String name;

    private String accession;
    private Integer arrayDesignCount;
    private String batchConfound;
    private String batchEffect;
    private Integer bioMaterialCount;
    private Boolean currentUserHasWritePermission = null;
    private Boolean currentUserIsOwner = null;
    private Long experimentalDesign;
    private String externalDatabase;
    private String externalUri;
    private GeeqValueObject geeq;
    private Boolean isPublic = false;
    private Boolean isShared = false;
    private String metadata;
    private Integer processedExpressionVectorCount;
    private String shortName;
    private String source;
    private Boolean suitableForDEA = true;
    private String taxon;
    private Long taxonId;

    private String technologyType;

    public ExpressionExperimentValueObject( Long id, String shortName, String name ) {
        super( id );
        this.shortName = shortName;
        this.name = name;
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
            this.bioAssayCount = ee.getBioAssays().size();
            // this is available too because AD are eagerly fetched
            this.arrayDesignCount = ( int ) ee.getBioAssays().stream().map( BioAssay::getArrayDesignUsed ).count();
        } else {
            // this is a denormalization, so we merely use it as a fallback if bioAssays are not initialized
            this.bioAssayCount = ee.getNumberOfSamples();
            this.arrayDesignCount = null; // the number of AD is unknown, unfortunately
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
        this.bioAssayCount = vo.bioAssayCount;
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

    @Deprecated
    public ExpressionExperimentValueObject( Long id ) {
        super( id );
    }

    public String getAccession() {
        return accession;
    }

    public Integer getArrayDesignCount() {
        return arrayDesignCount;
    }

    public String getBatchConfound() {
        return batchConfound;
    }

    public String getBatchEffect() {
        return batchEffect;
    }

    public Integer getBioAssayCount() {
        return bioAssayCount;
    }

    public Integer getBioMaterialCount() {
        return bioMaterialCount;
    }

    public Boolean getCurrentUserHasWritePermission() {
        return currentUserHasWritePermission;
    }

    public Boolean getCurrentUserIsOwner() {
        return currentUserIsOwner;
    }

    public String getDescription() {
        return description;
    }

    public Long getExperimentalDesign() {
        return experimentalDesign;
    }

    public String getExternalDatabase() {
        return externalDatabase;
    }

    public String getExternalUri() {
        return externalUri;
    }

    public GeeqValueObject getGeeq() {
        return geeq;
    }

    @Override
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    public boolean getIsShared() {
        return this.isShared;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getName() {
        return name;
    }

    public Integer getProcessedExpressionVectorCount() {
        return processedExpressionVectorCount;
    }

    @Override
    public Class<? extends Securable> getSecurableClass() {
        return ExpressionExperiment.class;
    }

    public String getShortName() {
        return shortName;
    }

    public String getSource() {
        return source;
    }

    public Boolean getSuitableForDEA() {
        return suitableForDEA;
    }

    public String getTaxon() {
        return taxon;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public String getTechnologyType() {
        return technologyType;
    }

    @Override
    public boolean getUserCanWrite() {
        if ( this.currentUserHasWritePermission == null )
            return false;
        return this.currentUserHasWritePermission;
    }

    @Override
    public boolean getUserOwned() {
        if ( this.currentUserIsOwner == null )
            return false;
        return this.currentUserIsOwner;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public void setArrayDesignCount( Integer arrayDesignCount ) {
        this.arrayDesignCount = arrayDesignCount;
    }

    public void setBatchConfound( String batchConfound ) {
        this.batchConfound = batchConfound;
    }

    public void setBatchEffect( String batchEffect ) {
        this.batchEffect = batchEffect;
    }

    public void setBioAssayCount( Integer bioAssayCount ) {
        this.bioAssayCount = bioAssayCount;
    }

    public void setBioMaterialCount( Integer bioMaterialCount ) {
        this.bioMaterialCount = bioMaterialCount;
    }

    public void setCurrentUserHasWritePermission( Boolean currentUserHasWritePermission ) {
        this.currentUserHasWritePermission = currentUserHasWritePermission;
    }

    public void setCurrentUserIsOwner( Boolean currentUserIsOwner ) {
        this.currentUserIsOwner = currentUserIsOwner;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setExperimentalDesign( Long experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    public void setExternalDatabase( String externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public void setExternalUri( String externalUri ) {
        this.externalUri = externalUri;
    }

    public void setGeeq( GeeqValueObject geeq ) {
        this.geeq = geeq;
    }

    @Override
    public void setIsPublic( boolean b ) {
        this.isPublic = b;
    }

    @Override
    public void setIsShared( boolean b ) {
        this.isShared = b;
    }

    public void setMetadata( String metadata ) {
        this.metadata = metadata;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setProcessedExpressionVectorCount( Integer processedExpressionVectorCount ) {
        this.processedExpressionVectorCount = processedExpressionVectorCount;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSource( String source ) {
        this.source = source;
    }

    public void setSuitableForDEA( Boolean suitableForDEA ) {
        this.suitableForDEA = suitableForDEA;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public void setTechnologyType( String technologyType ) {
        this.technologyType = technologyType;
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