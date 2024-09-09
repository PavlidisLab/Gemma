package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5Group;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Dataframe implements AutoCloseable {

    private final H5Group group;

    public Dataframe( H5Group group ) {
        Assert.isTrue( Objects.equals( group.getStringAttribute( "encoding-type" ), "dataframe" ),
                "The H5 group must have an 'encoding-type' attribute set to 'dataframe'." );
        Assert.isTrue( group.hasAttribute( "encoding-version" ), "A dataframe must have an 'encoding-version' attribute." );
        Assert.isTrue( group.hasAttribute( "_index" ), "A dataframe must have a '_index' attribute." );
        Assert.isTrue( group.hasAttribute( "column-order" ), "A dataframe must have a 'column-order' attribute." );
        this.group = group;
    }

    /**
     * Obtain the name of the column used as an index.
     */
    public String getIndexColumn() {
        return group.getStringAttribute( "_index" );
    }

    /**
     * Obtain all the columns defined in the dataframe.
     */
    public List<String> getColumns() {
        return group.getChildren();
    }

    /**
     * Obtain the encoding type of a column.
     * <p>
     * This is usually one of {@code string-array}, {@code categorical} or {@code array}, but many other encodings are
     * possible.
     */
    public String getColumnType( String columnName ) {
        Assert.isTrue( group.exists( columnName ), "There is no column named " + columnName + ". Possible columns are: " + String.join( ", ", getColumns() ) + "." );
        return group.getStringAttribute( columnName, "encoding-type" );
    }

    public H5Dataset getArrayColumn( String columnName ) {
        checkIfColumnExists( columnName, "array" );
        Assert.isTrue( Objects.equals( group.getStringAttribute( columnName, "encoding-type" ), "array" ),
                "The column " + columnName + " is not an array." );
        return group.getDataset( columnName );
    }

    /**
     * Obtain a categorical column.
     * Load the values of a categorical column from a dataframe.
     */
    public CategoricalArray getCategoricalColumn( String columnName ) {
        checkIfColumnExists( columnName, "categorical" );
        Assert.isTrue( Objects.equals( group.getStringAttribute( columnName, "encoding-type" ), "categorical" ),
                "The column " + columnName + " is not categorical." );
        try ( H5Group dataset = group.getGroup( columnName ) ) {
            return new CategoricalArray( dataset );
        }
    }

    /**
     * Obtain a string-array column.
     */
    public String[] getStringArrayColumn( String columnName ) {
        checkIfColumnExists( columnName, "string-array" );
        Assert.isTrue( Objects.equals( group.getStringAttribute( columnName, "encoding-type" ), "string-array" ),
                "The column " + columnName + " is not a string array." );
        return group.getDataset( columnName ).toStringVector();
    }

    private void checkIfColumnExists( String columnName, String columnType ) {
        if ( !group.exists( columnName ) ) {
            String possibleColumns = getColumns().stream()
                    .filter( c -> columnType.equals( getColumnType( c ) ) )
                    .collect( Collectors.joining( ", " ) );
            throw new IllegalArgumentException( String.format( "There is no %s column named %s. Possible columns are: %s.",
                    columnType, columnName, possibleColumns ) );
        }
    }

    @Override
    public void close() {
        group.close();
    }
}
