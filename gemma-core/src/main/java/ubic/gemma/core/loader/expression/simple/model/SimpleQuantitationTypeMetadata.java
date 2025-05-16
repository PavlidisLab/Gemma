package ubic.gemma.core.loader.expression.simple.model;

import lombok.Data;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;

import javax.annotation.Nullable;
import java.io.Serializable;

@Data
public class SimpleQuantitationTypeMetadata implements Serializable {
    private String name;
    @Nullable
    private String description;
    private GeneralType generalType = GeneralType.QUANTITATIVE;
    private StandardQuantitationType type = StandardQuantitationType.AMOUNT;
    private ScaleType scale = ScaleType.LINEAR;
    private PrimitiveType representation = PrimitiveType.DOUBLE;
    private Boolean isBatchCorrected = Boolean.FALSE;
    private Boolean isPreferred = Boolean.FALSE;
    private Boolean isRatio = Boolean.FALSE;
}
