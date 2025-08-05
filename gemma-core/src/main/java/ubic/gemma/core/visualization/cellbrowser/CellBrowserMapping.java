package ubic.gemma.core.visualization.cellbrowser;

import lombok.Value;

/**
 * Represents a mapping between a Cell Browser metadata column and a Gemma entity.
 * @author poirigui
 */
@Value
public class CellBrowserMapping {
    /**
     * Type of mapping.
     */
    CellBrowserMappingType type;
    /**
     * Gemma identifier for entity used to create the metadata column.
     */
    Long identifier;
    /**
     * Identifier used by Cell Browser to refer to the metadata column (i.e. the one for the {@code meta} query
     * parameter).
     */
    String metaColumnId;
    /**
     * Label for the metadata column, used in the Cell Browser UI.
     */
    String metaColumnName;
}
