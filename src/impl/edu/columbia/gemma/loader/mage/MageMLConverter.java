package edu.columbia.gemma.loader.mage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.biomage.BQS.BibliographicReference;
import org.biomage.BioAssay.BioAssay;
import org.biomage.BioAssay.DerivedBioAssay;
import org.biomage.BioAssay.MeasuredBioAssay;
import org.biomage.BioAssay.PhysicalBioAssay;
import org.biomage.BioMaterial.BioSample;
import org.biomage.BioMaterial.BioSource;
import org.biomage.BioMaterial.LabeledExtract;
import org.biomage.Common.Describable;
import org.biomage.Common.Extendable;
import org.biomage.Common.Identifiable;
import org.biomage.Description.Database;
import org.biomage.Description.Description;
import org.biomage.Description.OntologyEntry;
import org.biomage.DesignElement.FeatureInformation;
import org.biomage.DesignElement.FeatureReporterMap;
import org.biomage.DesignElement.ReporterCompositeMap;
import org.biomage.DesignElement.ReporterPosition;
import org.biomage.Experiment.Experiment;
import org.biomage.Experiment.ExperimentDesign;
import org.biomage.Protocol.Hardware;
import org.biomage.Protocol.Protocol;
import org.biomage.QuantitationType.DerivedSignal;
import org.biomage.QuantitationType.MeasuredSignal;
import org.biomage.QuantitationType.PValue;
import org.biomage.QuantitationType.PresentAbsent;
import org.biomage.QuantitationType.QuantitationType;
import org.biomage.QuantitationType.Ratio;
import org.biomage.QuantitationType.SpecializedQuantitationType;

import edu.columbia.gemma.common.Identifscribable;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.quantitationtype.PrimitiveType;
import edu.columbia.gemma.common.quantitationtype.ScaleType;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.sequence.biosequence.PolymerType;
import edu.columbia.gemma.sequence.biosequence.SequenceType;
import edu.columbia.gemma.sequence.gene.Taxon;
import edu.columbia.gemma.util.ReflectionUtil;

/**
 * Class to convert Mage domain objects to Gemma domain objects. In most cases, the user can simply call the "convert"
 * method on any MAGE domain object and get a fully-populated Gemma domain object. There is no need to use the methods
 * in this class directly when handling MAGE-ML files: use the {@link edu.columbia.gemma.loader.mage.MageMLParser.}
 * <p>
 * FIXME: This is gigantic. The names of attributes etc. should be in an external file.
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
        Object result = findAndInvokeConverter( mageObj );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * TODO this can't clobber an existing array design, and there are a LOT of associations to figure out. TODO - array
     * deisgn has subclass physical array design, which we actually use.
     * 
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.arrayDesign.ArrayDesign convertArrayDesign( ArrayDesign mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "(Incomplete) Converting Array design: " + mageObj.getName() );

        edu.columbia.gemma.expression.arrayDesign.ArrayDesign result = edu.columbia.gemma.expression.arrayDesign.ArrayDesign.Factory
                .newInstance();

        result.setNumberOfFeatures( mageObj.getNumberOfFeatures().intValue() );

        // FIXME number of composite features?

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
        log.warn( "convertArrayDesignAssociations Not fully supported yet" );
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "CompositeGroups" ) ) {
            assert associatedObject instanceof List;
            ;
        } else if ( associationName.equals( "DesignProviders" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "FeatureGroups" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "ProtocolApplications" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "ReporterGroups" ) ) {
            assert associatedObject instanceof List;
        } else {
            log.debug( "Unsupported or unknown association, or it belongs to the subclass: " + associationName );
        }
    }

    /**
     * We don't support at all, so this is a no-op.
     */
    public Object convertArrayManufacture( ArrayManufacture mageObj ) {
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

        if ( mageObj == null || gemmaObj == null ) return;

        Class classToSeek = ReflectionUtil.getBaseForImpl( gemmaObj );
        String gemmaObjName = ReflectionUtil.classToTypeName( classToSeek );

        try {
            Class[] interfaces = mageObj.getClass().getInterfaces();
            for ( int i = 0; i < interfaces.length; i++ ) {
                Class infc = interfaces[i];
                String infcName = ReflectionUtil.classToTypeName( infc );

                if ( !infcName.startsWith( "Has" ) ) continue;

                String propertyName = infcName.substring( 3 );

                Method getter = mageObj.getClass().getMethod( "get" + propertyName, new Class[] {} );

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

    // public Description convertReplicateDescription()

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
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertBioAssay( BioAssay mageObj ) {
        if ( mageObj == null ) return null;
        log.debug( "Converting BioAssay " + mageObj.getIdentifier() );

        edu.columbia.gemma.expression.bioAssay.BioAssay result = edu.columbia.gemma.expression.bioAssay.BioAssay.Factory
                .newInstance();
        if ( !convertIdentifiable( mageObj, result ) ) convertAssociations( mageObj, result );
        return result;
    }

    /**
     * FIXME - most of these associations aren't supported by us.
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioAssayAssociations( BioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {

        log.warn( "convertBioAssayAssociations not fully supported!" );
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "BioAssayFactorValues" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "BioAssayFactorValues" );
        else if ( associationName.equals( "Channels" ) )
            ; // we don't support this.
        else
            log.debug( "Unsupported or unknown association: " + associationName );
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
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Characteristics" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, null );
        } else if ( associationName.equals( "MaterialType" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "QualityControlStatistics" ) ) {
            assert associatedObject instanceof List;
            // we don't support : TODO check
        } else if ( associationName.equals( "Treatments" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, null ); // FIXME - name of
            // association is wrong
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

        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );

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
    public edu.columbia.gemma.sequence.biosequence.BioSequence convertBioSequence(
            org.biomage.BioSequence.BioSequence mageObj ) {

        log.debug( "Converting BioSequence " + mageObj.getIdentifier() );

        if ( mageObj == null ) return null;

        edu.columbia.gemma.sequence.biosequence.BioSequence result = edu.columbia.gemma.sequence.biosequence.BioSequence.Factory
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
            edu.columbia.gemma.sequence.biosequence.BioSequence gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );

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

        // FIXME: sourceContact. - we don't have this.

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioSourceAssociations( BioSource mageObj, BioMaterial gemmaObj, Method getter ) {
        log.warn( "convertBioMaterialAssociations (biosource) Not fully supported yet" );
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );
        // FIXME source contact.
    }

    /**
     * FIXME - what do we do with this?
     * 
     * @param mageObj
     * @return
     */
    public Object convertCompositeGroup( CompositeGroup mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting CompositeGroup " + mageObj.getIdentifier() );
        log.warn( "Conversion operation not yet implemented: convertCompositeGroup" );

        // edu.columbia.gemma.expression.biomaterial. result = Reporter.Factory.newInstance();
        // convertIdentifiable( mageObj, result );
        // convertAssociations( mageObj, result );
        // return result;
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
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
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
     * FIXME - what do we do with this? Used for Label of LabeledExtract, and details of treatments and biomaterial
     * measurements.
     * 
     * @param mageObj
     * @return
     */
    public Object convertCompound( org.biomage.BioMaterial.Compound mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting Compound " + mageObj.getIdentifier() );
        log.warn( "Conversion operation not yet implemented: convertCompound" );

        // edu.columbia.gemma.expression.biomaterial. result = Reporter.Factory.newInstance();
        // if (!convertIdentifiable( mageObj, result )) convertAssociations( mageObj, result );
        // return result;
        return null;
    }

    /**
     * Todo: this should be set up so we don't get duplicate databases persisted.
     * 
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

        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
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
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
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
    public Object convertDerivedBioAssay( DerivedBioAssay mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting DerivedBioAssay " + mageObj.getIdentifier() );
        log.warn( "Conversion operation not yet implemented: convertDerivedBioAssay" );

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
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        log.warn( "convertDerivedBioAssayAssociations: Not supported fully" );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "DerivedBioAssayMap" ) ) {
            ;
            ;
        } else if ( associationName.equals( "DerivedBioAssayData" ) ) {
            ;
            ;
        } else if ( associationName.equals( "Type" ) ) {
            // simpleFillIn(associatedObject, gemmaObj, getter);
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
     * Convert a MAGE Describable to a Gemma domain object. We only allow a single description, so we take the first
     * one. The association to Security and Audit are not filled in here - TODO something about that.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertDescribable( Describable mageObj, Identifscribable gemmaObj ) {

        if ( mageObj == null ) return;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        if ( mageObj.getDescriptions().size() > 0 ) {
            gemmaObj.setDescription( convertDescription( ( Description ) mageObj.getDescriptions().get( 0 ) ) );
        }

        if ( mageObj.getDescriptions().size() > 1 )
            log.warn( "***** There were multiple descriptions from a MAGE.Describable! ***** " );

        convertExtendable( mageObj, gemmaObj );
    }

    /**
     * todo: mage Description isa Describable.
     * 
     * @param mageObj
     * @return edu.columbia.gemma.common.description.Description
     */
    public edu.columbia.gemma.common.description.Description convertDescription(
            org.biomage.Description.Description mageObj ) {

        if ( mageObj == null ) return null;

        log.debug( "Converting Description: " + mageObj.getText() );

        edu.columbia.gemma.common.description.Description result = edu.columbia.gemma.common.description.Description.Factory
                .newInstance();
        result.setText( mageObj.getText() );
        result.setURI( mageObj.getURI() );
        convertAssociations( mageObj, result );

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDescriptionAssociations( Description mageObj,
            edu.columbia.gemma.common.description.Description gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "BibliographicReferences" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "BibliographicReference" );
        } else if ( associationName.equals( "DatabaseReferences" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "DatabaseEntries" );
        } else if ( associationName.equals( "Annotations" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Annotations" );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertError(
            org.biomage.QuantitationType.Error mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * TODO Incomplete
     * 
     * @param mageObj
     * @return
     */
    public ExpressionExperiment convertExperiment( Experiment mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Incomplete: Converting Experiment: " + mageObj.getName() );

        ExpressionExperiment result = ExpressionExperiment.Factory.newInstance();

        result.setSource( "Imported from MAGE-ML" );

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

        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Category" ) )
            simpleFillIn( associatedObject, gemmaObj, getter );
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
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "AnalysisResults" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, null );
        } else if ( associationName.equals( "BioAssays" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "BioAssays" );
        } else if ( associationName.equals( "Providers" ) ) {
            assert associatedObject instanceof List; // FIXME currently broken association.
            // simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Provider" );
        } else if ( associationName.equals( "BioAssayData" ) ) {
            assert associatedObject instanceof List;
            // simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "BioAssayData" );
            // FIXME this is a potential problem, need to deal with specially.
            log.debug( "Haven't dealt with this yet." );
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

        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
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
            // we don't have this in our model TODO: check
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
    public void convertExtendable( Extendable mageObj, Identifscribable gemmaObj ) {
        ; // nothing to do, we aren't using this.
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertExternalDatabaseAssociations( Database mageObj, ExternalDatabase gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "Contacts" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Contact" );
        else
            log.debug( "Unsupported or unknown association: " + associationName );

    }

    /**
     * FIXME Unlike in MAGE, feature-reporter map is not an entity. (The mage name is also confusing: it is an
     * assocation between a reporter and the features that make it up). Therefore, this is a no-op.
     * 
     * @param mageObj
     * @return
     */
    public Object convertFeatureReporterMap( FeatureReporterMap mageObj ) {
        // NO-OP
        return null;
    }

    /**
     * Not supported, a no-op.
     * 
     * @param mageObj
     * @return
     */
    public Object convertHardware( Hardware mageObj ) {
        // no-op
        return null;
    }

    /**
     * Copy attributes from a MAGE identifiable to a Gemma identifiscribable.
     * 
     * @param mageObj
     * @return boolean True if the object is alreay in the cache and needs no further processing.
     */
    public boolean convertIdentifiable( Identifiable mageObj, Identifscribable gemmaObj ) {

        if ( mageObj == null ) return false;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        if ( isInCache( mageObj ) ) {
            log.debug( "Object exists in cache: " + mageObj.getIdentifier() );
            gemmaObj = ( Identifscribable ) identifiableCache.get( mageObj.getIdentifier() );
            return true;
        }

        identifiableCache.put( mageObj.getIdentifier(), gemmaObj );
        gemmaObj.setIdentifier( mageObj.getIdentifier() );
        gemmaObj.setName( mageObj.getName() );
        convertDescribable( mageObj, gemmaObj );
        return false;

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
        // TODO labels?

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertLabeledExtractAssociations( LabeledExtract mageObj, BioMaterial gemmaObj, Method getter ) {
        log.warn( "convertBioMaterialAssociations (labeled extract) Not fully supported yet" );
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public Object convertMeasuredBioAssay( MeasuredBioAssay mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting MeasuredBioAssay " + mageObj.getIdentifier() );
        log.warn( "Conversion operation not yet implemented: convertMeasuredBioAssay" );

        edu.columbia.gemma.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );
        // FIXME this will result in conversion twice because it is identifiable.
        convertAssociations( mageObj, result ); // FIXME this has to only do the
        // subclass part.
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertMeasuredBioAssayAssociations( MeasuredBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        log.warn( "convertMeasuredBioAssayAssociations: Not supported fully" );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "FeatureExtraction" ) ) {
            ;
            ;
        } else if ( associationName.equals( "MeasuredBioAssayData" ) ) {
            ;
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
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
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
    public Object convertPhysicalArrayDesign( org.biomage.ArrayDesign.PhysicalArrayDesign mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting PhysicalArrayDesign " + mageObj.getIdentifier() );

        edu.columbia.gemma.expression.arrayDesign.ArrayDesign result = convertArrayDesign( mageObj );

        // todo: convert the surfaceType and zoneGroups. We don't support these, though we could

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
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "SurfaceType" ) ) {
            ; // simpleFillIn(associatedObject, gemmaObj, getter); // we don't support this, do we?
        } else if ( associationName.equals( "ZoneGroups" ) ) {
            assert associatedObject instanceof List;
            // we don't support this.
        } else {
            log.warn( "Unsupported or unknown association, or it belongs to a subclass: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertPhysicalBioAssay( PhysicalBioAssay mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting PhysicalBioAssay " + mageObj.getIdentifier() );
        log.warn( "Conversion operation not yet fully implemented: convertPhysicalBioAssay" );

        edu.columbia.gemma.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );
        convertAssociations( mageObj, result ); // FIXME this has to only do the subclass part.
        return result;

    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPhysicalBioAssayAssociations( PhysicalBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        log.warn( "convertPhysicalBioAssayAssociations: Not supported fully" );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "BioAssayCreation" ) ) {
            ;
            ;
        } else if ( associationName.equals( "BioAssayTreatments" ) ) {
            ;
            ;
        } else if ( associationName.equals( "PhysicalBioAssayData" ) ) {
            ;
            ;
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
     * @return
     */
    public edu.columbia.gemma.common.protocol.Protocol convertProtocol( Protocol mageObj ) {
        if ( mageObj == null ) return null;
        log.warn( "Not fully supported: Protocol" );
        edu.columbia.gemma.common.protocol.Protocol result = edu.columbia.gemma.common.protocol.Protocol.Factory
                .newInstance();

        if ( !convertIdentifiable( mageObj, result ) ) {
            result.setText( mageObj.getText() );
            result.setTitle( mageObj.getTitle() );
            // result.setURI(mageObj.getURI()); // FIXME we should support
            convertAssociations( mageObj, result );
        }

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertProtocolAssociations( Protocol mageObj, edu.columbia.gemma.common.protocol.Protocol gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Hardwares" ) ) {
            ; // not supported
        } else if ( associationName.equals( "Softwares" ) ) {
            // FIXME - not navigable.
        } else if ( associationName.equals( "Type" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "ParameterTypes" ) ) {
            // FIXME broken.
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertPValue( PValue mageObj ) {
        return convertQuantitationType( mageObj );
    }

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
        log.warn( "Not fully implemented" );
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Channel" ) ) { // we aren't support this?
            ; // simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Associations" ); // we don't
            // support
        } else if ( associationName.equals( "ConfidenceIndicators" ) ) { // just another quantitation type.
            ; // simpleFillIn( associatedObject, gemmaObj, getter, "ExternalOntologyReference" ); // we don't support
        } else if ( associationName.equals( "DataType" ) ) {
            gemmaObj.setRepresentation( convertDataType( mageObj.getDataType() ) );
        } else if ( associationName.equals( "Scale" ) ) {
            gemmaObj.setScale( convertScale( mageObj.getScale() ) );
        } else if ( associationName.equals( "QuantitationTypeMaps" ) ) {
            ; // special case - transformations.
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public ScaleType convertScale( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;

        String val = mageObj.getValue();
        if ( val.equals( "" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equals( "" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equals( "" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equals( "" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equals( "" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equals( "" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equals( "" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equals( "" ) ) {
            return ScaleType.FOLDCHANGE;
        } else {
            log.error( "Unrecognized Scale " + val );
        }
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public PrimitiveType convertDataType( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;

        String val = mageObj.getValue();

        if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equals( "" ) ) {
            return PrimitiveType.BOOLEAN;
        } else {
            log.error( "Unrecognized DataType " + val );
        }
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertRatio( Ratio mageObj ) {
        return convertQuantitationType( mageObj );
    }

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
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
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
     * FIXME Unlike in MAGE, reporter-composite map is not an entity. (The mage name is also confusing: it is an
     * assocation betwee a composite sequence and the reporters that make it up). Therefore, this is a no-op.
     * 
     * @param mageObj
     * @return
     */
    public Object convertReporterCompositeMap( ReporterCompositeMap mageObj ) {
        // NO-OP
        return null;
    }

    /**
     * FIXME - what do we do with this?
     * 
     * @param mageObj
     * @return
     */
    public Object convertReporterGroup( ReporterGroup mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting ReporterGroup " + mageObj.getIdentifier() );
        log.warn( "Conversion operation not yet implemented: convertReporterGroup" );

        // edu.columbia.gemma.expression.biomaterial. result = Reporter.Factory.newInstance();
        // convertIdentifiable( mageObj, result );
        // convertAssociations( mageObj, result );
        // return result;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertSpecializedQuantitationType(
            SpecializedQuantitationType mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * This is a special case for an OntologyEntry that doesn't map to one in Gemma.
     * 
     * @param species
     * @return
     */
    public Taxon convertSpecies( OntologyEntry species ) {
        Taxon result = Taxon.Factory.newInstance();

        log.debug( "Converting Species from  " + species.getValue() );

        result.setCommonName( species.getValue() );

        // FIXME - this should be written to ensure that the result agrees with what is already in the database.
        return result;
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
                // todo: strand...
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
                org.biomage.DesignElement.Reporter repr = rps.getReporter();
                // FIXME: this reporter already exists in memory, probably.
                Reporter conv = convertReporter( repr );
                conv.setStartInBioChar( rps.getStart().intValue() );
                result.add( conv );
            }
            break; // only take the first one;
        }
        return result;
    }

    /**
     * @param mageObj
     * @return Converted object. If the source object is null, the return value is null.
     */
    private Object findAndInvokeConverter( Object mageObj ) {

        if ( mageObj == null ) return null;
        Object convertedGemmaObj = null;
        try {
            Method converter = findConverter( mageObj );
            if ( converter == null ) return null;
            convertedGemmaObj = converter.invoke( this, new Object[] { mageObj } );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( e );
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
            log.error( e );
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( "InvocationTargetException for " + mageObj.getClass().getName() + e );
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
            log.warn( "Conversion operation not yet supported: " + "convert" + associationName );
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
            log.error( "No such setter: " + "set" + propertyName, e );
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
    private Object intializeConversion( Object mageObj, Object gemmaObj, Method getter ) {
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
     * @param mageObj - Identifiable
     * @return boolean True if the object is alreay in the cache and needs no further processing.
     */
    private boolean isInCache( Identifiable mageObj ) {
        return identifiableCache.get( mageObj.getIdentifier() ) != null;
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

        String associationName = actualGemmaAssociationName; // todo, refactor so we use convertAssociationName

        if ( associationName == null )
            associationName = ReflectionUtil.classToTypeName( associatedList.get( 0 ).getClass() );

        try {
            if ( onlyTakeOne ) {
                log.debug( "Converting a MAGE list to a single instance" );
                Object mageObj = associatedList.get( 0 );
                Object convertedGemmaObj = findAndInvokeConverter( mageObj );
                if ( convertedGemmaObj == null ) return; // not supported.
                Class convertedGemmaClass = ReflectionUtil.getBaseForImpl( convertedGemmaObj );
                findAndInvokeSetter( gemmaObj, convertedGemmaObj, convertedGemmaClass, associationName );
            } else {
                log.debug( "Converting a MAGE list to a Gemma list" );
                Collection gemmaObjList = new ArrayList();
                for ( Iterator iter = associatedList.iterator(); iter.hasNext(); ) {
                    Object mageObj = iter.next();
                    Object convertedGemmaObj = findAndInvokeConverter( mageObj );
                    if ( convertedGemmaObj == null ) continue; // not supported.
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
            findAndInvokeSetter( gemmaObj, gemmaAssociatedObj, gemmaClass, inferredGemmaAssociationName );

        } catch ( SecurityException e ) {
            log.error( e );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        }

    }

}
