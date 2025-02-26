package ubic.gemma.core.visualization;

import lombok.Setter;
import org.jfree.chart.annotations.CategoryAnnotation;
import org.jfree.chart.annotations.CategoryPointerAnnotation;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.singleCell.SingleCellDescriptive;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.getSampleDataAsDoubles;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.getSampleStart;

/**
 * Represents a boxplot(s) of single-cell data.
 */
@Setter
public class SingleCellDataBoxplot {

    /**
     * Vector to display.
     */
    private final SingleCellExpressionDataVector vector;

    /**
     * List of assays to display.
     * <p>
     * Must be a subset of the dimension of {@link #vector}.
     */
    private List<BioAssay> bioAssays;

    /**
     * Cell-level characteristics to group single-cell data by.
     * <p>
     * One series of datapoint will be created for each value of the characteristic.
     */
    @Nullable
    private CellLevelCharacteristics cellLevelCharacteristics;

    /**
     * Cell-level characteristic to focus on.
     * <p>
     * Must be one of {@link #cellLevelCharacteristics}.
     */
    @Nullable
    private Characteristic focusedCellLevelCharacteristic;

    /**
     * Whether to show the mean.
     */
    private boolean showMean;

    public SingleCellDataBoxplot( SingleCellExpressionDataVector vector ) {
        this.vector = vector;
        this.bioAssays = vector.getSingleCellDimension().getBioAssays();
    }

    /**
     * Create a dataset.
     */
    public BoxAndWhiskerCategoryDataset createDataset() {
        return new SingleCellBoxAndWhiskerCategoryDataset();
    }

    /**
     * Obtain annotations to include in the plot.
     */
    public Collection<CategoryAnnotation> createAnnotations() {
        Random random = new Random( 123L );
        int row;
        if ( cellLevelCharacteristics != null ) {
            row = cellLevelCharacteristics.getCharacteristics().indexOf( focusedCellLevelCharacteristic );
        } else {
            row = 0;
        }
        List<CategoryAnnotation> annotations = new ArrayList<>();
        for ( BioAssay ba : bioAssays ) {
            int sampleStart = getSampleStart( vector, ba );
            double[] data = getSampleDataAsDoubles( vector, ba );
            for ( int i = 0; i < data.length; i++ ) {
                if ( cellLevelCharacteristics == null || cellLevelCharacteristics.getIndices()[sampleStart + i] == row ) {
                    annotations.add( new CategoryPointerAnnotation( "", ba.getName(), data[i], random.nextDouble() * 2 * Math.PI ) );
                }
            }
        }
        return annotations;
    }

    /**
     * Obtain the number of boxplots that would be displayed.
     */
    public int getNumberOfBoxplots() {
        int num = bioAssays.size();
        if ( cellLevelCharacteristics != null && focusedCellLevelCharacteristic == null ) {
            num *= cellLevelCharacteristics.getCharacteristics().size();
        }
        return num;
    }

    public void setBioAssays( List<BioAssay> bioAssays ) {
        Assert.isTrue( new HashSet<>( vector.getSingleCellDimension().getBioAssays() ).containsAll( bioAssays ) );
        this.bioAssays = bioAssays;
    }

    public void setCellLevelCharacteristics( @Nullable CellLevelCharacteristics cellLevelCharacteristics ) {
        // TODO:  the CTAs are lazy-loaded, so we cannot check this
        // Assert.isTrue( cellLevelCharacteristics == null || vector.getSingleCellDimension().getCellTypeAssignments().contains( cellLevelCharacteristics ) || vector.getSingleCellDimension().getCellLevelCharacteristics().contains( cellLevelCharacteristics ),
        //         "The cell-level characteristics must belong to " + vector.getSingleCellDimension() + "." );
        this.cellLevelCharacteristics = cellLevelCharacteristics;
    }

    public void setFocusedCellLevelCharacteristic( @Nullable Characteristic focusedCellLevelCharacteristic ) {
        Assert.isTrue( focusedCellLevelCharacteristic == null || ( cellLevelCharacteristics != null && cellLevelCharacteristics.getCharacteristics().contains( focusedCellLevelCharacteristic ) ),
                "If provided, the focused characteristic must be one of " + cellLevelCharacteristics + "." );
        this.focusedCellLevelCharacteristic = focusedCellLevelCharacteristic;
    }

    private class SingleCellBoxAndWhiskerCategoryDataset implements BoxAndWhiskerCategoryDataset {

        private final List<Comparable> columnKeys;
        private final Map<Comparable, Integer> columnIndex = new HashMap<>();
        private final Map<Comparable, Integer> rowIndex = new HashMap<>();
        private final List<Comparable> rowKeys;

        private DatasetGroup group;

        private SingleCellBoxAndWhiskerCategoryDataset() {
            int i = 0;
            for ( BioAssay ba : bioAssays ) {
                columnIndex.put( ba.getName(), i++ );
            }
            columnKeys = bioAssays.stream()
                    .map( BioAssay::getName )
                    .collect( Collectors.toList() );
            if ( cellLevelCharacteristics != null ) {
                if ( focusedCellLevelCharacteristic != null ) {
                    rowIndex.put( focusedCellLevelCharacteristic.getValue(), 0 );
                    rowKeys = Collections.singletonList( focusedCellLevelCharacteristic.getValue() );
                } else {
                    int j = 0;
                    for ( Characteristic c : cellLevelCharacteristics.getCharacteristics() ) {
                        rowIndex.put( c.getValue(), j++ );
                    }
                    rowKeys = cellLevelCharacteristics.getCharacteristics().stream()
                            .map( Characteristic::getValue )
                            .collect( Collectors.toList() );
                }
            } else {
                String rowKey = "All cells";
                rowIndex.put( rowKey, 0 );
                rowKeys = Collections.singletonList( rowKey );
            }
        }

        @Override
        public Number getMeanValue( int row, int column ) {
            if ( showMean ) {
                if ( cellLevelCharacteristics != null ) {
                    return SingleCellDescriptive.mean( vector, column, cellLevelCharacteristics, row );
                } else {
                    return SingleCellDescriptive.mean( vector, column );
                }
            } else {
                return null;
            }
        }

        @Override
        public Number getMeanValue( Comparable rowKey, Comparable columnKey ) {
            return getMeanValue( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Number getMedianValue( int row, int column ) {
            if ( cellLevelCharacteristics != null ) {
                return SingleCellDescriptive.median( vector, column, cellLevelCharacteristics, row );
            }
            return SingleCellDescriptive.median( vector, column );
        }

        @Override
        public Number getMedianValue( Comparable rowKey, Comparable columnKey ) {
            return getMedianValue( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Number getQ1Value( int row, int column ) {
            return SingleCellDescriptive.quantile( vector, column, 0.25 );
        }

        @Override
        public Number getQ1Value( Comparable rowKey, Comparable columnKey ) {
            return getQ1Value( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Number getQ3Value( int row, int column ) {
            if ( cellLevelCharacteristics != null ) {
                return SingleCellDescriptive.quantile( vector, column, cellLevelCharacteristics, row, 0.75 );
            }
            return SingleCellDescriptive.quantile( vector, column, 0.75 );
        }

        @Override
        public Number getQ3Value( Comparable rowKey, Comparable columnKey ) {
            return getQ3Value( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Number getMinRegularValue( int row, int column ) {
            if ( cellLevelCharacteristics != null ) {
                return SingleCellDescriptive.countFast( vector, column, cellLevelCharacteristics, row ) > 0 ? SingleCellDescriptive.min( vector, column, cellLevelCharacteristics, row ) : null;
            }
            return SingleCellDescriptive.countFast( vector, column ) > 0 ? SingleCellDescriptive.min( vector, column ) : null;
        }

        @Override
        public Number getMinRegularValue( Comparable rowKey, Comparable columnKey ) {
            return getMinRegularValue( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Number getMaxRegularValue( int row, int column ) {
            if ( cellLevelCharacteristics != null ) {
                return SingleCellDescriptive.countFast( vector, column, cellLevelCharacteristics, row ) > 0 ? SingleCellDescriptive.max( vector, column, cellLevelCharacteristics, row ) : null;
            }
            return SingleCellDescriptive.countFast( vector, column ) > 0 ? SingleCellDescriptive.max( vector, column ) : null;
        }

        @Override
        public Number getMaxRegularValue( Comparable rowKey, Comparable columnKey ) {
            return getMaxRegularValue( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Number getMinOutlier( int row, int column ) {
            return null;
        }

        @Override
        public Number getMinOutlier( Comparable rowKey, Comparable columnKey ) {
            return getMinOutlier( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Number getMaxOutlier( int row, int column ) {
            return null;
        }

        @Override
        public Number getMaxOutlier( Comparable rowKey, Comparable columnKey ) {
            return getMaxOutlier( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public List getOutliers( int row, int column ) {
            return Collections.emptyList();
        }

        @Override
        public List getOutliers( Comparable rowKey, Comparable columnKey ) {
            return getOutliers( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Number getValue( int row, int column ) {
            return 0;
        }

        @Override
        public Number getValue( Comparable rowKey, Comparable columnKey ) {
            return getValue( ( int ) rowIndex.get( rowKey ), ( int ) columnIndex.get( columnKey ) );
        }

        @Override
        public Comparable getRowKey( int row ) {
            return rowKeys.get( row );
        }

        @Override
        public int getRowIndex( Comparable key ) {
            return rowIndex.getOrDefault( key, -1 );
        }

        @Override
        public List getRowKeys() {
            return rowKeys;
        }

        @Override
        public Comparable getColumnKey( int column ) {
            return columnKeys.get( column );
        }

        @Override
        public int getColumnIndex( Comparable key ) {
            return columnIndex.getOrDefault( key, -1 );
        }

        @Override
        public List getColumnKeys() {
            return columnKeys;
        }

        @Override
        public int getRowCount() {
            return rowKeys.size();
        }

        @Override
        public int getColumnCount() {
            return columnKeys.size();
        }

        @Override
        public void addChangeListener( DatasetChangeListener listener ) {

        }

        @Override
        public void removeChangeListener( DatasetChangeListener listener ) {

        }

        @Override
        public DatasetGroup getGroup() {
            return group;
        }

        @Override
        public void setGroup( DatasetGroup group ) {
            this.group = group;
        }
    }
}
