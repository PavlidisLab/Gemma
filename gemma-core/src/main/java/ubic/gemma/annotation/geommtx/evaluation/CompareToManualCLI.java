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
package ubic.gemma.annotation.geommtx.evaluation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ubic.GEOMMTx.LabelLoader;
import ubic.GEOMMTx.OntologyTools;
import ubic.GEOMMTx.ProjectRDFModelTools;
import ubic.GEOMMTx.evaluation.CheckHighLevelSpreadSheet;
import ubic.GEOMMTx.evaluation.DescriptionExtractor;
import ubic.GEOMMTx.filters.FMANullsFilter;
import ubic.GEOMMTx.filters.UninformativeFilter;
import ubic.GEOMMTx.mappers.DiseaseOntologyMapper;
import ubic.GEOMMTx.mappers.FMALiteMapper;
import ubic.GEOMMTx.mappers.NIFSTDMapper;
import ubic.GEOMMTx.util.SetupParameters;
import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotatorImpl;
import ubic.gemma.apps.GemmaCLI.CommandGroup;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractCLIContextCLI;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC_11;

/**
 * TODO Document Me
 * 
 * @author lfrench
 * @version $Id$
 */
public class CompareToManualCLI extends AbstractCLIContextCLI {

    public static Map<String, Integer> listToFrequencyMap( List<String> input ) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        int i;
        for ( String s : input ) {
            Integer iO = result.get( s );
            if ( iO == null )
                i = 0;
            else
                i = iO.intValue();
            result.put( s, i + 1 );
        }
        return result;
    }
    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.METADATA;
    }
    public static void main( String[] args ) {
        CompareToManualCLI p = new CompareToManualCLI();

        // DocumentRange t = null;

        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public static void printMap( Map<String, Integer> map ) {
        int total = 0;
        for ( String key : map.keySet() ) {
            total += map.get( key );
        }
        for ( String key : map.keySet() ) {
            System.out.println( key + " => " + map.get( key ) + "(" + ( float ) map.get( key ) / ( float ) total + ")" );
        }
    }

    String filename;
    Map<String, String> labels;
    Map<String, Set<String>> manualURLs;

    Map<String, Set<String>> mmtxURLs;

    HashSet<String> originalMMTxIDs;

    DescriptionExtractor de;

    public CompareToManualCLI() {
        labels = new HashMap<String, String>();
    }

    /*
     * Removes null URL's and also URL's from CHEBI Birnlex organismal taxonomy MGED Ontology
     */
    public void cleanURLs() {
        Set<String> datasets = new HashSet<String>( mmtxURLs.keySet() );
        datasets.addAll( manualURLs.keySet() );
        for ( String dataset : datasets ) {
            // Set<String> machineURLs = mmtxURLs.get( dataset );
            Set<String> humanURLs = manualURLs.get( dataset );
            humanURLs.remove( "null" );
            humanURLs.remove( null );
            humanURLs.remove( "" );

            // get rid of MGED URL's
            List<String> removeMe = new LinkedList<String>();
            for ( String url : humanURLs ) {
                if ( url.contains( "MGEDOntology.owl" ) ) {
                    removeMe.add( url );
                }
                if ( url.contains( "owl/CHEBI" ) ) {
                    removeMe.add( url );
                }
                // TAXON REMOVE
                // I didn't have this on the first run, so change it
                if ( url.contains( "OrganismalTaxonomy" ) ) {
                    removeMe.add( url );
                }
            }
            humanURLs.removeAll( removeMe );
        }
    }

    public Set<String> convertURLsToLabels( Set<String> URLs ) {
        Set<String> result = new HashSet<String>();
        for ( String url : URLs )
            result.add( labels.get( url ) );
        return result;
    }

    public void countBadURIs( Map<String, Set<String>> experiments ) {

        OntologyService os = this.getBean( OntologyService.class );

        FMANullsFilter nullFilter = new FMANullsFilter( os.getFmaOntologyService() );
        UninformativeFilter unFilter = new UninformativeFilter();

        int bad = 0;
        for ( String exp : experiments.keySet() ) {
            for ( String url : experiments.get( exp ) ) {
                // if its rejected by either filter
                if ( !nullFilter.accept( url ) || !unFilter.accept( url ) ) {
                    bad++;
                    log.info( exp + " BAD:" + url );
                }
            }
        }
        log.info( "Number of bad URLS:" + bad );
    }

    public int countMMTxHits( String URI ) {
        int hits = 0;

        for ( String exp : mmtxURLs.keySet() ) {
            Set<String> anno = mmtxURLs.get( exp );
            if ( anno.contains( URI ) ) {
                System.out.println( exp );
                System.out.println( de.getDecriptionType( exp, URI ) );
                hits++;
            }

        }
        return hits;
    }

    public void examineSingleSource( String source ) {
        loadMappings();
        removeExceptOneSource( source );
        System.out.println( "SOURCE = " + source + " ---------------" );
        printStats();
        System.out.println( "SOURCE 100 Stats= " + source + " ---------------" );
        loadInFinalEvaluation();
        print100Stats();
        System.out.println( "========END= " + source + " ---------------" );
    }

    public void filterAllURLs( String keepString ) {
        filter( keepString, manualURLs );
        filter( keepString, mmtxURLs );
    }

    public void filterHumanURLs( String keepString ) {
        filter( keepString, manualURLs );
    }

    public Set<String> getIntersectExperiments() {
        Set<String> intersect = new HashSet<String>( manualURLs.keySet() );
        intersect.retainAll( mmtxURLs.keySet() );
        return intersect;
    }

    public Set<String> getIntersection( String dataset ) {
        Set<String> machineURLs = mmtxURLs.get( dataset );
        Set<String> humanURLs = manualURLs.get( dataset );
        Set<String> intersect = new HashSet<String>( humanURLs );
        intersect.retainAll( machineURLs );
        return intersect;
    }

    public Set<String> getIntersectionByName( String dataset ) {
        Set<String> machineLabels = convertURLsToLabels( mmtxURLs.get( dataset ) );
        Set<String> humanLabels = convertURLsToLabels( manualURLs.get( dataset ) );
        Set<String> intersect = new HashSet<String>( humanLabels );
        intersect.retainAll( machineLabels );
        return intersect;
    }

    public Set<String> getMissed( String dataset ) {
        Set<String> machineURLs = mmtxURLs.get( dataset );
        Set<String> humanURLs = manualURLs.get( dataset );
        Set<String> missed = new HashSet<String>( humanURLs );
        missed.removeAll( machineURLs );
        return missed;
    }

    /*
     * Finds out how many mappings we fail to have for the human predictions
     */
    public void howManyMissingMappings() {
        Collection<String> result;

        filterAndPrint( "/owl/FMA#", false );
        FMALiteMapper fma = new FMALiteMapper();
        result = removeFromHumanSeen( fma.getAllURLs() );
        System.out.println( "Seen manual FMA URL's that we have no mapping to:" + result.size() );

        filterAndPrint( "/owl/DOID#", false );
        DiseaseOntologyMapper DO = new DiseaseOntologyMapper();
        result = removeFromHumanSeen( DO.getAllURLs() );
        System.out.println( "Seen manual DO URL's that we have no mapping to:" + result.size() );

        filterAndPrint( "birnlex", false );
        NIFSTDMapper BIRN = new NIFSTDMapper();
        result = removeFromHumanSeen( BIRN.getAllURLs() );
        System.out.println( "Seen manual NIFSTDMapper URL's that we have no mapping to:" + result.size() );

        loadMappings();
    }

    public String lineSpacedSet( Set<String> input ) {
        List<String> outputList = new LinkedList<String>();
        for ( String line : input ) {
            outputList.add( labels.get( line ) + "->" + line );
        }
        Collections.sort( outputList );

        String result = "";
        for ( String line : outputList )
            result += line + "\n";
        return result;
    }

    public void loadInFinalEvaluation() {
        CheckHighLevelSpreadSheetReader reader = new CheckHighLevelSpreadSheetReader();
        Map<String, Set<String>> accepted = null;

        accepted = reader.getAcceptedAnnotations();

        for ( String exp : accepted.keySet() ) {
            Set<String> anots = accepted.get( exp );
            // add them all to the manual annotations
            manualURLs.get( exp ).addAll( anots );
        }
    }

    public void makeSpreadSheet() throws Exception {
        Map<String, Set<String>> newPredictions = new HashMap<String, Set<String>>();
        for ( String dataset : originalMMTxIDs ) {
            Set<String> machineURLs = new HashSet<String>( mmtxURLs.get( dataset ) );
            Set<String> humanURLs = manualURLs.get( dataset );
            machineURLs.removeAll( humanURLs );
            newPredictions.put( dataset, machineURLs );
        }

        CheckHighLevelSpreadSheet spreadsheet = new CheckHighLevelSpreadSheet( "HighLevelPredictionsPlusOne2.xls" );
        spreadsheet.populate( newPredictions, labels, 101 );
        spreadsheet.save();
    }

    public void print100Stats() {
        setTo100EvalSet();
        // the below expID's are the ones we choose for manual curation
        printStats();
    }

    public void printForTagCloud( Map<String, Set<String>> experiments ) {
        int nulls = 0;
        for ( String exp : experiments.keySet() ) {
            for ( String url : experiments.get( exp ) ) {
                // System.out.print( url + "->" );
                if ( labels.get( url ) == null ) {
                    nulls++;
                    // log.info(url);
                    continue;
                }
                System.out.println( labels.get( url ).replace( " ", "~" ) );
            }
        }
        log.info( "Number of null labels:" + nulls );
    }

    public void printHumanForTagCloud() {
        System.out.println( "--------HUMAN----------" );
        printForTagCloud( manualURLs );
    }

    public void printMissedForTagCloud() {
        System.out.println( "--------MISSED----------" );
        printForTagCloud( getMissedURLS() );
    }

    public void printMMTxForTagCloud() {
        System.out.println( "--------MMTx----------" );
        printForTagCloud( mmtxURLs );
    }

    public Collection<String> removeFromHumanSeen( Set<String> removeSet ) {
        // List<String> seenHumanURLs = new LinkedList<String>();
        Set<String> seenHumanURLs = new HashSet<String>();

        for ( Set<String> seenURLs : manualURLs.values() ) {
            seenHumanURLs.addAll( seenURLs );
        }
        seenHumanURLs.removeAll( removeSet );
        return seenHumanURLs;
    }

    // if the dataset is not processed by humans or mmtx, then set its annotations to empty set
    public void setNullstoEmpty() {
        Set<String> datasets = new HashSet<String>( mmtxURLs.keySet() );
        datasets.addAll( manualURLs.keySet() );
        for ( String dataset : datasets ) {
            if ( mmtxURLs.get( dataset ) == null ) {
                mmtxURLs.put( dataset, new HashSet<String>() );
            }
            if ( manualURLs.get( dataset ) == null ) {
                manualURLs.put( dataset, new HashSet<String>() );
            }

        }
    }

    public void setTo100EvalSet() {
        String[] exps100 = new String[] { "107", "114", "129", "137", "140", "155", "159", "167", "198", "199", "2",
                "20", "206", "211", "213", "216", "219", "221", "232", "241", "243", "245", "246", "257", "258", "26",
                "265", "267", "268", "277", "288", "295", "299", "302", "319", "323", "35", "36", "363", "368", "369",
                "374", "375", "380", "385", "389", "39", "403", "406", "446", "454", "455", "484", "49", "504", "510",
                "522", "524", "528", "533", "535", "54", "544", "548", "559", "571", "579", "587", "588", "591", "595",
                "596", "597", "6", "602", "606", "609", "613", "617", "619", "625", "627", "633", "639", "64", "647",
                "651", "653", "657", "66", "663", "667", "672", "699", "74", "76", "79", "80", "90", "95" };
        originalMMTxIDs = new HashSet<String>( Arrays.asList( exps100 ) );
    }

    public void showMe( String experimentID ) {
        Set<String> machineURLs = new HashSet<String>( mmtxURLs.get( experimentID ) );
        Set<String> humanURLs = new HashSet<String>( manualURLs.get( experimentID ) );
        Set<String> intersect = getIntersection( experimentID );

        machineURLs.removeAll( intersect );
        humanURLs.removeAll( intersect );

        System.out.println( "MMTx URLs:" );
        System.out.println( lineSpacedSet( machineURLs ) );
        System.out.println( "Human URLs:" );
        System.out.println( lineSpacedSet( humanURLs ) );
        if ( intersect.size() == 0 ) {
            System.out.println( "No Intersection URLs" );
        } else {

            System.out.println( "Intersection URLs:" );
            System.out.println( lineSpacedSet( intersect ) );
        }
    }

    @Override
    protected void buildOptions() {
    }

    @Override
    protected Exception doWork( String[] args ) {
        // System.out.println(ProjectRDFModelTools.getMentionCount( "mergedRDFBirnLexUpdate.afterrejected.rdf"));
        // /System.out.println(ProjectRDFModelTools.getMentionCount("mergedRDFBirnLexUpdate.afterUseless.rdf");
        // System.out.println(ProjectRDFModelTools.getMentionCount( "mergedRDF.firstrun.rdf"));
        // System.out.println(getMentionCount( "mergedRDFBirnLexUpdate.rdf"));
        // System.out.println(getMentionCount( "mergedRDFBirnLexUpdateNoExp.rdf"));
        // System.out.println(getMentionCount( "mergedRDFBirnLexUpdate.afterrejected.rdf"));
        // System.out.println(getMentionCount( "mergedRDFBirnLexUpdate.afterUseless.rdf"));
        // System.exit(1);

        // FOR SECOND RUN switch OrganismalTaxonomy in cleanURL's
        // filename = "mergedRDF.rejected.removed.rdf"; //second run
        filename = "mergedRDFBirnLexUpdate.afterUseless.rdf"; // first run latest

        filename = "mergedRDFBirnLexUpdate.afterUseless.axon4.filtered.rdf";
        // System.out.println(ProjectRDFModelTools.getMentionCount(filename));

        long totaltime = System.currentTimeMillis();
        Exception err = processCommandLine( args );
        if ( err != null ) return err;

        // System.exit( 1 );

        // long time = System.currentTimeMillis();

        // get human and mmtx mappings
        try {
            de = new DescriptionExtractor( filename );
            getMappings();
        } catch ( IOException e ) {
            return e;
        }

        log.info( "gemma intersect mmtx size=" + getIntersectExperiments().size() );
        // log.info( intersect );

        Set<String> minus = new HashSet<String>( mmtxURLs.keySet() );
        minus.removeAll( manualURLs.keySet() );
        log.info( "mmtx minus gemma size=" + minus.size() );
        log.info( minus );
        // getHumanMappingsFromServer();
        setNullstoEmpty();
        cleanURLs();

        // writeExperimentTitles();

        printStats();

        log.info( "HUMAN" );
        countBadURIs( manualURLs );

        // print100Stats();
        // loadInFinalEvaluation();
        // print100Stats();

        // showMe("672");
        // log.info( countMMTxHits( "http://purl.org/obo/owl/FMA#FMA_67093" ) );
        // printSourceStats();

        // System.out.println( "Just one" );
        // filterForOneSource();
        // printStats();
        // loadMappings();
        //
        // System.out.println( "Two or more" );
        // filterForTwoOrMoreSources();
        // printStats();
        // loadInFinalEvaluation();
        // print100Stats();
        // loadMappings();
        //
        // // removeAbstractSource();
        // System.out.println( "Remove abstracts" );
        // removeAbstractOneSource();
        // printStats();

        // print100Stats();

        // howManyMissingMappings();

        // printMMTxForTagCloud();
        // printHumanForTagCloud();
        // printMissedForTagCloud();

        // getHumanMappingsFromServer();

        boolean evalSetOnly = false;
        filterAndPrint( "/owl/FMA#", evalSetOnly );
        filterAndPrint( "/owl/DOID#", evalSetOnly );
        filterAndPrint( "birnlex", evalSetOnly );

        // printStats();

        // examineSingleSource( "primaryReference/abstract" );
        // examineSingleSource( "bioAssay/name" );
        // examineSingleSource( "bioAssay/description" );
        // examineSingleSource( "experiment/name" );
        // examineSingleSource( "experiment/description" );
        // examineSingleSource( "primaryReference/title" );

        // System.out.println( "two or more" );
        // loadMappings();
        // filterForTwoOrMoreSources();
        // printStats();
        // loadInFinalEvaluation();
        // print100Stats();

        // filterAndPrint( "/owl/DOID#", false );
        //
        // ParentFinder parentFinder = new ParentFinder();
        // try {
        // parentFinder.init();
        // } catch ( Exception e ) {
        // e.printStackTrace();
        // }
        // mmtxURLs = parentFinder.expandToParents( mmtxURLs );
        //
        // System.out.println( "Children/leaves stats" );
        // printStats();
        //
        // mmtxURLs = parentFinder.reduceToLeaves( mmtxURLs );
        // System.out.println( "Leaves only" );
        // printStats();
        //
        // System.out.println( "Nulls: " + parentFinder.nullTerms );

        // for ( String dataset : originalMMTxIDs ) {
        // System.out.println( "-----------------------------------" );
        // System.out.println( "ID" + dataset );
        // showMe( dataset );
        // }

        // printComparisonsCSV();

        // printMissedURLs();

        // try {
        // makeSpreadSheet();
        // } catch ( Exception e ) {
        // e.printStackTrace();
        // }

        // printROCCurveValues( filename );

        System.out.println( "Total time:" + ( System.currentTimeMillis() - totaltime ) / 1000 + "s" );
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    private void filter( String keepString, Map<String, Set<String>> map ) {
        for ( String dataset : map.keySet() ) {
            Set<String> URLs = map.get( dataset );
            List<String> removeMe = new LinkedList<String>();
            for ( String url : URLs ) {
                if ( url.contains( keepString ) ) {
                    // keep it
                } else {
                    removeMe.add( url );
                }
            }
            URLs.removeAll( removeMe );
        }
    }

    // return this.getClass().getName() + ".mappings";

    private void filterAndPrint( String filterString, boolean evalSet ) {
        loadMappings();
        if ( evalSet ) {
            setTo100EvalSet();
            loadInFinalEvaluation();
        }
        log.info( filterString );
        filterAllURLs( filterString );
        printStats();
    }

    @SuppressWarnings("unused")
    private void filterForOneSource() {
        for ( String dataset : originalMMTxIDs ) {
            Set<String> machineURLs = mmtxURLs.get( dataset );
            Set<String> oneSourceMachineURLs = new HashSet<String>();
            for ( String URI : machineURLs ) {
                if ( de.getDecriptionType( dataset, URI ).size() != 1 ) continue;
                oneSourceMachineURLs.add( URI );
            }
            mmtxURLs.put( dataset, oneSourceMachineURLs );
        }
    }

    @SuppressWarnings("unused")
    private void filterForTwoOrMoreSources() {
        for ( String dataset : originalMMTxIDs ) {
            Set<String> machineURLs = mmtxURLs.get( dataset );
            Set<String> notOneSourceMachineURLs = new HashSet<String>();
            for ( String URI : machineURLs ) {
                if ( de.getDecriptionType( dataset, URI ).size() < 2 ) continue;
                notOneSourceMachineURLs.add( URI );
            }
            mmtxURLs.put( dataset, notOneSourceMachineURLs );
        }
    }

    private Map<String, Set<String>> getHumanMappingsFromDisk() throws Exception {
        Map<String, Set<String>> result;
        try (ObjectInputStream o = new ObjectInputStream( new FileInputStream(
                SetupParameters.getString( "gemma.annotator.cachedGemmaAnnotations" ) ) );) {
            result = ( Map<String, Set<String>> ) o.readObject();
        }

        log.info( "Loaded Gemma annotations from local disk" );
        return result;
    }

    private Map<String, Set<String>> getHumanMappingsFromServer() {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );
        Collection<ExpressionExperiment> experiments = ees.loadAll();

        int c = 0;
        for ( ExpressionExperiment experiment : experiments ) {
            c++;
            // if (c == 30) break;
            log.info( "Experiment number:" + c + " of " + experiments.size() + " ID:" + experiment.getId() );

            experiment = ees.thawLite( experiment );

            // if its mouse then keep it
            // Taxon taxon = ees.getTaxon( experiment.getId() );
            // if ( !TaxonUtility.isMouse( taxon ) ) {
            // continue;
            // }

            Collection<Characteristic> characters = experiment.getCharacteristics();

            Set<String> currentURL = new HashSet<String>();
            result.put( experiment.getId() + "", currentURL );

            for ( Characteristic ch : characters ) {
                if ( ch instanceof VocabCharacteristic ) {
                    VocabCharacteristic vc = ( VocabCharacteristic ) ch;
                    currentURL.add( vc.getValueUri() );

                    // System.out.println( vc.getCategory() );
                    // System.out.println( vc.getCategoryUri() );
                    // if ( specificLabels ) {
                    // labels.put( vc.getValueUri(), vc.getValue() + "[Gemma]" );
                    // } else {
                    // labels.put( vc.getValueUri(), vc.getValue() );
                    // }
                }
            }

        }
        return result;
    }

    private void getMappings() throws IOException {
        // get the human mappings
        loadHumanMappings();

        // get the labels
        // LabelLoader labelLoader = new LabelLoader();
        try {
            labels = LabelLoader.readLabels();
        } catch ( Exception e ) {
            log.warn( "Couldnt load labels" );
            e.printStackTrace();
            System.exit( 1 );
        }

        mmtxURLs = ProjectRDFModelTools.getURLsExperiments( filename );

        originalMMTxIDs = new HashSet<String>( mmtxURLs.keySet() );

        log.info( "mmtx size=" + mmtxURLs.size() );
        log.info( "gemma size=" + manualURLs.size() );
    }

    private Map<String, Set<String>> getMissedURLS() {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        for ( String dataset : originalMMTxIDs ) {
            result.put( dataset, getMissed( dataset ) );
        }
        return result;
    }

    private void loadHumanMappings() {
        try {
            manualURLs = getHumanMappingsFromDisk();
        } catch ( Exception e ) {
            log.info( "gettings annotations from local cache failed, getting from server" );
            manualURLs = getHumanMappingsFromServer();
            saveHumanMappingsToDisk();
        }
    }

    private void loadMappings() {
        // Reset
        try {
            getMappings();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        setNullstoEmpty();
        cleanURLs();
    }

    @SuppressWarnings("unused")
    private void printComparisonsCSV() {
        System.out.println( "ID, machineURLs.size()" + "," + "humanURLs.size()" + "," + "intersect.size()" );
        for ( String dataset : originalMMTxIDs ) {
            Set<String> machineURLs = mmtxURLs.get( dataset );
            Set<String> humanURLs = manualURLs.get( dataset );
            Set<String> intersect = getIntersection( dataset );
            System.out.println( dataset + "," + machineURLs.size() + "," + humanURLs.size() + "," + intersect.size() );
        }
    }

    @SuppressWarnings("unused")
    private void printMissedURLs() {
        Map<String, Integer> missed = new HashMap<String, Integer>();
        for ( Set<String> missedURLs : getMissedURLS().values() ) {
            for ( String URI : missedURLs ) {
                Integer i = missed.get( URI );
                if ( i == null )
                    i = 1;
                else
                    i++;
                missed.put( URI, i );
            }
        }

        for ( String URI : missed.keySet() ) {
            System.out.println( labels.get( URI ) + "|" + URI + "|" + missed.get( URI ) );
        }
    }

    @SuppressWarnings("unused")
    private void printROCCurveValues( String f ) throws IOException {
        // table that has scores mapped to how many predictions were made at the score, and how many were correct
        Map<Integer, Integer> intersections = new HashMap<Integer, Integer>();
        Map<Integer, Integer> predictions = new HashMap<Integer, Integer>();
        // from RDF
        String queryString = "PREFIX rdf: <http://www.w3.org/2000/01/rdf-schema#>"
                + "PREFIX gemmaAnn: <http://bioinformatics.ubc.ca/Gemma/ws/xml/gemmaAnnotations.owl#>\n                              "
                + "\n                                                            "
                + "SELECT DISTINCT ?dataset ?mapping ?score\n                                                            "
                + "WHERE {\n                                                            "
                + "    ?dataset gemmaAnn:describedBy ?description .\n                                                            "
                + "    ?description gemmaAnn:hasPhrase ?phrase .\n                                                            "
                + "    ?phrase gemmaAnn:hasMention ?mention .\n                                                            "
                + "    ?mention gemmaAnn:mappedTerm ?mapping .\n                                                            "
                + "    ?mention gemmaAnn:hasScore ?score .\n                                                            "
                + "    ?mention rdf:label ?label .\n" + "}";

        Model model = ProjectRDFModelTools.loadModel( f );

        Query q = QueryFactory.create( queryString );
        // go through them all and put in excel file
        QueryExecution qexec = QueryExecutionFactory.create( q, model );

        int row = 1;
        ResultSet results = qexec.execSelect();
        log.info( "Query executed" );

        // put it into a set
        Map<String, Integer> highest = new HashMap<String, Integer>();

        while ( results.hasNext() ) {
            QuerySolution qTemp = results.nextSolution();

            // for ( QuerySolution qTemp : tempSolns ) {
            int score = Integer.parseInt( OntologyTools.varToString( "score", qTemp ) );
            String dataset = OntologyTools.varToString( "dataset", qTemp );
            dataset = dataset.substring( dataset.lastIndexOf( '/' ) + 1 );
            String URL = OntologyTools.varToString( "mapping", qTemp );
            String key = URL + "|" + dataset; // ugly
            if ( highest.get( key ) == null || highest.get( key ) < score ) {
                // put the higher score
                highest.put( key, score );
            }
        }
        log.info( "highest size:" + highest.size() );
        qexec.close();

        QueryExecution qexec2 = QueryExecutionFactory.create( q, model );
        ResultSet results2 = qexec2.execSelect();
        // results.
        while ( results2.hasNext() ) {
            QuerySolution soln = results2.nextSolution();
            // for ( QuerySolution soln : solns ) {
            row++;
            String dataset = OntologyTools.varToString( "dataset", soln );
            dataset = dataset.substring( dataset.lastIndexOf( '/' ) + 1 );
            int score = Integer.parseInt( OntologyTools.varToString( "score", soln ) );
            String URL = OntologyTools.varToString( "mapping", soln );

            String key = URL + "|" + dataset; // ugly
            if ( highest.get( key ) == score ) {
                // dont do this one again
                highest.put( key, score + 1 );

                Integer predictionsForScore = predictions.get( score );
                if ( predictionsForScore == null ) {
                    predictions.put( score, 1 );
                } else {
                    predictions.put( score, 1 + predictionsForScore );
                }

                if ( intersections.get( score ) == null ) intersections.put( score, 0 );

                // if it is correct
                if ( manualURLs.get( dataset ).contains( URL ) ) {
                    intersections.put( score, intersections.get( score ) + 1 );
                }
            }
        }
        System.out.println( "score, predictions, correct" );
        for ( int score : predictions.keySet() ) {
            System.out.println( score + "," + predictions.get( score ) + "," + intersections.get( score ) );
        }
    }

    @SuppressWarnings("unused")
    private void printSourceStats() {
        List<String> allSources = new LinkedList<String>();
        List<String> intersectSources = new LinkedList<String>();

        for ( String dataset : originalMMTxIDs ) {
            Set<String> machineURLs = mmtxURLs.get( dataset );
            Set<String> intersect = getIntersection( dataset );

            for ( String URI : machineURLs ) {
                if ( de.getDecriptionType( dataset, URI ).size() != 1 ) continue;
                allSources.addAll( de.getDecriptionType( dataset, URI ) );
            }
            // only look at those with one source
            for ( String URI : intersect ) {
                if ( de.getDecriptionType( dataset, URI ).size() != 1 ) continue;
                intersectSources.addAll( de.getDecriptionType( dataset, URI ) );
            }
        }
        // crunch them down to a hash
        System.out.println( "== all MMTx predictions ==" );
        printMap( listToFrequencyMap( allSources ) );

        System.out.println( "== all MMTx predictions that matched existing ==" );
        printMap( listToFrequencyMap( intersectSources ) );
    }

    private void printStats() {
        int totalHuman = 0, totalMachine = 0, totalIntersect = 0;
        Set<String> uniqueHuman = new HashSet<String>();
        Set<String> uniqueMachine = new HashSet<String>();
        Set<String> uniqueIntersect = new HashSet<String>();
        for ( String dataset : originalMMTxIDs ) {
            Set<String> machineURLs = mmtxURLs.get( dataset );
            Set<String> humanURLs = manualURLs.get( dataset );
            Set<String> intersect = getIntersection( dataset );
            uniqueHuman.addAll( humanURLs );
            uniqueMachine.addAll( machineURLs );
            uniqueIntersect.addAll( intersect );

            totalMachine += machineURLs.size();
            totalHuman += humanURLs.size();
            totalIntersect += intersect.size();
        }
        System.out.println( "Human total:" + totalHuman + " Unique:" + uniqueHuman.size() + "  percent unique:"
                + ( int ) ( 100.0 * uniqueHuman.size() / totalHuman ) );
        System.out.println( "Machine:" + totalMachine + " Unique:" + uniqueMachine.size() + "  percent unique:"
                + ( int ) ( 100.0 * uniqueMachine.size() / totalMachine ) );
        System.out.println( "Intersect:" + totalIntersect + " Unique:" + uniqueIntersect.size() );
        float recall = totalIntersect / ( float ) totalHuman;
        System.out.println( "Recall:" + recall );
        float precision = totalIntersect / ( float ) totalMachine;
        System.out.println( "Precision:" + precision );
        System.out.println( "F-measure:" + 2 * precision * recall / ( precision + recall ) );
    }

    @SuppressWarnings("unused")
    private void removeAbstractSource() {
        for ( String dataset : originalMMTxIDs ) {
            Set<String> machineURLs = mmtxURLs.get( dataset );
            Set<String> oneSourceMachineURLs = new HashSet<String>();
            for ( String URI : machineURLs ) {
                if ( de.getDecriptionType( dataset, URI ).contains( "primaryReference/abstract" ) ) continue;
                oneSourceMachineURLs.add( URI );
            }
            mmtxURLs.put( dataset, oneSourceMachineURLs );
        }
    }

    // primaryReference/abstract
    // bioAssay/name
    // bioAssay/description
    // experiment/name
    // experiment/description
    // primaryReference/title
    // primaryReference/abstract
    private void removeExceptOneSource( String source ) {
        for ( String dataset : originalMMTxIDs ) {
            Set<String> machineURLs = mmtxURLs.get( dataset );
            Set<String> oneSourceMachineURLs = new HashSet<String>();
            for ( String URI : machineURLs ) {
                if ( de.getDecriptionType( dataset, URI ).contains( source )
                        && de.getDecriptionType( dataset, URI ).size() == 1 ) continue;
                oneSourceMachineURLs.add( URI );
            }
            mmtxURLs.put( dataset, oneSourceMachineURLs );
        }
    }

    private void saveHumanMappingsToDisk() {
        log.info( "Saved mappings" );
        try (ObjectOutputStream o = new ObjectOutputStream( new FileOutputStream(
                SetupParameters.getString( "gemma.annotator.cachedGemmaAnnotations" ) ) )) {
            o.writeObject( manualURLs );
            o.close();

            // depreciated
            // ObjectOutputStream o2 = new ObjectOutputStream( new FileOutputStream( "label.mappings" ) );
            // o2.writeObject( labels );
            // o2.close();

            log.info( "Saved manual annotations" );
        } catch ( Exception e ) {
            throw new RuntimeException( "cannot save CUI mappings" );
        }
    }

    @SuppressWarnings("unused")
    private void writeExperimentTitles( String f ) {
        ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );
        Collection<ExpressionExperiment> experiments = ees.loadAll();
        Model model = ModelFactory.createDefaultModel();

        int c = 0;
        for ( ExpressionExperiment experiment : experiments ) {
            c++;
            // if (c == 30) break;
            Long ID = experiment.getId();

            log.info( "Experiment number:" + c + " of " + experiments.size() + " ID:" + experiment.getId() );

            experiment = ees.thawLite( experiment );

            String GEOObjectURI = ExpressionExperimentAnnotatorImpl.gemmaNamespace + "experiment/" + ID;
            Resource expNode = model.createResource( GEOObjectURI );
            expNode.addProperty( DC_11.title, experiment.getName() );
        }
        try {
            model.write( new FileWriter( f ) );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        // TODO Auto-generated method stub
        return null;
    }

}
