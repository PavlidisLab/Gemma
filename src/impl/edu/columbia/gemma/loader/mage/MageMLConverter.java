package edu.columbia.gemma.loader.mage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.ArrayDesign.ArrayDesign;
import org.biomage.BioMaterial.BioSample;
import org.biomage.BioMaterial.BioSource;
import org.biomage.BioMaterial.LabeledExtract;
import org.biomage.Common.Describable;
import org.biomage.Common.Extendable;
import org.biomage.Common.Identifiable;
import org.biomage.Description.Database;
import org.biomage.Description.Description;
import org.biomage.Description.OntologyEntry;
import org.biomage.Experiment.Experiment;

import edu.columbia.gemma.common.Identifscribable;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.sequence.biosequence.PolymerType;
import edu.columbia.gemma.sequence.biosequence.SequenceType;
import edu.columbia.gemma.sequence.gene.Taxon;
import edu.columbia.gemma.util.ReflectionUtil;

/**
 * Class to convert Mage domain objects to Gemma domain objects. In most cases, the user can simply call the "convert"
 * method on any MAGE domain object and get a fully-populated Gemma domain object. There is no need to use the methods
 * in this class directly when handling MAGE-ML files: use the {@link edu.columbia.gemma.loader.mage.MageMLParser.}
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
    private static final boolean CONVERT_ALL = false;

    /**
     * Used to indicate that when a MAGE list is encountered, we should only process the first element of the list.
     */
    private static final boolean CONVERT_FIRST_ONLY = true;
    private static final String START_AUDIT_NOTE = "Imported from MAGE";

    protected static final Log log = LogFactory.getLog( MageMLConverter.class );

    /**
     * A generic converter that figures out which specific conversion method to call based on the class of the object.
     * 
     * @param mageObj
     * @return
     */
    public Object convert( Object mageObj ) {

        Object result = null;

        try {
            String targetObjectName = ReflectionUtil.objectToTypeName( mageObj );
            String callMethodName = "convert" + targetObjectName;
            Method[] methods = this.getClass().getMethods();

            // locate a method that has the right signature. todo This is inefficient..
            Method callMethod = null;
            for ( int i = 0; i < methods.length; i++ ) {
                Class[] params = methods[i].getParameterTypes();
                if ( methods[i].getName().equals( callMethodName ) && params.length == 1
                        && params[0] == mageObj.getClass() ) {
                    callMethod = methods[i];
                    break;
                }
            }

            if ( callMethod == null ) {
                log.warn( "Operation not yet supported: " + callMethodName );
                return null;
            }

            result = callMethod.invoke( this, new Object[] { mageObj } );

            if ( result != null ) convertAssociations( mageObj, result );

        } catch ( IllegalArgumentException e ) {
            log.error( e );
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( "Call resulted in an exception " + e.getCause(), e );
        } catch ( SecurityException e ) {
            log.error( e );
        }
        return result;
    }

    /**
     * TODO this can't clobber an existing array design, and there are a LOT of associations to figure out.
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

        // number of composite features?

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );

        return result;
    }

    /**
     * Generic method to find the associations a Mage object has and call the appropriate converter method.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertAssociations( Object mageObj, Object gemmaObj ) {

        Class classToSeek = ReflectionUtil.getBaseForImpl( gemmaObj );
        String gemmaObjName = ReflectionUtil.classToTypeName( classToSeek );

        try {
            Class[] interfaces = mageObj.getClass().getInterfaces();
            for ( int i = 0; i < interfaces.length; i++ ) {
                Class infc = interfaces[i];
                String infcName = ReflectionUtil.classToTypeName( infc );

                if ( infcName.startsWith( "Has" ) ) {
                    String propertyName = infcName.substring( 3 );

                    Method getter = mageObj.getClass().getMethod( "get" + propertyName, new Class[] {} );

                    if ( getter != null ) {
                        try {
                            Method converter = this.getClass().getMethod( "convert" + gemmaObjName + "Associations",
                                    new Class[] { mageObj.getClass(), classToSeek, getter.getClass() } );

                            if ( converter == null ) throw new NoSuchMethodException();

                            converter.invoke( this, new Object[] { mageObj, gemmaObj, getter } );
                        } catch ( NoSuchMethodException e ) {
                            log.warn( "Converstion of Associations Operation not yet supported: " + "convert"
                                    + gemmaObjName + "Associations" );
                        }
                    }
                }
            }
        } catch ( NoSuchMethodException e ) {
            log.error( e );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( "InvocationTargetException For: " + gemmaObjName, e );
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

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
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
            edu.columbia.gemma.sequence.biosequence.BioSequence gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associationName.equals( "PolymerType" ) ) { // Ontology Entry - enumerated type.
            simpleFillIn( mageObj, gemmaObj, getter );
        } else if ( associationName.equals( "SequenceDatabases" ) ) { // list of DatabaseEntries, we use one
            List seqdbs = ( List ) associatedObject;
            simpleFillIn( seqdbs, gemmaObj, getter, true, "SequenceDatabaseEntry" );
        } else if ( associationName.equals( "Type" ) ) { // ontology entry, we map to a enumerated type.
            simpleFillIn( mageObj, gemmaObj, getter, "Type" ); // yes, we do.
        } else if ( associationName.equals( "Species" ) ) { // ontology entry, we map to a enumerated type.
            simpleFillIn( mageObj, gemmaObj, getter );
        } else if ( associationName.equals( "SeqFeatures" ) ) {
            ; // list of Sequence features, we ignore
        } else if ( associationName.equals( "OntologyEntries" ) ) {
            ; // list of generic ontology entries, we ignore.
        } else {
            log.error( "Unknown or unsupported type " + associationName );
        }

    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.designElement.CompositeSequence convertCompositeSequence(
            org.biomage.DesignElement.CompositeSequence mageObj ) {

        if ( mageObj == null ) return null;

        CompositeSequence result = CompositeSequence.Factory.newInstance();
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
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
        convertIdentifiable( mageObj, result ); // takes care of description...
        convertAssociations( mageObj, result ); // contacts
        return result;
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
        intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associationName.equals( "Database" ) )
            simpleFillIn( mageObj, gemmaObj, getter );
        else if ( associationName.equals( "Type" ) )
            ; // we ain't got that.
        else
            log.warn( "Unsupported or unknown association: " + associationName );
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
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioMaterialAssociations( BioSample mageObj, BioMaterial gemmaObj, Method getter ) {
        log.debug( "convertBioMaterialAssociations (biosample) Not supported yet" );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioMaterialAssociations( LabeledExtract mageObj, BioMaterial gemmaObj, Method getter ) {
        log.debug( "convertBioMaterialAssociations (labeled extract) Not supported yet" );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioMaterialAssociations( BioSource mageObj, BioMaterial gemmaObj, Method getter ) {
        log.debug( "convertBioMaterialAssociations (biosource) Not supported yet" );
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

        if ( associationName.equals( "BibliographicReferences" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "BibliographicReference" );
        } else if ( associationName.equals( "DatabaseReferences" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "DatabaseEntries" );
        } else if ( associationName.equals( "Annotations" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "Annotations" );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Incomplete
     * 
     * @param mageObj
     * @return
     */
    public ExpressionExperiment convertExperiment( Experiment mageObj ) {
        if ( mageObj == null ) return null;

        log.debug( "Converting Experiment: " + mageObj.getName() );

        ExpressionExperiment result = ExpressionExperiment.Factory.newInstance();

        result.setSource( "Imported from MAGE-ML" );

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertExpressionExperimentAssociations( Experiment mageObj, ExpressionExperiment gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, gemmaObj, getter );
        String associationName = getterToPropertyName( getter );

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

        if ( associationName.equals( "Contacts" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Contact" );
        else
            log.warn( "Unsupported or unknown association: " + associationName );

    }

    /**
     * Copy attributes from a MAGE identifiable to a Gemma identifiscribable.
     * 
     * @param mageObj
     */
    public void convertIdentifiable( Identifiable mageObj, Identifscribable gemmaObj ) {

        if ( mageObj == null ) return;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        gemmaObj.setIdentifier( mageObj.getIdentifier() );
        gemmaObj.setName( mageObj.getName() );
        convertDescribable( mageObj, gemmaObj );
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
     * TODO a reporter has a feature, which has the location information we need for the Gemma reporter.
     * 
     * @param mageObj
     * @return edu.columbia.gemma.expression.designElement.Reporter
     */
    public edu.columbia.gemma.expression.designElement.Reporter convertReporter(
            org.biomage.DesignElement.Reporter mageObj ) {

        if ( mageObj == null ) return null;

        Reporter result = Reporter.Factory.newInstance();
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );

        return result;
    }

    /**
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
     * @param mageObj
     * @return Converted object. If the source object is null, the return value is null.
     */
    private Object findAndInvokeConverter( Object mageObj ) {

        if ( mageObj == null ) return null;

        Method converter = findConverter( mageObj );
        if ( converter == null ) {
            return null;
        }
        Object convertedGemmaObj = null;
        try {
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
        if ( gemmaConverter == null ) {
            return null;
        }
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
            log.warn( "Conversion operation not yet supported: " + "convert" + mageTypeName );
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
            log.warn( "Getter called on " + mageObj.getClass().getName() + " but failed to return a value: "
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
        try {
            associatedObject = getter.invoke( mageObj, new Object[] {} );
        } catch ( IllegalArgumentException e ) {
            log.error( e ); // really impossible, as we are calling a no-arg getter.
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( mageObj.getClass().getName() + "." + getter.getName() + " threw an exception: " + e.getCause(),
                    e );
        }
        return associatedObject;
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
            log.warn( "List was not null, but empty" );
            return;
        }

        String associationName = actualGemmaAssociationName;

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( IllegalArgumentException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Generic method to fill in a Gemma object's association with a Mage object where the name can be predicted from
     * the associated object type. E.g., the Gemma object with an association to "BioSequence" has a "bioSequence"
     * property; sometimes instead we have things like ImmobilizedCharacteristic.
     * 
     * @param mageObj The mage object from which to obtain the object
     * @param gemmaObj The Gemma object in which to place the converted Mage object.
     * @param getter The getter for the Mage object
     */
    private void simpleFillIn( Object mageObj, Object gemmaObj, Method getter ) {
        this.simpleFillIn( mageObj, gemmaObj, getter, null );
    }

    /**
     * Generic method to fill in a Gemma object's association with a Mage object where the name might be predicted from
     * the associated object type. E.g., the Gemma object with an association to "BioSequence" has a "bioSequence"
     * property; sometimes instead we have things like ImmobilizedCharacteristic.
     * 
     * @param mageObj The mage object from which to obtain the object
     * @param gemmaObj The Gemma object in which to place the converted Mage object.
     * @param getter The getter for the Mage object
     * @param actualGemmaAssociationName - Replacement name for the Gemma association. This is to handle situations
     *        where the getter does not have a name that can be figured out. If null, the name is figured out from the
     *        getter.
     */
    private void simpleFillIn( Object mageObj, Object gemmaObj, Method getter, String actualGemmaAssociationName ) {
        Object associatedObject = invokeGetter( mageObj, getter );
        if ( associatedObject == null ) return;

        String associationName = getterToPropertyName( getter );

        try {
            Class mageAssociatedType = getter.getReturnType();
            Object gemmaAssociatedObj = findAndInvokeConverter( associatedObject, associationName, mageAssociatedType );
            if ( gemmaAssociatedObj == null ) return;

            Class gemmaClass = ReflectionUtil.getImplForBase( gemmaAssociatedObj.getClass() );

            String inferredGemmaAssociationName;
            if ( actualGemmaAssociationName != null ) {
                inferredGemmaAssociationName = actualGemmaAssociationName;
            } else {
                inferredGemmaAssociationName = ReflectionUtil.classToTypeName( ReflectionUtil
                        .getBaseForImpl( gemmaAssociatedObj ) );
            }

            findAndInvokeSetter( gemmaObj, gemmaAssociatedObj, gemmaClass, inferredGemmaAssociationName );

        } catch ( SecurityException e ) {
            log.error( e );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        }

    }

}
