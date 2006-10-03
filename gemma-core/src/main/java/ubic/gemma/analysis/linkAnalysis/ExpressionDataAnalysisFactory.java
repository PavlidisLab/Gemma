package ubic.gemma.analysis.linkAnalysis;

/*
* @author xiangwan
*/

public class ExpressionDataAnalysisFactory {
    public static LinkAnalysis linkAnalysis(ExpressionDataLoader expressionDataLoader ) {
        return new LinkAnalysis(expressionDataLoader);
    }
}
