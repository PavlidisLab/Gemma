package ubic.gemma.tasks.visualization;

public class MiniPieVisualizationValueObject {
    private int numProbesTotal;
    private int numProbesDiffExpressed;
    private double sliceAngle; // pie chart angle in rad
    
    public MiniPieVisualizationValueObject(int numProbesDiffExpressed, int numProbesTotal) {
        this.numProbesTotal = numProbesTotal;
        this.numProbesDiffExpressed = numProbesDiffExpressed;
        this.sliceAngle = (2 * Math.PI * numProbesDiffExpressed) / numProbesDiffExpressed;        
    }
        
    public int getNumProbesTotal() {
        return numProbesTotal;
    }
    
    public void setNumProbesTotal( int numProbesTotal ) {
        this.numProbesTotal = numProbesTotal;
    }
    
    public int getNumProbesDiffExpressed() {
        return numProbesDiffExpressed;
    }
    
    public void setNumProbesDiffExpressed( int numProbesDiffExpressed ) {
        this.numProbesDiffExpressed = numProbesDiffExpressed;
    }

    public double getSliceAngle() {
        return sliceAngle;
    }

    public void setSliceAngle( double sliceAngle ) {
        this.sliceAngle = sliceAngle;
    }
    
}
