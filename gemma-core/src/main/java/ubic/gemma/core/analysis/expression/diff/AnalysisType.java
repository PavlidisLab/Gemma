package ubic.gemma.core.analysis.expression.diff;

/**
 * Defines the different types of analyses our linear modeling framework supports:
 * <ul>
 * <li>GENERICLM - generic linear regression (interactions are omitted, but this could change)
 * <li>OSTTEST - one sample t-test
 * <li>OWA - one-way ANOVA
 * <li>TTEST - two sample t-test
 * <li>TWO_WAY_ANOVA_WITH_INTERACTION
 * <li>TWO_WAY_ANOVA_NO_INTERACTION
 * </ul>
 *
 * @author Paul
 */
public enum AnalysisType {
    GENERICLM, //
    OSTTEST, //one-sample
    OWA, //one-way ANOVA
    TTEST, //
    TWO_WAY_ANOVA_WITH_INTERACTION, //with interactions
    TWO_WAY_ANOVA_NO_INTERACTION //no interactions
}
