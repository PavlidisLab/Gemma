package ubic.gemma.web.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;

// This object represents a column in visualization. It also contains contrasts data so each factor can
// be expanded...
// 
// 
public class DifferentialExpressionAnalysisResultSetVisualizationValueObject {    
    private int numberOfGeneGroups;
    private int[] geneGroupSizes;    
    private int numberOfFactorValues;
    
    // [geneGroupIndex] [geneIndex]
    //private List<List<String>> geneNames;
    //private List<List<Long>> geneIds;
    
    private String datasetName;
    private String datasetLink;
    private Long datasetId;
    private Long analysisId;
    private String subsetFactor;
    private String subsetFactorValue;
    private String factorName;
    private String factorCategory;
    
    // Various metrics/scores to be use for display/sorting/filtering.
    private int numberOfProbesTotal;    
    private int numberOfProbesDiffExpressed;
    private int numberOfProbesUpRegulated;
    private int numberOfProbesDownRegulated;
    
    private int numberOfGenesDiffExpressedFromGeneGroup;
    
    private boolean analysisNotRun = false;
        
    // Data
    
    // 
    private List<List<Double>> visualizationValues;
    private List<List<Double>> pValues;
    // Number of probes per gene ( per dataset ). Can be used to show genes with multiple probes on the array.  
    private List<List<Integer>> numberOfProbes;                 
    
    // Contrasts:    
    private Map<Long, String> contrastsFactorValues;
    private List<Long> contrastsFactorValueIds;
    
    private String baselineFactorValue;

    // [geneGroupIndex][geneIndex][contrastIndex]
    private List<List<List<Double>>> contrastsVisualizationValues;
    private List<List<List<Double>>> constrastsFoldChangeValues;
   
    public DifferentialExpressionAnalysisResultSetVisualizationValueObject ( int[] geneGroupSizes ) {
        this.numberOfGeneGroups = geneGroupSizes.length;
        this.geneGroupSizes = geneGroupSizes;
        
        // Initialize lists        
        this.visualizationValues = new ArrayList<List<Double>>( numberOfGeneGroups );
        this.pValues = new ArrayList<List<Double>>( numberOfGeneGroups );                
        this.numberOfProbes = new ArrayList<List<Integer>>( numberOfGeneGroups );
        for ( int i = 0; i < numberOfGeneGroups; i++ ) {
            this.visualizationValues.add ( new ArrayList<Double> ( geneGroupSizes[i] ) );
            this.pValues.add ( new ArrayList<Double> ( geneGroupSizes[i] ) );
            this.numberOfProbes.add ( new ArrayList<Integer> ( geneGroupSizes[i] ) );
        }                
        
        this.contrastsFactorValues = new HashMap<Long,String>();
        this.contrastsFactorValueIds = new ArrayList<Long>();
    }    
    
//  List<List<List<Double>>>  contrastsVisualizationValues    = new ArrayList<List<List<Double>>>();
//  List<List<List<Double>>>  contrastsFoldChangeValues       = new ArrayList<List<List<Double>>>();                        
    
    public boolean getAnalysisNotRun() {
        return analysisNotRun;
    }

    public void setAnalysisNotRun( boolean analysisNotRun ) {
        this.analysisNotRun = analysisNotRun;
    }

    public int getNumberOfProbesTotal() {
        return numberOfProbesTotal;
    }

    public void setNumberOfProbesTotal( int numberOfProbesTotal ) {
        this.numberOfProbesTotal = numberOfProbesTotal;
    }

    public int getNumberOfProbesDiffExpressed() {
        return numberOfProbesDiffExpressed;
    }

    public void setNumberOfProbesDiffExpressed( int numberOfProbesDiffExpressed ) {
        this.numberOfProbesDiffExpressed = numberOfProbesDiffExpressed;
    }

    public int getNumberOfProbesUpRegulated() {
        return numberOfProbesUpRegulated;
    }

    public void setNumberOfProbesUpRegulated( int numberOfProbesUpRegulated ) {
        this.numberOfProbesUpRegulated = numberOfProbesUpRegulated;
    }

    public int getNumberOfProbesDownRegulated() {
        return numberOfProbesDownRegulated;
    }

    public void setNumberOfProbesDownRegulated( int numberOfProbesDownRegulated ) {
        this.numberOfProbesDownRegulated = numberOfProbesDownRegulated;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId( Long analysisId ) {
        this.analysisId = analysisId;
    }

    public List<List<Double>> getVisualizationValues() {
        return visualizationValues;
    }

    public void setVisualizationValues( List<List<Double>> visualizationValues ) {
        this.visualizationValues = visualizationValues;
    }

    public void setVisualizationValue ( int geneGroupIndex, int geneIndex, Double value ) {
        this.visualizationValues.get( geneGroupIndex ).add( geneIndex, value );
    }    
    
    public List<List<Double>> getpValues() {
        return pValues;
    }

    public void setpValues( List<List<Double>> pValues ) {
        this.pValues = pValues;
    }

    public void setPvalue( int geneGroupIndex, int geneIndex, Double pValue ) {
        this.pValues.get( geneGroupIndex ).add( geneIndex, pValue );
    }
    
    public List<List<Integer>> getNumberOfProbes() {
        return numberOfProbes;
    }

    public void setNumberOfProbes( List<List<Integer>> numberOfProbes ) {
        this.numberOfProbes = numberOfProbes;
    }

    public void setNumberOfProbes( int geneGroupIndex, int geneIndex, Integer numberOfProbes ) {
        this.numberOfProbes.get( geneGroupIndex ).add( geneIndex, numberOfProbes );
    }
    
    public Map<Long,String> getContrastsFactorValues() {
        return contrastsFactorValues;
    }

    public void addContrastsFactorValue ( long factorValueId, String factorValueName ) {
        this.contrastsFactorValueIds.add ( factorValueId );
        this.contrastsFactorValues.put( factorValueId, factorValueName );
    }
    
    public void setContrastsFactorValues( Map<Long,String> contrastsFactorValues ) {
        this.contrastsFactorValues = contrastsFactorValues;
    }

    public String getBaselineFactorValue() {
        return baselineFactorValue;
    }

    public void setBaselineFactorValue( String baselineFactorValue ) {
        this.baselineFactorValue = baselineFactorValue;
    }

    public List<List<List<Double>>> getContrastsVisualizationValues() {
        return contrastsVisualizationValues;
    }

    public void setContrastsVisualizationValues( List<List<List<Double>>> contrastsVisualizationValues ) {
        this.contrastsVisualizationValues = contrastsVisualizationValues;
    }

    public List<List<List<Double>>> getConstrastsFoldChangeValues() {
        return constrastsFoldChangeValues;
    }

    public void setConstrastsFoldChangeValues( List<List<List<Double>>> constrastsFoldChangeValues ) {
        this.constrastsFoldChangeValues = constrastsFoldChangeValues;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName( String datasetName ) {
        this.datasetName = datasetName;
    }

    public String getDatasetLink() {
        return datasetLink;
    }

    public void setDatasetLink( String datasetLink ) {
        this.datasetLink = datasetLink;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId( Long datasetId ) {
        this.datasetId = datasetId;
    }

    public String getFactorName() {
        return factorName;
    }

    public void setFactorName( String factorName ) {
        this.factorName = factorName;
    }

    public String getFactorCategory() {
        return this.factorCategory;
    }
    
    public void setFactorCategory( String name ) {
        this.factorCategory = name;
    }

    public List<Long> getContrastsFactorValueIds() {
        return this.contrastsFactorValueIds;        
    }

    
}
