package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.util.ModelUtils;

import org.springframework.lang.Nullable;
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
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "nothing populates it: no constructor assigns it, no setter call site exists, there is no MIN_PVALUE column, and no HQL projection or alias transformer reaches it — so it would serialize a permanent null")
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
    public ExpressionExperimentSubsetValueObject( ExpressionExperimentSubSet ees, @Nullable Map<ArrayDesign, ArrayDesignValueObject> arrayDesignValueObjectsById, @Nullable Map<BioAssay, BioAssay> bioAssay2SourceBioAssayMap, boolean includeAssays, boolean basic, boolean allFactorValues ) {
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
     * Always {@code null}: a subset has no accession, and never has had one.
     * <p>
     * Present only because {@link BioAssaySetValueObject#getAccession()} declares it. The backing
     * field was removed once it was established that nothing had ever written it — no constructor
     * sets it, and no call site anywhere in the reactor invokes the setter — so the field could only
     * ever have serialized a permanent {@code null}. To get a subset's accession, read
     * {@link #getSourceExperimentId()} and fetch the source experiment.
     *
     * @deprecated Do not use, there's never been an accession field in the data model.
     */
    @Deprecated
    @Override
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "nothing ever populated it; a subset has no accession in the data model, so publishing it would serialize a permanent null that reads as data")
    public String getAccession() {
        return null;
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
