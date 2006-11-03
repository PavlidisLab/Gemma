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
package ubic.gemma.loader.expression.geo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.util.CancellationException;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.loader.expression.geo.model.GeoChannel;
import ubic.gemma.loader.expression.geo.model.GeoContact;
import ubic.gemma.loader.expression.geo.model.GeoData;
import ubic.gemma.loader.expression.geo.model.GeoDataset;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoReplication;
import ubic.gemma.loader.expression.geo.model.GeoSample;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.loader.expression.geo.model.GeoSubset;
import ubic.gemma.loader.expression.geo.model.GeoVariable;
import ubic.gemma.loader.expression.geo.model.GeoDataset.ExperimentType;
import ubic.gemma.loader.expression.geo.model.GeoDataset.PlatformType;
import ubic.gemma.loader.expression.geo.model.GeoReplication.ReplicationType;
import ubic.gemma.loader.expression.geo.model.GeoVariable.VariableType;
import ubic.gemma.loader.expression.geo.util.GeoConstants;
import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.loader.util.parser.ExternalDatabaseUtils;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;

/**
 * Convert GEO domain objects into Gemma objects. Usually we trigger this by passing in GeoDataset objects.
 * <p>
 * GEO has four basic kinds of objects: Platforms (ArrayDesigns), Samples (BioAssays), Series (Experiments) and DataSets
 * (which are curated Experiments). Note that a sample can belong to more than one series. A series can include more
 * than one dataset. See http://www.ncbi.nlm.nih.gov/projects/geo/info/soft2.html.
 * <p>
 * For our purposes, a usable expression data set is at first represented by a GEO "GDS" number (a curated dataset),
 * which corresponds to a series. HOWEVER, multiple datasets may go together to form a series (GSE). This can happen
 * when the "A" and "B" arrays were both run on the same samples. Thus we actually normally go by GSE.
 * <p>
 * This service can be used in database-aware or unaware states.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="geoConverter"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name="taxonService" ref="taxonService"
 */
public class GeoConverter implements Converter {

    /**
     * How often we tell the user about data processing (items per update)
     */
    private static final int LOGGING_VECTOR_COUNT_UPDATE = 2000;

    private static Log log = LogFactory.getLog( ArrayDesignSequenceProcessingService.class.getName() );

    /**
     * Initial guess at how many designelementdatavectors to allocate space for.
     */
    private static final int INITIAL_VECTOR_CAPACITY = 10000;

    private ExternalDatabaseService externalDatabaseService;

    private TaxonService taxonService;

    private ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    private ExternalDatabase geoDatabase;

    private Map<String, Map<String, CompositeSequence>> platformDesignElementMap = new HashMap<String, Map<String, CompositeSequence>>();

    private Map<String, Taxon> platformTaxonMap = new HashMap<String, Taxon>();

    private Collection<Object> results = new HashSet<Object>();

    private Map<String, ArrayDesign> seenPlatforms = new HashMap<String, ArrayDesign>();

    private ExternalDatabase genbank;

    private static final String RAT = "rattus";

    private static Map<String, String> organismDatabases = new HashMap<String, String>();

    static {
        organismDatabases.put( "Saccharomyces cerevisiae", "SGD" );
        organismDatabases.put( "Schizosaccharomyces pombe", "GeneDB" );
    }

    /**
     * Find the bioassay in the experiment that matches the one given.
     */
    @SuppressWarnings("unchecked")
    private boolean addMatchingBioAssayToSubSet( ExpressionExperimentSubSet subSet, String accession,
            ExpressionExperiment expExp ) {
        log.debug( "Seeking subset match for " + accession );
        Collection<BioAssay> experimentBioAssays = expExp.getBioAssays();
        for ( BioAssay assay : experimentBioAssays ) {
            String testAccession = assay.getAccession().getAccession();
            if ( testAccession.equals( accession ) ) {
                subSet.getBioAssays().add( assay );
                return true;
            }
        }
        return false;
    }

    /**
     * Remove old results. Call this prior to starting converstion of a full dataset.
     */
    public void clear() {
        results = new HashSet<Object>();
        seenPlatforms = new HashMap<String, ArrayDesign>();
        platformDesignElementMap = new HashMap<String, Map<String, CompositeSequence>>();
    }

    /**
     * @param seriesMap
     */
    @SuppressWarnings("unchecked")
    public Collection<Object> convert( Collection geoObjects ) {
        for ( Object geoObject : geoObjects ) {
            Object convertedObject = convert( geoObject );
            if ( convertedObject != null ) {
                if ( convertedObject instanceof Collection ) {
                    results.addAll( ( Collection ) convertedObject );
                } else {
                    results.add( convertedObject );
                }
            }
        }

        log.info( "Converted object tally:\n" + this );

        // log.debug( "Detailed object tree:" );
        // log.debug( PrettyPrinter.print( results ) );

        return results;
    }

    /**
     * @param geoObject
     */
    public Object convert( Object geoObject ) {
        if ( geoObject == null ) {
            log.warn( "Null object" );
            return null;
        }
        if ( geoObject instanceof Collection ) {
            return convert( ( Collection ) geoObject );
        } else if ( geoObject instanceof GeoDataset ) {
            return convertDataset( ( GeoDataset ) geoObject );
        } else if ( geoObject instanceof GeoSeries ) { // typically we start here, with a series.
            return convertSeries( ( GeoSeries ) geoObject );
        } else if ( geoObject instanceof GeoSubset ) {
            throw new IllegalArgumentException( "Can't deal with " + geoObject.getClass().getName() + " ('" + geoObject
                    + "')" );
        } else if ( geoObject instanceof GeoSample ) {
            throw new IllegalArgumentException( "Can't deal with " + geoObject.getClass().getName() + " ('" + geoObject
                    + "')" );
        } else if ( geoObject instanceof GeoPlatform ) {
            return convertPlatform( ( GeoPlatform ) geoObject );
        } else {
            throw new IllegalArgumentException( "Can't deal with " + geoObject.getClass().getName() + " ('" + geoObject
                    + "')" );
        }

    }

    /**
     * GEO does not keep track of 'biomaterials' that make up different channels. Therefore the two channels effectively
     * make up a single biomaterial, as far as we're concerned. We're losing information here.
     * 
     * @param sample
     * @param channel
     * @return
     */
    @SuppressWarnings("unchecked")
    private BioMaterial convertChannel( GeoSample sample, GeoChannel channel, BioMaterial bioMaterial ) {
        if ( bioMaterial == null ) return null;
        log.debug( "Sample: " + sample.getGeoAccession() + " - Converting channel " + channel.getSourceName() );

        bioMaterial.setDescription( ( bioMaterial.getDescription() == null ? "" : bioMaterial.getDescription() + ";" )
                + "Channel " + channel.getChannelNumber() );

        if ( !StringUtils.isBlank( channel.getGrowthProtocol() ) ) {
            Treatment treatment = Treatment.Factory.newInstance();
            treatment.setName( sample.getGeoAccession() + " channel " + channel.getChannelNumber() + " treatment" );
            treatment.setDescription( channel.getTreatmentProtocol() );
            bioMaterial.getTreatments().add( treatment );
        }

        if ( !StringUtils.isBlank( channel.getTreatmentProtocol() ) ) {
            Treatment treatment = Treatment.Factory.newInstance();
            treatment.setName( sample.getGeoAccession() + " channel " + channel.getChannelNumber() + " growth" );
            treatment.setDescription( channel.getGrowthProtocol() );
            bioMaterial.getTreatments().add( treatment );
        }

        if ( !StringUtils.isBlank( channel.getExtractProtocol() ) ) {
            Treatment treatment = Treatment.Factory.newInstance();
            treatment.setName( sample.getGeoAccession() + " channel " + channel.getChannelNumber() + " extraction" );
            treatment.setDescription( channel.getExtractProtocol() );
            bioMaterial.getTreatments().add( treatment );
        }

        if ( !StringUtils.isBlank( channel.getLabelProtocol() ) ) {
            Treatment treatment = Treatment.Factory.newInstance();
            treatment.setName( sample.getGeoAccession() + " channel " + channel.getChannelNumber() + " label" );
            treatment.setDescription( channel.getLabelProtocol() );
            bioMaterial.getTreatments().add( treatment );
        }

        for ( String characteristic : channel.getCharacteristics() ) {
            Characteristic gemmaChar = Characteristic.Factory.newInstance();

            characteristic = trimString( characteristic );

            gemmaChar.setCategory( "GEO Sample characteristic" );
            gemmaChar.setValue( characteristic );
            bioMaterial.getCharacteristics().add( gemmaChar );
        }

        if ( channel.getSourceName() != null ) {
            Characteristic sourceChar = Characteristic.Factory.newInstance();
            sourceChar.setCategory( "GEO Sample source" );
            String characteristic = trimString( channel.getSourceName() );
            sourceChar.setValue( characteristic );
            bioMaterial.getCharacteristics().add( sourceChar );
        }

        if ( channel.getOrganism() != null ) {
            Taxon taxon = Taxon.Factory.newInstance();
            taxon.setScientificName( channel.getOrganism() );
            bioMaterial.setSourceTaxon( taxon );
        }

        if ( channel.getMolecule() != null ) {
            Characteristic moleculeChar = Characteristic.Factory.newInstance();
            moleculeChar.setCategory( "GEO Sample molecule" );
            String characteristic = trimString( channel.getMolecule().toString() );
            moleculeChar.setValue( characteristic );
            bioMaterial.getCharacteristics().add( moleculeChar );
        }

        if ( channel.getLabel() != null ) {
            Characteristic labelChar = Characteristic.Factory.newInstance();
            labelChar.setCategory( "GEO Sample label" );
            String characteristic = trimString( channel.getLabel() );
            labelChar.setValue( characteristic );
            bioMaterial.getCharacteristics().add( labelChar );
        }

        return bioMaterial;
    }

    private String trimString( String characteristic ) {
        if ( characteristic.length() > 255 ) {
            log.warn( "** Characteristic too long: " + characteristic + " - will truncate - ****" );
            characteristic = characteristic.substring( 0, 199 ) + " (truncated at 200 characters)";
        }
        return characteristic;
    }

    /**
     * @param contact
     * @return
     */
    private Person convertContact( GeoContact contact ) {
        Person result = Person.Factory.newInstance();
        result.setAddress( contact.getCity() + " " + contact.getState() + " " + contact.getCountry() + " "
                + contact.getPostCode() );
        result.setPhone( contact.getPhone() );
        result.setName( contact.getName() );
        result.setEmail( contact.getEmail() );
        result.setFax( contact.getFax() );
        result.setURL( contact.getWebLink() );

        return result;
    }

    /**
     * Take contact and contributer information from a GeoSeries and put it in the ExpressionExperiment.
     * 
     * @param series
     * @param expExp
     */
    @SuppressWarnings("unchecked")
    private void convertContacts( GeoSeries series, ExpressionExperiment expExp ) {
        expExp.getInvestigators().add( convertContact( series.getContact() ) );
        if ( series.getContributers().size() > 0 ) {
            expExp.setDescription( expExp.getDescription() + "\nContributers: " );
            for ( GeoContact contributer : series.getContributers() ) {
                expExp.setDescription( expExp.getDescription() + " " + contributer.getName() );
                expExp.getInvestigators().add( convertContact( contributer ) );
            }
            expExp.setDescription( expExp.getDescription() + "\n" );
        }
    }

    /**
     * Convert a vector of strings into a byte[] for saving in the database. . Blanks(missing values) are treated as NAN
     * (double), 0 (integer), false (booleans) or just empty strings (strings). Other invalid values are treated the
     * same way as missing data (to keep the parser from failing when dealing with strange GEO files that have values
     * like "Error" for an expression value).
     * 
     * @param vector of Strings to be converted to primitive values (double, int etc)
     * @param qt The quantitation type for the values to be converted.
     * @return
     */
    protected byte[] convertData( List<Object> vector, QuantitationType qt ) {

        if ( vector == null || vector.size() == 0 ) return null;

        boolean containsAtLeastOneNonNull = false;
        for ( Object string : vector ) {
            if ( string != null ) {
                containsAtLeastOneNonNull = true;
                break;
            }
        }

        if ( !containsAtLeastOneNonNull ) {
            if ( log.isDebugEnabled() ) {
                log.debug( "No data for " + qt + " in vector of length " + vector.size() );
            }
            return null;
        }

        List<Object> toConvert = new ArrayList<Object>();
        PrimitiveType pt = qt.getRepresentation();
        for ( Object rawValue : vector ) {
            if ( rawValue == null ) {
                handleMissing( toConvert, pt );
            } else if ( rawValue instanceof String ) { // needs to be coverted.
                try {
                    if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                        toConvert.add( Double.parseDouble( ( String ) rawValue ) );
                    } else if ( pt.equals( PrimitiveType.STRING ) ) {
                        toConvert.add( ( String ) rawValue );
                    } else if ( pt.equals( PrimitiveType.INT ) ) {
                        toConvert.add( Integer.parseInt( ( String ) rawValue ) );
                    } else if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                        toConvert.add( Boolean.parseBoolean( ( String ) rawValue ) );

                    } else {
                        throw new UnsupportedOperationException( "Data vectors of type " + pt + " not supported" );
                    }
                } catch ( NumberFormatException e ) {
                    handleMissing( toConvert, pt );
                } catch ( NullPointerException e ) {
                    handleMissing( toConvert, pt );
                }
            } else { // use as is.
                toConvert.add( rawValue );
            }
        }
        return byteArrayConverter.toBytes( toConvert.toArray() );
    }

    /**
     * Often-needed generation of a valid databaseentry object.
     * 
     * @param geoData
     * @return
     */
    private DatabaseEntry convertDatabaseEntry( GeoData geoData ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();

        initGeoExternalDatabase();

        result.setExternalDatabase( this.geoDatabase );
        result.setAccession( geoData.getGeoAccession() );

        return result;
    }

    /**
     * @param geoDataset
     */
    private ExpressionExperiment convertDataset( GeoDataset geoDataset ) {

        if ( geoDataset.getSeries().size() == 0 ) {
            throw new IllegalArgumentException( "GEO Dataset must have associated series" );
        }

        if ( geoDataset.getSeries().size() > 1 ) {
            throw new UnsupportedOperationException( "GEO Dataset can only be associated with one series" );
        }

        Collection<ExpressionExperiment> results = this.convertSeries( geoDataset.getSeries().iterator().next() );
        assert results.size() == 1; // unless we have multiple species, not possible.
        return results.iterator().next();
    }

    /**
     * @param dataset
     * @param expExp
     */
    private ExpressionExperiment convertDataset( GeoDataset geoDataset, ExpressionExperiment expExp ) {
        log.info( "Converting dataset:" + geoDataset );

        convertDatasetDescriptions( geoDataset, expExp );

        GeoPlatform platform = geoDataset.getPlatform();
        ArrayDesign ad = seenPlatforms.get( platform.getGeoAccession() );
        if ( ad == null ) {
            throw new IllegalStateException( "ArrayDesigns must be converted before datasets - didn't find "
                    + geoDataset.getPlatform() );
        }

        ad.setDescription( ad.getDescription() + "\nFrom " + platform.getGeoAccession() + "\nLast Updated: "
                + platform.getLastUpdateDate() );

        LocalFile arrayDesignRawFile = convertSupplementaryFileToLocalFile( platform );
        if ( arrayDesignRawFile != null ) {
            Collection<LocalFile> arrayDesignLocalFiles = ad.getLocalFiles();
            if ( arrayDesignLocalFiles == null ) {
                arrayDesignLocalFiles = new HashSet<LocalFile>();
            }
            arrayDesignLocalFiles.add( arrayDesignRawFile );
            ad.setLocalFiles( arrayDesignLocalFiles );
        }

        convertDataSetDataVectors( geoDataset, expExp );

        convertSubsetAssociations( expExp, geoDataset );
        return expExp;

    }

    /**
     * Use this when we don't have a GDS for a GSE.
     * 
     * @param geoSeries
     * @param expExp
     */
    private void convertSeriesDataVectors( GeoSeries geoSeries, ExpressionExperiment expExp ) {
        /*
         * Tricky thing is that series contains data from multiple platforms.
         */
        Map<GeoPlatform, List<GeoSample>> platformSamples = DatasetCombiner.getPlatformSampleMap( geoSeries );

        for ( GeoPlatform platform : platformSamples.keySet() ) {
            convertSampleOnPlatformVectors( expExp, platformSamples.get( platform ), platform );
        }

    }

    /**
     * Convert the GEO data into DesignElementDataVectors associated with the ExpressionExperiment
     * 
     * @param geoDataset Source of the data
     * @param expExp ExpressionExperiment to fill in.
     */
    private void convertDataSetDataVectors( GeoDataset geoDataset, ExpressionExperiment expExp ) {
        List<GeoSample> datasetSamples = new ArrayList<GeoSample>( getDatasetSamples( geoDataset ) );
        GeoPlatform geoPlatform = geoDataset.getPlatform();

        convertSampleOnPlatformVectors( expExp, datasetSamples, geoPlatform );
    }

    /**
     * @param expExp
     * @param datasetSamples
     * @param geoPlatform
     */
    private void convertSampleOnPlatformVectors( ExpressionExperiment expExp, List<GeoSample> datasetSamples,
            GeoPlatform geoPlatform ) {
        assert datasetSamples.size() > 0 : "No samples in dataset";

        BioAssayDimension bioAssayDimension = convertGeoSampleList( datasetSamples, expExp );

        if ( bioAssayDimension.getBioAssays().size() == 0 )
            throw new IllegalStateException( "No bioAssays in the BioAssayDimension" );

        sanityCheckQuantitationTypes( datasetSamples );

        List<String> quantitationTypes = datasetSamples.iterator().next().getColumnNames();
        List<String> quantitationTypeDescriptions = datasetSamples.iterator().next().getColumnDescriptions();
        boolean first = true;

        /*
         * For the data that are put in 'datasets' (GDS), we know the type of data, but it can be misleading (e.g., Affy
         * data is 'counts'). For others we just have free text in the column descriptions
         */

        // use a List for performance.
        Collection<DesignElementDataVector> vectors = new ArrayList<DesignElementDataVector>();

        int quantitationTypeIndex = 0;
        for ( String quantitationType : quantitationTypes ) {

            // skip the first quantitationType, it's the ID or ID_REF.
            if ( first ) {
                first = false;
                quantitationTypeIndex++;
                continue;
            }

            /*
             * We get the data by index, not quantitation type name, because the column names often do not match up
             * among the samples. The first quantitation type is in column 0 (the zeroth column is the ID_REF)
             */
            Map<String, List<Object>> dataVectors = makeDataVectors( datasetSamples, quantitationTypeIndex );

            if ( dataVectors == null ) {
                log.debug( "No data for " + quantitationType );
                quantitationTypeIndex++;
                continue;
            } else {
                log.debug( "Got " + dataVectors.size() + " data vectors for " + quantitationType );
            }

            QuantitationType qt = QuantitationType.Factory.newInstance();
            qt.setName( quantitationType );
            String description = quantitationTypeDescriptions.get( quantitationTypeIndex );
            qt.setDescription( description );
            guessQuantitationTypeParameters( qt, quantitationType, description );

            int count = 0;
            for ( String designElementName : dataVectors.keySet() ) {
                List<Object> dataVector = dataVectors.get( designElementName );
                assert dataVector != null && dataVector.size() != 0;
                DesignElementDataVector vector = convertDesignElementDataVector( geoPlatform, expExp,
                        bioAssayDimension, designElementName, dataVector, qt );

                if ( vector == null ) {
                    continue;
                }

                if ( log.isTraceEnabled() ) {
                    log.trace( designElementName + " " + qt.getName() + " " + qt.getRepresentation() + " "
                            + dataVector.size() + " elements in vector" );
                }

                vectors.add( vector );

                if ( log.isInfoEnabled() && ++count % LOGGING_VECTOR_COUNT_UPDATE == 0 ) {
                    log.info( count + " Data vectors added" );
                }
            }
            if ( log.isInfoEnabled() ) {
                log.info( count + " Data vectors added for '" + quantitationType + "'" );
            }

            quantitationTypeIndex++;
        }

        addVectorsToExpressionExperiment( expExp, vectors );

    }

    /**
     * @param expExp
     * @param vectors
     */
    private void addVectorsToExpressionExperiment( final ExpressionExperiment expExp,
            final Collection<DesignElementDataVector> vectors ) {
        FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {

            public Boolean call() throws Exception {
                if ( expExp.getDesignElementDataVectors().size() == 0 ) {
                    expExp.setDesignElementDataVectors( new HashSet<DesignElementDataVector>( vectors ) );
                } else {
                    expExp.getDesignElementDataVectors().addAll( vectors );
                }
                return Boolean.TRUE;
            }

        } );

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute( future );
        executor.shutdown();

        while ( !future.isDone() ) {
            try {
                TimeUnit.SECONDS.sleep( 1L );
            } catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
            log.info( expExp.getDesignElementDataVectors().size() + " vectors added." );
        }

        try {
            Boolean ok = future.get();
            if ( !ok ) {
                throw new RuntimeException( "Something bad happened during adding of expression vectors." );
            }
            log.info( expExp.getDesignElementDataVectors().size() + " vectors added." );
        } catch ( ExecutionException e ) {
            throw new RuntimeException( "Failed", e.getCause() );
        } catch ( CancellationException e ) {
            throw new RuntimeException( "Cancelled", e.getCause() );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( "interrupted", e.getCause() );
        }
    }

    /**
     * @param geoDataset
     * @param expExp
     */
    private void convertDatasetDescriptions( GeoDataset geoDataset, ExpressionExperiment expExp ) {
        if ( StringUtils.isEmpty( expExp.getDescription() ) ) {
            expExp.setDescription( geoDataset.getDescription() ); // probably not empty.
        }

        expExp.setDescription( expExp.getDescription() + "\nIncludes " + geoDataset.getGeoAccession() + ".\n" );
        if ( StringUtils.isNotEmpty( geoDataset.getUpdateDate() ) ) {
            expExp.setDescription( expExp.getDescription() + " Update date: " + geoDataset.getUpdateDate() + ".\n" );
        }

        if ( StringUtils.isEmpty( expExp.getName() ) ) {
            expExp.setName( geoDataset.getTitle() );
        } else {
            expExp.setDescription( expExp.getDescription() + " Dataset description " + geoDataset.getGeoAccession()
                    + ": " + geoDataset.getTitle() + ".\n" );
        }
    }

    /**
     * @param geoDataset
     * @param expExp
     * @param bioAssayDimension
     * @param designElementName
     * @param dataVector
     * @return
     */
    private DesignElementDataVector convertDesignElementDataVector( GeoPlatform geoPlatform,
            ExpressionExperiment expExp, BioAssayDimension bioAssayDimension, String designElementName,
            List<Object> dataVector, QuantitationType qt ) {
        byte[] blob = convertData( dataVector, qt );
        if ( blob == null ) {
            return null;
        }
        if ( log.isDebugEnabled() ) {
            log.debug( blob.length + " bytes for " + dataVector.size() + " raw elements" );
        }

        CompositeSequence compositeSequence = platformDesignElementMap.get( convertPlatform( geoPlatform ).getName() )
                .get( designElementName );

        if ( compositeSequence == null ) {
            assert compositeSequence != null : "No composite sequence " + designElementName;
        }

        if ( compositeSequence.getBiologicalCharacteristic() != null
                && compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null ) {
            assert compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry().getExternalDatabase()
                    .getName() != null;
        }

        DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
        vector.setDesignElement( compositeSequence );
        vector.setExpressionExperiment( expExp );

        vector.setBioAssayDimension( bioAssayDimension );
        vector.setQuantitationType( qt );
        vector.setData( blob );
        return vector;
    }

    /**
     * @param datasetSamples List of GeoSamples to be matched up with BioAssays.
     * @param expExp ExpresssionExperiment
     * @return BioAssayDimension representing the samples.
     */
    @SuppressWarnings("unchecked")
    private BioAssayDimension convertGeoSampleList( List<GeoSample> datasetSamples, ExpressionExperiment expExp ) {
        BioAssayDimension resultBioAssayDimension = BioAssayDimension.Factory.newInstance();

        StringBuilder bioAssayDimName = new StringBuilder();
        Collections.sort( datasetSamples );
        for ( GeoSample sample : datasetSamples ) {
            boolean found = false;
            String sampleAcc = sample.getGeoAccession();
            bioAssayDimName.append( sampleAcc + "," ); // this is rather silly!
            found = matchSampleToBioAssay( expExp, resultBioAssayDimension, sampleAcc );
            if ( !found ) {
                // this is normal because not all headings are
                // sample ids.
                log.warn( "No bioassay match for " + sampleAcc );
            }
        }
        log.info( resultBioAssayDimension.getBioAssays().size() + " Bioassays in biodimension" );
        resultBioAssayDimension.setName( formatName( bioAssayDimName ) );
        resultBioAssayDimension.setDescription( bioAssayDimName.toString() );
        return resultBioAssayDimension;
    }

    /**
     * @param platform
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign convertPlatform( GeoPlatform platform ) {

        if ( seenPlatforms.containsKey( platform.getGeoAccession() ) ) {
            return ( seenPlatforms.get( platform.getGeoAccession() ) );
        }

        if ( platform.getTechnology() == PlatformType.SAGE || platform.getTechnology() == PlatformType.SAGENlaIII
                || platform.getTechnology() == PlatformType.SAGERsaI
                || platform.getTechnology() == PlatformType.SAGESau3A ) {
            throw new UnsupportedOperationException( "This data set uses SAGE, it cannot be handled yet" );
        }

        log.debug( "Converting platform: " + platform.getGeoAccession() );
        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( platform.getTitle() );
        arrayDesign.setShortName( platform.getGeoAccession() );
        arrayDesign.setDescription( platform.getDescriptions() );

        platformDesignElementMap.put( arrayDesign.getName(), new HashMap<String, CompositeSequence>() );

        Taxon taxon = convertPlatformOrganism( platform );

        // convert the design element information.
        String identifier = determinePlatformIdentifier( platform );
        String externalReference = determinePlatformExternalReferenceIdentifier( platform );
        String descriptionColumn = determinePlatformDescriptionColumn( platform );
        ExternalDatabase externalDb = determinePlatformExternalDatabase( platform );

        assert externalDb != null;

        List<String> identifiers = platform.getColumnData( identifier );
        List<String> externalRefs = platform.getColumnData( externalReference );
        List<String> descriptions = platform.getColumnData( descriptionColumn );

        assert identifier != null;
        assert externalRefs != null : "No externalRefs found for column " + externalReference;

        assert externalRefs.size() == identifiers.size() : "Unequal numbers of identifiers and external references! "
                + externalRefs.size() + " != " + identifiers.size();

        if ( log.isDebugEnabled() ) {
            log.debug( "Converting " + identifiers.size() + " probe identifiers on GEO platform "
                    + platform.getGeoAccession() );
        }

        Iterator<String> refIter = externalRefs.iterator();
        Iterator<String> descIter = null;

        if ( descriptions != null ) {
            descIter = descriptions.iterator();
        }

        // much faster than hashset to add to when it gets large.
        Collection compositeSequences = new ArrayList( 5000 );
        for ( String id : identifiers ) {
            String externalRef = refIter.next();

            String[] refs = externalRef.split( "," );
            String description = "";

            if ( refs.length > 1 ) {
                description = description + "Multiple external sequence references: " + externalRef + "; ";
                externalRef = refs[0];
            }

            if ( descIter != null ) description = description + " " + descIter.next();

            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( id );
            cs.setDescription( description );
            cs.setArrayDesign( arrayDesign );

            if ( StringUtils.isBlank( externalRef ) ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( "Blank external reference for biosequence for " + cs + " on " + arrayDesign
                            + ", no biological characteristic will be added." );
                }
            } else {
                BioSequence bs = BioSequence.Factory.newInstance();
                bs.setTaxon( taxon );
                bs.setPolymerType( PolymerType.DNA );
                bs.setType( SequenceType.DNA );

                DatabaseEntry dbe;
                if ( externalDb.getName().equalsIgnoreCase( "genbank" ) ) {
                    // deal with accessions in the form XXXXX.N
                    dbe = ExternalDatabaseUtils.getGenbankAccession( externalRef );
                    dbe.setExternalDatabase( externalDb ); // make sure it matches the one used here.
                    bs.setName( dbe.getAccession() ); // trimmed version.
                } else {
                    bs.setName( externalRef );
                    dbe = DatabaseEntry.Factory.newInstance();
                    dbe.setAccession( externalRef );
                    dbe.setExternalDatabase( externalDb );
                }

                bs.setSequenceDatabaseEntry( dbe );
                cs.setBiologicalCharacteristic( bs );
            }

            compositeSequences.add( cs );

            platformDesignElementMap.get( arrayDesign.getName() ).put( id, cs );
        }
        arrayDesign.setCompositeSequences( new HashSet( compositeSequences ) );
        arrayDesign.setAdvertisedNumberOfDesignElements( compositeSequences.size() );

        // We don't get reporters from GEO SOFT files.
        // arrayDesign.setReporters( new HashSet() );

        if ( StringUtils.isNotBlank( platform.getManufacturer() ) ) {
            Contact manufacturer = Contact.Factory.newInstance();
            manufacturer.setName( platform.getManufacturer() );
            arrayDesign.setDesignProvider( manufacturer );
        }

        arrayDesign.getExternalReferences().add( convertDatabaseEntry( platform ) );

        seenPlatforms.put( platform.getGeoAccession(), arrayDesign );

        return arrayDesign;
    }

    /**
     * @param platform
     * @return
     */
    private Taxon convertPlatformOrganism( GeoPlatform platform ) {

        Collection<String> organisms = platform.getOrganisms();

        if ( organisms.size() > 1 ) {
            log.warn( "!!!! Multiple organisms represented on platform " + platform
                    + " --- BioSequences will be associated with the first one found." );
        }

        if ( organisms.size() == 0 ) {
            log.warn( "No organisms for platform " + platform );
            return null;
        }

        String organism = organisms.iterator().next();
        log.debug( "Organism: " + organism );

        /* see if taxon exists in map */
        if ( platformTaxonMap.containsKey( organism ) ) {
            return platformTaxonMap.get( organism );
        }

        /* if not, either create a new one and persist, or get from db and put in map. */

        if ( organism.toLowerCase().startsWith( GeoConverter.RAT ) ) {
            organism = GeoConverter.RAT; // we don't distinguish between species.
        }

        Taxon taxon = Taxon.Factory.newInstance();

        taxon.setScientificName( organism );

        if ( taxonService != null ) {
            Taxon t = taxonService.findOrCreate( taxon );
            if ( t != null ) {
                taxon = t;
            }
        }

        platformTaxonMap.put( organism, taxon );
        return taxon;

    }

    /**
     * @param repType
     * @return
     */
    private OntologyEntry convertReplicatationType( ReplicationType repType ) {
        OntologyEntry result = OntologyEntry.Factory.newInstance();
        ExternalDatabase mged = ExternalDatabase.Factory.newInstance();

        if ( !repType.equals( VariableType.other ) ) {
            mged.setName( "MGED Ontology" );
            mged.setType( DatabaseType.ONTOLOGY );
            result.setExternalDatabase( mged );
        }

        if ( repType.equals( ReplicationType.biologicalReplicate ) ) {
            result.setValue( "biological_replicate" );
        } else if ( repType.equals( ReplicationType.technicalReplicateExtract ) ) {
            result.setValue( "technical_replicate" );
        } else if ( repType.equals( ReplicationType.technicalReplicateLabeledExtract ) ) {
            result.setValue( "technical_replicate" ); // MGED doesn't have a term to distinguish these.
        } else {
            throw new IllegalStateException();
        }

        return result;

    }

    /**
     * Convert a variable into a ExperimentalFactor
     * 
     * @param variable
     * @return
     */
    private ExperimentalFactor convertReplicationToFactor( GeoReplication replication ) {
        log.debug( "Converting replication " + replication.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( replication.getType().toString() );
        result.setDescription( replication.getDescription() );
        OntologyEntry term = convertReplicatationType( replication.getType() );

        result.setCategory( term );
        return result;

    }

    /**
     * @param replication
     * @return
     */
    private FactorValue convertReplicationToFactorValue( GeoReplication replication ) {
        FactorValue factorValue = FactorValue.Factory.newInstance();
        factorValue.setValue( replication.getDescription() );
        return factorValue;
    }

    /**
     * @param variable
     * @param factor
     */
    @SuppressWarnings("unchecked")
    private void convertReplicationToFactorValue( GeoReplication replication, ExperimentalFactor factor ) {
        FactorValue factorValue = convertReplicationToFactorValue( replication );
        factor.getFactorValues().add( factorValue );
    }

    /**
     * A Sample corresponds to a BioAssay; the channels correspond to BioMaterials.
     * 
     * @param sample
     */
    @SuppressWarnings("unchecked")
    private BioAssay convertSample( GeoSample sample, BioMaterial bioMaterial ) {
        if ( sample == null ) {
            log.warn( "Null sample" );
            return null;
        }

        if ( sample.getGeoAccession() == null || sample.getGeoAccession().length() == 0 ) {
            log.error( "No GEO accession for sample" );
            return null;
        }

        log.debug( "Converting sample: " + sample.getGeoAccession() );

        BioAssay bioAssay = BioAssay.Factory.newInstance();
        String title = sample.getTitle();
        if ( StringUtils.isBlank( title ) ) {
            // throw new IllegalArgumentException( "Title cannot be blank for sample " + sample );
            log.warn( "Blank title for sample " + sample + ", using accession number instead." );
            sample.setTitle( sample.getGeoAccession() );
        }
        bioAssay.setName( sample.getTitle() );
        bioAssay.setDescription( sample.getDescription() );
        bioAssay.setAccession( convertDatabaseEntry( sample ) );

        /*
         * NOTE - according to GEO (http://www.ncbi.nlm.nih.gov/projects/geo/info/soft2.html) "variable information is
         * optional and does not appear in Series records or downloads, but will be used to assemble corresponding GEO
         * DataSet records" If we would get that informatio we would pass it into this method as
         * expExp.getExperimentalDesign().getExperimentalFactors().
         */

        // : use the ones from the ExperimentalFactor. In other words, these factor values should correspond to
        // experimentalfactors
        // for ( GeoReplication replication : sample.getReplicates() ) {
        //
        // // find the experimentalFactor that matches this.
        //
        // bioMaterial.getFactorValues().add( convertReplicationToFactorValue( replication ) );
        // }
        //
        // // : use the ones from the ExperimentalFactor.
        // for ( GeoVariable variable : sample.getVariables() ) {
        //
        // // find the experimentalFactor that matches this.
        //
        // bioMaterial.getFactorValues().add( convertVariableToFactorValue( variable ) );
        // }
        for ( GeoChannel channel : sample.getChannels() ) {
            /*
             * In reality GEO does not have information about the samples run on each channel. We're just making it up.
             * So we need to just add the channel information to the biomaterials we have already.
             */
            convertChannel( sample, channel, bioMaterial );
            bioAssay.getSamplesUsed().add( bioMaterial );
        }

        Taxon lastTaxon = null;

        for ( GeoPlatform platform : sample.getPlatforms() ) {
            ArrayDesign arrayDesign;
            if ( seenPlatforms.containsKey( platform.getGeoAccession() ) ) {
                arrayDesign = seenPlatforms.get( platform.getGeoAccession() );
            } else {
                arrayDesign = convertPlatform( platform );
            }

            // Allow for possibility that platforms use different taxa.
            Taxon taxon = convertPlatformOrganism( platform );
            if ( lastTaxon != null && !taxon.equals( lastTaxon ) ) {
                log.warn( "Multiple taxa found among platforms for single sample, "
                        + " new biomaterial will be associated with the last taxon found." );
            }
            lastTaxon = taxon;

            bioMaterial.setSourceTaxon( taxon );

            bioAssay.setArrayDesignUsed( arrayDesign );

        }

        return bioAssay;
    }

    /**
     * Convert a GEO series into one or more ExpressionExperiments. The more than one case comes up if the are platforms
     * from more than one organism represented in the series. If the series is split into two or more
     * ExpressionExperiments, each refers to a modified GEO accession such as GSE2393.1, GSE2393.2 etc for each organism
     * <p>
     * Similarly, because there is no concept of "biomaterial" in GEO, samples that are inferred to have been run using
     * the same biomaterial. The biomaterials are given names after the GSE and the bioAssays (GSMs) such as
     * GSE2939_biomaterial_1|GSM12393|GSN12394.
     * 
     * @param series
     * @return
     */
    private Collection<ExpressionExperiment> convertSeries( GeoSeries series ) {

        Collection<ExpressionExperiment> converted = new HashSet<ExpressionExperiment>();

        // figure out if there are multiple species involved here.

        Map<String, Collection<GeoDataset>> organismDatasetMap = getOrganismDatasetMap( series );

        if ( organismDatasetMap.size() > 1 ) {
            log.warn( "**** Multiple-species dataset! ****" );
            int i = 1;
            for ( String organism : organismDatasetMap.keySet() ) {
                convertSpeciesSpecific( series, converted, organismDatasetMap, i, organism );
                i++;
            }
        } else {
            converted.add( this.convertSeries( series, null ) );
        }

        return converted;
    }

    /**
     * @param series
     * @param converted
     * @param organismDatasetMap
     * @param i
     * @param organism
     */
    private void convertSpeciesSpecific( GeoSeries series, Collection<ExpressionExperiment> converted,
            Map<String, Collection<GeoDataset>> organismDatasetMap, int i, String organism ) {
        GeoSeries speciesSpecific = new GeoSeries();

        Collection<GeoDataset> datasets = organismDatasetMap.get( organism );
        assert datasets.size() > 0;

        for ( GeoSample sample : series.getSamples() ) {
            // ugly, we have to assume there is only one platform and one organism...
            if ( sample.getPlatforms().iterator().next().getOrganisms().iterator().next().equals( organism ) ) {
                speciesSpecific.addSample( sample );
            }
        }

        // strip out samples that aren't from this organism.

        for ( GeoDataset dataset : datasets ) {
            dataset.dissociateFromSeries( series );
            speciesSpecific.addDataSet( dataset );
        }

        /*
         * Basically copy over most of the information
         */
        speciesSpecific.setContact( series.getContact() );
        speciesSpecific.setContributers( series.getContributers() );
        speciesSpecific.setGeoAccession( series.getGeoAccession() + "." + i );
        speciesSpecific.setKeyWords( series.getKeyWords() );
        speciesSpecific.setOverallDesign( series.getOverallDesign() );
        speciesSpecific.setPubmedIds( series.getPubmedIds() );
        speciesSpecific.setReplicates( series.getReplicates() );
        speciesSpecific.setSampleCorrespondence( series.getSampleCorrespondence() );
        speciesSpecific.setSummaries( series.getSummaries() );
        speciesSpecific.setTitle( series.getTitle() + " - " + organism );
        speciesSpecific.setWebLinks( series.getWebLinks() );

        converted.add( convertSeries( speciesSpecific, null ) );
    }

    /**
     * @param series
     * @return
     */
    private Map<String, Collection<GeoDataset>> getOrganismDatasetMap( GeoSeries series ) {
        Map<String, Collection<GeoDataset>> organisms = new HashMap<String, Collection<GeoDataset>>();

        for ( GeoDataset dataset : series.getDatasets() ) {
            String organism = dataset.getOrganism();
            if ( organisms.get( organism ) == null ) {
                organisms.put( organism, new HashSet<GeoDataset>() );
            }
            organisms.get( organism ).add( dataset );
        }
        return organisms;
    }

    /**
     * @param series
     * @param resultToAddTo
     * @return
     * @see convertSeries
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperiment convertSeries( GeoSeries series, ExpressionExperiment resultToAddTo ) {
        if ( series == null ) return null;
        log.info( "Converting series: " + series.getGeoAccession() );

        Collection<GeoDataset> dataSets = series.getDatasets();
        Collection<String> dataSetsToSkip = new HashSet<String>();
        Collection<GeoSample> samplesToSkip = new HashSet<GeoSample>();
        checkForDataToSkip( dataSets, dataSetsToSkip, samplesToSkip );
        if ( dataSets.size() > 0 && dataSetsToSkip.size() == dataSets.size() ) {
            return null;
        }

        ExpressionExperiment expExp;

        if ( resultToAddTo == null ) {
            expExp = ExpressionExperiment.Factory.newInstance();
            expExp.setDescription( "" );
        } else {
            expExp = resultToAddTo;
        }

        expExp.setDescription( series.getSummaries() + ".\nDate " + series.getGeoAccession() );

        if ( series.getLastUpdateDate() != null ) {
            expExp.setDescription( expExp.getDescription() + " Last Updated: " + series.getLastUpdateDate() + "\n" );
        }

        expExp.setName( series.getTitle() );
        expExp.setShortName( series.getGeoAccession() );

        convertContacts( series, expExp );

        expExp.setAccession( convertDatabaseEntry( series ) );

        LocalFile expExpRawDataFile = convertSupplementaryFileToLocalFile( series );
        expExp.setRawDataFile( expExpRawDataFile );

        ExperimentalDesign design = ExperimentalDesign.Factory.newInstance();
        design.setDescription( "" );
        design.setName( "" );
        Collection<GeoVariable> variables = series.getVariables().values();
        for ( GeoVariable variable : variables ) {
            log.debug( "Adding variable " + variable );
            ExperimentalFactor ef = convertVariableToFactor( variable );
            convertVariableToFactorValue( variable, ef );
            design.getExperimentalFactors().add( ef );
            design.setName( variable.getDescription() + " " + design.getName() );
        }

        if ( series.getKeyWords().size() > 0 ) {
            for ( String keyWord : series.getKeyWords() ) {
                // design.setDescription( design.getDescription() + " Keyword: " + keyWord );
                OntologyEntry o = OntologyEntry.Factory.newInstance();
                o.setCategory( "GEO Keyword" );
                o.setValue( keyWord );
                o.setDescription( "Keyword from GEO series definition file." );
                o.setExternalDatabase( this.geoDatabase );
            }
        }

        if ( series.getOverallDesign() != null ) {
            design.setDescription( design.getDescription() + " Overall design: " + series.getOverallDesign() );
        }

        Collection<GeoReplication> replication = series.getReplicates().values();
        for ( GeoReplication replicate : replication ) {
            log.debug( "Adding replication " + replicate );
            ExperimentalFactor ef = convertReplicationToFactor( replicate );
            convertReplicationToFactorValue( replicate, ef );
            design.getExperimentalFactors().add( ef );
        }

        expExp.setExperimentalDesign( design );

        // GEO does not have the concept of a biomaterial.
        Collection<GeoSample> allSeriesSamples = series.getSamples();
        expExp.setBioAssays( new HashSet() );

        if ( series.getSampleCorrespondence().size() == 0 ) {
            throw new IllegalArgumentException( "No sample correspondence!" );
        }

        // spits out a big summary of the correspondence.
        if ( log.isDebugEnabled() ) log.debug( series.getSampleCorrespondence() );

        int i = 1;

        /* For each _set_ of "corresponding" samples (from the same RNA) we make up a new BioMaterial. */
        for ( Iterator iter = series.getSampleCorrespondence().iterator(); iter.hasNext(); ) {

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            String bioMaterialName = getBiomaterialPrefix( series, i );
            String bioMaterialDescription = "Biomaterial corresponding to samples: ";

            // From the series samples, find the sample that corresponds and convert it.
            Set<String> correspondingSamples = ( Set<String> ) iter.next();
            for ( String cSample : correspondingSamples ) {
                boolean found = false;
                for ( GeoSample sample : allSeriesSamples ) {
                    if ( sample == null || sample.getGeoAccession() == null ) {
                        log.warn( "Null sample or no accession for " + sample );
                        continue;
                    }

                    if ( samplesToSkip.contains( sample ) ) {
                        continue;
                    }

                    String accession = sample.getGeoAccession();

                    if ( accession.equals( cSample ) ) {
                        BioAssay ba = convertSample( sample, bioMaterial );

                        LocalFile rawDataFile = convertSupplementaryFileToLocalFile( sample );
                        ba.setRawDataFile( rawDataFile );// deal with null at UI

                        ba.setDescription( ba.getDescription() + "\nSource GEO sample is " + sample.getGeoAccession()
                                + "\nLast updated (according to GEO): " + sample.getLastUpdateDate() );
                        ba.getSamplesUsed().add( bioMaterial );
                        bioMaterial.getBioAssaysUsedIn().add( ba );
                        bioMaterialDescription = bioMaterialDescription + " " + sample;
                        bioMaterialName = addToBioMaterialName( bioMaterialName, sample );
                        if ( log.isDebugEnabled() )
                            log.debug( "Adding " + ba + " and associating with  " + bioMaterial );
                        expExp.getBioAssays().add( ba );
                        found = true;
                        break;
                    }

                }
                if ( !found ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "No sample found in " + series + " to match " + cSample
                                + "; this can happen if some samples were not run on all platforms." );

                }

                i++;
            }
            bioMaterial.setName( bioMaterialName );
            bioMaterial.setDescription( bioMaterialDescription );
        }

        log.info( "Expression Experiment from " + series + " has " + expExp.getBioAssays().size() + " bioassays" );

        // Dataset has additional information about the samples.

        if ( dataSets.size() == 0 ) {
            // we miss extra description and the subset information.
            convertSeriesDataVectors( series, expExp );
        } else {
            for ( GeoDataset dataset : dataSets ) {
                if ( dataSetsToSkip.contains( dataset.getGeoAccession() ) ) continue;
                convertDataset( dataset, expExp );
            }
        }

        return expExp;
    }

    /**
     * Flag as unneeded data that are not from experiments types that we support, such as ChIP.
     * 
     * @param dataSets
     * @param dataSetsToSkip
     * @param samplesToSkip
     */
    private void checkForDataToSkip( Collection<GeoDataset> dataSets, Collection<String> dataSetsToSkip,
            Collection<GeoSample> samplesToSkip ) {
        for ( GeoDataset dataset : dataSets ) {

            // This doesn't cover every possibility...
            if ( dataset.getExperimentType() == ExperimentType.arrayCGH
                    || dataset.getExperimentType() == ExperimentType.ChIPChip
                    || dataset.getExperimentType() == ExperimentType.geneExpressionSAGEbased ) {
                log.warn( "Gemma does not know how to handle " + dataset.getExperimentType() );

                if ( dataSets.size() == 1 ) {
                    log.warn( "Because the experiment type cannot be handled, "
                            + "and there is only one data set in this series, nothing will be returned!" );
                }
                samplesToSkip.addAll( this.getDatasetSamples( dataset ) );
                dataSetsToSkip.add( dataset.getGeoAccession() );
            } else {
                log.info( "Data from " + dataset + " is " + dataset.getExperimentType() );
            }
        }
    }

    /**
     * Converts a supplementary file to a LocalFile object. If the supplementary file is null, the LocalFile=null is
     * returned.
     * 
     * @param object
     * @return LocalFile
     */
    public LocalFile convertSupplementaryFileToLocalFile( Object object ) {

        URL remoteFileUrl = null;
        LocalFile remoteFile = null;

        if ( object instanceof GeoSeries ) {
            GeoSeries series = ( GeoSeries ) object;
            if ( !StringUtils.isEmpty( series.getSupplementaryFile() ) ) {
                try {
                    remoteFile = LocalFile.Factory.newInstance();
                    remoteFileUrl = new URL( series.getSupplementaryFile() );
                } catch ( MalformedURLException e ) {
                    reportUrlError( remoteFileUrl, e );
                }
            }
        }

        else if ( object instanceof GeoSample ) {
            GeoSample sample = ( GeoSample ) object;
            if ( !StringUtils.isEmpty( sample.getSupplementaryFile() ) ) {
                try {
                    remoteFile = LocalFile.Factory.newInstance();
                    remoteFileUrl = new URL( sample.getSupplementaryFile() );
                } catch ( MalformedURLException e ) {
                    reportUrlError( remoteFileUrl, e );
                }
            }
        }

        else if ( object instanceof GeoPlatform ) {
            GeoPlatform platform = ( GeoPlatform ) object;
            if ( !StringUtils.isEmpty( platform.getSupplementaryFile() ) ) {
                try {
                    remoteFile = LocalFile.Factory.newInstance();
                    remoteFileUrl = new URL( platform.getSupplementaryFile() );
                } catch ( MalformedURLException e ) {
                    reportUrlError( remoteFileUrl, e );
                }
            }
        }

        /* nulls allowed in remoteFile ... deal with later. */
        if ( remoteFile != null ) remoteFile.setRemoteURL( remoteFileUrl );

        return remoteFile;
    }

    /**
     * @param remoteFileUrl
     * @param e
     */
    private void reportUrlError( URL remoteFileUrl, MalformedURLException e ) {
        log.error( "Problems with url: " + remoteFileUrl
                + ".  Will not store the url of the raw data file.  Full error is: " );
        e.printStackTrace();
    }

    /**
     * @param bioMaterialName
     * @param sample
     * @return
     */
    private String addToBioMaterialName( String bioMaterialName, GeoSample sample ) {
        bioMaterialName = bioMaterialName + "|" + sample;
        return bioMaterialName;
    }

    /**
     * @param series
     * @param i
     * @return
     */
    private String getBiomaterialPrefix( GeoSeries series, int i ) {
        String bioMaterialName = series.getGeoAccession() + "_bioMaterial_" + i;
        return bioMaterialName;
    }

    /**
     * @param expExp
     * @param geoSubSet
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperimentSubSet convertSubset( ExpressionExperiment expExp, GeoSubset geoSubSet ) {
        ExpressionExperimentSubSet subSet = getExistingOrNewSubSet( expExp, geoSubSet );
        for ( GeoSample sample : geoSubSet.getSamples() ) {
            boolean found = addMatchingBioAssayToSubSet( subSet, sample.getGeoAccession(), expExp );
            assert found : "No matching bioassay found for " + sample.getGeoAccession() + " in subset. "
                    + " Make sure the ExpressionExperiment was initialized "
                    + "properly by converting the samples before converting the subsets.";
        }
        return subSet;
    }

    /**
     * check to see if the expExp already has a matching subset to add this to.
     * 
     * @param expExp
     * @param geoSubSet
     * @return
     */
    private ExpressionExperimentSubSet getExistingOrNewSubSet( ExpressionExperiment expExp, GeoSubset geoSubSet ) {

        ExpressionExperimentSubSet subSet = null;

        boolean alreadyExists = false;
        for ( ExpressionExperimentSubSet existingSubset : expExp.getSubsets() ) {
            if ( geoSubSet.getDescription().equals( existingSubset.getName() ) ) {
                subSet = existingSubset;
                alreadyExists = true;
                break;
            }
        }

        if ( !alreadyExists ) {
            subSet = ExpressionExperimentSubSet.Factory.newInstance();
            subSet.setName( geoSubSet.getDescription() );
            subSet.setDescription( geoSubSet.getType().toString() );
            subSet.setSourceExperiment( expExp );
            subSet.setBioAssays( new HashSet<BioAssay>() );
        }
        return subSet;
    }

    /**
     * Converts Geo subsets to experimental factors. This adds a new factor value to the experimental factor of an
     * experimental design, and adds the factor value to each BioMaterial of a specific BioAssay.
     * 
     * @param expExp
     * @param geoSubSet
     * @return ExperimentalFactor
     */
    public void convertSubsetToExperimentalFactor( ExpressionExperiment expExp, GeoSubset geoSubSet ) {

        ExperimentalDesign experimentalDesign = expExp.getExperimentalDesign();
        Collection<ExperimentalFactor> existingExperimentalFactors = experimentalDesign.getExperimentalFactors();

        ExperimentalFactor experimentalFactor = ExperimentalFactor.Factory.newInstance();
        experimentalFactor.setName( geoSubSet.getType().toString() );
        experimentalFactor.setDescription( "Converted from GEO subset " + geoSubSet.getGeoAccession() );

        boolean duplicateExists = false;
        for ( ExperimentalFactor existingExperimentalFactor : existingExperimentalFactors ) {
            if ( ( experimentalFactor.getName() ).equalsIgnoreCase( existingExperimentalFactor.getName() ) ) {
                duplicateExists = true;
                experimentalFactor = existingExperimentalFactor;
                if ( log.isDebugEnabled() )
                    log.debug( experimentalFactor.getName()
                            + " already exists.  Not adding to list of experimental factors." );
                break;
            }
        }

        if ( !duplicateExists ) {
            experimentalDesign.getExperimentalFactors().add( experimentalFactor );
        }

        /* bi-directional ... don't forget this. */
        experimentalFactor.setExperimentalDesign( experimentalDesign );

        FactorValue factorValue = convertSubsetDescriptionToFactorValue( geoSubSet, experimentalFactor );

        // would be preferable.
        experimentalFactor.getFactorValues().add( factorValue );

        addFactorValueToBioMaterial( expExp, geoSubSet, factorValue );
    }

    private FactorValue convertSubsetDescriptionToFactorValue( GeoSubset geoSubSet,
            ExperimentalFactor experimentalFactor ) {
        // By definition each subset defines a new factor value.
        FactorValue factorValue = FactorValue.Factory.newInstance();
        factorValue.setExperimentalFactor( experimentalFactor );
        factorValue.setValue( StringUtils.strip( geoSubSet.getDescription() ) ); // :( OntologyEntry or Measurement
        return factorValue;
    }

    private void addFactorValueToBioMaterial( ExpressionExperiment expExp, GeoSubset geoSubSet, FactorValue factorValue ) {
        // fill in biomaterial-->factorvalue.
        for ( GeoSample sample : geoSubSet.getSamples() ) {

            // find the matching biomaterial(s) in the expression experiment.
            for ( BioAssay bioAssay : expExp.getBioAssays() ) {
                if ( bioAssay.getAccession().getAccession().equals( sample.getGeoAccession() ) ) {
                    Collection<BioMaterial> bioMaterials = bioAssay.getSamplesUsed();

                    // this is a bit funny if one of them is the control channel
                    // ....how do we figure this out!
                    for ( BioMaterial material : bioMaterials ) {
                        if ( log.isInfoEnabled() ) {
                            log.info( "Adding " + factorValue.getExperimentalFactor() + " : " + factorValue + " to "
                                    + material );
                        }
                        material.getFactorValues().add( factorValue );
                    }
                    break;
                }
            }

        }
    }

    /**
     * @param result
     * @param geoDataset
     */
    @SuppressWarnings("unchecked")
    private void convertSubsetAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        for ( GeoSubset subset : geoDataset.getSubsets() ) {
            log.debug( "Converting subset: " + subset.getType() );
            ExpressionExperimentSubSet ees = convertSubset( result, subset );
            result.getSubsets().add( ees );

            convertSubsetToExperimentalFactor( result, subset );
        }
    }

    /**
     * Convert a variable into a ExperimentalFactor
     * 
     * @param variable
     * @return
     */
    private ExperimentalFactor convertVariableToFactor( GeoVariable variable ) {
        log.debug( "Converting variable " + variable.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( variable.getType().toString() );
        result.setDescription( variable.getDescription() );
        OntologyEntry term = convertVariableType( variable.getType() );
        result.setCategory( term );
        return result;

    }

    /**
     * @param variable
     * @return
     */
    private FactorValue convertVariableToFactorValue( GeoVariable variable ) {
        log.info( "Converting variable " + variable );
        FactorValue factorValue = FactorValue.Factory.newInstance();
        factorValue.setValue( variable.getDescription() );
        return factorValue;
    }

    /**
     * @param variable
     * @param factor
     */
    @SuppressWarnings("unchecked")
    private void convertVariableToFactorValue( GeoVariable variable, ExperimentalFactor factor ) {
        FactorValue factorValue = convertVariableToFactorValue( variable );
        factor.getFactorValues().add( factorValue );
    }

    /**
     * Convert a variable
     * 
     * @param variable
     * @return
     */
    private OntologyEntry convertVariableType( VariableType varType ) {
        OntologyEntry categoryTerm = OntologyEntry.Factory.newInstance();
        ExternalDatabase mged = ExternalDatabase.Factory.newInstance();

        if ( !varType.equals( VariableType.other ) ) {
            mged.setName( "MGED Ontology" );
            mged.setType( DatabaseType.ONTOLOGY );
            categoryTerm.setExternalDatabase( mged );
        }

        if ( varType.equals( VariableType.age ) ) {
            categoryTerm.setValue( "Age" );
        } else if ( varType.equals( VariableType.agent ) ) {
            categoryTerm.setValue( "Agent" );
        } else if ( varType.equals( VariableType.cellLine ) ) {
            categoryTerm.setValue( "CellLine" );
        } else if ( varType.equals( VariableType.cellType ) ) {
            categoryTerm.setValue( "CellType" );
        } else if ( varType.equals( VariableType.developmentStage ) ) {
            categoryTerm.setValue( "DevelopmentalStage" );
        } else if ( varType.equals( VariableType.diseaseState ) ) {
            categoryTerm.setValue( "DiseaseState" );
        } else if ( varType.equals( VariableType.dose ) ) {
            categoryTerm.setValue( "Dose" );
        } else if ( varType.equals( VariableType.gender ) ) {
            categoryTerm.setValue( "Sex" );
        } else if ( varType.equals( VariableType.genotypeOrVariation ) ) {
            categoryTerm.setValue( "IndividualGeneticCharacteristics" );
        } else if ( varType.equals( VariableType.growthProtocol ) ) {
            categoryTerm.setValue( "GrowthCondition" );
        } else if ( varType.equals( VariableType.individual ) ) {
            categoryTerm.setValue( "Individiual" );
        } else if ( varType.equals( VariableType.infection ) ) {
            categoryTerm.setValue( "Phenotype" );
        } else if ( varType.equals( VariableType.isolate ) ) {
            categoryTerm.setValue( "Age" );
        } else if ( varType.equals( VariableType.metabolism ) ) {
            categoryTerm.setValue( "Metabolism" );
        } else if ( varType.equals( VariableType.other ) ) {
            categoryTerm.setValue( "Other" );
        } else if ( varType.equals( VariableType.protocol ) ) {
            categoryTerm.setValue( "Protocol" );
        } else if ( varType.equals( VariableType.shock ) ) {
            categoryTerm.setValue( "EnvironmentalStress" );
        } else if ( varType.equals( VariableType.species ) ) {
            categoryTerm.setValue( "Organism" );
        } else if ( varType.equals( VariableType.specimen ) ) {
            categoryTerm.setValue( "BioSample" );
        } else if ( varType.equals( VariableType.strain ) ) {
            categoryTerm.setValue( "StrainOrLine" );
        } else if ( varType.equals( VariableType.stress ) ) {
            categoryTerm.setValue( "EnvironmentalStress" );
        } else if ( varType.equals( VariableType.temperature ) ) {
            categoryTerm.setValue( "Temperature" );
        } else if ( varType.equals( VariableType.time ) ) {
            categoryTerm.setValue( "Time" );
        } else if ( varType.equals( VariableType.tissue ) ) {
            categoryTerm.setValue( "OrganismPart" );
        } else {
            throw new IllegalStateException();
        }

        log.debug( "Category term: " + categoryTerm.getValue() + " " );
        return categoryTerm;

    }

    /**
     * @param platform
     * @return
     */
    private String determinePlatformDescriptionColumn( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyProbeDescription( string ) ) {
                log.info( string + " appears to indicate the  probe descriptions in column " + index + " for platform "
                        + platform );
                return string;
            }
            index++;
        }
        log.warn( "No platform element description column found for " + platform );
        return null;
    }

    /**
     * @param platform
     * @return
     */
    private ExternalDatabase determinePlatformExternalDatabase( GeoPlatform platform ) {
        ExternalDatabase result = ExternalDatabase.Factory.newInstance();

        String likelyExternalDatabaseIdentifier = determinePlatformExternalReferenceIdentifier( platform );
        String dbIdentifierDescription = getDbIdentifierDescription( platform );

        String url = null;
        if ( dbIdentifierDescription == null ) {
            throw new IllegalStateException( "Could not identify database identifier column in " + platform );
        } else if ( dbIdentifierDescription.indexOf( "LINK_PRE:" ) >= 0 ) {
            // example: #ORF = ORF reference LINK_PRE:"http://genome-www4.stanford.edu/cgi-bin/SGD/locus.pl?locus="
            url = dbIdentifierDescription.substring( dbIdentifierDescription.indexOf( "LINK_PRE:" ) );
            result.setWebUri( url );
        }

        if ( likelyExternalDatabaseIdentifier.equals( "GB_ACC" ) || likelyExternalDatabaseIdentifier.equals( "GB_LIST" )
                || likelyExternalDatabaseIdentifier.toLowerCase().equals( "genbank" ) ) {
            if ( genbank == null ) {
                if ( externalDatabaseService != null ) {
                    genbank = externalDatabaseService.find( "Genbank" );
                } else {
                    result.setName( "Genbank" );
                    result.setType( DatabaseType.SEQUENCE );
                    genbank = result;
                }
            }
            result = genbank;
        } else if ( likelyExternalDatabaseIdentifier.equals( "ORF" ) ) {
            String organism = platform.getOrganisms().iterator().next();

            result.setType( DatabaseType.GENOME );

            if ( organismDatabases.containsKey( organism ) ) {
                result.setName( organismDatabases.get( organism ) );
            } else {
                // Placeholder
                result.setName( organism + " ORFs" );
                log.warn( "External database is " + result );
            }

        }
        if ( result == null || result.getName() == null ) {
            throw new IllegalStateException( "No external database was identified" );
        }
        return result;
    }

    /**
     * @param platform
     * @return
     */
    private String determinePlatformExternalReferenceIdentifier( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyExternalReference( string ) ) {
                log.debug( string + " appears to indicate the external reference identifier in column " + index
                        + " for platform " + platform );
                return string;
            }
            index++;
        }
        return null;
    }

    /**
     * @param platform
     * @return
     */
    private String determinePlatformIdentifier( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyId( string ) ) {
                log.debug( string + " appears to indicate the array element identifier in column " + index
                        + " for platform " + platform );
                return string;
            }
            index++;
        }
        return null;
    }

    /**
     * Turn a rough-cut dimension name into something of reasonable length.
     * 
     * @param dimensionName
     * @return
     */
    private String formatName( StringBuilder dimensionName ) {
        return dimensionName.length() > 100 ? dimensionName.toString().substring( 0, 100 ) : dimensionName.toString()
                + "...";
    }

    /**
     * @param geoDataset
     * @return
     */
    private Collection<GeoSample> getDatasetSamples( GeoDataset geoDataset ) {
        Collection<GeoSample> seriesSamples = getSeriesSamplesForDataset( geoDataset );

        // get just the samples used in this series
        Collection<GeoSample> datasetSamples = new ArrayList<GeoSample>();

        for ( GeoSample sample : seriesSamples ) {
            if ( geoDataset.getColumnNames().contains( sample.getGeoAccession() ) ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( "Dataset " + geoDataset + " includes sample " + sample + " on platform "
                            + sample.getPlatforms().iterator().next() );
                }
                datasetSamples.add( sample );
            }

            if ( log.isDebugEnabled() ) {
                log.debug( "Dataset " + geoDataset + " DOES NOT include sample " + sample + " on platform "
                        + sample.getPlatforms().iterator().next() );
            }
        }

        return datasetSamples;
    }

    /**
     * @param platform
     * @return
     */
    private String getDbIdentifierDescription( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyExternalReference( string ) ) {
                return platform.getColumnDescriptions().get( index );
            }
            index++;
        }
        return null;
    }

    private Collection<GeoSample> getSeriesSamplesForDataset( GeoDataset geoDataset ) {
        Collection<GeoSample> seriesSamples = null;
        Collection<GeoSeries> series = geoDataset.getSeries();

        // this is highly defensive programming prompted by a bug that caused the same series to be listed more than
        // once, but empty in one case.

        if ( series == null || series.size() == 0 ) {
            throw new IllegalStateException( "No series for " + geoDataset );
        }

        if ( series.size() > 1 ) {
            log.warn( "More than one series for a data set, probably some kind of parsing bug!" );
        }

        boolean found = false;
        for ( GeoSeries series2 : series ) {
            if ( series2.getSamples() != null && series2.getSamples().size() > 0 ) {
                if ( found == true ) {
                    throw new IllegalStateException( "More than one of the series for " + geoDataset + " has samples: "
                            + series2 );
                }
                seriesSamples = series2.getSamples();
                found = true;
            }
        }

        if ( seriesSamples == null || seriesSamples.size() == 0 ) {
            throw new IllegalStateException( "No series had samples for " + geoDataset );
        }

        return seriesSamples;
    }

    /**
     * Attempt to fill in the details of the quantitation type.
     * 
     * @param qt QuantitationType to fill in details for.
     * @param name of the quantitation type from the GEO sample column
     * @param description of the quantitation type from the GEO sample column
     */
    private void guessQuantitationTypeParameters( QuantitationType qt, String name, String description ) {

        GeneralType gType = GeneralType.QUANTITATIVE;
        ScaleType sType = ScaleType.UNSCALED;
        StandardQuantitationType qType = StandardQuantitationType.MEASUREDSIGNAL;
        Boolean isBackground = Boolean.FALSE;

        qt.setRepresentation( guessRepresentation( name, description ) );

        if ( name.contains( "Probe ID" ) || description.equalsIgnoreCase( "Probe Set ID" ) ) {
            /*
             * special case...not a quantitation type.
             */
            qType = StandardQuantitationType.OTHER;
            sType = ScaleType.UNSCALED;
            gType = GeneralType.CATEGORICAL;
        } else if ( name.matches( "CH[12][ABD]_(MEAN|MEDIAN)" ) ) {
            qType = StandardQuantitationType.DERIVEDSIGNAL;
            sType = ScaleType.LINEAR;
        } else if ( name.equals( "DET_P" ) || name.equals( "DETECTION P-VALUE" )
                || name.equalsIgnoreCase( "Detection_p-value" ) ) {
            qType = StandardQuantitationType.CONFIDENCEINDICATOR;
        } else if ( name.equals( "VALUE" ) ) {
            if ( description.toLowerCase().contains( "signal" ) || description.contains( "RMA" ) ) {
                qType = StandardQuantitationType.DERIVEDSIGNAL;
            }
        } else if ( name.matches( "ABS_CALL" ) ) {
            qType = StandardQuantitationType.PRESENTABSENT;
            sType = ScaleType.OTHER;
            gType = GeneralType.CATEGORICAL;
        }

        if ( description.toLowerCase().contains( "background" ) ) {
            qType = StandardQuantitationType.DERIVEDSIGNAL;
            isBackground = Boolean.TRUE;
        }

        if ( description.contains( "log2" ) ) {
            sType = ScaleType.LOG2;
        } else if ( description.contains( "log10" ) ) {
            sType = ScaleType.LOG10;
        } else if ( description.contains( "log" ) ) {
            sType = ScaleType.LOGBASEUNKNOWN; // though probably log 2.
        }

        if ( name.matches( "TOP" ) || name.matches( "LEFT" ) || name.matches( "RIGHT" ) || name.matches( "^BOT.*" ) ) {
            qType = StandardQuantitationType.COORDINATE;
        } else if ( name.matches( "^RAT[12]N?_(MEAN|MEDIAN)" ) ) {
            qType = StandardQuantitationType.RATIO;
        } else if ( name.matches( "fold_change" ) || description.contains( "log ratio" )
                || name.toLowerCase().contains( "ratio" ) || description.contains( "ratio" ) ) {
            qType = StandardQuantitationType.RATIO;
        }

        qt.setGeneralType( gType );
        qt.setScale( sType );
        qt.setType( qType );
        qt.setIsBackground( isBackground );

        if ( log.isInfoEnabled() ) {
            log.info( "Inferred that quantitation type \"" + name + "\" (Description: \"" + description
                    + "\") corresponds to: " + qType + ",  " + sType + ( qt.getIsBackground() ? " (Background) " : "" )
                    + " Encoding=" + qt.getRepresentation() );
        }
    }

    /**
     * @param name
     * @param description
     * @return
     */
    private PrimitiveType guessRepresentation( String name, String description ) {
        PrimitiveType pType = PrimitiveType.DOUBLE;
        if ( name.contains( "Probe ID" ) || description.equalsIgnoreCase( "Probe Set ID" ) ) {
            /*
             * special case...not a quantitation type.
             */
            pType = PrimitiveType.STRING;
        } else if ( name.matches( "ABS_CALL" ) ) {
            pType = PrimitiveType.STRING;
        }
        return pType;

    }

    /**
     * Deal with missing values, identified by nulls or number format exceptions.
     * 
     * @param toConvert
     * @param pt
     */
    private void handleMissing( List<Object> toConvert, PrimitiveType pt ) {
        if ( pt.equals( PrimitiveType.DOUBLE ) ) {
            toConvert.add( Double.NaN );
        } else if ( pt.equals( PrimitiveType.STRING ) ) {
            toConvert.add( "" );
        } else if ( pt.equals( PrimitiveType.INT ) ) {
            toConvert.add( 0 );
        } else if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
            toConvert.add( false );
        } else {
            throw new UnsupportedOperationException( "Data vectors of type " + pt + " not supported" );
        }
    }

    /**
     * 
     */
    private void initGeoExternalDatabase() {
        if ( geoDatabase == null ) {
            if ( externalDatabaseService != null ) {
                ExternalDatabase ed = externalDatabaseService.find( "GEO" );
                if ( ed != null ) {
                    geoDatabase = ed;
                }
            } else {
                geoDatabase = ExternalDatabase.Factory.newInstance();
                geoDatabase.setName( "GEO" );
                geoDatabase.setType( DatabaseType.EXPRESSION );
            }
        }
    }

    /**
     * Convert the by-sample data for a given quantitation type to by-designElement data vectors.
     * 
     * @param datasetSamples The samples we want to get data for.
     * @param quantitationTypeIndex The index of the quantitation type we want to examine. We used to do this by
     *        quantitationType name but too often these don't match up between samples. The value entered should be zero
     *        to access the first quantitation type column.
     * @return A map of Strings (design element names) to Lists of Strings containing the data.
     * @throws IllegalArgumentException if the columnNumber is not valid
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<Object>> makeDataVectors( List<GeoSample> datasetSamples, int quantitationTypeIndex ) {
        if ( quantitationTypeIndex < 0 ) {
            throw new IllegalArgumentException();
        }
        Map<String, List<Object>> dataVectors = new HashMap<String, List<Object>>( INITIAL_VECTOR_CAPACITY );
        Collections.sort( datasetSamples );
        for ( GeoSample sample : datasetSamples ) {
            Collection<GeoPlatform> platforms = sample.getPlatforms();
            assert platforms.size() != 0;
            if ( platforms.size() > 1 ) {
                throw new UnsupportedOperationException(
                        "Can't handle GEO sample ids associated with multiple platforms just yet" );
            }
            GeoPlatform platform = platforms.iterator().next();
            String identifier = determinePlatformIdentifier( platform );
            List<String> designElements = platform.getColumnData( identifier );
            for ( String designElementName : designElements ) {
                if ( !dataVectors.containsKey( designElementName ) ) {
                    dataVectors.put( designElementName, new ArrayList<Object>() );
                }
                Object datum = sample.getDatum( designElementName, quantitationTypeIndex );

                /*
                 * Note: null data can happen if the platform has probes that aren't in the data, or if this is a
                 * quantitation type that was filtered out during parsing.
                 */
                dataVectors.get( designElementName ).add( datum );
            }
        }

        /*
         * Check to see if we got any data. If not, we should return null. This can happen if the quantitation type was
         * filtered during parsing.
         */
        boolean filledIn = false;
        for ( List<Object> vector : dataVectors.values() ) {
            for ( Object object : vector ) {
                if ( object != null ) {
                    filledIn = true;
                    break;
                }
            }
            if ( filledIn == true ) {
                break;
            }
        }

        if ( !filledIn ) return null;

        return dataVectors;
    }

    /**
     * @param expExp ExpressionExperiment to be searched for matching BioAssays
     * @param bioAssayDimension BioAssayDimension to be added to
     * @param sampleAcc The GEO accession id for the sample. This is compared to the external accession recorded for the
     *        BioAssay
     * @return
     */
    private boolean matchSampleToBioAssay( ExpressionExperiment expExp, BioAssayDimension bioAssayDimension,
            String sampleAcc ) {

        for ( BioAssay bioAssay : expExp.getBioAssays() ) {
            if ( sampleAcc.equals( bioAssay.getAccession().getAccession() ) ) {
                bioAssayDimension.getBioAssays().add( bioAssay );
                log.debug( "Found sample match for bioAssay " + bioAssay.getAccession().getAccession() );
                return true;
            }
        }
        return false;
    }

    /**
     * Sanity check hopefully the first one is representative.
     * 
     * @param datasetSamples
     */
    private void sanityCheckQuantitationTypes( List<GeoSample> datasetSamples ) {
        List<String> reference = datasetSamples.iterator().next().getColumnNames();
        for ( GeoSample sample : datasetSamples ) {
            List<String> columnNames = sample.getColumnNames();
            if ( !reference.equals( columnNames ) && log.isWarnEnabled() ) {

                StringBuilder buf = new StringBuilder();
                buf.append( "\nSample " + sample.getGeoAccession() + ":" );
                for ( String string : columnNames ) {
                    buf.append( " " + string );
                }
                buf.append( "\nReference " + datasetSamples.iterator().next().getGeoAccession() + ":" );
                for ( String string : reference ) {
                    buf.append( " " + string );
                }

                log.warn( "*** Sample quantitation type names do not match: " + buf.toString() );
            }
        }
    }

    /**
     * @param externalDatabaseService the externalDatabaseService to set
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Map<String, Integer> tally = new HashMap<String, Integer>();
        for ( Object element : results ) {
            String clazz = element.getClass().getName();
            if ( !tally.containsKey( clazz ) ) {
                tally.put( clazz, new Integer( 0 ) );
            }
            tally.put( clazz, new Integer( ( tally.get( clazz ) ).intValue() + 1 ) );
        }
        for ( String clazz : tally.keySet() ) {
            buf.append( tally.get( clazz ) + " " + clazz + "s\n" );
        }

        return buf.toString();
    }
}