package edu.columbia.gemma.loader.mage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.biomage.BioAssay.Channel;
import org.biomage.BioAssay.DerivedBioAssay;
import org.biomage.BioAssay.MeasuredBioAssay;
import org.biomage.BioAssay.PhysicalBioAssay;
import org.biomage.BioMaterial.BioSample;
import org.biomage.BioMaterial.BioSource;
import org.biomage.BioMaterial.LabeledExtract;
import org.biomage.Common.Describable;
import org.biomage.Common.Extendable;
import org.biomage.Description.Database;
import org.biomage.Description.Description;
import org.biomage.Description.OntologyEntry;
import org.biomage.DesignElement.FeatureInformation;
import org.biomage.DesignElement.FeatureReporterMap;
import org.biomage.DesignElement.ReporterCompositeMap;
import org.biomage.DesignElement.ReporterPosition;
import org.biomage.Experiment.Experiment;
import org.biomage.Experiment.ExperimentDesign;
import org.biomage.Measurement.Unit;
import org.biomage.Measurement.Measurement.KindCV;
import org.biomage.Measurement.Measurement.Type;
import org.biomage.Protocol.Hardware;
import org.biomage.Protocol.Protocol;
import org.biomage.QuantitationType.DerivedSignal;
import org.biomage.QuantitationType.MeasuredSignal;
import org.biomage.QuantitationType.PValue;
import org.biomage.QuantitationType.PresentAbsent;
import org.biomage.QuantitationType.QuantitationType;
import org.biomage.QuantitationType.Ratio;
import org.biomage.QuantitationType.SpecializedQuantitationType;
import org.biomage.QuantitationType.StandardQuantitationType;

import edu.columbia.gemma.common.Identifiable;
import edu.columbia.gemma.common.IdentifiableDao;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.measurement.Measurement;
import edu.columbia.gemma.common.measurement.MeasurementKind;
import edu.columbia.gemma.common.measurement.MeasurementType;
import edu.columbia.gemma.common.quantitationtype.PrimitiveType;
import edu.columbia.gemma.common.quantitationtype.ScaleType;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.biomaterial.Treatment;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.FactorValue;
import edu.columbia.gemma.loader.loaderutils.IdentifierCreator;
import edu.columbia.gemma.genome.biosequence.PolymerType;
import edu.columbia.gemma.genome.biosequence.SequenceType;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.util.PrettyPrinter;
import edu.columbia.gemma.util.ReflectionUtil;

/**
 * Class to convert Mage domain objects to Gemma domain objects. In most cases, the user can simply call the "convert"
 * method on any MAGE domain object and get a fully-populated Gemma domain object. There is no need to use the methods
 * in this class directly when handling MAGE-ML files: use the {@link edu.columbia.gemma.loader.mage.MageMLParser.}
 * <p>
 * The names of attributes etc. should be in an external file.
 * <h2>Zoo of packages that have references between them</h2>
 * <h3>DesignElement_package and ArrayDesign_package</h3>
 * <p>
 * DesignElement Contains the ReporterCompositeMap (and the FeatureReporterMap). This allows us to fill in the map in
 * the CompositeSequence.
 * <p>
 * ArrayDesign Contains the CompositeGropus, FeatureGroups and ReporterGroups. This leads us to a description of all the
 * DesignElement on the array, but not the reportercompositemap.
 * <p>
 * Thus both of these have references to CompositeSequences for example; the two packages can be in different files.
 * Therefore we need to fill in object that have the same Identifier with data from the other file.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @see edu.columbia.gemma.loader.mage.MageMLParser
 * @author pavlidis
 * @version $Id$
 */
public class MageMLConverter {

    /**
     * Used to indicate that a MAGE list should be converted to a Gemma list (or collection)
     */
    public static final boolean CONVERT_ALL = false;

    /**
     * Used to indicate that when a MAGE list is encountered, we should only process the first element of the list.
     */
    public static final boolean CONVERT_FIRST_ONLY = true;

    public static final Log log = LogFactory.getLog( MageMLConverter.class );
    public static final String START_AUDIT_NOTE = "Imported from MAGE";

    /**
     * Used to hold references to Identifiables, so we don't convert them over and over again.
     */
    public Map identifiableCache;

    private IdentifiableDao identifiableDao;
    private TaxonDao taxonDao;

    /**
     * 
     *
     */
    public MageMLConverter() {
        identifiableCache = new HashMap();
    }

    /**
     * A generic converter that figures out which specific conversion method to call based on the class of the object.
     * 
     * @param mageObj
     * @return
     */
    public Object convert( Object mageObj ) {
        assert identifiableDao != null;
        return findAndInvokeConverter( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.arrayDesign.ArrayDesign convertArrayDesign( ArrayDesign mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "(Incomplete) Converting Array design: " + mageObj.getName() );

        edu.columbia.gemma.expression.arrayDesign.ArrayDesign result = edu.columbia.gemma.expression.arrayDesign.ArrayDesign.Factory
                .newInstance();

        result.setNumberOfFeatures( mageObj.getNumberOfFeatures().intValue() );

        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertArrayDesignAssociations( ArrayDesign mageObj,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "CompositeGroups" ) ) {
            assert associatedObject instanceof List;
            specialConvertCompositeGroups( ( List ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "DesignProviders" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "FeatureGroups" ) ) {
            assert associatedObject instanceof List;
            specialConvertFeatureGroups( ( List ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "ProtocolApplications" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "ReporterGroups" ) ) {
            assert associatedObject instanceof List;
            specialConvertReporterGroups( ( List ) associatedObject, gemmaObj );
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
            inferredGemmaAssociationName = ReflectionUtil.classToTypeName( ReflectionUtil
                    .getBaseForImpl( gemmaAssociatedObj ) );
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
        convertAssociations( mageObj.getClass(), mageObj, gemmaObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.BibliographicReference convertBibliographicReference(
            BibliographicReference mageObj ) {
        if ( mageObj == null ) return null;
        log.debug( "Converting BibliographicReference " + mageObj.getTitle() );
        edu.columbia.gemma.common.description.BibliographicReference result = edu.columbia.gemma.common.description.BibliographicReference.Factory
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
        // result.setFullTextURI( mageObj.getURI() );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBibliographicReferenceAssociations( BibliographicReference mageObj,
            edu.columbia.gemma.common.description.BibliographicReference gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Accessions" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "PubAccession" );
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
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertBioAssay( BioAssay mageObj ) {
        if ( mageObj == null ) return null;
        log.debug( "Converting BioAssay -- associations with array designs not implemented yet "
                + mageObj.getIdentifier() );

        edu.columbia.gemma.expression.bioAssay.BioAssay result = edu.columbia.gemma.expression.bioAssay.BioAssay.Factory
                .newInstance();

        // FIXME - we need the array designs to get filled in.

        if ( !convertIdentifiable( mageObj, result ) ) {
            convertAssociations( mageObj, result );
        }
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioAssayAssociations( BioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {

        log.warn( "convertBioAssayAssociations not fully supported!" );
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "BioAssayFactorValues" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "BioAssayFactorValues" );
        } else if ( associationName.equals( "Channels" ) ) {
            ; // we don't support this.
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.biomaterial.BioMaterial convertBioMaterial(
            org.biomage.BioMaterial.BioMaterial mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.expression.biomaterial.BioMaterial result = edu.columbia.gemma.expression.biomaterial.BioMaterial.Factory
                .newInstance();

        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioMaterialAssociations( org.biomage.BioMaterial.BioMaterial mageObj, BioMaterial gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Characteristics" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Characteristics" );
        } else if ( associationName.equals( "MaterialType" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "MaterialType" );
        } else if ( associationName.equals( "QualityControlStatistics" ) ) {
            assert associatedObject instanceof List;
            // we don't support
        } else if ( associationName.equals( "Treatments" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Treatments" );
        } else {
            log.debug( "Unsupported or unknown association, or from subclass: " + associationName );
        }

    }

    /**
     * @param mageObj
     * @return
     */
    public BioMaterial convertBioSample( BioSample mageObj ) {
        log.debug( "Converting BioSample: " + mageObj.getIdentifier() );

        if ( mageObj == null ) return null;

        BioMaterial result = BioMaterial.Factory.newInstance();

        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
        return result;
    }

    /**
     * TODO - call superclass converter.
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioSampleAssociations( BioSample mageObj, BioMaterial gemmaObj, Method getter ) {
        log.debug( "convertBioMaterialAssociations (biosample) Not fully supported yet" );
        if ( mageObj == null ) return;

        convertBioMaterialAssociations( mageObj, gemmaObj, getter );

        Object associatedObject = intializeConversion( mageObj, getter );

        if ( associatedObject == null ) return;

        String associationName = getterToPropertyName( getter );

        if ( associationName.equals( "Type" ) ) {
            // we don't support.
        } else {
            log.debug( "Unknown or unsupported type, or is for superclass " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return edu.columbia.gemma.sequence.biosequence.BioSequence
     */
    public edu.columbia.gemma.genome.biosequence.BioSequence convertBioSequence(
            org.biomage.BioSequence.BioSequence mageObj ) {

        log.debug( "Converting BioSequence " + mageObj.getIdentifier() );

        if ( mageObj == null ) return null;

        edu.columbia.gemma.genome.biosequence.BioSequence result = edu.columbia.gemma.genome.biosequence.BioSequence.Factory
                .newInstance();

        result.setSequence( mageObj.getSequence() );
        if ( mageObj.getLength() != null ) result.setLength( mageObj.getLength().intValue() );
        if ( mageObj.getIsApproximateLength() != null )
            result.setIsApproximateLength( mageObj.getIsApproximateLength().booleanValue() );
        if ( mageObj.getIsCircular() != null ) result.setIsCircular( mageObj.getIsCircular().booleanValue() );

        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
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
            edu.columbia.gemma.genome.biosequence.BioSequence gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );

        if ( associatedObject == null ) return;

        String associationName = getterToPropertyName( getter );

        if ( associationName.equals( "PolymerType" ) ) { // Ontology Entry - enumerated type.
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "SequenceDatabases" ) ) { // list of DatabaseEntries, we use one
            List seqdbs = ( List ) associatedObject;
            simpleFillIn( seqdbs, gemmaObj, getter, true, "SequenceDatabaseEntry" );
        } else if ( associationName.equals( "Type" ) ) { // ontology entry, we map to a enumerated type.
            simpleFillIn( associatedObject, gemmaObj, getter, "Type" ); // yes, we do.
        } else if ( associationName.equals( "Species" ) ) { // ontology entry, we map to a enumerated type.
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "SeqFeatures" ) ) {
            ; // list of Sequence features, we ignore
        } else if ( associationName.equals( "OntologyEntries" ) ) {
            ; // list of generic ontology entries, we ignore.
        } else {
            log.debug( "Unknown or unsupported type " + associationName );
        }

    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.biomaterial.BioMaterial convertBioSource( BioSource mageObj ) {
        if ( mageObj == null ) return null;

        edu.columbia.gemma.expression.biomaterial.BioMaterial result = convertBioMaterial( mageObj );

        // mageObj.getSourceContact();
        return result;
    }

    /**
     * FIXME - need to push anything useful up to a single biomaterial.
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioSourceAssociations( BioSource mageObj, BioMaterial gemmaObj, Method getter ) {
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "SourceContacts" ) ) {
            // no-op, we don't support this.
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertCategory( OntologyEntry mageObj ) {
        return convertOntologyEntry( mageObj );
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
    public edu.columbia.gemma.expression.designElement.CompositeSequence convertCompositeSequence(
            org.biomage.DesignElement.CompositeSequence mageObj ) {

        if ( mageObj == null ) return null;

        CompositeSequence result = CompositeSequence.Factory.newInstance();

        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertCompositeSequenceAssociations( org.biomage.DesignElement.CompositeSequence mageObj,
            edu.columbia.gemma.expression.designElement.CompositeSequence gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "BiologicalCharacteristics" ) ) {
            if ( ( ( List ) associatedObject ).size() > 1 )
                log.warn( "*** Moroe than one BiologicalCharacteristic for a MAGE CompositeSequence!" );
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "BiologicalCharacteristic" );
        } else if ( associationName.equals( "CompositeCompositeMaps" ) ) {
            ; // we don't support.
        } else if ( associationName.equals( "ReporterCompositeMaps" ) ) {
            // special case. This is complicated, because the mage model has compositeSequence ->
            // reportercompositemap(s) -> reporterposition(s) -> reporter(1)
            gemmaObj.setReporters( specialConvertReporterCompositeMaps( ( List ) associatedObject ) );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * no-op for now.
     * 
     * @param mageObj
     * @return
     */
    public Object convertCompound( org.biomage.BioMaterial.Compound mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.auditAndSecurity.Contact convertContact( Contact mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public ExternalDatabase convertDatabase( Database mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting Database " + mageObj.getIdentifier() );

        ExternalDatabase result = ExternalDatabase.Factory.newInstance();
        result.setWebURI( mageObj.getURI() );
        // we don't use version.
        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDatabaseAssociations( Database mageObj,
            edu.columbia.gemma.common.description.ExternalDatabase gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "Contacts" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Contact" );
        else
            log.debug( "Unsupported or unknown association: " + associationName );
    }

    /**
     * @param mageObj
     * @return
     */
    public DatabaseEntry convertDatabaseEntry( org.biomage.Description.DatabaseEntry mageObj ) {

        if ( mageObj == null ) return null;

        log.debug( "Converting DatabaseEntry " + mageObj.getAccession() );

        DatabaseEntry result = DatabaseEntry.Factory.newInstance();
        result.setAccession( mageObj.getAccession() );
        result.setAccessionVersion( mageObj.getAccessionVersion() );
        result.setURI( mageObj.getURI() );
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
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Database" ) )
            simpleFillIn( associatedObject, gemmaObj, getter );
        else if ( associationName.equals( "Type" ) )
            ; // we ain't got that.
        else
            log.debug( "Unsupported or unknown association: " + associationName );
    }

    /**
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
        } else if ( val.equalsIgnoreCase( "double" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "float" ) ) {
            return PrimitiveType.FLOAT;
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
        }
        log.error( "Unrecognized DataType " + val );
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertDerivedBioAssay( DerivedBioAssay mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting DerivedBioAssay " + mageObj.getIdentifier() );

        edu.columbia.gemma.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );
        convertAssociations( mageObj, result ); // FIXME this has to only do the subclass part.
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDerivedBioAssayAssociations( DerivedBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "DerivedBioAssayMap" ) ) {
            if ( ( ( List ) associatedObject ).size() > 0 ) log.warn( "Missing out on DerivedBioAssayMap" );
        } else if ( associationName.equals( "DerivedBioAssayData" ) ) {
            if ( ( ( List ) associatedObject ).size() > 0 ) log.warn( "Missing out on DerivedBioAssayData" );
            ; // FIXME this may be what we actually use.
            ;
        } else if ( associationName.equals( "Type" ) ) {
            // simpleFillIn( associatedObject, gemmaObj, getter, "Type" );
        } else if ( associationName.equals( "Channels" ) || associationName.equals( "BioAssayFactorValues" ) ) {
            ; // nothing.
        } else {
            log.warn( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertDerivedSignal( DerivedSignal mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDerivedSignalAssociations( DerivedSignal mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * Convert a MAGE Describable to a Gemma domain object. We only allow a single description, so we take the first
     * one. The association to Security and Audit are not filled in here.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertDescribable( Describable mageObj, edu.columbia.gemma.common.Describable gemmaObj ) {

        if ( mageObj == null ) return;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        if ( mageObj.getDescriptions().size() > 0 ) {
            gemmaObj.setDescription( convertDescription( ( Description ) mageObj.getDescriptions().get( 0 ) ) );
        }

        if ( mageObj.getDescriptions().size() > 1 )
            log.warn( "***** There were multiple descriptions from a MAGE.Describable! ***** " );

        convertExtendable( mageObj, gemmaObj );
    }

//    /**
//     * todo: mage Description isa Describable.
//     * 
//     * @param mageObj
//     * @return edu.columbia.gemma.common.description.Description
//     */
//    public edu.columbia.gemma.common.description.Description convertDescription(
//            org.biomage.Description.Description mageObj ) {
//
//        if ( mageObj == null ) return null;
//
//        log.debug( "Converting Description: " + mageObj.getText() );
//
//        edu.columbia.gemma.common.description.Description result = edu.columbia.gemma.common.description.Description.Factory
//                .newInstance();
//        result.setText( mageObj.getText() );
//        result.setURI( mageObj.getURI() );
//        convertAssociations( mageObj, result );
//
//        return result;
//    }

//    /**
//     * @param mageObj
//     * @param gemmaObj
//     * @param getter
//     */
//    public void convertDescriptionAssociations( Description mageObj,
//            edu.columbia.gemma.common.description.Description gemmaObj, Method getter ) {
//        Object associatedObject = intializeConversion( mageObj, getter );
//        String associationName = getterToPropertyName( getter );
//        if ( associatedObject == null ) return;
//        if ( associationName.equals( "BibliographicReferences" ) ) {
//            if ( ( ( List ) associatedObject ).size() > 0 )
//                log.warn( "*** A MAGE description had Bibliographic References! Description is " + mageObj.getText() );
//            if ( ( ( List ) associatedObject ).size() > 1 )
//                log.warn( "*** Multiple bibliographic references for a MAGE description! Description is "
//                        + mageObj.getText() );
//            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "BibliographicReference" );
//        } else if ( associationName.equals( "DatabaseReferences" ) ) {
//            if ( ( ( List ) associatedObject ).size() > 0 )
//                log.warn( "*** A MAGE description had Database Entries! Description is " + mageObj.getText() );
//            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "DatabaseEntries" );
//        } else if ( associationName.equals( "Annotations" ) ) {
//            if ( ( ( List ) associatedObject ).size() > 0 ) {
//                log.warn( "*** A MAGE description had Annotations! " );
//                log.warn( PrettyPrinter.print( mageObj ) );
//            }
//            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Annotations" );
//        } else {
//            log.debug( "Unsupported or unknown association: " + associationName );
//        }
//    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertError(
            org.biomage.QuantitationType.Error mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertErrorAssociations( org.biomage.QuantitationType.Error mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public ExpressionExperiment convertExperiment( Experiment mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting Experiment: " + mageObj.getName() );

        ExpressionExperiment result = ExpressionExperiment.Factory.newInstance();
        result.setSource( "Imported from MAGE-ML" ); // duh.

        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @return
     */
    public ExperimentalFactor convertExperimentalFactor( org.biomage.Experiment.ExperimentalFactor mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting ExperimentalFactor: " + mageObj.getIdentifier() );
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
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
            simpleFillIn( associatedObject, gemmaObj, getter, "Category" );
        else if ( associationName.equals( "Annotations" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Annotations" );
        else if ( associationName.equals( "FactorValues" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "FactorValues" );
        else
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
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, null );
        } else if ( associationName.equals( "BioAssays" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "BioAssays" );
        } else if ( associationName.equals( "Providers" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Provider" );
        } else if ( associationName.equals( "BioAssayData" ) ) {
            assert associatedObject instanceof List;
            // simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "BioAssayData" );
            // FIXME this is a potential problem, need to deal with specially.
            log.warn( "Haven't dealt with this yet." );
        } else if ( associationName.equals( "ExperimentDesigns" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "ExperimentalDesigns" );
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

        log.debug( "Converting ExperimentDesign" );
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
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "ExperimentalFactors" );
        } else if ( associationName.equals( "NormalizationDescription" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "QualityControlDescription" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "ReplicateDescription" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "TopLevelBioAssays" ) ) {
            assert associatedObject instanceof List;
            // we don't have this in our model --- check
        } else if ( associationName.equals( "Types" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Types" );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * This is a no-op.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertExtendable( Extendable mageObj, edu.columbia.gemma.common.Describable gemmaObj ) {
        if ( mageObj == null || gemmaObj == null ) return;
        ; // nothing to do, we aren't using this.
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
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Contact" );
        else
            log.debug( "Unsupported or unknown association: " + associationName );

    }

    /**
     * @param mageObj
     * @return
     */
    public FactorValue convertFactorValue( org.biomage.Experiment.FactorValue mageObj ) {
        if ( mageObj == null ) return null;

        FactorValue result = FactorValue.Factory.newInstance();
        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
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
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "Measurement" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "Measurement" );
        } else if ( associationName.equals( "Value" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "Value" );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
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
     * Not supported, a no-op.
     * 
     * @param mageObj
     * @return
     */
    public Object convertHardware( Hardware mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * Copy attributes from a MAGE identifiable to a Gemma identifiscribable.
     * 
     * @param mageObj
     * @return boolean True if the object is alreay in the cache and needs no further processing.
     */
    public boolean convertIdentifiable( org.biomage.Common.Identifiable mageObj,
            edu.columbia.gemma.common.Identifiable gemmaObj ) {

        if ( mageObj == null ) return false;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        if ( isInCache( gemmaObj ) ) {
            log.debug( "Object exists in cache: " + gemmaObj.getIdentifier() );
            gemmaObj = ( edu.columbia.gemma.common.Identifiable ) identifiableCache.get( mageObj );
            return true;
        }

        Identifiable k = fetchExistingIdentifiable( IdentifierCreator.create( gemmaObj ) );
        if ( k != null ) {
            gemmaObj = k;
            log.debug( "Object is already persistent: " + gemmaObj.getIdentifier() );
        } else {
            gemmaObj.setIdentifier( mageObj.getIdentifier() );
        }

        gemmaObj.setName( mageObj.getName() ); // we do this here because Mage names go with Identifiable, not
        // describable.
        identifiableCache.put( gemmaObj.getIdentifier(), gemmaObj );

        convertDescribable( mageObj, gemmaObj );
        return false;

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
     * FIXME this is a problem because we don't want separate labeledextract and biomaterial objects.
     * 
     * @param mageObj
     * @return
     */
    public Object convertLabeledExtract( org.biomage.BioMaterial.LabeledExtract mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting LabeledExtract " + mageObj.getIdentifier() );
        // log.warn( "Conversion operation not yet fully implemented: convertLabeledExtract" );

        BioMaterial result = convertBioMaterial( mageObj );
        // mageObj.getLabels()

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertLabeledExtractAssociations( LabeledExtract mageObj, BioMaterial gemmaObj, Method getter ) {
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "Labels" ) ) {
            // we don't use this?
        } else {
            log.debug( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertMaterialType( OntologyEntry mageObj ) {
        return convertOntologyEntry( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertMeasuredBioAssay( MeasuredBioAssay mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting MeasuredBioAssay " + mageObj.getIdentifier() );
        edu.columbia.gemma.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );
        // FIXME this will result in conversion twice because it is identifiable.
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertMeasuredBioAssayAssociations( MeasuredBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        log.debug( "Not supported fully yet" );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "FeatureExtraction" ) ) {
            ; // we may not use this.
            ;
        } else if ( associationName.equals( "MeasuredBioAssayData" ) ) {
            ; // we may not use this.
            ;
        } else {
            log.debug( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertMeasuredSignal( MeasuredSignal mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertMeasuredSignalAssociations( MeasuredSignal mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
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

        if ( mageObj.getValue() != null ) result.setValue( mageObj.getValue().toString() ); // FIME - is this okay

        result.setType( convertMeasurementType( mageObj.getType() ) );
        // result.setRepresentation(...) // FIXME
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

//    /**
//     * @param mageObj
//     * @return
//     */
//    public edu.columbia.gemma.common.description.Description convertNormalizationDescription( Description mageObj ) {
//        return convertDescription( mageObj );
//    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertOntologyEntry( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;
        log.debug( "Converting ontologyEntry" );
        edu.columbia.gemma.common.description.OntologyEntry result = edu.columbia.gemma.common.description.OntologyEntry.Factory
                .newInstance();
        result.setCategory( mageObj.getCategory() );
        result.setDescription( mageObj.getDescription() );
        result.setValue( mageObj.getValue() );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertOntologyEntryAssociations( OntologyEntry mageObj,
            edu.columbia.gemma.common.description.OntologyEntry gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Associations" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Associations" );
        } else if ( associationName.equals( "OntologyReference" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "ExternalOntologyReference" );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.auditAndSecurity.Organization convertOrganization( Organization mageObj ) {
        if ( mageObj == null ) return null;

        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.auditAndSecurity.Person convertPerson( Person mageObj ) {
        if ( mageObj == null ) return null;

        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public Object convertPhysicalArrayDesign( org.biomage.ArrayDesign.PhysicalArrayDesign mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting PhysicalArrayDesign " + mageObj.getIdentifier() );

        edu.columbia.gemma.expression.arrayDesign.ArrayDesign result = convertArrayDesign( mageObj );

        // convert the surfaceType and zoneGroups? We don't support these, though we could

        convertAssociations( mageObj, result );
        return result;

    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPhysicalArrayDesignAssociations( PhysicalArrayDesign mageObj,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj, Method getter ) {
        convertArrayDesignAssociations( mageObj, gemmaObj, getter );
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "SurfaceType" ) ) {
            ; // simpleFillIn(associatedObject, gemmaObj, getter); // we don't support this, do we?
        } else if ( associationName.equals( "ZoneGroups" ) ) {
            assert associatedObject instanceof List;
            // we don't support this.
        } else if ( associationName.equals( "ReporterGroups" ) || associationName.equals( "FeatureGroups" )
                || associationName.equals( "DesignProviders" ) || associationName.equals( "CompositeGroups" )
                || associationName.equals( "ProtocolApplications" ) ) {
            ; // nothing, superclass.
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertPhysicalBioAssay( PhysicalBioAssay mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting PhysicalBioAssay " + mageObj.getIdentifier() );

        edu.columbia.gemma.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );

        convertAssociations( mageObj, result );
        return result;

    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPhysicalBioAssayAssociations( PhysicalBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "BioAssayCreation" ) ) { // we only use this to get the array designs.
            specialGetArrayDesignForPhysicalBioAssay( mageObj, gemmaObj );
        } else if ( associationName.equals( "BioAssayTreatments" ) ) {
            assert associatedObject instanceof List;
            // if ( ( ( List ) associatedObject ).size() > 0 ) log.warn( "BioAssayTreatments is being missed" );
            // this is not supported in our data model currently.
        } else if ( associationName.equals( "PhysicalBioAssayData" ) ) {
            assert associatedObject instanceof List;
            if ( ( ( List ) associatedObject ).size() > 0 ) log.warn( "PhysicalBioAssayData is being missed" );
        } else {
            log.debug( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }

    }

    /**
     * @param associatedObject
     * @return
     */
    public PolymerType convertPolymerType( OntologyEntry mageObj ) {
        log.debug( "Converting PolymerType " + mageObj.getValue() );

        if ( mageObj.getValue().equalsIgnoreCase( "DNA" ) ) {
            return PolymerType.DNA;
        } else if ( mageObj.getValue().equalsIgnoreCase( "protein" ) ) {
            return PolymerType.PROTEIN;
        } else if ( mageObj.getValue().equalsIgnoreCase( "RNA" ) ) {
            return PolymerType.RNA;
        } else {
            log.error( "Unsupported polymer type:" + mageObj.getValue() );
        }
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertPresentAbsent( PresentAbsent mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPresentAbsentAssociations( org.biomage.QuantitationType.PresentAbsent mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

//    /**
//     * @param mageObj
//     * @return
//     */
//    public edu.columbia.gemma.common.protocol.Protocol convertProtocol( Protocol mageObj ) {
//        if ( mageObj == null ) return null;
//        // log.warn( "Not fully supported: Protocol" );
//        edu.columbia.gemma.common.protocol.Protocol result = edu.columbia.gemma.common.protocol.Protocol.Factory
//                .newInstance();
//
//        if ( !convertIdentifiable( mageObj, result ) ) {
//            result.setText( mageObj.getText() );
//            result.setTitle( mageObj.getTitle() );
//            // result.setURI(mageObj.getURI()); // we should support
//            convertAssociations( mageObj, result );
//        }
//
//        return result;
//    }

//    /**
//     * @param mageObj
//     * @param gemmaObj
//     * @param getter
//     */
//    public void convertProtocolAssociations( Protocol mageObj, edu.columbia.gemma.common.protocol.Protocol gemmaObj,
//            Method getter ) {
//        Object associatedObject = intializeConversion( mageObj, getter );
//        String associationName = getterToPropertyName( getter );
//        if ( associatedObject == null ) return;
//
//        if ( associationName.equals( "Hardwares" ) ) {
//            ; // not supported
//        } else if ( associationName.equals( "Softwares" ) ) {
//            // simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Softwares" ); Bug 78
//        } else if ( associationName.equals( "Type" ) ) {
//            simpleFillIn( associatedObject, gemmaObj, getter );
//        } else if ( associationName.equals( "ParameterTypes" ) ) {
//            // broken. Bug 77
//        } else {
//            log.debug( "Unsupported or unknown association: " + associationName );
//        }
//    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertPValue( PValue mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPValueAssociations( PValue mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

//    /**
//     * @param mageObj
//     * @return
//     */
//    public edu.columbia.gemma.common.description.Description convertQualityControlDescription( Description mageObj ) {
//        return convertDescription( mageObj );
//    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertQuantitationType( QuantitationType mageObj ) {

        edu.columbia.gemma.common.quantitationtype.QuantitationType result = edu.columbia.gemma.common.quantitationtype.QuantitationType.Factory
                .newInstance();
        if ( !convertIdentifiable( mageObj, result ) ) {
            result.setIsBackground( mageObj.getIsBackground().booleanValue() );
            convertAssociations( mageObj, result );
        }
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertQuantitationTypeAssociations( QuantitationType mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        // FIXME - we need to fill in the 'general type' and the 'type'.
        if ( associationName.equals( "Channel" ) ) { // we aren't support this?
            ; // simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Associations" ); // we don't
            // support
        } else if ( associationName.equals( "ConfidenceIndicators" ) ) { // just another quantitation type.
            // this is bidirectionally navigable in MAGE.
            ; // simpleFillIn( associatedObject, gemmaObj, getter, "ExternalOntologyReference" ); // we don't support
        } else if ( associationName.equals( "DataType" ) ) {
            gemmaObj.setRepresentation( convertDataType( mageObj.getDataType() ) );
        } else if ( associationName.equals( "Scale" ) ) {
            gemmaObj.setScale( convertScale( mageObj.getScale() ) );
        } else if ( associationName.equals( "QuantitationTypeMaps" ) ) {
            ; // special case - transformations.
        } else if ( associationName.equals( "TargetQuantitationType" ) ) { // from ConfidenceIndicator.
            // this is an association to another QuantitationType: the confidence in it. I think we skip for now.
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertRatio( Ratio mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertRatioAssociations( Ratio mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

//    /**
//     * @param mageObj
//     * @return
//     */
//    public edu.columbia.gemma.common.description.Description convertReplicateDescription( Description mageObj ) {
//        return convertDescription( mageObj );
//    }

    /**
     * TODO a reporter has a feature, which has the location information we need for the Gemma reporter.
     * 
     * @param mageObj
     * @return edu.columbia.gemma.expression.designElement.Reporter
     */
    public edu.columbia.gemma.expression.designElement.Reporter convertReporter(
            org.biomage.DesignElement.Reporter mageObj ) {

        if ( mageObj == null ) return null;

        Reporter result = Reporter.Factory.newInstance();
        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
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
            // simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Associations" ); // we don't
            // support
        } else if ( associationName.equals( "FeatureReporterMaps" ) ) {
            // simpleFillIn( associatedObject, gemmaObj, getter, "ExternalOntologyReference" ); // we don't support
        } else if ( associationName.equals( "ImmobilizedCharacteristics" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "ImmobilizedCharacteristic" );
        } else if ( associationName.equals( "WarningType" ) ) {
            specialConvertFeatureReporterMaps( ( List ) associatedObject, gemmaObj );
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
            return ScaleType.OTHER; // FIXME - add unscaled to the model bug 76
        } else {
            log.error( "Unrecognized Scale " + val );
        }
        return null;
    }

//    /**
//     * @param mageObj
//     * @return
//     */
//    public Software convertSoftware( org.biomage.Protocol.Software mageObj ) {
//        if ( mageObj == null ) return null;
//        Software result = Software.Factory.newInstance();
//        if ( !convertIdentifiable( mageObj, result ) ) {
//            convertAssociations( mageObj, result );
//        }
//        return result;
//    }

//    /**
//     * @param mageObj
//     * @param gemmaObj
//     * @param getter
//     */
//    public void convertSoftwareAssociations( org.biomage.Protocol.Software mageObj, Software gemmaObj, Method getter ) {
//        Object associatedObject = intializeConversion( mageObj, getter );
//        String associationName = getterToPropertyName( getter );
//        if ( associatedObject == null ) return;
//        if ( associationName.equals( "Hardware" ) ) {
//            // no-op
//        } else if ( associationName.equals( "SoftwareManufacturers" ) ) {
//            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "SoftwareManufacturers" );
//        } else if ( associationName.equals( "Softwares" ) ) {
//            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "SoftwareComponents" );
//        } else if ( associationName.equals( "Type" ) ) {
//            simpleFillIn( associatedObject, gemmaObj, getter );
//        } else {
//            log.debug( "Unsupported or unknown association: " + associationName );
//        }
//    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertSpecializedQuantitationType(
            SpecializedQuantitationType mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertSpecializedQuantitationTypeAssociations( SpecializedQuantitationType mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * This is a special case for an OntologyEntry that doesn't map to one in Gemma. Unfortunately, a 'species' in Mage
     * doesn't necessarily include the taxon id or anything else that can be used to unequivocally identify the
     * organism. Therefore we require that taxa match ones already in the data store.
     * 
     * @param species
     * @return Taxon
     */
    public Taxon convertSpecies( OntologyEntry species ) {

        log.debug( "Converting Species from  " + species.getValue() );

        return fetchExistingTaxonByCommonName( species.getValue() );
        // FIXME there must be a better way.
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertStandardQuantitationType(
            StandardQuantitationType mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertStandardQuantitationTypeAssociations( StandardQuantitationType mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    public Treatment convertTreatment( org.biomage.BioMaterial.Treatment mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting Treatment: " + mageObj.getIdentifier() );

        Treatment result = Treatment.Factory.newInstance();

        // we don't use version.
        if ( !convertIdentifiable( mageObj, result ) ) {
            convertAssociations( mageObj, result );
        }
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

            // we don't support any of this!
        } else if ( associationName.equals( "Action" ) ) {
            // simpleFillIn(associatedObject, gemmaObj, getter);
        } else if ( associationName.equals( "ActionMeasurement" ) ) {
            //
        } else if ( associationName.equals( "CompoundMeasurments" ) ) {
            //
        } else if ( associationName.equals( "SourceBioMaterialMeasurements" ) ) {
            //
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Special case, OntologyEntry maps to an Enum.
     * 
     * @param mageObj
     * @return SequenceType
     */
    public SequenceType convertType( OntologyEntry mageObj ) {

        if ( mageObj == null ) return null;

        log.debug( "Converting sequence type of \"" + mageObj.getValue() + "\"" );

        String value = mageObj.getValue();
        if ( value.equalsIgnoreCase( "bc" ) ) {
            return SequenceType.BAC;
        } else if ( value.equalsIgnoreCase( "est" ) ) {
            return SequenceType.EST;
        } else if ( value.equalsIgnoreCase( "affyprobe" ) ) {
            return SequenceType.AFFY_PROBE;
        } else if ( value.equalsIgnoreCase( "affytarget" ) ) {
            return SequenceType.AFFY_TARGET;
        } else if ( value.equalsIgnoreCase( "mrna" ) ) {
            return SequenceType.mRNA;
        } else if ( value.equalsIgnoreCase( "refseq" ) ) {
            return SequenceType.REFSEQ;
        } else if ( value.equalsIgnoreCase( "chromosome" ) ) {
            return SequenceType.WHOLE_CHROMOSOME;
        } else if ( value.equalsIgnoreCase( "genome" ) ) {
            return SequenceType.WHOLE_GENOME;
        } else if ( value.equalsIgnoreCase( "orf" ) ) {
            return SequenceType.ORF;
        } else if ( value.equalsIgnoreCase( "dna" ) ) {
            return SequenceType.DNA;
        } else {
            return SequenceType.OTHER;
        }
    }

    /**
     * @param unit
     * @return
     */
    public edu.columbia.gemma.common.measurement.Unit convertUnit( Unit unit ) {
        if ( unit == null ) return null;

        edu.columbia.gemma.common.measurement.Unit result = edu.columbia.gemma.common.measurement.Unit.Factory
                .newInstance();
        result.setUnitNameCV( unit.getUnitName() );
        return result;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertValue( OntologyEntry mageObj ) {
        return convertOntologyEntry( mageObj );
    }

    /**
     * 
     *
     */
    public void setIdentifiableDao( IdentifiableDao persistenceManager ) {
        this.identifiableDao = persistenceManager;
    }

    /**
     * Special case: Convert a ReporterCompositeMaps (list) to a Collection of Reporters.
     * 
     * @param reporterCompositeMaps
     * @return Collection of Gemma Reporters.
     */
    public void specialConvertFeatureReporterMaps( List featureReporterMaps, Reporter rep ) {

        if ( featureReporterMaps.size() > 1 ) log.warn( "**** More than one FeatureReporterMap for a Reporter!" );

        for ( Iterator iter = featureReporterMaps.iterator(); iter.hasNext(); ) {
            FeatureReporterMap rcp = ( FeatureReporterMap ) iter.next();
            List rcpps = rcp.getFeatureInformationSources();
            for ( Iterator iterator = rcpps.iterator(); iterator.hasNext(); ) {
                log.debug( "Found feature information for reporter: " + rep.getIdentifier() );
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
     * Special case: Convert a ReporterCompositeMaps (list) to a Collection of Reporters.
     * 
     * @param reporterCompositeMaps
     * @return Collection of Gemma Reporters.
     */
    public Collection specialConvertReporterCompositeMaps( List reporterCompositeMaps ) {

        if ( reporterCompositeMaps.size() > 1 ) log.warn( "**** More than one ReporterCompositeMaps for a Reporter!" );

        Collection result = new ArrayList();
        for ( Iterator iter = reporterCompositeMaps.iterator(); iter.hasNext(); ) {
            ReporterCompositeMap rcp = ( ReporterCompositeMap ) iter.next();
            List rcpps = rcp.getReporterPositionSources();
            log.debug( "Found reporters for composite sequence" );
            for ( Iterator iterator = rcpps.iterator(); iterator.hasNext(); ) {
                ReporterPosition rps = ( ReporterPosition ) iterator.next();

                if ( rps == null ) continue;

                org.biomage.DesignElement.Reporter repr = rps.getReporter();

                if ( repr == null ) continue;

                Reporter conv = convertReporter( repr );

                if ( conv == null ) {
                    log.error( "Null converted reporter!" );
                    continue;
                }

                result.add( conv );

                Integer m = rps.getStart();

                if ( m == null ) {
                    // log.error( "Null start for reporter " + repr.getIdentifier() );
                    continue;
                }

                conv.setStartInBioChar( m.intValue() );

            }
            break; // only take the first one;
        }
        return result;
    }

    /**
     * @param mageClass
     * @param mageObj
     * @param gemmaObj
     */
    private void convertAssociations( Class mageClass, Object mageObj, Object gemmaObj ) {

        if ( mageObj == null || gemmaObj == null ) return;

        Class classToSeek = ReflectionUtil.getBaseForImpl( gemmaObj );
        String gemmaObjName = ReflectionUtil.classToTypeName( classToSeek );

        try {
            Class[] interfaces = mageClass.getInterfaces();

            if ( interfaces.length == 0 ) return;

            for ( int i = 0; i < interfaces.length; i++ ) {
                Class infc = interfaces[i];
                String infcName = ReflectionUtil.classToTypeName( infc );

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
                        log.warn( "Converstion of associations -- Operation not yet supported: " + "convert"
                                + ReflectionUtil.objectToTypeName( mageObj ) + "Associations("
                                + mageObj.getClass().getName() + ", " + gemmaObjName + ", "
                                + getter.getClass().getName() + ")" );
                    }
                }

            }

            Class superclazz = mageClass.getSuperclass();
            if ( superclazz == null ) return;

            String superclassName = superclazz.getName();
            if ( superclassName.startsWith( "org.biomage" ) && !superclassName.endsWith( "Extendable" )
                    && !superclassName.endsWith( "Describable" ) && !superclassName.endsWith( "Identifiable" ) )
                convertAssociations( superclazz, mageObj, gemmaObj );

        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( "InvocationTargetException For: " + gemmaObjName, e );
        }
    }

    /**
     * @param gemmaObj
     * @return
     */
    private Identifiable fetchExistingIdentifiable( String identifier ) {
        if ( identifiableDao != null ) return this.identifiableDao.findByIdentifier( identifier );
        return null;
    }

    /**
     * @param result
     * @return
     */
    private Taxon fetchExistingTaxonByCommonName( String commonName ) {
        if ( taxonDao != null ) return taxonDao.findByCommonName( commonName );
        return null;
    }

    /**
     * @param mageObj
     * @return Converted object. If the source object is null, the return value is null.
     */
    private Object findAndInvokeConverter( Object mageObj ) {

        if ( mageObj == null ) return null;
        Object convertedGemmaObj = null;
        Method gemmaConverter = null;
        try {
            gemmaConverter = findConverter( mageObj );
            if ( gemmaConverter == null ) return null;
            convertedGemmaObj = gemmaConverter.invoke( this, new Object[] { mageObj } );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( "InvocationTargetException caused by " + e.getCause() + " when invoking  "
                    + gemmaConverter.getName() + " on a " + mageObj.getClass().getName(), e );
            throw new RuntimeException( e );
        }
        return convertedGemmaObj;
    }

    /**
     * @param objectToConvert
     * @param converterBaseName
     * @param mageTypeToConvert
     * @return Converted object. If the input mageObj is null, the return value is null.
     */
    private Object findAndInvokeConverter( Object mageObj, String converterBaseName, Class mageTypeToConvert ) {

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
            log.error( "InvocationTargetException caused by " + e.getCause() + " when invoking  "
                    + gemmaConverter.getName() + " on a " + mageObj.getClass().getName(), e );
            throw new RuntimeException( e );
        }
        return convertedGemmaObj;
    }

    /**
     * @param setterObj - The object on which we will call the setter
     * @param settee - The object we want to set
     * @param setteeClass - The class accepted by the setter - not necessarily the class of the setterObj (example:
     *        DatabaseEntry vs. DatabaseEntryImpl)
     * @param propertyName - The property name corresponding to the setteeClass
     */
    private void findAndInvokeSetter( Object setterObj, Object settee, Class setteeClass, String propertyName ) {
        Method gemmaSetter = findSetter( setterObj, propertyName, setteeClass );
        if ( gemmaSetter == null ) {
            return;
        }
        try {
            gemmaSetter.invoke( setterObj, new Object[] { settee } );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( e );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    private Method findConverter( Object mageObj ) {
        String mageTypeName = ReflectionUtil.objectToTypeName( mageObj );
        Method converter = null;
        try {
            converter = this.getClass().getMethod( "convert" + mageTypeName, new Class[] { mageObj.getClass() } );
        } catch ( NoSuchMethodException e ) {
            log.warn( "Conversion operation not yet supported: " + "convert" + mageTypeName + "("
                    + mageObj.getClass().getName() + ")" );
        }
        return converter;
    }

    /**
     * @param associationName
     * @param mageAssociatedType
     * @return
     */
    private Method findConverter( String associationName, Class mageAssociatedType ) {
        Method gemmaConverter = null;
        try {
            gemmaConverter = this.getClass()
                    .getMethod( "convert" + associationName, new Class[] { mageAssociatedType } );
        } catch ( NoSuchMethodException e ) {
            log.warn( "Conversion operation not yet supported: " + "convert" + associationName + "("
                    + mageAssociatedType.getName() + ")" );
        }
        return gemmaConverter;
    }

    /**
     * @param setter - The object on which we want to call the setter
     * @param propertyName - The name of the property we want to set
     * @param setee - The object which we want to enter as an argument to the setter.
     * @return
     */
    private Method findSetter( Object setter, String propertyName, Class setee ) {
        Method gemmaSetter = null;
        try {
            gemmaSetter = ReflectionUtil.getBaseForImpl( setter ).getMethod( "set" + propertyName,
                    new Class[] { setee } );
        } catch ( SecurityException e ) {
            log.error( e );
        } catch ( NoSuchMethodException e ) {
            log.error( "No such setter: " + "set" + propertyName + "(" + setee.getName() + ")", e );
        }
        return gemmaSetter;
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
     * Initialize the conversion process by calling the getter and getting the association name
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @return The name of the association, taken from the getter.
     */
    private Object intializeConversion( Object mageObj, Method getter ) {
        Object associatedObject = invokeGetter( mageObj, getter );

        if ( associatedObject == null ) {
            log.debug( "Getter called on " + mageObj.getClass().getName() + " but failed to return a value: "
                    + getter.getName() + " (Probably no data)" );
            return null;
        }

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

        if ( mageObj == null ) throw new IllegalArgumentException( "Attempt to run get... on null object" );

        try {
            // log.debug( "Calling " + getter.getName() + " from " + getter.getDeclaringClass().getName() + " on a "
            // + mageObj.getClass().getName() );
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

    /**
     * @param gemmaObj - Identifiable
     * @return boolean True if the object is alreay in the cache and should not be created anew.
     */
    private boolean isInCache( edu.columbia.gemma.common.Identifiable gemmaObj ) {
        return identifiableCache.containsKey( gemmaObj.getIdentifier() );
    }

    /**
     * Generic method to fill in a Gemma object where the association in Mage has cardinality of >1.
     * 
     * @param associatedList - The result of the Getter call.
     * @param gemmObj - The Gemma object in which to place the converted Mage object(s).
     * @param onlyTakeOne - This indicates that the cardinality in the Gemma object is at most 1. Therefore we pare down
     *        the Mage object list to take just the first one.
     * @param actualGemmaAssociationName - for example, a BioSequence hasa "SequenceDatabaseEntry", not a
     *        "DatabaseEntry". If null, the name is inferred.
     */
    private void simpleFillIn( List associatedList, Object gemmaObj, Method getter, boolean onlyTakeOne,
            String actualGemmaAssociationName ) {

        if ( associatedList == null || gemmaObj == null || getter == null )
            throw new IllegalArgumentException( "Null objects" );

        if ( associatedList.size() == 0 ) {
            log.debug( "List was not null, but empty" );
            return;
        }

        String associationName = actualGemmaAssociationName; // FIXME, refactor so we use convertAssociationName

        if ( associationName == null )
            associationName = ReflectionUtil.classToTypeName( associatedList.get( 0 ).getClass() );

        try {
            if ( onlyTakeOne ) {

                Object mageObj = associatedList.get( 0 );
                Object convertedGemmaObj = findAndInvokeConverter( mageObj );
                if ( convertedGemmaObj == null ) return; // not supported.
                Class convertedGemmaClass = ReflectionUtil.getBaseForImpl( convertedGemmaObj );
                log.debug( "Converting a MAGE list to a single instance of "
                        + ReflectionUtil.classToTypeName( convertedGemmaClass ) );
                findAndInvokeSetter( gemmaObj, convertedGemmaObj, convertedGemmaClass, associationName );
            } else {

                Collection gemmaObjList = new ArrayList();
                for ( Iterator iter = associatedList.iterator(); iter.hasNext(); ) {
                    Object mageObj = iter.next();
                    Object convertedGemmaObj = findAndInvokeConverter( mageObj );
                    if ( convertedGemmaObj == null ) continue; // not supported.
                    log.debug( "Converting a MAGE list to a Gemma list" );
                    gemmaObjList.add( convertedGemmaObj );
                }

                findAndInvokeSetter( gemmaObj, gemmaObjList, Collection.class, associationName );
            }

        } catch ( SecurityException e ) {
            log.error( e );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
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

        if ( associatedMageObject == null ) return;
        String associationName = getterToPropertyName( getter );

        try {
            Class mageAssociatedType = associatedMageObject.getClass();
            Object gemmaAssociatedObj = findAndInvokeConverter( associatedMageObject, associationName,
                    mageAssociatedType );
            if ( gemmaAssociatedObj == null ) return;

            Class gemmaClass = ReflectionUtil.getImplForBase( gemmaAssociatedObj.getClass() );
            String inferredGemmaAssociationName = convertAssociationName( actualGemmaAssociationName,
                    gemmaAssociatedObj );
            log.debug( "Filling in " + inferredGemmaAssociationName );
            findAndInvokeSetter( gemmaObj, gemmaAssociatedObj, gemmaClass, inferredGemmaAssociationName );

        } catch ( SecurityException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        }

    }

    /**
     * Extract compositeSequence information from the ArrayDesign package. The ArrayDesign package doesn't have any
     * information about the compositeSequences, other than the fact that they belong to this arrayDesign.
     * 
     * @param compositeGroups
     * @param gemmaObj
     */
    private void specialConvertCompositeGroups( List compositeGroups,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj ) {
        log.info( "Converting compositeGroups" );

        Collection designObjs;
        if ( gemmaObj.getDesignElements() == null ) {
            designObjs = new HashSet();
            gemmaObj.setDesignElements( designObjs );
        } else {
            designObjs = gemmaObj.getDesignElements();
        }

        for ( Iterator iter = compositeGroups.iterator(); iter.hasNext(); ) {
            CompositeGroup rg = ( CompositeGroup ) iter.next();
            List reps = rg.getCompositeSequences();
            for ( Iterator iterator = reps.iterator(); iterator.hasNext(); ) {
                org.biomage.DesignElement.CompositeSequence compseq = ( org.biomage.DesignElement.CompositeSequence ) iterator
                        .next();

                CompositeSequence csconv = convertCompositeSequence( compseq );

                if ( !designObjs.contains( csconv ) ) {
                    log.debug( "Adding new compositeSequence to the ArrayDesign" );
                    designObjs.add( csconv );
                } else {
                    log.debug( "We already have this compositesequence " + csconv.getIdentifier() );
                }
            }
        }
        log.info( designObjs.size() + " Composite sequences in the array design" );
        gemmaObj.setNumberOfCompositeSequences( designObjs.size() );
    }

    /**
     * We don't have features, just reporter, so FIXME
     * 
     * @param featureGroups
     * @param gemmaObj
     */
    private void specialConvertFeatureGroups( List featureGroups,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj ) {
        log.warn( "Converting featureGroups (but not doing anything)" );
        //
        // Collection designObjs;
        // if ( gemmaObj.getDesignElements() == null ) {
        // designObjs = new HashSet();
        // gemmaObj.setDesignElements( designObjs );
        // } else {
        // designObjs = gemmaObj.getDesignElements();
        // }
        //
        // for ( Iterator iter = featureGroups.iterator(); iter.hasNext(); ) {
        // FeatureGroup rg = ( FeatureGroup ) iter.next();
        // List reps = rg.getFeatures();
        // for ( Iterator iterator = reps.iterator(); iterator.hasNext(); ) {
        // org.biomage.DesignElement.Feature feature = ( org.biomage.DesignElement.Feature ) iterator.next();
        //
        // edu.columbia.gemma.expression.designElement.Feature csconv = convertFeature( feature );
        // if ( !designObjs.contains( csconv ) ) {
        // log.debugn( "Adding new feature to the ArrayDesign" );
        // designObjs.add( csconv );
        // } else {
        // log.debug( "We already have this feature " + csconv.getIdentifier() );
        // }
        // }
        // }
    }

    /**
     * Convert all the reporters via the reporter groups.
     * 
     * @param reporterGroups
     * @param gemmaObj
     */
    private void specialConvertReporterGroups( List reporterGroups,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj ) {

        log.info( "Converting reporterGroups" );

        Collection designObjs;
        if ( gemmaObj.getDesignElements() == null ) {
            designObjs = new HashSet();
            gemmaObj.setDesignElements( designObjs );
        } else {
            designObjs = gemmaObj.getDesignElements();
        }

        for ( Iterator iter = reporterGroups.iterator(); iter.hasNext(); ) {
            ReporterGroup rg = ( ReporterGroup ) iter.next();
            List reps = rg.getReporters();
            for ( Iterator iterator = reps.iterator(); iterator.hasNext(); ) {
                org.biomage.DesignElement.Reporter reporter = ( org.biomage.DesignElement.Reporter ) iterator.next();
                Reporter csconv = convertReporter( reporter );
                if ( !designObjs.contains( csconv ) ) {
                    log.debug( "Adding new reporter to the ArrayDesign" );
                    designObjs.add( csconv );
                } else {
                    log.debug( "We already have this reporter " + csconv.getIdentifier() );
                }
            }
        }
        log.info( designObjs.size() + " Features (reporters, actually) in the array design" );
        gemmaObj.setNumberOfFeatures( designObjs.size() );
    }

    /**
     * From a PhysicalBioAssay, find the associated ArrayDesign.
     * 
     * @param mageObj
     * @param result
     */
    private void specialGetArrayDesignForPhysicalBioAssay( PhysicalBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay result ) {

        BioAssayCreation bac = mageObj.getBioAssayCreation();
        if ( bac == null ) return;

        ArrayDesign ad = bac.getArray().getArrayDesign();
        if ( ad == null ) return;

        log.info( "Looked in " + mageObj.getIdentifier() + " for ArrayDesign " + ad.getIdentifier() );

        edu.columbia.gemma.expression.arrayDesign.ArrayDesign conv = convertArrayDesign( ad );
        if ( result.getArrayDesignsUsed() == null ) result.setArrayDesignsUsed( new ArrayList() );
        result.getArrayDesignsUsed().add( conv );
    }

}
