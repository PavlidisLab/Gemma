package ubic.gemma.model.expression.experiment;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import gemma.gsec.util.SecurityUtil;
import org.hibernate.Hibernate;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Date;
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
        this.bioAssayCount = ee.getBioAssays() != null && Hibernate.isInitialized( ee.getBioAssays() ) ? ee.getBioAssays().size() : null;
        this.accession = ee.getAccession() != null && Hibernate.isInitialized( ee.getAccession() ) ? ee.getAccession().toString() : null;
        this.experimentalDesign = ee.getExperimentalDesign() != null && Hibernate.isInitialized( ee.getExperimentalDesign() )
                ? ee.getExperimentalDesign().getId()
                : null;
    }

    public ExpressionExperimentValueObject( Long id, String shortName, String name ) {
        this.id = id;
        this.shortName = shortName;
        this.name = name;
    }

    public ExpressionExperimentValueObject( Long id, String name, String description, Integer bioAssayCount,
            String accession, String batchConfound, String batchEffect, String externalDatabase, String externalUri,
            String metadata, String shortName, String source, String taxon, String technologyType, Long taxonId,
            Long experimentalDesign, Integer processedExpressionVectorCount, Integer arrayDesignCount,
            Integer bioMaterialCount, Boolean currentUserHasWritePermission, Boolean currentUserIsOwner,
            Boolean isPublic, Boolean isShared, Date lastUpdated, Boolean troubled,
            AuditEventValueObject lastTroubledEvent, Boolean needsAttention,
            AuditEventValueObject lastNeedsAttentionEvent, String curationNote,
            AuditEventValueObject lastNoteUpdateEvent, GeeqValueObject geeqValueObject, Boolean suitableForDEA ) {
        super( id, lastUpdated, troubled, lastTroubledEvent, needsAttention, lastNeedsAttentionEvent, curationNote,
                lastNoteUpdateEvent );
        this.name = name;
        this.description = description;
        this.bioAssayCount = bioAssayCount;
        this.accession = accession;
        this.batchConfound = batchConfound;
        this.batchEffect = batchEffect;
        this.externalDatabase = externalDatabase;
        this.externalUri = externalUri;
        this.metadata = metadata;
        this.shortName = shortName;
        this.source = source;
        this.taxon = taxon;
        this.technologyType = technologyType;
        this.taxonId = taxonId;
        this.experimentalDesign = experimentalDesign;
        this.processedExpressionVectorCount = processedExpressionVectorCount;
        this.arrayDesignCount = arrayDesignCount;
        this.bioMaterialCount = bioMaterialCount;
        this.currentUserHasWritePermission = currentUserHasWritePermission;
        this.currentUserIsOwner = currentUserIsOwner;
        this.isPublic = isPublic;
        this.isShared = isShared;
        this.geeq = geeqValueObject;
        this.suitableForDEA = suitableForDEA;

    }

    /**
     * Constructor for creating an EEVO from a database row retrieved in the DAO
     *
     * @param row          the database row to read the VO parameters from
     * @param totalInBatch the number indicating how many VOs have been returned in the database query along with this
     *                     one.
     */
    public ExpressionExperimentValueObject( Object[] row, Integer totalInBatch ) {
        super( ( Long ) row[0], ( Date ) row[12], ( Boolean ) row[13], ( AuditEvent ) row[24], ( Boolean ) row[14],
                ( AuditEvent ) row[23], ( String ) row[15], ( AuditEvent ) row[22], totalInBatch );

        // EE
        this.name = ( String ) row[1];
        this.source = ( String ) row[2];
        this.shortName = ( String ) row[3];
        this.metadata = ( String ) row[4];
        this.processedExpressionVectorCount = row[5] == null ? 0 : ( Integer ) row[5];
        this.accession = ( String ) row[6];

        // ED
        this.externalDatabase = ( String ) row[7];
        this.externalUri = ( String ) row[8];

        // Description
        this.description = ( String ) row[9];

        // AD
        Object technology = row[10];
        if ( technology != null ) {
            this.technologyType = technology.toString();
        }

        // Taxon
        this.taxon = ( String ) row[10]; // common name
        this.taxonId = ( Long ) row[11];

        // 12-15 used in call to super

        // Counts
        this.bioAssayCount = ( Integer ) row[16];
        //  this.arrayDesignCount = ( ( Long ) row[17] ).intValue();
        //  this.bioMaterialCount = ( ( Long ) row[19] ).intValue();

        // Other
        this.experimentalDesign = ( Long ) row[17];

        // ACL
        AclObjectIdentity aoi = ( AclObjectIdentity ) row[18];

        boolean[] permissions = EntityUtils.getPermissions( aoi );
        this.setIsPublic( permissions[0] );
        this.setUserCanWrite( permissions[1] );
        this.setIsShared( permissions[2] );

        if ( row[19] instanceof AclPrincipalSid ) {
            this.setUserOwned( Objects.equals( ( ( AclPrincipalSid ) row[19] ).getPrincipal(),
                    SecurityUtil.getCurrentUsername() ) );
        } else {
            this.setUserOwned( false );
        }

        // Batch info
        batchEffect = ( String ) row[20];
        batchConfound = ( String ) row[21];

        // 24-25 used in call to super.

        // Geeq: for administrators, create an admin geeq VO. Normal geeq VO otherwise.
        geeq = row[25] == null ? null
                : SecurityUtil.isUserAdmin() ? new GeeqAdminValueObject( ( Geeq ) row[25] ) : new GeeqValueObject( ( Geeq ) row[25] );

        // 29: other parts

    }

    /**
     * Required when using the class as a spring bean.
     */
    protected ExpressionExperimentValueObject() {
    }

    protected ExpressionExperimentValueObject( Long id ) {
        super( id );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( !this.getClass().isAssignableFrom( obj.getClass() ) )
            return false;
        ExpressionExperimentValueObject other = ( ExpressionExperimentValueObject ) obj;
        if ( id == null ) {
            return other.id == null;
        } else
            return id.equals( other.id );
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * ( result + ( ( id == null ) ? 0 : id.hashCode() ) );
        return result;
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