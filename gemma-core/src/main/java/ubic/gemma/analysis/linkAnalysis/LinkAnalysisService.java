/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.analysis.linkAnalysis;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.Sorting;

import ubic.basecode.dataStructure.Link;
import ubic.basecode.math.CorrelationStats;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.association.coexpression.HumanProbeCoExpression;
import ubic.gemma.model.association.coexpression.MouseProbeCoExpression;
import ubic.gemma.model.association.coexpression.OtherProbeCoExpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.RatProbeCoExpression;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.TaxonUtility;

/**
 * Running link analyses through the spring context; will persist the results if the configuration says so.
 * 
 * @spring.bean id="linkAnalysisService"
 * @spring.property name="vectorService" ref="designElementDataVectorService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="ppService" ref="probe2ProbeCoexpressionService"
 * @author Paul
 * @version $Id$
 */
public class LinkAnalysisService {

    private static final int LINK_BATCH_SIZE = 1000;

    private static Log log = LogFactory.getLog( LinkAnalysisService.class.getName() );

    ExpressionExperimentService eeService;
    DesignElementDataVectorService vectorService;
    private Probe2ProbeCoexpressionService ppService = null;

    /**
     * Run a link analysis on an experiment, and persist the results if the configuration says to.
     * 
     * @param ee Experiment to be processed
     * @param filterConfig Configuration for filtering of the input data.
     * @param linkAnalysisConfig Configuration for the link analysis.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void process( ExpressionExperiment ee, FilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig )
            throws Exception {
        LinkAnalysis la = new LinkAnalysis( linkAnalysisConfig );
        la.clear();

        log.info( "Begin link processing: " + ee );
        Collection<DesignElementDataVector> dataVectors = getVectors( ee );

        if ( dataVectors == null ) throw new IllegalArgumentException( "No data vectors in " + ee );

        ExpressionExperimentFilter filter = new ExpressionExperimentFilter( ee, eeService.getArrayDesignsUsed( ee ),
                filterConfig );
        ExpressionDataDoubleMatrix eeDoubleMatrix = filter.getFilteredMatrix( dataVectors );

        la.setDataMatrix( eeDoubleMatrix );
        la.setDataVectors( dataVectors ); // shouldn't have to do this.
        la.setTaxon( eeService.getTaxon( ee.getId() ) );

        /*
         * Start the analysis.
         */
        log.info( "Starting generating Raw Links for " + ee );
        la.analyze();
        if ( linkAnalysisConfig.isUseDb() ) {
            saveLinks( la );
        }
        log.info( "Done with processing of " + ee );

    }

    /**
     * Persist the links to the database.
     */
    private void saveLinks( LinkAnalysis la ) {

        /*
         * Delete old links for this expressionexperiment
         */

        ExpressionExperiment expressionExperiment = la.getP2v().values().iterator().next().getExpressionExperiment();
        log.info( "Deleting any old links for " + expressionExperiment + " ..." );
        ppService.deleteLinks( expressionExperiment );

        log.info( "Start submitting data to database." );
        StopWatch watch = new StopWatch();
        watch.start();
        Object[] links = la.getKeep().elements();
        int numColumns = la.getDataMatrix().columns();

        saveLinks( la, numColumns, links );

        // now create 'reversed' links, by sorting by the 'y' coordinate.
        log.info( "Sorting links to create flipped set" );
        Sorting.quickSort( links, new Comparator() {
            public int compare( Object arg0, Object arg1 ) {
                Link a = ( Link ) arg0;
                Link b = ( Link ) arg1;

                if ( a.gety() < b.gety() ) {
                    return -1;
                } else if ( a.gety() > b.gety() ) {
                    return 1;
                } else {
                    return 0;
                }

            }
        } );

        log.info( "Saving flipped links" );
        saveLinks( la, numColumns, links );

        watch.stop();
        log.info( "Seconds to insert " + la.getKeep().size() + " links plus flipped versions:"
                + ( double ) watch.getTime() / 1000.0 );
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<ArrayDesign> checkForMixedTechnologies( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            boolean containsTwoColor = false;
            boolean containsOneColor = false;
            for ( ArrayDesign arrayDesign : arrayDesignsUsed ) {
                if ( arrayDesign.getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {
                    containsOneColor = true;
                }
                if ( !arrayDesign.getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {
                    containsTwoColor = true;
                }
            }

            if ( containsTwoColor && containsOneColor ) {
                throw new UnsupportedOperationException(
                        "Can't correctly handle expression experiments that combine different array technologies." );
            }
        }
        return arrayDesignsUsed;
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<DesignElementDataVector> getVectors( ExpressionExperiment ee ) {
        checkForMixedTechnologies( ee );
        Collection<QuantitationType> qts = ExpressionDataMatrixBuilder.getUsefulQuantitationTypes( ee );
        if ( qts.size() == 0 ) throw new IllegalArgumentException( "No usable quantitation type in " + ee );

        log.info( "Loading vectors..." );
        Collection<DesignElementDataVector> dataVectors = eeService.getDesignElementDataVectors( ee, qts );
        vectorService.thaw( dataVectors );
        return dataVectors;
    }

    /**
     * @param numColumns
     * @param w
     * @param v1
     * @param taxon
     * @return
     */
    private Probe2ProbeCoexpression initCoexp( int numColumns, double w, DesignElementDataVector v1, Taxon taxon ) {
        Probe2ProbeCoexpression ppCoexpression;
        if ( TaxonUtility.isMouse( taxon ) ) {
            ppCoexpression = MouseProbeCoExpression.Factory.newInstance();
        } else if ( TaxonUtility.isRat( taxon ) ) {
            ppCoexpression = RatProbeCoExpression.Factory.newInstance();
        } else if ( TaxonUtility.isHuman( taxon ) ) {
            ppCoexpression = HumanProbeCoExpression.Factory.newInstance();
        } else {
            ppCoexpression = OtherProbeCoExpression.Factory.newInstance();
        }
        ppCoexpression.setScore( w );
        ppCoexpression.setPvalue( CorrelationStats.pvalue( w, numColumns ) );
        ppCoexpression.setQuantitationType( v1.getQuantitationType() );
        return ppCoexpression;
    }

    /**
     * @param numColumns
     * @param p2plinks
     * @param i
     * @param w
     * @param v1
     * @param v2
     * @return
     */
    private void persist( int numColumns, Collection<Probe2ProbeCoexpression> p2plinks, int i, double w,
            DesignElementDataVector v1, DesignElementDataVector v2, Taxon taxon ) {

        if ( taxon == null ) {
            throw new IllegalStateException( "Taxon cannot be null" );
        }

        Probe2ProbeCoexpression ppCoexpression = initCoexp( numColumns, w, v1, taxon );

        ppCoexpression.setFirstVector( v1 );
        ppCoexpression.setSecondVector( v2 );

        p2plinks.add( ppCoexpression );

        if ( i % LINK_BATCH_SIZE == 0 ) {
            this.ppService.create( p2plinks );
            p2plinks.clear();
        }
    }

    /**
     * @param c
     * @param links
     */
    private void saveLinks( LinkAnalysis la, int c, Object[] links ) {
        Collection<Probe2ProbeCoexpression> p2plinkBatch = new HashSet<Probe2ProbeCoexpression>();
        for ( int i = 0, n = links.length; i < n; i++ ) {
            Link m = ( Link ) links[i];
            Double w = m.getWeight();

            assert w != null;

            DesignElement p1 = la.getMetricMatrix().getProbeForRow( la.getDataMatrix().getRowElement( m.getx() ) );
            DesignElement p2 = la.getMetricMatrix().getProbeForRow( la.getDataMatrix().getRowElement( m.gety() ) );

            DesignElementDataVector v1 = la.getP2v().get( p1 );
            DesignElementDataVector v2 = la.getP2v().get( p2 );

            persist( c, p2plinkBatch, i, w, v1, v2, la.getTaxon() );

            if ( i > 0 && i % 50000 == 0 ) {
                log.info( i + " links loaded into the database" );
            }

        }
        if ( p2plinkBatch.size() > 0 ) {
            this.ppService.create( p2plinkBatch );
            p2plinkBatch.clear();
        }
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    public void setPpService( Probe2ProbeCoexpressionService ppService ) {
        this.ppService = ppService;
    }

    public void setVectorService( DesignElementDataVectorService vectorService ) {
        this.vectorService = vectorService;
    }

}
