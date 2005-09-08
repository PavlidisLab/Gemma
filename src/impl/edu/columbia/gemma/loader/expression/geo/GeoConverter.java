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
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentSubSet;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.loader.expression.geo.model.GeoContact;
import edu.columbia.gemma.loader.expression.geo.model.GeoData;
import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;
import edu.columbia.gemma.loader.expression.geo.model.GeoPlatform;
import edu.columbia.gemma.loader.expression.geo.model.GeoSample;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.expression.geo.model.GeoSubset;
import edu.columbia.gemma.loader.expression.geo.model.GeoVariable;
import edu.columbia.gemma.loader.expression.geo.util.GeoConstants;
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
        } else {
            throw new IllegalArgumentException( "Can't deal with " + geoObject.getClass().getName() );
        }

    }

    /**
     * @param geoDataset
     */
    private ExpressionExperiment convert( GeoDataset geoDataset ) {
        log.info( "Converting dataset:" + geoDataset.getGeoAccesssion() );
        ExpressionExperiment result = ExpressionExperiment.Factory.newInstance();
        result.setAccession( convertDatabaseEntry( geoDataset ) );
        convertSubsetAssociations( result, geoDataset );
        convertSeriesAssociations( result, geoDataset );
        return result;
    }

    /**
     * @param result
     * @param geoDataset
     */
    private void convertSubsetAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        log.info( "Converting subsets associations of a dataset" );
        for ( GeoSubset subset : geoDataset.getSubsets() ) {
            // fixme
            convert( subset );
        }
    }

    /**
     * @param result
     * @param geoDataset
     */
    private void convertSeriesAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        log.info( "Converting series associations of a dataset" );
        for ( GeoSeries series : geoDataset.getSeries() ) {
            // fixme;
            convert( series );
        }
    }

    /**
     * @param series
     */
    private ExpressionExperiment convert( GeoSeries series ) {
        if ( series == null ) return null;
        log.info( "Converting series: " + series.getGeoAccesssion() );

        Collection<GeoVariable> variables = series.getVariables();

        ExperimentalDesign design = ExperimentalDesign.Factory.newInstance();

        ExpressionExperiment expExp = ExpressionExperiment.Factory.newInstance();
        expExp.setAccession( convertDatabaseEntry( series ) );

        expExp.setExperimentalDesigns( new HashSet<ExperimentalDesign>() );
        expExp.getExperimentalDesigns().add( design );

        Collection<GeoSample> samples = series.getSamples();
        for ( GeoSample sample : samples ) {
            convert( sample );
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
    private ExpressionExperimentSubSet convert( GeoSubset subset ) {
        // treat this like an expression experiment, but it has to be attached to the parent experiment.
        ExpressionExperimentSubSet expExp = ExpressionExperimentSubSet.Factory.newInstance();
        expExp.setAccession( convertDatabaseEntry( subset ) );
        // FIXME expExp.setSourceExperiment();
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
     * @param sample
     */
    private BioMaterial convert( GeoSample sample ) {
        if ( sample == null ) {
            log.warn( "Null sample" );
            return null;
        }
        log.info( "Converting sample: " + sample.getGeoAccesssion() );
        BioMaterial bioMaterial = BioMaterial.Factory.newInstance();

        // bioMaterial.setAccession(convertDatabaseEntry(sample));
        bioMaterial.setName( sample.getTitle() );
        bioMaterial.setDescription( sample.getDescription() );

        Collection<GeoPlatform> platforms = sample.getPlatforms();
        for ( GeoPlatform platform : platforms ) {
            convert( platform );
        }

        return bioMaterial;
    }

    /**
     * @param platform
     */
    private void convert( GeoPlatform platform ) {
        log.info( "Converting platform: " + platform.getGeoAccesssion() );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( platform.getTitle() );
        ad.setDescription( platform.getDescriptions() );

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
        ad.setDesignElements( compositeSequences );

        // convert the manufacturer information.
        if ( platform.getManufacturer() != null ) {
            Contact manufacturer = ( Contact ) convert( platform.getManufacturer() );
            ad.setDesignProvider( manufacturer );
        }
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
                log.info( string + " appears to indicate the external reference identifier in column " + index
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
