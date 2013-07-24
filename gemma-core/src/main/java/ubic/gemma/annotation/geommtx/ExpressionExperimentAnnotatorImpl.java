/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.annotation.geommtx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.CacheManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.GEOMMTx.ProjectRDFModelTools;
import ubic.GEOMMTx.Text2Owl;
import ubic.GEOMMTx.Vocabulary;
import ubic.GEOMMTx.filters.AbstractFilter;
import ubic.GEOMMTx.filters.CUIIRIFilter;
import ubic.GEOMMTx.filters.CUISUIFilter;
import ubic.GEOMMTx.filters.FMANullsFilter;
import ubic.GEOMMTx.filters.UninformativeFilter;
import ubic.gemma.annotation.geommtx.evaluation.CheckHighLevelSpreadSheetReader;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.AutomatedAnnotationEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Service that runs the "GEOMMTx" automated tagger on expression experiments. Because the annotator is
 * memory-intensive, this is controlled via a configuration setting (see default.properties)
 * 
 * @author lfrench, paul
 * @version $Id$
 */
@Component
public class ExpressionExperimentAnnotatorImpl implements InitializingBean, ExpressionExperimentAnnotator {

    // for RDF output.
    public static String gemmaNamespace = "http://bioinformatics.ubc.ca/Gemma/";

    protected static Log log = LogFactory.getLog( ExpressionExperimentAnnotatorImpl.class );

    private static AtomicBoolean initializing = new AtomicBoolean( false );

    private final static String MODEL_OUTPUT_PATH = ConfigUtils.getAnalysisStoragePath();

    private static AtomicBoolean ready = new AtomicBoolean( false );

    private static Text2Owl text2Owl;

    /**
     * @return true if the annotator is ready to be used.
     */
    public static boolean ready() {
        return ready.get();
    }

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PredictedCharacteristicFactory charGen;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private List<AbstractFilter> filters;

    @Autowired
    private OntologyService ontologyService;

    private Map<String, Set<String>> rejectedFromReview;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {

        boolean activated = ConfigUtils.getBoolean( MMTX_ACTIVATION_PROPERTY_KEY );

        if ( !activated ) {
            log.debug( "Automated tagger disabled; to turn on set " + MMTX_ACTIVATION_PROPERTY_KEY
                    + "=true in your Gemma.properties file" );
            return;
        }

        init();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator#annotate(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, boolean)
     */
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator#annotate(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, boolean)
     */
    @Override
    public Collection<Characteristic> annotate( ExpressionExperiment e, boolean force ) {

        if ( !ready.get() ) {
            log.warn( "Automated tagging is not available; switch it on (set " + MMTX_ACTIVATION_PROPERTY_KEY
                    + "=true in your configuration file) and/or wait for it to finish initializing" );
            return new HashSet<Characteristic>();
        }

        if ( !needToRun( e, force ) ) {
            log.info( "Annotation has already been run on this experiment" );
            return new HashSet<Characteristic>();
        }

        log.info( "Annotating: " + e );

        e = expressionExperimentService.thawLite( e );

        long time = System.currentTimeMillis();

        Model model = initModel( e );

        Set<String> predictedAnnotations;

        // business.
        annotateAll( model, e );

        // all the above calls for each text source builds a RDF model
        predictedAnnotations = ProjectRDFModelTools.getURLsFromSingle( model );
        log.debug( "Before filtering:" );
        for ( String string : predictedAnnotations ) {
            log.debug( string );
        }

        // apply the filters one by one
        for ( AbstractFilter filter : filters ) {
            int result = filter.filter( model );
            log.debug( "Removed: " + result );
        }

        log.debug( "After filtering:" );
        for ( String string : predictedAnnotations ) {
            log.debug( string );
        }

        log.debug( "Final Mentions:" + ProjectRDFModelTools.getMentionCount( model ) );

        // write the file somewhere we may also want to write the file before
        // it's filtered
        writeModel( model, e );
        log.debug( ( ( System.currentTimeMillis() - time ) / 1000 ) + "s for whole experiment" );

        // convert the mentions into annotations using a sparql query
        predictedAnnotations = ProjectRDFModelTools.getURLsFromSingle( model );

        Collection<Characteristic> characteristics = e.getCharacteristics();
        Collection<String> alreadyHas = new HashSet<String>();
        for ( Characteristic ch : characteristics ) {
            // alreadyHas.add(ch.getValue());
            if ( ch instanceof VocabCharacteristic ) {
                String valueUri = ( ( VocabCharacteristic ) ch ).getValueUri();
                alreadyHas.add( valueUri );
            }
        }

        Collection<String> rejectedBy100Eval = rejectedFromReview.get( e.getId() + "" );

        // for each URI print it and its label and get VocabCharacteristic
        // to represent it
        Collection<Characteristic> newChars = new HashSet<Characteristic>();
        for ( String URI : predictedAnnotations ) {

            if ( alreadyHas.contains( URI ) ) {
                log.debug( "Experiment already has tag " + charGen.getLabel( URI ) + ", skipping" );
                continue;
            }

            if ( !charGen.hasLabel( URI ) ) {
                log.debug( "No label for " + URI + ", skipping" );
                continue;
            }

            if ( rejectedBy100Eval != null && rejectedBy100Eval.contains( URI ) ) {
                log.debug( "Tag was Rejected by previous review of 100 experiments " + charGen.getLabel( URI ) + " "
                        + URI + ", skipping" );
                continue;
            }

            Characteristic c = charGen.getCharacteristic( URI );

            if ( c.getCategory() == null ) {
                log.debug( "No category for " + URI + ", skipping" );
                continue;
            }

            log.debug( e + " " + c.getCategory() + " -> " + charGen.getLabel( URI ) + " - " + URI );

            newChars.add( c );

        }

        // attach the Characteristic to the experiment. Comment out these lines if you don't want to save the
        // results to the database
        log.info( "Saving " + newChars.size() + " new annotations for " + e );
        ontologyService.saveExpressionExperimentStatements( newChars, e );
        audit( e );

        return newChars;
    }

    /**
     * force initialization of MMTx
     */
    @Override
    public void init() {

        if ( initializing.get() ) {
            log.info( "Already loading..." );
            return;
        }

        charGen.init();

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {

                initializing.set( true );

                try {
                    text2Owl = new Text2Owl( cacheManager );
                } catch ( Exception e ) {
                    log.warn( "Automated tagger could not be initialized: " + e.getMessage(), e );
                    return;
                } finally {
                    ready.set( false );
                    initializing.set( false );
                }

                // order matters for these filters
                filters = new LinkedList<AbstractFilter>();
                filters.add( new CUISUIFilter() );
                filters.add( new CUIIRIFilter() );
                FMANullsFilter birnFMANull = new FMANullsFilter( ontologyService.getFmaOntologyService() );
                filters.add( birnFMANull );
                filters.add( new UninformativeFilter() );

                // use the HighLevel Review of 100 experiments.xls spreadsheet
                CheckHighLevelSpreadSheetReader highLevelResults = new CheckHighLevelSpreadSheetReader();
                rejectedFromReview = highLevelResults.getRejectedAnnotations();

                ready.set( true );
                initializing.set( false );
            }
        }, "MMTX initialization" );

        if ( initializing.get() ) return; // no need to start it, we already finished, somehow
        loadThread.setDaemon( true ); // So vm doesn't wait on these threads to shutdown (if shutting down)
        loadThread.start();

        log.info( "Started MMTX initialization" );
    }

    /**
     * @param model
     * @param experiment
     */
    private void annotateAll( Model model, ExpressionExperiment experiment ) {
        log.info( "Examining name" );
        annotateName( model, experiment );

        log.info( "Examining description" );
        annotateDescription( model, experiment );

        log.info( "Examining publications" );
        annotateReferences( model, experiment );

        log.info( "Examining experimental design" );
        annotateExperimentalDesign( model, experiment );

        log.info( "Examining bioAssays" );
        annotateBioAssays( model, experiment );
    }

    /**
     * @param model
     * @param experiment
     */
    private void annotateBioAssays( Model model, ExpressionExperiment experiment ) {

        int i = 0;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( BioAssay ba : experiment.getBioAssays() ) {
            String nameSpaceBase = "bioAssay/" + ba.getId() + "/";
            if ( ba.getName() != null ) {
                doRDF( model, experiment.getId(), ba.getName().replace( "Expr(", "Expr " ), nameSpaceBase + "name" );
            }

            if ( ba.getDescription() != null ) {
                doRDF( model, experiment.getId(), ba.getDescription(), nameSpaceBase + "description" );
            }

            if ( ++i % 10 == 0 && timer.getTime() > 10000 ) {
                log.info( i + " bioassays processed" );
                timer.reset();
                timer.start();
            }

        }
    }

    /**
     * @param model
     * @param experiment
     */
    private void annotateDescription( Model model, ExpressionExperiment experiment ) {
        String description = experiment.getDescription();
        doRDF( model, experiment.getId(), description, "experiment/" + experiment.getId() + "/description" );
    }

    /**
     * @param model
     * @param experiment
     */
    private void annotateExperimentalDesign( Model model, ExpressionExperiment experiment ) {
        ExperimentalDesign design = experiment.getExperimentalDesign();

        // Special case

        if ( design != null ) {
            String nameSpaceBase = "experimentalDesign/" + design.getId() + "/";
            if ( design.getDescription() != null ) {
                doRDF( model, experiment.getId(), design.getDescription(), nameSpaceBase + "description" );
            }

            Collection<ExperimentalFactor> factors = design.getExperimentalFactors();
            if ( factors != null ) {
                for ( ExperimentalFactor factor : factors ) {
                    String nameSpaceBaseFactors = "experimentalFactor/" + factor.getId() + "/";
                    doRDF( model, experiment.getId(), factor.getName(), nameSpaceBaseFactors + "name" );

                    doRDF( model, experiment.getId(), factor.getDescription(), nameSpaceBaseFactors + "description" );

                }
            }
        }
    }

    /**
     * @param model
     * @param experiment
     */
    private void annotateName( Model model, ExpressionExperiment experiment ) {
        // experiment then desc then name
        doRDF( model, experiment.getId(), experiment.getName(), "experiment/" + experiment.getId() + "/name" );

    }

    /**
     * @param model
     * @param experiment
     */
    private void annotateReferences( Model model, ExpressionExperiment experiment ) {
        BibliographicReference ref = experiment.getPrimaryPublication();
        if ( ref != null ) {
            String nameSpaceBase = "primaryReference/" + ref.getId() + "/";

            log.debug( "in title doRDF" );
            doRDF( model, experiment.getId(), ref.getTitle(), nameSpaceBase + "title" );
            if ( ref.getAbstractText() != null ) {
                log.debug( "in abstract doRDF" );
                doRDF( model, experiment.getId(), ref.getAbstractText(), nameSpaceBase + "abstract" );
            }
        }

        // Secondary Publications
        Collection<BibliographicReference> others = experiment.getOtherRelevantPublications();
        if ( others != null ) {
            for ( BibliographicReference other : others ) {
                String nameSpaceBase = "otherReference/" + other.getId() + "/";
                doRDF( model, experiment.getId(), other.getTitle(), nameSpaceBase + "title" );

                if ( other.getAbstractText() != null ) {
                    doRDF( model, experiment.getId(), other.getAbstractText(), nameSpaceBase + "abstract" );
                }
            }
        }
    }

    /**
     * @param experiment
     */
    private void audit( ExpressionExperiment experiment ) {
        this.auditTrailService.addUpdateEvent( experiment, AutomatedAnnotationEvent.Factory.newInstance(), "" );
    }

    /**
     * So this calls mmtx get the phrases, concepts and mappings and links them to the root node (the experiment)
     * 
     * @param text the text to be annotated
     * @param desc the description of the text, its appended on to the URI
     */
    private void doRDF( Model model, Long eeId, String text, String desc ) {
        if ( StringUtils.isBlank( text ) ) return;
        String textToProcess = text;

        // FIXME these strings should be obtained from GeoConverter, not hardcoded here.
        textToProcess = textToProcess.replaceAll( "Source GEO sample is GSM[0-9]+", "" );
        textToProcess = textToProcess.replaceAll( "Last updated [(]according to GEO[)].+[\\d]{4}", "" );

        String cleanText = desc.replaceAll( "[()]", "" );
        String thisObjectURI = gemmaNamespace + cleanText;
        Resource thisResource = model.createResource( thisObjectURI );

        // connect root to this resource
        Resource root = model.getResource( gemmaNamespace + "experiment/" + eeId );
        root.addProperty( Vocabulary.describedBy, thisResource );

        // this is to avoid text2Owl init times while testing, should be refactored
        if ( text2Owl == null ) return;

        // a bit strange here, since it takes in the root
        text2Owl.processText( textToProcess, thisResource );
    }

    /**
     * @param experiment
     * @return
     */
    private Model initModel( ExpressionExperiment experiment ) {
        Model model = ModelFactory.createDefaultModel();
        String GEOObjectURI = gemmaNamespace + "experiment/" + experiment.getId();
        Resource root = model.createResource( GEOObjectURI );
        root.addProperty( RDFS.label, experiment.getShortName() );
        return model;
    }

    /**
     * @param auditable
     * @param force
     * @return true if force is true OR tagging has not been run on this experiment before.
     */
    private boolean needToRun( ExpressionExperiment auditable, boolean force ) {
        if ( force ) return true;

        List<AuditEvent> events = this.auditTrailService.getEvents( auditable );

        for ( AuditEvent event : events ) {
            if ( event == null ) continue; // legacy of ordered-list, should not happen any more
            AuditEventType eventType = event.getEventType();
            if ( eventType != null && AutomatedAnnotationEvent.class.isAssignableFrom( eventType.getClass() )
                    && !eventType.getClass().getSimpleName().startsWith( "Fail" ) ) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param model
     * @param experiment
     */
    private void writeModel( Model model, ExpressionExperiment experiment ) {
        try {
            File f = new File( MODEL_OUTPUT_PATH );
            if ( !f.exists() ) f.mkdirs();
            FileWriter fout = new FileWriter( MODEL_OUTPUT_PATH + File.separator + experiment.getId() + ".rdf" );
            model.write( fout );
            fout.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
