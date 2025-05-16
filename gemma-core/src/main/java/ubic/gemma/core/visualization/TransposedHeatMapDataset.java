package ubic.gemma.core.visualization;

import org.jfree.data.general.HeatMapDataset;

@SuppressWarnings("SuspiciousNameCombination")
public class TransposedHeatMapDataset implements HeatMapDataset {

    private final HeatMapDataset delegate;

    public TransposedHeatMapDataset( HeatMapDataset dataset ) {
        this.delegate = dataset;
    }

    @Override
    public int getXSampleCount() {
        return delegate.getYSampleCount();
    }

    @Override
    public int getYSampleCount() {
        return delegate.getXSampleCount();
    }

    @Override
    public double getMinimumXValue() {
        return delegate.getMinimumYValue();
    }

    @Override
    public double getMaximumXValue() {
        return delegate.getMaximumYValue();
    }

    @Override
    public double getMinimumYValue() {
        return delegate.getMinimumXValue();
    }

    @Override
    public double getMaximumYValue() {
        return delegate.getMaximumXValue();
    }

    @Override
    public double getXValue( int xIndex ) {
        return delegate.getYValue( xIndex );
    }

    @Override
    public double getYValue( int yIndex ) {
        return delegate.getXValue( yIndex );
    }

    @Override
    public double getZValue( int xIndex, int yIndex ) {
        return delegate.getZValue( yIndex, xIndex );
    }

    @Override
    public Number getZ( int xIndex, int yIndex ) {
        return delegate.getZ( yIndex, xIndex );
    }
}
