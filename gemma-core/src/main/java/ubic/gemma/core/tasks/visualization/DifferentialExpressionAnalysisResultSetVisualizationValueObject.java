package ubic.gemma.core.tasks.visualization;

import ubic.gemma.model.common.ValueObject;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains data for a column in metaheatmap visualization.
 */
@SuppressWarnings("unused") // Used in frontend
@ValueObject
public class DifferentialExpressionAnalysisResultSetVisualizationValueObject implements Serializable {

    private String datasetName;
    private String datasetShortName;
    private String datasetLink;
    private Long datasetId;

    private Long analysisId;
    private boolean analysisNotRun = false;

    private Long resultSetId;

    private Long factorId;
    private String factorName;
    private String factorCategory;
    private String factorDescription;

    // Various metrics/scores to be use for display/sorting/filtering.
    private int numberOfProbesTotal;
    private int numberOfProbesDiffExpressed;
    private int numberOfProbesUpRegulated;
    private int numberOfProbesDownRegulated;

    // Visualization values (used to determine the colour of the cell)
    private List<List<Double>> visualizationValues;
    private List<List<Double>> qValues;

    // Number of probes per gene ( per dataset ). Can be used to show genes with multiple probes on the array.
    private List<List<Integer>> numberOfProbes;

    // Contrasts.
    private Map<Long, String> contrastsFactorValues;
    private List<Long> contrastsFactorValueIds;
    private String baselineFactorValue;
    private Long baselineFactorValueId;

    public DifferentialExpressionAnalysisResultSetVisualizationValueObject() {
        super();
    }

    public DifferentialExpressionAnalysisResultSetVisualizationValueObject( int[] geneGroupSizes ) {
        int numberOfGeneGroups = geneGroupSizes.length;

        // Initialize lists
        this.visualizationValues = new ArrayList<>( numberOfGeneGroups );
        this.qValues = new ArrayList<>( numberOfGeneGroups );
        this.numberOfProbes = new ArrayList<>( numberOfGeneGroups );
        for ( int geneGroupSize : geneGroupSizes ) {
            this.visualizationValues.add( new ArrayList<Double>( geneGroupSize ) );
            this.qValues.add( new ArrayList<Double>( geneGroupSize ) );
            this.numberOfProbes.add( new ArrayList<Integer>( geneGroupSize ) );
        }

        this.contrastsFactorValues = new HashMap<>();
        this.contrastsFactorValueIds = new ArrayList<>();
    }

    public void addContrastsFactorValue( long factorValueId, String factorValueName ) {
        this.contrastsFactorValueIds.add( factorValueId );
        this.contrastsFactorValues.put( factorValueId, factorValueName );
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId( Long analysisId ) {
        this.analysisId = analysisId;
    }

    public boolean getAnalysisNotRun() {
        return analysisNotRun;
    }

    public void setAnalysisNotRun( boolean analysisNotRun ) {
        this.analysisNotRun = analysisNotRun;
    }

    public String getBaselineFactorValue() {
        return baselineFactorValue;
    }

    public void setBaselineFactorValue( String baselineFactorValue ) {
        this.baselineFactorValue = baselineFactorValue;
    }

    public Long getBaselineFactorValueId() {
        return baselineFactorValueId;
    }

    public void setBaselineFactorValueId( Long baselineFactorValueId ) {
        this.baselineFactorValueId = baselineFactorValueId;
    }

    public List<Long> getContrastsFactorValueIds() {
        return this.contrastsFactorValueIds;
    }

    public Map<Long, String> getContrastsFactorValues() {
        return contrastsFactorValues;
    }

    public void setContrastsFactorValues( Map<Long, String> contrastsFactorValues ) {
        this.contrastsFactorValues = contrastsFactorValues;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId( Long datasetId ) {
        this.datasetId = datasetId;
    }

    public String getDatasetLink() {
        return datasetLink;
    }

    public void setDatasetLink( String datasetLink ) {
        this.datasetLink = datasetLink;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName( String datasetName ) {
        this.datasetName = datasetName;
    }

    public String getDatasetShortName() {
        return datasetShortName;
    }

    public void setDatasetShortName( String datasetShortName ) {
        this.datasetShortName = datasetShortName;
    }

    public String getFactorCategory() {
        return this.factorCategory;
    }

    public void setFactorCategory( String name ) {
        this.factorCategory = name;
    }

    public String getFactorDescription() {
        return factorDescription;
    }

    public void setFactorDescription( String factorDescription ) {
        this.factorDescription = factorDescription;
    }

    public Long getFactorId() {
        return factorId;
    }

    public void setFactorId( Long factorId ) {
        this.factorId = factorId;
    }

    public String getFactorName() {
        return factorName;
    }

    public void setFactorName( String factorName ) {
        this.factorName = factorName;
    }

    public List<List<Integer>> getNumberOfProbes() {
        return numberOfProbes;
    }

    public void setNumberOfProbes( List<List<Integer>> numberOfProbes ) {
        this.numberOfProbes = numberOfProbes;
    }

    public int getNumberOfProbesDiffExpressed() {
        return numberOfProbesDiffExpressed;
    }

    public void setNumberOfProbesDiffExpressed( int numberOfProbesDiffExpressed ) {
        this.numberOfProbesDiffExpressed = numberOfProbesDiffExpressed;
    }

    public int getNumberOfProbesDownRegulated() {
        return numberOfProbesDownRegulated;
    }

    public void setNumberOfProbesDownRegulated( int numberOfProbesDownRegulated ) {
        this.numberOfProbesDownRegulated = numberOfProbesDownRegulated;
    }

    public int getNumberOfProbesTotal() {
        return numberOfProbesTotal;
    }

    public void setNumberOfProbesTotal( int numberOfProbesTotal ) {
        this.numberOfProbesTotal = numberOfProbesTotal;
    }

    public int getNumberOfProbesUpRegulated() {
        return numberOfProbesUpRegulated;
    }

    public void setNumberOfProbesUpRegulated( int numberOfProbesUpRegulated ) {
        this.numberOfProbesUpRegulated = numberOfProbesUpRegulated;
    }

    public List<List<Double>> getqValues() {
        return qValues;
    }

    public void setqValues( List<List<Double>> qValues ) {
        this.qValues = qValues;
    }

    public Long getResultSetId() {
        return resultSetId;
    }

    public void setResultSetId( Long resultSetId ) {
        this.resultSetId = resultSetId;
    }

    public List<List<Double>> getVisualizationValues() {
        return visualizationValues;
    }

    public void setVisualizationValues( List<List<Double>> visualizationValues ) {
        this.visualizationValues = visualizationValues;
    }

    public void setNumberOfProbes( int geneGroupIndex, int geneIndex, Integer numberOfProbes ) {
        this.numberOfProbes.get( geneGroupIndex ).add( geneIndex, numberOfProbes );
    }

    public void setQvalue( int geneGroupIndex, int geneIndex, Double qValue ) {
        this.qValues.get( geneGroupIndex ).add( geneIndex, qValue );
    }

    public void setVisualizationValue( int geneGroupIndex, int geneIndex, Double value ) {
        this.visualizationValues.get( geneGroupIndex ).add( geneIndex, value );
    }

    public String toText() {
        DecimalFormat df = new DecimalFormat( "#.######" );
        StringBuilder text = new StringBuilder();
        text.append( "|" ).append( datasetShortName ).append( "|" ).append( factorName ).append( "|" );
        for ( Long fvId : this.contrastsFactorValues.keySet() ) {
            text.append( this.contrastsFactorValues.get( fvId ).trim() ).append( "," );
        }
        text.append( "|" );
        for ( List<Double> pValueGroup : this.qValues ) {
            for ( Double pValue : pValueGroup ) {
                if ( pValue == null ) {
                    text.append( "NA|" );
                } else {
                    text.append( df.format( pValue ) ).append( "|" );
                }
            }
        }
        text.append( "\n" );
        return text.toString();
    }
}
