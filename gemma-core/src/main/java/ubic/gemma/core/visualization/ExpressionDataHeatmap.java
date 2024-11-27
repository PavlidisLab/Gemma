package ubic.gemma.core.visualization;

import lombok.Getter;
import lombok.Setter;
import org.jfree.data.general.HeatMapDataset;
import org.jfree.data.general.HeatMapUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Expression data heatmap for experiments and subsets.
 * <p>
 * There is also an implementation of this in JavaScript
 * @author poirigui
 */
@Getter
@Setter
public class ExpressionDataHeatmap implements Heatmap {

    /**
     * Create a heatmap for a given set of vectors.
     * @param ee        experiment from which the data originates
     * @param dimension dimension to render
     * @param vectors   vectors to display
     * @param genes     genes to display as row labels (must match the number of vectors)
     */
    public static ExpressionDataHeatmap fromVectors( ExpressionExperiment ee, BioAssayDimension dimension, Slice<? extends BulkExpressionDataVector> vectors, @Nullable List<Gene> genes ) {
        return new ExpressionDataHeatmap( ee, dimension, null, vectors, null, genes );
    }

    /**
     * Create a heatmap for a subset.
     * @param subSet    subset from which the data originates
     * @param dimension dimension encompassing the subset, only the assays from the subset will be displayed
     * @see #fromVectors(ExpressionExperiment, BioAssayDimension, Slice, List)
     */
    public static ExpressionDataHeatmap fromVectors( ExpressionExperimentSubSet subSet, BioAssayDimension dimension, Slice<? extends BulkExpressionDataVector> vectors, @Nullable List<Gene> genes ) {
        return new ExpressionDataHeatmap( subSet, dimension, subSet.getBioAssays(), vectors, null, genes );
    }

    /**
     * Create a heatmap for a given set of design elements.
     * <p>
     * in this mode, no image can be generated, but labels can are available.
     * @see #fromVectors(ExpressionExperiment, BioAssayDimension, Slice, List)
     */
    public static ExpressionDataHeatmap fromDesignElements( ExpressionExperiment ee, BioAssayDimension dimension, Slice<CompositeSequence> designElements, @Nullable List<Gene> genes ) {
        return new ExpressionDataHeatmap( ee, dimension, null, null, designElements, genes );
    }

    /**
     * Create a heatmap for a subset using design elements.
     * Create a heatmap for a subset.
     * @see #fromVectors(ExpressionExperimentSubSet, BioAssayDimension, Slice, List)
     */
    public static ExpressionDataHeatmap fromDesignElements( ExpressionExperimentSubSet subSet, BioAssayDimension dimension, Slice<CompositeSequence> designElements, @Nullable List<Gene> genes ) {
        return new ExpressionDataHeatmap( subSet, dimension, subSet.getBioAssays(), null, designElements, genes );
    }

    private final BioAssaySet bioAssaySet;
    private final BioAssayDimension dimension;
    private final List<BioAssay> samples;
    private final int[] sampleIndex;
    @Nullable
    private final Slice<? extends BulkExpressionDataVector> vectors;
    @Nullable
    private final Slice<CompositeSequence> designElements;
    @Nullable
    private final List<Gene> genes;
    private int cellSize = 16;
    private boolean transpose = false;

    private ExpressionDataHeatmap( BioAssaySet bioAssaySet, BioAssayDimension dimension,
            @Nullable Collection<BioAssay> bioAssays, @Nullable Slice<? extends BulkExpressionDataVector> vectors,
            @Nullable Slice<CompositeSequence> designElements, @Nullable List<Gene> genes ) {
        Assert.isTrue( bioAssays == null || new HashSet<>( dimension.getBioAssays() ).containsAll( bioAssays ),
                "The dimension must contain all the provided assays." );
        Assert.isTrue( vectors != null || designElements != null,
                "Either vectors or design elements must be provided." );
        Assert.isTrue( designElements == null || vectors == null || ( Objects.equals( designElements.getSort(), vectors.getSort() )
                        && Objects.equals( designElements.getOffset(), vectors.getOffset() )
                        && Objects.equals( designElements.getLimit(), vectors.getLimit() )
                        && designElements.size() == vectors.size() ),
                "The number of vectors and design elements must match if they are both provided." );
        Assert.isTrue( vectors == null || genes == null || vectors.size() == genes.size(),
                "The number of genes must match the number of vectors." );
        Assert.isTrue( designElements == null || genes == null || designElements.size() == genes.size(),
                "The number of genes must match the number of design elements." );
        this.bioAssaySet = bioAssaySet;
        this.dimension = dimension;
        this.samples = dimension.getBioAssays().stream()
                .filter( ba -> bioAssays == null || bioAssays.contains( ba ) )
                .collect( Collectors.toList() );
        this.sampleIndex = new int[samples.size()];
        for ( int i = 0; i < samples.size(); i++ ) {
            this.sampleIndex[i] = dimension.getBioAssays().indexOf( samples.get( i ) );
        }
        this.vectors = vectors;
        if ( vectors != null && designElements == null ) {
            this.designElements = new Slice<>( vectors.stream().map( BulkExpressionDataVector::getDesignElement ).collect( Collectors.toList() ),
                    vectors.getSort(), vectors.getOffset(), vectors.getLimit(), vectors.getTotalElements() );
        } else {
            this.designElements = designElements;
        }
        this.genes = genes;
    }

    public BufferedImage createImage( int cellSize ) {
        Assert.notNull( vectors, "Vectors must be set to generate an image." );
        double maxData = Double.MIN_VALUE;
        double minData = Double.MAX_VALUE;
        double[][] data = new double[samples.size()][vectors.size()];
        for ( int i = 0; i < vectors.size(); i++ ) {
            BulkExpressionDataVector vec = vectors.get( i );
            DoubleBuffer buffer = ByteBuffer.wrap( vec.getData() ).asDoubleBuffer();
            for ( int j = 0; j < samples.size(); j++ ) {
                data[j][i] = buffer.get( sampleIndex[j] );
                if ( Double.isFinite( data[j][i] ) ) {
                    maxData = Math.max( maxData, data[j][i] );
                    minData = Math.min( minData, data[j][i] );
                }
            }
        }
        HeatMapDataset dataset = new HeatMapDatasetImpl( data, minData, maxData );
        if ( transpose ) {
            dataset = new TransposedHeatMapDataset( dataset );
        }
        if ( cellSize > 1 ) {
            dataset = new ScaledHeatMapDataset( dataset, cellSize );
        }
        return HeatMapUtils.createHeatMapImage( dataset, GEMMA_PAINT_SCALE );
    }

    @Override
    public List<String> getXLabels() {
        if ( transpose ) {
            return samples.stream().map( BioAssay::getName ).collect( Collectors.toList() );
        }
        if ( genes != null ) {
            return genes.stream().map( g -> g != null ? g.getOfficialSymbol() : "null" ).collect( Collectors.toList() );
        } else if ( vectors != null ) {
            return vectors.stream().map( v -> v.getDesignElement().getName() ).collect( Collectors.toList() );
        } else {
            throw new IllegalStateException( "No genes nor vectors are set, cannot generate row labels." );
        }
    }

    @Override
    public List<String> getYLabels() {
        if ( transpose ) {
            if ( genes != null ) {
                return genes.stream().map( g -> g != null ? g.getOfficialSymbol() : "null" ).collect( Collectors.toList() );
            } else if ( vectors != null ) {
                return vectors.stream().map( v -> v.getDesignElement().getName() ).collect( Collectors.toList() );
            } else {
                throw new IllegalStateException( "No genes nor vectors are set, cannot generate row labels." );
            }
        }
        return samples.stream().map( BioAssay::getName ).collect( Collectors.toList() );
    }

    private static class HeatMapDatasetImpl implements HeatMapDataset {

        private final double[][] data;
        private final double minData, maxData;

        private HeatMapDatasetImpl( double[][] data, double minData, double maxData ) {
            this.data = data;
            this.minData = minData;
            this.maxData = maxData;
        }

        @Override
        public int getXSampleCount() {
            return data.length;
        }

        @Override
        public int getYSampleCount() {
            return data.length > 0 ? data[0].length : 0;
        }

        @Override
        public double getMinimumXValue() {
            return 0;
        }

        @Override
        public double getMaximumXValue() {
            return 1;
        }

        @Override
        public double getMinimumYValue() {
            return 0;
        }

        @Override
        public double getMaximumYValue() {
            return 1;
        }

        @Override
        public double getXValue( int xIndex ) {
            return 0;
        }

        @Override
        public double getYValue( int yIndex ) {
            return 0;
        }

        @Override
        public double getZValue( int xIndex, int yIndex ) {
            return ( data[xIndex][yIndex] - minData ) / ( maxData - minData );
        }

        @Override
        public Number getZ( int xIndex, int yIndex ) {
            return ( data[xIndex][yIndex] - minData ) / ( maxData - minData );
        }
    }
}
