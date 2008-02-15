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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.Link;
import ubic.basecode.math.CorrelationStats;
import ubic.gemma.analysis.preprocess.InsufficientProbesException;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.service.AnalysisHelperService;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.analysis.ProbeCoexpressionAnalysis;
import ubic.gemma.model.association.coexpression.HumanProbeCoExpression;
import ubic.gemma.model.association.coexpression.MouseProbeCoExpression;
import ubic.gemma.model.association.coexpression.OtherProbeCoExpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.RatProbeCoExpression;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.TaxonUtility;
import cern.colt.list.ObjectArrayList;

/**
 * Running link analyses through the spring context; will persist the results if the configuration says so.
 * 
 * @spring.bean id="linkAnalysisService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="ppService" ref="probe2ProbeCoexpressionService"
 * @spring.property name="csService" ref="compositeSequenceService"
 * @spring.property name="quantitationTypeService" ref="quantitationTypeService"
 * @spring.property name="analysisHelperService" ref="analysisHelperService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @author Paul
 * @version $Id$
 */
public class LinkAnalysisService {

    private static final int LINK_BATCH_SIZE = 5000;

    private static final boolean useDB = true; // useful for debugging.

    private static Log log = LogFactory.getLog( LinkAnalysisService.class.getName() );
    QuantitationTypeService quantitationTypeService;
    ExpressionExperimentService eeService;
    CompositeSequenceService csService;
    private Probe2ProbeCoexpressionService ppService = null;
    private PersisterHelper persisterHelper;
    private AnalysisHelperService analysisHelperService = null;

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

        log.info( "Fetching expression data ... " + ee );
        eeService.thawLite( ee );
        Collection<DesignElementDataVector> dataVectors = analysisHelperService.getPreferredAndMissingValueVectors( ee );

        checkVectors( ee, dataVectors );

        ExpressionDataDoubleMatrix eeDoubleMatrix = analysisHelperService.getFilteredMatrix( ee, filterConfig,
                dataVectors );

        if ( eeDoubleMatrix.rows() == 0 ) {
            log.info( "No rows left after filtering" );
            throw new InsufficientProbesException( "No rows left after filtering" );
        } else if ( eeDoubleMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientProbesException( "To few rows (" + eeDoubleMatrix.rows()
                    + "), data sets are not analyzed unless they have at least " + FilterConfig.MINIMUM_ROWS_TO_BOTHER
                    + " rows" );
        }

        /*
         * Might as well while we have the data handy. (FIXME: be able to suppress this)
         */
        log.info( "Creating sample correlation matrix ..." );
        ExpressionDataSampleCorrelation.process( eeDoubleMatrix, ee );

        /*
         * Link analysis section.
         */
        log.info( "Starting link analysis... " + ee );
        setUpForAnalysis( ee, la, dataVectors, eeDoubleMatrix );
        addAnalysisObj( ee, eeDoubleMatrix, filterConfig, linkAnalysisConfig, la );
        Map<CompositeSequence, DesignElementDataVector> p2v = getProbe2VectorMap( dataVectors );
        la.analyze();

        // output
        if ( linkAnalysisConfig.isUseDb() && !linkAnalysisConfig.isTextOut() ) {

            Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
            ees.add( ee );

            saveLinks( p2v, la );
        } else if ( linkAnalysisConfig.isTextOut() ) {
            writeLinks( la, new PrintWriter( System.out ) );
        }

        log.info( "Done with processing of " + ee );

    }

    public void setAnalysisHelperService( AnalysisHelperService analysisHelperService ) {
        this.analysisHelperService = analysisHelperService;
    }

    public void setCsService( CompositeSequenceService csService ) {
        this.csService = csService;
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public void setPpService( Probe2ProbeCoexpressionService ppService ) {
        this.ppService = ppService;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    /**
     * @param ee
     * @param eeDoubleMatrix
     * @param filterConfig
     * @param linkAnalysisConfig
     * @param la
     */
    private void addAnalysisObj( ExpressionExperiment ee, ExpressionDataDoubleMatrix eeDoubleMatrix,
            FilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig, LinkAnalysis la ) {

        /*
         * Set up basics.
         */
        ProbeCoexpressionAnalysis analysis = linkAnalysisConfig.toAnalysis();
        analysis.setName( ee.getShortName() + " link analysis" );
        analysis.getProtocol().setDescription(
                analysis.getProtocol().getDescription() + "# FilterConfig:\n" + filterConfig.toString() );

        assert ee.getId() != null;
        analysis.getExperimentsAnalyzed().add( ee );

        /*
         * Add probes used.
         */
        List<ExpressionDataMatrixRowElement> rowElements = eeDoubleMatrix.getRowElements();
        Collection<CompositeSequence> probesUsed = new HashSet<CompositeSequence>();
        for ( ExpressionDataMatrixRowElement el : rowElements ) {
            probesUsed.add( ( CompositeSequence ) el.getDesignElement() );
        }
        analysis.setProbesUsed( probesUsed );

        la.setAnalysisObj( analysis );
    }

    /**
     * @param ee
     * @param dataVectors
     */
    private void checkVectors( ExpressionExperiment ee, Collection<DesignElementDataVector> dataVectors ) {
        if ( dataVectors == null || dataVectors.size() == 0 )
            throw new IllegalArgumentException( "No data vectors in " + ee );
    }

    // Delete old links for this expressionexperiment
    private void deleteOldLinks( LinkAnalysis la ) {
        ExpressionExperiment expressionExperiment = la.getExpressionExperiment();
        log.info( "Deleting any old links for " + expressionExperiment + " ..." );
        ppService.deleteLinks( expressionExperiment );
    }

    /**
     * Somewhat misnamed. It fills in the probe2gene map for the linkAnalysis, but also returns a map of composite
     * sequence to vector.
     * 
     * @param la
     * @param dataVectors
     * @return map of probes to vectors.
     */
    @SuppressWarnings("unchecked")
    private void getProbe2GeneMap( LinkAnalysis la, Collection<DesignElementDataVector> dataVectors ) {
        log.info( "Getting probe-to-gene map" );

        Collection<CompositeSequence> probesForVectors = new HashSet<CompositeSequence>();
        for ( DesignElementDataVector v : dataVectors ) {
            CompositeSequence cs = ( CompositeSequence ) v.getDesignElement();
            probesForVectors.add( cs );
        }
        // Map<CompositeSequence, Collection<Gene>> probeToGeneMap = csService.getGenes( probesForVectors );
        // la.setProbeToGeneMap( probeToGeneMap );

        Map<CompositeSequence, Map<PhysicalLocation, Collection<BlatAssociation>>> specificityData = csService
                .getGenesWithSpecificity( probesForVectors );

        /*
         * Convert the specificity
         */
        Map<CompositeSequence, Collection<Collection<Gene>>> probeToGeneMap = new HashMap<CompositeSequence, Collection<Collection<Gene>>>();
        for ( CompositeSequence cs : specificityData.keySet() ) {
            if ( !probeToGeneMap.containsKey( cs ) ) {
                probeToGeneMap.put( cs, new HashSet() );
            }
            Map<PhysicalLocation, Collection<BlatAssociation>> plba = specificityData.get( cs );
            for ( PhysicalLocation pl : plba.keySet() ) {
                Collection<Gene> cluster = new HashSet<Gene>();
                for ( BlatAssociation bla : plba.get( pl ) ) {
                    Gene gene = bla.getGeneProduct().getGene();
                    cluster.add( gene );
                }
                probeToGeneMap.get( cs ).add( cluster );
            }
        }

        // if ( la.getConfig().useKnownGenesOnly() ) {
        // log.info( "Removing probes that assay non 'known genes'" );
        // Collection<DesignElement> els = ExpressionExperimentFilter.getProbesForKnownGenes( probeToGeneMap );
        // probeToGeneMap.keySet().removeAll( els );
        // }

        la.setProbeToGeneMap( probeToGeneMap );
    }

    /**
     * @param dataVectors
     * @return map of probes to vectors.
     */
    private Map<CompositeSequence, DesignElementDataVector> getProbe2VectorMap(
            Collection<DesignElementDataVector> dataVectors ) {
        Map<CompositeSequence, DesignElementDataVector> p2v = new HashMap<CompositeSequence, DesignElementDataVector>();
        for ( DesignElementDataVector v : dataVectors ) {
            CompositeSequence cs = ( CompositeSequence ) v.getDesignElement();
            p2v.put( cs, v );
        }
        return p2v;
    }

    /**
     * @param numColumns
     * @param w
     * @param c helper class
     * @param metric e.g. Pearson Correlation
     * @param analysisObj
     * @return
     */
    private Probe2ProbeCoexpression initCoexp( int numColumns, double w, Creator c, QuantitationType metric,
            ProbeCoexpressionAnalysis analysisObj ) {
        Probe2ProbeCoexpression ppCoexpression = c.create();
        ppCoexpression.setScore( w );
        ppCoexpression.setPvalue( CorrelationStats.pvalue( w, numColumns ) );
        ppCoexpression.setMetric( metric );
        ppCoexpression.setSourceAnalysis( analysisObj );
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
     * @param analysisObj
     * @param c class that can create instances of the correct type of probe2probecoexpression.
     * @return
     */
    private void persist( int numColumns, List<Probe2ProbeCoexpression> p2plinks, int i, double w,
            DesignElementDataVector v1, DesignElementDataVector v2, QuantitationType metric,
            ProbeCoexpressionAnalysis analysisObj, Creator c ) {

        Probe2ProbeCoexpression ppCoexpression = initCoexp( numColumns, w, c, metric, analysisObj );

        ppCoexpression.setFirstVector( v1 );
        ppCoexpression.setSecondVector( v2 );

        p2plinks.add( ppCoexpression );

        if ( i % LINK_BATCH_SIZE == 0 ) {
            this.ppService.create( p2plinks );
            p2plinks.clear();
        }
    }

    private void removeNonKnownGenes( Collection<Gene> cluster ) {
        for ( Iterator<Gene> iterator = cluster.iterator(); iterator.hasNext(); ) {
            Gene gene = iterator.next();
            if ( gene instanceof PredictedGene || gene instanceof ProbeAlignedRegion ) {
                iterator.remove();
            }
        }
    }

    /**
     * Persist the links to the database. This takes care of saving a 'flipped' version of the links.
     * <p>
     * Note that if "known genes only" is set, then links between probes that _only_ target other types of genes will
     * not be stored. However, because the links are stored at the probe level, the links saved will potentially
     * includes links between any type of gene (but always at least between two known genes).
     * 
     * @param p2v
     * @param la
     */
    private void saveLinks( Map<CompositeSequence, DesignElementDataVector> p2v, LinkAnalysis la ) {

        if ( useDB ) {
            deleteOldLinks( la );
            // Create new analysis object.
            ProbeCoexpressionAnalysis analysisObj = la.getAnalysisObj();
            analysisObj = ( ProbeCoexpressionAnalysis ) persisterHelper.persist( analysisObj );
            la.setAnalysisObj( analysisObj );
        }

        log.info( "Start submitting data to database." );
        StopWatch watch = new StopWatch();
        watch.start();

        ObjectArrayList links = la.getKeep();

        /*
         * Important implementation note: For efficiency reason, it is important that they be stored in order of "x"
         * (where x is the index of the first DEDV in the correlation matrix, not the ID of the DEDV.) in the database:
         * so all all links with probe=x are clustered. (the actual order of x1 vs x2 doesn't matter, just so long they
         * are clustered). This makes retrievals much faster for the most common types of queries. Note that this relies
         * on expected behavior that links.sort() will sort by the x coordinate. It also requires that when we persist
         * the elements, we retain the order.
         */
        links.sort();

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
        links.mergeSortFromTo( 0, links.size() - 1, new Comparator() {
            public int compare( Object arg0, Object arg1 ) {
                if ( arg0 == null || arg1 == null ) return 1;
                Link a = ( Link ) arg0;
                Link b = ( Link ) arg1;

                if ( a.gety() < b.gety() ) {
                    return -1;
                } else if ( a.gety() > b.gety() ) {
                    return 1;
                } else {
                    if ( a.getx() < b.getx() ) {
                        return -1;
                    } else if ( a.getx() > b.getx() ) {
                        return 1;
                    }
                    return 0;
                }
            }
        } );

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
        log.info( "Seconds to process " + links.size() + " links plus flipped versions:" + watch.getTime() / 1000.0 );
    }

    /**
     * @param p2v
     * @param la
     * @param links
     * @boolean flip
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

        List<Probe2ProbeCoexpression> p2plinkBatch = new ArrayList<Probe2ProbeCoexpression>();
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
                persist( numColumns, p2plinkBatch, i, w, v2, v1, metric, la.getAnalysisObj(), c );
            } else {
                persist( numColumns, p2plinkBatch, i, w, v1, v2, metric, la.getAnalysisObj(), c );
            }

            if ( i > 0 && i % 50000 == 0 ) {
                log.info( i + " links loaded into the database" );
            }

        }

        // last batch.
        if ( p2plinkBatch.size() > 0 ) {
            this.ppService.create( p2plinkBatch );
            p2plinkBatch.clear();
        }
    }

    /**
     * Initializes the LinkAnalysis object; populates the probe2gene map.
     * 
     * @param ee
     * @param la
     * @param dataVectors
     * @param eeDoubleMatrix
     */
    private void setUpForAnalysis( ExpressionExperiment ee, LinkAnalysis la,
            Collection<DesignElementDataVector> dataVectors, ExpressionDataDoubleMatrix eeDoubleMatrix ) {

        la.setDataMatrix( eeDoubleMatrix );
        la.setTaxon( eeService.getTaxon( ee.getId() ) );
        la.setExpressionExperiment( ee );

        getProbe2GeneMap( la, dataVectors );
    }

    /**
     * Write links as text. If "known genes only", only known genes will be displayed, even if the probe in question
     * targets other "types" of genes.
     * 
     * @param la
     * @param wr
     */
    private void writeLinks( final LinkAnalysis la, Writer wr ) throws IOException {
        wr.write( la.getConfig().toString() );
        Map<CompositeSequence, Collection<Collection<Gene>>> probeToGeneMap = la.getProbeToGeneMap();
        ObjectArrayList links = la.getKeep();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits( 4 );

        Transformer officialSymbolExtractor = new Transformer() {
            public Object transform( Object input ) {
                Gene g = ( Gene ) input;

                return g.getOfficialSymbol();
            }
        };

        int i = 0;
        for ( int n = links.size(); i < n; i++ ) {
            Object val = links.getQuick( i );
            if ( val == null ) continue;
            Link m = ( Link ) val;
            Double w = m.getWeight();

            assert w != null;

            DesignElement p1 = la.getMetricMatrix().getProbeForRow( la.getDataMatrix().getRowElement( m.getx() ) );
            DesignElement p2 = la.getMetricMatrix().getProbeForRow( la.getDataMatrix().getRowElement( m.gety() ) );

            Collection<Collection<Gene>> g1 = probeToGeneMap.get( p1 );
            Collection<Collection<Gene>> g2 = probeToGeneMap.get( p2 );

            List<String> genes1 = new ArrayList<String>();
            for ( Collection<Gene> cluster : g1 ) {
                if ( la.getConfig().useKnownGenesOnly() ) {
                    removeNonKnownGenes( cluster );
                }
                String t = StringUtils.join( new TransformIterator( cluster.iterator(), officialSymbolExtractor ), "," );
                if ( StringUtils.isBlank( t ) ) {
                    continue;
                }
                genes1.add( t );
            }

            List<String> genes2 = new ArrayList<String>();
            for ( Collection<Gene> cluster : g2 ) {
                if ( la.getConfig().useKnownGenesOnly() ) {
                    removeNonKnownGenes( cluster );
                }
                String t = StringUtils.join( new TransformIterator( cluster.iterator(), officialSymbolExtractor ), "," );
                if ( StringUtils.isBlank( t ) ) {
                    continue;
                }
                genes2.add( t );
            }

            if ( genes2.size() == 0 || genes1.size() == 0 ) {
                continue;
            }

            wr.write( p1.getId() + "\t" + p2.getId() + "\t" + StringUtils.join( genes1.iterator(), "|" ) + "\t"
                    + StringUtils.join( genes2.iterator(), "|" ) + "\t" + nf.format( w ) + "\n" );

            if ( i > 0 && i % 50000 == 0 ) {
                log.info( i + " links printed" );
            }

        }
        wr.flush();
        log.info( "Done, " + i + " links printed" );
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

}
