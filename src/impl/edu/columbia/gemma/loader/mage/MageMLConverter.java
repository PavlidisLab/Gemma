package edu.columbia.gemma.loader.mage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.collections.functors.InstanceofPredicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.ArrayDesign.ArrayDesign;
import org.biomage.AuditAndSecurity.Audit;
import org.biomage.BioSequence.BioSequence;
import org.biomage.Common.Identifiable;
import org.biomage.Description.OntologyEntry;
import org.biomage.DesignElement.DesignElement;
import org.biomage.Interface.HasAuditTrail.AuditTrail_list;

import edu.columbia.gemma.common.Identifscribable;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.auditAndSecurity.Security;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.loader.smd.DataFileFetcher;
import edu.columbia.gemma.sequence.biosequence.SequenceType;

/**
 * Class to convet Mage domain objects to Gemma domain objects.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageMLConverter {

    protected static final Log log = LogFactory.getLog( MageMLConverter.class );
    private static final String START_AUDIT_NOTE = "Imported from MAGE";

    /**
     * A generic converter that figures out which specific method to call based on the class of the object.
     * 
     * @param fromObj
     * @return
     */
    public Object convert( Object fromObj ) {

        Object result = null;

        try {
            Class c = fromObj.getClass();
            String targetObjectName = c.getName().substring( c.getName().lastIndexOf( '.' ) + 1, c.getName().length() );
            String callMethodName = "convert" + targetObjectName;
            log.debug( "Seeking " + callMethodName );
            Method[] methods = this.getClass().getMethods();

            // locate a method that has the right signature. todo This is inefficient..
            Method callMethod = null;
            for ( int i = 0; i < methods.length; i++ ) {
                Class[] params = methods[i].getParameterTypes();
                // log.debug("Testing " + methods[i].getName());
                if ( methods[i].getName().equals( callMethodName ) && params.length == 1
                        && params[0] == fromObj.getClass() ) {
                    callMethod = methods[i];
                    break;
                }
            }

            if ( callMethod == null ) {
                log.warn( "Operation not yet supported: " + callMethodName );
                return null;
            }
            log.debug( "Invoking" + callMethod.getName() );

            result = callMethod.invoke( this, new Object[] {
                fromObj
            } );
        } catch ( IllegalArgumentException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( IllegalAccessException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( SecurityException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param mbs
     * @return
     */
    public edu.columbia.gemma.sequence.biosequence.BioSequence convertBioSequence(
            org.biomage.BioSequence.BioSequence fromObj ) {

        log.debug( "Converting BioSequence" );

        if ( fromObj == null ) return null;

        edu.columbia.gemma.sequence.biosequence.BioSequence result = edu.columbia.gemma.sequence.biosequence.BioSequence.Factory
                .newInstance();

        result.setIdentifier( fromObj.getIdentifier() );
        result.setName( fromObj.getName() );
        result.setSequence( fromObj.getSequence() );
        result.setType( this.convertBioSequenceType( fromObj.getType() ) );

        if ( fromObj.getDescriptions().size() > 1 ) {
            log.warn( "Multiple descriptions for " + fromObj );
        }

        if ( fromObj.getDescriptions().size() > 0 )
            result.setDescription( convertDescription( ( org.biomage.Description.Description ) fromObj.getDescriptions()
                    .get( 0 ) ) ); // we only allow one description.

        return result;
    }

    /**
     * @param type
     * @return
     */
    public SequenceType convertBioSequenceType( OntologyEntry fromObj ) {

        if ( fromObj == null ) return null;

        String value = fromObj.getValue();
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
        } else if ( value.equalsIgnoreCase( "dna" ) ) {
            // return SequenceType.DNA;
            return SequenceType.EST;
        } else {
            // return SequenceType.OTHER; // FIXME
            return SequenceType.EST;
        }
    }

    // public edu.columbia.gemma.common.auditAndSecurity.AuditTrail convertAuditTrail(List auditTrailList) {
    // if (auditTrailList == null) return null;
    //        
    // edu.columbia.gemma.common.auditAndSecurity.AuditTrail result =
    // edu.columbia.gemma.common.auditAndSecurity.AuditTrail.Factory.newInstance();
    //        
    // for ( Iterator iter = auditTrailList.iterator(); iter.hasNext(); ) {
    // Audit audit = ( Audit ) iter.next();
    // audit.getAction();
    // audit.getDate();
    // audit.getPerformer();
    // audit.get
    // }
    //        
    // return result;
    // }
    //    

    /**
     * @param ds
     * @return
     */
    public edu.columbia.gemma.common.description.Description convertDescription(
            org.biomage.Description.Description fromObj ) {

        if ( fromObj == null ) return null;

        edu.columbia.gemma.common.description.Description result = edu.columbia.gemma.common.description.Description.Factory
                .newInstance();
        result.setText( fromObj.getText() );
        return result;
    }

    /**
     * @param fromObj
     * @return
     */
    public edu.columbia.gemma.expression.designElement.Reporter convertReporter(
            org.biomage.DesignElement.Reporter fromObj ) {

        if ( fromObj == null ) return null;

        Reporter result = Reporter.Factory.newInstance();

        fromObj.getFeatureReporterMaps();

        result.setIdentifier( fromObj.getIdentifier() );
        result.setName( fromObj.getName() );

        if ( fromObj.getImmobilizedCharacteristics() != null ) {
            // fixme
        }

        return result;
    }

    /**
     * @param fromObj
     * @return
     */
    public edu.columbia.gemma.expression.designElement.CompositeSequence convertCompositeSequence(
            org.biomage.DesignElement.CompositeSequence fromObj ) {

        if ( fromObj == null ) return null;

        CompositeSequence result = CompositeSequence.Factory.newInstance();

        result.setIdentifier( fromObj.getIdentifier() );
        result.setName( fromObj.getName() );

        if ( fromObj.getBiologicalCharacteristics() != null ) {
            List bioSequences = fromObj.getBiologicalCharacteristics();
            if ( bioSequences.size() > 1 ) {
                log.warn( "Multiple biological characteristics for compositesequence" + fromObj );
            }
            result.setBiologicalCharacteristic( convertBioSequence( ( BioSequence ) bioSequences.get( 0 ) ) );

        }
        // TODO Auto-generated method stub
        return result;
    }

    /**
     * @param fromObj
     * @return
     */
    public Identifscribable convertIdentifiable( Identifiable fromObj ) {

        if ( fromObj == null ) return null;

        Identifscribable result = Identifscribable.Factory.newInstance();

        result.setIdentifier( fromObj.getIdentifier() );
        result.setName( fromObj.getName() );

        result.setSecurity( convertSecurity( fromObj.getSecurity() ) );
        result.setAuditTrail( convertAudit( fromObj.getAuditTrail() ) );

        return result;
    }

    /**
     * Convert an audit trail. Note that we don't want to import other people's audit trails! FIXME
     * 
     * @param fromObj
     * @return
     */
    public AuditTrail convertAudit( AuditTrail_list auditTrailList ) {

        if ( auditTrailList == null ) return null;

        AuditTrail result = AuditTrail.Factory.newInstance();

        result.start( START_AUDIT_NOTE );

        return result;
    }

    /**
     * Convert a security object. Note that we don't want to import other people's security!
     * 
     * @param fromObj
     * @return
     */
    public Security convertSecurity( org.biomage.AuditAndSecurity.Security security ) {

        if ( security == null ) return null;

        Security result = Security.Factory.newInstance();

        return result;
    }

    /**
     * @param fromObj
     * @return
     */
    public edu.columbia.gemma.expression.arrayDesign.ArrayDesign convertArrayDesign( ArrayDesign fromObj ) {
        if ( fromObj == null ) return null;

        edu.columbia.gemma.expression.arrayDesign.ArrayDesign result = edu.columbia.gemma.expression.arrayDesign.ArrayDesign.Factory
                .newInstance();

        return result;
    }

}
