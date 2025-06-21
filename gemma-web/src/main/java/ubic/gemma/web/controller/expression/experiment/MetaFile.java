package ubic.gemma.web.controller.expression.experiment;

/**
 * Wrapper for metadata file information for the frontend
 */
@SuppressWarnings("unused")
// used in frontend
public class MetaFile {
    private final int typeId;
    private final String displayName;

    public MetaFile( int typeId, String displayName ) {
        this.typeId = typeId;
        this.displayName = displayName;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
