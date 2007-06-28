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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.Sorting;
import cern.colt.list.ObjectArrayList;

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
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.association.coexpression.RatProbeCoExpression;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.TaxonUtility;

/**
 * Running link analyses through the spring context; will persist the results if the configuration says so.
 * 
 * @spring.bean id="linkAnalysisService"
 * @spring.property name="vectorService" ref="designElementDataVectorService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="ppService" ref="probe2ProbeCoexpressionService"
 * @spring.property name="csService" ref="compositeSequenceService"
 * @spring.property name="quantitationTypeService" ref="quantitationTypeService"
 * @author Paul
 * @version $Id$
 */
public class LinkAnalysisService {

    private static final int LINK_BATCH_SIZE = 1000;

    private static final boolean useDB = true; // useful for debugging.

    private static Log log = LogFactory.getLog( LinkAnalysisService.class.getName() );
    QuantitationTypeService quantitationTypeService;
    ExpressionExperimentService eeService;
    CompositeSequenceService csService;
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
        eeService.thawLite( ee );
        la.setExpressionExperiment( ee );
        Map<CompositeSequence, DesignElementDataVector> p2v = getProbe2GeneMap( la, dataVectors );

        /*
         * Start the analysis.
         */
        log.info( "Starting generating Raw Links for " + ee );
        la.analyze();
        if ( linkAnalysisConfig.isUseDb() ) {
            saveLinks( p2v, la );
        }
        log.info( "Done with processing of " + ee );

    }

    /**
     * @param la
     * @param dataVectors
     * @return map of probes to vectors.
     */
    @SuppressWarnings("unchecked")
    private Map<CompositeSequence, DesignElementDataVector> getProbe2GeneMap( LinkAnalysis la,
            Collection<DesignElementDataVector> dataVectors ) {
        log.info( "Getting probe-to-gene map" );
        Map<CompositeSequence, DesignElementDataVector> p2v = new HashMap<CompositeSequence, DesignElementDataVector>();
        Collection<CompositeSequence> probesForVectors = new HashSet<CompositeSequence>();
        for ( DesignElementDataVector v : dataVectors ) {
            CompositeSequence cs = ( CompositeSequence ) v.getDesignElement();
            probesForVectors.add( cs );
            p2v.put( cs, v );
        }
        Map<CompositeSequence, Collection<Gene>> probeToGeneMap = csService.getGenes( probesForVectors );
        la.setProbeToGeneMap( probeToGeneMap );
        return p2v;
    }

    /**
     * Persist the links to the database. This takes care of saving a 'flipped' version of the links.
     * 
     * @param p2v
     * @param la
     */
    private void saveLinks( Map<CompositeSequence, DesignElementDataVector> p2v, LinkAnalysis la ) {

        if ( useDB ) {
            deleteOldLinks( la );
        }

        log.info( "Start submitting data to database." );
        StopWatch watch = new StopWatch();
        watch.start();

        ObjectArrayList links = la.getKeep();
        /*
         * Important implementation note: For efficiency reason, it is important that they be stored in order of "x"
         * (the first designelementdatavector) in the database: so all all links with probe=x are clustered. (the actual
         * order of x1 vs x2 doesn't matter). This makes retrievals much faster for the most common types of queries.
         */
        Object[] linksar = links.elements();
        Sorting.quickSort( linksar, 0, links.size(), new Comparator() {
            public int compare( Object arg0, Object arg1 ) {
                Link a = ( Link ) arg0;
                Link b = ( Link ) arg1;

                if ( a.getx() < b.getx() ) {
                    return -1;
                } else if ( a.getx() > b.getx() ) {
                    return 1;
                } else {
                    return 0;
                }
            }
        } );
        links.elements( linksar );

        if ( log.isDebugEnabled() ) {
            for ( Object link : links.elements() ) {
                if ( link == null ) continue;
                log.debug( ( ( Link ) link ).getx() + " " + ( ( Link ) link ).gety() );
            }
        }

        if ( useDB ) {
            saveLinks( p2v, la, links, false );
        }

        /*
         * now create 'reversed' links, first by sorting by the 'y' coordinate. Again, this sort is critical to keep the
         * links in an ordering that the RDBMS can use efficiently.
         */
        log.info( "Sorting links to create flipped set" );
        Sorting.quickSort( linksar, 0, links.size(), new Comparator() {
            public int compare( Object arg0, Object arg1 ) {
                if ( arg0 == null || arg1 == null ) return 1;
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
        links.elements( linksar );

        log.info( "Saving flipped links" );

        if ( log.isDebugEnabled() ) {
            for ( Object link : links.elements() ) {
                if ( link == null ) continue;
                log.debug( ( ( Link ) link ).getx() + " " + ( ( Link ) link ).gety() );
            }
        }

        if ( useDB ) {
            saveLinks( p2v, la, links, true );
        }

        watch.stop();
        log.info( "Seconds to process " + links.size() + " links plus flipped versions:" + ( double ) watch.getTime()
                / 1000.0 );
    }

    // Delete old links for this expressionexperiment
    private void deleteOldLinks( LinkAnalysis la ) {
        ExpressionExperiment expressionExperiment = la.getExpressionExperiment();
        log.info( "Deleting any old links for " + expressionExperiment + " ..." );
        ppService.deleteLinks( expressionExperiment );
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

    // a closure would be just the thing here.
    private class Creator {

        Method m;
        private Object[] arg;
        private Class clazz;

        Creator( Class clazz ) {
            this.clazz = clazz;
            this.arg = new Object[] {};
            try {
                m = clazz.getMethod( "newInstance", new Class[] {} );
            } catch ( SecurityException e ) {
                throw new RuntimeException( e );
            } catch ( NoSuchMethodException e ) {
                throw new RuntimeException( e );
            }
        }

        public Probe2ProbeCoexpression create() {
            try {
                return ( Probe2ProbeCoexpression ) m.invoke( clazz, arg );
            } catch ( IllegalArgumentException e ) {
                throw new RuntimeException( e );
            } catch ( IllegalAccessException e ) {
                throw new RuntimeException( e );
            } catch ( InvocationTargetException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    /**
     * @param numColumns
     * @param w
     * @param v1
     * @param taxon
     * @return
     */
    private Probe2ProbeCoexpression initCoexp( int numColumns, double w, Creator c, QuantitationType metric ) {
        Probe2ProbeCoexpression ppCoexpression = c.create();

        ppCoexpression.setScore( w );
        ppCoexpression.setPvalue( CorrelationStats.pvalue( w, numColumns ) );
        ppCoexpression.setMetric( metric ); // FIXME THIS is incorrect. This should be the
        // quantitation type for the SCORE. (e.g., pearson correlation)
        return ppCoexpression;
    }

    /**
     * @param numColumns
     * @param p2plinks
     * @param i
     * @param w
     * @param v1
     * @param v2
     * @param metric type of score (pearson correlation for example)
     * @param c class that can create instances of the correct type of probe2probecoexpression.
     * @return
     */
    private void persist( int numColumns, Collection<Probe2ProbeCoexpression> p2plinks, int i, double w,
            DesignElementDataVector v1, DesignElementDataVector v2, QuantitationType metric, Creator c ) {

        Probe2ProbeCoexpression ppCoexpression = initCoexp( numColumns, w, c, metric );

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
    private void saveLinks( Map<CompositeSequence, DesignElementDataVector> p2v, LinkAnalysis la,
            ObjectArrayList links, boolean flip ) {
        int numColumns = la.getDataMatrix().columns();

        QuantitationType metric = quantitationTypeService.findOrCreate( la.getMetric() );

        Taxon taxon = la.getTaxon();
        Creator c;
        if ( TaxonUtility.isMouse( taxon ) ) {
            c = new Creator( MouseProbeCoExpression.Factory.class );
        } else if ( TaxonUtility.isRat( taxon ) ) {
            c = new Creator( RatProbeCoExpression.Factory.class );
        } else if ( TaxonUtility.isHuman( taxon ) ) {
            c = new Creator( HumanProbeCoExpression.Factory.class );
        } else {
            c = new Creator( OtherProbeCoExpression.Factory.class );
        }

        Collection<Probe2ProbeCoexpression> p2plinkBatch = new HashSet<Probe2ProbeCoexpression>();
        for ( int i = 0, n = links.size(); i < n; i++ ) {
            Object val = links.getQuick( i );
            if ( val == null ) continue;
            Link m = ( Link ) val;
            Double w = m.getWeight();

            assert w != null;

            DesignElement p1 = la.getMetricMatrix().getProbeForRow( la.getDataMatrix().getRowElement( m.getx() ) );
            DesignElement p2 = la.getMetricMatrix().getProbeForRow( la.getDataMatrix().getRowElement( m.gety() ) );

            DesignElementDataVector v1 = p2v.get( p1 );
            DesignElementDataVector v2 = p2v.get( p2 );

            if ( flip ) {
                persist( numColumns, p2plinkBatch, i, w, v1, v2, metric, c );
            } else {
                persist( numColumns, p2plinkBatch, i, w, v2, v1, metric, c );
            }

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

    public void setCsService( CompositeSequenceService csService ) {
        this.csService = csService;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

}
