package ubic.gemma.model.expression.experiment;

import gemma.gsec.model.SecureValueObject;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;

/**
 * Interface for VOs deriving from {@link BioAssaySet}.
 * @author poirigui
 */
public interface BioAssaySetValueObject extends Describable, SecureValueObject, Serializable {

    /**
     * Obtain the accession of this set if one exists.
     */
    @Nullable
    String getAccession();

    /**
     * Obtain the number of assays in this set.
     */
    Integer getNumberOfBioAssays();

    Collection<CharacteristicValueObject> getCharacteristics();
}
