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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
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
import ubic.gemma.loader.expression.geo.model.GeoValues;
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
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.ontology.MgedOntologyService;

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

    /**
     * The scientific name used for rat species. FIXME this should be updated elsewhere; avoid this hardcoding.
     */
    private static final String RAT = "Rattus norvegicus";

    private static Map<String, String> organismDatabases = new HashMap<String, String>();

    static {
        organismDatabases.put( "Saccharomyces cerevisiae", "SGD" );
        organismDatabases.put( "Schizosaccharomyces pombe", "GeneDB" );
    }

    /**
     * Remove old results. Call this prior to starting conversion of a full dataset.
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
            treatment.setDescription( channel.getGrowthProtocol() );
            bioMaterial.getTreatments().add( treatment );
        }

        if ( !StringUtils.isBlank( channel.getTreatmentProtocol() ) ) {
            Treatment treatment = Treatment.Factory.newInstance();
            treatment.setName( sample.getGeoAccession() + " channel " + channel.getChannelNumber() + " growth" );
            treatment.setDescription( channel.getTreatmentProtocol() );
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
            treatment.setName( sample.getGeoAccession() + " channel " + channel.getChannelNumber() + " labeling" );
            treatment.setDescription( channel.getLabelProtocol() );
            bioMaterial.getTreatments().add( treatment );
        }

        for ( String characteristic : channel.getCharacteristics() ) {

            characteristic = trimString( characteristic );

            /*
             * Sometimes values are like Age:8 weeks, so we can try to convert them.
             */
            String[] fields = characteristic.split( ":" );
            String defaultDescription = "GEO Sample characteristic";
            if ( fields.length == 2 ) {

                String category = fields[0].trim();
                String value = fields[1].trim();

                try {
                    VocabCharacteristic gemmaChar = convertVariableType( GeoVariable.convertStringToType( category ) );
                    gemmaChar.setDescription( defaultDescription );
                    gemmaChar.setValue( value );
                    bioMaterial.getCharacteristics().add( gemmaChar );
                } catch ( Exception e ) {
                    // conversion didn't work, fall back.
                    Characteristic gemmaChar = Characteristic.Factory.newInstance();
                    gemmaChar.setValue( characteristic );
                    gemmaChar.setDescription( defaultDescription );
                    bioMaterial.getCharacteristics().add( gemmaChar );
                }

            } else {
                // no colon, just use raw (same as fallback above)
                Characteristic gemmaChar = Characteristic.Factory.newInstance();
                gemmaChar.setValue( characteristic );
                gemmaChar.setDescription( defaultDescription );
                bioMaterial.getCharacteristics().add( gemmaChar );
            }

        }

        if ( StringUtils.isNotBlank( channel.getSourceName() ) ) {
            VocabCharacteristic sourceChar = VocabCharacteristic.Factory.newInstance();
            sourceChar.setDescription( "GEO Sample source" );
            String characteristic = trimString( channel.getSourceName() );
            sourceChar.setCategory( "BioSource" );
            sourceChar.setCategoryUri( MgedOntologyService.MGED_ONTO_BASE_URL + "#BioSource" );
            sourceChar.setValue( characteristic );
            bioMaterial.getCharacteristics().add( sourceChar );
        }

        if ( StringUtils.isNotBlank( channel.getOrganism() ) ) {
            Taxon taxon = Taxon.Factory.newInstance();
            taxon.setScientificName( channel.getOrganism() );
            bioMaterial.setSourceTaxon( taxon );
        }

        if ( channel.getMolecule() != null ) {
            // this we can convert automatically pretty easily.
            Characteristic c = channel.getMoleculeAsCharacteristic();
            bioMaterial.getCharacteristics().add( c );
        }

        if ( StringUtils.isNotBlank( channel.getLabel() ) ) {
            String characteristic = trimString( channel.getLabel() );
            // This is typically something like "biotin-labeled nucleotides", which we can convert later.
            VocabCharacteristic labelChar = VocabCharacteristic.Factory.newInstance();
            labelChar.setDescription( "GEO Sample label" );
            labelChar.setCategory( "LabelCompound" );
            labelChar.setCategoryUri( MgedOntologyService.MGED_ONTO_BASE_URL + "#LabelCompound" );
            labelChar.setValue( characteristic );
            bioMaterial.getCharacteristics().add( labelChar );
        }

        return bioMaterial;
    }

    /**
     * @param characteristic
     * @return
     */
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
        int numMissing = 0;
        for ( Object rawValue : vector ) {
            if ( rawValue == null ) {
                numMissing++;
                handleMissing( toConvert, pt );
            } else if ( rawValue instanceof String ) { // needs to be coverted.
                try {
                    if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                        toConvert.add( Double.parseDouble( ( String ) rawValue ) );
                    } else if ( pt.equals( PrimitiveType.STRING ) ) {
                        toConvert.add( ( String ) rawValue );
                    } else if ( pt.equals( PrimitiveType.CHAR ) ) {
                        if ( ( ( String ) rawValue ).length() != 1 ) {
                            throw new IllegalStateException( "Attempt to cast a string of length "
                                    + ( ( String ) rawValue ).length() + " to a char: " + rawValue
                                    + "(quantitation type =" + qt );
                        }
                        toConvert.add( ( Character ) ( ( String ) rawValue ).toCharArray()[0] );
                    } else if ( pt.equals( PrimitiveType.INT ) ) {
                        toConvert.add( Integer.parseInt( ( String ) rawValue ) );
                    } else if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                        toConvert.add( Boolean.parseBoolean( ( String ) rawValue ) );
                    } else {
                        throw new UnsupportedOperationException( "Data vectors of type " + pt + " not supported" );
                    }
                } catch ( NumberFormatException e ) {
                    numMissing++;
                    handleMissing( toConvert, pt );
                } catch ( NullPointerException e ) {
                    numMissing++;
                    handleMissing( toConvert, pt );
                }
            } else { // use as is.
                toConvert.add( rawValue );
            }
        }

        if ( numMissing == vector.size() ) {
            return null;
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

        /*
         * First figure out of there are any samples for this data set. It could be that they were duplicates of ones
         * found in other series, so were skipped. See GeoDatasetService
         */
        if ( this.getDatasetSamples( geoDataset ).size() == 0 ) {
            log.info( "No samples remain for " + geoDataset + ", nothing to do" );
            return expExp;
        }

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

        convertDataSetDataVectors( geoDataset.getSeries().iterator().next().getValues(), geoDataset, expExp );

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
            List<GeoSample> samples = platformSamples.get( platform );
            log.info( samples.size() + " samples on " + platform );
            convertVectorsForPlatform( geoSeries.getValues(), expExp, samples, platform );
            geoSeries.getValues().clear( platform );
        }

    }

    /**
     * Convert the GEO data into DesignElementDataVectors associated with the ExpressionExperiment
     * 
     * @param geoDataset Source of the data
     * @param expExp ExpressionExperiment to fill in.
     */
    private void convertDataSetDataVectors( GeoValues values, GeoDataset geoDataset, ExpressionExperiment expExp ) {
        List<GeoSample> datasetSamples = new ArrayList<GeoSample>( getDatasetSamples( geoDataset ) );
        log.info( datasetSamples.size() + " samples in " + geoDataset );
        GeoPlatform geoPlatform = geoDataset.getPlatform();

        convertVectorsForPlatform( values, expExp, datasetSamples, geoPlatform );

        values.clear( geoPlatform );
    }

    /**
     * For data coming from a single platform, create vectors.
     * 
     * @param values A GeoValues object holding the parsed results.
     * @param expExp
     * @param datasetSamples
     * @param geoPlatform
     */
    private void convertVectorsForPlatform( GeoValues values, ExpressionExperiment expExp,
            List<GeoSample> datasetSamples, GeoPlatform geoPlatform ) {
        assert datasetSamples.size() > 0 : "No samples in dataset";

        log.info( "Converting vectors for " + geoPlatform.getGeoAccession() + ", " + datasetSamples.size()
                + " samples." );

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

        for ( String quantitationType : quantitationTypes ) {

            // skip the first quantitationType, it's the ID or ID_REF.
            if ( first ) {
                first = false;
                continue;
            }

            int columnAccordingToSample = quantitationTypes.indexOf( quantitationType );

            int quantitationTypeIndex = values.getQuantitationTypeIndex( geoPlatform, quantitationType );
            log.info( "Processing " + quantitationType + " (column=" + quantitationTypeIndex
                    + " - according to sample, it's " + columnAccordingToSample + ")" );

            Map<String, List<Object>> dataVectors = makeDataVectors( values, datasetSamples, quantitationTypeIndex );

            if ( dataVectors == null ) {
                // log.info( "No data for " + quantitationType + " (column=" + quantitationTypeIndex + ")" );
                continue;
            } else {
                // log.info( "Got " + dataVectors.size() + " data vectors for " + quantitationType + " (column="
                // + quantitationTypeIndex + ")" );
            }

            QuantitationType qt = QuantitationType.Factory.newInstance();
            qt.setName( quantitationType );
            String description = quantitationTypeDescriptions.get( columnAccordingToSample );
            qt.setDescription( description );
            QuantitationTypeParameterGuesser.guessQuantitationTypeParameters( qt, quantitationType, description );

            int count = 0;
            for ( String designElementName : dataVectors.keySet() ) {
                List<Object> dataVector = dataVectors.get( designElementName );
                if ( dataVector == null || dataVector.size() == 0 ) continue;

                DesignElementDataVector vector = convertDesignElementDataVector( geoPlatform, expExp,
                        bioAssayDimension, designElementName, dataVector, qt );

                if ( vector == null ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "Null vector for DE=" + designElementName + " QT=" + quantitationType );
                    continue;
                }

                if ( log.isTraceEnabled() ) {
                    log.trace( designElementName + " " + qt.getName() + " " + qt.getRepresentation() + " "
                            + dataVector.size() + " elements in vector" );
                }

                expExp.getDesignElementDataVectors().add( vector );

                if ( ++count % LOGGING_VECTOR_COUNT_UPDATE == 0 && log.isDebugEnabled() ) {
                    log.debug( count + " Data vectors added" );
                }
            }

            if ( count > 0 ) {
                expExp.getQuantitationTypes().add( qt );
            }

            if ( log.isInfoEnabled() ) {
                log.info( count + " Data vectors added for '" + quantitationType + "'" );
            }
        }
        log.info( "Total of " + expExp.getDesignElementDataVectors().size() + " vectors so far..."
                + expExp.getQuantitationTypes().size() + " quantitation types." );
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
     * @param dataVector to convert.
     * @return vector, or null if the dataVector was null or empty.
     */
    private DesignElementDataVector convertDesignElementDataVector( GeoPlatform geoPlatform,
            ExpressionExperiment expExp, BioAssayDimension bioAssayDimension, String designElementName,
            List<Object> dataVector, QuantitationType qt ) {

        if ( dataVector == null || dataVector.size() == 0 ) return null;

        byte[] blob = convertData( dataVector, qt );
        if ( blob == null ) { // all missing etc.
            if ( log.isDebugEnabled() ) log.debug( "All missing values for DE=" + designElementName + " QT=" + qt );
            return null;
        }
        if ( log.isDebugEnabled() ) {
            log.debug( blob.length + " bytes for " + dataVector.size() + " raw elements" );
        }

        ArrayDesign p = convertPlatform( geoPlatform );
        assert p != null;

        Map<String, CompositeSequence> designMap = platformDesignElementMap.get( p.getShortName() );
        assert designMap != null;

        CompositeSequence compositeSequence = designMap.get( designElementName );

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

        if ( isSage( platform ) ) {
            throw new UnsupportedOperationException( "This data set uses SAGE, it cannot be handled yet" );
        }

        log.info( "Converting platform: " + platform.getGeoAccession() );
        ArrayDesign arrayDesign = createMinimalArrayDesign( platform );

        platformDesignElementMap.put( arrayDesign.getShortName(), new HashMap<String, CompositeSequence>() );

        Taxon taxon = convertPlatformOrganism( platform );

        // convert the design element information.
        String identifier = platform.getIdColumnName();
        if ( identifier == null ) {
            throw new IllegalStateException( "Cannot determine the platform design element id column." );
        }

        Collection<String> externalReferences = determinePlatformExternalReferenceIdentifier( platform );
        String descriptionColumn = determinePlatformDescriptionColumn( platform );
        String sequenceColumn = determinePlatformSequenceColumn( platform );
        ExternalDatabase externalDb = determinePlatformExternalDatabase( platform );

        List<String> identifiers = platform.getColumnData( identifier );
        List<String> descriptions = platform.getColumnData( descriptionColumn );

        List<String> sequences = null;
        if ( sequenceColumn != null ) {
            sequences = platform.getColumnData( sequenceColumn );
        }

        /*
         * This is a very commonly found column name in files, it seems standard in GEO. If we don't find it, it's okay.
         */
        List<String> cloneIdentifiers = platform.getColumnData( "CLONE_ID" );
        assert cloneIdentifiers == null || cloneIdentifiers.size() == identifiers.size();

        List<List<String>> externalRefs = null;
        if ( externalReferences != null ) {
            externalRefs = platform.getColumnData( externalReferences );
        }

        if ( externalRefs != null ) {
            assert externalRefs.iterator().next().size() == identifiers.size() : "Unequal numbers of identifiers and external references! "
                    + externalRefs.iterator().next().size() + " != " + identifiers.size();
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "Converting " + identifiers.size() + " probe identifiers on GEO platform "
                    + platform.getGeoAccession() );
        }

        Iterator<String> descIter = null;

        if ( descriptions != null ) {
            descIter = descriptions.iterator();
        }

        Collection compositeSequences = new ArrayList( 5000 );
        int i = 0; // to get sequences, if we have them, and clone identifiers.
        for ( String id : identifiers ) {
            String externalAccession = null;
            if ( externalRefs != null ) {
                externalAccession = getExternalAccession( externalRefs, i );
            }

            String cloneIdentifier = cloneIdentifiers == null ? null : cloneIdentifiers.get( i );

            String description = "";
            if ( externalAccession != null ) {
                String[] refs = externalAccession.split( "," );
                if ( refs.length > 1 ) {
                    description = "Multiple external sequence references: " + externalAccession + "; ";
                    externalAccession = refs[0];
                }
            }

            if ( descIter != null ) description = description + " " + descIter.next();

            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( id );
            cs.setDescription( description );
            cs.setArrayDesign( arrayDesign );

            BioSequence bs = createMinimalBioSequence( taxon );

            boolean isRefseq = false;
            // ExternalDB will be null if it's IMAGE (this is really pretty messy, sorry)
            if ( externalDb != null && externalDb.getName().equals( "Genbank" )
                    && StringUtils.isNotBlank( externalAccession ) ) {
                // http://www.ncbi.nlm.nih.gov/RefSeq/key.html#accessions : "RefSeq accession numbers can be
                // distinguished from GenBank accessions by their prefix distinct format of [2 characters|underbar]"
                isRefseq = externalAccession.matches( "^[A-Z]{2}_" );
            }

            boolean isImage = false;
            if ( StringUtils.isNotBlank( cloneIdentifier ) ) {
                bs.setName( cloneIdentifier );
                isImage = cloneIdentifier.startsWith( "IMAGE" );
            }

            /*
             * If we are given a sequence, we don't need the genbank identifier, which is probably not correct anyway.
             */
            if ( sequences != null && StringUtils.isNotBlank( sequences.get( i ) ) ) {
                bs.setSequence( sequences.get( i ) );
                bs.setIsApproximateLength( false );
                bs.setLength( new Long( bs.getSequence().length() ) );
                bs.setType( SequenceType.DNA );
                bs.setName( id + "_sequence" );
                bs.setDescription( "Sequence provided by manufacturer. "
                        + ( externalAccession != null ? "Used in leiu of " + externalAccession
                                : "No external accession provided" ) );
            } else if ( externalAccession != null && !isRefseq && !isImage && externalDb != null ) {
                /*
                 * We don't use this if we have an IMAGE clone because the accession might be wrong (e.g., for a
                 * Refseq). During persisting the IMAGE clone will be replaced with the 'real' thing.
                 */

                /*
                 * We also don't store them if they are refseq ids, because refseq ids are not actually put on arrays.
                 */

                DatabaseEntry dbe = createDatabaseEntry( externalDb, externalAccession, bs );
                bs.setSequenceDatabaseEntry( dbe );
            }

            /*
             * If we have no basis for describing the sequence, we have to skip it.
             */
            if ( StringUtils.isBlank( externalAccession ) && StringUtils.isBlank( cloneIdentifier ) ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( "Blank external reference and clone id for " + cs + " on " + arrayDesign
                            + ", no biological characteristic can be added." );
                }
            } else {
                cs.setBiologicalCharacteristic( bs );
            }

            compositeSequences.add( cs );

            platformDesignElementMap.get( arrayDesign.getShortName() ).put( id, cs );
            i++;
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
    private ArrayDesign createMinimalArrayDesign( GeoPlatform platform ) {
        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( platform.getTitle() );
        arrayDesign.setShortName( platform.getGeoAccession() );
        arrayDesign.setDescription( platform.getDescriptions() );
        PlatformType technology = platform.getTechnology();
        if ( technology == PlatformType.dualChannel || technology == PlatformType.dualChannelGenomic
                || technology == PlatformType.spottedOligonucleotide || technology == PlatformType.spottedDNAOrcDNA ) {
            arrayDesign.setTechnologyType( TechnologyType.TWOCOLOR );
        } else if ( technology == PlatformType.singleChannel || technology == PlatformType.oligonucleotideBeads
                || technology == PlatformType.inSituOligonucleotide ) {
            arrayDesign.setTechnologyType( TechnologyType.ONECOLOR );
        } else {
            throw new IllegalArgumentException( "Don't know how to interpret technology type " + technology );
        }
        return arrayDesign;
    }

    /**
     * Is this a SAGE (Serial Analysis of Gene Expression) platform? (A non-array method)
     * 
     * @param platform
     * @return
     */
    private boolean isSage( GeoPlatform platform ) {
        return platform.getTechnology() == PlatformType.SAGE || platform.getTechnology() == PlatformType.SAGENlaIII
                || platform.getTechnology() == PlatformType.SAGERsaI
                || platform.getTechnology() == PlatformType.SAGESau3A;
    }

    private BioSequence createMinimalBioSequence( Taxon taxon ) {
        BioSequence bs = BioSequence.Factory.newInstance();
        bs.setTaxon( taxon );
        bs.setPolymerType( PolymerType.DNA );
        bs.setType( SequenceType.DNA );
        return bs;
    }

    private DatabaseEntry createDatabaseEntry( ExternalDatabase externalDb, String externalRef, BioSequence bs ) {
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
        return dbe;
    }

    private String getExternalAccession( List<List<String>> externalRefs, int i ) {
        for ( List<String> refs : externalRefs ) {
            if ( StringUtils.isNotBlank( refs.get( i ) ) ) {
                return refs.get( i );
            }
        }
        return null;
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
    private VocabCharacteristic convertReplicatationType( ReplicationType repType ) {
        VocabCharacteristic result = VocabCharacteristic.Factory.newInstance();
        result.setCategory( "ReplicateDescriptionType" );
        result.setCategoryUri( MgedOntologyService.MGED_ONTO_BASE_URL + "ReplicateDescriptionType" );
        ExternalDatabase mged = ExternalDatabase.Factory.newInstance();

        if ( !repType.equals( VariableType.other ) ) {
            mged.setName( "MGED Ontology" );
            mged.setType( DatabaseType.ONTOLOGY );
        }

        if ( repType.equals( ReplicationType.biologicalReplicate ) ) {
            result.setValue( "biological_replicate" );
            result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "biological_replicate" );
        } else if ( repType.equals( ReplicationType.technicalReplicateExtract ) ) {
            result.setValue( "technical_replicate" );
            result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "technical_replicate" );
        } else if ( repType.equals( ReplicationType.technicalReplicateLabeledExtract ) ) {
            result.setValue( "technical_replicate" );
            result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "technical_replicate" ); // MGED doesn't have
            // a
            // term to distinguish
            // these.
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
        VocabCharacteristic term = convertReplicatationType( replication.getType() );

        result.setCategory( term );
        return result;

    }

    /**
     * @param replication
     * @return
     */
    private FactorValue convertReplicationToFactorValue( GeoReplication replication ) {
        FactorValue factorValue = FactorValue.Factory.newInstance();
        VocabCharacteristic term = convertReplicatationType( replication.getType() );
        factorValue.getCharacteristics().add( term );
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
    private BioAssay convertSample( GeoSample sample, BioMaterial bioMaterial, ExperimentalDesign experimentalDesign ) {
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
         * DataSet records" If we would get that information we would pass it into this method as
         * expExp.getExperimentalDesign().getExperimentalFactors().
         */

        // : use the ones from the ExperimentalFactor. In other words, these factor values should correspond to
        // experimentalfactors
        Collection<ExperimentalFactor> experimentalFactors = experimentalDesign.getExperimentalFactors();
        for ( GeoReplication replication : sample.getReplicates() ) {
            matchSampleReplicationToExperimentalFactorValue( bioMaterial, experimentalFactors, replication );
        }

        // : use the ones from the ExperimentalFactor.
        for ( GeoVariable variable : sample.getVariables() ) {
            matchSampleVariableToExperimentalFactorValue( bioMaterial, experimentalFactors, variable );
        }

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
     * @param bioMaterial
     * @param experimentalFactors
     * @param variable
     */
    private void matchSampleVariableToExperimentalFactorValue( BioMaterial bioMaterial,
            Collection<ExperimentalFactor> experimentalFactors, GeoVariable variable ) {
        // find the experimentalFactor that matches this.
        FactorValue convertVariableToFactorValue = convertVariableToFactorValue( variable );
        FactorValue matchingFactorValue = findMatchingExperimentalFactorValue( experimentalFactors,
                convertVariableToFactorValue );

        if ( matchingFactorValue != null ) {
            bioMaterial.getFactorValues().add( matchingFactorValue );
        } else {
            throw new IllegalStateException( "Could not find matching factor value for " + variable
                    + " in experimental design for sample " + bioMaterial );
        }
    }

    /**
     * @param experimentalFactors
     * @param convertVariableToFactorValue
     * @return
     */
    private FactorValue findMatchingExperimentalFactorValue( Collection<ExperimentalFactor> experimentalFactors,
            FactorValue convertVariableToFactorValue ) {
        Collection<Characteristic> characteristics = convertVariableToFactorValue.getCharacteristics();
        if ( characteristics.size() > 1 )
            throw new UnsupportedOperationException(
                    "Can't handle factor values with multiple characteristics in GEO conversion" );
        Characteristic c = characteristics.iterator().next();

        FactorValue matchingFactorValue = null;
        factors: for ( ExperimentalFactor factor : experimentalFactors ) {
            for ( FactorValue fv : factor.getFactorValues() ) {
                for ( Characteristic m : fv.getCharacteristics() ) {
                    if ( m.getCategory().equals( c.getCategory() ) && m.getValue().equals( c.getValue() ) ) {
                        matchingFactorValue = fv;
                        break factors;
                    }

                }
            }
        }
        return matchingFactorValue;
    }

    /**
     * @param bioMaterial
     * @param experimentalFactors
     * @param variable
     */
    private void matchSampleReplicationToExperimentalFactorValue( BioMaterial bioMaterial,
            Collection<ExperimentalFactor> experimentalFactors, GeoReplication replication ) {
        // find the experimentalFactor that matches this.
        FactorValue convertVariableToFactorValue = convertReplicationToFactorValue( replication );
        FactorValue matchingFactorValue = findMatchingExperimentalFactorValue( experimentalFactors,
                convertVariableToFactorValue );
        if ( matchingFactorValue != null ) {
            bioMaterial.getFactorValues().add( matchingFactorValue );
        } else {
            throw new IllegalStateException( "Could not find matching factor value for " + replication
                    + " in experimental design for sample " + bioMaterial );
        }
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

        Map<String, Collection<GeoData>> organismDatasetMap = getOrganismDatasetMap( series );

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
            Map<String, Collection<GeoData>> organismDatasetMap, int i, String organism ) {
        GeoSeries speciesSpecific = new GeoSeries();

        Collection<GeoData> datasets = organismDatasetMap.get( organism );
        assert datasets.size() > 0;

        for ( GeoSample sample : series.getSamples() ) {
            // ugly, we have to assume there is only one platform and one organism...
            if ( sample.getPlatforms().iterator().next().getOrganisms().iterator().next().equals( organism ) ) {
                speciesSpecific.addSample( sample );
            }
        }

        // strip out samples that aren't from this organism.

        for ( GeoData dataset : datasets ) {
            if ( dataset instanceof GeoDataset ) {
                ( ( GeoDataset ) dataset ).dissociateFromSeries( series );
                speciesSpecific.addDataSet( ( GeoDataset ) dataset );
            }
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
        speciesSpecific.setValues( series.getValues() );

        converted.add( convertSeries( speciesSpecific, null ) );
    }

    /**
     * @param series
     * @return map of organisms to a collection of either datasets or platforms.
     */
    private Map<String, Collection<GeoData>> getOrganismDatasetMap( GeoSeries series ) {
        Map<String, Collection<GeoData>> organisms = new HashMap<String, Collection<GeoData>>();

        if ( series.getDatasets() == null || series.getDatasets().size() == 0 ) {
            for ( GeoSample sample : series.getSamples() ) {
                assert sample.getPlatforms().size() > 0 : sample + " has no platform";
                assert sample.getPlatforms().size() == 1 : sample + " has multiple platforms: "
                        + sample.getPlatforms().toArray();
                String organism = sample.getPlatforms().iterator().next().getOrganisms().iterator().next();
                if ( organisms.get( organism ) == null ) {
                    organisms.put( organism, new HashSet<GeoData>() );
                }
                organisms.get( organism ).add( sample.getPlatforms().iterator().next() );
            }
        } else {
            for ( GeoDataset dataset : series.getDatasets() ) {
                String organism = dataset.getOrganism();
                if ( organisms.get( organism ) == null ) {
                    organisms.put( organism, new HashSet<GeoData>() );
                }
                organisms.get( organism ).add( dataset );
            }
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

        expExp.setDescription( series.getSummaries() + ( series.getSummaries().endsWith( "\n" ) ? "" : "\n" ) );
        if ( series.getLastUpdateDate() != null ) {
            expExp.setDescription( expExp.getDescription() + "Last Updated (by provider): "
                    + series.getLastUpdateDate() + "\n" );
        }

        expExp.setName( series.getTitle() );
        expExp.setShortName( series.getGeoAccession() );

        convertContacts( series, expExp );

        convertPubMedIds( series, expExp );

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
                Characteristic o = Characteristic.Factory.newInstance();
                o.setDescription( "GEO Keyword" );
                o.setValue( keyWord );
                o.setDescription( "Keyword from GEO series definition file." );
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
        log.info( "Series has " + series.getSamples().size() + " samples" );
        if ( samplesToSkip.size() > 0 ) {
            log.info( samplesToSkip.size() + " samples will be skipped" );
        }
        expExp.setBioAssays( new HashSet() );

        if ( series.getSampleCorrespondence().size() == 0 ) {
            throw new IllegalArgumentException( "No sample correspondence!" );
        }

        // spits out a big summary of the correspondence.
        if ( log.isDebugEnabled() ) log.debug( series.getSampleCorrespondence() );
        int numBioMaterials = 0;

        /*
         * For each _set_ of "corresponding" samples (from the same RNA, or so we think) we make up a new BioMaterial.
         */

        Collection<String> seen = new HashSet<String>();
        for ( Iterator iter = series.getSampleCorrespondence().iterator(); iter.hasNext(); ) {

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            String bioMaterialName = getBiomaterialPrefix( series, ++numBioMaterials );
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

                        if ( seen.contains( accession ) ) {
                            log.error( "Got " + accession + " twice, this time in set " + correspondingSamples );
                        }
                        seen.add( accession );

                        BioAssay ba = convertSample( sample, bioMaterial, expExp.getExperimentalDesign() );

                        LocalFile rawDataFile = convertSupplementaryFileToLocalFile( sample );
                        ba.setRawDataFile( rawDataFile );// deal with null at UI

                        ba.setDescription( ba.getDescription() + "\nSource GEO sample is " + sample.getGeoAccession()
                                + "\nLast updated (according to GEO): " + sample.getLastUpdateDate() );
                        ba.getSamplesUsed().add( bioMaterial );
                        bioMaterial.getBioAssaysUsedIn().add( ba );
                        bioMaterialDescription = bioMaterialDescription + " " + sample;
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
            }
            bioMaterial.setName( bioMaterialName );
            bioMaterial.setDescription( bioMaterialDescription );
        }

        log.info( "Expression Experiment from " + series + " has " + expExp.getBioAssays().size() + " bioassays and "
                + numBioMaterials + " biomaterials." );

        int expectedNumSamples = series.getSamples().size() - samplesToSkip.size();
        int actualNumSamples = expExp.getBioAssays().size();
        if ( expectedNumSamples > actualNumSamples ) {
            log
                    .warn( ( expectedNumSamples - actualNumSamples )
                            + " samples were not in the 'sample correspondence'"
                            + " and have been omitted. Possibly they were in the Series (GSE) but not in the corresponding Dataset (GDS)?" );
        }

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
     * @param series
     * @param expExp
     */
    private void convertPubMedIds( GeoSeries series, ExpressionExperiment expExp ) {
        Collection<String> ids = series.getPubmedIds();
        if ( ids == null || ids.size() == 0 ) return;
        for ( String string : ids ) {
            BibliographicReference bibRef = BibliographicReference.Factory.newInstance();
            DatabaseEntry pubAccession = DatabaseEntry.Factory.newInstance();
            pubAccession.setAccession( string );
            ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
            ed.setName( "PubMed" );
            pubAccession.setExternalDatabase( ed );
            bibRef.setPubAccession( pubAccession );
            expExp.setPrimaryPublication( bibRef );
            break; // usually just one...
        }
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
                log.info( "Data from " + dataset + " is of type " + dataset.getExperimentType() + ", "
                        + getDatasetSamples( dataset ).size() + " samples." );
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
            String file = series.getSupplementaryFile();
            if ( !StringUtils.isEmpty( file ) && !StringUtils.equalsIgnoreCase( file, "NONE" ) ) {
                try {
                    remoteFile = LocalFile.Factory.newInstance();
                    remoteFileUrl = new URL( file );
                } catch ( MalformedURLException e ) {
                    reportUrlError( remoteFileUrl, e );
                }
            }
        }

        else if ( object instanceof GeoSample ) {
            GeoSample sample = ( GeoSample ) object;
            String file = sample.getSupplementaryFile();
            if ( !StringUtils.isEmpty( file ) && !StringUtils.equalsIgnoreCase( file, "NONE" ) ) {
                try {
                    remoteFile = LocalFile.Factory.newInstance();
                    remoteFileUrl = new URL( file );
                } catch ( MalformedURLException e ) {
                    reportUrlError( remoteFileUrl, e );
                }
            }
        }

        else if ( object instanceof GeoPlatform ) {
            GeoPlatform platform = ( GeoPlatform ) object;
            String file = platform.getSupplementaryFile();
            if ( !StringUtils.isEmpty( file ) && !StringUtils.equalsIgnoreCase( file, "NONE" ) ) {
                try {
                    remoteFile = LocalFile.Factory.newInstance();
                    remoteFileUrl = new URL( file );
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
     * @param series
     * @param i
     * @return
     */
    private String getBiomaterialPrefix( GeoSeries series, int i ) {
        String bioMaterialName = series.getGeoAccession() + "_bioMaterial_" + i;
        return bioMaterialName;
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
        Characteristic term = convertVariableType( geoSubSet.getType() );
        term.setDescription( "Converted from GEO subset " + geoSubSet.getGeoAccession() );
        term.setValue( term.getCategory() );
        if ( term instanceof VocabCharacteristic ) {
            ( ( VocabCharacteristic ) term ).setValueUri( ( ( VocabCharacteristic ) term ).getCategoryUri() );
        }
        experimentalFactor.setCategory( term );
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

    /**
     * @param geoSubSet
     * @param experimentalFactor
     * @return
     */
    private FactorValue convertSubsetDescriptionToFactorValue( GeoSubset geoSubSet,
            ExperimentalFactor experimentalFactor ) {
        // By definition each subset defines a new factor value.
        FactorValue factorValue = FactorValue.Factory.newInstance();
        Characteristic term = convertVariableType( geoSubSet.getType() );
        term.setValue( geoSubSet.getDescription() );
        term.setDescription( "Converted from GEO subset " + geoSubSet.getGeoAccession() );
        factorValue.getCharacteristics().add( term );
        factorValue.setExperimentalFactor( experimentalFactor );
        return factorValue;
    }

    /**
     * @param expExp
     * @param geoSubSet
     * @param factorValue
     */
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
                        if ( log.isDebugEnabled() ) {
                            log.debug( "Adding " + factorValue.getExperimentalFactor() + " : " + factorValue + " to "
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
            if ( log.isDebugEnabled() ) log.debug( "Converting subset to experimentalFactor" + subset.getType() );
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
        Characteristic term = convertVariableType( variable.getType() );
        result.setCategory( term );
        return result;
    }

    /**
     * @param variable
     * @return Category will be filled in with a URI but value will just be plain text.
     */
    private FactorValue convertVariableToFactorValue( GeoVariable variable ) {
        log.info( "Converting variable " + variable );
        VariableType type = variable.getType();
        FactorValue factorValue = convertTypeToFactorValue( type, variable.getDescription() );
        return factorValue;
    }

    /**
     * @param variable
     * @param type
     * @return
     */
    private FactorValue convertTypeToFactorValue( VariableType type, String value ) {
        FactorValue factorValue = FactorValue.Factory.newInstance();
        Characteristic term = convertVariableType( type );
        term.setValue( value ); // TODO map onto an ontology.
        factorValue.getCharacteristics().add( term );
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
     * @return a VocabCharacteristic with the category URI and category filled in.
     */
    private VocabCharacteristic convertVariableType( VariableType varType ) {

        String mgedTerm = null;
        if ( varType.equals( VariableType.age ) ) {
            mgedTerm = "Age";
        } else if ( varType.equals( VariableType.agent ) ) {
            mgedTerm = "Agent";
        } else if ( varType.equals( VariableType.cellLine ) ) {
            mgedTerm = "CellLine";
        } else if ( varType.equals( VariableType.cellType ) ) {
            mgedTerm = "CellType";
        } else if ( varType.equals( VariableType.developmentStage ) ) {
            mgedTerm = "DevelopmentalStage";
        } else if ( varType.equals( VariableType.diseaseState ) ) {
            mgedTerm = "DiseaseState";
        } else if ( varType.equals( VariableType.dose ) ) {
            mgedTerm = "Dose";
        } else if ( varType.equals( VariableType.gender ) ) {
            mgedTerm = "Sex";
        } else if ( varType.equals( VariableType.genotypeOrVariation ) ) {
            mgedTerm = "IndividualGeneticCharacteristics";
        } else if ( varType.equals( VariableType.growthProtocol ) ) {
            mgedTerm = "GrowthCondition";
        } else if ( varType.equals( VariableType.individual ) ) {
            mgedTerm = "Individiual";
        } else if ( varType.equals( VariableType.infection ) ) {
            mgedTerm = "Phenotype";
        } else if ( varType.equals( VariableType.isolate ) ) {
            mgedTerm = "Age";
        } else if ( varType.equals( VariableType.metabolism ) ) {
            mgedTerm = "Metabolism";
        } else if ( varType.equals( VariableType.other ) ) {
            mgedTerm = "Other";
        } else if ( varType.equals( VariableType.protocol ) ) {
            mgedTerm = "Protocol";
        } else if ( varType.equals( VariableType.shock ) ) {
            mgedTerm = "EnvironmentalStress";
        } else if ( varType.equals( VariableType.species ) ) {
            mgedTerm = "Organism";
        } else if ( varType.equals( VariableType.specimen ) ) {
            mgedTerm = "BioSample";
        } else if ( varType.equals( VariableType.strain ) ) {
            mgedTerm = "StrainOrLine";
        } else if ( varType.equals( VariableType.stress ) ) {
            mgedTerm = "EnvironmentalStress";
        } else if ( varType.equals( VariableType.temperature ) ) {
            mgedTerm = "Temperature";
        } else if ( varType.equals( VariableType.time ) ) {
            mgedTerm = "Time";
        } else if ( varType.equals( VariableType.tissue ) ) {
            mgedTerm = "OrganismPart";
        } else {
            throw new IllegalStateException();
        }

        log.debug( "Category term: " + mgedTerm + " " );
        return setCategory( mgedTerm );

    }

    /**
     * @param mgedTerm
     * @return
     */
    private VocabCharacteristic setCategory( String mgedTerm ) {
        VocabCharacteristic categoryTerm = VocabCharacteristic.Factory.newInstance();
        categoryTerm.setCategory( mgedTerm );
        categoryTerm.setCategoryUri( MgedOntologyService.MGED_ONTO_BASE_URL + mgedTerm );
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
        log.debug( "No platform element description column found for " + platform );
        return null;
    }

    /**
     * @param platform
     * @return
     */
    private String determinePlatformSequenceColumn( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelySequence( string ) ) {
                log.info( string + " appears to indicate the  probe descriptions in column " + index + " for platform "
                        + platform );
                return string;
            }
            index++;
        }
        log.debug( "No platform sequence description column found for " + platform );
        return null;
    }

    /**
     * @param platform
     * @return
     */
    private ExternalDatabase determinePlatformExternalDatabase( GeoPlatform platform ) {
        ExternalDatabase result = ExternalDatabase.Factory.newInstance();

        Collection<String> likelyExternalDatabaseIdentifiers = determinePlatformExternalReferenceIdentifier( platform );
        String dbIdentifierDescription = getDbIdentifierDescription( platform );

        String url = null;
        if ( dbIdentifierDescription == null ) {
            return null;
        } else if ( dbIdentifierDescription.indexOf( "LINK_PRE:" ) >= 0 ) {
            // example: #ORF = ORF reference LINK_PRE:"http://genome-www4.stanford.edu/cgi-bin/SGD/locus.pl?locus="
            url = dbIdentifierDescription.substring( dbIdentifierDescription.indexOf( "LINK_PRE:" ) );
            result.setWebUri( url );
        }

        if ( likelyExternalDatabaseIdentifiers == null || likelyExternalDatabaseIdentifiers.size() == 0 ) {
            throw new IllegalStateException( "No external database identifier column was identified" );
        }

        String likelyExternalDatabaseIdentifier = likelyExternalDatabaseIdentifiers.iterator().next();
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

            // } else if ( likelyExternalDatabaseIdentifier.equals( "CLONE_ID" ) ) {
            // String sample = platform.getColumnData( "CLONE_ID" ).iterator().next();
            // if ( sample.startsWith( "IMAGE" ) ) {
            // result.setType( DatabaseType.SEQUENCE );
            // result.setName( "IMAGE" );
            // } else {
            // throw new IllegalStateException( "No external database was identified, but had CLONE_ID" );
            // }
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
    private Collection<String> determinePlatformExternalReferenceIdentifier( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        Collection<String> matches = new HashSet<String>();
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyExternalReference( string ) ) {
                log.debug( string + " appears to indicate a possible external reference identifier in column " + index
                        + " for platform " + platform );
                matches.add( string );

            }
            index++;
        }

        if ( matches.size() == 0 ) {
            return null;
        }
        return matches;

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

        // get just the samples used in this dataset
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
            throw new UnsupportedOperationException( "Missing values in data vectors of type " + pt + " not supported" );
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
     * @param datasetSamples The samples we want to get data for. These should all have been run on the same platform.
     * @param quantitationTypeIndex - first index is 0
     * @return A map of Strings (design element names) to Lists of Strings containing the data.
     * @throws IllegalArgumentException if the columnNumber is not valid
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<Object>> makeDataVectors( GeoValues values, List<GeoSample> datasetSamples,
            Integer quantitationTypeIndex ) {
        Map<String, List<Object>> dataVectors = new HashMap<String, List<Object>>( INITIAL_VECTOR_CAPACITY );
        Collections.sort( datasetSamples );
        GeoPlatform platform = getPlatformForSamples( datasetSamples );

        // log.info(values.toString());

        // the locations of the data we need in the target vectors (mostly reordering)
        Integer[] indices = values.getIndices( platform, datasetSamples, quantitationTypeIndex );

        if ( indices == null || indices.length == 0 ) return null; // can happen if quantitation type was filtered out.

        assert indices.length == datasetSamples.size();

        String identifier = platform.getIdColumnName();
        List<String> designElements = platform.getColumnData( identifier );
        for ( String designElementName : designElements ) {
            /*
             * Note: null data can happen if the platform has probes that aren't in the data, or if this is a
             * quantitation type that was filtered out during parsing, or absent from some samples.
             */
            List ob = values.getValues( platform, quantitationTypeIndex, designElementName, indices );
            if ( ob == null || ob.size() == 0 ) continue;
            assert ob.size() == datasetSamples.size();
            dataVectors.put( designElementName, ob );
        }

        boolean filledIn = isPopulated( dataVectors );

        values.clear( platform, datasetSamples, quantitationTypeIndex );

        if ( !filledIn ) return null;

        return dataVectors;
    }

    /**
     * Check to see if we got any data. If not, we should return null. This can happen if the quantitation type was
     * filtered during parsing.
     */
    private boolean isPopulated( Map<String, List<Object>> dataVectors ) {
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
        return filledIn;
    }

    /**
     * Assumes that all samples have the same platform. If not, throws an exception.
     * 
     * @param datasetSamples
     * @return
     */
    private GeoPlatform getPlatformForSamples( List<GeoSample> datasetSamples ) {
        GeoPlatform platform = null;
        for ( GeoSample sample : datasetSamples ) {
            Collection<GeoPlatform> platforms = sample.getPlatforms();
            assert platforms.size() != 0;
            if ( platforms.size() > 1 ) {
                throw new UnsupportedOperationException(
                        "Can't handle GEO sample ids associated with multiple platforms just yet" );
            }
            GeoPlatform nextPlatform = platforms.iterator().next();
            if ( platform == null )
                platform = nextPlatform;
            else if ( !platform.equals( nextPlatform ) )
                throw new IllegalArgumentException( "All samples here must use the same platform" );
        }
        return platform;
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
        boolean someDidntMatch = false;
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
                someDidntMatch = true;

                log.debug( "*** Sample quantitation type names do not match: " + buf.toString() );
            }
        }
        if ( someDidntMatch ) {
            log.warn( "Samples do not have consistent quantification type names" );
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