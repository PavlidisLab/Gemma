package ubic.gemma.core.loader.util.anndata;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Represents an AnnData dataframe.
 * @param <K> the type of index being used
 * @author poirigui
 */
@CommonsLog
public class Dataframe<K> implements Iterable<Dataframe.Column<K, ?>>, AutoCloseable {

    /**
     * Lits of supported encoding types.
     */
    private static final String[] SUPPORTED_ENCODING_TYPE = { "categorical", "string-array", "array", "nullable-integer", "nullable-boolean" };

    private final H5Group group;
    @Nullable
    private final Class<K> indexClass;

    private IndexedColumn index;

    /**
     * Mapping of cached columns.
     * <p>
     * It's important to keep track of the class of the column because some column (i.e. enum) can use different
     * representation.
     */
    private final Map<ColumnKey, Column<K, ?>> cachedColumns = new ConcurrentHashMap<>();

    @Value(staticConstructor = "of")
    private static class ColumnKey {
        String columnName;
        Class<?> desiredRepresentation;
    }

    /**
     * Create a new dataframe from an H5 group.
     * @param group      an H5 group that contains a dataset
     * @param indexClass the type of index to use, or null to ignore. If left unset, indexing will not be possible via
     *                   {@link Column#get(Object)}.
     */
    public Dataframe( H5Group group, @Nullable Class<K> indexClass ) {
        Assert.isTrue( Objects.equals( group.getStringAttribute( "encoding-type" ), "dataframe" ),
                "The H5 group must have an 'encoding-type' attribute set to 'dataframe'." );
        Assert.isTrue( group.hasAttribute( "encoding-version" ), "A dataframe must have an 'encoding-version' attribute." );
        Assert.isTrue( group.hasAttribute( "_index" ), "A dataframe must have a '_index' attribute." );
        Assert.isTrue( group.hasAttribute( "column-order" ), "A dataframe must have a 'column-order' attribute." );
        this.group = group;
        this.indexClass = indexClass;
    }

    /**
     * Obtain the name of the column used as an index.
     */
    public String getIndexColumn() {
        return group.getStringAttribute( "_index" );
    }

    /**
     * Obtain all the columns defined in the dataframe.
     * <p>
     * Unsupported encoding types are ignored.
     */
    public List<String> getColumns() {
        return group.getChildren().stream()
                .filter( columnName -> {
                    String encodingType = group.getStringAttribute( columnName, "encoding-type" );
                    if ( ArrayUtils.contains( SUPPORTED_ENCODING_TYPE, encodingType ) ) {
                        return true;
                    } else if ( encodingType == null ) {
                        log.warn( "Ignoring invalid H5 group '" + columnName + "' lacking an 'encoding-type' attribute." );
                        return false;
                    } else {
                        log.warn( "Ignoring unknown encoding type '" + encodingType + "' for group '" + columnName + "'." );
                        return false;
                    }
                } )
                .collect( Collectors.toList() );
    }

    /**
     * Obtain the encoding type of a column.
     * <p>
     * This is usually one of {@code string-array}, {@code categorical} or {@code array}, but many other encodings are
     * possible.
     */
    public String getColumnEncodingType( String columnName ) {
        checkIfColumnExists( columnName, null );
        return requireNonNull( group.getStringAttribute( columnName, "encoding-type" ),
                "Column '" + columnName + "' does not have an 'encoding-type' attribute." );
    }

    /**
     * Obtain the index column.
     */
    public Column<K, K> getIndex() {
        Assert.notNull( indexClass, "No index type is specified, cannot create the index." );
        if ( index == null ) {
            index = new IndexedColumn( getColumn( getIndexColumn(), indexClass ) );
        }
        return index;
    }

    /**
     * Guess the type of a given column.
     */
    public Class<?> getColumnType( String columnName ) {
        checkIfColumnExists( columnName, null );
        switch ( getColumnEncodingType( columnName ) ) {
            case "categorical":
                return guessCategoricalType( columnName );
            case "string-array":
                return String.class;
            case "array":
                try ( H5Dataset ac = getArrayColumn( columnName ) ) {
                    switch ( ac.getType().getFundamentalType() ) {
                        case INTEGER:
                        case ENUM:
                            return Integer.class;
                        case FLOAT:
                            return Double.class;
                        case STRING:
                            return String.class;
                        default:
                            throw new UnsupportedOperationException( "Unsupported array type " + ac.getType() + " for column " + columnName + "." );
                    }
                }
            case "nullable-integer":
                return Integer.class;
            case "nullable-boolean":
                return Boolean.class;
            default:
                throw new UnsupportedOperationException( "Unsupported column type " + getColumnEncodingType( columnName ) + " for column " + columnName + "." );
        }
    }

    /**
     * Obtain a column, the element type is guessed as per {@link #getColumnType(String)}
     * @see #getColumnType(String)
     * @see #getColumn(String, Class)
     */
    public Column<K, ?> getColumn( String columnName ) {
        checkIfColumnExists( columnName, null );
        return getColumn( columnName, getColumnType( columnName ) );
    }

    /**
     * Obtain a column of the given element type.
     */
    public <T> Column<K, T> getColumn( String columnName, Class<T> clazz ) {
        checkIfColumnExists( columnName, null );
        //noinspection unchecked
        return ( Column<K, T> ) cachedColumns.computeIfAbsent( ColumnKey.of( columnName, clazz ), k -> getColumnInternal( columnName, clazz ) );
    }

    private <T> Column<K, T> getColumnInternal( String columnName, Class<T> clazz ) {
        Column<K, T> column;
        switch ( getColumnEncodingType( columnName ) ) {
            case "categorical":
                column = new CategoricalColumn<>( columnName, getCategoricalColumn( columnName, clazz ) );
                break;
            case "string-array":
                Assert.isTrue( String.class.isAssignableFrom( clazz ) );
                //noinspection unchecked
                column = ( Column<K, T> ) new ArrayColumn<>( columnName, getStringArrayColumn( columnName ) );
                break;
            case "array":
                if ( Boolean.class.isAssignableFrom( clazz ) ) {
                    //noinspection unchecked
                    column = ( Column<K, T> ) new BooleanArrayColumn( columnName, getArrayColumn( columnName ).toBooleanVector() );
                } else if ( Integer.class.isAssignableFrom( clazz ) ) {
                    //noinspection unchecked
                    column = ( Column<K, T> ) new IntArrayColumn( columnName, getArrayColumn( columnName ).toIntegerVector() );
                } else if ( Double.class.isAssignableFrom( clazz ) ) {
                    //noinspection unchecked
                    column = ( Column<K, T> ) new DoubleArrayColumn( columnName, getArrayColumn( columnName ).toDoubleVector() );
                } else if ( String.class.isAssignableFrom( clazz ) ) {
                    //noinspection unchecked
                    column = ( Column<K, T> ) new ArrayColumn<>( columnName, getArrayColumn( columnName ).toStringVector() );
                } else {
                    throw new UnsupportedOperationException( "Unsupported scalar type for array column:  " + clazz.getName() );
                }
                break;
            case "nullable-integer":
                Assert.isTrue( Integer.class.isAssignableFrom( clazz ) );
                //noinspection unchecked
                column = new ArrayColumn<>( columnName, ( T[] ) getNullableIntegerArrayColumn( columnName ) );
                break;
            case "nullable-boolean":
                Assert.isTrue( Boolean.class.isAssignableFrom( clazz ) );
                //noinspection unchecked
                column = new ArrayColumn<>( columnName, ( T[] ) getNullableBooleanArrayColumn( columnName ) );
                break;
            default:
                throw new UnsupportedOperationException( "Unsupported column encoding type: " + getColumnEncodingType( columnName ) );
        }
        return column;
    }

    /**
     * Obtain an array column as a H5 dataset.
     */
    public H5Dataset getArrayColumn( String columnName ) {
        checkIfColumnExists( columnName, "array" );
        Assert.isTrue( Objects.equals( group.getStringAttribute( columnName, "encoding-type" ), "array" ),
                "The column " + columnName + " is not an array." );
        return group.getDataset( columnName );
    }

    /**
     * Obtain a categorical column.
     */
    public <T> CategoricalArray<T> getCategoricalColumn( String columnName, Class<T> categoryType ) {
        checkIfColumnExists( columnName, "categorical" );
        Assert.isTrue( Objects.equals( group.getStringAttribute( columnName, "encoding-type" ), "categorical" ),
                "The column " + columnName + " is not categorical." );
        return new CategoricalArray<>( group.getGroup( columnName ), categoryType );
    }

    /**
     * Obtain a categorical column.
     */
    public CategoricalArray<?> getCategoricalColumn( String columnName ) {
        checkIfColumnExists( columnName, "categorical" );
        Assert.isTrue( Objects.equals( group.getStringAttribute( columnName, "encoding-type" ), "categorical" ),
                "The column " + columnName + " is not categorical." );
        return new CategoricalArray<>( group.getGroup( columnName ), guessCategoricalType( columnName ) );
    }

    private Class<?> guessCategoricalType( String columnName ) {
        try ( H5Dataset g = group.getDataset( columnName + "/categories" ) ) {
            switch ( g.getType().getFundamentalType() ) {
                case INTEGER:
                case ENUM:
                    return Integer.class;
                case FLOAT:
                    return Double.class;
                case STRING:
                    return String.class;
                default:
                    throw new UnsupportedOperationException( "Unsupported array type " + g.getType() + " for categorical column " + columnName + "." );
            }
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

    /**
     * Obtain a nullable integer array column.
     */
    public Integer[] getNullableIntegerArrayColumn( String columnName ) {
        checkIfColumnExists( columnName, "nullable-integer" );
        return new NullableIntArray( group.getGroup( columnName ) ).toIntegerArray();
    }

    /**
     * Obtain a nullable boolean array column.
     */
    public Boolean[] getNullableBooleanArrayColumn( String columnName ) {
        checkIfColumnExists( columnName, "nullable-boolean" );
        return new NullableBoolArray( group.getGroup( columnName ) ).toBooleanArray();
    }

    private void checkIfColumnExists( String columnName, @Nullable String columnType ) {
        if ( !group.exists( columnName ) ) {
            String possibleColumns = getColumns().stream()
                    .filter( c -> columnType == null || columnType.equals( getColumnEncodingType( c ) ) )
                    .collect( Collectors.joining( ", " ) );
            throw new IllegalArgumentException( String.format( "There is no %s named %s. Possible columns are: %s.",
                    columnType != null ? columnType + " column" : "column", columnName, possibleColumns ) );
        }
    }

    @Override
    @Nonnull
    public Iterator<Column<K, ?>> iterator() {
        List<String> columnNames = getColumns();
        return new Iterator<Column<K, ?>>() {

            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < columnNames.size();
            }

            @Override
            public Column<K, ?> next() {
                return getColumn( columnNames.get( i++ ) );
            }
        };
    }

    @Override
    public void close() {
        cachedColumns.forEach( ( name, col ) -> {
            if ( col instanceof AutoCloseable ) {
                try {
                    ( ( AutoCloseable ) col ).close();
                } catch ( Exception e ) {
                    log.error( "Error while closing column " + name + " from " + this, e );
                }
            }
        } );
        cachedColumns.clear();
        group.close();
    }

    public interface Column<K, T> extends Iterable<T> {

        T get( int i ) throws IndexOutOfBoundsException;

        boolean getBool( int i ) throws IndexOutOfBoundsException;

        int getInt( int i ) throws IndexOutOfBoundsException;

        double getDouble( int i ) throws IndexOutOfBoundsException;

        T get( K key );

        boolean getBool( K key );

        int getInt( K key );

        double getDouble( K key );

        int size();

        int indexOf( T element );

        Set<T> uniqueValues();
    }

    private abstract class AbstractColumn<T> implements Column<K, T> {

        private final String name;

        protected AbstractColumn( String name ) {
            this.name = name;
        }

        @Override
        public boolean getBool( int i ) {
            return ( Boolean ) get( i );
        }

        @Override
        public int getInt( int i ) {
            return ( Integer ) get( i );
        }

        @Override
        public double getDouble( int i ) {
            return ( Double ) get( i );
        }

        @Override
        public T get( K key ) {
            int i = getIndex().indexOf( key );
            if ( i == -1 ) {
                throw new NoSuchElementException( "No entry with key " + key + " found in index." );
            }
            return get( i );
        }

        @Override
        public boolean getBool( K key ) {
            int i = getIndex().indexOf( key );
            if ( i == -1 ) {
                throw new NoSuchElementException( "No entry with key " + key + " found in index." );
            }
            return getBool( i );
        }

        @Override
        public int getInt( K key ) {
            int i = getIndex().indexOf( key );
            if ( i == -1 ) {
                throw new NoSuchElementException( "No entry with key " + key + " found in index." );
            }
            return getInt( i );
        }

        @Override
        public double getDouble( K key ) {
            int i = getIndex().indexOf( key );
            if ( i == -1 ) {
                throw new NoSuchElementException( "No entry with key " + key + " found in index." );
            }
            return getDouble( i );
        }

        @Override
        @Nonnull
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < size();
                }

                @Override
                public T next() {
                    try {
                        return get( i++ );
                    } catch ( IndexOutOfBoundsException e ) {
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class CategoricalColumn<T> extends AbstractColumn<T> implements AutoCloseable {

        private final CategoricalArray<T> c;

        public CategoricalColumn( String name, CategoricalArray<T> c ) {
            super( name );
            this.c = c;
        }

        @Override
        public T get( int i ) {
            return c.get( i );
        }

        @Override
        public int size() {
            return c.size();
        }

        @Override
        public int indexOf( T element ) {
            int i = ArrayUtils.indexOf( c.getCategories(), element );
            return i != -1 ? ArrayUtils.indexOf( c.getCodes(), i ) : -1;
        }

        @Override
        public Set<T> uniqueValues() {
            return new HashSet<>( Arrays.asList( c.getCategories() ) );
        }

        @Override
        public void close() {
            c.close();
        }

        @Override
        public String toString() {
            return super.toString() + " " + c;
        }
    }

    private class ArrayColumn<T> extends AbstractColumn<T> {

        private final T[] arr;

        private ArrayColumn( String name, T[] arr ) {
            super( name );
            this.arr = arr;
        }

        @Override
        public T get( int i ) {
            return arr[i];
        }

        @Override
        public int size() {
            return arr.length;
        }

        @Override
        public int indexOf( T element ) {
            return ArrayUtils.indexOf( arr, element );
        }

        @Override
        @Nonnull
        public Iterator<T> iterator() {
            return new ArrayIterator<>( arr );
        }

        @Override
        public Set<T> uniqueValues() {
            return new HashSet<>( Arrays.asList( arr ) );
        }

        @Override
        public String toString() {
            return super.toString() + " " + arr.length;
        }
    }

    private class BooleanArrayColumn extends AbstractColumn<Boolean> {

        private final Set<Boolean>
                ONLY_TRUE = new HashSet<>( Collections.singletonList( true ) ),
                ONLY_FALSE = new HashSet<>( Collections.singletonList( false ) ),
                BOTH = new HashSet<>( Arrays.asList( true, false ) );

        private final boolean[] arr;

        private BooleanArrayColumn( String name, boolean[] arr ) {
            super( name );
            this.arr = arr;
        }

        @Override
        public Boolean get( int i ) {
            return arr[i];
        }

        @Override
        public boolean getBool( int i ) {
            return arr[i];
        }

        @Override
        public int indexOf( Boolean element ) {
            return ArrayUtils.indexOf( arr, element );
        }

        @Override
        public int size() {
            return arr.length;
        }

        @Override
        @Nonnull
        public Iterator<Boolean> iterator() {
            return new ArrayIterator<>( arr );
        }

        @Override
        public Set<Boolean> uniqueValues() {
            boolean hasTrue = false, hasFalse = false;
            for ( boolean b : arr ) {
                hasTrue |= b;
                hasFalse |= !b;
                if ( hasTrue && hasFalse ) {
                    break;
                }
            }
            if ( hasTrue && hasFalse ) {
                return BOTH;
            } else if ( hasTrue ) {
                return ONLY_TRUE;
            } else if ( hasFalse ) {
                return ONLY_FALSE;
            } else {
                return Collections.emptySet();
            }
        }

        @Override
        public String toString() {
            return super.toString() + " " + arr.length + " booleans";
        }
    }

    private class IntArrayColumn extends AbstractColumn<Integer> {

        private final int[] arr;

        private IntArrayColumn( String name, int[] arr ) {
            super( name );
            this.arr = arr;
        }

        @Override
        public Integer get( int i ) {
            return arr[i];
        }

        @Override
        public int getInt( int i ) {
            return arr[i];
        }

        @Override
        public double getDouble( int i ) {
            return arr[i];
        }

        @Override
        public int indexOf( Integer element ) {
            return ArrayUtils.indexOf( arr, element );
        }

        @Override
        public int size() {
            return arr.length;
        }

        @Override
        public Set<Integer> uniqueValues() {
            return IntStream.of( arr ).boxed().collect( Collectors.toSet() );
        }

        @Override
        @Nonnull
        public Iterator<Integer> iterator() {
            return new ArrayIterator<>( arr );
        }

        @Override
        public String toString() {
            return super.toString() + " " + arr.length + " integers";
        }
    }

    private class DoubleArrayColumn extends AbstractColumn<Double> {

        private final double[] arr;

        private DoubleArrayColumn( String name, double[] arr ) {
            super( name );
            this.arr = arr;
        }

        @Override
        public Double get( int i ) {
            return arr[i];
        }

        @Override
        public int getInt( int i ) {
            return ( int ) arr[i];
        }

        @Override
        public double getDouble( int i ) {
            return arr[i];
        }

        @Override
        public int size() {
            return arr.length;
        }

        @Override
        public int indexOf( Double element ) {
            return ArrayUtils.indexOf( arr, element );
        }

        @Override
        @Nonnull
        public Iterator<Double> iterator() {
            return new ArrayIterator<>( arr );
        }

        @Override
        public Set<Double> uniqueValues() {
            return DoubleStream.of( arr ).boxed().collect( Collectors.toSet() );
        }

        @Override
        public String toString() {
            return super.toString() + " " + arr.length + " doubles";
        }
    }

    private class IndexedColumn implements Column<K, K> {

        private final Column<K, K> column;
        private final Map<K, Integer> index;

        public IndexedColumn( Column<K, K> column ) {
            this.column = column;
            this.index = new HashMap<>();
            for ( int i = 0; i < column.size(); i++ ) {
                index.putIfAbsent( column.get( i ), i );
            }
        }

        @Override
        public K get( int i ) {
            return column.get( i );
        }

        @Override
        public boolean getBool( int i ) {
            return column.getBool( i );
        }

        @Override
        public int getInt( int i ) {
            return column.getInt( i );
        }

        @Override
        public double getDouble( int i ) {
            return column.getDouble( i );
        }

        @Override
        public K get( K key ) {
            Integer i = index.get( key );
            if ( i == null ) {
                throw new NoSuchElementException( "No entry with key " + key + " found in index." );
            }
            return column.get( i );
        }

        @Override
        public boolean getBool( K key ) {
            Integer i = index.get( key );
            if ( i == null ) {
                throw new NoSuchElementException( "No entry with key " + key + " found in index." );
            }
            return column.getBool( i );
        }

        @Override
        public int getInt( K key ) {
            Integer i = index.get( key );
            if ( i == null ) {
                throw new NoSuchElementException( "No entry with key " + key + " found in index." );
            }
            return column.getInt( i );
        }

        @Override
        public double getDouble( K key ) {
            Integer i = index.get( key );
            if ( i == null ) {
                throw new NoSuchElementException( "No entry with key " + key + " found in index." );
            }
            return column.getDouble( i );
        }

        @Override
        public int size() {
            return column.size();
        }

        @Override
        public int indexOf( K element ) {
            Integer i = index.get( element );
            return i != null ? i : -1;
        }

        @Override
        @Nonnull
        public Iterator<K> iterator() {
            return column.iterator();
        }

        @Override
        public Set<K> uniqueValues() {
            return index.keySet();
        }

        @Override
        public String toString() {
            return column.toString() + " [indexed]";
        }
    }
}
