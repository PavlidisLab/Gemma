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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
import edu.columbia.gemma.expression.experiment.FactorValue;
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

    // private Collection<ExpressionExperiment> results;

    public GeoConverter() {
        geoDatabase = ExternalDatabase.Factory.newInstance();
        geoDatabase.setName( "GEO" );
        geoDatabase.setType( DatabaseType.EXPRESSION );
        // results = new HashSet<ExpressionExperiment>();
    }

    /**
     * @param seriesMap
     */
    public Collection<Object> convert( Collection geoObjects ) {
        Collection<Object> results = new HashSet<Object>();
        for ( Object geoObject : geoObjects ) {
            results.add( convert( geoObject ) );
        }
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
        if ( geoObject instanceof GeoDataset ) {
            return convert( ( GeoDataset ) geoObject );
        } else if ( geoObject instanceof GeoSeries ) {
            return convert( ( GeoSeries ) geoObject );
        } else if ( geoObject instanceof GeoSubset ) {
            return convert( ( GeoSubset ) geoObject );
        } else if ( geoObject instanceof GeoSample ) {
            return convert( ( GeoSample ) geoObject );
        } else if ( geoObject instanceof GeoVariable ) {
            return convert( ( GeoVariable ) geoObject );
        } else if ( geoObject instanceof GeoReplication ) {
            return convert( ( GeoReplication ) geoObject );
        } else {
            throw new IllegalArgumentException( "Can't deal with " + geoObject.getClass().getName() );
        }

    }

    /**
     * @param replicates
     * @return
     */
    public ExperimentalFactor convert( GeoReplication replicates ) {
        log.debug( "Converting replicates " + replicates.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( replicates.getType().toString() ); // FIXME this is an enum that can be made into an
        // OntologyEntry.
        result.setDescription( replicates.getDescription() );
        // FIXME - fill in BioAssay FactorValues.
        return result;
    }

    /**
     * @param variable
     * @return
     */
    public ExperimentalFactor convert( GeoVariable variable ) {
        log.debug( "Converting variable " + variable.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( variable.getType().toString() ); // FIXME this is an enum that can be made into an
        // OntologyEntry.
        result.setDescription( variable.getDescription() );
        // FIXME - fill in BioAssay FactorValues.
        return result;
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
     * @param result
     * @param geoDataset
     */
    @SuppressWarnings("unchecked")
    private void convertSubsetAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        for ( GeoSubset subset : geoDataset.getSubsets() ) {
            log.debug( "Converting subset: " + subset.getType() );
            ExpressionExperimentSubSet ees = convert( subset );
            result.getSubsets().add( ees );
        }
    }

    /**
     * @param result
     * @param geoDataset
     */
    private void convertSeriesAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        for ( GeoSeries series : geoDataset.getSeries() ) {
            log.debug( "Converting series associated with dataset: " + series.getGeoAccesssion() );
            ExpressionExperiment newInfo = convert( series );
            BeanPropertyCompleter.complete( result, newInfo );
        }
    }

    /**
     * @param series
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperiment convert( GeoSeries series ) {
        if ( series == null ) return null;
        log.debug( "Converting series: " + series.getGeoAccesssion() );

        ExpressionExperiment expExp = ExpressionExperiment.Factory.newInstance();
        expExp.setAccession( convertDatabaseEntry( series ) );

        ExperimentalDesign design = ExperimentalDesign.Factory.newInstance();
        design.setExperimentalFactors( new HashSet() );
        Collection<GeoVariable> variables = series.getVariables().values();
        for ( GeoVariable variable : variables ) {
            ExperimentalFactor ef = convert( variable );
            design.getExperimentalFactors().add( ef );
        }

        expExp.setExperimentalDesigns( new HashSet<ExperimentalDesign>() );
        expExp.getExperimentalDesigns().add( design );

        Collection<GeoSample> samples = series.getSamples();
        expExp.setBioAssays( new HashSet() );
        for ( GeoSample sample : samples ) {
            BioAssay ba = convert( sample );
            expExp.getBioAssays().add( ba );
        }
        return expExp;
    }

    /**
     * @param contact
     * @return
     */
    private Person convert( GeoContact contact ) {
        Person result = Person.Factory.newInstance();
        result.setAddress( contact.getCity() );
        result.setPhone( contact.getPhone() );
        result.setName( contact.getName() );
        // FIXME - set other stuff
        return result;
    }

    /**
     * @param subset
     */
    @SuppressWarnings("unchecked")
    private ExpressionExperimentSubSet convert( GeoSubset subset ) {
        ExpressionExperimentSubSet expExp = ExpressionExperimentSubSet.Factory.newInstance();
        expExp.setSourceExperiment( convert( subset.getOwningDataset() ) );
        expExp.setBioAssays( new HashSet() );
        for ( GeoSample sample : subset.getSamples() ) {
            expExp.getBioAssays().add( convert( sample ) );
        }
        return expExp;
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
        result.setAccession( geoData.getGeoAccesssion() );
        return result;
    }

    /**
     * A Sample corresponds to a BioAssay; the channels correspond to BioMaterials.
     * 
     * @param sample
     */
    @SuppressWarnings("unchecked")
    private BioAssay convert( GeoSample sample ) {
        if ( sample == null ) {
            log.warn( "Null sample" );
            return null;
        }

        if ( sample.getGeoAccesssion() == null || sample.getGeoAccesssion().length() == 0 ) {
            log.error( "No GEO accession for sample" );
            return null;
        }

        log.debug( "Converting sample: " + sample.getGeoAccesssion() );

        Collection<GeoPlatform> platforms = sample.getPlatforms();
        for ( GeoPlatform platform : platforms ) {
            convert( platform );
        }

        BioAssay bioAssay = BioAssay.Factory.newInstance();
        bioAssay.setName( sample.getTitle() );
        bioAssay.setDescription( sample.getDescription() );
        bioAssay.setSamplesUsed( new HashSet() );

        // Need to fill in FactorValues.
        bioAssay.setFactorValues( new HashSet() );

        Collection<Characteristic> characteristics = new HashSet<Characteristic>();
        for ( GeoChannel channel : sample.getChannelData() ) {

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();

            bioMaterial.setExternalAccession( convertDatabaseEntry( sample ) );
            bioMaterial.setName( channel.getSourceName() );
            // FIXME: fill in other fields from channel.

            for ( String characteristic : channel.getCharacteristics() ) {
                Characteristic gemmaChar = Characteristic.Factory.newInstance();
                gemmaChar.setCategory( characteristic );
                gemmaChar.setValue( characteristic ); // FIXME, need value.
                characteristics.add( gemmaChar );
            }
            bioMaterial.setCharacteristics( characteristics );

            bioAssay.getSamplesUsed().add( bioMaterial );

        }

        return bioAssay;
    }

    /**
     * @param platform
     */
    private ArrayDesign convert( GeoPlatform platform ) {
        log.debug( "Converting platform: " + platform.getGeoAccesssion() );
        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( platform.getTitle() );
        arrayDesign.setDescription( platform.getDescriptions() );

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

        Iterator<String> refIter = externalRefs.iterator();
        Iterator<String> descIter = descriptions.iterator();
        Collection compositeSequences = new HashSet();
        for ( String id : identifiers ) {
            String externalRef = refIter.next();
            String description = descIter.next();
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( id );
            cs.setDescription( description );

            BioSequence bs = BioSequence.Factory.newInstance();
            bs.setName( externalRef );

            cs.setBiologicalCharacteristic( bs );

        }
        arrayDesign.setDesignElements( compositeSequences );

        // convert the manufacturer information.
        if ( platform.getManufacturer() != null ) {
            Contact manufacturer = ( Contact ) convert( platform.getManufacturer() );
            arrayDesign.setDesignProvider( manufacturer );
        }

        return arrayDesign;
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
        return null;
    }

}
