package edu.columbia.gemma.loader.mage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.ArrayDesign.ArrayDesign;
import org.biomage.BioSequence.BioSequence;
import org.biomage.Common.Describable;
import org.biomage.Common.Extendable;
import org.biomage.Common.Identifiable;
import org.biomage.Description.Description;
import org.biomage.Description.OntologyEntry;

import edu.columbia.gemma.common.Identifscribable;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.sequence.biosequence.PolymerType;
import edu.columbia.gemma.sequence.biosequence.SequenceType;
import edu.columbia.gemma.sequence.gene.Taxon;

/**
 * Class to convet Mage domain objects to Gemma domain objects. In most cases, the user can simply call the "convert"
 * method on any MAGE domain object and get a fully-populated Gemma domain object.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageMLConverter {
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
            Class c = mageObj.getClass();
            String targetObjectName = c.getName().substring( c.getName().lastIndexOf( '.' ) + 1, c.getName().length() );
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

            result = callMethod.invoke( this, new Object[] {
                mageObj
            } );

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
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.arrayDesign.ArrayDesign convertArrayDesign( ArrayDesign mageObj ) {
        if ( mageObj == null ) return null;

        edu.columbia.gemma.expression.arrayDesign.ArrayDesign result = edu.columbia.gemma.expression.arrayDesign.ArrayDesign.Factory
                .newInstance();

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

        convertIdentifiable( mageObj, result );
        result.setSequence( mageObj.getSequence() );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioSequenceAssociations( org.biomage.BioSequence.BioSequence mageObj,
            edu.columbia.gemma.sequence.biosequence.BioSequence gemmaObj, Method getter ) {

        Object associatedObject = invokeGetter( mageObj, getter );

        if ( associatedObject != null ) {
            String associationName = getterToPropertyName( getter );

            log.debug( mageObj.getClass().getName() + "--->" + associationName );

            if ( associationName.equals( "PolymerType" ) ) { // Ontology Entry - enumerated type.
                gemmaObj.setPolymerType( this.convertPolymerType( ( OntologyEntry ) associatedObject ) );
            } else if ( associationName.equals( "SeqFeatures" ) ) { // list of Sequence features, we ignore
                ;
            } else if ( associationName.equals( "OntologyEntries" ) ) { // list of generic ontology entries, we ignore.
                ;
            } else if ( associationName.equals( "SequenceDatabases" ) ) { // list of DatabaseEntries, we use one
                List seqdbs = ( List ) associatedObject;

                if ( seqdbs.size() == 0 ) {
                    log.error( "SequenceDatabases was not null, but empty" );
                    return;
                }

                org.biomage.Description.DatabaseEntry dbe = ( org.biomage.Description.DatabaseEntry ) seqdbs.get( 0 );
                gemmaObj.setSequenceDatabaseEntry( this.convertDatabaseEntry( dbe ) );
            } else if ( associationName.equals( "Type" ) ) { // ontology entry, we map to a enumerated type.
                gemmaObj.setType( this.convertBioSequenceType( ( OntologyEntry ) associatedObject ) );
            } else if ( associationName.equals( "Species" ) ) { // ontology entry, we map to a enumerated type.
                gemmaObj.setTaxon( this.convertSpecies( ( OntologyEntry ) associatedObject ) );
            } else {
                log.error( "Unknown type" );
            }

            // figure out what we need to do.
        } else {
            log.warn( "Getter called but failed to return a value: " + getter.getName() + " (Probably no data)" );
        }
    }

    /**
     * Call a 'get' method to retrieve an associated MAGE object for conversion.
     * 
     * @param mageObj
     * @param getter
     * @return A MAGE domain object, or a List of MAGE domain objects. The caller has to figure out which.
     */
    private Object invokeGetter( org.biomage.BioSequence.BioSequence mageObj, Method getter ) {
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
     * @param mageObj
     * @return SequenceType
     */
    public SequenceType convertBioSequenceType( OntologyEntry mageObj ) {

        if ( mageObj == null ) return null;

        log.debug( "Converting sequence type of \"" + mageObj.getValue() + "\"" );

        String value = mageObj.getValue();
        if ( value.equalsIgnoreCase( "bc" ) ) {
            return SequenceType.BAC;
        } else if ( value.equalsIgnoreCase( "est" ) ) {
            return SequenceType.EST;
        } else if ( value.equalsIgnoreCase( "affyprobe" ) ) {
            return SequenceType.AffyProbe;
        } else if ( value.equalsIgnoreCase( "affytarget" ) ) {
            return SequenceType.AffyTarget;
        } else if ( value.equalsIgnoreCase( "mrna" ) ) {
            return SequenceType.mRNA;
        } else if ( value.equalsIgnoreCase( "refseq" ) ) {
            return SequenceType.RefSeq;
        } else if ( value.equalsIgnoreCase( "chromosome" ) ) {
            return SequenceType.WholeChromosome;
        } else if ( value.equalsIgnoreCase( "genome" ) ) {
            return SequenceType.WholeGenome;
        } else if ( value.equalsIgnoreCase( "orf" ) ) {
            // fixme
            return SequenceType.EST;
        } else if ( value.equalsIgnoreCase( "dna" ) ) {
            // return SequenceType.DNA; // FIXME
            return SequenceType.EST;
        } else {
            // return SequenceType.OTHER; // FIXME
            return SequenceType.EST;
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
     * @param mageObj
     * @param gemmaObj
     */
    public void convertDescribable( Describable mageObj, Identifscribable gemmaObj ) {

        if ( mageObj == null ) return;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        if ( mageObj.getDescriptions().size() > 0 ) {
            gemmaObj.setDescription( convertDescription( ( Description ) mageObj.getDescriptions().get( 0 ) ) );
        }
        convertExtendable( mageObj, gemmaObj );
    }

    /**
     * @param mageObj
     * @return edu.columbia.gemma.common.description.Description
     */
    public edu.columbia.gemma.common.description.Description convertDescription(
            org.biomage.Description.Description mageObj ) {

        if ( mageObj == null ) return null;

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
     */
    public void convertExtendable( Extendable mageObj, Identifscribable gemmaObj ) {
        ; // nothing to do.
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
     * Generic method to find the associations a Mage object has and call the appropriate converter method.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    private void convertAssociations( Object mageObj, Object gemmaObj ) {

        String gemmaObjName = gemmaObj.getClass().getName().substring(
                gemmaObj.getClass().getName().lastIndexOf( '.' ) + 1 );

        Class classToSeek = null;

        if ( gemmaObjName.endsWith( "Impl" ) ) {
            gemmaObjName = gemmaObjName.substring( 0, gemmaObjName.lastIndexOf( "Impl" ) );
            classToSeek = gemmaObj.getClass().getSuperclass();
        }

        try {
            Class[] interfaces = mageObj.getClass().getInterfaces();
            for ( int i = 0; i < interfaces.length; i++ ) {
                Class infc = interfaces[i];
                String infcName = infc.getName().substring( infc.getName().lastIndexOf( '.' ) + 1 );

                if ( infcName.startsWith( "Has" ) ) {
                    String propertyName = infcName.substring( 3 );

                    Method getter = mageObj.getClass().getMethod( "get" + propertyName, new Class[] {} );

                    if ( getter != null ) {
                        // log.debug( mageObj.getClass().getName() + " has " + propertyName );
                        Method converter = null;
                        try {

                            // log.debug( "Seeking " + "convert" + gemmaObjName + "Association("
                            // + mageObj.getClass().getName() + ", " + classToSeek.getName() + ", Method" );

                            converter = this.getClass().getMethod( "convert" + gemmaObjName + "Associations",
                                    new Class[] {
                                            mageObj.getClass(), classToSeek, getter.getClass()
                                    } );

                            // log.debug( "Found " + converter.getName() );

                            if ( converter == null )
                                throw new NoSuchMethodException( "convert" + gemmaObjName + "Associations"
                                        + ": Not supported yet." );

                            converter.invoke( this, new Object[] {
                                    mageObj, gemmaObj, getter
                            } );
                        } catch ( NoSuchMethodException e ) {
                            ; // no problem, we're working on it.
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
            log.error( "For: " + gemmaObjName, e );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    private DatabaseEntry convertDatabaseEntry( org.biomage.Description.DatabaseEntry mageObj ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();
        // DatabaseEntry should not be identifiscribable, but it is.
        log.debug( "Converting DatabaseEntry " + mageObj.getAccession() );
        result.setAccession( mageObj.getAccession() );
        result.setURI( mageObj.getURI() );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param associatedObject
     * @return
     */
    private PolymerType convertPolymerType( OntologyEntry mageObj ) {
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
     * @param species
     * @return
     */
    private Taxon convertSpecies( OntologyEntry species ) {
        Taxon result = Taxon.Factory.newInstance();

        log.debug( "Converting Species from  " + species.getValue() );

        result.setCommonName( species.getValue() );

        // FIXME - this should be written to ensure that the result agrees with what is already in the database.
        return result;
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

}
