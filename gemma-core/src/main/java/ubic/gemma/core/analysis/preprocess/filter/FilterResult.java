package ubic.gemma.core.analysis.preprocess.filter;

import lombok.Data;

@Data
public class FilterResult {
    private int startingRows = 0;
    private int afterDistinctValueCut = 0;
    private int afterInitialFilter = 0;
    private int afterLowExpressionCut = 0;
    private int afterLowVarianceCut = 0;
    private int afterMinPresentFilter = 0;
    private int afterZeroVarianceCut = 0;

    @Override
    public String toString() {
        return "# startingProbes:" + startingRows + "\n" +
                "# afterInitialFilter:" + afterInitialFilter + "\n" +
                "# afterMinPresentFilter:" + afterMinPresentFilter + "\n" +
                "# afterLowVarianceCut:" + afterLowVarianceCut + "\n" +
                "# afterLowExpressionCut:" + afterLowExpressionCut;
    }
}
