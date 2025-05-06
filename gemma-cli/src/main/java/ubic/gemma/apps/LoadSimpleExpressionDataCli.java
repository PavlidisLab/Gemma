/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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

package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.EnumConverter;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.simple.model.*;
import ubic.gemma.core.ontology.ValueStringToOntologyMapping;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Command Line tools for loading the expression experiment in flat files
 *
 * @author xiangwan
 */
public class LoadSimpleExpressionDataCli extends AbstractAuthenticatedCLI {

    private static final String[] LEGACY_HEADER = new String[] {
            "name", "short_name", "description", "array_design_short_name", "data_file", "species", "qt_name",
            "qt_description", "qt_type", "qt_scale", "pubmed_id", "source", "array_design_name", "technology_type"
    };

    // For historical reason, this is the header assumed in the TSV file if unspecified. Please do not alter!
    private static final CSVFormat LEGACY_TSV_FORMAT = CSVFormat.TDF.builder()
            .setHeader( LEGACY_HEADER )
            .setCommentMarker( '#' )
            .setIgnoreEmptyLines( true )
            .get();

    private static final String[] HEADER = ArrayUtils.addAll( LEGACY_HEADER, "samples_metadata_file" );

    private static final CSVFormat TSV_FORMAT = CSVFormat.TDF.builder()
            .setHeader()
            .setSkipHeaderRecord( true )
            .setCommentMarker( '#' )
            .setIgnoreEmptyLines( true )
            .get();

    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private SimpleExpressionDataLoaderService eeLoaderService;

    private Path metadataFile;
    @Nullable
    private Path dataDir;
    private boolean legacy;
    private boolean createImageClones;

    @Override
    public String getCommandName() {
        return "addTSVData";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    public String getShortDesc() {
        return "Load an experiment from a tab-delimited file instead of GEO";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "f" )
                .longOpt( "file" )
                .required().hasArg()
                .argName( "FILE" )
                .type( Path.class )
                .desc( "List of experiments to load with their basic metadata. It is a tabular file containing the following columns: " + String.join( ", ", HEADER ) + "." )
                .build() );
        options.addOption( Option.builder( "d" )
                .longOpt( "dir" )
                .hasArg().type( Path.class )
                .argName( "DIRECTORY" )
                .type( Path.class )
                .desc( "If supplied, resolve data files mentioned in the metadata file via -f/--file relative to that directory." )
                .build() );
        options.addOption( Option.builder( "legacy" )
                .longOpt( "legacy" )
                .desc( "Load pre-1.32 metadata files lacking a header. The following columns are assumed: " + String.join( ", ", LEGACY_HEADER ) + "." )
                .build() );
        options.addOption( Option.builder( "createImageClones" )
                .longOpt( "create-image-clones" )
                .desc( "When populating a platform from the data file, also create biological characteristics for the design elements." )
                .build() );
        addBatchOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        metadataFile = commandLine.getParsedOptionValue( 'f' );
        dataDir = commandLine.getParsedOptionValue( 'd' );
        legacy = commandLine.hasOption( "legacy" );
        createImageClones = commandLine.hasOption( "createImageClones" );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        log.info( "Loading experiments from " + this.metadataFile );
        try ( CSVParser parser = CSVParser.parse( metadataFile, StandardCharsets.UTF_8, legacy ? LEGACY_TSV_FORMAT : TSV_FORMAT ) ) {
            for ( CSVRecord record : parser ) {
                String expName = getRequiredRecordField( record, "name" );
                try {
                    loadExperiment( record );
                    addSuccessObject( expName );
                } catch ( Exception e ) {
                    addErrorObject( expName, e );
                }
            }
        }
    }

    private void loadExperiment( CSVRecord record ) throws IOException {
        String shortName = getRequiredRecordField( record, "short_name" );

        ExpressionExperiment existing = eeService.findByShortName( shortName );
        if ( existing != null ) {
            throw new IllegalArgumentException( "There is already an experiment with short name " + shortName
                    + "; please choose something unique." );
        }

        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();
        metaData.setShortName( shortName );
        metaData.setName( getRequiredRecordField( record, "name" ) );
        metaData.setDescription( getRecordField( record, "description" ) );

        String accession;
        if ( ( accession = getRecordField( record, "accession" ) ) != null ) {
            String[] pieces = accession.split( ":", 2 );
            if ( pieces.length != 2 ) {
                throw new IllegalArgumentException( "Invalid value for the 'accession' column, must be of the form <database>:<accession>." );
            }
            metaData.setAccession( SimpleDatabaseEntry.fromAccession( pieces[1], pieces[0] ) );
        }

        this.configureTaxon( record, metaData );

        Map<String, SimplePlatformMetadata> id2ad = new HashMap<>();
        this.configureArrayDesigns( record, metaData, id2ad );

        metaData.setSource( getRecordField( record, "source" ) );
        String pubMedId = getRecordField( record, "pubmed_id" );
        if ( pubMedId != null ) {
            metaData.setPubMedId( pubMedId );
        }

        String samplesMetadataFile;
        if ( ( samplesMetadataFile = getRecordField( record, "samples_metadata_file" ) ) != null ) {
            configureSamplesMetadata( samplesMetadataFile, metaData, id2ad );
        }

        String dataFile;
        if ( ( dataFile = getRecordField( record, "data_file" ) ) != null ) {
            this.configureQuantitationType( record, metaData );
            try ( InputStream data = openDataFile( shortName, dataFile ) ) {
                eeLoaderService.create( metaData, new DoubleMatrixReader().read( data ) );
            }
        } else {
            // only create with metadata
            eeLoaderService.create( metaData, null );
        }
    }

    private void configureTaxon( CSVRecord record, SimpleExpressionExperimentMetadata metaData ) {
        String species = getRequiredRecordField( record, "species" );
        try {
            metaData.setTaxon( SimpleTaxonMetadata.forNcbiId( Integer.parseInt( species ) ) );
        } catch ( NumberFormatException e ) {
            metaData.setTaxon( SimpleTaxonMetadata.forName( species ) );
        }
    }

    private void configureArrayDesigns( CSVRecord record, SimpleExpressionExperimentMetadata metaData, Map<String, SimplePlatformMetadata> id2ad ) {
        String shortName = getRecordField( record, "array_design_short_name" );
        String name = getRecordField( record, "array_design_name" );
        if ( shortName == null ) {
            SimplePlatformMetadata ad = new SimplePlatformMetadata();
            // that's okay, so long as we get an array design name
            ad.setName( requireNonNull( name, "A name must be provided if the 'array_design_short_name' is left blank or omitted." ) );
            ad.setShortName( name );
            TechnologyType techType = EnumConverter.of( TechnologyType.class )
                    .apply( getRequiredRecordField( record, "technology_type" ) );
            ad.setTechnologyType( techType );
            metaData.getArrayDesigns().add( ad );
            id2ad.put( name, ad );
        } else if ( ( legacy && shortName.equalsIgnoreCase( "IMAGE" ) ) ) {
            log.info( "Treating the 'IMAGE' array design short name as a special case since legacy mode is enabled." );
            SimplePlatformMetadata ad = new SimplePlatformMetadata();
            ad.setName( requireNonNull( name, "A name must be provided if 'IMAGE' is used as platform short name." ) );
            ad.setShortName( name ); // don't use IMAGE as short name
            TechnologyType techType = EnumConverter.of( TechnologyType.class )
                    .apply( getRequiredRecordField( record, "technology_type" ) );
            ad.setTechnologyType( techType );
            metaData.getArrayDesigns().add( ad );
            metaData.setProbeIdsAreImageClones( true );
            id2ad.put( name, ad );
        } else if ( name != null ) {
            // allow for the case where there is an additional new array design to be added.
            SimplePlatformMetadata ad = new SimplePlatformMetadata();
            ad.setName( name );
            ad.setShortName( shortName );
            TechnologyType techType = EnumConverter.of( TechnologyType.class )
                    .apply( getRequiredRecordField( record, "technology_type" ) );
            ad.setTechnologyType( techType );
            metaData.getArrayDesigns().add( ad );
            metaData.setProbeIdsAreImageClones( createImageClones );
            id2ad.put( shortName, ad );
            id2ad.put( name, ad );
        } else {
            // existing ADs
            String[] arrayDesignShortNames = StringUtils.split( getRequiredRecordField( record, "array_design_short_name" ), legacy ? '+' : TsvUtils.SUB_DELIMITER );
            for ( String arrayDesignShortName : arrayDesignShortNames ) {
                SimplePlatformMetadata ad = new SimplePlatformMetadata();
                ad.setShortName( arrayDesignShortName );
                metaData.getArrayDesigns().add( ad );
                id2ad.put( arrayDesignShortName, ad );
            }
        }
    }

    private void configureSamplesMetadata( String sampleFile, SimpleExpressionExperimentMetadata metaData, Map<String, SimplePlatformMetadata> id2ad ) throws IOException {
        try ( CSVParser parser = CSVParser.parse( openSampleMetadataFile( metaData.getName(), sampleFile ), StandardCharsets.UTF_8, TSV_FORMAT ) ) {
            for ( CSVRecord sampleRecord : parser ) {
                String name = getRequiredRecordField( sampleRecord, "name" );
                String description = getRecordField( sampleRecord, "description" );
                SimpleSampleMetadata bm = new SimpleSampleMetadata();
                bm.setName( name );
                bm.setDescription( description );
                String accession;
                if ( ( accession = getRecordField( sampleRecord, "accession" ) ) != null ) {
                    String[] pieces = accession.split( ":", 2 );
                    if ( pieces.length != 2 ) {
                        throw new IllegalArgumentException( "Invalid value for the 'accession' column for sample with name '" + name + "', must be of the form <database>:<accession>." );
                    }
                    bm.setAccession( SimpleDatabaseEntry.fromAccession( pieces[1], pieces[0] ) );
                }
                configureSampleCharacteristics( sampleRecord, bm );
                SimplePlatformMetadata ad = id2ad.get( getRecordField( sampleRecord, "array_design_used" ) );
                if ( ad == null ) {
                    throw new IllegalArgumentException( "Invalid value for the 'array_design_used' column, must be one of: " + id2ad.keySet().stream().sorted().collect( Collectors.joining( ", " ) ) + "." );
                }
                bm.setPlatformUsed( ad );
                metaData.getSamples().add( bm );
            }
        }
    }

    private void configureSampleCharacteristics( CSVRecord sampleRecord, SimpleSampleMetadata bm ) throws IOException {
        String c = getRecordField( sampleRecord, "characteristics" );
        if ( c != null ) {
            for ( String piece : StringUtils.split( c, TsvUtils.SUB_DELIMITER ) ) {
                bm.getCharacteristics().add( parseCharacteristic( piece ) );
            }
        }
        String cf = getRecordField( sampleRecord, "characteristics_file" );
        if ( cf != null ) {
            try ( CSVParser parser = CSVParser.parse( openSampleCharacteristicsFile( bm.getName(), cf ), StandardCharsets.UTF_8, TSV_FORMAT ) ) {
                for ( CSVRecord record : parser ) {
                    bm.getCharacteristics().add( parseCharacteristic( record ) );
                }
            }
        }
    }

    private SimpleCharacteristic parseCharacteristic( String s ) {
        String[] pieces = s.split( ":", 2 );
        String category, value;
        if ( pieces.length == 2 ) {
            category = StringUtils.strip( pieces[0] );
            value = StringUtils.strip( pieces[1] );
        } else {
            category = null;
            value = s;
        }
        Collection<Characteristic> results;
        if ( category != null ) {
            Characteristic c = ValueStringToOntologyMapping.lookup( value, category );
            if ( c != null ) {
                return new SimpleCharacteristic( c.getCategory(), c.getCategoryUri(), c.getValue(), c.getValueUri() );
            } else {
                return new SimpleCharacteristic( category, null, value, null );
            }
        } else {
            results = ValueStringToOntologyMapping.lookup( value );
            if ( results.isEmpty() ) {
                return new SimpleCharacteristic( null, null, s, null );
            } else if ( results.size() == 1 ) {
                Characteristic c = results.iterator().next();
                return new SimpleCharacteristic( c.getCategory(), c.getCategoryUri(), c.getValue(), c.getValueUri() );
            } else {
                log.warn( "More than one characteristic found for " + s + ": " + results + ", will treat it as uncategorized." );
                return new SimpleCharacteristic( null, null, s, null );
            }
        }
    }

    private SimpleCharacteristic parseCharacteristic( CSVRecord record ) {
        return new SimpleCharacteristic(
                getRecordField( record, "category" ), getRecordField( record, "category_uri" ),
                getRequiredRecordField( record, "value" ), getRecordField( record, "value_uri" ) );
    }

    private void configureQuantitationType( CSVRecord record, SimpleExpressionExperimentMetadata metaData ) {
        SimpleQuantitationTypeMetadata qtMetadata = new SimpleQuantitationTypeMetadata();
        qtMetadata.setName( getRequiredRecordField( record, "qt_name" ) );
        qtMetadata.setDescription( getRecordField( record, "qt_description" ) );
        qtMetadata.setGeneralType( GeneralType.QUANTITATIVE );

        StandardQuantitationType sQType = EnumConverter.of( StandardQuantitationType.class ).apply( getRequiredRecordField( record, "qt_type" ) );
        qtMetadata.setType( sQType );

        ScaleType sType = EnumConverter.of( ScaleType.class ).apply( getRequiredRecordField( record, "qt_scale" ) );
        qtMetadata.setScale( sType );
        qtMetadata.setRepresentation( PrimitiveType.DOUBLE );

        metaData.setQuantitationType( qtMetadata );
    }

    private InputStream openDataFile( String expName, String dataFile ) throws IOException {
        Path p = resolveDataFile( dataFile );
        log.info( "Reading data for " + expName + " from " + p + "..." );
        return openFile( p );
    }

    private InputStream openSampleMetadataFile( String expName, String sampleMetadataFile ) throws IOException {
        Path p = resolveDataFile( sampleMetadataFile );
        log.info( "Reading sample metadata data for " + expName + " from " + p + "..." );
        return openFile( p );
    }

    private InputStream openSampleCharacteristicsFile( String sampleName, String characteristicFile ) throws IOException {
        Path p = resolveDataFile( characteristicFile );
        log.info( "Reading sample characteristics data for " + sampleName + " from " + p + "..." );
        return openFile( p );
    }

    private Path resolveDataFile( String dataFile ) {
        if ( dataDir != null ) {
            return dataDir.resolve( dataFile );
        } else {
            return Paths.get( dataFile );
        }
    }

    private InputStream openFile( Path p ) throws IOException {
        if ( p.getFileName().toString().endsWith( ".gz" ) ) {
            return FileUtils.openCompressedFile( p );
        } else {
            return Files.newInputStream( p );
        }
    }

    @Nullable
    private String getRecordField( CSVRecord record, String field ) {
        return record.isMapped( field ) ? StringUtils.stripToNull( record.get( field ) ) : null;
    }

    private String getRequiredRecordField( CSVRecord record, String field ) {
        return requireNonNull( getRecordField( record, field ), () -> String.format( "Required value for field %s not found.", field ) );
    }
}
