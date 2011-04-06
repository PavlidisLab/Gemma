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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionMetaAnalysisValueObject;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.Reference;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao;
import ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionSummaryValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentExperimentalFactorValueObject;
import ubic.gemma.web.session.SessionListManager;
import ubic.gemma.web.view.TextView;
import ubic.gemma.web.visualization.DifferentialExpressionAnalysisResultSetVisualizationValueObject;
import ubic.gemma.web.visualization.DifferentialExpressionVisualizationValueObject;

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

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private SessionListManager sessionListManager;

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


    // So that we're consistent throughout this visualization code...
    private static final double VISUALIZATION_P_VALUE_THRESHOLD = 0.9;

    /*
     * Helper method to get factor values. TODO: Fix FactoValue class to return correct factor value in the first place.
     */
    private String getFactorValueString( FactorValue fv ) {
        if ( fv == null ) return "null";

        if ( fv.getCharacteristics() != null && fv.getCharacteristics().size() > 0 ) {
            return fv.getCharacteristics().iterator().next().getValue();
        } else if ( fv.getMeasurement() != null ) {
            return fv.getMeasurement().getValue();
        } else if ( fv.getValue() != null && !fv.getValue().isEmpty() ) {
            return fv.getValue();
        } else
            return "absent ";
    }

    /**
     * side effects
     * 
     * @return
     */
    public DifferentialExpressionAnalysisResultSetVisualizationValueObject buildVisualizationColumn(
            int[] geneGroupSizes, int datasetGroupIndex, List<List<Gene>> genes, ExpressionAnalysisResultSet resultSet,
            DifferentialExpressionVisualizationValueObject theResult, Collection<Long> arrayDesignIds ) {
        DifferentialExpressionAnalysisResultSetVisualizationValueObject vizColumn = new DifferentialExpressionAnalysisResultSetVisualizationValueObject(
                geneGroupSizes );

        StopWatch timer = new StopWatch();
        timer.start();
        // TODO: should be part of result set?
        Integer numberDiffExpressedProbes = differentialExpressionAnalysisService.countProbesMeetingThreshold(
                resultSet, VISUALIZATION_P_VALUE_THRESHOLD );// differentialExpressionAnalysisDao.countNumberOfDifferentiallyExpressedProbes(
        // resultSet.getId(),
        // VISUALIZATION_P_VALUE_THRESHOLD );
        timer.stop();
        log.info( "DiffEx probes: " + numberDiffExpressedProbes + ", call took :" + timer.getTime() + " ms" );

        vizColumn.setNumberOfProbesDiffExpressed( numberDiffExpressedProbes );

        ExperimentalFactor factor = resultSet.getExperimentalFactors().iterator().next();
        log.info( "Factor description: " + factor.getDescription() + ", number of factor values: "
                + factor.getFactorValues().size() );

        vizColumn.setBaselineFactorValue( getFactorValueString( resultSet.getBaselineGroup() ) );
        vizColumn.setFactorName( factor.getName() );
        // vizColumn.setFactorCategory( factor.getCategory().getName() );

        for ( FactorValue fvalue : factor.getFactorValues() ) {
            vizColumn.addContrastsFactorValue( fvalue.getId(), getFactorValueString( fvalue ) );
            log.info( "Factor value: " + getFactorValueString( fvalue ) );
        }

        for ( int geneGroupIndex = 0; geneGroupIndex < genes.size(); geneGroupIndex++ ) {
            for ( int geneIndex = 0; geneIndex < genes.get( geneGroupIndex ).size(); geneIndex++ ) {

                List<Double> resultsForGene = differentialExpressionResultService.findGeneInResultSets( genes.get(
                        geneGroupIndex ).get( geneIndex ), resultSet, arrayDesignIds, 1 );

                if ( resultsForGene == null || resultsForGene.isEmpty() || resultsForGene.get( 0 ) == null ) {
                    vizColumn.setVisualizationValue( geneGroupIndex, geneIndex, null );
                    vizColumn.setPvalue( geneGroupIndex, geneIndex, null );
                    vizColumn.setNumberOfProbes( geneGroupIndex, geneIndex, 0 );
                } else {
                    Double correctedPvalue = resultsForGene.get( 0 );
                    log.info( "pValue: " + correctedPvalue );
                    vizColumn.setNumberOfProbes( geneGroupIndex, geneIndex, resultsForGene.size() ); // show that there
                                                                                                     // are multiple
                                                                                                     // probes for the
                                                                                                     // gene
                    vizColumn.setPvalue( geneGroupIndex, geneIndex, correctedPvalue );
                    vizColumn.setVisualizationValue( geneGroupIndex, geneIndex,
                            calculateVisualizationValueBasedOnPvalue( correctedPvalue ) );

                    theResult.addToGeneScore( datasetGroupIndex, geneGroupIndex, geneIndex, Math.min( 5, Math
                            .floor( -Math.log10( correctedPvalue ) ) ) );
                }
            }
        }
        return vizColumn;
    }

    /**
     * @param geneGroupSizes
     * @param genes
     * @param datasetGroupIndex
     * @param analysis
     * @param deaValueObject
     * @param theResult
     * @return
     */
    private List<DifferentialExpressionAnalysisResultSetVisualizationValueObject> buildVisualizationColumnsFromAnalysis(
            int[] geneGroupSizes, List<List<Gene>> genes, int datasetGroupIndex,
            DifferentialExpressionAnalysis analysis, DifferentialExpressionVisualizationValueObject theResult,
            Collection<Long> arrayDesignIds ) {
        List<DifferentialExpressionAnalysisResultSetVisualizationValueObject> analysisColumns = new ArrayList<DifferentialExpressionAnalysisResultSetVisualizationValueObject>();

        if ( analysis == null ) return analysisColumns; // Analysis was not run

        StopWatch timer = new StopWatch();
        timer.start();
        differentialExpressionAnalysisService.thaw( analysis );
        timer.stop();
        log.info( "Thawing analysis took :" + timer.getTime() );

        for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
            // Currently, we skip result sets containing interactions.
            if ( resultSet.getExperimentalFactors().size() != 1 ) continue;

            DifferentialExpressionAnalysisResultSetVisualizationValueObject vizColumn = buildVisualizationColumn(
                    geneGroupSizes, datasetGroupIndex, genes, resultSet, theResult, arrayDesignIds );

            // Common properties for result sets in this analysis.
            vizColumn.setAnalysisId( analysis.getId() );

            // Done with constructing visualization column.
            analysisColumns.add( vizColumn );
        }

        return analysisColumns;
    }

    /**
     * Ajax.
     * 
     * @param taxonId
     * @param datasetGroupReferences 
     * @param geneGroupReferences 
     * @return
     */
    public DifferentialExpressionVisualizationValueObject differentialExpressionAnalysisVisualizationSearch(
            Long taxonId, Collection<Reference> datasetGroupReferences, Collection<Reference> geneGroupReferences ) {

        // decode the encoded references
        // Collection<Reference> geneGroupReferences = Reference.decode( encodedGeneGroupReferences );
        // Collection<Reference> datasetGroupReferences = Reference.decode( encodedDatasetGroupReferences );

        // We get ids from the UI. First step is to load associated genes and experiments.
        List<Collection<BioAssaySet>> experiments = new ArrayList<Collection<BioAssaySet>>();
        for ( Reference ref : datasetGroupReferences ) {
            if ( ref != null ) {
                // if the ids being passed in are session ids, use a different method to load them
                if ( ref.getType() == Reference.SESSION_BOUND_GROUP ) {
                    Collection<ExpressionExperimentValueObject> eevos = sessionListManager
                            .getExperimentsInSetByReference( ref );
                    Collection<Long> ids = new ArrayList<Long>();
                    for ( ExpressionExperimentValueObject eevo : eevos ) {
                        ids.add( eevo.getId() );
                    }
                    Collection<ExpressionExperiment> experimentsInsideGroup = expressionExperimentService
                            .loadMultiple( ids );
                    Collection<BioAssaySet> bioAssaySetsInsideGroup = new ArrayList<BioAssaySet>();
                    for ( ExpressionExperiment experiment : experimentsInsideGroup ) {
                        bioAssaySetsInsideGroup.add( ( BioAssaySet ) experiment );
                    }
                    experiments.add( bioAssaySetsInsideGroup );
                } else if ( ref.getType() == Reference.DATABASE_BACKED_GROUP ) {
                    ExpressionExperimentSet datasetGroup = expressionExperimentSetService.load( ref.getId() );
                    Collection<BioAssaySet> experimentsInsideGroup = datasetGroup.getExperiments();
                    experiments.add( experimentsInsideGroup );
                }
            }

        }

        // Load genes
        List<List<Gene>> genes = new ArrayList<List<Gene>>();
        List<List<String>> geneNames = new ArrayList<List<String>>();
        List<List<Long>> geneIds = new ArrayList<List<Long>>();

        int geneGroupIndex = 0;

        for ( Reference ref : geneGroupReferences ) {
            if ( ref != null ) {
                List<Gene> genesInsideSet = new ArrayList<Gene>();
                List<String> geneNamesInsideSet = new ArrayList<String>();
                List<Long> geneIdsInsideSet = new ArrayList<Long>();

                // if the ids being passed in are session ids, use a different method to load them
                if ( ref.getType() == Reference.SESSION_BOUND_GROUP ) {
                    Collection<GeneValueObject> geneValueObjectsInSet = sessionListManager
                            .getGenesInSetByReference( ref );
                    for ( GeneValueObject gsvo : geneValueObjectsInSet ) {
                        Gene gene = geneService.load( gsvo.getId() );
                        if ( gene != null ) {
                            genesInsideSet.add( gene );
                            geneNamesInsideSet.add( gene.getOfficialSymbol() );
                            geneIdsInsideSet.add( gene.getId() );
                        }
                    }
                } else if ( ref.getType() == Reference.DATABASE_BACKED_GROUP ) {
                    GeneSet geneSet = geneSetService.load( ref.getId() );
                    for ( GeneSetMember memberGene : geneSet.getMembers() ) {
                        if ( memberGene.getGene() != null ) {
                            genesInsideSet.add( memberGene.getGene() );
                            geneNamesInsideSet.add( memberGene.getGene().getOfficialSymbol() );
                            geneIdsInsideSet.add( memberGene.getGene().getId() );
                        }
                    }
                }
                genes.add( genesInsideSet );
                geneNames.add( geneNamesInsideSet );
                geneIds.add( geneIdsInsideSet );
                geneGroupIndex++;
            }

        }

        int numberOfGeneGroups = genes.size();
        int[] geneGroupSizes = new int[numberOfGeneGroups];
        int i = 0;
        for ( List<Gene> geneGroup : genes ) {
            geneGroupSizes[i] = geneGroup.size();
            i++;
        }

        return buildDifferentialExpressionVisualizationValueObject( geneGroupSizes, experiments.size(), genes,
                geneNames, geneIds, experiments );
    }

    /**
     * Ajax. the same as differentialExpressionAnalysisVisualizationSearch but takes a collection of id lists instead of
     * a collection of set ids (for both genes and experiments)
     * 
     * @param taxonId
     * @param experimentIdsByGroup
     * @param geneIdsByGroup
     * @return
     */
    public DifferentialExpressionVisualizationValueObject differentialExpressionAnalysisVisualizationSearchIdBased(
            Long taxonId, Collection<List<Long>> experimentIdsByGroup, Collection<List<Long>> geneIdsByGroup ) {

        // We get ids from the UI. First step is to load associated genes and experiments.
        List<Collection<BioAssaySet>> experiments = new ArrayList<Collection<BioAssaySet>>();
        for ( List<Long> experimentIds : experimentIdsByGroup ) {

            Collection<ExpressionExperiment> experimentsInsideGroup = expressionExperimentService
                    .loadMultiple( experimentIds );
            Collection<BioAssaySet> bioAssaySetsInsideGroup = new ArrayList<BioAssaySet>();
            for ( ExpressionExperiment experiment : experimentsInsideGroup ) {
                bioAssaySetsInsideGroup.add( ( BioAssaySet ) experiment );
            }
            experiments.add( bioAssaySetsInsideGroup );
        }

        // Load genes
        List<List<Gene>> genes = new ArrayList<List<Gene>>();
        List<List<String>> geneNames = new ArrayList<List<String>>();
        List<List<Long>> geneIds = new ArrayList<List<Long>>();

        int geneGroupIndex = 0;

        for ( List<Long> geneGroupIds : geneIdsByGroup ) {

            List<Gene> genesInsideSet = ( List<Gene> ) geneService.loadMultiple( geneGroupIds );
            List<String> geneNamesInsideSet = new ArrayList<String>();
            List<Long> geneIdsInsideSet = new ArrayList<Long>();
            for ( Gene gene : genesInsideSet ) {
                if ( gene != null ) {
                    geneNamesInsideSet.add( gene.getOfficialSymbol() );
                    geneIdsInsideSet.add( gene.getId() );
                }
            }
            genes.add( genesInsideSet );
            geneNames.add( geneNamesInsideSet );
            geneIds.add( geneIdsInsideSet );
            geneGroupIndex++;
        }

        int numberOfGeneGroups = genes.size();
        int[] geneGroupSizes = new int[numberOfGeneGroups];
        int i = 0;
        for ( List<Gene> geneGroup : genes ) {
            geneGroupSizes[i] = geneGroup.size();
            i++;
        }

        return buildDifferentialExpressionVisualizationValueObject( geneGroupSizes, experiments.size(), genes,
                geneNames, geneIds, experiments );
    }

    /**
     * @param taxonId
     * @param datasetGroupIds
     * @param geneGroupIds
     * @return
     */
    public DifferentialExpressionVisualizationValueObject buildDifferentialExpressionVisualizationValueObject(
            int[] geneGroupSizes, int numberOfDatasets, List<List<Gene>> genes, List<List<String>> geneNames,
            List<List<Long>> geneIds, List<Collection<BioAssaySet>> experiments ) {
        DifferentialExpressionVisualizationValueObject mainVisuzalizationDataObject = new DifferentialExpressionVisualizationValueObject(
                numberOfDatasets, geneGroupSizes );

        // Iterates through dataset groups....

        int datasetGroupIndex = 0;
        for ( Collection<BioAssaySet> groupExperiemnts : experiments ) {
            List<DifferentialExpressionAnalysisResultSetVisualizationValueObject> dataColumnsDatasetGroup = new ArrayList<DifferentialExpressionAnalysisResultSetVisualizationValueObject>();

            for ( BioAssaySet experiment : groupExperiemnts ) {
                Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService
                        .getAnalyses( ( ExpressionExperiment ) experiment );

                for ( DifferentialExpressionAnalysis analysis : analyses ) {
                    ExpressionExperiment e = ( ExpressionExperiment ) experiment;
                    StopWatch timer = new StopWatch();
                    timer.start();
                    int numberOfProbesOnArray = expressionExperimentDao.getProcessedExpressionVectorCount( e );
                    timer.stop();
                    log.info( "Call to get number of probes on the array for 1 experiment took : " + timer.getTime()
                            + " ms" );

                    Collection<Long> arrayDesignIds = new ArrayList<Long>();
                    for ( ArrayDesign arrayDesign : expressionExperimentService.getArrayDesignsUsed( e ) ) {
                        arrayDesignIds.add( e.getId() );
                    }

                    timer.reset();
                    timer.start();
                    List<DifferentialExpressionAnalysisResultSetVisualizationValueObject> analysisColumns = buildVisualizationColumnsFromAnalysis(
                            geneGroupSizes, genes, datasetGroupIndex, analysis, mainVisuzalizationDataObject,
                            arrayDesignIds );

                    timer.stop();
                    log.info( "Call to constructVisualizationColumnsFromAnalysis took : " + timer.getTime() + " ms" );

                    // Set common properties for all columns in this dataset.
                    for ( DifferentialExpressionAnalysisResultSetVisualizationValueObject vizColumn : analysisColumns ) {
                        vizColumn.setDatasetName( experiment.getName() );
                        vizColumn.setDatasetId( experiment.getId() );
                        vizColumn.setNumberOfProbesTotal( numberOfProbesOnArray );
                    }

                    dataColumnsDatasetGroup.addAll( analysisColumns );
                }
            }
            mainVisuzalizationDataObject.addDatasetGroup( dataColumnsDatasetGroup );
            datasetGroupIndex++;
        }

        mainVisuzalizationDataObject.setGeneNames( geneNames );
        mainVisuzalizationDataObject.setGeneIds( geneIds );

        return mainVisuzalizationDataObject;
    }

    /*
     * Computes visualization value (0..1) from pValue. Paul suggested using probit transform to help emphasize
     * differences in small to modest p-values range and to de-emphasize differences in medium to large p-values. TODO:
     * Tweak this if necessary.
     */
    private double calculateVisualizationValueBasedOnPvalue( double pValue ) {
        double visualizationValue = 0.0;
        pValue = Math.abs( pValue );
        if ( pValue < 0.5 && pValue >= 0.25 )
            visualizationValue = 0.1;
        else if ( pValue < 0.25 && pValue >= 0.1 )
            visualizationValue = 0.2;
        else if ( pValue < 0.1 && pValue >= 0.05 )
            visualizationValue = 0.3;
        else if ( pValue < 0.05 && pValue >= 0.01 )
            visualizationValue = 0.4;
        else if ( pValue < 0.01 && pValue >= 0.001 )
            visualizationValue = 0.5;
        else if ( pValue < 0.001 && pValue >= 0.0001 )
            visualizationValue = 0.6;
        else if ( pValue < 0.0001 && pValue >= 0.00001 )
            visualizationValue = 0.7;
        else if ( pValue < 0.00001 ) visualizationValue = 1;
        return visualizationValue;
    }

    // 0 - 2?
    private double calculateVisualizationValueBasedOnFoldChange( double foldChange ) {
        double visualizationValue = 0.0;
        double absFoldChange = Math.abs( foldChange - 1 );
        if ( absFoldChange >= 0 && absFoldChange < 0.05 )
            visualizationValue = 0.1;
        else if ( absFoldChange >= 0.05 && absFoldChange < 0.1 )
            visualizationValue = 0.2;
        else if ( absFoldChange >= 0.1 && absFoldChange < 0.2 )
            visualizationValue = 0.3;
        else if ( absFoldChange >= 0.2 && absFoldChange < 0.3 )
            visualizationValue = 0.4;
        else if ( absFoldChange >= 0.3 && absFoldChange < 0.4 )
            visualizationValue = 0.5;
        else if ( absFoldChange >= 0.4 && absFoldChange < 0.5 )
            visualizationValue = 0.6;
        else if ( absFoldChange >= 0.5 && absFoldChange < 0.6 )
            visualizationValue = 0.7;
        else if ( absFoldChange >= 0.6 && absFoldChange < 0.7 )
            visualizationValue = 0.8;
        else if ( absFoldChange >= 0.7 && absFoldChange < 0.8 )
            visualizationValue = 0.9;
        else if ( absFoldChange >= 0.8 ) visualizationValue = 1;

        if ( foldChange < 0 ) visualizationValue = ( -1 ) * visualizationValue;
        return visualizationValue;
    }

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
        Collection<Long> eeIds = null;
        try {
            eeSetId = Long.parseLong( request.getParameter( "a" ) );
        } catch ( NumberFormatException e ) {
            //
        }
        if ( eeSetId == null ) {
            eeIds = extractIds( request.getParameter( "ees" ) );
        }

        String fs = request.getParameter( "fm" );
        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = extractFactorInfo( fs );

        DiffExpressionSearchCommand command = new DiffExpressionSearchCommand();
        command.setGeneIds( geneIds );
        command.setEeSetId( eeSetId );
        command.setEeIds( eeIds );
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

    // public DifferentialExpressionAnalysisResultSetVisualizationValueObject
    // differentialExpressionAnalysisVisualizationLoadContrastsInfo (
    // long resultSetId, List<List<Long>> geneIds
    // )
    // {
    // // Load genes
    // List<List<Gene>> genes = new ArrayList<List<Gene>>();
    //
    // geneSetService.
    //
    // int numberOfGeneGroups = genes.size();
    // int[] geneGroupSizes = new int[numberOfGeneGroups];
    // int i = 0;
    // for (List<Gene> geneGroup : genes) {
    // geneGroupSizes[i] = geneGroup.size();
    // }
    //
    // //public DifferentialExpressionAnalysisResultSetVisualizationValueObject loadContrastsData ( long resultSetId,
    // List<Long> geneIds ) {
    // //
    // // load result set
    // //timer.reset();
    // //timer.start();
    // //ProbeAnalysisResult result = differentialExpressionAnalysisResultDao.load ( probeResultId );
    // //timer.stop();
    // //log.info( "Loading probe result took :"+timer.getTime() );
    //
    // //differentialExpressionResultService.thaw( result );
    //
    // //differentialExpressionResultService.findGeneInResultSets( gene, resultSet, threshold, limit );
    // //
    // //for (ContrastResult cr : result.getContrasts()) {
    // //String key = getFactorValueString(cr.getFactorValue());
    // //if (cr.getPvalue() < 0.01) {
    // // contrastsFoldChangeValuesPerGene.put ( key, cr.getLogFoldChange() );
    // // contrastsVisualizationValuesPerGene.put ( key, calculateVisualizationValueBasedOnFoldChange(
    // cr.getLogFoldChange()) );
    // //}
    // //}
    // //
    // //}
    //
    //
    // }
    //

}
