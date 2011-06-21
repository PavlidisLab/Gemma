package ubic.gemma.web.visualization;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.util.StringUtils;

import edu.emory.mathcs.backport.java.util.Collections;

// This object represents a column in visualization. 
// 
public class DifferentialExpressionAnalysisResultSetVisualizationValueObject {    
    private int numberOfGeneGroups;
    private int[] geneGroupSizes;    
    private int numberOfFactorValues;
    
    // [geneGroupIndex] [geneIndex]
    //private List<List<String>> geneNames;
    //private List<List<Long>> geneIds;
    
    private String datasetName;
    private String datasetShortName;
    private String datasetLink;
    private Long datasetId;
    private Long analysisId;
    private Long resultSetId;    
    private String subsetFactor;
    private String subsetFactorValue;
    private Long factorId;
    private String factorName;

    private String factorCategory;

    private String factorDescription;
    // Various metrics/scores to be use for display/sorting/filtering.
    private int numberOfProbesTotal;
    private int numberOfProbesDiffExpressed;
    
    private int numberOfProbesUpRegulated;

    private int numberOfProbesDownRegulated;

    private int numberOfGenesDiffExpressedFromGeneGroup;    
    private boolean analysisNotRun = false;
    // 
    private List<List<Double>> visualizationValues;
    private List<List<Double>> pValues;
    
    // Number of probes per gene ( per dataset ). Can be used to show genes with multiple probes on the array.  
    private List<List<Integer>> numberOfProbes;
    
    // Contrasts:    
    private Map<Long, String> contrastsFactorValues;
        
    // Data
    
    private List<Long> contrastsFactorValueIds;
    private String baselineFactorValue;
    private Long baselineFactorValueId;
    
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
    
    public void addContrastsFactorValue ( long factorValueId, String factorValueName ) {
        this.contrastsFactorValueIds.add ( factorValueId );
        this.contrastsFactorValues.put( factorValueId, factorValueName );                
    }

    public Long getBaselineFactorValueId() {
        return baselineFactorValueId;
    }

    public void setBaselineFactorValueId( Long baselineFactorValueId ) {
        this.baselineFactorValueId = baselineFactorValueId;
    }

    public Long getResultSetId() {
        return resultSetId;
    }

    public void setResultSetId(Long resultSetId) {
        this.resultSetId = resultSetId;
    }
    
    public Long getAnalysisId() {
        return analysisId;
    }
    public boolean getAnalysisNotRun() {
        return analysisNotRun;
    }
   
    public String getBaselineFactorValue() {
        return baselineFactorValue;
    }    
    
//  List<List<List<Double>>>  contrastsVisualizationValues    = new ArrayList<List<List<Double>>>();
//  List<List<List<Double>>>  contrastsFoldChangeValues       = new ArrayList<List<List<Double>>>();                        
    
    public List<List<List<Double>>> getConstrastsFoldChangeValues() {
        return constrastsFoldChangeValues;
    }

    public List<Long> getContrastsFactorValueIds() {
        return this.contrastsFactorValueIds;        
    }

    public Map<Long,String> getContrastsFactorValues() {
        return contrastsFactorValues;
    }

    public List<List<List<Double>>> getContrastsVisualizationValues() {
        return contrastsVisualizationValues;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public String getDatasetLink() {
        return datasetLink;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getDatasetShortName() {
        return datasetShortName;
    }

    public String getFactorCategory() {
        return this.factorCategory;
    }

    public String getFactorDescription() {
        return factorDescription;
    }

    public Long getFactorId() {
        return factorId;
    }

    public String getFactorName() {
        return factorName;
    }

    public List<List<Integer>> getNumberOfProbes() {
        return numberOfProbes;
    }

    public int getNumberOfProbesDiffExpressed() {
        return numberOfProbesDiffExpressed;
    }

    public int getNumberOfProbesDownRegulated() {
        return numberOfProbesDownRegulated;
    }    
    
    public int getNumberOfProbesTotal() {
        return numberOfProbesTotal;
    }

    public int getNumberOfProbesUpRegulated() {
        return numberOfProbesUpRegulated;
    }

    public List<List<Double>> getpValues() {
        return pValues;
    }
    
    public List<List<Double>> getVisualizationValues() {
        return visualizationValues;
    }

    public void setAnalysisId( Long analysisId ) {
        this.analysisId = analysisId;
    }

    public void setAnalysisNotRun( boolean analysisNotRun ) {
        this.analysisNotRun = analysisNotRun;
    }
    
    public void setBaselineFactorValue( String baselineFactorValue ) {
        this.baselineFactorValue = baselineFactorValue;
    }

    public void setConstrastsFoldChangeValues( List<List<List<Double>>> constrastsFoldChangeValues ) {
        this.constrastsFoldChangeValues = constrastsFoldChangeValues;
    }
    
    public void setContrastsFactorValues( Map<Long,String> contrastsFactorValues ) {
        this.contrastsFactorValues = contrastsFactorValues;
    }

    public void setContrastsVisualizationValues( List<List<List<Double>>> contrastsVisualizationValues ) {
        this.contrastsVisualizationValues = contrastsVisualizationValues;
    }

    public void setDatasetId( Long datasetId ) {
        this.datasetId = datasetId;
    }

    public void setDatasetLink( String datasetLink ) {
        this.datasetLink = datasetLink;
    }

    public void setDatasetName( String datasetName ) {
        this.datasetName = datasetName;
    }

    public void setDatasetShortName( String datasetShortName ) {
        this.datasetShortName = datasetShortName;
    }

    public void setFactorCategory( String name ) {
        this.factorCategory = name;
    }

    public void setFactorDescription( String factorDescription ) {
        this.factorDescription = factorDescription;
    }

    public void setFactorId( Long factorId ) {
        this.factorId = factorId;
    }

    public void setFactorName( String factorName ) {
        this.factorName = factorName;
    }

    public void setNumberOfProbes( int geneGroupIndex, int geneIndex, Integer numberOfProbes ) {
        this.numberOfProbes.get( geneGroupIndex ).add( geneIndex, numberOfProbes );
    }

    public void setNumberOfProbes( List<List<Integer>> numberOfProbes ) {
        this.numberOfProbes = numberOfProbes;
    }

    public void setNumberOfProbesDiffExpressed( int numberOfProbesDiffExpressed ) {
        this.numberOfProbesDiffExpressed = numberOfProbesDiffExpressed;
    }

    public void setNumberOfProbesDownRegulated( int numberOfProbesDownRegulated ) {
        this.numberOfProbesDownRegulated = numberOfProbesDownRegulated;
    }

    public void setNumberOfProbesTotal( int numberOfProbesTotal ) {
        this.numberOfProbesTotal = numberOfProbesTotal;
    }

    public void setNumberOfProbesUpRegulated( int numberOfProbesUpRegulated ) {
        this.numberOfProbesUpRegulated = numberOfProbesUpRegulated;
    }

    public void setPvalue( int geneGroupIndex, int geneIndex, Double pValue ) {
        this.pValues.get( geneGroupIndex ).add( geneIndex, pValue );
    }

    public void setpValues( List<List<Double>> pValues ) {
        this.pValues = pValues;
    }
    
    public void setVisualizationValue ( int geneGroupIndex, int geneIndex, Double value ) {
        this.visualizationValues.get( geneGroupIndex ).add( geneIndex, value );
    }

    public void setVisualizationValues( List<List<Double>> visualizationValues ) {
        this.visualizationValues = visualizationValues;
    }

    public String toText() {
        DecimalFormat df = new DecimalFormat("#.##");
        StringBuilder text = new StringBuilder();
        text.append("|"+datasetShortName + "|"+ factorName + "|");
        for (Long fvId : this.contrastsFactorValues.keySet() ) {
            text.append( this.contrastsFactorValues.get( fvId ).trim()+"," );
        }
        text.append("|");
        for ( List<Double> pValueGroup : this.pValues ) {
            for ( Double pValue : pValueGroup ) {
                if (pValue == null) {
                    text.append("NA|");
                } else {
                    text.append(df.format(pValue) + "|");                    
                }
            }
        } 
        text.append("\n");
        return text.toString();
    }
}
