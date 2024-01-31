package ubic.gemma.model.expression.bioAssayData;

public class RawOrProcessedExpressionDataVector extends DesignElementDataVector {

    private BioAssayDimension bioAssayDimension;

    public BioAssayDimension getBioAssayDimension() {
        return bioAssayDimension;
    }

    public void setBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }
}
