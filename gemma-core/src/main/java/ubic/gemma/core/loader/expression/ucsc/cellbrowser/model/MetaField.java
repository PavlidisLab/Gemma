package ubic.gemma.core.loader.expression.ucsc.cellbrowser.model;

import lombok.Data;

import java.util.List;

@Data
public class MetaField {
    String name;
    String label;
    MetaFieldType type;
    String arrType;
    int maxSize;
    int diffValCount;
    String md5;
}
