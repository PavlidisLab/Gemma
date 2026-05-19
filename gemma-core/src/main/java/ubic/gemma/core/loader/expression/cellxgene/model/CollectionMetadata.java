package ubic.gemma.core.loader.expression.cellxgene.model;

import lombok.Data;

import org.springframework.lang.Nullable;
import java.util.List;

@Data
public class CollectionMetadata {
    String id;
    String name;
    String description;
    @Nullable
    private List<DatasetMetadata> datasets;
    @Nullable
    private List<Link> links;
}
