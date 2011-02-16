/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import edu.emory.mathcs.backport.java.util.Collections;

import ubic.gemma.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionMetaAnalysisValueObject;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.analysis.ContrastResult;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionSummaryValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentExperimentalFactorValueObject;
import ubic.gemma.web.view.TextView;

/**
 * A controller used to get differential expression analysis and meta analysis results.
 * 
 * @author keshav
 * @version $Id$ *
 */
public class DifferentialExpressionSearchController extends BaseFormController {

    private static final double DEFAULT_THRESHOLD = 0.01;

    private static final int MAX_GENES_PER_QUERY = 20;

    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    private GeneDifferentialExpressionService geneDifferentialExpressionService = null;
    private GeneService geneService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private ExpressionExperimentSetService expressionExperimentSetService = null;
    
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private GeneSetService geneSetService;
    
    @Autowired
    private TaxonService taxonService;
    
    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;
    
    @Autowired
    private ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao differentialExpressionAnalysisResultDao;
    
    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold ) {

        return this.getDifferentialExpression( geneId, threshold, null );
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold,
            Integer limit ) {

        Gene g = geneService.thaw( geneService.load( geneId ) );

        if ( g == null ) {
            return new ArrayList<DifferentialExpressionValueObject>();
        }

        return geneDifferentialExpressionService.getDifferentialExpression( g, threshold, limit );
    }

    /**
     * AJAX entry which returns differential expression results for the gene with the given id, in the selected factors,
     * at the given significance threshold.
     * 
     * @param geneId
     * @param threshold corrected pvalue threshold (normally this means FDR)
     * @param factorMap
     * @deprecated as far as I can tell this is not used.
     * @return
     */
    @Deprecated
    public Collection<DifferentialExpressionValueObject> getDifferentialExpressionForFactors( Long geneId,
            double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        if ( factorMap.isEmpty() || geneId == null ) {
            return null;
        }

        Gene g = geneService.load( geneId );
        Collection<DifferentialExpressionValueObject> result = geneDifferentialExpressionService
                .getDifferentialExpression( g, threshold, factorMap );

        return result;
    }

    // One column + Multiple columns for contrasts
    // Multiple gene groups
    public class DifferentialExpressionAnalysisResultSetValueObject {
        // [geneGroupIndex] [geneIndex]
        String [][] geneNames;
        
        Double [][] visualizationValues;
        Double [][] pValues;

        Integer [][] numberOfProbes; // to show that there are more probes for this gene
        
        String [] contrastsFactorValues;
        String baslineFactorValue;
        // [geneGroupIndex][geneIndex][contrastIndex]
        Double [][][] contrastsVisualizationValues;
        Double [][][] constrastsPvalues;
        
        String datasetName;
        String datasetLink;
        String datasetId;
        
        String factorName;
        
        // Various metrics/scores to be use for display/sorting/filtering.
        Map<String, String> metrics;

        public String[][] getGeneNames() {
            return geneNames;
        }

        public void setGeneNames( String[][] geneNames ) {
            this.geneNames = geneNames;
        }

        public Double[][] getVisualizationValues() {
            return visualizationValues;
        }

        public void setVisualizationValues( Double[][] visualizationValues ) {
            this.visualizationValues = visualizationValues;
        }

        public Double[][] getpValues() {
            return pValues;
        }

        public void setpValues( Double[][] pValues ) {
            this.pValues = pValues;
        }

        public Integer[][] getNumberOfProbes() {
            return numberOfProbes;
        }

        public void setNumberOfProbes( Integer[][] numberOfProbes ) {
            this.numberOfProbes = numberOfProbes;
        }

        public String[] getContrastsFactorValues() {
            return contrastsFactorValues;
        }

        public void setContrastsFactorValues( String[] contrastsFactorValues ) {
            this.contrastsFactorValues = contrastsFactorValues;
        }

        public String getBaslineFactorValue() {
            return baslineFactorValue;
        }

        public void setBaslineFactorValue( String baslineFactorValue ) {
            this.baslineFactorValue = baslineFactorValue;
        }

        public Double[][][] getContrastsVisualizationValues() {
            return contrastsVisualizationValues;
        }

        public void setContrastsVisualizationValues( Double[][][] contrastsVisualizationValues ) {
            this.contrastsVisualizationValues = contrastsVisualizationValues;
        }

        public Double[][][] getConstrastsPvalues() {
            return constrastsPvalues;
        }

        public void setConstrastsPvalues( Double[][][] constrastsPvalues ) {
            this.constrastsPvalues = constrastsPvalues;
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

        public String getDatasetId() {
            return datasetId;
        }

        public void setDatasetId( String datasetId ) {
            this.datasetId = datasetId;
        }

        public String getFactorName() {
            return factorName;
        }

        public void setFactorName( String factorName ) {
            this.factorName = factorName;
        }

        public Map<String, String> getMetrics() {
            return metrics;
        }

        public void setMetrics( Map<String, String> metrics ) {
            this.metrics = metrics;
        }
        
    }
    
    public class DifferentialExpressionMetaVisualizationValueObject {
        List<List<DifferentialExpressionAnalysisResultSetValueObject>> resultSetValueObjects;
        String[][] geneNames;
        List<List<String>> analysisLabels;        
        
        public List<List<String>> getAnalysisLabels() {
            return analysisLabels;
        }

        public void setAnalysisLabels( List<List<String>> analysisLabels ) {
            this.analysisLabels = analysisLabels;
        }

        public String[][] getGeneNames() {
            return geneNames;
        }

        public void setGeneNames( String[][] geneNames ) {
            this.geneNames = geneNames;
        }

        public List<List<DifferentialExpressionAnalysisResultSetValueObject>> getResultSetValueObjects() {
            return resultSetValueObjects;
        }

        public void setResultSetValueObjects( List<List<DifferentialExpressionAnalysisResultSetValueObject>> resultSetValueObjects ) {
            this.resultSetValueObjects = resultSetValueObjects;
        }
                
    }    
    
    public class DifferentialExpressionResultsValueObject {
        String [][] datasetLabels;
        String [][] analysisLabels;
        String [][] geneLabels;
        Double [][][][] pvalues;
        Double [][][][] contrasts;
        
        List<String> datasetPlusFactorLabels;
        
        List<String> datasetIds;
        List<String> baselineFactorValues;
        List<String> contrastFactorValues;

        List<String> factorCategories;
        
        Map<String, Map<String, Double>> pValues;
        Map<String, List<Double>> foldChanges;
        
        Integer[][] pieValues;
        
        public Integer[][] getPieValues() {
            return pieValues;
        }
        public void setPieValues( Integer[][] pieValues ) {
            this.pieValues = pieValues;
        }
        public List<String> getDatasetPlusFactorLabels() {
            return datasetPlusFactorLabels;
        }
        public void setDatasetPlusFactorLabels( List<String> datasetPlusFactorLabels ) {
            this.datasetPlusFactorLabels = datasetPlusFactorLabels;
        }

        public String[][] getAnalysisLabels() {
            return analysisLabels;
        }
        
        public void setAnalysisLabels( String[][] analysisLabels ) {
            this.analysisLabels = analysisLabels;
        }        
        
        public Double[][][][] getPvalues() {
            return pvalues;
        }
        public void setPvalues( Double[][][][] pvalues ) {
            this.pvalues = pvalues;
        }
        
        public String[][] getDatasetLabels() {
            return datasetLabels;
        }
        
        public void setDatasetLabels( String[][] datasetLabels ) {
            this.datasetLabels = datasetLabels;
        }
        
        public List<String> getDatasetIds() {
            return datasetIds;
        }
        
        public void setDatasetIds( List<String> datasetIds ) {
            this.datasetIds = datasetIds;
        }
        public List<String> getBaselineFactorValues() {
            return baselineFactorValues;
        }
        public void setBaselineFactorValues( List<String> baselineFactorValues ) {
            this.baselineFactorValues = baselineFactorValues;
        }
        public List<String> getContrastFactorValues() {
            return contrastFactorValues;
        }
        public void setContrastFactorValues( List<String> contrastFactorValues ) {
            this.contrastFactorValues = contrastFactorValues;
        }
        public List<String> getFactorCategories() {
            return factorCategories;
        }
        public void setFactorCategories( List<String> factorCategories ) {
            this.factorCategories = factorCategories;
        }
        public String[][] getGeneLabels() {
            return geneLabels;
        }
        public void setGeneLabels( String[][] geneLabels ) {
            this.geneLabels = geneLabels;
        }
        public Map<String, Map<String, Double>> getpValues() {
            return pValues;
        }
        public void setpValues( Map<String, Map<String, Double>> pValues ) {
            this.pValues = pValues;
        }
        public Map<String, List<Double>> getFoldChanges() {
            return foldChanges;
        }
        public void setFoldChanges( Map<String, List<Double>> foldChanges ) {
            this.foldChanges = foldChanges;
        }               
    }
    
    
    public DifferentialExpressionMetaVisualizationValueObject getVisualizationTestData() {
        // factor labels
        // factor value labels
        
        List<List<String>> analysisLabels = new ArrayList<List<String>>();
        
        
        int numberOfGeneGroups = 2;
        String[][] geneNames = new String [2][3];
        geneNames[0][0] = "CAPG";
        geneNames[0][1] = "PIGH";
        geneNames[0][2] = "PIGH";
        
        geneNames[1][0] = "FKBP2";
        geneNames[1][1] = "RPS16";
        geneNames[1][2] = "CTSL2";
        
        // 1 analysis id
        // brain 2003 Sampling Location
        List<Long> ids = new ArrayList<Long>();
        ids.add( Long.valueOf(1752) );

        Taxon taxon = taxonService.findByCommonName( "salmonid" );

        Gene[][] genes = new Gene[2][3];
        
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 3; y++) {
                genes[x][y] = geneService.findByOfficialSymbol( geneNames[x][y], taxon );                            
            }            
        }
        
        
        Map<Long, DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService.findByInvestigationIds(ids);
        DifferentialExpressionAnalysis analysis = analyses.get( Long.valueOf(1752) );               
        differentialExpressionAnalysisService.thaw( analysis );
        Collection<ExpressionAnalysisResultSet> sets = analysis.getResultSets();
                
       List<DifferentialExpressionAnalysisResultSetValueObject> callResults = new ArrayList<DifferentialExpressionAnalysisResultSetValueObject>();
                
        for (ExpressionAnalysisResultSet resultSet : sets) {        
            // We skip result sets containing interactions.            
            if (resultSet.getExperimentalFactors().size() > 1) continue;

            for (ExperimentalFactor factor : resultSet.getExperimentalFactors()) {                                
                log.info("Factor description: " + factor.getDescription());                
                log.info("Factor values: "+ factor.getFactorValues().size());
                
                Double [][][] contrastsValues = new Double [numberOfGeneGroups][3][ factor.getFactorValues().size()];
                String [] constrastFactorValues = new String [factor.getFactorValues().size()];
                
                Map<String, Integer> factorValueToIndex = new HashMap<String, Integer>();
                int tempIndex = 0;                              
                for (FactorValue fvalue : factor.getFactorValues() ) {
                    FactorValueValueObject factorValueVO = new FactorValueValueObject(fvalue);
                    factorValueToIndex.put( factorValueVO.getValue(), tempIndex);
                    constrastFactorValues[tempIndex] = factorValueVO.getValue();
                    tempIndex ++;
                }
                
                FactorValueValueObject baselineFactorValueVO = new FactorValueValueObject(resultSet.getBaselineGroup());                            
                String baselineValue = baselineFactorValueVO.getValue();
                
                Integer[][] numProbes = new Integer[numberOfGeneGroups][3];        
                Double [][] pValues   = new Double [numberOfGeneGroups][3];
                Double [][] vizValues = new Double [numberOfGeneGroups][3];

                for (int geneGroupIndex = 0; geneGroupIndex < 2; geneGroupIndex++) {                    
                    for (int geneIndex = 0; geneIndex < 3; geneIndex++) {
                        Gene gene = genes[geneGroupIndex][geneIndex];
                        List<Long> resultsForGene = differentialExpressionResultService.findGeneInResultSets(gene, resultSet, 0.9, 200);       
                        if (resultsForGene == null || resultsForGene.isEmpty()) {
                            log.info( "HUYAK!!!!");
                            continue;
                        }

                        Long probeResultId = resultsForGene.get( 0 );
                        // Grab one with smallest p-value;
                        ProbeAnalysisResult result = differentialExpressionAnalysisResultDao.load (probeResultId  );            
                        differentialExpressionResultService.thaw( result );
                        int numberOfprobesFound = resultsForGene.size();
                        numProbes[geneGroupIndex][geneIndex] = numberOfprobesFound;
                        pValues[geneGroupIndex][geneIndex] = result.getCorrectedPvalue();
                        double visualizationValue = calculateVisualizationValueBasedOnPvalue(result.getCorrectedPvalue());
                        vizValues[geneGroupIndex][geneIndex] = visualizationValue;

                        log.info("gene: "+geneIndex+", probes: "+numberOfprobesFound+ ", pValue: "+result.getCorrectedPvalue() +" # contrasts" +result.getContrasts().size());                        

                        for (ContrastResult  cr : result.getContrasts()) {                            
                            FactorValueValueObject factorValueVO = new FactorValueValueObject(cr.getFactorValue());
                            String key = factorValueVO.getValue();                            
                            log.info( "factor value: "+factorValueVO.getValue() );
                            if (cr.getPvalue() < 0.01) {
                                if ( cr.getLogFoldChange() < 0 ) {
                                    contrastsValues[geneGroupIndex][geneIndex][factorValueToIndex.get(key)] = (-1)*visualizationValue;
                                } else {
                                    contrastsValues[geneGroupIndex][geneIndex][factorValueToIndex.get(key)] = visualizationValue;
                                }
                            } else {
                                contrastsValues[geneGroupIndex][geneIndex][factorValueToIndex.get(key)] = 0.0;                                
                            }                                                        
                        }                                                                                                
                    }        
                }
                // Create new DifferentialExpressionAnalysisResultSetValueObject and set its fields...        
                DifferentialExpressionAnalysisResultSetValueObject vizColumnValueObject = new DifferentialExpressionAnalysisResultSetValueObject();
                vizColumnValueObject.setFactorName( factor.getDescription() );
                vizColumnValueObject.setContrastsVisualizationValues( contrastsValues );
                vizColumnValueObject.setContrastsFactorValues( constrastFactorValues );
                vizColumnValueObject.setpValues( pValues );
                vizColumnValueObject.setVisualizationValues( vizValues );
                vizColumnValueObject.setBaslineFactorValue( baselineValue );
                vizColumnValueObject.setGeneNames( geneNames );
                callResults.add (vizColumnValueObject);
            }
        }
     
        DifferentialExpressionMetaVisualizationValueObject callResult = new DifferentialExpressionMetaVisualizationValueObject();
        List<List<DifferentialExpressionAnalysisResultSetValueObject>>  datasetGroupList = new ArrayList<List<DifferentialExpressionAnalysisResultSetValueObject>>();
        datasetGroupList.add( callResults );
        callResult.setResultSetValueObjects( datasetGroupList );
        callResult.setGeneNames( geneNames );
                        
        return callResult;        
    }        
    
    // Get list of taxon, eeSet ids and list of geneSet ids.
    public DifferentialExpressionResultsValueObject getDataForNewVisualizationTest(Long taxonId, List<Long> datasetGroupIds, List<Long> geneGroupIds) {
        // Prepare dataset groups     
        List<Collection<BioAssaySet>> experiments = new ArrayList<Collection<BioAssaySet>>();          
        for (long datasetGroupId : datasetGroupIds) {
            ExpressionExperimentSet datasetGroup = expressionExperimentSetService.load( datasetGroupId );
            Collection<BioAssaySet> experimentsInsideGroup = datasetGroup.getExperiments();                
            experiments.add( experimentsInsideGroup );
        }
        
        // Prepare gene groups
        Taxon human = taxonService.load( taxonId );        

        List<List<Gene>> genes = new ArrayList<List<Gene>>();          
        for (long geneGroupId : geneGroupIds) {
            GeneSet geneSet = geneSetService.load( geneGroupId );
            List<Gene> genesInsideSet = new ArrayList<Gene>();
            for (GeneSetMember memberGene : geneSet.getMembers()) {                
                if (memberGene.getGene() != null) genesInsideSet.add(memberGene.getGene());                
            }                    
            genes.add(genesInsideSet);
        }                        
                                
        return getDifferentialExpressionForGenesAndExperimentSet(genes, experiments);
    }
        
    public DifferentialExpressionResultsValueObject getDifferentialExpressionForGenesAndExperimentSet(List<List<Gene>> genes, List<Collection<BioAssaySet>> experiments) {        
        // Text labels we display.
        List<List<String>> analysisLabels = new ArrayList<List<String>>(experiments.size());
        List<List<String>> datasetLabels = new ArrayList<List<String>>(experiments.size());
        List<List<String>> geneNames = new ArrayList<List<String>>();

        // pValues for each cell.
        List<Map<String, Map<String, Double>>> pValues = new ArrayList<Map<String, Map<String, Double>>>(experiments.size());   //new HashMap<String, Map<String, Double>>();
        
        // Stores whether or not probe is missing from ???TODO
        Map<String, Boolean> missingProbeHelperMap = new HashMap<String,Boolean>();         
                
        // Iterate over all genes and all experiments. Fetch pValue data.        
        int datasetGroupIndex = 0;
        for (Collection<BioAssaySet> experimentsInGroup : experiments) {
            pValues.add( datasetGroupIndex, new HashMap <String, Map<String, Double>>());       
            for (Collection<Gene> geneGroup : genes) {            
                List<String> geneNamesForGroup = new ArrayList<String>();                            
                for (Gene gene : geneGroup) {                            
                    String currentGeneSymbol = gene.getOfficialSymbol(); 
                    geneNamesForGroup.add( currentGeneSymbol );                
                    Map<BioAssaySet, List<ProbeAnalysisResult>> resultsForGene = differentialExpressionResultService.find( gene, experimentsInGroup, 0.7, 200);
                    for (BioAssaySet e : experimentsInGroup ) {                        
                        ExpressionExperiment experiment = (ExpressionExperiment) e;                    
                        // We found some probes for a given gene and dataset.
                        if ( resultsForGene.keySet().contains(e) ) {                                                                                    
                            ProbeAnalysisResult minPvalueResult = findMinPvalue(resultsForGene.get( e ));                            
                            Collection<ContrastResult> contrasts = minPvalueResult.getContrasts();
                            double visualizationValue = calculateVisualizationValueBasedOnPvalue( minPvalueResult.getCorrectedPvalue() );
                            //log.info( "Experiment: "+ experiment.getShortName() + ", contrasts: "+contrasts.size() + ", probes: "+probeList.size());                            
                            getPvaluesForContrasts(currentGeneSymbol, contrasts, visualizationValue, experiment, pValues.get(datasetGroupIndex));                        
                        } else { //We didn't find any probes.
                            markProbeAsMissing(currentGeneSymbol, experiment.getShortName(), missingProbeHelperMap);
                        }
                    }
                }
                if (datasetGroupIndex == 0) geneNames.add( geneNamesForGroup );
            }
            datasetGroupIndex++;
        }                
        // Post-process results:

        // Hacky temporary map TODO
        List<List<String>> datasetPlusFactorLabels = new ArrayList<List<String>>(experiments.size());
        
        List<List<Double>> datasetExpressionScores = new ArrayList<List<Double>>(experiments.size());
        Map<String, Double> geneScores = new HashMap<String, Double>();
        
        calculateDatasetExpressionScores(pValues, datasetExpressionScores, geneScores);
        
        Map<String, Integer> pieChartValues =  new HashMap<String, Integer>();
        specificityMetric (experiments, pieChartValues);
        List<List<Integer>> pieValuesList =  new ArrayList<List<Integer>>();
        
        for (datasetGroupIndex = 0; datasetGroupIndex < experiments.size(); datasetGroupIndex++) {
            datasetLabels.add( new ArrayList<String>() );
            analysisLabels.add( new ArrayList<String>() );
            
            List<String> currentDatasetPlusFactorLabels = new ArrayList<String>(pValues.get(datasetGroupIndex).keySet());
            Collections.sort ( currentDatasetPlusFactorLabels );                
            //currentDatasetPlusFactorLabels = sortUsingScores(datasetExpressionScores.get( datasetGroupIndex ), currentDatasetPlusFactorLabels);
            datasetPlusFactorLabels.add (datasetGroupIndex, currentDatasetPlusFactorLabels );
            
            pieValuesList.add( new ArrayList<Integer>() );
            for (String dsPlusfactor : currentDatasetPlusFactorLabels) {
                String[] parts = dsPlusfactor.split( "&&&", -1 );
                String ds = parts[0];
                String id = parts[1];
                String value = parts[2];            
                if (value.equals("")) id = "null";
                datasetLabels.get( datasetGroupIndex ).add( ds );          
                analysisLabels.get( datasetGroupIndex ).add( value );
                if ( pieChartValues.get( dsPlusfactor ) != null) {
                    pieValuesList.get( datasetGroupIndex ).add ( pieChartValues.get( dsPlusfactor ));
                } else {
                    pieValuesList.get( datasetGroupIndex ).add (0);
                }
            }                       
        }

        // future TODO: sort genes in some sensible order
        for (List<String> geneNamesGroup : geneNames) {
            List<Double> geneScoreList = new ArrayList<Double>();
            for (String geneName : geneNamesGroup) {
                if (geneScores.get(geneName) == null) {
                    geneScoreList.add( new Double(0.0)  );                
                } else {
                    geneScoreList.add(geneScores.get(geneName)  );
                }            
            }
            //sortUsingScores(geneScoreList, geneNamesGroup);            
        }
                                
        List<String> missingAnalysisList = new ArrayList<String>();
        List<String> absentContrastsList = new ArrayList<String>();
                        
        
        // Building visualization results object that heatmaplib.js (BioHeatmap) understands.
        DifferentialExpressionResultsValueObject resultData = new DifferentialExpressionResultsValueObject();
                
        Integer[][] pieValuesArray = new Integer[experiments.size()][];
        datasetGroupIndex = 0; 
        for (List<Integer> currentDatasetPies : pieValuesList) {
            pieValuesArray[datasetGroupIndex] = currentDatasetPies.toArray( new Integer[]{} );
            datasetGroupIndex++;
        }                
        for (int i=0; i<pieValuesArray[0].length; i++) {
            log.info(" >>>>"+ pieValuesArray[0][i]);
        }
        
        
        String[][] labels = new String[experiments.size()][];        
        datasetGroupIndex = 0;    
        for (List<String> currentDatasetLabels : datasetLabels) {
            labels[datasetGroupIndex] = currentDatasetLabels.toArray( new String[]{} );
            datasetGroupIndex++;
        }                
        resultData.setDatasetLabels( labels );

        labels = new String[experiments.size()][];
        datasetGroupIndex = 0;    
        for (List<String> currentAnalysisLabels : analysisLabels) {
            labels[datasetGroupIndex] = currentAnalysisLabels.toArray( new String[]{} ); 
            datasetGroupIndex++;
        }                
        resultData.setAnalysisLabels( labels );
                
        labels = new String[genes.size()][];        
        int geneGroupIndex = 0;
        for (List<String> currentGeneLabels : geneNames) {
            labels[geneGroupIndex] = currentGeneLabels.toArray( new String[]{} );                    
            geneGroupIndex++;
        }                                
        resultData.setGeneLabels( labels );
        
        Double[][][][] vizValues = prepareVisualizationData(geneNames, datasetPlusFactorLabels, pValues, missingProbeHelperMap);
        resultData.setPvalues( vizValues );
        resultData.setPieValues( pieValuesArray );
        return resultData;
    }
    
    private double calculateVisualizationValueBasedOnPvalue(double pValue) {
        // Color contrasts with low enough p values, the rest will be black.
        double visualizationValue = 0.0;
        pValue = Math.abs(pValue);
        if (pValue < 0.2 && pValue >= 0.1) visualizationValue = 0.1;
        else if (pValue < 0.1 && pValue >= 0.05 ) visualizationValue = 0.2;         
        else if (pValue < 0.05 && pValue >= 0.01 ) visualizationValue = 0.5; 
        else if (pValue < 0.01 && pValue >= 0.001 ) visualizationValue = 0.6;
        else if (pValue < 0.001 && pValue >= 0.0001 ) visualizationValue = 0.8;
        else if (pValue < 0.0001 && pValue >= 0.00001 ) visualizationValue = 0.9;
        else if (pValue < 0.00001  ) visualizationValue = 1;
        return visualizationValue;
    }

    private void calculateDatasetExpressionScores(List<Map<String, Map<String, Double>>> pValues, List<List<Double>> datasetExpressionScores, Map<String, Double> geneScores) {        
        for (Map<String, Map<String, Double>> currentPvaluesMap : pValues) {
            List<Double> currentDatasetExpressionScores = new ArrayList<Double>();
            for (Map<String, Double> genePvaluesMap : currentPvaluesMap.values()) {
               double score = 0;         
               for (String geneName : genePvaluesMap.keySet()) {
                   Double value = Math.abs( genePvaluesMap.get( geneName ) );
                   score += value;
                   Double savedGeneScore = geneScores.get( geneName );
                   if (savedGeneScore == null)  {
                       geneScores.put( geneName, Math.abs( value ) );                   
                   } else {
                       geneScores.put( geneName, savedGeneScore + Math.abs(value) );
                   }
               }
               currentDatasetExpressionScores.add(score);
               log.info( "Score:" + score );
           }
           datasetExpressionScores.add( currentDatasetExpressionScores ); 
        }        
    }
        
    private List<String> sortUsingScores(List<Double> scores,  List<String> labels) {        
        for (String l : labels) {
            log.info(l);
        }

        boolean swapped = true;
        int j = 0;

        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < labels.size() - j; i++) {                                       
                if (scores.get(i).doubleValue() < scores.get(i+1).doubleValue() ) {
                    java.util.Collections.swap( scores, i, i+1 );
                    java.util.Collections.swap( labels, i, i+1 );
                    swapped = true;
                }                                            
            }                
        } 

        for (double l : scores) {
            log.info(">"+l);
        }
        return labels;
    }
    
    private Double [][][][] prepareVisualizationData(List<List<String>> geneNames, List<List<String>> datasetPlusFactorLabels,List<Map<String, Map<String, Double>>> pValues, Map<String, Boolean> missingProbeHelperMap ){
        // Size: number of gene groups, dataset groups, max columns, max rows. 
        Double [][][][] visualizationValues = new Double[geneNames.size()][datasetPlusFactorLabels.size()][][]; //[datasetPlusFactorLabels.size()+1][Math.max(geneNames[0].size(),geneNames[1].size())+1];
        for (int datasetGroupIndex = 0; datasetGroupIndex < datasetPlusFactorLabels.size(); datasetGroupIndex++) {
            for (int geneGroupIndex = 0; geneGroupIndex < geneNames.size(); geneGroupIndex++) {   
                Double [][] subHeatMap = new Double[datasetPlusFactorLabels.get(datasetGroupIndex).size()][geneNames.get( geneGroupIndex ).size()];
                visualizationValues[geneGroupIndex][datasetGroupIndex] = subHeatMap;
                for (int i = 0; i < datasetPlusFactorLabels.get(datasetGroupIndex).size(); i++) {                                                
                    for (int j = 0; j < geneNames.get( geneGroupIndex ).size(); j++) {                        
                        Map<String, Double> geneNameToPvaluesRow = pValues.get( datasetGroupIndex ).get( datasetPlusFactorLabels.get( datasetGroupIndex ).get( i ) );
                        if (geneNameToPvaluesRow != null && geneNameToPvaluesRow.get( geneNames.get(geneGroupIndex).get(j) ) != null) {
                            visualizationValues[geneGroupIndex][datasetGroupIndex][i][j] = geneNameToPvaluesRow.get( geneNames.get(geneGroupIndex).get(j) );
                        } else {                                                
                            String[] parts = datasetPlusFactorLabels.get( datasetGroupIndex ).get( i ).split( "&&&", -1 );
                            String ds = parts[0];
                            if ( isProbeMissing(geneNames.get(geneGroupIndex).get( j ), ds, missingProbeHelperMap) ) { 
                                visualizationValues[geneGroupIndex][datasetGroupIndex][i][j] = null;
                            } else visualizationValues[geneGroupIndex][datasetGroupIndex][i][j] = Double.valueOf(0.0);
                        }
                    }
                }                                
            }
        }
                        
        return visualizationValues;
    }
    
//    private List<VisualizationColumnDataElement> prepareVisualizationDataColumns(int datasetGroupIndex, List<List<String>> geneNames, List<List<String>> datasetPlusFactorLabels,List<Map<String, Map<String, Double>>> pValues, Map<String, Boolean> missingProbeHelperMap ){
//            List<VisualizationColumnDataElement> columnElements = new ArrayList<VisualizationColumnDataElement>();            
//            
//            for (int geneGroupIndex = 0; geneGroupIndex < geneNames.size(); geneGroupIndex++) {   
//                VisualizationColumnDataElement columnElement = new VisualizationColumnDataElement();
//                columnElement.setDatasetName( datasetName );
//                columnElement.setFactorName( factorName );
//                columnElement.setFactorValueName( factorValueName );
//                Double [][] subHeatMap = new Double[datasetPlusFactorLabels.get(datasetGroupIndex).size()][geneNames.get( geneGroupIndex ).size()];
//                visualizationValues[geneGroupIndex][datasetGroupIndex] = subHeatMap;
//                for (int i = 0; i < datasetPlusFactorLabels.get(datasetGroupIndex).size(); i++) {                                                
//                    for (int j = 0; j < geneNames.get( geneGroupIndex ).size(); j++) {                        
//                        Map<String, Double> geneNameToPvaluesRow = pValues.get( datasetGroupIndex ).get( datasetPlusFactorLabels.get( datasetGroupIndex ).get( i ) );
//                        if (geneNameToPvaluesRow != null && geneNameToPvaluesRow.get( geneNames.get(geneGroupIndex).get(j) ) != null) {
//                            visualizationValues[geneGroupIndex][datasetGroupIndex][i][j] = geneNameToPvaluesRow.get( geneNames.get(geneGroupIndex).get(j) );
//                        } else {                                                
//                            String[] parts = datasetPlusFactorLabels.get( datasetGroupIndex ).get( i ).split( "&&&", -1 );
//                            String ds = parts[0];
//                            if ( isProbeMissing(geneNames.get(geneGroupIndex).get( j ), ds, missingProbeHelperMap) ) { 
//                                visualizationValues[geneGroupIndex][datasetGroupIndex][i][j] = null;
//                            } else visualizationValues[geneGroupIndex][datasetGroupIndex][i][j] = Double.valueOf(0.0);
//                        }
//                    }
//                }                                
//            }
//        }
//                        
//        return visualizationValues;
//    }
    
    private ProbeAnalysisResult findMinPvalue(List<ProbeAnalysisResult> probeList) {
        // Choose best(lowest) p-value when there are multiple probes per gene. 
        ProbeAnalysisResult minPvalueResult = null;
        for ( ProbeAnalysisResult probeAnalysis : probeList ) {
            if (minPvalueResult == null) {
                minPvalueResult = probeAnalysis;
            }
            if ( probeAnalysis.getCorrectedPvalue() < minPvalueResult.getCorrectedPvalue() ) {
                minPvalueResult = probeAnalysis;
            }                            
        }
        differentialExpressionResultService.thaw( minPvalueResult );
        
        return minPvalueResult;
    }
    
    private void getPvaluesForContrasts(String currentGeneSymbol, Collection<ContrastResult> contrasts, double visualizationValue, ExpressionExperiment experiment, Map<String, Map<String, Double>> pValues ) {
        for ( ContrastResult cr : contrasts ) {
            // Ignore interactions
            if ( cr.getSecondFactorValue() == null) {
                FactorValueValueObject vo = new FactorValueValueObject(cr.getFactorValue());                
                String key = experiment.getShortName()+"&&&"+cr.getFactorValue().getId()+"&&&"+vo.getValue();

                if ( pValues.get( key ) == null ) {
                    pValues.put(key, new HashMap<String, Double>());                            
                    //datasetPlusFactorLabels.add( key );
                }

                if (cr.getPvalue() < 0.01) {
                    if ( cr.getLogFoldChange() < 0 ) {
                        pValues.get( key ).put( currentGeneSymbol, (-1)*visualizationValue );
                    } else {
                        pValues.get( key ).put( currentGeneSymbol, visualizationValue );
                    }
                } else {
                    pValues.get( key ).put( currentGeneSymbol, 0.2 );                                
                }
            }
        }                
    }

    private void getPvaluesForContrasts2 (String currentGeneSymbol, Collection<ContrastResult> contrasts, double visualizationValue, ExpressionExperiment experiment, Map<String, Map<String, Double>> pValues ) {               
        for ( ContrastResult cr : contrasts ) {
            // Ignore interactions            
            if ( cr.getSecondFactorValue() == null) {
                //FactorValueValueObject vo = new FactorValueValueObject(cr.getFactorValue());                
                String key = experiment.getShortName()+"&&&"+"aaa"+"&&&"+cr.getFactorValue().getExperimentalFactor().getDescription();
                if ( pValues.get( key ) == null ) {
                    pValues.put(key, new HashMap<String, Double>());                            
                }

                if (cr.getPvalue() < 0.01) {
                    if ( cr.getLogFoldChange() < 0 ) {
                        pValues.get( key ).put( currentGeneSymbol, visualizationValue );
                    } else {
                        pValues.get( key ).put( currentGeneSymbol, visualizationValue );
                    }
                } else {
                    pValues.get( key ).put( currentGeneSymbol, 0.2 );                                
                }
            }
        }                        
    }
    
    private void addItemsToVisualizationMap(String geneName, String experimentName, DifferentialExpressionAnalysis analysis, double visualizationValue, Map<String, Map<String, Double>> pValues ) {
        
//        analysis.getResultSets().iterator().next().getResults().iterator().next().getCorrectedPvalue();        
    }
    
    private void markProbeAsMissing(String geneSymbol, String datasetName, Map<String, Boolean> missingProbeHelperMap) {//TODO: use id?
        missingProbeHelperMap.put( datasetName+"&&&"+geneSymbol, true );        
    }
     
    private boolean isProbeMissing(String geneSymbol, String datasetName, Map<String, Boolean> missingProbeHelperMap) {
        return (missingProbeHelperMap.get( datasetName+"&&&"+geneSymbol ) != null); 
    }
    
    private void specificityMetric (List<Collection<BioAssaySet>> allExperiments, Map<String, Integer>pieChartValues) {
        for (Collection<BioAssaySet> experiments : allExperiments) {
            for (BioAssaySet experiment: experiments) {                
                ExpressionExperimentValueObject vo = expressionExperimentReportService.generateSummaryObject( experiment.getId() );
                Integer numberOfProbesTotal = vo.getProcessedExpressionVectorCount();
                log.info("Total probes: " + numberOfProbesTotal);
                for (DifferentialExpressionAnalysisValueObject analysisVo : vo.getDifferentialExpressionAnalyses()) {
                    for ( DifferentialExpressionSummaryValueObject summaryVo : analysisVo.getResultSets() ) {
                        Integer numberOfProbesDiffEx = summaryVo.getNumberOfDiffExpressedProbes();
                        log.info("DiffEx probes: " + numberOfProbesDiffEx);                        
                        log.info("Down probes: " + summaryVo.getDownregulatedCount());
                        if (numberOfProbesDiffEx != null && numberOfProbesTotal != null) {
                            int value = ( int ) ( numberOfProbesDiffEx.doubleValue() / numberOfProbesTotal.doubleValue() * 360.0 );
                            String key = vo.getShortName()+"&&&"+"aaa"+"&&&"+summaryVo.getExperimentalFactors().iterator().next().getDescription();
                            if (pieChartValues.get( key ) != null) {
                                int storedValue = pieChartValues.get( key );                            
                                pieChartValues.put( key , Math.max( value, storedValue ));
                            } else {                                
                                pieChartValues.put( key ,value );
                            }                            
                            log.info("Inserting into pieMap: "+key +" value:"+value);
                        }                           
                    }                
                }
            }
        }        
    }
    
//  for (BioAssaySet assaySet : experiments) {
//  ExpressionExperiment e = (ExpressionExperiment) assaySet;                        
//  if (! datasetLabels.contains( e.getShortName() )) {
//      try {
//          Collection<ExpressionAnalysisResultSet> analyses = differentialExpressionAnalysisService.getResultSets( e );
//          if (analyses == null || analyses.isEmpty()) {
//              missingAnalysisList.add( e.getShortName() ); 
//          } else {
//              absentContrastsList.add( e.getShortName() );                                             
//          }
//      } catch (Exception eee) {}                    
//  }            
//}
//for (String s : absentContrastsList) {
//  analysisLabels.add("absent contrasts ");           
//  datasetPlusFactorLabels.add( s );
//  datasetLabels.add( s );
//}
//
//for (String s : missingAnalysisList) {
//  analysisLabels.add("missing analysis");
//  datasetPlusFactorLabels.add( s );                    
//  datasetLabels.add( s );
//}
              

    
    
    
    
    
    /**
     * AJAX entry. Returns the meta-analysis results.
     * <p>
     * Gets the differential expression results for the genes in {@link DiffExpressionSearchCommand}.
     * 
     * @param command
     * @return
     */
    public Collection<DifferentialExpressionMetaAnalysisValueObject> getDiffExpressionForGenes(
            DiffExpressionSearchCommand command ) {

        Collection<Long> eeScopeIds = command.getEeIds();
        int eeScopeSize = 0;

        if ( eeScopeIds != null && !eeScopeIds.isEmpty() ) {

            // do we need to validate these ids further? It should get checked late (in the analysis stage)

            eeScopeSize = eeScopeIds.size();
        } else {
            if ( command.getEeSetName() != null ) {
                Collection<ExpressionExperimentSet> eeSet = this.expressionExperimentSetService.findByName( command
                        .getEeSetName() );

                if ( eeSet == null || eeSet.isEmpty() ) {
                    throw new IllegalArgumentException( "Unknown or ambiguous set name: " + command.getEeSetName() );
                }

                eeScopeSize = eeSet.iterator().next().getExperiments().size();
            } else {
                Long eeSetId = command.getEeSetId();
                if ( eeSetId >= 0 ) {
                    ExpressionExperimentSet eeSet = this.expressionExperimentSetService.load( eeSetId );
                    // validation/security check.
                    if ( eeSet == null ) {
                        throw new IllegalArgumentException( "No such set with id=" + eeSetId );
                    }
                    eeScopeSize = eeSet.getExperiments().size();
                }
            }

        }

        Collection<Long> geneIds = command.getGeneIds();

        if ( geneIds.size() > MAX_GENES_PER_QUERY ) {
            throw new IllegalArgumentException( "Too many genes selected, please limit searches to "
                    + MAX_GENES_PER_QUERY );
        }

        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = command.getSelectedFactors();

        double threshold = command.getThreshold();

        Collection<DifferentialExpressionMetaAnalysisValueObject> mavos = new ArrayList<DifferentialExpressionMetaAnalysisValueObject>();
        for ( long geneId : geneIds ) {
            DifferentialExpressionMetaAnalysisValueObject mavo = getDifferentialExpressionMetaAnalysis( geneId,
                    selectedFactors, threshold );

            if ( mavo == null ) {
                continue; // no results.
            }

            mavo.setSortKey();
            if ( selectedFactors != null && !selectedFactors.isEmpty() ) {
                mavo.setNumSearchedExperiments( selectedFactors.size() );
            }

            mavo.setNumExperimentsInScope( eeScopeSize );

            mavos.add( mavo );

        }

        return mavos;
    }

    public ExpressionExperimentSetService getExpressionExperimentSetService() {
        return expressionExperimentSetService;
    }

    /**
     * AJAX entry.
     * <p>
     * Value objects returned contain experiments that have 2 factors and have had the diff analysis run on it.
     * 
     * @param eeIds
     */
    public Collection<ExpressionExperimentExperimentalFactorValueObject> getFactors( final Collection<Long> eeIds ) {

        Collection<ExpressionExperimentExperimentalFactorValueObject> result = new HashSet<ExpressionExperimentExperimentalFactorValueObject>();

        final Collection<Long> securityFilteredIds = securityFilterExpressionExperimentIds( eeIds );

        if ( securityFilteredIds.size() == 0 ) {
            return result;
        }

        log.debug( "Getting factors for experiments with ids: "
                + StringUtils.abbreviate( securityFilteredIds.toString(), 100 ) );

        Collection<Long> filteredEeIds = new HashSet<Long>();

        Map<Long, DifferentialExpressionAnalysis> diffAnalyses = differentialExpressionAnalysisService
                .findByInvestigationIds( securityFilteredIds );

        if ( diffAnalyses.isEmpty() ) {
            log.debug( "No differential expression analyses for given ids: " + StringUtils.join( filteredEeIds, ',' ) );
            return result;
        }

        Collection<ExpressionExperimentValueObject> eevos = this.expressionExperimentService
                .loadValueObjects( diffAnalyses.keySet() );

        Map<Long, ExpressionExperimentValueObject> eevoMap = new HashMap<Long, ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevoMap.put( eevo.getId(), eevo );
        }

        for ( Long id : diffAnalyses.keySet() ) {

            DifferentialExpressionAnalysis analysis = diffAnalyses.get( id );
            differentialExpressionAnalysisService.thaw( analysis );

            Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
            for ( FactorAssociatedAnalysisResultSet fars : analysis.getResultSets() ) {
                // FIXME includes factors making up interaction terms, but shouldn't
                // matter, because they will be included as main effects too. If not, this will be wrong!
                factors.addAll( fars.getExperimentalFactors() );
            }

            filteredEeIds.add( id );
            ExpressionExperimentValueObject eevo = eevoMap.get( id );
            ExpressionExperimentExperimentalFactorValueObject eeefvo = new ExpressionExperimentExperimentalFactorValueObject();
            eeefvo.setExpressionExperiment( eevo );
            eeefvo.setNumFactors( factors.size() );
            for ( ExperimentalFactor ef : factors ) {
                ExperimentalFactorValueObject efvo = geneDifferentialExpressionService
                        .configExperimentalFactorValueObject( ef );
                eeefvo.getExperimentalFactors().add( efvo );
            }

            result.add( eeefvo );
        }
        log.info( "Filtered experiments.  Returning factors for experiments with ids: "
                + StringUtils.abbreviate( filteredEeIds.toString(), 100 ) );
        return result;
    }

    /**
     * @param differentialExpressionAnalyzerService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    /**
     * @param geneDifferentialExpressionService
     */
    public void setGeneDifferentialExpressionService(
            GeneDifferentialExpressionService geneDifferentialExpressionService ) {
        this.geneDifferentialExpressionService = geneDifferentialExpressionService;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /*
     * Handles the case exporting results as text.
     * 
     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        if ( request.getParameter( "export" ) == null ) return new ModelAndView( this.getFormView() );

        // -------------------------
        // Download diff expression data for a specific diff expresion search

        double threshold = DEFAULT_THRESHOLD;
        try {
            threshold = Double.parseDouble( request.getParameter( "t" ) );
        } catch ( NumberFormatException e ) {
            log.warn( "invalid threshold; using default " + threshold );
        }

        Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );

        Long eeSetId = null;
        try {
            eeSetId = Long.parseLong( request.getParameter( "a" ) );
        } catch ( NumberFormatException e ) {
            //
        }

        String fs = request.getParameter( "fm" );
        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = extractFactorInfo( fs );

        DiffExpressionSearchCommand command = new DiffExpressionSearchCommand();
        command.setGeneIds( geneIds );
        command.setEeSetId( eeSetId );
        command.setSelectedFactors( selectedFactors );
        command.setThreshold( threshold );

        Collection<DifferentialExpressionMetaAnalysisValueObject> result = getDiffExpressionForGenes( command );

        ModelAndView mav = new ModelAndView( new TextView() );

        StringBuilder buf = new StringBuilder();

        for ( DifferentialExpressionMetaAnalysisValueObject demavo : result ) {
            buf.append( demavo );
        }

        String output = buf.toString();

        mav.addObject( "text", output.length() > 0 ? output : "no results" );
        return mav;

    }

    /**
     * @param fs
     * @return
     */
    private Collection<DiffExpressionSelectedFactorCommand> extractFactorInfo( String fs ) {
        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = new HashSet<DiffExpressionSelectedFactorCommand>();
        try {
            if ( fs != null ) {
                String[] fss = fs.split( "," );
                for ( String fm : fss ) {
                    String[] m = fm.split( "\\." );
                    if ( m.length != 2 ) {
                        continue;
                    }
                    String eeIdStr = m[0];
                    String efIdStr = m[1];

                    Long eeId = Long.parseLong( eeIdStr );
                    Long efId = Long.parseLong( efIdStr );
                    DiffExpressionSelectedFactorCommand dsfc = new DiffExpressionSelectedFactorCommand( eeId, efId );
                    selectedFactors.add( dsfc );
                }
            }
        } catch ( NumberFormatException e ) {
            log.warn( "Error parsing factor info" );
        }
        return selectedFactors;
    }

    /**
     * Returns the results of the meta-analysis.
     * 
     * @param geneId
     * @param eeIds
     * @param selectedFactors
     * @param threshold
     * @return
     */
    private DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis( Long geneId,
            Collection<DiffExpressionSelectedFactorCommand> selectedFactors, double threshold ) {

        Gene g = geneService.load( geneId );

        if ( g == null ) {
            log.warn( "No Gene with id=" + geneId );
            return null;
        }

        /* find experiments that have had the diff cli run on it and have the gene g (analyzed) - security filtered. */
        Collection<BioAssaySet> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );

        if ( experimentsAnalyzed.size() == 0 ) {
            log.warn( "No experiments analyzed for that gene: " + g );
            return null;
        }

        /* the 'chosen' factors (and their associated experiments) */
        Map<Long, Long> eeFactorsMap = new HashMap<Long, Long>();
        for ( DiffExpressionSelectedFactorCommand selectedFactor : selectedFactors ) {
            Long eeId = selectedFactor.getEeId();
            eeFactorsMap.put( eeId, selectedFactor.getEfId() );
            if ( log.isDebugEnabled() ) log.debug( eeId + " --> " + selectedFactor.getEfId() );
        }

        /*
         * filter experiments that had the diff cli run on it and are in the scope of eeFactorsMap eeIds
         * (active/available to the user).
         */
        Collection<BioAssaySet> activeExperiments = null;
        if ( eeFactorsMap.keySet() == null || eeFactorsMap.isEmpty() ) {
            activeExperiments = experimentsAnalyzed;
        } else {
            activeExperiments = new ArrayList<BioAssaySet>();
            for ( BioAssaySet ee : experimentsAnalyzed ) {
                if ( eeFactorsMap.keySet().contains( ee.getId() ) ) {
                    activeExperiments.add( ee );
                }
            }
        }

        DifferentialExpressionMetaAnalysisValueObject mavo = geneDifferentialExpressionService
                .getDifferentialExpressionMetaAnalysis( threshold, g, eeFactorsMap, activeExperiments );

        return mavo;
    }

    /**
     * @param ids
     * @return
     */
    private Collection<Long> securityFilterExpressionExperimentIds( Collection<Long> ids ) {
        /*
         * Because this method returns the results, we have to screen.
         */
        Collection<ExpressionExperiment> securityScreened = expressionExperimentService.loadMultiple( ids );

        Collection<Long> filteredIds = new HashSet<Long>();
        for ( ExpressionExperiment ee : securityScreened ) {
            filteredIds.add( ee.getId() );
        }
        return filteredIds;
    }

}
