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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.genome.taxon.service.TaxonService;
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
import ubic.gemma.loader.util.parser.ExternalDatabaseUtils;
import ubic.gemma.model.association.GOEvidenceCode;
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
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.ontology.providers.MgedOntologyService;

/**
 * Convert GEO domain objects into Gemma objects. Usually we trigger this by passing in GeoSeries objects.
 * <p>
 * GEO has four basic kinds of objects: Platforms (ArrayDesigns), Samples (BioAssays), Series (Experiments) and DataSets
 * (which are curated Experiments). Note that a sample can belong to more than one series. A series can include more
 * than one dataset. GEO also supports the concept of a superseries. See
 * http://www.ncbi.nlm.nih.gov/projects/geo/info/soft2.html.
 * <p>
 * A curated expression data set is at first represented by a GEO "GDS" number (a curated dataset), which maps to a
 * series (GSE). HOWEVER, multiple datasets may go together to form a series (GSE). This can happen when the "A" and "B"
 * arrays were both run on the same samples. Thus we actually normally go by GSE.
 * <p>
 * This service can be used in database-aware or unaware states. However, it has prototype scope as it has some 'global'
 * data structures used during processing.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GeoConverterImpl implements GeoConverter {

    /**
     * This string is inserted into the descriptions of constructed biomaterials.
     */
    private static final String BIOMATERIAL_DESCRIPTION_PREFIX = "BioMat:";

    /**
     * This string is inserted into the names of constructed biomaterials, so you get names like GSE5929_BioMat_58.
     */
    private static final String BIOMATERIAL_NAME_TAG = "_Biomat_";

    /**
     * How often we tell the user about data processing (items per update)
     */
    private static final int LOGGING_VECTOR_COUNT_UPDATE = 2000;

    private static Log log = LogFactory.getLog( ArrayDesignSequenceProcessingService.class.getName() );

    /**
     * Initial guess at how many designelementdatavectors to allocate space for.
     */
    private static final int INITIAL_VECTOR_CAPACITY = 10000;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private TaxonService taxonService;

    private ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    private ExternalDatabase geoDatabase;

    private Map<String, Map<String, CompositeSequence>> platformDesignElementMap = new HashMap<String, Map<String, CompositeSequence>>();

    private Map<String, Taxon> taxonScientificNameMap = new HashMap<String, Taxon>();

    private Map<String, Taxon> taxonAbbreviationMap = new HashMap<String, Taxon>();

    private Collection<Object> results = new HashSet<Object>();

    private Map<String, ArrayDesign> seenPlatforms = new HashMap<String, ArrayDesign>();

    private ExternalDatabase genbank;

    /**
     * `
     */
    private boolean splitByPlatform = false;

    /**
     * The scientific name used for rat species. FIXME this should be updated elsewhere; avoid this hardcoding.
     */
    private static final String RAT = "Rattus norvegicus";

    private static Map<String, String> organismDatabases = new HashMap<String, String>();

    static {
        organismDatabases.put( "Saccharomyces cerevisiae", "SGD" );
        organismDatabases.put( "Schizosaccharomyces pombe", "GeneDB" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.GeoConverter#clear()
     */
    @Override
    public void clear() {
        results = new HashSet<Object>();
        seenPlatforms = new HashMap<String, ArrayDesign>();
        platformDesignElementMap = new HashMap<String, Map<String, CompositeSequence>>();
        taxonAbbreviationMap.clear();
        taxonScientificNameMap.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.GeoConverter#convert(java.util.Collection)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<Object> convert( Collection<? extends GeoData> geoObjects ) {
        for ( Object geoObject : geoObjects ) {
            Object convertedObject = convert( ( GeoData ) geoObject );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.GeoConverter#convert(java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object convert( GeoData geoObject ) {
        if ( geoObject == null ) {
            log.warn( "Null object" );
            return null;
        }
        if ( geoObject instanceof Collection ) {
            return convert( ( Collection<GeoData> ) geoObject );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.GeoConverter#convertData(java.util.List,
     * ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public byte[] convertData( List<Object> vector, QuantitationType qt ) {

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
                String valueString = ( String ) rawValue;
                if ( StringUtils.isBlank( valueString ) ) {
                    numMissing++;
                    handleMissing( toConvert, pt );
                    continue;
                }
                try {
                    if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                        toConvert.add( Double.parseDouble( valueString ) );
                    } else if ( pt.equals( PrimitiveType.STRING ) ) {
                        toConvert.add( rawValue );
                    } else if ( pt.equals( PrimitiveType.CHAR ) ) {
                        if ( valueString.length() != 1 ) {
                            throw new IllegalStateException( "Attempt to cast a string of length "
                                    + valueString.length() + " to a char: " + rawValue + "(quantitation type =" + qt );
                        }
                        toConvert.add( valueString.toCharArray()[0] );
                    } else if ( pt.equals( PrimitiveType.INT ) ) {
                        toConvert.add( Integer.parseInt( valueString ) );
                    } else if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                        toConvert.add( Boolean.parseBoolean( valueString ) );
                    } else {
                        throw new UnsupportedOperationException( "Data vectors of type " + pt + " not supported" );
                    }
                } catch ( NumberFormatException e ) {
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

        byte[] bytes = byteArrayConverter.toBytes( toConvert.toArray() );

        /*
         * Debugging - absolutely make sure we can convert the data back.
         */
        if ( pt.equals( PrimitiveType.DOUBLE ) ) {
            double[] byteArrayToDoubles = byteArrayConverter.byteArrayToDoubles( bytes );
            if ( byteArrayToDoubles.length != vector.size() ) {
                throw new IllegalStateException( "Expected " + vector.size() + " got " + byteArrayToDoubles.length
                        + " doubles" );
            }
        } else if ( pt.equals( PrimitiveType.INT ) ) {
            int[] byteArrayToInts = byteArrayConverter.byteArrayToInts( bytes );
            if ( byteArrayToInts.length != vector.size() ) {
                throw new IllegalStateException( "Expected " + vector.size() + " got " + byteArrayToInts.length
                        + " ints" );
            }
        } else if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
            boolean[] byteArrayToBooleans = byteArrayConverter.byteArrayToBooleans( bytes );
            if ( byteArrayToBooleans.length != vector.size() ) {
                throw new IllegalStateException( "Expected " + vector.size() + " got " + byteArrayToBooleans.length
                        + " booleans" );
            }
        }

        return bytes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.GeoConverter#convertSubsetToExperimentalFactor(ubic.gemma.model.expression.
     * experiment.ExpressionExperiment, ubic.gemma.loader.expression.geo.model.GeoSubset)
     */
    @Override
    public void convertSubsetToExperimentalFactor( ExpressionExperiment expExp, GeoSubset geoSubSet ) {

        ExperimentalDesign experimentalDesign = expExp.getExperimentalDesign();
        Collection<ExperimentalFactor> existingExperimentalFactors = experimentalDesign.getExperimentalFactors();

        ExperimentalFactor experimentalFactor = ExperimentalFactor.Factory.newInstance();
        experimentalFactor.setName( geoSubSet.getType().toString() );
        VocabCharacteristic term = convertVariableType( geoSubSet.getType() );
        term.setDescription( "Converted from GEO subset " + geoSubSet.getGeoAccession() );
        term.setValue( term.getCategory() );

        term.setValueUri( term.getCategoryUri() );

        experimentalFactor.setCategory( term );
        experimentalFactor.setType( FactorType.CATEGORICAL );
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
        addFactorValueToBioMaterial( expExp, geoSubSet, factorValue );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.GeoConverter#convertSupplementaryFileToLocalFile(java.lang.Object)
     */
    @Override
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
     * This method determines the primary taxon on the array: There are 4 main branches of logic. 1.First it checks if
     * there is only one platform taxon defined on the GEO submission: If there is that is the primary taxon. 2.If
     * multiple taxa are given for the platform then the taxa are checked to see if they share a common parent if so
     * that is the primary taxon e.g. salmonid where atlantic salmon and rainbow trout are given. 3.Finally the
     * probeTaxa are looked at and the most common probe taxa is calculated as the primary taxon 4. No taxon found
     * throws an error
     * 
     * @param platformTaxa Collection of taxa that were given on the GEO array submission as platform taxa
     * @param probeTaxa Collection of taxa strings defining the taxon of each probe on the array.
     * @return Primary taxon of array as determined by this method
     * @exception Thrown if no primary taxon can be determined for array.
     */
    @Override
    public Taxon getPrimaryArrayTaxon( Collection<Taxon> platformTaxa, Collection<String> probeTaxa )
            throws IllegalArgumentException {

        if ( platformTaxa == null || platformTaxa.isEmpty() ) {
            return null;
        }

        // if there is only 1 taxon on the platform submission then this is the primary taxon
        if ( platformTaxa.size() == 1 ) {
            log.debug( "Only 1 taxon given on GEO platform: " + platformTaxa.iterator().next() );
            return platformTaxa.iterator().next();
        }

        // If there are multiple taxa on array
        else if ( platformTaxa.size() > 1 ) {
            log.debug( platformTaxa.size() + " taxa in GEO platform" );
            // check if they share a common parent taxon to use as primary taxa.
            Collection<Taxon> parentTaxa = new HashSet<Taxon>();
            for ( Taxon platformTaxon : platformTaxa ) {
                // thaw to get parent taxon
                this.taxonService.thaw( platformTaxon );
                Taxon platformParentTaxon = platformTaxon.getParentTaxon();
                parentTaxa.add( platformParentTaxon );
            }
            // check now if we only have one parent taxon adn check if no null, if a null then there was a taxon with no
            // parent
            if ( !( parentTaxa.contains( null ) ) && parentTaxa.size() == 1 ) {
                log.debug( "Parent taxon found " + parentTaxa );
                return parentTaxa.iterator().next();
            }
            // No common parent then calculate based on probe taxa:

            log.debug( "Looking at probe taxa to determine 'primary' taxon" );
            // create a hashmap keyed on taxon with a counter to count the number of probes for that taxon.
            Map<String, Integer> taxonProbeNumberList = new HashMap<String, Integer>();

            for ( String probeTaxon : probeTaxa ) {
                // reset each iteration so if no probes already processed set to 1
                Integer counter = 1;
                if ( taxonProbeNumberList.containsKey( probeTaxon ) ) {
                    counter = taxonProbeNumberList.get( probeTaxon ) + 1;
                    taxonProbeNumberList.put( probeTaxon, counter );
                }
                taxonProbeNumberList.put( probeTaxon, counter );
            }
            String primaryTaxonName = "";
            Integer highestScore = 0;
            for ( String taxon : taxonProbeNumberList.keySet() ) {
                // filter out those probes that have no taxon set control spots. Here's that 'n/a' again, kind of
                // ugly but we see it in some arrays
                if ( !taxon.equals( "n/a" ) && StringUtils.isNotBlank( taxon )
                        && taxonProbeNumberList.get( taxon ) > highestScore ) {
                    primaryTaxonName = taxon;
                    highestScore = taxonProbeNumberList.get( taxon );
                }
            }
            if ( StringUtils.isNotBlank( primaryTaxonName ) ) {
                return this.convertProbeOrganism( primaryTaxonName );
            }

        }
        // error no taxon on array submission

        throw new IllegalArgumentException( "No taxon could be determined for GEO platform " );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.geo.GeoConverter#setExternalDatabaseService(ubic.gemma.model.common.description.
     * ExternalDatabaseService)
     */
    @Override
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.GeoConverter#setSplitByPlatform(boolean)
     */
    @Override
    public void setSplitByPlatform( boolean splitByPlatform ) {
        this.splitByPlatform = splitByPlatform;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.GeoConverter#setTaxonService(ubic.gemma.model.genome.TaxonService)
     */
    @Override
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
     * @param bioMaterial
     * @param experimentalFactor
     * @return true if the biomaterial already has a factorvalue for the given experimentalFactor; false otherwise.
     */
    private boolean alreadyHasFactorValueForFactor( BioMaterial bioMaterial, ExperimentalFactor experimentalFactor ) {
        for ( FactorValue fv : bioMaterial.getFactorValues() ) {
            ExperimentalFactor existingEf = fv.getExperimentalFactor();
            // This is a weak form of 'equals' - we just check the name.
            if ( existingEf.getName().equals( experimentalFactor.getName() ) ) {
                return true;
            }
        }
        return false;
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
     * Used for the case where we want to split the GSE into two (or more) separate ExpressionExperiments based on
     * platform. This is necessary when the two platforms are completely incompatible.
     * 
     * @param series
     * @param converted
     * @param platformDatasetMap
     * @param i
     * @param platform
     */
    private void convertByPlatform( GeoSeries series, Collection<ExpressionExperiment> converted,
            Map<GeoPlatform, Collection<GeoData>> platformDatasetMap, int i, GeoPlatform platform ) {
        GeoSeries platformSpecific = new GeoSeries();

        Collection<GeoData> datasets = platformDatasetMap.get( platform );
        assert datasets.size() > 0;

        for ( GeoSample sample : series.getSamples() ) {
            // ugly, we have to assume there is only one platform per sampl.
            if ( sample.getPlatforms().iterator().next().equals( platform ) ) {
                platformSpecific.addSample( sample );
            }
        }

        // strip out samples that aren't from this platform.
        for ( GeoData dataset : datasets ) {
            if ( dataset instanceof GeoDataset ) {
                ( ( GeoDataset ) dataset ).dissociateFromSeries( series );
                platformSpecific.addDataSet( ( GeoDataset ) dataset );
            }
        }

        /*
         * Basically copy over most of the information
         */
        platformSpecific.setContact( series.getContact() );
        platformSpecific.setContributers( series.getContributers() );
        platformSpecific.setGeoAccession( series.getGeoAccession() + "." + i );
        platformSpecific.setKeyWords( series.getKeyWords() );
        platformSpecific.setOverallDesign( series.getOverallDesign() );
        platformSpecific.setPubmedIds( series.getPubmedIds() );
        platformSpecific.setReplicates( series.getReplicates() );
        platformSpecific.setSampleCorrespondence( series.getSampleCorrespondence() );
        platformSpecific.setSummaries( series.getSummaries() );
        platformSpecific.setTitle( series.getTitle() + " - " + platform.getGeoAccession() );
        platformSpecific.setWebLinks( series.getWebLinks() );
        platformSpecific.setValues( series.getValues() );

        converted.add( convertSeries( platformSpecific, null ) );

    }

    /**
     * GEO does not keep track of 'biomaterials' that make up different channels. Therefore the two channels effectively
     * make up a single biomaterial, as far as we're concerned. We're losing information here.
     * 
     * @param sample
     * @param channel
     * @return
     */
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
                    gemmaChar.setEvidenceCode( GOEvidenceCode.IIA );
                    bioMaterial.getCharacteristics().add( gemmaChar );
                } catch ( Exception e ) {
                    // conversion didn't work, fall back.
                    Characteristic gemmaChar = Characteristic.Factory.newInstance();
                    gemmaChar.setValue( characteristic );
                    gemmaChar.setDescription( defaultDescription );
                    gemmaChar.setEvidenceCode( GOEvidenceCode.IIA );
                    bioMaterial.getCharacteristics().add( gemmaChar );
                }

            } else {
                // no colon, just use raw (same as fallback above)
                Characteristic gemmaChar = Characteristic.Factory.newInstance();
                gemmaChar.setValue( characteristic );
                gemmaChar.setDescription( defaultDescription );
                gemmaChar.setEvidenceCode( GOEvidenceCode.IIA );
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
            sourceChar.setEvidenceCode( GOEvidenceCode.IIA );
            bioMaterial.getCharacteristics().add( sourceChar );
        }

        if ( StringUtils.isNotBlank( channel.getOrganism() ) ) {
            // if we have a case where the two channels have different taxon throw an exception.
            String currentChannelTaxon = channel.getOrganism();
            if ( bioMaterial.getSourceTaxon() != null ) {
                String previousChannelTaxon = bioMaterial.getSourceTaxon().getScientificName();
                if ( previousChannelTaxon != null && !( previousChannelTaxon.equals( currentChannelTaxon ) ) ) {
                    throw new IllegalArgumentException( "Channel 1 taxon is "
                            + bioMaterial.getSourceTaxon().getScientificName() + " Channel 2 taxon is "
                            + currentChannelTaxon + " Check that is expected for sample " + sample.getGeoAccession() );
                }

            } else {
                Taxon taxon = Taxon.Factory.newInstance();
                taxon.setIsSpecies( true );
                taxon.setScientificName( channel.getOrganism() );
                taxon.setIsGenesUsable( true ); // plausible default, doesn't matter.
                bioMaterial.setSourceTaxon( taxon );
            }

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
            labelChar.setEvidenceCode( GOEvidenceCode.IIA );
            bioMaterial.getCharacteristics().add( labelChar );
        }

        return bioMaterial;
    }

    /**
     * @param contact
     * @return
     */
    private Person convertContact( GeoContact contact ) {
        Person result = Person.Factory.newInstance();

        /*
         * Note: removed address conversion. We don't normally get that info from GEO nor do we need it.
         */

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
     * Often-needed generation of a valid databaseentry object.
     * 
     * @param geoData
     * @return
     */
    private DatabaseEntry convertDatabaseEntry( GeoData geoData ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();

        initGeoExternalDatabase();

        result.setExternalDatabase( this.geoDatabase );

        // remove trailing ".1" etc. in case it was split.
        result.setAccession( geoData.getGeoAccession().replaceAll( "\\.[0-9]+$", "" ) );

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

        Collection<ExpressionExperiment> seriesResults = this.convertSeries( geoDataset.getSeries().iterator().next() );
        assert seriesResults.size() == 1; // unless we have multiple species, not possible.
        return seriesResults.iterator().next();
    }

    /**
     * @param dataset
     * @param expExp
     */
    private ExpressionExperiment convertDataset( GeoDataset geoDataset, ExpressionExperiment expExp ) {

        /*
         * First figure out of there are any samples for this data set. It could be that they were duplicates of ones
         * found in other series, so were skipped. See GeoService
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
            /*
             * See bug 1672. Sometimes the platform for the dataset is wrong so we should just go on. The exception was
             * otherwise catching a case we don't see under normal use.
             */
            throw new IllegalStateException( "ArrayDesigns must be converted before datasets - didn't find "
                    + geoDataset.getPlatform() + "; possibly dataset has incorrect platform?" );
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
    private RawExpressionDataVector convertDesignElementDataVector( GeoPlatform geoPlatform,
            ExpressionExperiment expExp, BioAssayDimension bioAssayDimension, String designElementName,
            List<Object> dataVector, QuantitationType qt ) {

        if ( dataVector == null || dataVector.size() == 0 ) return null;

        int numValuesExpected = bioAssayDimension.getBioAssays().size();
        if ( dataVector.size() != numValuesExpected ) {
            throw new IllegalArgumentException( "Expected " + numValuesExpected
                    + " in bioassaydimension, data contains " + dataVector.size() );
        }
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

        /*
         * Replace name with the one we're using in the array design after conversion. This information gets filled in
         * earlier in the conversion process (see GeoService)
         */
        String mappedName = geoPlatform.getProbeNamesInGemma().get( designElementName );

        if ( mappedName == null ) {
            // Sigh..this is unlikely to work in general, but see bug 1709.
            mappedName = geoPlatform.getProbeNamesInGemma().get( designElementName.toUpperCase() );
        }

        if ( mappedName == null ) {
            throw new IllegalStateException( "There is  no probe matching " + designElementName );
        }

        CompositeSequence compositeSequence = designMap.get( mappedName );

        if ( compositeSequence == null )
            throw new IllegalStateException( "No composite sequence " + designElementName );

        if ( compositeSequence.getBiologicalCharacteristic() != null
                && compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null
                && compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry().getExternalDatabase()
                        .getName() == null ) {
            // this is obscure.
            throw new IllegalStateException( compositeSequence + " sequence accession external database lacks name" );
        }

        if ( log.isDebugEnabled() ) log.debug( "Associating " + compositeSequence + " with dedv" );
        RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
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
    private BioAssayDimension convertGeoSampleList( List<GeoSample> datasetSamples, ExpressionExperiment expExp ) {
        BioAssayDimension resultBioAssayDimension = BioAssayDimension.Factory.newInstance();

        StringBuilder bioAssayDimName = new StringBuilder();
        Collections.sort( datasetSamples );
        bioAssayDimName.append( expExp.getShortName() + ": " );
        for ( GeoSample sample : datasetSamples ) {
            boolean found = false;
            String sampleAcc = sample.getGeoAccession();
            bioAssayDimName.append( sampleAcc + "," ); // FIXME this is rather silly!
            found = matchSampleToBioAssay( expExp, resultBioAssayDimension, sampleAcc );
            if ( !found ) {
                // this is normal because not all headings are
                // sample ids.
                log.warn( "No bioassay match for " + sampleAcc );
            }
        }
        log.debug( resultBioAssayDimension.getBioAssays().size() + " Bioassays in biodimension" );
        resultBioAssayDimension.setName( formatName( bioAssayDimName ) );
        resultBioAssayDimension.setDescription( bioAssayDimName.toString() );
        return resultBioAssayDimension;
    }

    /**
     * Given an organisms name from GEO, create or find the taxon in the DB.
     * 
     * @param organisms name as provided by GEO presumed to be a scientific name
     * @return Taxon details
     */
    private Taxon convertOrganismToTaxon( String taxonScientificName ) {
        assert taxonScientificName != null;

        /* if not, either create a new one and persist, or get from db and put in map. */

        if ( taxonScientificName.toLowerCase().startsWith( GeoConverterImpl.RAT ) ) {
            taxonScientificName = GeoConverterImpl.RAT; // we don't distinguish between species.
        }

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setScientificName( taxonScientificName );
        taxon.setIsSpecies( true );
        taxon.setIsGenesUsable( false );
        if ( taxonService != null ) {
            Taxon t = taxonService.findOrCreate( taxon );
            if ( t != null ) {
                taxon = t;
            }
        }

        taxonScientificNameMap.put( taxonScientificName, taxon );
        return taxon;

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

        ArrayDesign arrayDesign = createMinimalArrayDesign( platform );

        log.info( "Converting platform: " + platform.getGeoAccession() );
        platformDesignElementMap.put( arrayDesign.getShortName(), new HashMap<String, CompositeSequence>() );

        // convert the design element information.
        String identifier = platform.getIdColumnName();
        if ( identifier == null ) {
            throw new IllegalStateException( "Cannot determine the platform design element id column for " + platform
                    + "; " + platform.getColumnNames().size() + " column names available." );
        }

        Collection<String> externalReferences = determinePlatformExternalReferenceIdentifier( platform );
        String descriptionColumn = determinePlatformDescriptionColumn( platform );
        String sequenceColumn = determinePlatformSequenceColumn( platform );
        String probeOrganismColumn = determinePlatformProbeOrganismColumn( platform );
        ExternalDatabase externalDb = determinePlatformExternalDatabase( platform );

        List<String> identifiers = platform.getColumnData( identifier );
        List<String> descriptions = platform.getColumnData( descriptionColumn );

        List<String> sequences = null;
        if ( sequenceColumn != null ) {
            sequences = platform.getColumnData( sequenceColumn );
        }
        // The primary taxon for the array: this should be a taxon that is listed as the platform taxon on geo
        // submission
        Collection<Taxon> platformTaxa = convertPlatformOrganisms( platform, probeOrganismColumn );

        // these taxons represent taxons for the probes
        List<String> probeOrganism = null;
        if ( probeOrganismColumn != null ) {
            log.debug( "Organism details found for probes on array " + platform.getGeoAccession() );
            probeOrganism = platform.getColumnData( probeOrganismColumn );
        }

        // The primary taxon for the array: either taxon listed on geo submission, or parent taxon listed on geo
        // submission or predominant probe taxon
        // calcualted using platformTaxa or probeOrganismColumn
        Taxon primaryTaxon = this.getPrimaryArrayTaxon( platformTaxa, probeOrganism );

        if ( primaryTaxon == null ) {
            throw new IllegalStateException( "No taxon could be determined for array design: " + arrayDesign );
        }

        arrayDesign.setPrimaryTaxon( primaryTaxon );

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

        Pattern refSeqAccessionPattern = Pattern.compile( "^[A-Z]{2}_" );

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
            String probeName = platform.getProbeNamesInGemma().get( id );
            if ( probeName == null ) {
                probeName = id;
                if ( log.isDebugEnabled() ) log.debug( "Probe retaining original name: " + probeName );
                platform.getProbeNamesInGemma().put( id, id ); // must make sure this is populated.
            } else {
                if ( log.isDebugEnabled() ) log.debug( "Found probe: " + probeName );
            }

            cs.setName( probeName );
            cs.setDescription( description );
            cs.setArrayDesign( arrayDesign );

            // LMD:1647- If There is a Organism Column given for the probe then set taxon from that overwriting platform
            // if probeOrganismColumn is set but for this probe no taxon do not set probeTaxon and thus create no
            // biosequence
            Taxon probeTaxon = Taxon.Factory.newInstance();
            if ( probeOrganism != null && StringUtils.isNotBlank( probeOrganism.get( i ) ) ) {
                probeTaxon = convertProbeOrganism( probeOrganism.get( i ) );
            }
            // if there are no probe taxons then all the probes should take the taxon from the primary taxon
            if ( probeOrganismColumn == null ) {
                probeTaxon = primaryTaxon;
            }

            BioSequence bs = createMinimalBioSequence( probeTaxon );

            boolean isRefseq = false;
            // ExternalDB will be null if it's IMAGE (this is really pretty messy, sorry)
            if ( externalAccession != null && externalDb != null && externalDb.getName().equals( "Genbank" )
                    && StringUtils.isNotBlank( externalAccession ) ) {
                // http://www.ncbi.nlm.nih.gov/RefSeq/key.html#accessions : "RefSeq accession numbers can be
                // distinguished from GenBank accessions by their prefix distinct format of [2 characters|underbar]"
                Matcher refSeqAccessionMatcher = refSeqAccessionPattern.matcher( externalAccession );
                isRefseq = refSeqAccessionMatcher.matches();
            }

            boolean isImage = false;
            if ( cloneIdentifier != null ) {
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
                bs.setName( platform.getGeoAccession() + "_" + id );
                bs.setDescription( "Sequence provided by manufacturer. "
                        + ( externalAccession != null ? "Used in leiu of " + externalAccession
                                : "No external accession provided" ) );
            } else if ( externalAccession != null && !isRefseq && !isImage && externalDb != null ) {
                /*
                 * We don't use this if we have an IMAGE clone because the accession might be wrong (e.g., for a
                 * Refseq). During persisting the IMAGE clone will be replaced with the 'real' thing.
                 */

                /*
                 * We also don't store them if they are refseq ids, because refseq ids are generally not the actual
                 * sequences put on arrays.
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
            } else if ( probeTaxon == null ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( "No taxon for " + cs + " on " + arrayDesign
                            + ", no biological characteristic can be added." );
                }
            } else if ( probeTaxon.getId() != null ) {
                // IF there is no taxon given for probe do not create a biosequence otherwise bombs as there is no taxon
                // to persist
                cs.setBiologicalCharacteristic( bs );

            }

            compositeSequences.add( cs );
            platformDesignElementMap.get( arrayDesign.getShortName() ).put( probeName, cs );

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
     * Retrieve full taxon details for a platform given the organisms scientific name in GEO. If multiple organisms are
     * recorded against an array only first taxon details are returned. Warning is given when no column is found to give
     * the taxons for the probes
     * 
     * @param platform GEO platform details
     * @param probeTaxonColumnName Column name of probe taxons
     * @return List of taxons on platform
     */
    private Collection<Taxon> convertPlatformOrganisms( GeoPlatform platform, String probeTaxonColumnName ) {
        Collection<String> organisms = platform.getOrganisms();
        Collection<Taxon> platformTaxa = new HashSet<Taxon>();
        StringBuffer taxaOnPlatform = new StringBuffer();

        if ( organisms.isEmpty() ) {
            return platformTaxa;
        }

        for ( String taxonScientificName : organisms ) {
            if ( taxonScientificName == null ) continue;
            taxaOnPlatform.append( ": " + taxonScientificName );
            // make sure add scientific name to map for platform
            if ( taxonScientificNameMap.containsKey( taxonScientificName ) ) {
                platformTaxa.add( taxonScientificNameMap.get( taxonScientificName ) );
            } else {
                platformTaxa.add( convertOrganismToTaxon( taxonScientificName ) );
            }
        }

        // multiple organisms are found on the platform yet there is no column defined to represent taxon for the
        // probes.
        if ( platformTaxa.size() > 1 && probeTaxonColumnName == null ) {
            throw new IllegalArgumentException( platformTaxa.size() + " taxon found on platform" + taxaOnPlatform
                    + " but there is no probe specific taxon Column found for platform " + platform );
        }
        // no platform organism given
        if ( platformTaxa.size() == 0 ) {
            throw new IllegalArgumentException( "No organisms found on platform  " + platform );
        }
        return platformTaxa;

    }

    /**
     * Retrieve taxon details for a probe given an abbreviation or scientific name. All scientific names should be in
     * the map as they were set there by the convertPlatform method. If the abbreviation is not found in the database
     * then stop processing as the organism name is likely to be an unknown abbreviation.
     * 
     * @param probeOrganism scientific name, common name or abbreviation of organism associated to a biosequence.
     * @return Taxon of biosequence.
     * @throws IllegalArgumentException taxon supplied has not been processed before, it does not match the scientific
     *         names used in platform definition and does not match a known abbreviation in the database.
     */
    private Taxon convertProbeOrganism( String probeOrganism ) {
        Taxon taxon = Taxon.Factory.newInstance();
        // Check if we have processed this organism before as defined by scientific or abbreviation definition.
        assert probeOrganism != null;

        /*
         * Detect blank taxon. We support 'n/a' here .... a little kludgy but shows up in some files.
         */
        if ( StringUtils.isBlank( probeOrganism ) || probeOrganism.equals( "n/a" ) ) {
            return null;
        }
        if ( taxonScientificNameMap.containsKey( probeOrganism ) ) {
            return taxonScientificNameMap.get( probeOrganism );
        }
        if ( taxonAbbreviationMap.containsKey( probeOrganism ) ) {
            return taxonAbbreviationMap.get( probeOrganism );
        }

        taxon.setAbbreviation( probeOrganism );
        // taxon not processed before check database.
        if ( taxonService != null ) {
            Taxon t = taxonService.findByAbbreviation( probeOrganism.toLowerCase() );

            if ( t != null ) {
                taxon = t;
                taxonAbbreviationMap.put( taxon.getAbbreviation(), t );
            } else {

                t = taxonService.findByCommonName( probeOrganism.toLowerCase() );

                if ( t != null ) {
                    taxon = t;
                    taxonAbbreviationMap.put( taxon.getAbbreviation(), t );
                } else {

                    // if probe organism can not be found i.e it is not a known abbreviation or scientific name
                    // and it was not already created during platform organism processing then warn user
                    throw new IllegalArgumentException( probeOrganism + " is not recognized as a taxon in Gemma" );
                }
            }
        }
        return taxon;

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
     * @param repType
     * @return
     */
    private VocabCharacteristic convertReplicatationType( ReplicationType repType ) {
        VocabCharacteristic result = VocabCharacteristic.Factory.newInstance();
        result.setCategory( "ReplicateDescriptionType" );
        result.setCategoryUri( MgedOntologyService.MGED_ONTO_BASE_URL + "#ReplicateDescriptionType" );
        result.setEvidenceCode( GOEvidenceCode.IIA );
        ExternalDatabase mged = ExternalDatabase.Factory.newInstance();
        mged.setName( "MGED Ontology" );
        mged.setType( DatabaseType.ONTOLOGY );

        if ( repType.equals( ReplicationType.biologicalReplicate ) ) {
            result.setValue( "biological_replicate" );
            result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "#biological_replicate" );
        } else if ( repType.equals( ReplicationType.technicalReplicateExtract ) ) {
            result.setValue( "technical_replicate" );
            result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "#technical_replicate" );
        } else if ( repType.equals( ReplicationType.technicalReplicateLabeledExtract ) ) {
            result.setValue( "technical_replicate" );
            result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "#technical_replicate" ); // MGED doesn't have
            // a
            // term to distinguish
            // these.
        } else {
            throw new IllegalStateException( "Unhandled replication type: " + repType );
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
        result.setType( FactorType.CATEGORICAL );
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
    private void convertReplicationToFactorValue( GeoReplication replication, ExperimentalFactor factor ) {
        FactorValue factorValue = convertReplicationToFactorValue( replication );
        factor.getFactorValues().add( factorValue );
    }

    /**
     * A Sample corresponds to a BioAssay; the channels correspond to BioMaterials.
     * 
     * @param sample
     */
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
             * So we need to just add the channel information to the biomaterials we have already. Note taxon is now
             * taken from sample
             */
            convertChannel( sample, channel, bioMaterial );
            bioAssay.getSamplesUsed().add( bioMaterial );
        }

        // Taxon lastTaxon = null;

        for ( GeoPlatform platform : sample.getPlatforms() ) {
            ArrayDesign arrayDesign;
            if ( seenPlatforms.containsKey( platform.getGeoAccession() ) ) {
                arrayDesign = seenPlatforms.get( platform.getGeoAccession() );
            } else {
                // platform not exist yet
                arrayDesign = convertPlatform( platform );
            }

            bioAssay.setArrayDesignUsed( arrayDesign );

        }

        return bioAssay;
    }

    /**
     * Convert a GEO series into one or more ExpressionExperiments. The more than one case comes up if the are platforms
     * from more than one organism represented in the series, ir if 'split by platform' is set. If the series is split
     * into two or more ExpressionExperiments, each refers to a modified GEO accession such as GSE2393.1, GSE2393.2 etc
     * for each organism/platform
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
        Map<GeoPlatform, Collection<GeoData>> platformDatasetMap = getPlatformDatasetMap( series );
        // get map of platform to dataset.

        if ( organismDatasetMap.size() > 1 ) {
            log.warn( "**** Multiple-species dataset! This data set will be split into one data set per species. ****" );
            int i = 1;
            for ( String organism : organismDatasetMap.keySet() ) {
                convertSpeciesSpecific( series, converted, organismDatasetMap, i, organism );
                i++;
            }
        } else if ( platformDatasetMap.size() > 1 && this.splitByPlatform ) {
            int i = 1;
            for ( GeoPlatform platform : platformDatasetMap.keySet() ) {
                convertByPlatform( series, converted, platformDatasetMap, i, platform );
                i++;
            }
        } else {
            converted.add( this.convertSeries( series, null ) );
        }

        return converted;
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
                o.setEvidenceCode( GOEvidenceCode.IIA );
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
            String bioMaterialDescription = BIOMATERIAL_DESCRIPTION_PREFIX;

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
                        bioMaterialDescription = bioMaterialDescription + "," + sample;
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
            log.warn( ( expectedNumSamples - actualNumSamples )
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
            log.debug( samples.size() + " samples on " + platform );
            convertVectorsForPlatform( geoSeries.getValues(), expExp, samples, platform );
            geoSeries.getValues().clear( platform );
        }

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
     * @param result
     * @param geoDataset
     */
    private void convertSubsetAssociations( ExpressionExperiment result, GeoDataset geoDataset ) {
        for ( GeoSubset subset : geoDataset.getSubsets() ) {
            if ( log.isDebugEnabled() ) log.debug( "Converting subset to experimentalFactor" + subset.getType() );
            convertSubsetToExperimentalFactor( result, subset );
        }
    }

    /**
     * Creates a new factorValue, or identifies an existing one, matching the subset. If it is a new one it adds it to
     * the given experimentalFactor.
     * 
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

        /* Check that there isn't already a factor value for this in the factor */

        for ( FactorValue fv : experimentalFactor.getFactorValues() ) {
            if ( fv.equals( factorValue ) ) {
                log.debug( factorValue + " is matched by existing factorValue for " + experimentalFactor );
                return fv;
            }
        }
        experimentalFactor.getFactorValues().add( factorValue );
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
     * Convert a variable into a ExperimentalFactor
     * 
     * @param variable
     * @return
     */
    private ExperimentalFactor convertVariableToFactor( GeoVariable variable ) {
        log.debug( "Converting variable " + variable.getType() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setName( variable.getType().toString() );
        result.setType( FactorType.CATEGORICAL );
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
     * @param factor
     */
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
            mgedTerm = "Compound"; // THERE IS no such term as 'Agent' in MGED.
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
            log.debug( "Processing " + quantitationType + " (column=" + quantitationTypeIndex
                    + " - according to sample, it's " + columnAccordingToSample + ")" );

            Map<String, List<Object>> dataVectors = makeDataVectors( values, datasetSamples, quantitationTypeIndex );

            if ( dataVectors == null || dataVectors.size() == 0 ) {
                log.debug( "No data for " + quantitationType + " (column=" + quantitationTypeIndex + ")" );
                continue;
            }
            log.info( dataVectors.size() + " data vectors for " + quantitationType );

            Object exampleValue = dataVectors.values().iterator().next().iterator().next();

            QuantitationType qt = QuantitationType.Factory.newInstance();
            qt.setName( quantitationType );
            String description = quantitationTypeDescriptions.get( columnAccordingToSample );
            qt.setDescription( description );
            QuantitationTypeParameterGuesser.guessQuantitationTypeParameters( qt, quantitationType, description,
                    exampleValue );

            int count = 0;
            int skipped = 0;
            for ( String designElementName : dataVectors.keySet() ) {
                List<Object> dataVector = dataVectors.get( designElementName );
                if ( dataVector == null || dataVector.size() == 0 ) continue;

                RawExpressionDataVector vector = convertDesignElementDataVector( geoPlatform, expExp,
                        bioAssayDimension, designElementName, dataVector, qt );

                if ( vector == null ) {
                    skipped++;
                    if ( log.isDebugEnabled() )
                        log.debug( "Null vector for DE=" + designElementName + " QT=" + quantitationType );
                    continue;
                }

                if ( log.isTraceEnabled() ) {
                    log.trace( designElementName + " " + qt.getName() + " " + qt.getRepresentation() + " "
                            + dataVector.size() + " elements in vector" );
                }

                expExp.getRawExpressionDataVectors().add( vector );

                if ( ++count % LOGGING_VECTOR_COUNT_UPDATE == 0 && log.isDebugEnabled() ) {
                    log.debug( count + " Data vectors added" );
                }
            }

            if ( count > 0 ) {
                expExp.getQuantitationTypes().add( qt );
                if ( log.isDebugEnabled() && count > 1000 ) {
                    log.debug( count + " Data vectors added for '" + quantitationType + "'" );
                }
            } else {
                log.info( "No vectors were retained for " + quantitationType
                        + " -- usually this is due to all values being missing." );
            }

            if ( skipped > 0 ) {
                log.info( "Skipped " + skipped + " vectors" );
            }
        }
        log.info( "Total of " + expExp.getRawExpressionDataVectors().size() + " vectors on platform " + geoPlatform
                + ", " + expExp.getQuantitationTypes().size() + " quantitation types." );
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
        } else if ( technology == null ) {
            log.warn( "No technology type available for " + platform + ", provisionally setting to 'dual mode'" );
            arrayDesign.setTechnologyType( TechnologyType.DUALMODE );
        } else {
            throw new IllegalArgumentException( "Don't know how to interpret technology type " + technology );
        }
        return arrayDesign;
    }

    private BioSequence createMinimalBioSequence( Taxon taxon ) {
        BioSequence bs = BioSequence.Factory.newInstance();
        bs.setTaxon( taxon );
        bs.setPolymerType( PolymerType.DNA );
        bs.setType( SequenceType.DNA );
        return bs;
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
                log.debug( string + " appears to indicate the  probe descriptions in column " + index
                        + " for platform " + platform );
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
     * Allow multiple taxa for a platform. Method retrieves from parsed GEO file the header column name which contains
     * the species/organism used to create probe.
     * 
     * @param platform Parsed GEO platform details.
     * @return Column name in GEO used to identify column containing species/organism used to create probe
     */
    private String determinePlatformProbeOrganismColumn( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String columnName : columnNames ) {
            if ( GeoConstants.likelyProbeOrganism( columnName ) ) {
                log.debug( "'" + columnName + "' appears to indicate the sequences in column " + index
                        + " for platform " + platform );
                return columnName;
            }
            index++;
        }
        log.debug( "No platform organism description column found for " + platform );
        return null;
    }

    /**
     * @param platform
     * @return
     */
    private String determinePlatformSequenceColumn( GeoPlatform platform ) {
        Collection<String> columnNames = platform.getColumnNames();
        int index = 0;
        for ( String columnName : columnNames ) {
            if ( GeoConstants.likelySequence( columnName ) ) {
                log.debug( "'" + columnName + "' appears to indicate the sequences in column " + index
                        + " for platform " + platform );
                return columnName;
            }
            index++;
        }
        log.debug( "No platform sequence description column found for " + platform );
        return null;
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
     * @param series
     * @param i
     * @return
     */
    private String getBiomaterialPrefix( GeoSeries series, int i ) {
        String bioMaterialName = series.getGeoAccession() + BIOMATERIAL_NAME_TAG + i;
        return bioMaterialName;
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

    private String getExternalAccession( List<List<String>> externalRefs, int i ) {
        for ( List<String> refs : externalRefs ) {
            if ( StringUtils.isNotBlank( refs.get( i ) ) ) {
                return refs.get( i );
            }
        }
        return null;
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
                        + StringUtils.join( sample.getPlatforms().toArray(), "," );
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
     * @return
     */
    private Map<GeoPlatform, Collection<GeoData>> getPlatformDatasetMap( GeoSeries series ) {
        Map<GeoPlatform, Collection<GeoData>> platforms = new HashMap<GeoPlatform, Collection<GeoData>>();

        if ( series.getDatasets() == null || series.getDatasets().size() == 0 ) {
            for ( GeoSample sample : series.getSamples() ) {
                assert sample.getPlatforms().size() > 0 : sample + " has no platform";
                assert sample.getPlatforms().size() == 1 : sample + " has multiple platforms: "
                        + StringUtils.join( sample.getPlatforms().toArray(), "," );
                GeoPlatform platform = sample.getPlatforms().iterator().next();

                if ( platforms.get( platform ) == null ) {
                    platforms.put( platform, new HashSet<GeoData>() );
                }
                // This is a bit silly, but made coding this easier.
                platforms.get( platform ).add( sample.getPlatforms().iterator().next() );
            }
        } else {
            for ( GeoDataset dataset : series.getDatasets() ) {
                GeoPlatform platform = dataset.getPlatform();
                if ( platforms.get( platform ) == null ) {
                    platforms.put( platform, new HashSet<GeoData>() );
                }
                platforms.get( platform ).add( dataset );
            }
        }
        return platforms;
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

        if ( matchingFactorValue == null ) {
            throw new IllegalStateException( "Could not find matching factor value for " + variable
                    + " in experimental design for sample " + bioMaterial );
        }

        // make sure we don't put the factor value on more than once.
        if ( alreadyHasFactorValueForFactor( bioMaterial, matchingFactorValue.getExperimentalFactor() ) ) {
            return;
        }

        bioMaterial.getFactorValues().add( matchingFactorValue );

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
     * Sanity check.
     * 
     * @param datasetSamples
     */
    private void sanityCheckQuantitationTypes( List<GeoSample> datasetSamples ) {
        List<String> reference = new ArrayList<String>();

        // Choose a reference that is populated ...
        for ( GeoSample sample : datasetSamples ) {
            reference = sample.getColumnNames();
            if ( !reference.isEmpty() ) break;
        }

        if ( reference.isEmpty() ) {
            throw new IllegalStateException( "None of the samples have any quantitation type names" );
        }

        boolean someDidntMatch = false;
        String lastError = "";
        for ( GeoSample sample : datasetSamples ) {
            List<String> columnNames = sample.getColumnNames();

            assert !columnNames.isEmpty();

            if ( !reference.equals( columnNames ) ) {

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

                lastError = "*** Sample quantitation type names do not match: " + buf.toString();
                log.debug( lastError );
            }
        }
        if ( someDidntMatch ) {
            log.warn( "Samples do not have consistent quantification type names. Last error was: " + lastError );
        }
    }

    /**
     * @param mgedTerm
     * @return
     */
    private VocabCharacteristic setCategory( String mgedTerm ) {
        VocabCharacteristic categoryTerm = VocabCharacteristic.Factory.newInstance();
        categoryTerm.setCategory( mgedTerm );
        categoryTerm.setCategoryUri( MgedOntologyService.MGED_ONTO_BASE_URL + "#" + mgedTerm );
        categoryTerm.setEvidenceCode( GOEvidenceCode.IIA );
        return categoryTerm;
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

}