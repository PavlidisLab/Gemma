/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.geo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.description.Characteristic;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentSubSet;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.loader.expression.geo.model.GeoChannel;
import edu.columbia.gemma.loader.expression.geo.model.GeoContact;
import edu.columbia.gemma.loader.expression.geo.model.GeoData;
import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;
import edu.columbia.gemma.loader.expression.geo.model.GeoPlatform;
import edu.columbia.gemma.loader.expression.geo.model.GeoReplication;
import edu.columbia.gemma.loader.expression.geo.model.GeoSample;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.expression.geo.model.GeoSubset;
import edu.columbia.gemma.loader.expression.geo.model.GeoVariable;
import edu.columbia.gemma.loader.expression.geo.util.GeoConstants;
import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;
import edu.columbia.gemma.loader.loaderutils.Converter;
import edu.columbia.gemma.util.PrettyPrinter;

/**
 * Convert GEO domain objects into Gemma objects.
 * <p>
 * GEO has four basic kinds of objects: Platforms (ArrayDesigns), Samples (BioAssays), Series (Experiments) and DataSets
 * (which are curated Experiments). Note that a sample can belong to more than one series. A series can belong to more
 * than one dataSet. See http://www.ncbi.nlm.nih.gov/projects/geo/info/soft2.html.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoConverter implements Converter {

    private static Log log = LogFactory.getLog( GeoConverter.class.getName() );

    private ExternalDatabase geoDatabase;

    private Collection<Object> results = new HashSet<Object>();

    private Collection<String> seenPlatforms = new HashSet<String>();

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
            return convert( ( GeoDataset ) geoObject );
        } else if ( geoObject instanceof GeoSeries ) {
            return convertSeries( ( GeoSeries ) geoObject );
        } else if ( geoObject instanceof GeoSubset ) {
            return convertSubset( ( GeoSubset ) geoObject );
        } else if ( geoObject instanceof GeoSample ) {
            return convertSample( ( GeoSample ) geoObject );
        } else if ( geoObject instanceof GeoVariable ) {
            return convertVariable( ( GeoVariable ) geoObject );
        } else if ( geoObject instanceof GeoReplication ) {
            return convertReplication( ( GeoReplication ) geoObject );
        } else if ( geoObject instanceof GeoPlatform ) {
            GeoPlatform platform = ( GeoPlatform ) geoObject;
            if ( !seenPlatforms.contains( platform.getGeoAccesssion() ) ) {
                seenPlatforms.add( platform.getGeoAccesssion() );
                return convertPlatform( platform );
            }
            return null;
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
     * @param geoDataset
     */
    private ExpressionExperiment convert( GeoDataset geoDataset ) {
        log.debug( "Converting dataset:" + geoDataset.getGeoAccesssion() );
        ExpressionExperiment result = ExpressionExperiment.Factory.newInstance();
        result.setDescription( geoDataset.getDescription() );
        result.setName( geoDataset.getTitle() );
        result.setAccession( convertDatabaseEntry( geoDataset ) );

        convertSubsetAssociations( result, geoDataset );
        convertSeriesAssociations( result, geoDataset );
        return result;
    }

    /**
     * @param sample
     * @param channel
     * @return
     */
    private BioMaterial convertChannel( GeoSample sample, GeoChannel channel ) {
        log.debug( "Sample: " + sample.getGeoAccesssion() + " - Converting channel " + channel.getSourceName() );
        BioMaterial bioMaterial = BioMaterial.Factory.newInstance();

        bioMaterial.setExternalAccession( convertDatabaseEntry( sample ) );
        bioMaterial.setName( sample.getGeoAccesssion() + "_channel_" + channel.getChannelNumber() );
        bioMaterial.setDescription( "Channel sample source="
                + channel.getOrganism()
                + " "
                + channel.getSourceName()
                + ( StringUtils.isBlank( channel.getExtractProtocol() ) ? "" : " Extraction Protocol: "
                        + channel.getExtractProtocol() )
                + ( StringUtils.isBlank( channel.getLabelProtocol() ) ? "" : " Labeling Protocol: "
                        + channel.getLabelProtocol() )
                + ( StringUtils.isBlank( channel.getTreatmentProtocol() ) ? "" : " Treatment Protocol: "
                        + channel.getTreatmentProtocol() ) );
        // FIXME: these protocols could be made into 'real' protocols, if anybody cares.

        Collection<Characteristic> characteristics = new HashSet<Characteristic>();
        for ( String characteristic : channel.getCharacteristics() ) {
            Characteristic gemmaChar = Characteristic.Factory.newInstance();
            gemmaChar.setCategory( characteristic );
            gemmaChar.setValue( characteristic ); // FIXME, need value.
            characteristics.add( gemmaChar );
        }
        bioMaterial.setCharacteristics( characteristics );
        return bioMaterial;
    }

    // /**
    // * @param contact
    // * @return
    // */
    // private Person convertContact( GeoContact contact ) {
    // Person result = Person.Factory.newInstance();
    // result.setAddress( contact.getCity() );
    // result.setPhone( contact.getPhone() );
    // result.setName( contact.getName() );
    // // FIXME - set other stuff
    // return result;
    // }

    /**
     * Often-needed generation of a valid databaseentry object.
     * 
     * @param geoData
     * @return
     */
    private DatabaseEntry convertDatabaseEntry( GeoData geoData ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();
        result.setExternalDatabase( this.geoDatabase );
        result.setAccession( geoData.getGeoAccesssion() );
        return result;
    }

    /**
     * @param platform
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign convertPlatform( GeoPlatform platform ) {
        log.debug( "Converting platform: " + platform.getGeoAccesssion() );
        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( platform.getTitle() );
        arrayDesign.setDescription( platform.getDescriptions() );

        Taxon taxon = convertPlatformOrganism( platform );

        // convert the design element information.
        String identifier = determinePlatformIdentifier( platform );
        String externalReference = determinePlatformExternalReferenceIdentifier( platform );
        String descriptionColumn = determinePlatformDescriptionColumn( platform );

        List<String> identifiers = platform.getData().get( identifier );
        List<String> externalRefs = platform.getData().get( externalReference );
        List<String> descriptions = platform.getData().get( descriptionColumn );

        assert identifier != null;
        assert externalRefs != null;
        assert externalRefs.size() == identifiers.size() : "Unequal numbers of identifiers and external references! "
                + externalRefs.size() + " != " + identifiers.size();

        log.debug( "Converting " + identifiers.size() + " probe identifiers on GEO platform "
                + platform.getGeoAccesssion() );

        Iterator<String> refIter = externalRefs.iterator();
        Iterator<String> descIter = descriptions.iterator();
        Collection compositeSequences = new HashSet();
        for ( String id : identifiers ) {
            String externalRef = refIter.next();
            String description = descIter.next();
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( id );
            cs.setDescription( description );
            cs.setArrayDesign( arrayDesign );

            BioSequence bs = BioSequence.Factory.newInstance();
            bs.setName( externalRef );
            bs.setTaxon( taxon );
            cs.setBiologicalCharacteristic( bs );

            compositeSequences.add( cs );
        }
        arrayDesign.setDesignElements( compositeSequences );
        arrayDesign.setAdvertisedNumberOfDesignElements( compositeSequences.size() );

        Contact manufacturer = Contact.Factory.newInstance();
        if ( platform.getManufacturer() != null ) {
            manufacturer.setName( platform.getManufacturer() );
        } else {
            manufacturer.setName( "Unknown" );
        }
        arrayDesign.setDesignProvider( manufacturer );

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

        String organism = organisms.iterator().next();
        log.debug( "Organism: " + organism );
        taxon.setScientificName( organism );
        return taxon;
    }

    /**
     * @param replicates
     * @return
     */
    private ExperimentalFactor convertReplication( GeoReplication replicates ) {
        log.debug( "Converting replicates " + replicates.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( replicates.getType().toString() ); // FIXME this is an enum that can be made into an
        // OntologyEntry.
        result.setDescription( replicates.getDescription() );
        // FIXME - fill in BioAssay FactorValues.
        return result;
    }

    /**
     * A Sample corresponds to a BioAssay; the channels correspond to BioMaterials.
     * 
     * @param sample
     */
    @SuppressWarnings("unchecked")
    private BioAssay convertSample( GeoSample sample ) {
        if ( sample == null ) {
            log.warn( "Null sample" );
            return null;
        }

        if ( sample.getGeoAccesssion() == null || sample.getGeoAccesssion().length() == 0 ) {
            log.error( "No GEO accession for sample" );
            return null;
        }

        log.debug( "Converting sample: " + sample.getGeoAccesssion() );

        BioAssay bioAssay = BioAssay.Factory.newInstance();
        bioAssay.setName( sample.getTitle() );
        bioAssay.setDescription( sample.getDescription() );

        // FIXME: factor values must be filled in.

        for ( GeoChannel channel : sample.getChannels() ) {
            BioMaterial bioMaterial = convertChannel( sample, channel );
            bioAssay.getSamplesUsed().add( bioMaterial );
        }

        for ( GeoPlatform platform : sample.getPlatforms() ) {
            if ( !seenPlatforms.contains( platform.getGeoAccesssion() ) ) {
                results.add( convertPlatform( platform ) );
                seenPlatforms.add( platform.getGeoAccesssion() );
            }
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
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperiment convertSeries( GeoSeries series, ExpressionExperiment resultToAddTo ) {
        if ( series == null ) return null;
        log.debug( "Converting series: " + series.getGeoAccesssion() );

        ExpressionExperiment expExp;

        if ( resultToAddTo == null ) {
            expExp = ExpressionExperiment.Factory.newInstance();
        } else {
            expExp = resultToAddTo;
        }

        expExp.setAccession( convertDatabaseEntry( series ) );

        ExperimentalDesign design = ExperimentalDesign.Factory.newInstance();
        design.setExperimentalFactors( new HashSet() );
        Collection<GeoVariable> variables = series.getVariables().values();
        for ( GeoVariable variable : variables ) {
            log.debug( "Adding variable " + variable );
            ExperimentalFactor ef = convertVariable( variable );
            design.getExperimentalFactors().add( ef );
        }

        expExp.setExperimentalDesigns( new HashSet<ExperimentalDesign>() );
        expExp.getExperimentalDesigns().add( design );

        Collection<GeoSample> samples = series.getSamples();
        expExp.setBioAssays( new HashSet() );
        for ( GeoSample sample : samples ) {
            if ( sample == null || sample.getGeoAccesssion() == null ) continue;
            log.debug( "Adding sample " + sample );
            BioAssay ba = convertSample( sample );
            expExp.getBioAssays().add( ba );
        }
        return expExp;
    }

    /**
     * @param result
     * @param geoDataset
     */
    private void convertSeriesAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        for ( GeoSeries series : geoDataset.getSeries() ) {
            log.debug( "Converting series associated with dataset: " + series.getGeoAccesssion() );
            ExpressionExperiment newInfo = convertSeries( series, result );
            BeanPropertyCompleter.complete( newInfo, result ); // not good enough.
        }
    }

    /**
     * @param subset
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperimentSubSet convertSubset( GeoSubset subset ) {
        ExpressionExperimentSubSet expExp = ExpressionExperimentSubSet.Factory.newInstance();
        expExp.setSourceExperiment( convert( subset.getOwningDataset() ) );
        expExp.setBioAssays( new HashSet() );
        for ( GeoSample sample : subset.getSamples() ) {
            expExp.getBioAssays().add( convertSample( sample ) );
        }
        return expExp;
    }

    /**
     * @param result
     * @param geoDataset
     */
    @SuppressWarnings("unchecked")
    private void convertSubsetAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        for ( GeoSubset subset : geoDataset.getSubsets() ) {
            log.debug( "Converting subset: " + subset.getType() );
            ExpressionExperimentSubSet ees = convertSubset( subset );
            result.getSubsets().add( ees );
        }
    }

    /**
     * @param variable
     * @return
     */
    private ExperimentalFactor convertVariable( GeoVariable variable ) {
        log.debug( "Converting variable " + variable.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( variable.getType().toString() ); // FIXME this is an enum that can be made into an
        // OntologyEntry.
        result.setDescription( variable.getDescription() );
        // FIXME - fill in BioAssay FactorValues.
        return result;
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
        log.warn( "No platform element description column found" );
        return null;
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
                log.info( string + " appears to indicate the array element identifier in column " + index
                        + " for platform " + platform );
                return string;
            }
            index++;
        }
        return null;
    }

}
