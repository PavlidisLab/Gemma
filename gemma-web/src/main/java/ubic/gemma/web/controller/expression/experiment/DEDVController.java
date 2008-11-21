/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.controller.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.analysis.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.visualization.VisualizationValueObject;
import ubic.gemma.web.view.TextView;

/**
 * Exposes methods for accessing underlying Design Element Data Vectors. eg: ajax methods for visualization
 * 
 * @spring.bean id="dedvController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "processedExpressionDataVectorService" ref="processedExpressionDataVectorService"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 * @spring.property name="geneDifferentialExpressionService" ref="geneDifferentialExpressionService"
 * @author kelsey
 * @version $Id$
 */
public class DEDVController extends BaseFormController {

    private ExpressionExperimentService expressionExperimentService;
    private GeneDifferentialExpressionService geneDifferentialExpressionService;
    private GeneService geneService;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    /**
     * Given a collection of expression experiment Ids and a geneId returns a map of DEDV value objects to a collection
     * of genes. The EE info is in the value object.
     */
    public Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> getDEDV( Collection<Long> eeIds,
            Collection<Long> geneIds ) throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvMap = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvMap.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size()
                + " genes in " + time + " ms." );

        return makeVectorMap( dedvMap );

    }

    /**
     * AJAX exposed method
     * 
     * @param eeIds
     * @param geneIds
     * @return
     */
    public VisualizationValueObject[] getDEDVForCoexpressionVisualization( Collection<Long> eeIds, Long queryGeneId,
            Long coexpressedGeneId ) {

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        Gene queryGene = geneService.load( queryGeneId );
        Gene coexpressedGene = geneService.load( coexpressedGeneId );

        List<Gene> genes = new ArrayList<Gene>();
        genes.add( queryGene );
        genes.add( coexpressedGene );
        geneService.thawLite( genes );

        if ( genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes );

        watch.stop();
        Long time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + genes.size()
                    + " genes in " + time + " ms." );
        }

        Map<Long, Collection<Long>> validatedProbes = getProbeLinkValidation( ees, queryGene, coexpressedGene, dedvs );

        return makeVisCollection( dedvs, genes, validatedProbes );

    }

    /**
     * AJAX exposed method
     * 
     * @param eeIds
     * @param geneIds (could be just one)
     * @param threshold for 'significance'
     * @param factorMap Collection of DiffExpressionSelectedFactorCommand showing which factors to use.
     * @return
     */
    public VisualizationValueObject[] getDEDVForDiffExVisualization( Collection<Long> eeIds, Collection<Long> geneIds,
            Double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        if ( eeIds.isEmpty() || geneIds.isEmpty() ) return null;

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;
        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes );

        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = getProbeDiffExValidation( ees,
                genes, threshold, factorMap, dedvs );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size()
                + " genes in " + time + " ms." );

        return makeDiffVisCollection( dedvs, new ArrayList<Gene>( genes ), validatedProbes );

    }

    /**
     * @param ees
     * @param genes
     * @param threshold
     * @param factorMap
     * @param dedvs
     * @return
     */
    private Map<Long, Collection<DifferentialExpressionValueObject>> getProbeDiffExValidation(
            Collection<ExpressionExperiment> ees, Collection<Gene> genes, Double threshold,
            Collection<DiffExpressionSelectedFactorCommand> factorMap, Collection<DoubleVectorValueObject> dedvs ) {

        Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes = new HashMap<Long, Collection<DifferentialExpressionValueObject>>();

        Collection<Long> wantedFactors = new HashSet<Long>();
        for ( DiffExpressionSelectedFactorCommand factor : factorMap ) {
            wantedFactors.add( factor.getEfId() );
        }

        for ( Gene gene : genes ) {
            Collection<DifferentialExpressionValueObject> differentialExpression = geneDifferentialExpressionService
                    .getDifferentialExpression( ees, gene, threshold, factorMap );

            for ( DifferentialExpressionValueObject diffVo : differentialExpression ) {
                assert diffVo.getP() <= threshold;
                Long eeId = diffVo.getExpressionExperiment().getId();

                if ( !validatedProbes.containsKey( eeId ) ) {
                    validatedProbes.put( eeId, new HashSet<DifferentialExpressionValueObject>() );
                }

                Long probeId = diffVo.getProbeId();
                Collection<ExperimentalFactorValueObject> factors = diffVo.getExperimentalFactors();

                for ( ExperimentalFactorValueObject fac : factors ) {
                    if ( wantedFactors.contains( fac.getId() ) ) {
                        validatedProbes.get( eeId ).add( diffVo );
                    }
                }
            }
        }
        return validatedProbes;
    }

    /**
     * AJAX exposed method
     * 
     * @param eeIds
     * @param geneIds
     * @return
     */

    public VisualizationValueObject[] getDEDVForVisualization( Collection<Long> eeIds, Collection<Long> geneIds ) {

        StopWatch watch = new StopWatch();
        watch.start();
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if ( ees == null || ees.isEmpty() ) return null;

        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        if ( genes == null || genes.isEmpty() ) return null;

        Collection<DoubleVectorValueObject> dedvs = processedExpressionDataVectorService.getProcessedDataArrays( ees,
                genes );

        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvs.size() + " DEDVs for " + eeIds.size() + " EEs and " + geneIds.size()
                + " genes in " + time + " ms." );

        return makeVisCollection( dedvs, new ArrayList<Gene>( genes ), null );

    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setGeneDifferentialExpressionService(
            GeneDifferentialExpressionService geneDifferentialExpressionService ) {
        this.geneDifferentialExpressionService = geneDifferentialExpressionService;
    }

    // --------------------------------
    // Dependency injection setters
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

    /**
     * @param processedExpressionDataVectorService the processedExpressionDataVectorService to set
     */
    public void setProcessedExpressionDataVectorService(
            ProcessedExpressionDataVectorService processedExpressionDataVectorService ) {
        this.processedExpressionDataVectorService = processedExpressionDataVectorService;
    }

    /*
     * Handle case of text export of the results.
     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
        Collection<Long> eeIds = extractIds( request.getParameter( "ee" ) );

        ModelAndView mav = new ModelAndView( new TextView() );

        if ( geneIds == null || geneIds.isEmpty() || eeIds == null || eeIds.isEmpty() ) {
            mav.addObject( "text", "Input empty for finding DEDVs: " + geneIds + " and " + eeIds );
            return mav;

        }

        Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> result = getDEDV( eeIds, geneIds );

        if ( result == null || result.isEmpty() ) {
            mav.addObject( "text", " No DEDV results for genes: " + geneIds + " and datasets: " + eeIds );
            return mav;
        }

        mav.addObject( "text", format4File( result ) );
        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved and Formated" + result.keySet().size() + " DEDVs for eeIDs: " + eeIds + " and GeneIds: "
                + geneIds + " in : " + time + " ms." );

        return mav;

    }

    /**
     * Converts the given map into a tab delimited String
     * 
     * @param result
     * @return
     */
    private String format4File( Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> result ) {
        StringBuffer converted = new StringBuffer();
        converted.append( "Experiment\tGene\tProbe\tData\n" );
        Map<Long, String> genes = new HashMap<Long, String>();
        for ( ExpressionExperiment ee : result.keySet() ) {

            for ( Gene g : result.get( ee ).keySet() ) {
                Long geneId = g.getId();
                String geneName;
                if ( genes.containsKey( geneId ) ) {
                    geneName = genes.get( geneId );
                } else {
                    geneName = geneService.load( geneId ).getOfficialSymbol();
                    genes.put( geneId, geneName );
                }

                for ( DoubleVectorValueObject dedv : result.get( ee ).get( g ) ) {
                    ee = dedv.getExpressionExperiment();

                    converted.append( ee.getShortName() + " \t " );
                    converted.append( ee.getName() + " \t " );
                    converted.append( geneName + " \t " + g.getOfficialName() + "\t" );
                    converted.append( dedv.getDesignElement().getName() + "\t" );

                    for ( double data : dedv.getData() ) {
                        converted.append( String.format( "%.3f", data ) + "|" );
                    }
                    converted.deleteCharAt( converted.length() - 1 ); // remove the pipe.
                    converted.append( "\n" );
                }
            }
        }
        converted.append( "\r\n" );
        return converted.toString();
    }

    /**
     * @param ees
     * @param queryGene
     * @param coexpressedGene
     * @param dedvs
     * @return map of EEID -> collection ProbeIDs
     */
    private Map<Long, Collection<Long>> getProbeLinkValidation( Collection<ExpressionExperiment> ees, Gene queryGene,
            Gene coexpressedGene, Collection<DoubleVectorValueObject> dedvs ) {
        StopWatch watch;
        Long time;
        watch = new StopWatch();
        watch.start();
        Map<Long, Collection<Long>> coexpressedEE2ProbeIds = new HashMap<Long, Collection<Long>>();
        Map<Long, Collection<Long>> queryEE2ProbeIds = new HashMap<Long, Collection<Long>>();

        for ( DoubleVectorValueObject dedv : dedvs ) {
            if ( dedv.getGenes().contains( queryGene ) ) {
                if ( queryEE2ProbeIds.containsKey( dedv.getExpressionExperiment().getId() ) )
                    queryEE2ProbeIds.get( dedv.getExpressionExperiment().getId() )
                            .add( dedv.getDesignElement().getId() );
                else {
                    Collection<Long> qProbeIds = new ArrayList<Long>();
                    qProbeIds.add( dedv.getDesignElement().getId() );
                    queryEE2ProbeIds.put( dedv.getExpressionExperiment().getId(), qProbeIds );
                }
            } else if ( dedv.getGenes().contains( coexpressedGene ) ) {
                if ( coexpressedEE2ProbeIds.containsKey( dedv.getExpressionExperiment().getId() ) )
                    coexpressedEE2ProbeIds.get( dedv.getExpressionExperiment().getId() ).add(
                            dedv.getDesignElement().getId() );
                else {
                    Collection<Long> cProbeIds = new ArrayList<Long>();
                    cProbeIds.add( dedv.getDesignElement().getId() );
                    coexpressedEE2ProbeIds.put( dedv.getExpressionExperiment().getId(), cProbeIds );
                }
            } else {
                log.error( "Impossible! Dedv doesn't belong to coexpressed or query gene. QueryGene= "
                        + queryGene.getOfficialSymbol() + "CoexprssedGene= " + coexpressedGene.getOfficialSymbol()
                        + "DEDV " + dedv.getId() + " has genes: " + dedv.getGenes() );
            }
        }

        Map<Long, Collection<Long>> validatedProbes = new HashMap<Long, Collection<Long>>();
        for ( ExpressionExperiment ee : ees ) {
            validatedProbes.put( ee.getId(), this.probe2ProbeCoexpressionService.validateProbesInCoexpression(
                    queryEE2ProbeIds.get( ee.getId() ), coexpressedEE2ProbeIds.get( ee.getId() ), ee, queryGene
                            .getTaxon().getCommonName() ) );
        }

        watch.stop();
        time = watch.getTime();

        if ( time > 1000 ) {
            log.info( "Validation of probes for " + ees.size() + " experiments in " + time + " ms." );
        }
        return validatedProbes;
    }

    /**
     * @param newResults
     * @return
     */
    private Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> makeVectorMap(
            Collection<DoubleVectorValueObject> newResults ) {
        Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> result = new HashMap<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>>();
        for ( DoubleVectorValueObject v : newResults ) {
            ExpressionExperiment e = v.getExpressionExperiment();
            if ( !result.containsKey( e ) ) {
                result.put( e, new HashMap<Gene, Collection<DoubleVectorValueObject>>() );
            }
            Map<Gene, Collection<DoubleVectorValueObject>> innerMap = result.get( e );
            for ( Gene g : v.getGenes() ) {
                if ( !innerMap.containsKey( g ) ) {
                    innerMap.put( g, new HashSet<DoubleVectorValueObject>() );
                }
                innerMap.get( g ).add( v );
            }
        }
        return result;
    }

    /**
     * Takes the DEDVs and put them in point objects and normalize the values. returns a map of eeid to visValueObject.
     * Currently removes multiple hits for same gene. Tries to pick best DEDV.
     * 
     * @param dedvs
     * @param genes
     * @return
     */
    private VisualizationValueObject[] makeVisCollection( Collection<DoubleVectorValueObject> dedvs, List<Gene> genes,
            Map<Long, Collection<Long>> validatedProbes ) {

        Map<ExpressionExperiment, Collection<DoubleVectorValueObject>> vvoMap = new HashMap<ExpressionExperiment, Collection<DoubleVectorValueObject>>();

        // Organize by expression experiment
        for ( DoubleVectorValueObject dvvo : dedvs ) {
            ExpressionExperiment ee = dvvo.getExpressionExperiment();
            if ( !vvoMap.containsKey( ee ) ) {
                vvoMap.put( ee, new HashSet<DoubleVectorValueObject>() );
            }
            vvoMap.get( ee ).add( dvvo );
        }

        VisualizationValueObject[] result = new VisualizationValueObject[vvoMap.keySet().size()];
        // Create collection of visualizationValueObject for flotr on js side
        int i = 0;
        for ( ExpressionExperiment ee : vvoMap.keySet() ) {
            Collection<Long> validatedProbeList = null;
            if ( validatedProbes != null ) {
                validatedProbeList = validatedProbes.get( ee.getId() );
            }
            VisualizationValueObject vvo = new VisualizationValueObject( vvoMap.get( ee ), genes, validatedProbeList );
            result[i] = vvo;
            i++;
        }

        return result;

    }

    /**
     * Takes the DEDVs and put them in point objects and normalize the values. returns a map of eeid to visValueObject.
     * Currently removes multiple hits for same gene. Tries to pick best DEDV.
     * 
     * @param dedvs
     * @param genes
     * @return
     */
    private VisualizationValueObject[] makeDiffVisCollection( Collection<DoubleVectorValueObject> dedvs,
            List<Gene> genes, Map<Long, Collection<DifferentialExpressionValueObject>> validatedProbes ) {

        Map<Long, Collection<DoubleVectorValueObject>> vvoMap = new HashMap<Long, Collection<DoubleVectorValueObject>>();

        // Organize by expression experiment
        for ( DoubleVectorValueObject dvvo : dedvs ) {
            ExpressionExperiment ee = dvvo.getExpressionExperiment();
            if ( !vvoMap.containsKey( ee.getId() ) ) {
                vvoMap.put( ee.getId(), new HashSet<DoubleVectorValueObject>() );
            }
            vvoMap.get( ee.getId() ).add( dvvo );
        }

        class EE2PValue implements Comparable {
            Long EEId;
            double pValue;

            public EE2PValue() {
                super();
            }

            public EE2PValue( Long eeid, double pValue ) {
                this();
                this.EEId = eeid;
                this.pValue = pValue;
            }

            public Long getEEId() {
                return EEId;
            }

            public double getPValue() {
                return pValue;
            }

            public int compareTo( Object o ) {
                EE2PValue compareTo = ( EE2PValue ) o;
                if ( this.pValue > compareTo.getPValue() )
                    return 1;
                else if ( this.pValue > compareTo.getPValue() )
                    return -1;
                else
                    return 0;
            }

        }

        List<EE2PValue> sortedEE = new ArrayList<EE2PValue>();

        // Need to sort the expression experiments by lowest p-value
        for ( Long eeId : vvoMap.keySet() ) {
            Collection<DifferentialExpressionValueObject> devos = validatedProbes.get( eeId );
            double minP = 1;

            if ( devos != null && !devos.isEmpty() ) {
                for ( DifferentialExpressionValueObject devo : devos ) {
                    if ( minP > devo.getP() ) {
                        minP = devo.getP();
                    }
                }
            }
            sortedEE.add( new EE2PValue( eeId, minP ) );
        }

        Collections.sort( sortedEE );

        VisualizationValueObject[] result = new VisualizationValueObject[vvoMap.keySet().size()];

        // Create collection of visualizationValueObject for flotr on js side
        int i = 0;
        for ( EE2PValue ee2P : sortedEE ) {
            Collection<Long> validatedProbeIdList = new ArrayList<Long>();
            if ( validatedProbes.get(ee2P.getEEId()) != null && !validatedProbes.get(ee2P.getEEId()).isEmpty() ) {
                for ( DifferentialExpressionValueObject devo : validatedProbes.get( ee2P.getEEId() ) ) {
                    validatedProbeIdList.add( devo.getProbeId() );
                }
            }
            VisualizationValueObject vvo = new VisualizationValueObject( vvoMap.get( ee2P.getEEId() ), genes,
                    validatedProbeIdList );
            result[i] = vvo;
            i++;
        }

        return result;

    }

}
