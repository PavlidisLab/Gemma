package ubic.gemma.model.expression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.MetadataType;

import java.sql.Blob;

/**
 * Metadata associated to an {@link ExpressionExperiment} or {@link BioAssay}.
 * @author poirigui
 */
@Data
@EqualsAndHashCode(of = { "id" })
public class AdditionalMetadata implements Describable {

    private Long id;
    private String name;
    private String description;
    private MetadataType type;
    private Blob contents;
    private String mediaType;
}
