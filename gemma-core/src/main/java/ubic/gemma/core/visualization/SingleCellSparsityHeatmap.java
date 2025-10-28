package ubic.gemma.core.visualization;

import lombok.Getter;
import lombok.Setter;
import org.jfree.data.general.DefaultHeatMapDataset;
import org.jfree.data.general.HeatMapDataset;
import org.jfree.data.general.HeatMapUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Heatmap that displays the sparsity of single-cell data.
 * @author poirigui
 */
@Getter
@Setter
public class SingleCellSparsityHeatmap implements Heatmap {

    private final ExpressionExperiment expressionExperiment;
    private final List<BioAssay> samples;
    private final SingleCellDimension singleCellDimension;
    private final BioAssayDimension dimension;
    private final List<ExpressionExperimentSubSet> subSets;
    @Nullable
    private final Map<BioAssay, Long> designElementsPerSample;
    @Nullable
    private final SingleCellHeatmapType type;
    private int cellSize = 16;
    private boolean transpose;

    /**
     *
     * @param expressionExperiment    an experiment containing single-cell data
     * @param singleCellDimension     a single-cell dimension
     * @param dimension               a regular dimension for aggregated data
     * @param subSets                 a set of subsets containing aggregated data
     * @param designElementsPerSample the number of design elements for the platform of each sample
     * @param type                    the type of heatmap to generate
     */
    public SingleCellSparsityHeatmap( ExpressionExperiment expressionExperiment, SingleCellDimension singleCellDimension, BioAssayDimension dimension, Collection<ExpressionExperimentSubSet> subSets, @Nullable Map<BioAssay, Long> designElementsPerSample, @Nullable SingleCellHeatmapType type ) {
        Assert.isTrue( expressionExperiment.getBioAssays().containsAll( singleCellDimension.getBioAssays() ) );
        Assert.isTrue( subSets.stream().map( BioAssaySet::getBioAssays ).allMatch( dimension.getBioAssays()::containsAll ) );
        this.expressionExperiment = expressionExperiment;
        this.samples = expressionExperiment.getBioAssays().stream()
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .collect( Collectors.toList() );
        this.singleCellDimension = singleCellDimension;
        this.dimension = dimension;
        this.subSets = subSets.stream()
                .sorted( Comparator.comparing( ExpressionExperimentSubSet::getName ) )
                .collect( Collectors.toList() );
        this.designElementsPerSample = designElementsPerSample;
        this.type = type;
    }

    public enum SingleCellHeatmapType {
        CELL, GENE
    }

    @Override
    public BufferedImage createImage( int cellSize ) {
        Assert.notNull( type );
        DefaultHeatMapDataset d = new DefaultHeatMapDataset( subSets.size(), expressionExperiment.getBioAssays().size(), 0, 1, 0, 1 );
        for ( int i = 0; i < subSets.size(); i++ ) {
            for ( int j = 0; j < samples.size(); j++ ) {
                BioAssay sourceAssay = samples.get( j );
                int sampleIndex = singleCellDimension.getBioAssays().indexOf( sourceAssay );
                BioAssay assay = subSets.get( i ).getBioAssays().stream()
                        .filter( b -> Objects.equals( b.getSampleUsed().getSourceBioMaterial(), sourceAssay.getSampleUsed() ) )
                        .findFirst()
                        .orElse( null );
                double z;
                if ( assay != null ) {
                    switch ( type ) {
                        case CELL:
                            if ( assay.getNumberOfCells() != null ) {
                                z = ( double ) assay.getNumberOfCells() / ( double ) singleCellDimension.getNumberOfCellIdsBySample( sampleIndex );
                            } else {
                                z = Double.NaN;
                            }
                            break;
                        case GENE:
                            Assert.notNull( designElementsPerSample, "The number of design elements per sample must be supplied when plotting gene-level sparsity." );
                            if ( assay.getNumberOfDesignElements() != null ) {
                                z = ( double ) assay.getNumberOfDesignElements() / ( double ) designElementsPerSample.get( sourceAssay );
                            } else {
                                z = Double.NaN;
                            }
                            break;
                        default:
                            throw new IllegalArgumentException( "Unknown heatmap type" );
                    }
                } else {
                    z = Double.NaN;
                }
                d.setZValue( i, j, z );
            }
        }
        HeatMapDataset d2 = d;
        if ( cellSize > 1 ) {
            d2 = new ScaledHeatMapDataset( d, cellSize );
        }
        if ( transpose ) {
            d2 = new TransposedHeatMapDataset( d );
        }
        return HeatMapUtils.createHeatMapImage( d2, GEMMA_PAINT_SCALE );
    }

    public BufferedImage createAggregateImage( SingleCellHeatmapType type ) {
        DefaultHeatMapDataset d = new DefaultHeatMapDataset( 1, samples.size(), 0, 1, 0, 1 );
        for ( int j = 0; j < samples.size(); j++ ) {
            BioAssay sample = samples.get( j );
            int sampleIndex = singleCellDimension.getBioAssays().indexOf( sample );
            double z;
            switch ( type ) {
                case CELL:
                    if ( sample.getNumberOfCells() != null ) {
                        z = ( double ) sample.getNumberOfCells() / singleCellDimension.getNumberOfCellIdsBySample( sampleIndex );
                    } else {
                        z = Double.NaN;
                    }
                    break;
                case GENE:
                    Assert.notNull( designElementsPerSample, "The number of design elements per sample must be supplied when plotting gene-level sparsity." );
                    if ( sample.getNumberOfDesignElements() != null ) {
                        z = ( double ) sample.getNumberOfDesignElements() / ( double ) designElementsPerSample.get( sample );
                    } else {
                        z = Double.NaN;
                    }
                    break;
                default:
                    throw new IllegalArgumentException( "Unknown heatmap type" );
            }
            d.setZValue( 0, j, z );
        }
        HeatMapDataset d2 = d;
        if ( cellSize > 1 ) {
            d2 = new ScaledHeatMapDataset( d2, cellSize );
        }
        if ( transpose ) {
            d2 = new TransposedHeatMapDataset( d2 );
        }
        return HeatMapUtils.createHeatMapImage( d2, GEMMA_PAINT_SCALE );
    }

    @Override
    public List<String> getXLabels() {
        if ( transpose ) {
            return subSets.stream().map( ExpressionExperimentSubSet::getName ).collect( Collectors.toList() );
        }
        return samples.stream().map( BioAssay::getName ).collect( Collectors.toList() );
    }

    @Override
    public List<String> getYLabels() {
        if ( transpose ) {
            return samples.stream().map( BioAssay::getName ).collect( Collectors.toList() );
        }
        return subSets.stream().map( ExpressionExperimentSubSet::getName ).collect( Collectors.toList() );
    }
}
