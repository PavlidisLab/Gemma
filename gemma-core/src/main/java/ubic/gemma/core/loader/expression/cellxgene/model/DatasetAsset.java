package ubic.gemma.core.loader.expression.cellxgene.model;

import lombok.Data;
import ubic.gemma.core.loader.expression.cellxgene.FileType;

import org.springframework.lang.Nullable;

@Data
public class DatasetAsset {
    String id;
    @Nullable
    FileType filetype;
}
