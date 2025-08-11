package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.util.SparseRangeArrayList;

import javax.annotation.Nullable;
import java.util.*;

public class SingleCellDesignMatrixImpl implements SingleCellDesignMatrix {

    // rows
    private final SparseRangeArrayList<BioAssay> assays;
    private final List<String> cellIds;
    private final Map<BioAssay, Map<String, Integer>> index;

    // columns
    private final List<ExperimentalFactor> factors;
    private final Map<ExperimentalFactor, Integer> factorsIndex;

    /**
     * This is technically a matrix, but using {@link List} allows for sparse range array to be used for sample-level
     * factors.
     * <p>
     * Also, this is transposed w.r.t. to rows/columns that the interface requires. This is due to the fact that
     * sparsity is better handled along factors
     */
    private final List<List<FactorValue>> factorValues;

    public SingleCellDesignMatrixImpl( SingleCellDimension dimension, List<BioAssay> assays, List<ExperimentalFactor> factors, List<CellLevelCharacteristics> cellLevelCharacteristics ) {
        int[] bioAssayOffsets = new int[assays.size()];
        int k = 0;
        List<String> cellIdsL = new ArrayList<>( dimension.getNumberOfCells() );
        Map<BioAssay, Map<String, Integer>> index = new HashMap<>( assays.size() );
        for ( int i = 0; i < assays.size(); i++ ) {
            BioAssay assay = assays.get( i );
            int sampleIndex = dimension.getBioAssays().indexOf( assay );
            if ( sampleIndex < 0 ) {
                throw new IllegalArgumentException( assay + " is not part of " + dimension + "." );
            }
            List<String> sampleCellIds = dimension.getCellIdsBySample( sampleIndex );
            bioAssayOffsets[i] = k;
            cellIdsL.addAll( sampleCellIds );
            Map<String, Integer> cellid2pos = new HashMap<>();
            for ( int j = 0; j < sampleCellIds.size(); j++ ) {
                cellid2pos.put( sampleCellIds.get( j ), k + j );
            }
            index.put( assay, cellid2pos );
            k += sampleCellIds.size();
        }
        this.assays = new SparseRangeArrayList<>( assays, bioAssayOffsets, k );
        this.cellIds = cellIdsL;
        this.index = index;
        ArrayList<ExperimentalFactor> factorsL = new ArrayList<>( factors.size() + cellLevelCharacteristics.size() );
        factorsL.addAll( factors );
        for ( CellLevelCharacteristics clc : cellLevelCharacteristics ) {
            ExperimentalFactor factor = createFactorFromCellLevelCharacteristics( clc );
            factorsL.add( factor );
        }
        this.factors = Collections.unmodifiableList( factorsL );
        this.factorsIndex = Collections.unmodifiableMap( ListUtils.indexOfElements( factorsL ) );
        // TODO: fill the matrix
        this.factorValues = new ArrayList<>( factors.size() );
    }

    @Override
    public List<ExperimentalFactor> getFactors() {
        return factors;
    }

    @Override
    public List<BioAssay> getBioAssays() {
        return assays;
    }

    @Override
    public List<String> getCellIds() {
        return cellIds;
    }

    @Override
    public int columns() {
        return factors.size();
    }

    @Override
    public List<FactorValue> getColumn( int column ) {
        return factorValues.get( column );
    }

    @Nullable
    @Override
    public List<FactorValue> getColumn( ExperimentalFactor factor ) {
        int index = factors.indexOf( factor );
        if ( index == -1 ) {
            return null;
        }
        return getColumn( index );
    }

    @Override
    public int getColumnIndex( ExperimentalFactor factor ) {
        return 0;
    }

    @Override
    public ExperimentalFactor getFactorForColumn( int column ) {
        return factors.get( column );
    }

    @Nullable
    @Override
    public List<FactorValue> getRow( BioAssay bioAssay, String cellId ) {
        int row = getRowIndex( bioAssay, cellId );
        if ( row == -1 ) {
            return null;
        }
        return getRow( row );
    }

    @Override
    public List<FactorValue> getRow( int row ) {
        List<FactorValue> fvs = new ArrayList<>( factors.size() );
        for ( int i = 0; i < factors.size(); i++ ) {
            fvs.add( factorValues.get( i ).get( row ) );
        }
        return fvs;
    }

    @Override
    public int rows() {
        return cellIds.size();
    }

    @Override
    public BioAssay getBioAssayForRow( int row ) {
        return assays.get( row );
    }

    @Override
    public BioMaterial getBioMaterialForRow( int row ) {
        return getBioAssayForRow( row ).getSampleUsed();
    }

    @Override
    public String getCellIdForRow( int row ) {
        return cellIds.get( row );
    }

    @Override
    public int getRowIndex( BioAssay bioAssay, String cellId ) {
        Map<String, Integer> cell2pos = index.get( bioAssay );
        if ( cell2pos == null ) {
            return -1;
        }
        return cell2pos.getOrDefault( cellId, -1 );
    }

    private ExperimentalFactor createFactorFromCellLevelCharacteristics( CellLevelCharacteristics characteristics ) {
        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( characteristics.getName(), FactorType.CATEGORICAL );
        for ( Characteristic c : characteristics.getCharacteristics() ) {
            FactorValue fv = FactorValue.Factory.newInstance( factor, c );
            factor.getFactorValues().add( fv );
        }
        return factor;
    }
}
