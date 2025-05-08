package ubic.gemma.core.loader.expression.sra;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import ubic.gemma.core.loader.expression.sra.model.*;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Parses SRA runinfo format.
 * @author poirigui
 */
public class SraRuninfoParser {

    // Run,ReleaseDate,LoadDate,spots,bases,spots_with_mates,avgLength,size_MB,AssemblyName,download_path,Experiment,LibraryName,LibraryStrategy,LibrarySelection,LibrarySource,LibraryLayout,InsertSize,InsertDev,Platform,Model,SRAStudy,BioProject,Study_Pubmed_id,ProjectID,Sample,BioSample,SampleType,TaxID,ScientificName,SampleName,g1k_pop_code,source,g1k_analysis_group,Subject_ID,Sex,Disease,Tumor,Affection_Status,Analyte_Type,Histological_Type,Body_Site,CenterName,Submission,dbgap_study_accession,Consent,RunHash,ReadHash
    private static final CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord( true ).get();
    private static final DateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

    public SraExperimentPackageSet parse( Reader reader ) throws IOException {
        try ( CSVParser parser = csvFormat.parse( reader ) ) {
            Map<String, SraExperiment> id2e = new HashMap<>();
            Map<String, SraSubmission> id2s = new HashMap<>();
            Map<String, SraOrganization> id2o = new HashMap<>();
            Map<String, List<SraRun>> id2r = new HashMap<>();
            for ( CSVRecord record : parser ) {
                String experiment = record.get( "Experiment" );
                id2e.computeIfAbsent( experiment, k -> parseExperiment( record ) );
                id2s.computeIfAbsent( experiment, k -> parseSubmission( record ) );
                id2o.computeIfAbsent( experiment, k -> parseOrganization( record ) );
                id2r.computeIfAbsent( experiment, k -> new ArrayList<>() )
                        .add( parseRun( record ) );
            }
            List<SraExperimentPackage> packages = new ArrayList<>();
            for ( String experiment : id2e.keySet() ) {
                SraExperimentPackage p = new SraExperimentPackage();
                p.setExperiment( id2e.get( experiment ) );
                p.setSubmission( id2s.get( experiment ) );
                p.setOrganization( id2o.get( experiment ) );
                List<SraRun> runs = id2r.get( experiment );
                SraRunSet runSet = new SraRunSet();
                runSet.setBases( runs.stream().mapToLong( SraRun::getTotalBases ).sum() );
                runSet.setSpots( runs.stream().mapToLong( SraRun::getTotalSpots ).sum() );
                runSet.setBytes( runs.stream().mapToLong( SraRun::getSize ).sum() );
                runSet.setRuns( runs );
                p.setRunSets( Collections.singletonList( runSet ) );
                packages.add( p );
            }
            SraExperimentPackageSet result = new SraExperimentPackageSet();
            result.setExperimentPackages( packages );
            return result;
        }
    }

    private SraSubmission parseSubmission( CSVRecord record ) {
        SraSubmission result = new SraSubmission();
        result.setAccession( record.get( "Submission" ) );
        result.setCenterName( record.get( "CenterName" ) );
        return result;
    }

    private SraOrganization parseOrganization( CSVRecord record ) {
        return new SraOrganization();
    }

    private SraExperiment parseExperiment( CSVRecord record ) {
        SraExperiment e = new SraExperiment();
        SraIdentifiers identifiers = new SraIdentifiers();
        SraPrimaryId primaryId = new SraPrimaryId();
        primaryId.setId( record.get( "Experiment" ) );
        identifiers.setPrimaryId( primaryId );
        e.setIdentifiers( identifiers );
        e.setAccession( record.get( "Experiment" ) );
        SraExperimentDesign design = new SraExperimentDesign();
        SraLibraryDescriptor ld = new SraLibraryDescriptor();
        ld.setName( record.get( "LibraryName" ) );
        ld.setStrategy( record.get( "LibraryStrategy" ) );
        ld.setSelection( record.get( "LibrarySelection" ) );
        ld.setSource( record.get( "LibrarySource" ) );
        ld.setLayout( parseLibraryLayout( record ) );
        design.setLibraryDescriptor( ld );
        e.setDesign( design );
        e.setPlatform( parsePlatform( record ) );
        return e;
    }
    private SraPlatform parsePlatform( CSVRecord record ) {
        SraPlatform platform = new SraPlatform();
        platform.setInstrumentPlatform( record.get( "Platform" ) );
        platform.setInstrumentModel( record.get( "Model" ) );
        return platform;
    }

    private SraLibraryLayout parseLibraryLayout( CSVRecord record ) {
        SraLibraryLayout ll = new SraLibraryLayout();
        ll.setPaired( record.get( "LibraryLayout" ).equals( "PAIRED" ) );
        ll.setSingle( record.get( "LibraryLayout" ).equals( "SINGLE" ) );
        return ll;
    }

    private SraRun parseRun( CSVRecord record ) {
        SraRun run = new SraRun();
        run.setAccession( record.get( "Run" ) );
        run.setPublished( parseDate( record.get( "ReleaseDate" ) ) );
        SraPool pool = parsePool( record );
        run.setPool( pool );
        run.setTotalSpots( pool.getMembers().stream().mapToLong( SraPoolMember::getSpots ).sum() );
        run.setTotalBases( pool.getMembers().stream().mapToLong( SraPoolMember::getBases ).sum() );
        run.setSize( 1024L * 1024L * Integer.parseInt( record.get( "size_MB" ) ) );
        return run;
    }

    private SraPool parsePool( CSVRecord record ) {
        SraPool pool = new SraPool();
        SraPoolMember member = new SraPoolMember();
        member.setBases( Long.parseLong( record.get( "bases" ) ) );
        member.setSpots( Long.parseLong( record.get( "spots" ) ) );
        member.setTaxonId( Integer.parseInt( record.get( "TaxID" ) ) );
        member.setOrganism( record.get( "ScientificName" ) );
        member.setSampleName( record.get( "SampleName" ) );
        pool.setMembers( Collections.singletonList( member ) );
        return pool;
    }

    private Date parseDate( String s ) {
        try {
            synchronized ( format ) {
                return format.parse( s );
            }
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }
    }
}
