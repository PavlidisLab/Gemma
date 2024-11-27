package ubic.gemma.core.visualization;

import org.jfree.data.general.HeatMapDataset;

class ScaledHeatMapDataset implements HeatMapDataset {

    private final HeatMapDataset delegate;
    private final int scaleFactor;

    ScaledHeatMapDataset( HeatMapDataset delegate, int scaleFactor ) {
        this.delegate = delegate;
        this.scaleFactor = scaleFactor;
    }

    @Override
    public int getXSampleCount() {
        return scaleFactor * delegate.getXSampleCount();
    }

    @Override
    public int getYSampleCount() {
        return scaleFactor * delegate.getYSampleCount();
    }

    @Override
    public double getMinimumXValue() {
        return delegate.getMinimumXValue();
    }

    @Override
    public double getMaximumXValue() {
        return delegate.getMinimumXValue();
    }

    @Override
    public double getMinimumYValue() {
        return delegate.getMinimumYValue();
    }

    @Override
    public double getMaximumYValue() {
        return delegate.getMaximumYValue();
    }

    @Override
    public double getXValue( int xIndex ) {
        return delegate.getXValue( xIndex / scaleFactor );
    }

    @Override
    public double getYValue( int yIndex ) {
        return delegate.getYValue( yIndex / scaleFactor );
    }

    @Override
    public double getZValue( int xIndex, int yIndex ) {
        return delegate.getZValue( xIndex / scaleFactor, yIndex / scaleFactor );
    }

    @Override
    public Number getZ( int xIndex, int yIndex ) {
        return delegate.getZ( xIndex / scaleFactor, yIndex / scaleFactor );
    }
}
