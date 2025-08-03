package ubic.gemma.core.datastructure.matrix;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SingleCellDesignMatrixImpl implements SingleCellDesignMatrix {

    private final SparseRangeArrayList<BioAssay> assays;
    private final List<String> cellIds;
    private final List<ExperimentalFactor> factors;

    public SingleCellDesignMatrixImpl( SingleCellDimension dimension, List<BioAssay> assays, List<ExperimentalFactor> factors, List<CellLevelCharacteristics> cellLevelCharacteristics ) {
        int[] bioAssayOffsets = new int[assays.size()];
        int k = 0;
        List<String> cellIdsL = new ArrayList<>( dimension.getNumberOfCells() );
        for ( int i = 0; i < assays.size(); i++ ) {
            BioAssay assay = assays.get( i );
            int sampleIndex = dimension.getBioAssays().indexOf( assay );
            if ( sampleIndex < 0 ) {
                throw new IllegalArgumentException( assay + " is not part of " + dimension + "." );
            }
            List<String> sampleCellIds = dimension.getCellIdsBySample( sampleIndex );
            bioAssayOffsets[i] = k;
            cellIdsL.addAll( sampleCellIds );
            k += sampleCellIds.size();
        }
        this.assays = new SparseRangeArrayList<>( assays, bioAssayOffsets, k );
        this.cellIds = cellIdsL;
        ArrayList<ExperimentalFactor> factorsL = new ArrayList<>( factors.size() + cellLevelCharacteristics.size() );
        factorsL.addAll( factors );
        for ( CellLevelCharacteristics clc : cellLevelCharacteristics ) {
            ExperimentalFactor factor = createFactorFromCellLevelCharacteristics( clc );
            factorsL.add( factor );
        }
        this.factors = Collections.unmodifiableList( factorsL );
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
        return new FactorValue[0];
    }

    @Nullable
    @Override
    public List<FactorValue> getColumn( ExperimentalFactor factor ) {
        return new FactorValue[0];
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
        return new FactorValue[0];
    }

    @Override
    public List<FactorValue> getRow( int row ) {
        return new FactorValue[0];
    }

    @Override
    public int rows() {
        return 0;
    }

    @Override
    public BioAssay getBioAssayForRow( int row ) {
        return null;
    }

    @Override
    public BioMaterial getBioMaterialForRow( int row ) {
        return getBioAssayForRow( row ).getSampleUsed();
    }

    @Override
    public String getCellIdForRow( int row ) {
        return "";
    }

    @Override
    public int getRowIndex( BioAssay bioAssay, String cellId ) {
        return 0;
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
