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
package ubic.gemma.loader.expression.mage;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.Array.Array;
import org.biomage.Array.ArrayManufacture;
import org.biomage.ArrayDesign.ArrayDesign;
import org.biomage.ArrayDesign.CompositeGroup;
import org.biomage.ArrayDesign.PhysicalArrayDesign;
import org.biomage.ArrayDesign.ReporterGroup;
import org.biomage.AuditAndSecurity.Contact;
import org.biomage.AuditAndSecurity.Organization;
import org.biomage.AuditAndSecurity.Person;
import org.biomage.BQS.BibliographicReference;
import org.biomage.BioAssay.BioAssay;
import org.biomage.BioAssay.BioAssayCreation;
import org.biomage.BioAssay.BioAssayTreatment;
import org.biomage.BioAssay.Channel;
import org.biomage.BioAssay.DerivedBioAssay;
import org.biomage.BioAssay.FeatureExtraction;
import org.biomage.BioAssay.MeasuredBioAssay;
import org.biomage.BioAssay.PhysicalBioAssay;
import org.biomage.BioAssayData.BioAssayData;
import org.biomage.BioAssayData.BioAssayDimension;
import org.biomage.BioAssayData.BioAssayMap;
import org.biomage.BioAssayData.BioDataCube;
import org.biomage.BioAssayData.BioDataTuples;
import org.biomage.BioAssayData.BioDataValues;
import org.biomage.BioAssayData.CompositeSequenceDimension;
import org.biomage.BioAssayData.DataExternal;
import org.biomage.BioAssayData.DerivedBioAssayData;
import org.biomage.BioAssayData.DesignElementDimension;
import org.biomage.BioAssayData.FeatureDimension;
import org.biomage.BioAssayData.MeasuredBioAssayData;
import org.biomage.BioAssayData.QuantitationTypeDimension;
import org.biomage.BioAssayData.ReporterDimension;
import org.biomage.BioAssayData.Transformation;
import org.biomage.BioMaterial.BioMaterialMeasurement;
import org.biomage.BioMaterial.BioSample;
import org.biomage.BioMaterial.BioSource;
import org.biomage.BioMaterial.LabeledExtract;
import org.biomage.Common.Describable;
import org.biomage.Common.Extendable;
import org.biomage.Description.Database;
import org.biomage.Description.Description;
import org.biomage.Description.OntologyEntry;
import org.biomage.DesignElement.Feature;
import org.biomage.DesignElement.FeatureInformation;
import org.biomage.DesignElement.FeatureReporterMap;
import org.biomage.DesignElement.ReporterCompositeMap;
import org.biomage.Experiment.Experiment;
import org.biomage.Experiment.ExperimentDesign;
import org.biomage.Measurement.Unit;
import org.biomage.Measurement.Measurement.KindCV;
import org.biomage.Measurement.Measurement.Type;
import org.biomage.Protocol.Protocol;
import org.biomage.QuantitationType.ConfidenceIndicator;
import org.biomage.QuantitationType.DerivedSignal;
import org.biomage.QuantitationType.Failed;
import org.biomage.QuantitationType.MeasuredSignal;
import org.biomage.QuantitationType.PValue;
import org.biomage.QuantitationType.PresentAbsent;
import org.biomage.QuantitationType.QuantitationType;
import org.biomage.QuantitationType.Ratio;
import org.biomage.QuantitationType.SpecializedQuantitationType;
import org.biomage.tools.ontology.OntologyHelper;

import ubic.gemma.loader.expression.arrayDesign.Reporter;
import ubic.gemma.loader.expression.geo.QuantitationTypeParameterGuesser;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementKind;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.protocol.ProtocolApplication;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Compound;
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
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.ReflectionUtil;

/**
 * <p>
 * Class to convert Mage domain objects to Gemma domain objects. In most cases, the user can simply call the "convert"
 * method on any MAGE domain object and get a fully-populated Gemma domain object. There is no need to use the methods
 * in this class directly when handling MAGE-ML files: use the {@link ubic.gemma.model.loader.mage.MageMLParser.}
 * </p>
 * <h2>Implementation notes</h2>
 * <p>
 * Most MAGE objects have a corresponding method in this class called 'convertXXXXX', and many have a
 * 'convertXXXXAssociations' to handle the associations. Special cases (outlined below) have additional methods to map
 * MAGE associations to Gemma objects.
 * </p>
 * <h2>Zoo of packages that have references between them, but don't map directly to Gemma</h2> <h3>DesignElement_package
 * and ArrayDesign_package</h3>
 * <p>
 * DesignElement Contains the ReporterCompositeMap (and the FeatureReporterMap). This allows us to fill in the map in
 * the CompositeSequence.
 * </p>
 * <p>
 * ArrayDesign Contains the CompositeGropus, FeatureGroups and ReporterGroups. This leads us to a description of all the
 * DesignElement on the array, but not the reportercompositemap.
 * </p>
 * <p>
 * Thus both of these have references to CompositeSequences for example; the two packages can be in different files.
 * Therefore we need to fill in object that have the same Identifier with data from the other file.
 * </p>
 * <h3>BioAssay and BioAssayData</h3>
 * <p>
 * Gemma dispenses with the notion of a distinct BioAssayData object; BioAssays are directly associated to their
 * (external) data files, and in the database the actual data are stored as DataVectors associated with an
 * ExpressionExperiment. In contrast, Mage has a highly complex hierarchy of BioAssays and BioAssayData. In addition,
 * Gemma has only one type of BioAssay, which combines the features needed from the Physical, Measured and Derived
 * bioassays. (The Gemma BioAssay most closely resembles the DerivedBioAssay). Therefore we have to gather information
 * from the various Mage BioAssays and BioAssayData and put it all in one Gemma BioAssay object.
 * </p>
 * <h3>BioMaterial and subclasses</h3>
 * <p>
 * In Gemma we only have BioMaterial, not distinct BioSample, BioSource and LabeledExtract objects.
 * </p>
 * 
 * @see ubic.gemma.loader.expression.mage.MageMLParser
 * @author pavlidis
 * @version $Id$
 */
@SuppressWarnings("unchecked")
public class MageMLConverterHelper {

    /**
     * Used to indicate that a MAGE list should be converted to a Gemma list (or collection)
     */
    public static final boolean CONVERT_ALL = false;

    /**
     * Used to indicate that when a MAGE list is encountered, we should only process the first element of the list.
     */
    public static final boolean CONVERT_FIRST_ONLY = true;

    private static final String ARRAY_EXPRESS_LOCAL_DATAFILE_BASEPATH = "arrayExpress.local.datafile.basepath";

    /*
     * The weird second format shows up in at least one mage-ml file...I don't think it's supposed to be a literal Z but
     * that's what's in the date.
     */
    private static final String[] DATE_FORMATS = new String[] { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'hh-mm-ss'Z'" };

    private static Log log = LogFactory.getLog( MageMLConverterHelper.class.getName() );

    /**
     * 
     */
    private static final String MGED_DATABASE_IDENTIFIER = "MGED Ontology";

    private static final String MGED_ONTOLOGY_URL = MgedOntologyService.MGED_ONTO_BASE_URL;

    /**
     * Different ways to refer to the MAGE Ontology
     */
    public Set<String> mgedOntologyAliases;

    /**
     * Array ids to bioassays
     */
    Map<String, Collection<BioAssay>> array2BioAssay = new HashMap<String, Collection<BioAssay>>();

    private static ExternalDatabase arrayExpress = null;

    Map<String, Collection<FactorValue>> bioAssayFactors = new HashMap<String, Collection<FactorValue>>();
    /**
     * One of many ways to figure out how the heck samples are related to bioassays. Key is identifier for the
     * 'bioassaymaptarget' (that is, a bioassay)
     */
    Map<String, Collection<BioAssay>> bioAssayMap = new HashMap<String, Collection<BioAssay>>();

    Map<org.biomage.Experiment.FactorValue, MeasuredBioAssay> factors2MeasuredBioAssays = new HashMap<org.biomage.Experiment.FactorValue, MeasuredBioAssay>();

    Map<String, FactorValue> fvMap = new HashMap<String, FactorValue>();

    Set<String> missingFiles = new HashSet<String>();

    /**
     * Used in cases where there is no link from a derived or measured bioassay to the biosource; instead, it is via
     * biosource <- <- <- <- <- physicalbioassay -> treatment -> target -> physicalbioassay <- measuredbioassay <-
     * derivedbioassay (no, I'm not making that up)
     */
    Map<String, PhysicalBioAssay> physicalBioAssay2physicalBioAssay = new HashMap<String, PhysicalBioAssay>();

    /**
     * Stores the dimension information for the bioassays
     */
    private BioAssayDimensions bioAssayDimensions;

    /*
     * This is a global variable.
     */
    private boolean haveInitializedMgedOntologyHelper = false;

    private Map<String, String> id2Name = new HashMap<String, String>();

    /**
     * Places where, according to the current configuration, local MAGE bioDataCube external files are stored.
     */
    private Collection<String> localExternalDataPaths;

    private ExternalDatabase mgedOntology;

    /**
     * 
     */
    private OntologyHelper mgedOntologyHelper;

    /**
     * MAGE IDs of the bioassays we should retain.
     */
    private Collection<String> topLevelBioAssayIdentifiers = new HashSet<String>();

    /**
     * Constructor
     */
    public MageMLConverterHelper() {
        bioAssayDimensions = new BioAssayDimensions();

        initMGEDOntologyAliases();

        initLocalExternalDataPaths();

    }

    /**
     * A generic converter that figures out which specific conversion method to call based on the class of the object.
     * 
     * @param mageObj
     * @return
     */
    public Object convert( Object mageObj ) {
        log.debug( "Converting " + mageObj.getClass().getSimpleName() );
        return findAndInvokeConverter( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.description.Characteristic convertAction( OntologyEntry mageObj ) {
        Collection<Characteristic> results = convertOntologyEntry( mageObj );
        if ( results == null || results.isEmpty() ) {
            return null;
        }

        if ( results.size() > 1 ) {
            log.warn( "Multiple resulting characteristics for " + mageObj );
        }
        return results.iterator().next();
    }

    /**
     * Useful because the Array count is the actual assay count.
     * 
     * @param mageObj
     */
    public void convertArray( Array mageObj ) {
        log.debug( "Array..." + mageObj.getIdentifier() );
        this.array2BioAssay.put( mageObj.getIdentifier(), new HashSet<BioAssay>() );
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign convertArrayDesign( ArrayDesign mageObj ) {
        if ( mageObj == null ) return null;
        ubic.gemma.model.expression.arrayDesign.ArrayDesign result = ubic.gemma.model.expression.arrayDesign.ArrayDesign.Factory
                .newInstance();
        Integer numFeatures = mageObj.getNumberOfFeatures();
        if ( numFeatures != null ) result.setAdvertisedNumberOfDesignElements( numFeatures.intValue() );

        // convert the identifier into a external reference. We can use this to fetch the array design later.
        String accession = mageObj.getIdentifier();

        DatabaseEntry acc = DatabaseEntry.Factory.newInstance();
        acc.setAccession( accession );
        acc.setExternalDatabase( getArrayExpressReference() );

        result.getExternalReferences().add( acc );

        convertIdentifiable( mageObj, result );
        result.setShortName( result.getName() );
        convertAssociations( mageObj, result );

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertArrayDesignAssociations( ArrayDesign mageObj,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "CompositeGroups" ) ) {
            assert associatedObject instanceof List;
            specialConvertCompositeGroups( ( List<CompositeGroup> ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "DesignProviders" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "FeatureGroups" ) ) {
            // assert associatedObject instanceof List;
            // specialConvertFeatureGroups( ( List ) associatedObject, gemmaObj );
            // no-op
        } else if ( associationName.equals( "ProtocolApplications" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "ReporterGroups" ) ) {
            assert associatedObject instanceof List;
            // specialConvertReporterGroups( ( List ) associatedObject, gemmaObj );
            // no longer needed
        } else {
            log.debug( "Unsupported or unknown association, or it belongs to the subclass: " + associationName );
        }
    }

    /**
     * A no-op, as we don't keep track of this.
     * 
     * @param mageObj
     * @return
     */
    public Object convertArrayManufacture( ArrayManufacture mageObj ) {
        if ( mageObj == null ) return null;
        return null; // no-op.
    }

    /**
     * @param actualGemmaAssociationName
     * @param gemmaAssociatedObj
     * @return
     */
    public String convertAssociationName( String actualGemmaAssociationName, Object gemmaAssociatedObj ) {
        String inferredGemmaAssociationName;
        if ( actualGemmaAssociationName != null ) {
            inferredGemmaAssociationName = actualGemmaAssociationName;
        } else {
            inferredGemmaAssociationName = ReflectionUtil.getBaseForImpl( gemmaAssociatedObj ).getSimpleName();
        }
        return inferredGemmaAssociationName;
    }

    /**
     * Generic method to find the associations a Mage object has and call the appropriate converter method. The
     * converters are named after the MAGE class, not the Gemma class.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertAssociations( Object mageObj, Object gemmaObj ) {
        log.debug( "Converting associations of " + mageObj.getClass().getSimpleName() + " into associations for Gemma "
                + gemmaObj.getClass().getSimpleName() );
        convertAssociations( mageObj.getClass(), mageObj, gemmaObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.description.BibliographicReference convertBibliographicReference(
            BibliographicReference mageObj ) {
        if ( mageObj == null ) return null;
        ubic.gemma.model.common.description.BibliographicReference result = ubic.gemma.model.common.description.BibliographicReference.Factory
                .newInstance();
        convertDescribable( mageObj, result );
        result.setEditor( mageObj.getEditor() );
        result.setAuthorList( mageObj.getAuthors() );
        result.setIssue( mageObj.getIssue() );
        result.setPages( mageObj.getPages() );
        result.setPublication( mageObj.getPublication() );
        result.setPublisher( mageObj.getPublisher() );
        result.setTitle( mageObj.getTitle() );
        result.setVolume( mageObj.getVolume() );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBibliographicReferenceAssociations( BibliographicReference mageObj,
            ubic.gemma.model.common.description.BibliographicReference gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Accessions" ) ) {
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "PubAccession" );
        } else if ( associationName.equals( "Parameters" ) ) {
            // no-op, we don't support.
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay convertBioAssay( BioAssay mageObj ) {
        if ( mageObj == null ) return null;
        ubic.gemma.model.expression.bioAssay.BioAssay result = ubic.gemma.model.expression.bioAssay.BioAssay.Factory
                .newInstance();

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioAssayAssociations( BioAssay mageObj, ubic.gemma.model.expression.bioAssay.BioAssay gemmaObj,
            Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "BioAssayFactorValues" ) ) {
            // Note that these are be the same factorvalues as referred to by the experimentalfactors.
            // simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Channels" ) ) {
            // we don't support this.
        } else {
            log.warn( "Unsupported or unknown bioassay association: " + associationName );
        }
    }

    /**
     * Special case. We don't have a BioAssayData object, just BioAssays.
     * <p>
     * Mage BioAssayData comes in two flavors: BioDataCubes, and BioDataTuples.BioDataCubes, instead of having all three
     * dimensions, tend to have only two: QuantitationType and DesignElement. (Waiting to see an exception to this -
     * certainly allowed by MAGE, but in practice...?)
     * <p>
     * There are two subclasses, Derived and Measured. It appers that the former is much more common. Measured
     * bioassaydata are the 'raw' data.
     * 
     * @param mageObj BioAssayData to be converted.
     * @return a LocalFile object representing the external data file. Unfortunately, the path stored here is often
     *         useless and must be filled in later.
     * @see specialConvertBioAssayBioAssayDataAssociations
     */
    public LocalFile convertBioAssayData( BioAssayData mageObj ) {
        // convertBioAssayDataAssociations( mageObj ); // FIXME this is now not needed so long as we are using processed
        // data.
        BioDataValues data = mageObj.getBioDataValues();
        LocalFile result = LocalFile.Factory.newInstance();
        result.setRemoteURL( null );
        result.setLocalURL( null );

        if ( data == null ) {
            return null;
        } else if ( data instanceof BioDataCube ) {
            DataExternal dataExternal = ( ( BioDataCube ) data ).getDataExternal();
            if ( dataExternal == null ) {
                log.warn( "BioDataCube with no external data" );
                return null;
            }

            URL localURL = findLocalMageExternalDataFile( dataExternal.getFilenameURI() );
            if ( localURL == null ) {

                // // keep from getting warned multiple times.
                // if ( log.isWarnEnabled() && !missingFiles.contains( dataExternal.getFilenameURI() ) ) {
                // log.warn( "External data file " + dataExternal.getFilenameURI()
                // + " not found; Data derived from MAGE BioAssayData " + mageObj.getName()
                // + " will not have reachable external data." );
                // missingFiles.add( dataExternal.getFilenameURI() );
                //
                // }

                // key part...local file is null.
                return null;
            }

            result.setLocalURL( localURL );

        } else if ( data instanceof BioDataTuples ) {
            log.error( "Not ready to deal with BioDataTuples from Mage" );
            return null;
        } else {
            throw new IllegalArgumentException( "Unknown BioDataValue class: " + data.getClass() );
        }

        return result;
    }

    /**
     * In a typical MAGE file, we have the following associations:
     * <p>
     * MeasuredBioAssayData->BioDataCube->DataExternal
     * </p>
     * and/or
     * <p>
     * DerivedBioAssayData->BioDataCube->DataExternal
     * </p>
     * <p>
     * The dimensions (designelement, quantitationtype and bioassay) are not full-scale Gemma objects, but we need to
     * extract this information so we can decipher the data files. However, the FeatureDimension is often not available
     * in the MAGE file (for example, in the ArrayExpress MAGE files for experiments done on Affymetrix platforms). In
     * these cases we have to get the feature dimension from somewhere else.
     * <p>
     * In MAGE, there can be more than one QuantitationType dimension associated with a single BioAssay, because there
     * can be more than one BioAssayData (Measured, Derived, Physical).
     * <p>
     * External data files contain compositesequence or reporter or feature data.
     * 
     * @param mageObj
     */
    public void convertBioAssayDataAssociations( BioAssayData mageObj ) {
        QuantitationTypeDimension qtd = mageObj.getQuantitationTypeDimension();
        List<ubic.gemma.model.common.quantitationtype.QuantitationType> convertedQuantitationTypes = new ArrayList<ubic.gemma.model.common.quantitationtype.QuantitationType>();
        if ( qtd != null ) {
            List<QuantitationType> quantitationTypes = qtd.getQuantitationTypes();
            for ( QuantitationType type : quantitationTypes ) {
                ubic.gemma.model.common.quantitationtype.QuantitationType convertedType = convertQuantitationType( type );
                if ( log.isDebugEnabled() ) log.debug( "Found quantitationtype: " + convertedType.getName() );
                convertedQuantitationTypes.add( convertedType );
            }
        }

        DesignElementDimension ded = mageObj.getDesignElementDimension();
        List<org.biomage.DesignElement.DesignElement> designElements = null;

        // Note: in general the bioassaydim will have only a single bioassay, except for the one that has the processed
        // data.
        BioAssayDimension bioAssayDim = mageObj.getBioAssayDimension();
        if ( bioAssayDim == null ) return;

        List<BioAssay> bioAssays = bioAssayDim.getBioAssays();
        log.info( "Bioassaydimension has " + bioAssays.size() + " assays" );
        for ( BioAssay bioAssay : bioAssays ) {
            if ( ded instanceof FeatureDimension ) {
                if ( log.isDebugEnabled() ) log.debug( "Got a feature dimension: " + ded.getIdentifier() );
                designElements = ( ( FeatureDimension ) ded ).getContainedFeatures();
            } else if ( ded instanceof CompositeSequenceDimension ) {
                if ( log.isDebugEnabled() ) log.debug( "Got a compositesequence dimension: " + ded.getIdentifier() );
                designElements = ( ( CompositeSequenceDimension ) ded ).getCompositeSequences();
            } else if ( ded instanceof ReporterDimension ) {
                if ( log.isDebugEnabled() ) log.debug( "Got a reporter dimension: " + ded.getIdentifier() );
                designElements = ( ( ReporterDimension ) ded ).getReporters();
            } else {
                throw new UnsupportedOperationException( "Unrecognized class: " + ded.getClass() );
            }

            List<CompositeSequence> convertedDesignElements = new ArrayList<CompositeSequence>();
            for ( org.biomage.DesignElement.DesignElement designElement : designElements ) {
                CompositeSequence de = convertDesignElement( designElement );
                if ( de == null ) continue;
                convertedDesignElements.add( de );
            }

            if ( convertedDesignElements.size() == 0 ) {
                // This happens with affymetrix raw data, which don't have explicit probe names. Not much to do about
                // it.
            }

            ubic.gemma.model.expression.bioAssay.BioAssay convertedBioAssay = ubic.gemma.model.expression.bioAssay.BioAssay.Factory
                    .newInstance();

            convertIdentifiable( bioAssay, convertedBioAssay );

            log.info( "Adding bioassaydimensions for " + convertedBioAssay );
            bioAssayDimensions.addDesignElementDimension( convertedBioAssay, convertedDesignElements );
            bioAssayDimensions.addQuantitationTypeDimension( convertedBioAssay, convertedQuantitationTypes );

        }

    }

    /**
     * Convert a MAGE-OM BioAssayDimension into a Gemma one. This is usually the way we get to the bioassays.
     * 
     * @param bad
     * @return
     */
    public ubic.gemma.model.expression.bioAssayData.BioAssayDimension convertBioAssayDimension( BioAssayDimension bad ) {

        ubic.gemma.model.expression.bioAssayData.BioAssayDimension resultBioAssayDimension = ubic.gemma.model.expression.bioAssayData.BioAssayDimension.Factory
                .newInstance();

        convertIdentifiable( bad, resultBioAssayDimension );

        Collection<BioAssay> bioAssayList = bad.getBioAssays();

        for ( BioAssay sample : bioAssayList ) {
            ubic.gemma.model.expression.bioAssay.BioAssay resultBioAssay;

            if ( sample instanceof MeasuredBioAssay ) {
                resultBioAssay = convertMeasuredBioAssay( ( MeasuredBioAssay ) sample );

                if ( log.isDebugEnabled() ) log.debug( "Adding from measured: " + resultBioAssay );
            } else if ( sample instanceof DerivedBioAssay ) {
                /*
                 * Note that this (sometimes) is the bioassaydimension we actually want, if the "measured" bioassay is
                 * also present.
                 */
                resultBioAssay = convertDerivedBioAssay( ( DerivedBioAssay ) sample );
                if ( log.isDebugEnabled() ) log.debug( "Adding from derived: " + resultBioAssay );

            } else {
                // physicalbioassay
                resultBioAssay = convertBioAssay( sample );
                if ( log.isDebugEnabled() ) log.debug( "Adding from physicalbioassay: " + resultBioAssay );
            }

            if ( resultBioAssay != null ) {
                resultBioAssayDimension.getBioAssays().add( resultBioAssay );
            }

        }
        if ( log.isDebugEnabled() )
            log.debug( resultBioAssayDimension.getBioAssays().size() + " bioassays in dimension " + bad.getIdentifier() );
        return resultBioAssayDimension;
    }

    /**
     * @param map
     */
    public void convertBioAssayMap( BioAssayMap map ) {
        String identifier = map.getBioAssayMapTarget().getIdentifier();
        if ( !bioAssayMap.containsKey( identifier ) ) {
            bioAssayMap.put( identifier, new HashSet<BioAssay>() );
        }
        bioAssayMap.get( identifier ).addAll( map.getSourceBioAssays() );
    }

    /**
     * Note that LabeledExtracts are where we get the BioMaterial information from (directly). Information from other
     * subclasses of org.biomage.BioMaterial.BioMaterial has to be merged in here.
     * 
     * @param mageObj
     * @return
     * @see convertLabeledExtract
     * @see convertBioSource
     * @see convertBioMaterialAssociations
     */
    public ubic.gemma.model.expression.biomaterial.BioMaterial convertBioMaterial(
            org.biomage.BioMaterial.BioMaterial mageObj ) {
        if ( mageObj == null ) return null;
        ubic.gemma.model.expression.biomaterial.BioMaterial resultBioMaterial = ubic.gemma.model.expression.biomaterial.BioMaterial.Factory
                .newInstance();

        convertIdentifiable( mageObj, resultBioMaterial );
        convertAssociations( mageObj, resultBioMaterial );

        /*
         * If it is a labeledExtract, get the treatment -> sourcebiomaterials and samples and merge them in into the
         * current. This is important as we get the Taxon for the biomaterial this way.
         */
        if ( mageObj instanceof LabeledExtract ) {
            LabeledExtract mageEx = ( LabeledExtract ) mageObj;
            for ( Object o : mageEx.getTreatments() ) {
                org.biomage.BioMaterial.Treatment treatment = ( org.biomage.BioMaterial.Treatment ) o;

                // drill down to biosamples and to biosources, recursively.
                for ( Object p : treatment.getSourceBioMaterialMeasurements() ) {
                    BioMaterialMeasurement bmm = ( BioMaterialMeasurement ) p;

                    org.biomage.BioMaterial.BioMaterial bm = bmm.getBioMaterial();

                    if ( !( bm instanceof BioSample ) ) {
                        throw new UnsupportedOperationException( "Didn't expect a " + bm.getClass().getName() );
                    }
                    BioSample bsample = ( BioSample ) bm;

                    processBioSampleCharacteristics( resultBioMaterial, bsample );

                }
            }

            if ( resultBioMaterial.getSourceTaxon() == null ) {
                convertCharacteristicToTaxon( resultBioMaterial );
            }

        } else if ( resultBioMaterial.getSourceTaxon() == null && mageObj instanceof BioSource ) {
            convertCharacteristicToTaxon( resultBioMaterial );
        }

        return resultBioMaterial;
    }

    /**
     * @param resultBioMaterial
     * @return
     */
    private Characteristic convertCharacteristicToTaxon(
            ubic.gemma.model.expression.biomaterial.BioMaterial resultBioMaterial ) {
        // explicitly convert the taxon over.
        Characteristic found = null;
        for ( Characteristic character : resultBioMaterial.getCharacteristics() ) {

            assert character.getCategory() != null;

            if ( character.getCategory().equals( "Organism" ) ) {
                String scientificName = character.getValue();
                Taxon t = Taxon.Factory.newInstance();
                t.setScientificName( scientificName );
                t.setIsSpecies( true );
                t.setIsGenesUsable( false );
                resultBioMaterial.setSourceTaxon( t );
                found = character;
                break;
            }
        }

        if ( found == null && log.isWarnEnabled() ) {
            log.warn( "There is no organism information available for " + resultBioMaterial );
        } else {

            /*
             * remove the taxon characteristic from the biomaterial - it is redundant.
             */
            resultBioMaterial.getCharacteristics().remove( found );

            if ( log.isDebugEnabled() )
                log.debug( "Found " + resultBioMaterial.getSourceTaxon() + " from ontology entries for "
                        + resultBioMaterial );
        }
        return found;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertBioMaterial
     */
    public void convertBioMaterialAssociations( org.biomage.BioMaterial.BioMaterial mageObj, BioMaterial gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        assert mageObj != null;
        if ( associationName.equals( "Characteristics" ) ) {
            // specialConvertBioMaterialBioCharacteristics( mageObj, gemmaObj );
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "MaterialType" ) ) { // characteristic
            simpleFillIn( associatedObject, gemmaObj, getter, "MaterialType", Characteristic.class );
        } else if ( associationName.equals( "QualityControlStatistics" ) ) {
            assert associatedObject instanceof List;
            // we don't support
        } else if ( associationName.equals( "Treatments" ) ) {
            assert associatedObject instanceof List;
            // specialConvertBioMaterialTreatmentAssociations(
            // ( List<org.biomage.BioMaterial.Treatment> ) associatedObject, gemmaObj );
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else {
            log.debug( "Unsupported or unknown association, or from subclass: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public BioMaterial convertBioSample( BioSample mageObj ) {
        if ( mageObj == null ) return null;

        BioMaterial result = BioMaterial.Factory.newInstance();

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertBioMaterialAssociations
     */
    public void convertBioSampleAssociations( BioSample mageObj, BioMaterial gemmaObj, Method getter ) {
        if ( mageObj == null ) return;
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );
        Object associatedObject = intializeConversion( mageObj, getter );
        if ( associatedObject == null ) return;
        String associationName = getterToPropertyName( getter );
        if ( associationName.equals( "Type" ) ) {
            // we don't support.
        } else {
            if ( log.isDebugEnabled() )
                log.debug( "Unknown or unsupported type, or is for superclass " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return ubic.gemma.model.sequence.biosequence.BioSequence
     */
    public ubic.gemma.model.genome.biosequence.BioSequence convertBioSequence(
            org.biomage.BioSequence.BioSequence mageObj ) {
        if ( mageObj == null ) return null;

        ubic.gemma.model.genome.biosequence.BioSequence result = ubic.gemma.model.genome.biosequence.BioSequence.Factory
                .newInstance();

        result.setSequence( mageObj.getSequence() );
        if ( mageObj.getLength() != null ) result.setLength( mageObj.getLength().longValue() );
        if ( mageObj.getIsApproximateLength() != null )
            result.setIsApproximateLength( mageObj.getIsApproximateLength().booleanValue() );
        if ( mageObj.getIsCircular() != null ) result.setIsCircular( mageObj.getIsCircular().booleanValue() );

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * Implementation note: This is passed a 'get' method for an association to one or more MAGE domain objects. In some
     * cases, the subsequent call can be computed using reflection; but in other cases it will have to be done by hand,
     * as when there is not a direct mapping of Gemma objects to MAGE objects and vice versa.
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioSequenceAssociations( org.biomage.BioSequence.BioSequence mageObj,
            ubic.gemma.model.genome.biosequence.BioSequence gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        if ( associatedObject == null ) return;
        String associationName = getterToPropertyName( getter );

        if ( associationName.equals( "PolymerType" ) ) { // Ontology Entry - enumerated type.
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "SequenceDatabases" ) ) { // list of DatabaseEntries, we use one
            assert ( associatedObject instanceof List );
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY,
                    "SequenceDatabaseEntry" );
        } else if ( associationName.equals( "Type" ) ) { // ontology entry, we map to a enumerated type.
            assert associatedObject instanceof OntologyEntry;
            specialConvertSequenceType( ( OntologyEntry ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "Species" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "Taxon" );
        } else if ( associationName.equals( "SeqFeatures" ) ) {
            // list of Sequence features, we ignore
        } else if ( associationName.equals( "OntologyEntries" ) ) {
            // list of generic ontology entries, we ignore.
        } else {
            log.debug( "Unknown or unsupported type " + associationName );
        }

    }

    /**
     * @param mageObjIn
     * @return BioMaterial
     * @see convertBioMaterial
     */
    public ubic.gemma.model.expression.biomaterial.BioMaterial convertBioSource( BioSource mageObj ) {
        if ( mageObj == null ) return null;

        ubic.gemma.model.expression.biomaterial.BioMaterial result = convertBioMaterial( mageObj );

        // mageObj.getSourceContact();
        return result;
    }

    /**
     * The only extra association a BioSource has is a contact; we ignore this, so this call is passed up to
     * convertBioMaterialAssociations
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertBioMaterialAssociations
     */
    public void convertBioSourceAssociations( BioSource mageObj, BioMaterial gemmaObj, Method getter ) {
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.description.Characteristic convertCategory( OntologyEntry mageObj ) {
        Collection<Characteristic> results = convertOntologyEntry( mageObj );
        if ( results == null || results.isEmpty() ) {
            return null;
        }

        if ( results.size() > 1 ) {
            log.warn( "Multiple resulting characteristics for " + mageObj );
        }
        return results.iterator().next();
    }

    /**
     * A no-op, since we don't explicitly support channels at the moment.
     * 
     * @param mageObj
     * @return
     */
    public Object convertChannel( Channel mageObj ) {
        if ( mageObj == null ) return null;
        return null; // No-op
    }

    /**
     * NO-op, we deal with it elsewhere specially.
     * 
     * @param mageObj
     * @return
     */
    public Object convertCompositeGroup( CompositeGroup mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence convertCompositeSequence(
            org.biomage.DesignElement.CompositeSequence mageObj ) {

        if ( mageObj == null ) return null;

        CompositeSequence result = CompositeSequence.Factory.newInstance();

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertCompositeSequenceAssociations( org.biomage.DesignElement.CompositeSequence mageObj,
            ubic.gemma.model.expression.designElement.CompositeSequence gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "BiologicalCharacteristics" ) ) {
            if ( ( ( List<Object> ) associatedObject ).size() > 1 )
                log.warn( "*** More than one BiologicalCharacteristic for a MAGE CompositeSequence!" );
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY,
                    "BiologicalCharacteristic" );
        } else if ( associationName.equals( "CompositeCompositeMaps" ) ) {
            // we don't support.
        } else if ( associationName.equals( "ReporterCompositeMaps" ) ) {
            // special case. This is complicated, because the mage model has compositeSequence ->
            // reportercompositemap(s) -> reporterposition(s) -> reporter(1)
            // gemmaObj.setComponentReporters( specialConvertReporterCompositeMaps( gemmaObj, ( List ) associatedObject
            // ) );
            // throw new UnsupportedOperationException( "Reporters not supported." );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public Compound convertCompound( org.biomage.BioMaterial.Compound mageObj ) {
        if ( mageObj == null ) return null;
        Compound result = Compound.Factory.newInstance();
        result.setIsSolvent( mageObj.getIsSolvent() == null ? Boolean.FALSE : mageObj.getIsSolvent() );
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * Convert associations of "Compound" MAGE object (representing chemicals)
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertCompoundAssociations( org.biomage.BioMaterial.Compound mageObj, Compound gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "CompoundIndices" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( associatedObject, gemmaObj, getter, "compoundIndices", Characteristic.class );
        } else if ( associationName.equals( "ExternalLIMS" ) ) {
            // noop
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    // /**
    // * @param mageObj
    // * @param gemmaObj
    // */
    // public ubic.gemma.model.expression.biomaterial.CompoundMeasurement convertCompoundMeasurement(
    // CompoundMeasurement mageObj ) {
    // ubic.gemma.model.expression.biomaterial.CompoundMeasurement result =
    // ubic.gemma.model.expression.biomaterial.CompoundMeasurement.Factory
    // .newInstance();
    // convertAssociations( result, mageObj );
    // return result;
    // }
    //
    // /**
    // * @param mageObj
    // * @param gemmaObj
    // * @param getter
    // */
    // public void convertCompoundMeasurementAssociations( CompoundMeasurement mageObj,
    // ubic.gemma.model.expression.biomaterial.CompoundMeasurement gemmaObj, Method getter ) {
    // Object associatedObject = intializeConversion( mageObj, getter );
    // String associationName = getterToPropertyName( getter );
    //
    // if ( associatedObject == null ) return;
    //
    // if ( associationName.equals( "Compound" ) ) {
    // simpleFillIn( associatedObject, gemmaObj, getter );
    // } else if ( associationName.equals( "Measurement" ) ) {
    // simpleFillIn( associatedObject, gemmaObj, getter );
    // } else {
    // log.debug( "Unsupported or unknown association: " + associationName );
    // }
    // }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.auditAndSecurity.Contact convertContact( Contact mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public ExternalDatabase convertDatabase( Database mageObj ) {
        if ( mageObj == null ) {
            return null;
        }

        if ( mgedOntologyAliases.contains( mageObj.getName() ) ) {
            return this.getMAGEOntologyDatabaseObject();
        }

        ExternalDatabase result = ExternalDatabase.Factory.newInstance();
        result.setWebUri( mageObj.getURI() );
        // we don't use version.
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDatabaseAssociations( Database mageObj,
            ubic.gemma.model.common.description.ExternalDatabase gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "Contacts" ) )
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Contact" );
        else
            log.debug( "Unsupported or unknown association: " + associationName );
    }

    /**
     * @param mageObj
     * @return
     */
    public DatabaseEntry convertDatabaseEntry( org.biomage.Description.DatabaseEntry mageObj ) {
        if ( mageObj == null ) return null;
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();

        result.setAccession( mageObj.getAccession() );
        result.setAccessionVersion( mageObj.getAccessionVersion() );
        result.setUri( mageObj.getURI() );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDatabaseEntryAssociations( org.biomage.Description.DatabaseEntry mageObj,
            DatabaseEntry gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) {
            return;
        } else if ( associationName.equals( "Database" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "Type" ) ) {
            // we ain't got that.
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Values here are based on the MGED Ontology allowable values.
     * 
     * @param mageObj
     * @return
     */
    public PrimitiveType convertDataType( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;

        String val = mageObj.getValue();

        if ( val.equals( "boolean" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equalsIgnoreCase( "char" ) ) {
            return PrimitiveType.CHAR;
        } else if ( val.equalsIgnoreCase( "character" ) ) {
            return PrimitiveType.CHAR;
        } else if ( val.equalsIgnoreCase( "float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "double" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "int" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "long" ) ) {
            return PrimitiveType.LONG;
        } else if ( val.equalsIgnoreCase( "string" ) ) {
            return PrimitiveType.STRING;
        } else if ( val.equalsIgnoreCase( "string_datatype" ) ) {
            return PrimitiveType.STRING;
        } else if ( val.equalsIgnoreCase( "list_of_floats" ) ) {
            return PrimitiveType.DOUBLEARRAY;
        } else if ( val.equalsIgnoreCase( "list_of_integers" ) ) {
            return PrimitiveType.INTARRAY;
        } else if ( val.equalsIgnoreCase( "list_of_strings" ) ) {
            return PrimitiveType.STRINGARRAY;
        } else if ( val.equalsIgnoreCase( "positive_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "negative_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "positive_integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "negative_integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "list_of_booleans" ) ) {
            return PrimitiveType.BOOLEANARRAY;
        } else if ( val.equalsIgnoreCase( "nonnegative_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "nonnegative_integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "nonnegative_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "nonpositive_integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "nonnegative_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "unknown" ) ) {
            return null;
        } else {
            log.error( "Unrecognized DataType " + val );
            return null;
        }
    }

    /**
     * @param dateString
     * @return
     */
    public Date convertDateString( String dateString ) {
        if ( dateString == null || dateString.length() == 0 || dateString.equals( "n\\a" ) || dateString.equals( "n/a" ) ) {
            return null;
        }
        try {
            return ( new SimpleDateFormat() ).parse( dateString );
        } catch ( ParseException e ) {
            log.debug( "Trying alternative date formats" );
            try {
                return DateUtils.parseDate( dateString, DATE_FORMATS );
            } catch ( ParseException e1 ) {
                log.error( "Could not parse date from  '" + dateString + "'" );
                return null;
            }
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public Measurement convertDefaultValue( org.biomage.Measurement.Measurement mageObj ) {
        return convertMeasurement( mageObj );
    }

    /**
     * Some data sets use this. We have to go to extra lengths to get the biomaterial information in this case
     * 
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay convertDerivedBioAssay( DerivedBioAssay mageObj ) {
        if ( mageObj == null ) return null;

        ubic.gemma.model.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );

        /*
         * Fill in the biomaterials, because this is only linked to the 'MeasuredBioAssay'...sometimes (varies)
         */
        Collection<BioAssay> bioAssays = getAssociatedSourceBioAssays( mageObj );

        if ( bioAssays == null || bioAssays.size() == 0 ) {
            log.debug( "DerivedBioAssayMap, but no sourcebioAssays" );
        } else {
            if ( bioAssays.size() > 1 ) {

                /*
                 * This is ... just confusing.
                 */
                // log.warn( "More than one sourcebioAssay for a DerivedBioAssay: " + mageObj.getIdentifier() + " has "
                // + bioAssays.size() + " sourceBioAssays, skipping" );
                return null;
            }

            for ( Iterator<BioAssay> iter = bioAssays.iterator(); iter.hasNext(); ) {
                BioAssay bioAssay = iter.next();
                if ( bioAssay instanceof MeasuredBioAssay ) {
                    PhysicalBioAssay physicalBioAssaySource = ( ( MeasuredBioAssay ) bioAssay ).getFeatureExtraction()
                            .getPhysicalBioAssaySource();
                    specialConvertAssociationsForPhysicalBioAssay( physicalBioAssaySource, result );

                    associateDerivedBioAssayWithArray( mageObj, physicalBioAssaySource );

                } else if ( bioAssay instanceof DerivedBioAssay ) {

                    associateDerivedBioAssayWithArray( mageObj, bioAssay );

                    /*
                     * Don't worry about it. We will get it through the measuredbioassays, I hope.
                     */
                } else {
                    log.error( "What kind of bioassay is associated?: " + bioAssay.getClass() );
                }
            }
        }

        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDerivedBioAssayAssociations( DerivedBioAssay mageObj,
            ubic.gemma.model.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "DerivedBioAssayMap" ) ) {
            // if ( ( ( List ) associatedObject ).size() > 0 ) log.warn( "Missing out on DerivedBioAssayMap" );
        } else if ( associationName.equals( "DerivedBioAssayData" ) ) {
            specialConvertBioAssayBioAssayDataAssociations( ( List<BioAssayData> ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "Type" ) ) {
            // simpleFillIn( associatedObject, gemmaObj, getter, "Type" );
        } else if ( associationName.equals( "Channels" ) ) {
            // noop
        } else if ( associationName.equals( "BioAssayFactorValues" ) ) {

            if ( !bioAssayFactors.containsKey( gemmaObj.getName() ) ) {
                bioAssayFactors.put( gemmaObj.getName(), new HashSet<FactorValue>() );
            }

            for ( org.biomage.Experiment.FactorValue magefv : ( Collection<org.biomage.Experiment.FactorValue> ) mageObj
                    .getBioAssayFactorValues() ) {

                FactorValue factorValue = convertFactorValue( magefv );

                /*
                 * This is an important step. If there is no DerivedBioAssayMap, then this seems to be the only way to
                 * get the factors attached to DerivedBioAssays (which we sometimes need to use).
                 */
                if ( factors2MeasuredBioAssays.containsKey( magefv ) ) {
                    PhysicalBioAssay physicalBioAssaySource = factors2MeasuredBioAssays.get( magefv )
                            .getFeatureExtraction().getPhysicalBioAssaySource();
                    if ( log.isDebugEnabled() )
                        log.debug( "Filling in " + gemmaObj + " with data from "
                                + physicalBioAssaySource.getIdentifier() );

                    /*
                     * There are strange cases where derived bioassays are associated with many source assays.
                     */
                    Collection<BioAssay> bioAssays = getAssociatedSourceBioAssays( mageObj );
                    if ( bioAssays != null && bioAssays.size() > 1 ) {
                        continue;
                    }
                    specialConvertAssociationsForPhysicalBioAssay( physicalBioAssaySource, gemmaObj );
                    associateDerivedBioAssayWithArray( mageObj, physicalBioAssaySource );
                }

                for ( BioMaterial bm : gemmaObj.getSamplesUsed() ) {
                    bm.getFactorValues().add( factorValue );
                }

                bioAssayFactors.get( gemmaObj.getName() ).add( factorValue );
            }
        } else {
            log.warn( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }

    }

    /**
     * An Affymetrix CHP file is considered DerivedBioAssayData.
     * 
     * @param mageObj
     * @return
     * @see convertBioAssayData
     */
    public LocalFile convertDerivedBioAssayData( DerivedBioAssayData mageObj ) {
        return convertBioAssayData( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertDerivedSignal( DerivedSignal mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDerivedSignalAssociations( DerivedSignal mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * Convert a MAGE Describable to a Gemma domain object. We only allow a single description, so we take the first
     * one. The association to Security and Audit are not filled in here.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertDescribable( Describable mageObj, ubic.gemma.model.common.Describable gemmaObj ) {

        if ( mageObj == null ) return;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        // This is a bit cheesy, we just concatenate the descriptions together.
        StringBuilder descBuf = new StringBuilder( "" );
        List<Description> descriptions = mageObj.getDescriptions();
        for ( Description description : descriptions ) {
            descBuf.append( description.getText() );
            descBuf.append( " " );
            List<OntologyEntry> annotations = description.getAnnotations();
            for ( OntologyEntry element : annotations ) {
                Collection<ubic.gemma.model.common.description.Characteristic> ontologyEntries = convertOntologyEntry( element );
                if ( ontologyEntries != null ) {
                    for ( Characteristic c : ontologyEntries ) {
                        log.debug( "Got association for describable: " + c.getValue() );
                    }
                }

                // gemmaObj.addAnnotation( ontologyEntry );
            }
        }

        /*
         * In case earlier we initialized the description with anything.
         */
        if ( descBuf.length() > 0 && gemmaObj.getDescription() != null && gemmaObj.getDescription().length() > 0 ) {
            gemmaObj.setDescription( gemmaObj.getDescription() + "; " + descBuf.toString() );
        } else {
            gemmaObj.setDescription( descBuf.toString() );
        }

        convertExtendable( mageObj, gemmaObj );
    }

    /**
     * @param designElement
     * @return
     */
    public CompositeSequence convertDesignElement( org.biomage.DesignElement.DesignElement designElement ) {
        if ( designElement instanceof org.biomage.DesignElement.Reporter ) {
            // return convertReporter( ( org.biomage.DesignElement.Reporter ) designElement );
            throw new UnsupportedOperationException( "Reporters no longer supported." );
        } else if ( designElement instanceof org.biomage.DesignElement.CompositeSequence ) {
            return convertCompositeSequence( ( org.biomage.DesignElement.CompositeSequence ) designElement );
        } else if ( designElement instanceof Feature ) {
            return convertFeature( ( Feature ) designElement );
        } else {
            throw new IllegalArgumentException( "Can't convert a " + designElement.getClass().getName() );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertError(
            org.biomage.QuantitationType.Error mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertErrorAssociations( org.biomage.QuantitationType.Error mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public ExpressionExperiment convertExperiment( Experiment mageObj ) {
        if ( mageObj == null ) return null;
        ExpressionExperiment result = ExpressionExperiment.Factory.newInstance();
        result.setSource( "Imported via MAGE-ML " + mageObj.getIdentifier() );

        /*
         * Assuming ArrayExpress...
         */
        DatabaseEntry acc = DatabaseEntry.Factory.newInstance();
        acc.setAccession( mageObj.getIdentifier() );
        acc.setExternalDatabase( getArrayExpressReference() );
        result.setAccession( acc );

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @return
     */
    public ExperimentalFactor convertExperimentalFactor( org.biomage.Experiment.ExperimentalFactor mageObj ) {
        if ( mageObj == null ) return null;
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        result.setType( FactorType.CATEGORICAL ); // temporary.
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertExperimentalFactorAssociations( org.biomage.Experiment.ExperimentalFactor mageObj,
            ExperimentalFactor gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Category" ) )
            simpleFillIn( associatedObject, gemmaObj, getter, "Category", Characteristic.class );
        else if ( associationName.equals( "Annotations" ) )
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, false );
        else if ( associationName.equals( "FactorValues" ) ) {
            // Note that these should be the same factorvalues as referred to by the bioassays.
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, false );
            for ( FactorValue factorValue : gemmaObj.getFactorValues() ) {
                if ( factorValue.getMeasurement() != null ) {
                    gemmaObj.setType( FactorType.CONTINUOUS );
                }
                factorValue.setExperimentalFactor( gemmaObj );
            }
        } else
            log.warn( "Unsupported or unknown association: " + associationName );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertExperimentAssociations( Experiment mageObj, ExpressionExperiment gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "AnalysisResults" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "BioAssays" ) ) {
            assert associatedObject instanceof List;
            if ( ( ( List<Object> ) associatedObject ).size() > 0 && log.isDebugEnabled() ) {
                log.debug( "Converting Experiment-->BioAssays" );
            }

            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );

            log.info( "Added " + gemmaObj.getBioAssays().size() + " bioassays via direct association" );
        } else if ( associationName.equals( "Providers" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Provider" );
        } else if ( associationName.equals( "BioAssayData" ) ) {
            // we get this directly through the bioassay->bioassay data association.
            // assert associatedObject instanceof List;
            // if ( gemmaObj.getBioAssays() == null ) {
            // // need bioAssays first!
            // log.error( "Need bioassays first before can convert bioassaydata!" );
            // }
            // specialConvertExperimentBioAssayDataAssociations( ( List ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "ExperimentDesigns" ) ) {
            assert associatedObject instanceof List;
            List<Object> list = ( List<Object> ) associatedObject;
            if ( list.size() > 1 ) {
                log.warn( "****** Multiple experimental designs - we only take one *******" );
            }
            simpleFillIn( ( ( List<Object> ) associatedObject ).iterator().next(), gemmaObj, getter,
                    "ExperimentalDesign" );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public ExperimentalDesign convertExperimentDesign( ExperimentDesign mageObj ) {
        if ( mageObj == null ) return null;
        ExperimentalDesign result = ExperimentalDesign.Factory.newInstance();
        convertDescribable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     */
    public void convertExperimentDesignAssociations( ExperimentDesign mageObj, ExperimentalDesign gemmaObj,
            Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "ExperimentalFactors" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "NormalizationDescription" ) ) {
            // not supported as an association
        } else if ( associationName.equals( "QualityControlDescription" ) ) {
            // not supported as an association
        } else if ( associationName.equals( "ReplicateDescription" ) ) {
            // not supported as an association
        } else if ( associationName.equals( "TopLevelBioAssays" ) ) {
            assert associatedObject instanceof List;

            /*
             * This is useful information: it's the ACTUAL bioassays they use. We don't use it in our experimental
             * design, really, but it can help us figure out which bioassays we should be paying attention to (measured
             * vs. derived etc).
             */
            Collection<Object> c = mageObj.getTopLevelBioAssays();
            for ( Object o : c ) {
                BioAssay ba = ( BioAssay ) o;
                topLevelBioAssayIdentifiers.add( ba.getIdentifier() );
            }

        } else if ( associationName.equals( "Types" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * For whatever reason, the name is plura when inferred from the MAGE objects
     * 
     * @param mageObjs
     * @return
     */
    public ExperimentalDesign convertExperimentDesigns( ExperimentDesign mageObj ) {
        return convertExperimentDesign( mageObj );
    }

    /**
     * This is a no-op.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertExtendable( Extendable mageObj, ubic.gemma.model.common.Describable gemmaObj ) {
        if ( mageObj == null || gemmaObj == null ) return;
        // nothing to do, we aren't using this.
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertExternalDatabaseAssociations( Database mageObj, ExternalDatabase gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "Contacts" ) )
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Contact" );
        else
            log.debug( "Unsupported or unknown association: " + associationName );

    }

    /**
     * @param mageObj
     * @return
     */
    public FactorValue convertFactorValue( org.biomage.Experiment.FactorValue mageObj ) {
        if ( mageObj == null ) return null;
        String identifier = mageObj.getIdentifier();
        if ( fvMap.containsKey( identifier ) ) {
            return fvMap.get( identifier );
        }
        FactorValue result = FactorValue.Factory.newInstance();
        convertAssociations( mageObj, result );
        fvMap.put( identifier, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertFactorValueAssociations( org.biomage.Experiment.FactorValue mageObj, FactorValue gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associationName.equals( "ExperimentalFactor" ) ) {
            // we let the ExperimentalFactor manage this association.
        } else if ( associationName.equals( "Measurement" ) ) {
            if ( gemmaObj.getExperimentalFactor() != null ) {
                gemmaObj.getExperimentalFactor().setType( FactorType.CONTINUOUS );
            }
            simpleFillIn( associatedObject, gemmaObj, getter, "Measurement" );
        } else if ( associationName.equals( "Value" ) ) {
            simpleFillIn( Arrays.asList( new Object[] { associatedObject } ), gemmaObj, getter, CONVERT_ALL,
                    "Characteristics" );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param feature
     * @return
     */
    public CompositeSequence convertFeature( Feature feature ) {
        // I think we just have to ignore this.
        return null;
    }

    /**
     * Unlike in MAGE, feature-reporter map is not an entity. (The mage name is also confusing: it is an assocation
     * between a reporter and the features that make it up). Therefore, this is a no-op.
     * 
     * @param mageObj
     * @return
     */
    public Object convertFeatureReporterMap( FeatureReporterMap mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * Copy attributes from a MAGE identifiable to a Gemma identifiable.
     * 
     * @param mageObj
     */
    public void convertIdentifiable( org.biomage.Common.Identifiable mageObj,
            ubic.gemma.model.common.Describable gemmaObj ) {

        if ( mageObj == null ) return;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        if ( mageObj instanceof BioAssay ) {
            /*
             * We need the _name_ so we can map to the processed data file.
             */
            gemmaObj.setName( mageObj.getIdentifier() );

            if ( StringUtils.isNotBlank( mageObj.getName() ) ) {
                this.id2Name.put( mageObj.getIdentifier(), mageObj.getName() );
            }

        } else if ( mageObj instanceof QuantitationType ) {
            /*
             * quantitation types are a weird case.
             */
            gemmaObj.setName( getUnqualifiedIdentifier( mageObj ) );
        } else {
            gemmaObj.setName( mageObj.getName() );
        }

        /* Use the identifier as a name if there isn't a name. */
        if ( gemmaObj.getName() == null ) {
            String identifier = mageObj.getIdentifier();
            if ( gemmaObj instanceof CompositeSequence ) {
                /*
                 * our lives are much easier if we ust use the end of the identifier.
                 */
                identifier = getUnqualifiedIdentifier( mageObj );
            }
            gemmaObj.setName( identifier );

        }

        convertDescribable( mageObj, gemmaObj );
    }

    /**
     * @param kindCV
     * @return
     */
    public MeasurementKind convertKindCV( KindCV kindCV ) {
        if ( kindCV == null ) return null;

        if ( kindCV.getValue() == kindCV.concentration ) {
            return MeasurementKind.CONCENTRATION;
        } else if ( kindCV.getValue() == kindCV.distance ) {
            return MeasurementKind.DISTANCE;
        } else if ( kindCV.getValue() == kindCV.mass ) {
            return MeasurementKind.MASS;
        } else if ( kindCV.getValue() == kindCV.quantity ) {
            return MeasurementKind.QUANTITY;
        } else if ( kindCV.getValue() == kindCV.temperature ) {
            return MeasurementKind.TEMPERATURE;
        } else if ( kindCV.getValue() == kindCV.time ) {
            return MeasurementKind.TIME;
        } else if ( kindCV.getValue() == kindCV.volume ) {
            return MeasurementKind.VOLUME;
        } else if ( kindCV.getValue() == kindCV.other ) {
            return MeasurementKind.OTHER;
        }

        log.error( "Unknown measurement kind: " + kindCV.getName() );
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public BioMaterial convertLabeledExtract( org.biomage.BioMaterial.LabeledExtract mageObj ) {
        if ( mageObj == null ) return null;
        return convertBioMaterial( mageObj );
    }

    /**
     * A labeled extract has no associations that we keep track of, so this delegates to convertBioMaterialAssociations.
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertBioMaterialAssociations
     */
    public void convertLabeledExtractAssociations( LabeledExtract mageObj, BioMaterial gemmaObj, Method getter ) {
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return an OntologyEntry correspondingi the the MaterialType.
     * @see convertOntologyEntry
     */
    public ubic.gemma.model.common.description.Characteristic convertMaterialType( OntologyEntry mageObj ) {
        Collection<Characteristic> results = convertOntologyEntry( mageObj );
        if ( results == null || results.isEmpty() ) {
            return null;
        }

        if ( results.size() > 1 ) {
            log.warn( "Multiple resulting characteristics for " + mageObj );
        }
        return results.iterator().next();
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay convertMeasuredBioAssay( MeasuredBioAssay mageObj ) {
        if ( mageObj == null ) return null;

        ubic.gemma.model.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );

        PhysicalBioAssay physicalBioAssaySource = mageObj.getFeatureExtraction().getPhysicalBioAssaySource();
        specialConvertAssociationsForPhysicalBioAssay( physicalBioAssaySource, result );

        associateMeasuredBioAssayWithArray( mageObj, physicalBioAssaySource );

        /*
         * This is actually okay, sometimes. Later we check that we got bioassays with biomaterials associated.
         */
        if ( result.getSamplesUsed().size() == 0 ) {
            // throw new IllegalStateException( "No BioMaterials for BioAssay: " + mageObj.getIdentifier() );
        }

        convertAssociations( mageObj, result );
        return result;
        // return null;

    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertMeasuredBioAssayAssociations( MeasuredBioAssay mageObj,
            ubic.gemma.model.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "FeatureExtraction" ) ) {
            specialConvertFeatureExtraction( ( FeatureExtraction ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "MeasuredBioAssayData" ) ) {
            specialConvertBioAssayBioAssayDataAssociations( ( List<BioAssayData> ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "BioAssayFactorValues" ) ) {
            if ( !bioAssayFactors.containsKey( gemmaObj.getName() ) )
                bioAssayFactors.put( gemmaObj.getName(), new HashSet<FactorValue>( 0 ) );

            for ( org.biomage.Experiment.FactorValue magefv : ( Collection<org.biomage.Experiment.FactorValue> ) mageObj
                    .getBioAssayFactorValues() ) {
                FactorValue factorValue = convertFactorValue( magefv );
                bioAssayFactors.get( gemmaObj.getName() ).add( factorValue );
                /*
                 * Store this information in case we need to get the factor later (e.g. for the derived bioassay if
                 * there is no derivedbioassaymap)
                 */
                factors2MeasuredBioAssays.put( magefv, mageObj );
            }
        } else {
            log.debug( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }
    }

    /**
     * The Affymetrix CEL files are usually considered MeasuredBioAssayData.
     * 
     * @param mageObj
     * @return
     * @see convertBioAssayData
     */
    public LocalFile convertMeasuredBioAssayData( MeasuredBioAssayData mageObj ) {
        return convertBioAssayData( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertMeasuredSignal( MeasuredSignal mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertMeasuredSignalAssociations( MeasuredSignal mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public Measurement convertMeasurement( org.biomage.Measurement.Measurement mageObj ) {
        if ( mageObj == null ) return null;
        Measurement result = Measurement.Factory.newInstance();

        result.setOtherKind( mageObj.getOtherKind() );
        result.setKindCV( convertKindCV( mageObj.getKindCV() ) );

        result.setUnit( convertUnit( mageObj.getUnit() ) );

        result.setRepresentation( PrimitiveType.STRING );
        if ( mageObj.getValue() != null ) {
            result.setValue( mageObj.getValue().toString() );

            boolean isDouble = false;
            try {
                Double.parseDouble( result.getValue() );
                isDouble = true;
            } catch ( Exception e ) {
            }

            if ( isDouble ) result.setRepresentation( PrimitiveType.DOUBLE );

        }

        result.setType( convertMeasurementType( mageObj.getType() ) );

        return result;
    }

    /**
     * @param type
     * @return
     */
    public MeasurementType convertMeasurementType( Type type ) {
        if ( type == null ) return null;

        if ( type.getValue() == type.absolute ) {
            return MeasurementType.ABSOLUTE;
        } else if ( type.getValue() == type.change ) {
            return MeasurementType.CHANGE;
        }

        log.error( "Unknown measurement type: " + type.getName() );
        return null;
    }

    /**
     * OntologyEntry is a subclass of DatabaseEntry in Gemma, but not in MAGE. Instead, a MAGE object has an
     * OntologyReference to a databaseEntry.
     * <p>
     * In Gemma, where possible we convert MGED ontology objects into the corresponding Gemma Characteristic object.
     * 
     * @param mageObj
     * @return
     */
    public Collection<ubic.gemma.model.common.description.Characteristic> convertOntologyEntry( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;
        Collection<Characteristic> results = new HashSet<Characteristic>();
        String category = mageObj.getCategory();
        String value = mageObj.getValue();
        org.biomage.Description.DatabaseEntry ontologyReference = mageObj.getOntologyReference();

        if ( !haveInitializedMgedOntologyHelper ) {
            this.initMGEDOntology();
            haveInitializedMgedOntologyHelper = true;
        }

        boolean categoryIsMo = mgedOntologyHelper.classExists( StringUtils.capitalize( category ) );

        Collection<String> instances = null;
        if ( categoryIsMo ) {
            instances = mgedOntologyHelper.getInstances( StringUtils.capitalize( category ) );
        }

        // It might not actually be a vocabcharacteristic. It might be a "category" - "value" pair.

        /**
         * <pre>
         * Cases: 
         *       - Category is a MO term
         *       - Value is a MO instance
         * </pre>
         */

        String genericDescription = "Imported from MAGE-ML";
        if ( !categoryIsMo ) {
            if ( log.isDebugEnabled() ) log.debug( "User-defined category=" + category + " value=" + value );
            Characteristic result = VocabCharacteristic.Factory.newInstance();

            // then it is user-defined, or something.
            result.setValue( value );
            result.setCategory( category );
            result.setDescription( genericDescription + ": Non-MGED Ontology class" );
            results.add( result );
        } else if ( category.equals( value ) ) {
            /**
             * This is the case where associated "has_*" objects hold the actual information. Example is "Age". In the
             * past we tried to extract all the information with the help of MAGE-simplify.xsl, but this proved to be
             * more trouble than it is worth. This could be handled with recursion, but this direct loop is easier for
             * the moment as we don't have to handle deeper associations in general.
             * <ul>
             * <li>DiseaseState -> has_value -> Stenosis</li>
             * <li>has_disease_staging -> DiseaseStaging -> has_value -> Symptomatic</li>
             * </ul>
             */

            for ( OntologyEntry oe : ( Collection<OntologyEntry> ) mageObj.getAssociations() ) {
                if ( oe.getValue().startsWith( "has_" ) ) {

                    for ( OntologyEntry oei : ( Collection<OntologyEntry> ) oe.getAssociations() ) {

                        if ( oe.getValue().equals( "has_value" ) && !oei.getValue().startsWith( "has_" ) ) {
                            Characteristic result = makeCharacteristic( mageObj, oei );
                            results.add( result );
                        } else {

                            for ( OntologyEntry oeii : ( Collection<OntologyEntry> ) oei.getAssociations() ) {

                                if ( oei.getValue().equals( "has_value" ) && !oeii.getValue().startsWith( "has_" ) ) {
                                    Characteristic result = makeCharacteristic( oe, oeii );
                                    results.add( result );
                                } else {

                                    for ( OntologyEntry oeiii : ( Collection<OntologyEntry> ) oeii.getAssociations() ) {
                                        if ( oeii.getValue().equals( "has_value" )
                                                && !oeiii.getValue().startsWith( "has_" ) ) {
                                            Characteristic result = makeCharacteristic( oei, oeiii );
                                            results.add( result );

                                        } else {

                                            for ( OntologyEntry oeiiii : ( Collection<OntologyEntry> ) oeiii
                                                    .getAssociations() ) {

                                                if ( oeiii.getValue().equals( "has_value" )
                                                        && !oeiiii.getValue().startsWith( "has_" ) ) {

                                                    Characteristic result = makeCharacteristic( oeii, oeiiii );
                                                    results.add( result );

                                                } else {
                                                    log.warn( "Too deep, replace with recursion" );
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } else { // instance
            VocabCharacteristic result = VocabCharacteristic.Factory.newInstance();
            boolean valueIsMOInstance = false;
            if ( instances != null ) {
                for ( String string : instances ) {
                    if ( string.equals( value ) ) {
                        valueIsMOInstance = true;
                    }
                }
            }
            String termUri = "";
            if ( valueIsMOInstance ) {
                termUri = MGED_ONTOLOGY_URL + "#" + value;
            }

            String classUri;

            if ( ontologyReference != null ) {
                classUri = ontologyReference.getURI();
            } else {
                classUri = MGED_ONTOLOGY_URL + "#" + StringUtils.capitalize( category );
            }
            result.setValue( value );
            result.setCategory( category );
            result.setCategoryUri( classUri );
            result.setValueUri( termUri );
            result.setDescription( genericDescription );
            if ( log.isDebugEnabled() ) log.debug( "Instance category=" + category + " value=" + value );
            // then it is an instance.
            results.add( result );
        }

        return results;
        /* DO NOT convert any associations. Keep it shallow. */
        // convertAssociations( mageObj, result );
    }

    /**
     * FIXME is this separate method really needed.
     * 
     * @param mageObj
     * @param gemmaObj - a VocabCharacteristic
     * @param getter
     */
    public void convertOntologyEntryAssociations( OntologyEntry mageObj, VocabCharacteristic gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Associations" ) ) {
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );
            // specialConvertOntologyEntryAssociations( ( List ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "OntologyReference" ) ) {
            // // No-op we don't maintain a link to the Ontology.
            // assert associatedObject instanceof org.biomage.Description.DatabaseEntry;
            // specialConvertOntologyEntryDatabaseEntry( ( org.biomage.Description.DatabaseEntry ) associatedObject,
            // gemmaObj );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return DatabaseEntry
     * @see convertDatabaseEntry
     */
    public DatabaseEntry convertOntologyReference( org.biomage.Description.DatabaseEntry mageObj ) {
        return this.convertDatabaseEntry( mageObj );
    }

    /**
     * Not supported.
     * 
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.auditAndSecurity.Organization convertOrganization( Organization mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    // /**
    // * @param list
    // * @param gemmaObj
    // */
    // private void specialConvertExperimentBioAssayDataAssociations( List<BioAssayData> bioAssayData,
    // ExpressionExperiment gemmaObj ) {
    // for ( BioAssayData data : bioAssayData ) {
    // LocalFile file = convertBioAssayData( data );
    // // need to attachi this to
    // }
    // }

    /**
     * Not supported.
     * 
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.auditAndSecurity.Person convertPerson( Person mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     * @see convertArrayDesign
     */
    public Object convertPhysicalArrayDesign( org.biomage.ArrayDesign.PhysicalArrayDesign mageObj ) {
        if ( mageObj == null ) return null;
        ubic.gemma.model.expression.arrayDesign.ArrayDesign result = convertArrayDesign( mageObj );
        convertAssociations( mageObj, result );
        return result;

    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPhysicalArrayDesignAssociations( PhysicalArrayDesign mageObj,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign gemmaObj, Method getter ) {
        convertArrayDesignAssociations( mageObj, gemmaObj, getter );
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "SurfaceType" ) ) {
            // we don't support this
        } else if ( associationName.equals( "ZoneGroups" ) ) {
            assert associatedObject instanceof List;
            // we don't support this.
        } else if ( associationName.equals( "ReporterGroups" ) || associationName.equals( "FeatureGroups" )
                || associationName.equals( "DesignProviders" ) || associationName.equals( "CompositeGroups" )
                || associationName.equals( "ProtocolApplications" ) ) {
            // nothing, superclass.
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * We don't use this except to locate other data, so we don't return the results.
     * 
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay convertPhysicalBioAssay( PhysicalBioAssay mageObj ) {
        if ( mageObj == null ) return null;
        ubic.gemma.model.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );

        convertAssociations( mageObj, result );
        return null;

        // return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPhysicalBioAssayAssociations( PhysicalBioAssay mageObj,
            ubic.gemma.model.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "BioAssayCreation" ) ) {
            specialConvertAssociationsForPhysicalBioAssay( mageObj, gemmaObj );
        } else if ( associationName.equals( "BioAssayTreatments" ) ) {

            for ( Object o : mageObj.getBioAssayTreatments() ) {
                BioAssayTreatment t = ( BioAssayTreatment ) o;
                PhysicalBioAssay targetPBA = t.getTarget();
                physicalBioAssay2physicalBioAssay.put( targetPBA.getIdentifier(), mageObj );

            }
            // assert associatedObject instanceof List;
            // this is not supported in our data model currently.
        } else if ( associationName.equals( "PhysicalBioAssayData" ) ) {
            assert associatedObject instanceof List;
            // these are Image objects - not supported
        } else {
            /*
             * LabeledExtract...
             */
            log.debug( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }
    }

    /**
     * @param associatedObject
     * @return
     */
    public PolymerType convertPolymerType( OntologyEntry mageObj ) {
        if ( mageObj.getValue().equalsIgnoreCase( "DNA" ) ) {
            return PolymerType.DNA;
        } else if ( mageObj.getValue().equalsIgnoreCase( "protein" ) ) {
            return PolymerType.PROTEIN;
        } else if ( mageObj.getValue().equalsIgnoreCase( "RNA" ) ) {
            return PolymerType.RNA;
        }
        log.error( "Unsupported polymer type:" + mageObj.getValue() );
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertPresentAbsent( PresentAbsent mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertQuantitationTypeAssociations
     */
    public void convertPresentAbsentAssociations( org.biomage.QuantitationType.PresentAbsent mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.protocol.Protocol convertProtocol( Protocol mageObj ) {
        if ( mageObj == null ) return null;
        ubic.gemma.model.common.protocol.Protocol result = ubic.gemma.model.common.protocol.Protocol.Factory
                .newInstance();

        result.setURI( mageObj.getURI() );
        convertIdentifiable( mageObj, result );

        // we just use the name and description to hold the text and title.
        result.setDescription( mageObj.getText() );
        result.setName( mageObj.getTitle() );

        if ( result.getName() == null ) {
            result.setName( mageObj.getIdentifier() );
        }

        convertAssociations( mageObj, result );

        return result;
    }

    public ProtocolApplication convertProtocolApplication( org.biomage.Protocol.ProtocolApplication mageObj ) {
        ProtocolApplication result = ProtocolApplication.Factory.newInstance();

        if ( mageObj.getActivityDate() != null ) {
            String dateString = mageObj.getActivityDate();
            Date date = convertDateString( dateString );
            result.setActivityDate( date );
        }
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertProtocolApplicationAssociations( org.biomage.Protocol.ProtocolApplication mageObj,
            ProtocolApplication gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "SoftwareApplications" ) ) {
            // no-op
        } else if ( associationName.equals( "HardwareApplications" ) ) {
            // no-op
        } else if ( associationName.equals( "Protocol" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "Performers" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertProtocolAssociations( Protocol mageObj, ubic.gemma.model.common.protocol.Protocol gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Hardwares" ) ) {
            // no-op
        } else if ( associationName.equals( "Softwares" ) ) {
            // no-op
        } else if ( associationName.equals( "Type" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "Type", Characteristic.class );
        } else if ( associationName.equals( "ParameterTypes" ) ) {
            // no-op
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     * @see convertQuantitationType
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertPValue( PValue mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPValueAssociations( PValue mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertQuantitationType( QuantitationType mageObj ) {

        ubic.gemma.model.common.quantitationtype.QuantitationType result = ubic.gemma.model.common.quantitationtype.QuantitationType.Factory
                .newInstance();

        // note that PrimitiveType and Scale are set via associations.
        if ( mageObj instanceof SpecializedQuantitationType ) {
            result.setGeneralType( GeneralType.UNKNOWN );
            result.setType( StandardQuantitationType.OTHER );
        } else if ( mageObj instanceof MeasuredSignal ) {
            result.setGeneralType( GeneralType.QUANTITATIVE );
            result.setType( StandardQuantitationType.AMOUNT );
        } else if ( mageObj instanceof DerivedSignal ) {
            result.setGeneralType( GeneralType.QUANTITATIVE );
            result.setType( StandardQuantitationType.AMOUNT );
        } else if ( mageObj instanceof Ratio ) {
            result.setGeneralType( GeneralType.QUANTITATIVE );
            result.setType( StandardQuantitationType.AMOUNT );
            result.setIsRatio( true );
        } else if ( mageObj instanceof Failed ) {
            result.setGeneralType( GeneralType.CATEGORICAL );
            result.setType( StandardQuantitationType.FAILED );
        } else if ( mageObj instanceof PresentAbsent ) {
            result.setGeneralType( GeneralType.CATEGORICAL );
            result.setType( StandardQuantitationType.PRESENTABSENT );
            result.setRepresentation( PrimitiveType.STRING );
        } else if ( mageObj instanceof ConfidenceIndicator ) {
            result.setGeneralType( GeneralType.QUANTITATIVE );
            result.setType( StandardQuantitationType.CONFIDENCEINDICATOR );
        } else {
            result.setGeneralType( GeneralType.UNKNOWN );
            result.setType( StandardQuantitationType.OTHER );
        }

        if ( mageObj.getIsBackground() != null ) {
            result.setIsBackground( mageObj.getIsBackground() );
        } else {
            result.setIsBackground( false );
        }

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );

        /*
         * This can be filled in altready through convertDataType. It causes problems when this is an intensity value,
         * we really need to treat as a double. Let the parameter guesser figure it out.
         */
        if ( result.getRepresentation() != null && result.getRepresentation().equals( PrimitiveType.INT )
                && result.getName().toLowerCase().contains( "mean" )
                || result.getName().toLowerCase().contains( "median" ) ) {
            result.setRepresentation( null );
        }

        QuantitationTypeParameterGuesser.guessQuantitationTypeParameters( result, result.getName(),
                result.getDescription() );

        if ( result.getIsRatio() == null ) result.setIsRatio( false );
        if ( result.getIsBackground() == null ) result.setIsBackground( false ); // OK
        if ( result.getIsBackgroundSubtracted() == null ) result.setIsBackgroundSubtracted( false );
        if ( result.getIsNormalized() == null ) result.setIsNormalized( false );
        if ( result.getIsPreferred() == null ) result.setIsPreferred( false );
        if ( result.getIsMaskedPreferred() == null ) result.setIsMaskedPreferred( false );
        if ( result.getRepresentation() == null ) result.setRepresentation( PrimitiveType.DOUBLE );

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertQuantitationTypeAssociations( QuantitationType mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Channel" ) ) {
            // we aren't support this
        } else if ( associationName.equals( "ConfidenceIndicators" ) ) {
            // this is bidirectionally navigable in MAGE - we don't support that.
        } else if ( associationName.equals( "DataType" ) ) {
            gemmaObj.setRepresentation( convertDataType( mageObj.getDataType() ) );
        } else if ( associationName.equals( "Scale" ) ) {
            gemmaObj.setScale( convertScale( mageObj.getScale() ) );
        } else if ( associationName.equals( "QuantitationTypeMaps" ) ) {
            // special case - transformations.
        } else if ( associationName.equals( "TargetQuantitationType" ) ) { // from ConfidenceIndicator.
            // this is an association to another QuantitationType: the confidence in it. I think we skip for now.
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    public Collection<ubic.gemma.model.common.quantitationtype.QuantitationType> convertQuantitationTypeDimension(
            QuantitationTypeDimension mageObj ) {
        Collection<QuantitationType> types = mageObj.getQuantitationTypes();
        Collection<ubic.gemma.model.common.quantitationtype.QuantitationType> result = new ArrayList<ubic.gemma.model.common.quantitationtype.QuantitationType>();
        for ( QuantitationType type : types ) {
            ubic.gemma.model.common.quantitationtype.QuantitationType convertedType = convertQuantitationType( type );
            result.add( convertedType );
        }
        return result;
    }

    /**
     * @param mageObj
     * @return
     * @see convertQuantitationType
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertRatio( Ratio mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertRatioAssociations( Ratio mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return ubic.gemma.model.expression.designElement.Reporter
     * @see convertReporterAssociations
     */
    public Reporter convertReporter( org.biomage.DesignElement.Reporter mageObj ) {
        if ( mageObj == null ) return null;
        Reporter result = Reporter.Factory.newInstance();
        specialGetReporterFeatureLocations( mageObj, result );
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertReporterAssociations( org.biomage.DesignElement.Reporter mageObj, Reporter gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "FailTypes" ) ) {
            // we don't support
        } else if ( associationName.equals( "FeatureReporterMaps" ) ) {
            // we don't support
        } else if ( associationName.equals( "ImmobilizedCharacteristics" ) ) {
            // no-op
        } else if ( associationName.equals( "WarningType" ) ) {
            specialConvertFeatureReporterMaps( ( List<Object> ) associatedObject, gemmaObj );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Unlike in MAGE, reporter-composite map is not an entity. (The mage name is also confusing: it is an assocation
     * betwee a composite sequence and the reporters that make it up). Therefore, this is a no-op, we deal with it
     * specially.
     * 
     * @param mageObj
     * @return
     */
    public Object convertReporterCompositeMap( ReporterCompositeMap mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * No-op, we deal with specially.
     * 
     * @param mageObj
     * @return
     */
    public Object convertReporterGroup( ReporterGroup mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public ScaleType convertScale( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;

        String val = mageObj.getValue();
        if ( val.equalsIgnoreCase( "foldchange" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equalsIgnoreCase( "fold_change" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equalsIgnoreCase( "linear_scale" ) ) {
            return ScaleType.LINEAR;
        } else if ( val.equalsIgnoreCase( "linear" ) ) {
            return ScaleType.LINEAR;
        } else if ( val.equalsIgnoreCase( "ln" ) ) {
            return ScaleType.LN;
        } else if ( val.equalsIgnoreCase( "log" ) ) {
            return ScaleType.LOGBASEUNKNOWN;
        } else if ( val.equalsIgnoreCase( "percent" ) ) {
            return ScaleType.PERCENT;
        } else if ( val.equalsIgnoreCase( "fraction" ) ) {
            return ScaleType.FRACTION;
        } else if ( val.equalsIgnoreCase( "log10" ) ) {
            return ScaleType.LOG10;
        } else if ( val.equalsIgnoreCase( "log_base_10" ) ) {
            return ScaleType.LOG10;
        } else if ( val.equalsIgnoreCase( "log2" ) ) {
            return ScaleType.LOG2;
        } else if ( val.equalsIgnoreCase( "log_base_2" ) ) {
            return ScaleType.LOG2;
        } else if ( val.equalsIgnoreCase( "other" ) ) {
            return ScaleType.OTHER;
        } else if ( val.equalsIgnoreCase( "unscaled" ) ) {
            return ScaleType.UNSCALED;
        } else if ( val.equalsIgnoreCase( "unknown" ) ) {
            return null;
        }
        log.error( "Unrecognized Scale " + val );
        return null;
    }

    /**
     * @param mageObj
     * @return
     * @see convertQuantitationType
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertSpecializedQuantitationType(
            SpecializedQuantitationType mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertSpecializedQuantitationTypeAssociations( SpecializedQuantitationType mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     * @see convertQuantitationType
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType convertStandardQuantitationType(
            org.biomage.QuantitationType.StandardQuantitationType mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertStandardQuantitationTypeAssociations(
            org.biomage.QuantitationType.StandardQuantitationType mageObj,
            ubic.gemma.model.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public Treatment convertTreatment( org.biomage.BioMaterial.Treatment mageObj ) {
        if ( mageObj == null ) return null;
        Treatment result = Treatment.Factory.newInstance();
        Integer order = mageObj.getOrder();
        if ( order != null ) {
            result.setOrderApplied( order.intValue() );
        } else {
            result.setOrderApplied( 1 );
        }
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertTreatmentAssociations( org.biomage.BioMaterial.Treatment mageObj, Treatment gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Action" ) ) {
            // no-op
        } else if ( associationName.equals( "ActionMeasurement" ) ) {
            // no-op
        } else if ( associationName.equals( "CompoundMeasurement" ) ) {
            // no-op
        } else if ( associationName.equals( "ProtocolApplications" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List<Object> ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "SourceBioMaterialMeasurements" ) ) {
            // deal with separately.
            // assert associatedObject instanceof List;
            // need the bioSource for our biomaterial.
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public ubic.gemma.model.common.description.Characteristic convertType( OntologyEntry mageObj ) {
        Collection<Characteristic> results = convertOntologyEntry( mageObj );
        if ( results == null || results.isEmpty() ) {
            return null;
        }

        if ( results.size() > 1 ) {
            log.warn( "Multiple resulting characteristics for " + mageObj );
        }
        return results.iterator().next();
    }

    /**
     * @param unit
     * @return
     */
    public ubic.gemma.model.common.measurement.Unit convertUnit( Unit unit ) {
        if ( unit == null ) return null;

        ubic.gemma.model.common.measurement.Unit result = ubic.gemma.model.common.measurement.Unit.Factory
                .newInstance();
        StringWriter w = new StringWriter();
        try {
            unit.writeAttributes( w );
        } catch ( IOException e ) {

        }
        String att = w.toString();

        att = att.replaceAll( ".+?=\"(.+?)\"", "$1" );

        result.setUnitNameCV( att );
        return result;
    }

    /**
     * This is hit for factor value conversion.
     * 
     * @param mageObj
     * @return
     * @see convertOntologyEntry
     */
    public ubic.gemma.model.common.description.Characteristic convertValue( OntologyEntry mageObj ) {
        Collection<Characteristic> results = convertOntologyEntry( mageObj );
        if ( results == null || results.isEmpty() ) {
            return null;
        }

        if ( results.size() > 1 ) {
            log.warn( "Multiple resulting characteristics for " + mageObj );
        }
        return results.iterator().next();
    }

    public Map<String, Collection<BioAssay>> getArray2BioAssay() {
        return this.array2BioAssay;
    }

    /**
     * @return Returns the bioAssayDimensions.
     * @deprecated
     */
    @Deprecated
    public BioAssayDimensions getBioAssayDimensions() {
        return this.bioAssayDimensions;
    }

    /**
     * @param bioAssay
     * @return A List of QuantitationTypes representing the QuantitationTypeDimension for the BioAssay. If there is no
     *         such bioAssay in the current data, returns null.
     * @deprecated Not used
     */
    @Deprecated
    public List<ubic.gemma.model.common.quantitationtype.QuantitationType> getBioAssayQuantitationTypeDimension(
            ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        if ( bioAssay == null ) throw new IllegalArgumentException();
        return bioAssayDimensions.getQuantitationTypeDimension( bioAssay );
    }

    /**
     * @return the id2Name
     */
    public Map<String, String> getId2Name() {
        return id2Name;
    }

    /**
     * @return
     * @deprecated Not used - but might be needed for bioassay data if we get it from raw files
     */
    @Deprecated
    public Collection<ubic.gemma.model.expression.bioAssay.BioAssay> getQuantitationTypeBioAssays() {
        return this.bioAssayDimensions.getQuantitationTypeBioAssays();
    }

    /**
     * @return the topLevelBioAssayIdentifiers
     */
    public Collection<String> getTopLevelBioAssayIdentifiers() {
        return topLevelBioAssayIdentifiers;
    }

    /**
     * Special case: Convert a ReporterCompositeMaps (list) to a Collection of Reporters.
     * 
     * @param reporterCompositeMaps
     * @return Collection of Gemma Reporters.
     */
    public void specialConvertFeatureReporterMaps( List<Object> featureReporterMaps, Reporter rep ) {

        if ( featureReporterMaps.size() > 1 ) log.warn( "**** More than one FeatureReporterMap for a Reporter!" );

        for ( Iterator<Object> iter = featureReporterMaps.iterator(); iter.hasNext(); ) {
            FeatureReporterMap rcp = ( FeatureReporterMap ) iter.next();
            List<Object> rcpps = rcp.getFeatureInformationSources();
            for ( Iterator<Object> iterator = rcpps.iterator(); iterator.hasNext(); ) {
                log.debug( "Found feature information for reporter: " + rep.getName() );
                FeatureInformation rps = ( FeatureInformation ) iterator.next();
                org.biomage.DesignElement.Feature repr = rps.getFeature();
                rep.setCol( repr.getPosition().getX().intValue() );
                rep.setRow( repr.getPosition().getY().intValue() );
                // strand...
            }
            break; // only take the first one
        }
    }

    /**
     * This is provided for tests.
     * 
     * @param path
     */
    protected void addLocalExternalDataPath( String path ) {
        localExternalDataPaths.add( path );
    }

    /**
     * @param biomaterial
     * @param factorValues
     */
    private void addFactorValuesToBioMaterial( BioMaterial biomaterial, Collection<FactorValue> factorValues ) {
        if ( factorValues == null || factorValues.isEmpty() ) return;
        if ( biomaterial.getFactorValues() == null ) biomaterial.setFactorValues( new HashSet<FactorValue>() );
        biomaterial.getFactorValues().addAll( factorValues );
    }

    /**
     * @param mageObj
     * @param bioAssaySource
     */
    private void associateDerivedBioAssayWithArray( DerivedBioAssay mageObj, BioAssay bioAssaySource ) {
        for ( String arrayId : this.array2BioAssay.keySet() ) {
            BioAssay measuredBioAssayToRemove = null;
            boolean add = false;
            for ( BioAssay ba : this.array2BioAssay.get( arrayId ) ) {
                if ( ba.getIdentifier().equals( bioAssaySource.getIdentifier() ) ) {
                    add = true;
                } else if ( ba instanceof MeasuredBioAssay ) {
                    measuredBioAssayToRemove = ba;
                }
            }

            if ( add ) {
                if ( log.isDebugEnabled() ) log.debug( arrayId + " ====> " + mageObj.getIdentifier() );
                this.array2BioAssay.get( arrayId ).add( mageObj );

                if ( measuredBioAssayToRemove != null ) {
                    if ( log.isDebugEnabled() )
                        log.debug( arrayId + " ==XX=> " + measuredBioAssayToRemove.getIdentifier() );
                    this.array2BioAssay.get( arrayId ).remove( measuredBioAssayToRemove );
                }
            }
        }
    }

    /**
     * @param mageObj
     * @param physicalBioAssaySource
     */
    private void associateMeasuredBioAssayWithArray( MeasuredBioAssay mageObj, PhysicalBioAssay physicalBioAssaySource ) {
        for ( String arrayId : this.array2BioAssay.keySet() ) {
            boolean add = false;
            for ( BioAssay ba : this.array2BioAssay.get( arrayId ) ) {
                if ( ba.getIdentifier().equals( physicalBioAssaySource.getIdentifier() ) ) {
                    add = true;
                    break;
                } else if ( ba instanceof DerivedBioAssay ) {
                    /*
                     * Favor the DerivedBioAssay, don't add it if there is one already
                     */
                    add = false;
                    break;
                }
            }

            if ( add ) {
                if ( log.isDebugEnabled() ) log.debug( arrayId + " ====> " + mageObj.getIdentifier() );
                this.array2BioAssay.get( arrayId ).add( mageObj );
            }
        }
    }

    /**
     * Generic method to convert associations of a Mage object. The association is resolved into a call to an
     * appropriate method to convert the particular type of association.
     * 
     * @param mageClass The class of the MAGE object to be converted.
     * @param mageObj The MAGE object to be converted.
     * @param gemmaObj The Gemma object whose associations need to be filled in.
     */
    private void convertAssociations( Class<?> mageClass, Object mageObj, Object gemmaObj ) {

        if ( mageObj == null || gemmaObj == null ) return;

        Class<?> classToSeek = ReflectionUtil.getBaseForImpl( gemmaObj );
        String gemmaObjName = classToSeek.getSimpleName();

        try {
            Class<?>[] interfaces = mageClass.getInterfaces();

            if ( interfaces.length == 0 ) return;

            for ( int i = 0; i < interfaces.length; i++ ) {
                Class<?> infc = interfaces[i];
                String infcName = infc.getSimpleName();

                if ( !infcName.startsWith( "Has" ) ) continue;

                String propertyName = infcName.substring( 3 );

                Method getter = mageClass.getMethod( "get" + propertyName, new Class[] {} );

                if ( getter != null ) {
                    try {
                        Method converter = this.getClass().getMethod(
                                "convert" + ReflectionUtil.objectToTypeName( mageObj ) + "Associations",
                                new Class[] { mageObj.getClass(), classToSeek, getter.getClass() } );

                        if ( converter == null ) throw new NoSuchMethodException();

                        converter.invoke( this, new Object[] { mageObj, gemmaObj, getter } );

                    } catch ( NoSuchMethodException e ) {
                        log.warn( "Conversion of associations -- Operation not yet supported: " + "convert"
                                + ReflectionUtil.objectToTypeName( mageObj ) + "Associations("
                                + mageObj.getClass().getName() + ", " + gemmaObjName + ", "
                                + getter.getClass().getName() + ")" );
                    }
                }

            }

            Class<?> superclazz = mageClass.getSuperclass();
            if ( superclazz == null ) return;

            String superclassName = superclazz.getName();
            if ( superclassName.startsWith( "org.biomage" ) && !superclassName.endsWith( "Extendable" )
                    && !superclassName.endsWith( "Describable" ) && !superclassName.endsWith( "Identifiable" ) )
                convertAssociations( superclazz, mageObj, gemmaObj );

        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( e );
        } catch ( IllegalArgumentException e ) {
            throw new RuntimeException( e );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( "InvocationTargetException For: " + gemmaObjName, e );
        }
    }

    /**
     * Locate a converter for a MAGE object.
     * 
     * @param mageObj
     * @return Converted object. If the source object is null, the return value is null.
     */
    private Object findAndInvokeConverter( Object mageObj ) {

        if ( mageObj == null ) return null;
        Object convertedGemmaObj = null;
        Method gemmaConverter = findConverter( mageObj );
        if ( gemmaConverter == null ) return null;
        try {
            convertedGemmaObj = gemmaConverter.invoke( this, new Object[] { mageObj } );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error(
                    "InvocationTargetException caused by " + e.getCause() + " when invoking "
                            + gemmaConverter.getName() + " on a " + mageObj.getClass().getName(), e );
            throw new RuntimeException( e );
        }
        return convertedGemmaObj;
    }

    /**
     * Locate a converter for a MAGE object and invoke it.
     * 
     * @param objectToConvert
     * @param converterBaseName
     * @param mageTypeToConvert
     * @return Converted object. If the input mageObj is null, the return value is null.
     */
    private Object findAndInvokeConverter( Object mageObj, String converterBaseName, Class<?> mageTypeToConvert ) {

        if ( mageObj == null ) return null;

        Method gemmaConverter = findConverter( converterBaseName, mageTypeToConvert );
        if ( gemmaConverter == null ) return null;

        Object convertedGemmaObj = null;
        try {
            convertedGemmaObj = gemmaConverter.invoke( this, new Object[] { mageObj } );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error(
                    "InvocationTargetException caused by " + e.getCause() + " when invoking  "
                            + gemmaConverter.getName() + " on a " + mageObj.getClass().getName(), e );
            throw new RuntimeException( e );
        }
        return convertedGemmaObj;
    }

    /**
     * @param getterObject
     * @param propertyName
     * @return
     */
    private Object findAndInvokeGetter( Object getterObject, String propertyName ) {
        Method gemmaGetter = findGetter( getterObject, propertyName );
        if ( gemmaGetter == null ) return null;
        try {
            return gemmaGetter.invoke( getterObject, new Object[] {} );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( e );
        }
        return null;
    }

    /**
     * Locate and invoke a setter method.
     * 
     * @param setterObj The object on which we will call the setter
     * @param settee The object we want to set
     * @param setteeClass The class accepted by the setter - not necessarily the class of the setterObj (example:
     *        DatabaseEntry vs. DatabaseEntryImpl)
     * @param propertyName The property name corresponding to the setteeClass
     */
    private void findAndInvokeSetter( Object setterObj, Object settee, Class<?> setteeClass, String propertyName ) {

        Method gemmaSetter = findSetter( setterObj, propertyName, setteeClass );
        if ( gemmaSetter == null ) return;

        try {
            if ( log.isDebugEnabled() ) {
                log.debug( "Setting " + settee + " on " + setterObj );
            }
            gemmaSetter.invoke( setterObj, new Object[] { settee } );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Find a converter method for a MAGE object.
     * 
     * @param mageObj
     * @return
     */
    private Method findConverter( Object mageObj ) {
        String mageTypeName = ReflectionUtil.objectToTypeName( mageObj );
        Method converter = null;
        try {
            converter = this.getClass().getMethod( "convert" + mageTypeName, new Class[] { mageObj.getClass() } );
        } catch ( NoSuchMethodException e ) {
            /*
             * This is usually an object we handle in some other way
             */
            log.warn( "Conversion operation not yet supported: " + "convert" + mageTypeName + "("
                    + mageObj.getClass().getName() + ") -- it may be handled more directly" );
        }
        return converter;
    }

    /**
     * Find a converter for an association.
     * 
     * @param associationName
     * @param mageAssociatedType
     * @return
     */
    private Method findConverter( String associationName, Class<?> mageAssociatedType ) {
        Method gemmaConverter = null;
        try {
            gemmaConverter = this.getClass()
                    .getMethod( "convert" + associationName, new Class[] { mageAssociatedType } );
            // log.debug( "Found converter: convert" + associationName + "( " + mageAssociatedType.getSimpleName() + "
            // )" );
        } catch ( NoSuchMethodException e ) {
            log.warn( "Conversion operation not yet supported: " + "convert" + associationName + "("
                    + mageAssociatedType.getName() + ")" );
        }
        return gemmaConverter;
    }

    /**
     * Given an object and a property name, get the getter method for that property.
     * 
     * @param gettee
     * @param propertyName
     * @return
     */
    private Method findGetter( Object gettee, String propertyName ) {
        Method gemmaGetter = null;
        try {
            gemmaGetter = ReflectionUtil.getBaseForImpl( gettee ).getMethod( "get" + propertyName, new Class[] {} );
        } catch ( SecurityException e ) {
            log.error( e );
        } catch ( NoSuchMethodException e ) {
            log.error( "No such getter: " + gettee.getClass().getSimpleName() + ".get" + propertyName + "(" + ")", e );
        }
        return gemmaGetter;
    }

    /**
     * Given a URI, try to find the corresponding local file. The only part of the URI that is looked at is the file
     * name. We then look in known local directory paths that are used to store MAGE-ML derived files. The search path
     * can be modified by using addLocalExternaldataPath
     * 
     * @param seekURI
     * @return URL matching the file.
     * @see addLocalExternaldataPath
     */
    private URL findLocalMageExternalDataFile( String rawFileName ) {
        String fileName = rawFileName;
        if ( fileName.lastIndexOf( File.separatorChar ) >= 0 ) {
            fileName = rawFileName.substring( rawFileName.lastIndexOf( File.separatorChar ) + 1 );
        }

        if ( log.isDebugEnabled() ) log.debug( "Seeking external data file " + fileName );
        for ( String path : this.localExternalDataPaths ) {
            File f = new File( path + File.separatorChar + fileName );
            if ( log.isDebugEnabled() ) log.debug( "Looking in " + f.getAbsolutePath() );
            if ( f.exists() ) {
                if ( log.isDebugEnabled() ) log.debug( "Found it! In " + f.getAbsolutePath() );
                try {
                    return f.toURI().toURL();
                } catch ( MalformedURLException e ) {
                    throw new RuntimeException( e );
                }
            }
        }
        return null;
    }

    /**
     * Find a setter for a property.
     * 
     * @param setter - The object on which we want to call the setter
     * @param propertyName - The name of the property we want to set
     * @param setee - The object which we want to enter as an argument to the setter.
     * @return
     */
    private Method findSetter( Object setter, String propertyName, Class<?> setee ) {
        Method gemmaSetter = null;
        try {
            gemmaSetter = ReflectionUtil.getBaseForImpl( setter ).getMethod( "set" + propertyName,
                    new Class[] { setee } );
        } catch ( SecurityException e ) {
            throw new RuntimeException( e );
        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( "No such setter: " + "set" + propertyName + "(" + setee.getSimpleName() + ")",
                    e );
        }
        return gemmaSetter;
    }

    /**
     * @return
     */
    public static ExternalDatabase getArrayExpressReference() {
        if ( arrayExpress == null ) {
            arrayExpress = ExternalDatabase.Factory.newInstance();
            arrayExpress.setName( "ArrayExpress" );
        }
        return arrayExpress;
    }

    /**
     * @param mageObj
     * @return
     */
    private Collection<BioAssay> getAssociatedSourceBioAssays( DerivedBioAssay mageObj ) {
        List<Object> map = mageObj.getDerivedBioAssayMap();
        Collection<BioAssay> bioAssays = new HashSet<BioAssay>();

        if ( map.size() > 0 ) {
            BioAssayMap dbap = ( BioAssayMap ) map.get( 0 );
            bioAssays = dbap.getSourceBioAssays();
        }

        /*
         * Alternative route, but we need BOTH (damn). If we add this we get extra bioassays with biomaterials, but it
         * doesn't help.
         */
        if ( this.bioAssayMap.containsKey( mageObj.getIdentifier() ) ) {
            bioAssays.addAll( bioAssayMap.get( mageObj.getIdentifier() ) );
        } else {
            log.debug( "No derivedbioassaymap" );
        }

        return bioAssays;
    }

    /**
     * Convenience method to access a ready-made ExternalDatabase representing the MGED Ontology.
     * 
     * @return
     */
    private ExternalDatabase getMAGEOntologyDatabaseObject() {
        if ( this.mgedOntology != null ) {
            return mgedOntology;
        }
        this.mgedOntology = ExternalDatabase.Factory.newInstance();
        mgedOntology.setName( MGED_DATABASE_IDENTIFIER );
        mgedOntology.setType( DatabaseType.ONTOLOGY );
        mgedOntology.setWebUri( MGED_ONTOLOGY_URL );
        return mgedOntology;
    }

    /**
     * For a method like "getFoo", returns "Foo".
     * 
     * @param getter
     * @return
     */
    private String getterToPropertyName( Method getter ) {
        if ( !getter.getName().startsWith( "get" ) ) throw new IllegalArgumentException( "Not a getter" );
        return getter.getName().substring( 3 );
    }

    /**
     * Turn an identifier like "Affymetrix:QuantitationType:Used" into "Used", if it follows that format. Otherwise just
     * return the identifier unmodified.
     * 
     * @param mageObj
     * @return
     */
    private String getUnqualifiedIdentifier( org.biomage.Common.Identifiable mageObj ) {
        String identifier = mageObj.getIdentifier();
        if ( identifier == null ) return null;
        return identifier.substring( identifier.lastIndexOf( ':' ) + 1, identifier.length() );
    }

    /**
     * @param gemmaObj
     * @return
     */
    private Collection<CompositeSequence> initializeCompositeSequenceCollection(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign gemmaObj ) {
        Collection<CompositeSequence> designObjs;
        if ( gemmaObj.getCompositeSequences() == null ) {
            designObjs = new HashSet<CompositeSequence>();
            gemmaObj.setCompositeSequences( designObjs );
        } else {
            designObjs = gemmaObj.getCompositeSequences();
        }
        return designObjs;
    }

    /**
     * 
     */
    private void initLocalExternalDataPaths() {
        localExternalDataPaths = new HashSet<String>();

        String path = ConfigUtils.getString( ARRAY_EXPRESS_LOCAL_DATAFILE_BASEPATH );

        if ( path == null ) {
            log.error( "No MAGE external data path is defined; please define " + ARRAY_EXPRESS_LOCAL_DATAFILE_BASEPATH );
            return;
        }

        File p = new File( path );
        if ( !p.canRead() ) {
            log.warn( "Cannot read from " + path + ", creating." );
            if ( !p.mkdirs() ) {
                log.error( "Could not make directories, bailing from path initialization; ArryExpress loading will fail." );
                return;
            }

        }
        localExternalDataPaths.add( path );

        // add temp file location.
        localExternalDataPaths.add( System.getProperty( "java.io.tmpdir" ) );

    }

    /**
     * FIXME this may duplicate functionality in the MGEDOntologyService.
     */
    private void initMGEDOntology() {
        log.info( "Reading MGED Ontology" );
        int maxTries = 3;
        for ( int i = 0; i < maxTries; i++ ) {
            try {
                mgedOntologyHelper = new OntologyHelper( MGED_ONTOLOGY_URL );
                return;
            } catch ( Exception a ) {
                try {
                    Thread.sleep( 5000 );
                } catch ( InterruptedException e1 ) {
                    throw new RuntimeException( a );
                }
            }
        }
        throw new RuntimeException( "Failed to initialize MGED Ontology" );
    }

    /**
     * Different ways the MGED ontology shows up in MAGE-ML files. Might have to be extended.
     */
    private void initMGEDOntologyAliases() {
        mgedOntologyAliases = new HashSet<String>();
        mgedOntologyAliases.add( "MGED Ontology" );
        mgedOntologyAliases.add( "MO" );
        mgedOntologyAliases.add( "ebi.ac.uk:Database:MO" );
    }

    /**
     * Initialize the conversion process by calling the getter on the MAGE object
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @return The 'got' object, either a MAGE domain object or a collection.
     */
    private Object intializeConversion( Object mageObj, Method getter ) {
        Object associatedObject = invokeGetter( mageObj, getter );

        if ( associatedObject == null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Getter called on " + mageObj.getClass().getName() + " but failed to return a value: "
                        + getter.getName() + " (Probably no data)" );
            return null;
        }

        if ( log.isDebugEnabled() )
            log.debug( mageObj.getClass().getName() + "--->" + getterToPropertyName( getter ) );

        return associatedObject;
    }

    /**
     * Call a 'get' method to retrieve an associated MAGE object for conversion.
     * 
     * @param mageObj
     * @param getter
     * @return A MAGE domain object, or a List of MAGE domain objects. The caller has to figure out which.
     */
    private Object invokeGetter( Object mageObj, Method getter ) {
        Object associatedObject = null;

        if ( getter == null ) throw new IllegalArgumentException( "Null getter passed" );
        if ( mageObj == null )
            throw new IllegalArgumentException( "Attempt to run " + getter.getName() + " on null object" );

        try {
            associatedObject = getter.invoke( mageObj, new Object[] {} );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( mageObj.getClass().getName() + "." + getter.getName() + " threw an exception: " + e.getCause(),
                    e );
        }
        return associatedObject;
    }

    private VocabCharacteristic makeCharacteristic( OntologyEntry category, OntologyEntry value ) {

        String cat = category.getValue();
        String val = value.getValue();

        Collection<String> instances;
        boolean catIsMO = mgedOntologyHelper.classExists( StringUtils.capitalize( cat ) );

        String catUri = null;
        if ( catIsMO ) {
            catUri = MGED_ONTOLOGY_URL + "#" + StringUtils.capitalize( cat );
        }

        String valUri = null;
        boolean valIsMO = mgedOntologyHelper.classExists( StringUtils.capitalize( val ) );

        if ( valIsMO ) {
            valUri = MGED_ONTOLOGY_URL + "#" + StringUtils.capitalize( val );
        }

        if ( !valIsMO && catIsMO ) {
            instances = mgedOntologyHelper.getInstances( StringUtils.capitalize( cat ) );
            if ( instances.contains( val ) ) {
                valIsMO = true;
            }
        }

        VocabCharacteristic result = VocabCharacteristic.Factory.newInstance();

        result.setCategory( cat );
        result.setValue( val );
        result.setCategoryUri( catUri );
        result.setValueUri( valUri );
        return result;
    }

    /**
     * @param result
     * @param bsample
     */
    private void processBioSampleCharacteristics( ubic.gemma.model.expression.biomaterial.BioMaterial result,
            BioSample bioSample ) {
        BioMaterial sample = convertBioSample( bioSample );

        // copy characteristics over.
        for ( Characteristic character : sample.getCharacteristics() ) {
            result.getCharacteristics().add( character );
        }

        // drill down to biosource
        for ( Object q : bioSample.getTreatments() ) {
            org.biomage.BioMaterial.Treatment sampleTreatment = ( org.biomage.BioMaterial.Treatment ) q;
            for ( Object r : sampleTreatment.getSourceBioMaterialMeasurements() ) {
                BioMaterialMeasurement bioMaterialMeas = ( BioMaterialMeasurement ) r;
                org.biomage.BioMaterial.BioMaterial bioMaterial = bioMaterialMeas.getBioMaterial();

                if ( bioMaterial instanceof BioSample ) {
                    BioSample biosample = ( BioSample ) bioMaterial;
                    processBioSampleCharacteristics( result, biosample ); // recursion.

                } else if ( bioMaterial instanceof BioSource ) {
                    processBioSourceChacteristics( result, bioMaterial );
                } else {
                    throw new UnsupportedOperationException( "Can't deal with " + bioMaterial.getClass().getName() );
                }

            }

        }
    }

    /**
     * @param result
     * @param bm2
     */
    private void processBioSourceChacteristics( ubic.gemma.model.expression.biomaterial.BioMaterial result,
            org.biomage.BioMaterial.BioMaterial bm2 ) {
        BioSource bs = ( BioSource ) bm2;
        BioMaterial source = convertBioSource( bs ); // here we should get the taxon.
        for ( Characteristic character : source.getCharacteristics() ) {
            result.getCharacteristics().add( character );
        }
        if ( source.getSourceTaxon() != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Adding " + source.getSourceTaxon() + " to " + result );
            result.setSourceTaxon( source.getSourceTaxon() );
        }
    }

    /**
     * Generic method to fill in a Gemma object where the association in Mage has cardinality of >1.
     * 
     * @param associatedList - The result of the Getter call.
     * @param gemmObj - The Gemma object in which to place the converted Mage object(s).
     * @param onlyTakeOne - This indicates that the cardinality in the Gemma object is at most 1. Therefore we pare down
     *        the Mage object list to take just the first one.
     */
    private void simpleFillIn( List<Object> associatedList, Object gemmaObj, Method getter, boolean onlyTakeOne ) {
        this.simpleFillIn( associatedList, gemmaObj, getter, onlyTakeOne, null, null );
    }

    /**
     * Generic method to fill in a Gemma object where the association in Mage has cardinality of >1.
     * 
     * @param associatedList - The result of the Getter call.
     * @param gemmObj - The Gemma object in which to place the converted Mage object(s). This might be a collection.
     * @param onlyTakeOne - This indicates that the cardinality in the Gemma object is at most 1. Therefore we pare down
     *        the Mage object list to take just the first one.
     * @param actualGemmaAssociationName - for example, a BioSequence hasa "SequenceDatabaseEntry", not a
     *        "DatabaseEntry". If null, the name is inferred.
     */
    private void simpleFillIn( List<Object> associatedList, Object gemmaObj, Method getter, boolean onlyTakeOne,
            String actualGemmaAssociationName ) {
        simpleFillIn( associatedList, gemmaObj, getter, onlyTakeOne, actualGemmaAssociationName, null );
    }

    /**
     * Generic method to fill in a Gemma object where the association in Mage has cardinality of >1.
     * 
     * @param associatedList - The result of the Getter call.
     * @param gemmObj - The Gemma object in which to place the converted Mage object(s). This might be a collection.
     * @param onlyTakeOne - This indicates that the cardinality in the Gemma object is at most 1. Therefore we pare down
     *        the Mage object list to take just the first one.
     * @param actualGemmaAssociationName - for example, a BioSequence hasa "SequenceDatabaseEntry", not a
     *        "DatabaseEntry". If null, the name is inferred.
     * @param actualArgumentClass For example, we might get a VocabCharacteristic but method will only match
     *        Characteristic. Only used if onlyTakeOne = true.
     */
    private void simpleFillIn( List<Object> associatedList, Object gemmaObj, Method getter, boolean onlyTakeOne,
            String actualGemmaAssociationName, Class<?> actualArgumentClass ) {

        if ( associatedList == null || gemmaObj == null || getter == null )
            throw new IllegalArgumentException( "Null objects" );

        if ( associatedList.size() == 0 ) {
            log.debug( "List was not null, but empty" );
            return;
        }

        // This could be refactored to share more code with the other simpleFillIn methods.
        String associationName = actualGemmaAssociationName;
        if ( associationName == null ) associationName = getterToPropertyName( getter );

        try {
            if ( onlyTakeOne ) {
                Object mageObj = associatedList.get( 0 );
                Object convertedGemmaObj = findAndInvokeConverter( mageObj );
                if ( convertedGemmaObj == null ) return; // not supported.
                Class<?> convertedGemmaClass;
                if ( actualArgumentClass != null ) {
                    convertedGemmaClass = actualArgumentClass;
                } else {
                    convertedGemmaClass = ReflectionUtil.getBaseForImpl( convertedGemmaObj );
                }
                log.debug( "Converting a MAGE list to a single instance of " + convertedGemmaClass.getSimpleName() );
                findAndInvokeSetter( gemmaObj, convertedGemmaObj, convertedGemmaClass, associationName );
            } else {
                // Collection
                Class<?> gemmaClass = ReflectionUtil.getBaseForImpl( gemmaObj );
                log.debug( "Converting a MAGE list to a Gemma list associated with a " + gemmaClass.getSimpleName() );
                Collection<Object> gemmaObjList = ( Collection<Object> ) findAndInvokeGetter( gemmaObj, associationName );
                if ( gemmaObjList == null ) {
                    gemmaObjList = new HashSet<Object>();
                } else if ( gemmaObjList.size() > 0 ) {
                    log.warn( "**** " + gemmaObjList + " (" + associationName + ") already contains "
                            + gemmaObjList.size() + " elements" );
                }

                // avoid adding the same object twice.
                for ( Object mageObj : associatedList ) {
                    Object convertedGemmaObj = findAndInvokeConverter( mageObj );
                    if ( convertedGemmaObj == null ) continue; // not supported.
                    if ( !gemmaObjList.contains( convertedGemmaObj ) ) {
                        if ( log.isDebugEnabled() ) {
                            log.debug( "Adding " + convertedGemmaObj + " to " + gemmaObjList + " (" + associationName
                                    + ")" );
                        }
                        if ( convertedGemmaObj instanceof Collection ) {
                            gemmaObjList.addAll( ( Collection<Object> ) convertedGemmaObj );
                        } else {
                            gemmaObjList.add( convertedGemmaObj );
                        }
                    }
                }
                findAndInvokeSetter( gemmaObj, gemmaObjList, Collection.class, associationName );
            }
        } catch ( SecurityException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        }

    }

    /**
     * Generic method to fill in a Gemma object's association with a Mage object where the name can be predicted from
     * the associated object type. E.g., the Gemma object with an association to "BioSequence" has a "bioSequence"
     * property; sometimes instead we have things like ImmobilizedCharacteristic.
     * 
     * @param associatedMageObject The associated object we need to find a place for.
     * @param gemmaObj The Gemma object in which to place the converted Mage object.
     * @param getter The getter for the Mage object
     */
    private void simpleFillIn( Object associatedMageObject, Object gemmaObj, Method getter ) {
        this.simpleFillIn( associatedMageObject, gemmaObj, getter, null );
    }

    /**
     * Generic method to fill in a Gemma object's association with a Mage object where the name might be predicted from
     * the associated object type. E.g., the Gemma object with an association to "BioSequence" has a "bioSequence"
     * property; sometimes instead we have things like ImmobilizedCharacteristic.
     * 
     * @param associatedMageObject The associated object we need to find a place for.
     * @param gemmaObj The Gemma object in which to place the converted Mage object.
     * @param getter The getter for the Mage object
     * @param actualGemmaAssociationName - Replacement name for the Gemma association. This is to handle situations
     *        where the getter does not have a name that can be figured out. If null, the name is figured out from the
     *        getter.
     */
    private void simpleFillIn( Object associatedMageObject, Object gemmaObj, Method getter,
            String actualGemmaAssociationName ) {
        simpleFillIn( associatedMageObject, gemmaObj, getter, actualGemmaAssociationName, null );

    }

    /**
     * @param associatedMageObject
     * @param gemmaObj
     * @param getter
     * @param actualGemmaAssociationName
     * @param actualArgumentClass - example, sometimes we get a VocabCharacteristic but setter wants a Characteristic.
     * @see simpleFillIn( Object associatedMageObject, Object gemmaObj, Method getter, String actualGemmaAssociationName
     *      )
     */
    private void simpleFillIn( Object associatedMageObject, Object gemmaObj, Method getter,
            String actualGemmaAssociationName, Class<?> actualArgumentClass ) {

        if ( associatedMageObject == null ) return;
        String associationName = getterToPropertyName( getter );
        String inferredGemmaAssociationName = actualGemmaAssociationName == null ? associationName
                : actualGemmaAssociationName;

        try {
            Class<?> mageAssociatedType = associatedMageObject.getClass();
            Object gemmaAssociatedObj = findAndInvokeConverter( associatedMageObject, associationName,
                    mageAssociatedType );
            if ( gemmaAssociatedObj == null ) return;

            Class<?> gemmaClass;
            if ( actualArgumentClass != null ) {
                gemmaClass = actualArgumentClass;
            } else {
                gemmaClass = ReflectionUtil.getBaseForImpl( gemmaAssociatedObj.getClass() );
            }
            findAndInvokeSetter( gemmaObj, gemmaAssociatedObj, gemmaClass, inferredGemmaAssociationName );

        } catch ( SecurityException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        }
    }

    /**
     * From a PhysicalBioAssay, find the associated ArrayDesign, and also sort out the biomaterials. This is important
     * because the biomaterial information is kind of hard to get except via this indirect route.
     * 
     * @param mageObj
     * @param result
     */
    private void specialConvertAssociationsForPhysicalBioAssay( PhysicalBioAssay mageObj,
            ubic.gemma.model.expression.bioAssay.BioAssay result ) {

        Collection<BioMaterial> biomaterials = new HashSet<BioMaterial>();

        BioAssayCreation bac = mageObj.getBioAssayCreation();
        if ( bac != null ) {

            String arrayId = bac.getArray().getIdentifier();
            log.debug( arrayId + "....Array" );

            /*
             * If array2BioAssay is empty, the MAGE-ML lacks the Array package.
             */
            if ( array2BioAssay.size() > 0 ) {

                if ( array2BioAssay.containsKey( arrayId ) ) {
                    array2BioAssay.get( arrayId ).add( mageObj );
                } else {
                    throw new IllegalStateException( "Bioassay " + mageObj.getIdentifier()
                            + " associated with unexpected array design id : " + arrayId );
                }
            }

            ArrayDesign ad = bac.getArray().getArrayDesign();
            if ( ad == null ) {
                /*
                 * This happens if the ArrayDesign package is missing, e.g. E-SMDB-1853. Pain but we deal with it later.
                 */
                // log.warn( "No array Design for " + result + " from " + mageObj );
            } else {
                ubic.gemma.model.expression.arrayDesign.ArrayDesign conv = convertArrayDesign( ad );

                assert conv != null;

                if ( log.isTraceEnabled() )
                    log.trace( "Adding array design used " + ad.getName() + " to " + result.getName() );

                if ( result.getArrayDesignUsed() != null && !result.getArrayDesignUsed().equals( conv ) ) {
                    throw new IllegalStateException( "Array design doesn't match one previously found for " + result );
                }

                result.setArrayDesignUsed( conv );
            }

            /*
             * One route to the biosource...
             */
            Collection<BioMaterialMeasurement> measurements = bac.getSourceBioMaterialMeasurements();

            Collection<FactorValue> factorValues = this.bioAssayFactors.get( result.getName() );

            for ( BioMaterialMeasurement bmm : measurements ) {
                if ( log.isDebugEnabled() ) log.debug( "Converting " + bmm.getBioMaterial() + " for " + mageObj );
                BioMaterial biomaterial = convertBioMaterial( bmm.getBioMaterial() );
                addFactorValuesToBioMaterial( biomaterial, factorValues );
                biomaterials.add( biomaterial );
                biomaterial.getBioAssaysUsedIn().add( result );
            }
        }

        if ( biomaterials.size() == 0 ) { // check to avoid endless loops.
            /*
             * ANOTHER route to the biosource
             */
            if ( physicalBioAssay2physicalBioAssay.containsKey( mageObj.getIdentifier() )
                    && result.getSamplesUsed().size() == 0 ) {
                PhysicalBioAssay sourceBioAssay = physicalBioAssay2physicalBioAssay.get( mageObj.getIdentifier() );
                specialConvertAssociationsForPhysicalBioAssay( sourceBioAssay, result );
            } else {

                /*
                 * ANOTHER way to the biosource
                 */
                for ( Object o : mageObj.getBioAssayTreatments() ) {
                    BioAssayTreatment t = ( BioAssayTreatment ) o;
                    PhysicalBioAssay pb = t.getTarget();
                    if ( result.getSamplesUsed().size() == 0 ) {
                        specialConvertAssociationsForPhysicalBioAssay( pb, result );
                    }
                }
            }
        }
        /*
         * Add the biomaterials to the bioassay.
         */
        if ( result.getSamplesUsed().size() > 0 ) {
            /*
             * Sample already has biomaterials...check we don't add twice.
             */
            for ( BioMaterial newbm : biomaterials ) {
                boolean found = false;
                String nam = newbm.getName();
                for ( BioMaterial bm : result.getSamplesUsed() ) {
                    if ( bm.getName().equals( nam ) ) {
                        found = true;
                    }
                }
                if ( !found ) {
                    result.getSamplesUsed().add( newbm );
                }
            }
            if ( result.getSamplesUsed().size() > 1 ) {
                // throw new UnsupportedOperationException( "More than one biomaterial per bioassay not supported" );
                log.warn( "More than one biomaterial per bioassay not supported." );
            }
        } else {
            result.setSamplesUsed( biomaterials );
        }
    }

    /**
     * Special case to convert BioAssayData associations of a BioAssay object. We only store references to the data
     * files - there is no BioAssayData object in gemma.model.
     * 
     * @param list BioAssayData objects to be handled.
     * @param gemmaObj Gemma BioAssay object to attach data files to.
     */
    private void specialConvertBioAssayBioAssayDataAssociations( List<BioAssayData> bioAssayData,
            ubic.gemma.model.expression.bioAssay.BioAssay gemmaObj ) {

        for ( BioAssayData bioAssayDatum : bioAssayData ) {
            LocalFile lf = convertBioAssayData( bioAssayDatum );
            if ( lf == null ) continue;

            if ( bioAssayDatum instanceof DerivedBioAssayData ) {
                DerivedBioAssayData derivedBioAssayData = ( DerivedBioAssayData ) bioAssayDatum;
                if ( gemmaObj.getDerivedDataFiles() == null ) gemmaObj.setDerivedDataFiles( new HashSet<LocalFile>() );

                gemmaObj.getDerivedDataFiles().add( lf );

                Transformation transformation = derivedBioAssayData.getProducerTransformation();
                List<BioAssayData> sources = transformation.getBioAssayDataSources();

                if ( sources.size() > 1 ) {
                    log.warn( "Derived bioassayData maps to more than one other bioassaydata!" );
                }

                for ( BioAssayData sourceData : sources ) {
                    if ( sourceData instanceof MeasuredBioAssayData ) {
                        MeasuredBioAssayData measuredSourceData = ( MeasuredBioAssayData ) sourceData;
                        gemmaObj.setRawDataFile( convertMeasuredBioAssayData( measuredSourceData ) );
                    }
                }

            } else if ( bioAssayDatum instanceof MeasuredBioAssayData ) {
                log.debug( "Got raw data file" );
                gemmaObj.setRawDataFile( lf );
            } else {
                throw new IllegalArgumentException( "Unknown BioAssayData class: " + bioAssayDatum.getClass().getName() );
            }
            log.debug( bioAssayDatum.getIdentifier() );
        }
    }

    /**
     * Extract compositeSequence information from the ArrayDesign package. The ArrayDesign package doesn't have any
     * information about the compositeSequences, other than the fact that they belong to this arrayDesign.
     * 
     * @param compositeGroups
     * @param gemmaObj
     */
    private void specialConvertCompositeGroups( List<CompositeGroup> compositeGroups,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign gemmaObj ) {

        Collection<CompositeSequence> designObjs = initializeCompositeSequenceCollection( gemmaObj );

        Taxon taxon = null;

        for ( CompositeGroup compositeGroup : compositeGroups ) {
            List<org.biomage.DesignElement.CompositeSequence> reps = compositeGroup.getCompositeSequences();
            for ( org.biomage.DesignElement.CompositeSequence compseq : reps ) {
                CompositeSequence csconv = convertCompositeSequence( compseq );

                if ( csconv.getBiologicalCharacteristic().getTaxon() != null )
                    taxon = csconv.getBiologicalCharacteristic().getTaxon();
                csconv.setArrayDesign( gemmaObj );
                if ( !designObjs.contains( csconv ) ) designObjs.add( csconv );
            }
        }

        gemmaObj.setPrimaryTaxon( taxon );
        gemmaObj.setCompositeSequences( designObjs );
        gemmaObj.setAdvertisedNumberOfDesignElements( designObjs.size() );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     */
    private void specialConvertFeatureExtraction( FeatureExtraction mageObj,
            ubic.gemma.model.expression.bioAssay.BioAssay gemmaObj ) {
        PhysicalBioAssay pba = mageObj.getPhysicalBioAssaySource();
        specialConvertAssociationsForPhysicalBioAssay( pba, gemmaObj );
        convertAssociations( pba, gemmaObj );
    }

    /**
     * Special case, OntologyEntry maps to an Enum.
     * 
     * @param mageObj
     * @return SequenceType
     */
    private void specialConvertSequenceType( OntologyEntry mageObj, BioSequence gemmaObj ) {

        if ( mageObj == null ) return;
        String value = mageObj.getValue();
        if ( value.equalsIgnoreCase( "bc" ) ) {
            gemmaObj.setType( SequenceType.BAC );
        } else if ( value.equalsIgnoreCase( "est" ) ) {
            gemmaObj.setType( SequenceType.EST );
        } else if ( value.equalsIgnoreCase( "affyprobe" ) ) {
            gemmaObj.setType( SequenceType.AFFY_PROBE );
        } else if ( value.equalsIgnoreCase( "affytarget" ) ) {
            gemmaObj.setType( SequenceType.AFFY_TARGET );
        } else if ( value.equalsIgnoreCase( "mrna" ) ) {
            gemmaObj.setType( SequenceType.mRNA );
        } else if ( value.equalsIgnoreCase( "refseq" ) ) {
            gemmaObj.setType( SequenceType.REFSEQ );
        } else if ( value.equalsIgnoreCase( "chromosome" ) ) {
            gemmaObj.setType( SequenceType.WHOLE_CHROMOSOME );
        } else if ( value.equalsIgnoreCase( "genome" ) ) {
            gemmaObj.setType( SequenceType.WHOLE_GENOME );
        } else if ( value.equalsIgnoreCase( "orf" ) ) {
            gemmaObj.setType( SequenceType.ORF );
        } else if ( value.equalsIgnoreCase( "dna" ) ) {
            gemmaObj.setType( SequenceType.DNA );
        } else {
            gemmaObj.setType( SequenceType.OTHER );
        }
    }

    /**
     * Extract the feature location information for a MAGE reporter and fill it into the Gemma Reporter.
     * 
     * @param mageObj
     * @param result
     */
    private void specialGetReporterFeatureLocations( org.biomage.DesignElement.Reporter mageObj, Reporter result ) {
        if ( mageObj == null ) return;
        if ( result == null ) throw new IllegalArgumentException( "Null Reporter passed" );
        List<FeatureReporterMap> featureReporterMaps = mageObj.getFeatureReporterMaps();
        if ( featureReporterMaps == null ) return;
        for ( FeatureReporterMap featureReporterMap : featureReporterMaps ) {
            if ( featureReporterMap == null ) continue;
            List<FeatureInformation> featureInformationSources = featureReporterMap.getFeatureInformationSources();
            for ( FeatureInformation featureInformation : featureInformationSources ) {
                if ( featureInformation == null ) continue;

                if ( featureInformation.getFeature() == null || featureInformation.getFeature().getPosition() == null )
                    continue;

                result.setCol( featureInformation.getFeature().getPosition().getX().intValue() );
                result.setRow( featureInformation.getFeature().getPosition().getY().intValue() );
                break;
            }
            break;
        }

    }
}
