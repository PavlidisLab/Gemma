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

import baseCode.io.ByteArrayConverter;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioMaterialDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
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
import ubic.gemma.loader.expression.geo.model.GeoDataset.PlatformType;
import ubic.gemma.loader.expression.geo.model.GeoReplication.ReplicationType;
import ubic.gemma.loader.expression.geo.model.GeoVariable.VariableType;
import ubic.gemma.loader.expression.geo.util.GeoConstants;
import ubic.gemma.loader.util.converter.Converter;

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
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoConverter implements Converter {

    /**
     * How often we tell the user about data processing (items per update)
     */
    private static final int LOGGING_VECTOR_COUNT_UPDATE = 2000;

    private static Log log = LogFactory.getLog( GeoConverter.class.getName() );

    /**
     * Initial guess at how many designelementdatavectors to allocate space for.
     */
    private static final int INITIAL_VECTOR_CAPACITY = 10000;

    private ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    private ExternalDatabase geoDatabase;

    private Map<String, Map<String, CompositeSequence>> platformDesignElementMap = new HashMap<String, Map<String, CompositeSequence>>();

    private Collection<Object> results = new HashSet<Object>();

    private Map<String, ArrayDesign> seenPlatforms = new HashMap<String, ArrayDesign>();

    public GeoConverter() {
        geoDatabase = ExternalDatabase.Factory.newInstance();
        geoDatabase.setName( "GEO" );
        geoDatabase.setType( DatabaseType.EXPRESSION );
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

    /**
     * @param subSet
     * @param accession
     * @param experimentBioAssays
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean addMatchingBioAssayToSubSet( ExpressionExperimentSubSet subSet, BioAssay bioAssay,
            ExpressionExperiment expExp ) {
        String accession = bioAssay.getAccession().getAccession();
        Collection<BioAssay> experimentBioAssays = expExp.getBioAssays();
        boolean found = false;
        for ( BioAssay assay : experimentBioAssays ) {
            String testAccession = assay.getAccession().getAccession();
            if ( testAccession.equals( accession ) ) {
                subSet.getBioAssays().add( assay );
                found = true;
                break;
            }

        }
        return found;
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
        // BioMaterial bioMaterial = BioMaterial.Factory.newInstance();

        // bioMaterial.setExternalAccession( convertDatabaseEntry( sample ) );
        // bioMaterial.setName( sample.getGeoAccession() + "_channel_" + channel.getChannelNumber() );
        bioMaterial.setDescription( ( bioMaterial.getDescription() == null ? "" : bioMaterial.getDescription() + ";" )
                + "Channel "
                + channel.getChannelNumber()
                + " sample source="
                + channel.getOrganism()
                + " "
                + channel.getSourceName()
                + ( StringUtils.isBlank( channel.getExtractProtocol() ) ? "" : " Extraction Protocol: "
                        + channel.getExtractProtocol() )
                + ( StringUtils.isBlank( channel.getLabelProtocol() ) ? "" : " Labeling Protocol: "
                        + channel.getLabelProtocol() )
                + ( StringUtils.isBlank( channel.getTreatmentProtocol() ) ? "" : " Treatment Protocol: "
                        + channel.getTreatmentProtocol() ) );
        // these protocols could be made into 'real' protocols, if anybody cares.

        for ( String characteristic : channel.getCharacteristics() ) {
            Characteristic gemmaChar = Characteristic.Factory.newInstance();
            gemmaChar.setCategory( characteristic );
            gemmaChar.setValue( characteristic ); // TODO need to put in actual value.
            bioMaterial.getCharacteristics().add( gemmaChar );
        }
        return bioMaterial;
    }

    /**
     * @param contact
     * @return
     */
    private Person convertContact( GeoContact contact ) {
        Person result = Person.Factory.newInstance();
        result.setAddress( contact.getCity() );
        result.setPhone( contact.getPhone() );
        result.setName( contact.getName() );
        result.setEmail( contact.getEmail() );

        // TODO - set other contact fields
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
        // TODO possibly add contributers to the investigators.
        if ( series.getContributers().size() > 0 ) {
            expExp.setDescription( expExp.getDescription() + " -- Contributers: " );
            for ( GeoContact contributer : series.getContributers() ) {
                expExp.setDescription( expExp.getDescription() + " " + contributer.getName() );
            }
        }
    }

    /**
     * Often-needed generation of a valid databaseentry object.
     * 
     * @param geoData
     * @return
     */
    private DatabaseEntry convertDatabaseEntry( GeoData geoData ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();
        result.setExternalDatabase( this.geoDatabase );
        result.setAccession( geoData.getGeoAccession() );
        return result;
    }

    /**
     * @param datasetSamples
     * @return
     */
    @SuppressWarnings("unchecked")
    private BioAssayDimension convertGeoSampleList( List<GeoSample> datasetSamples, ExpressionExperiment expExp ) {
        BioAssayDimension result = BioAssayDimension.Factory.newInstance();

        StringBuilder buf = new StringBuilder();
        Collections.sort( datasetSamples );
        for ( GeoSample sample : datasetSamples ) {
            boolean found = false;
            String sampleAcc = sample.getGeoAccession();
            buf.append( sampleAcc + "," );
            found = matchSampleToBioAssay( expExp, result, found, sampleAcc );
            if ( !found ) {
                log.warn( "No bioassay match for " + sampleAcc ); // this is normal because not all headings are
                // sample ids.
            }
        }
        result.setName( formatName( buf ) );
        result.setDescription( buf.toString() );
        // expExp.getBioAssayDimensions().add( result );
        return result;
    }

    /**
     * @param buf
     * @return
     */
    private String formatName( StringBuilder buf ) {
        return buf.length() > 100 ? buf.toString().substring( 0, 100 ) : buf.toString() + "...";
    }

    /**
     * @param expExp
     * @param result
     * @param found
     * @param sampleAcc
     * @return
     */
    private boolean matchSampleToBioAssay( ExpressionExperiment expExp, BioAssayDimension result, boolean found,
            String sampleAcc ) {

        BioMaterialDimension bioMaterialDimension = getAssociatedBioMaterialDimension( result );

        for ( BioAssay bioAssay : expExp.getBioAssays() ) {
            if ( sampleAcc.equals( bioAssay.getAccession().getAccession() ) ) {
                result.getDimensionBioAssays().add( bioAssay );

                matchBioMaterialToBioAssay( bioMaterialDimension, bioAssay );

                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * @param result
     * @return
     */
    private BioMaterialDimension getAssociatedBioMaterialDimension( BioAssayDimension result ) {
        Collection<BioMaterialDimension> bioMaterialDimensions = result.getBioMaterialDimensions();
        BioMaterialDimension bioMaterialDimension;
        if ( bioMaterialDimensions == null || bioMaterialDimensions.isEmpty() ) {
            bioMaterialDimension = BioMaterialDimension.Factory.newInstance();
            result.getBioMaterialDimensions().add( bioMaterialDimension );
        } else {
            if ( bioMaterialDimensions.size() > 1 )
                throw new UnsupportedOperationException( "Can't handle more than one BioMaterialDimension." );
            bioMaterialDimension = bioMaterialDimensions.iterator().next();
        }
        return bioMaterialDimension;
    }

    /**
     * @param bioMaterialDimension
     * @param bioAssay
     */
    private void matchBioMaterialToBioAssay( BioMaterialDimension bioMaterialDimension, BioAssay bioAssay ) {
        Collection<BioMaterial> bioMaterials = bioAssay.getSamplesUsed();
        assert !( bioMaterials == null || bioMaterials.isEmpty() );
        if ( bioMaterials.size() > 1 ) {
            throw new UnsupportedOperationException(
                    "Can't handle more than one biomaterial per bioassay (sample used per bioassay)" );
        }
        BioMaterial matchingBioMaterial = bioMaterials.iterator().next();
        bioMaterialDimension.getBioMaterials().add( matchingBioMaterial );
    }

    /**
     * // *
     * 
     * @param dataset // *
     * @param expExp // *
     * @return //
     */
    // @SuppressWarnings("unchecked")
    // private BioAssayDimension convertDataColumnHeadings( GeoDataset dataset, ExpressionExperiment expExp ) {
    // BioAssayDimension result = BioAssayDimension.Factory.newInstance();
    // result.setName( "BioAssayDimension for GEO " + dataset );
    // for ( String sampleAcc : dataset.getColumnNames() ) {
    // boolean found = false;
    // // some extra sanity checking here would be wise. What if two columns have the same id.
    // for ( BioAssay bioAssay : ( List<BioAssay> ) expExp.getBioAssays() ) {
    // if ( sampleAcc.equals( bioAssay.getAccession().getAccession() ) ) {
    // result.getBioAssays().add( bioAssay );
    // found = true;
    // break;
    // }
    // }
    // if ( !found ) {
    // log.warn( "No bioassay match for " + sampleAcc ); // this is normal because not all headings are
    // // sample ids.
    // }
    // }
    // return result;
    // }
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

        return this.convertSeries( geoDataset.getSeries().iterator().next() );

    }

    /**
     * @param dataset
     * @param expExp
     */
    private ExpressionExperiment convertDataset( GeoDataset geoDataset, ExpressionExperiment expExp ) {
        log.info( "Converting dataset:" + geoDataset );

        convertDatasetDescriptions( geoDataset, expExp );

        ArrayDesign ad = seenPlatforms.get( geoDataset.getPlatform().getGeoAccession() );
        if ( ad == null )
            throw new IllegalStateException( "ArrayDesigns must be converted before datasets - didn't find "
                    + geoDataset.getPlatform() );

        convertDataSetDataVectors( geoDataset, expExp );

        convertSubsetAssociations( expExp, geoDataset );
        return expExp;

    }

    /**
     * @param geoDataset
     * @param expExp
     */
    private void convertDatasetDescriptions( GeoDataset geoDataset, ExpressionExperiment expExp ) {
        if ( StringUtils.isEmpty( expExp.getDescription() ) ) {
            expExp.setDescription( geoDataset.getDescription() ); // probably not empty.
        }

        expExp.setDescription( expExp.getDescription() + " Includes " + geoDataset.getGeoAccession() + ". " );
        if ( StringUtils.isNotEmpty( geoDataset.getUpdateDate() ) ) {
            expExp.setDescription( expExp.getDescription() + " Update date " + geoDataset.getUpdateDate() + ". " );
        }

        if ( StringUtils.isEmpty( expExp.getName() ) ) {
            expExp.setName( geoDataset.getTitle() );
        } else {
            expExp.setDescription( expExp.getDescription() + " Dataset description " + geoDataset.getGeoAccession()
                    + ": " + geoDataset.getTitle() + ". " );
        }
    }

    /**
     * @param geoDataset
     * @param expExp
     */
    @SuppressWarnings("unchecked")
    private void convertDataSetDataVectors( GeoDataset geoDataset, ExpressionExperiment expExp ) {
        List<GeoSample> datasetSamples = new ArrayList<GeoSample>( getDatasetSamples( geoDataset ) );

        BioAssayDimension bioAssayDimension = convertGeoSampleList( datasetSamples, expExp );

        sanityCheckQuantitationTypes( datasetSamples );

        List<String> quantitationTypes = datasetSamples.iterator().next().getColumnNames();
        List<String> quantitationTypeDescriptions = datasetSamples.iterator().next().getColumnDescriptions();
        boolean first = true;

        /*
         * For the data that are put in 'datasets' (GDS), we know the type of data, but it can be misleading (e.g., Affy
         * data is 'counts'). For others we just have free text in the column descriptions
         */

        int i = 0;
        for ( String quantitationType : quantitationTypes ) {

            // skip the first quantitationType, it's the ID or ID_REF.
            if ( first ) {
                first = false;
                continue;
            }

            String description = quantitationTypeDescriptions.get( i );
            i++;

            QuantitationType qt = QuantitationType.Factory.newInstance();
            qt.setName( quantitationType );
            qt.setDescription( description );

            guessQuantitationTypeParameters( qt, quantitationType, description );

            Map<String, List<String>> dataVectors = makeDataVectors( datasetSamples, quantitationType );

            // use a List for performance.
            Collection<DesignElementDataVector> vectors = new ArrayList<DesignElementDataVector>();

            int count = 0;
            for ( String designElementName : dataVectors.keySet() ) {
                List<String> dataVector = dataVectors.get( designElementName );
                assert dataVector != null && dataVector.size() != 0;
                DesignElementDataVector vector = convertDesignElementDataVector( geoDataset, expExp, bioAssayDimension,
                        designElementName, dataVector, qt );

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
            expExp.getDesignElementDataVectors().addAll( new HashSet( vectors ) );
        }
    }

    /**
     * Attempt to fill in the details of the quantitation type. FIXME, this needs work and testing.
     * 
     * @param qt QuantitationType to fill in details for.
     * @param name of the quantitation type from the GEO sample column
     * @param description of the quantitation type from the GEO sample column
     */
    private void guessQuantitationTypeParameters( QuantitationType qt, String name, String description ) {

        GeneralType gType = GeneralType.QUANTITATIVE;
        PrimitiveType pType = PrimitiveType.DOUBLE;
        ScaleType sType = ScaleType.UNSCALED;
        StandardQuantitationType qType = StandardQuantitationType.MEASUREDSIGNAL;
        Boolean isBackground = Boolean.FALSE;

        if ( name.contains( "Probe ID" ) || description.equalsIgnoreCase( "Probe Set ID" ) ) {
            /*
             * FIXME special case...not a quantitation type.
             */
            qType = StandardQuantitationType.OTHER;
            pType = PrimitiveType.STRING;
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
            pType = PrimitiveType.STRING;
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
        }

        if ( name.matches( "TOP" ) || name.matches( "LEFT" ) || name.matches( "RIGHT" ) || name.matches( "^BOT.*" ) ) {
            qType = StandardQuantitationType.COORDINATE;
        } else if ( name.matches( "^RAT[12]N?_(MEAN|MEDIAN)" ) ) {
            qType = StandardQuantitationType.RATIO;
        } else if ( name.matches( "fold_change" ) || description.contains( "log ratio" )
                || name.toLowerCase().contains( "ratio" ) ) {
            qType = StandardQuantitationType.RATIO;
        }

        qt.setGeneralType( gType );
        qt.setRepresentation( pType );
        qt.setScale( sType );
        qt.setType( qType );
        qt.setIsBackground( isBackground );

        if ( log.isInfoEnabled() ) {
            log
                    .info( "Inferred that quantitation type \"" + name + "\" (Description: \"" + description
                            + "\") corresponds to: " + qType + ",  " + sType
                            + ( qt.getIsBackground() ? " (Background) " : "" ) );
        }

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
            if ( !reference.equals( columnNames ) ) {
                throw new IllegalStateException( "Two samples don't have the same data columns in sample "
                        + sample.getGeoAccession() );
            }
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
    private DesignElementDataVector convertDesignElementDataVector( GeoDataset geoDataset, ExpressionExperiment expExp,
            BioAssayDimension bioAssayDimension, String designElementName, List<String> dataVector, QuantitationType qt ) {
        byte[] blob = convertData( dataVector, qt );
        if ( blob == null ) {
            return null;
        }
        if ( log.isDebugEnabled() ) {
            log.debug( blob.length + " bytes for " + dataVector.size() + " raw elements" );
        }

        CompositeSequence compositeSequence = platformDesignElementMap.get(
                convertPlatform( geoDataset.getPlatform() ).getName() ).get( designElementName );

        DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
        vector.setDesignElement( compositeSequence );
        vector.setExpressionExperiment( expExp );

        vector.setBioAssayDimension( bioAssayDimension );
        vector.setQuantitationType( qt );
        vector.setData( blob );
        return vector;
    }

    /**
     * Convert the by-sample data for a given quantitation type to by-designElement data vectors.
     * 
     * @param datasetSamples
     * @param quantitationType
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> makeDataVectors( List<GeoSample> datasetSamples, String quantitationType ) {
        Map<String, List<String>> dataVectors = new HashMap<String, List<String>>( INITIAL_VECTOR_CAPACITY );
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
                    dataVectors.put( designElementName, new ArrayList<String>() );
                }
                String datum = sample.getDatum( designElementName, quantitationType );
                // this can happen if the platform has probes that aren't in the data
                if ( datum == null && log.isDebugEnabled() ) {
                    log.debug( "Data for sample " + sample.getGeoAccession() + " was missing for element "
                            + designElementName );
                }
                dataVectors.get( designElementName ).add( datum );
            }

        }
        return dataVectors;
    }

    /**
     * @param geoDataset
     * @return
     */
    private Collection<GeoSample> getDatasetSamples( GeoDataset geoDataset ) {
        Collection<GeoSample> seriesSamples = geoDataset.getSeries().iterator().next().getSamples();
        // get just the samples used in this series
        Collection<GeoSample> datasetSamples = new ArrayList<GeoSample>();
        for ( GeoSample sample : seriesSamples ) {
            if ( geoDataset.getColumnNames().contains( sample.getGeoAccession() ) ) {
                if ( log.isInfoEnabled() ) {
                    log.info( "Dataset " + geoDataset + " includes sample " + sample + " on platform "
                            + sample.getPlatforms().iterator().next() );
                }
                datasetSamples.add( sample );
            }
        }
        return datasetSamples;
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
        arrayDesign.setDescription( platform.getDescriptions() );

        platformDesignElementMap.put( arrayDesign.getName(), new HashMap<String, CompositeSequence>() );

        Taxon taxon = convertPlatformOrganism( platform );

        // convert the design element information.
        String identifier = determinePlatformIdentifier( platform );
        String externalReference = determinePlatformExternalReferenceIdentifier( platform );
        String descriptionColumn = determinePlatformDescriptionColumn( platform );
        ExternalDatabase externalDb = determinePlatformExternalDatabase( platform );

        List<String> identifiers = platform.getColumnData( identifier );
        List<String> externalRefs = platform.getColumnData( externalReference );
        List<String> descriptions = platform.getColumnData( descriptionColumn );

        assert identifier != null;
        assert externalRefs != null;
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
            String description = "";

            if ( descIter != null ) description = descIter.next();

            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( id );
            cs.setDescription( description );
            cs.setArrayDesign( arrayDesign );

            BioSequence bs = BioSequence.Factory.newInstance();
            bs.setName( externalRef );
            bs.setTaxon( taxon );
            bs.setPolymerType( PolymerType.DNA );
            bs.setType( SequenceType.DNA ); // TODO need to determine SequenceType and PolymerType.

            DatabaseEntry dbe = DatabaseEntry.Factory.newInstance();
            dbe.setAccession( externalRef );
            dbe.setExternalDatabase( externalDb );

            cs.setBiologicalCharacteristic( bs );

            compositeSequences.add( cs );

            platformDesignElementMap.get( arrayDesign.getName() ).put( id, cs );
        }
        arrayDesign.setCompositeSequences( new HashSet( compositeSequences ) );
        arrayDesign.setAdvertisedNumberOfDesignElements( compositeSequences.size() );

        // We don't get reporters from GEO SOFT files.
        arrayDesign.setReporters( new HashSet() );

        Contact manufacturer = Contact.Factory.newInstance();
        if ( platform.getManufacturer() != null ) {
            manufacturer.setName( platform.getManufacturer() );
        } else {
            manufacturer.setName( "Unknown" );
        }
        arrayDesign.setDesignProvider( manufacturer );

        seenPlatforms.put( platform.getGeoAccession(), arrayDesign );

        return arrayDesign;
    }

    /**
     * @param platform
     * @return
     */
    private Taxon convertPlatformOrganism( GeoPlatform platform ) {
        Taxon taxon = Taxon.Factory.newInstance();
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

        // FIXME yucky hard-coding of Rattus.
        if ( organism.toLowerCase().startsWith( "rattus" ) ) {
            organism = "rattus"; // we don't distinguish between species.
        }

        taxon.setScientificName( organism );
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

        // FIXME: use the ones from the ExperimentalFactor.
        for ( GeoReplication replication : sample.getReplicates() ) {
            bioAssay.getFactorValues().add( convertReplicationToFactorValue( replication ) );
        }

        // FIXME: use the ones from the ExperimentalFactor.
        for ( GeoVariable variable : sample.getVariables() ) {
            bioAssay.getFactorValues().add( convertVariableToFactorValue( variable ) );
        }

        for ( GeoChannel channel : sample.getChannels() ) {
            /*
             * In reality GEO does not have information about the samples run on each channel. We're just making it up.
             * So we need to just add the channel information to the biomaterials we have already.
             */
            convertChannel( sample, channel, bioMaterial );
            // bioAssay.getSamplesUsed().add( bioMaterial );
        }

        for ( GeoPlatform platform : sample.getPlatforms() ) {
            ArrayDesign arrayDesign;
            if ( seenPlatforms.containsKey( platform.getGeoAccession() ) ) {
                arrayDesign = seenPlatforms.get( platform.getGeoAccession() );
            } else {
                arrayDesign = convertPlatform( platform );
            }
            bioAssay.getArrayDesignsUsed().add( arrayDesign );
        }

        return bioAssay;
    }

    /**
     * @param series
     * @return
     */
    private ExpressionExperiment convertSeries( GeoSeries series ) {
        return this.convertSeries( series, null );
    }

    /**
     * @param series
     * @param resultToAddTo
     * @return
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperiment convertSeries( GeoSeries series, ExpressionExperiment resultToAddTo ) {
        if ( series == null ) return null;
        log.debug( "Converting series: " + series.getGeoAccession() );

        ExpressionExperiment expExp;

        if ( resultToAddTo == null ) {
            expExp = ExpressionExperiment.Factory.newInstance();
            expExp.setDescription( "" );
        } else {
            expExp = resultToAddTo;
        }

        expExp.setDescription( series.getSummaries() );
        expExp.setName( series.getTitle() );

        convertContacts( series, expExp );

        expExp.setAccession( convertDatabaseEntry( series ) );

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

        // FIXME - these could go somewhere more interesting, eg ontology enntry
        if ( series.getKeyWords().size() > 0 ) {
            for ( String keyWord : series.getKeyWords() ) {
                design.setDescription( design.getDescription() + " Keyword: " + keyWord );
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

        expExp.setExperimentalDesigns( new HashSet<ExperimentalDesign>() );
        expExp.getExperimentalDesigns().add( design );

        // GEO does not have the concept of a biomaterial.
        Collection<GeoSample> samples = series.getSamples();
        expExp.setBioAssays( new HashSet() );
        int i = 1;

        for ( Iterator iter = series.getSampleCorrespondence().iterator(); iter.hasNext(); ) {

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            bioMaterial.setName( series.getGeoAccession() + "_bioMaterial_" + i );
            i++;

            // Find the sample and convert it.
            Set<String> correspondingSamples = ( Set<String> ) iter.next();
            for ( String cSample : correspondingSamples ) {
                boolean found = false;
                for ( GeoSample sample : samples ) {
                    if ( sample == null || sample.getGeoAccession() == null ) {
                        log.warn( "Null sample or no accession for " + sample );
                        continue;
                    }

                    String accession = sample.getGeoAccession();

                    if ( accession.equals( cSample ) ) {
                        BioAssay ba = convertSample( sample, bioMaterial );
                        ba.getSamplesUsed().add( bioMaterial );
                        log.debug( "Adding " + ba + " and associating with  " + bioMaterial );
                        expExp.getBioAssays().add( ba );
                        found = true;
                        break;
                    }

                }
                if ( !found ) {
                    log.error( "No sample found for " + cSample );
                }
            }

            // bioMaterials.add( bioMaterial );
        }

        // Dataset has additional information about the samples.
        Collection<GeoDataset> dataSets = series.getDatasets();
        for ( GeoDataset dataset : dataSets ) {
            convertDataset( dataset, expExp );
        }

        return expExp;
    }

    /**
     * @param expExp
     * @param geoSubSet
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperimentSubSet convertSubset( ExpressionExperiment expExp, GeoSubset geoSubSet ) {

        ExpressionExperimentSubSet subSet = ExpressionExperimentSubSet.Factory.newInstance();

        // FIXME turn the geo subset information into factors if possible.
        subSet.setName( geoSubSet.getDescription() );
        subSet.setDescription( geoSubSet.getType().toString() );
        subSet.setSourceExperiment( expExp );
        subSet.setBioAssays( new HashSet() );

        for ( GeoSample sample : geoSubSet.getSamples() ) {

            BioAssay bioAssay = convertSample( sample, null ); // converted object only used for searching.

            boolean found = addMatchingBioAssayToSubSet( subSet, bioAssay, expExp );
            assert found : "No matching bioassay found for " + bioAssay.getAccession().getAccession() + " in subset. "
                    + " Make sure the ExpressionExperiment was initialized "
                    + "properly by converting the samples before converting the subsets.";
        }
        return subSet;
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
            categoryTerm.setValue( "----" ); // FIXME
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
        result.setType( DatabaseType.SEQUENCE );

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

        if ( likelyExternalDatabaseIdentifier.equals( "GB_ACC" ) || likelyExternalDatabaseIdentifier.equals( "GB_LIST" ) ) {
            result.setName( "Genbank" );
            result.setType( DatabaseType.SEQUENCE );
        } else if ( likelyExternalDatabaseIdentifier.equals( "ORF" ) ) {
            String organism = platform.getOrganisms().iterator().next();
            result.setName( organism + " ORFs" ); // TODO this is silly.
            result.setType( DatabaseType.GENOME );
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
    protected byte[] convertData( List<String> vector, QuantitationType qt ) {

        if ( vector == null || vector.size() == 0 ) return null;

        boolean containsAtLeastOneNonNull = false;
        for ( String string : vector ) {
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
        for ( String string : vector ) {
            try {
                if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                    toConvert.add( Double.parseDouble( string ) );
                } else if ( pt.equals( PrimitiveType.INT ) ) {
                    toConvert.add( Integer.parseInt( string ) );
                } else if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                    toConvert.add( Boolean.parseBoolean( string ) );
                } else if ( pt.equals( PrimitiveType.STRING ) ) {
                    toConvert.add( string );
                } else {
                    throw new UnsupportedOperationException( "Data vectors of type " + pt + " not supported" );
                }
            } catch ( NumberFormatException e ) {
                // if ( !StringUtils.isBlank( string ) ) {
                // throw e; // not a missing value, some other problem.
                // }
                if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                    toConvert.add( Double.NaN );
                } else if ( pt.equals( PrimitiveType.INT ) ) {
                    toConvert.add( 0 );
                } else if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                    toConvert.add( false ); // FIXME - what happens if boolean parsing fails?
                } else {
                    throw new UnsupportedOperationException( "Data vectors of type " + pt + " not supported" );
                }
            }
        }
        return byteArrayConverter.toBytes( toConvert.toArray() );
    }
}
