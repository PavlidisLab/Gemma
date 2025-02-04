package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@Setter
public class ExpressionExperimentSubsetValueObject extends IdentifiableValueObject<ExpressionExperimentSubSet> implements BioAssaySetValueObject {

    private Long sourceExperiment;
    private String sourceExperimentShortName;

    private String name;
    private String description;
    private String accession;
    private Integer numberOfBioAssays;
    private Collection<CharacteristicValueObject> characteristics;

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
        super( ees.getId() );
        this.sourceExperiment = ees.getSourceExperiment().getId();
        if ( Hibernate.isInitialized( ees.getSourceExperiment() ) ) {
            this.sourceExperimentShortName = ees.getSourceExperiment().getShortName();
        }
        this.numberOfBioAssays = ees.getBioAssays() != null ? ees.getBioAssays().size() : null;
        this.name = ees.getName();
        this.description = ees.getDescription();
        if ( ees.getAccession() != null && Hibernate.isInitialized( ees.getAccession() ) ) {
            this.accession = ees.getAccession().getAccession();
        }
        if ( Hibernate.isInitialized( ees.getBioAssays() ) ) {
            this.numberOfBioAssays = ees.getBioAssays().size();
        }
        if ( Hibernate.isInitialized( ees.getCharacteristics() ) ) {
            characteristics = ees.getCharacteristics().stream()
                    .map( CharacteristicValueObject::new )
                    .collect( Collectors.toSet() );
        }
    }

    @Override
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
