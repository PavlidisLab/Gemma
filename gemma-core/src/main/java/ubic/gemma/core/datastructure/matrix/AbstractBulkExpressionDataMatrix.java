package ubic.gemma.core.datastructure.matrix;

import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class for bulk expression data matrices.
 * <p>
 * If you need to handle multiple {@link BioAssay} per {@link BioMaterial}, use {@link AbstractMultiAssayExpressionDataMatrix}
 * instead.
 *
 * @param <T>
 * @author poirigui
 */
public abstract class AbstractBulkExpressionDataMatrix<T> extends AbstractExpressionDataMatrix<T> implements BulkExpressionDataMatrix<T> {

    @Nullable
    private final ExpressionExperiment expressionExperiment;
    private final QuantitationType quantitationType;
    private final BioAssayDimension bioAssayDimension;
    private final List<BioAssay> bioAssays;
    private final Map<BioAssay, Integer> bioAssayIndex;
    private final List<BioMaterial> bioMaterials;
    private final Map<BioMaterial, Integer> bioMaterialIndex;
    private final List<CompositeSequence> designElements;
    private final Map<CompositeSequence, int[]> designElementsIndex;

    @Nullable
    private List<ExpressionDataMatrixRowElement> rowElements;

    protected AbstractBulkExpressionDataMatrix( @Nullable ExpressionExperiment expressionExperiment, BioAssayDimension dimension,
            QuantitationType quantitationType, List<CompositeSequence> designElements ) {
        this.expressionExperiment = expressionExperiment;
        this.bioAssayDimension = dimension;
        this.bioAssays = new ArrayList<>( dimension.getBioAssays() );
        this.bioAssayIndex = ListUtils.indexOfElements( bioAssays );
        this.bioMaterials = bioAssays.stream().map( BioAssay::getSampleUsed ).collect( Collectors.toList() );
        this.bioMaterialIndex = ListUtils.indexOfElements( bioMaterials );
        this.quantitationType = quantitationType;
        this.designElements = Collections.unmodifiableList( designElements );
        this.designElementsIndex = ListUtils.indexOfAllElements( designElements );
    }

    protected AbstractBulkExpressionDataMatrix( List<? extends BulkExpressionDataVector> vectors ) {
        Assert.isTrue( !vectors.isEmpty(), "At least one vector must be provided." );
        BulkExpressionDataVector example = vectors.iterator().next();
        this.expressionExperiment = example.getExpressionExperiment();
        this.quantitationType = example.getQuantitationType();
        this.bioAssayDimension = example.getBioAssayDimension();
        this.bioAssays = new ArrayList<>( example.getBioAssayDimension().getBioAssays() );
        this.bioAssayIndex = ListUtils.indexOfElements( bioAssays );
        this.bioMaterials = bioAssays.stream().map( BioAssay::getSampleUsed ).collect( Collectors.toList() );
        this.bioMaterialIndex = ListUtils.indexOfElements( bioMaterials );
        List<CompositeSequence> de = new ArrayList<>( vectors.size() );
        for ( BulkExpressionDataVector vector : vectors ) {
            if ( !vector.getExpressionExperiment().equals( example.getExpressionExperiment() ) ) {
                throw new IllegalArgumentException( "All vectors must be from the same ExpressionExperiment" );
            }
            if ( !vector.getQuantitationType().equals( example.getQuantitationType() ) ) {
                throw new IllegalArgumentException( "All vectors must have the same QuantitationType" );
            }
            if ( !vector.getBioAssayDimension().equals( bioAssayDimension ) ) {
                throw new IllegalArgumentException( "All vectors must have the same BioAssayDimension" );
            }
            CompositeSequence designElement = vector.getDesignElement();
            de.add( designElement );
        }
        designElements = Collections.unmodifiableList( de );
        designElementsIndex = ListUtils.indexOfAllElements( designElements );
    }

    @Nullable
    @Override
    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    @Override
    public QuantitationType getQuantitationType() {
        return quantitationType;
    }

    @Override
    public BioAssayDimension getBioAssayDimension() {
        return bioAssayDimension;
    }

    @Override
    public int columns() {
        return bioAssays.size();
    }

    @Override
    public T[] getColumn( BioAssay bioAssay ) {
        int column = getColumnIndex( bioAssay );
        if ( column == -1 ) {
            return null;
        }
        return getColumn( column );
    }

    @Override
    public int getColumnIndex( BioAssay bioAssay ) {
        return bioAssayIndex.getOrDefault( bioAssay, -1 );
    }

    @Override
    public int getColumnIndex( BioMaterial bioMaterial ) {
        return bioMaterialIndex.getOrDefault( bioMaterial, -1 );
    }

    @Override
    public BioAssay getBioAssayForColumn( int index ) {
        return bioAssays.get( index );
    }

    @Override
    public BioMaterial getBioMaterialForColumn( int index ) {
        return bioMaterials.get( index );
    }

    @Override
    public int rows() {
        return designElements.size();
    }

    @Override
    public T[] getRow( CompositeSequence designElement ) {
        int index = getRowIndex( designElement );
        if ( index == -1 ) {
            return null;
        }
        return getRow( index );
    }

    @Override
    public int getRowIndex( CompositeSequence designElement ) {
        int[] indices = designElementsIndex.get( designElement );
        if ( indices == null ) {
            return -1;
        }
        return indices[0];
    }

    @Override
    public int[] getRowIndices( CompositeSequence designElement ) {
        return designElementsIndex.get( designElement );
    }

    @Override
    public List<CompositeSequence> getDesignElements() {
        return designElements;
    }

    @Override
    public CompositeSequence getDesignElementForRow( int index ) {
        return designElements.get( index );
    }

    @Override
    public T get( CompositeSequence designElement, BioAssay bioAssay ) {
        int row = getRowIndex( designElement );
        if ( row == -1 ) {
            return null;
        }
        int column = getColumnIndex( bioAssay );
        if ( column == -1 ) {
            return null;
        }
        return get( row, column );
    }

    @Override
    public List<ExpressionDataMatrixRowElement> getRowElements() {
        if ( rowElements == null ) {
            List<ExpressionDataMatrixRowElement> re = new ArrayList<>( designElements.size() );
            for ( int i = 0; i < designElements.size(); i++ ) {
                re.add( new ExpressionDataMatrixRowElement( this, i ) );
            }
            rowElements = Collections.unmodifiableList( re );
        }
        return rowElements;
    }

    @Override
    public ExpressionDataMatrixRowElement getRowElement( int row ) {
        if ( rowElements != null ) {
            return rowElements.get( row );
        } else if ( row >= 0 && row < rows() ) {
            return new ExpressionDataMatrixRowElement( this, row );
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    protected String getRowLabel( int i ) {
        return designElements.get( i ).getName();
    }

    @Override
    protected String getColumnLabel( int j ) {
        return bioAssays.get( j ).getName();
    }
}
