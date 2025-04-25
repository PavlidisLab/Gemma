package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.util.ModelUtils;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class ExpressionExperimentSubsetValueObject extends IdentifiableValueObject<ExpressionExperimentSubSet> implements BioAssaySetValueObject {

    /**
     * The ID of the {@link ExpressionExperiment} this is a subset of.
     */
    private Long sourceExperimentId;
    /**
     * The short name of the {@link ExpressionExperiment} this is a subset of.
     */
    private String sourceExperimentShortName;

    private String name;
    private String description;
    /**
     * @deprecated Do not use, there's never been an accession field in the data model.
     */
    @Deprecated
    @GemmaWebOnly
    private String accession;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer numberOfBioAssays;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<CharacteristicValueObject> characteristics;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<BioAssayValueObject> bioAssays;

    // these are populated by gsec
    @JsonIgnore
    private boolean isPublic = false;
    @JsonIgnore
    private boolean isShared = false;
    @JsonIgnore
    private boolean userCanWrite = false;
    @JsonIgnore
    private boolean userOwned = false;

    @Nullable
    @GemmaWebOnly
    private Double minPvalue;

    public ExpressionExperimentSubsetValueObject() {
        super();
    }

    public ExpressionExperimentSubsetValueObject( ExpressionExperimentSubSet ees ) {
        this( ees, null, null, false, false, false );
    }

    /**
     * @param bioAssay2SourceBioAssayMap mapping of assays to their source assays
     * @param includeAssays              whether to include assays in the serialization
     */
    public ExpressionExperimentSubsetValueObject( ExpressionExperimentSubSet ees, @Nullable Map<Long, ArrayDesignValueObject> arrayDesignValueObjectsById, @Nullable Map<BioAssay, BioAssay> bioAssay2SourceBioAssayMap, boolean includeAssays, boolean basic, boolean allFactorValues ) {
        super( ees.getId() );
        this.sourceExperimentId = ees.getSourceExperiment().getId();
        if ( ModelUtils.isInitialized( ees.getSourceExperiment() ) ) {
            this.sourceExperimentShortName = ees.getSourceExperiment().getShortName();
        }
        this.name = ees.getName();
        this.description = ees.getDescription();
        if ( ModelUtils.isInitialized( ees.getBioAssays() ) ) {
            this.numberOfBioAssays = ees.getBioAssays().size();
            if ( includeAssays ) {
                bioAssays = ees.getBioAssays().stream()
                        .map( ba -> new BioAssayValueObject( ba, arrayDesignValueObjectsById, bioAssay2SourceBioAssayMap != null ? bioAssay2SourceBioAssayMap.get( ba ) : null, basic, allFactorValues ) )
                        .collect( Collectors.toSet() );
            }
        } else {
            this.numberOfBioAssays = null;
        }
        if ( ModelUtils.isInitialized( ees.getCharacteristics() ) ) {
            characteristics = ees.getCharacteristics().stream()
                    .map( CharacteristicValueObject::new )
                    .collect( Collectors.toSet() );
        }
    }

    /**
     * @deprecated use {@link #getSourceExperimentId()} instead
     */
    @Deprecated
    @GemmaWebOnly
    public Long getSourceExperiment() {
        return sourceExperimentId;
    }

    @Override
    @JsonIgnore
    public Class<? extends Securable> getSecurableClass() {
        return ExpressionExperimentSubSet.class;
    }

    @Override
    public boolean getIsPublic() {
        return isPublic;
    }

    @Override
    public void setIsPublic( boolean b ) {
        this.isPublic = b;
    }

    @Override
    public boolean getIsShared() {
        return isShared;
    }

    @Override
    public void setIsShared( boolean b ) {
        this.isShared = b;
    }

    public boolean getUserCanWrite() {
        return userCanWrite;
    }

    @Override
    public void setUserCanWrite( boolean userCanWrite ) {
        this.userCanWrite = userCanWrite;
    }

    public boolean getUserOwned() {
        return userOwned;
    }

    @Override
    public void setUserOwned( boolean isUserOwned ) {
        this.userOwned = isUserOwned;
    }
}
