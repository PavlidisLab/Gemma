

package ubic.gemma.model.association;

import java.util.*;

/**
 * This enumeration was originally based on GO, but is used for all entities that have evidenciary aspects; Thus it has
 * been expanded to include: Terms from RGD&#160;(rat genome database)
 * <ul>
 * <li>IED = Inferred from experimental data
 * <li>IAGP = Inferred from association of genotype and phenotype
 * <li>IPM = Inferred from phenotype manipulation
 * <li>QTM = Quantitative Trait Measurement
 * </ul>
 * And our own custom code IIA which means Inferred from Imported Annotation to distinguish IEAs that we ourselves have
 * computed
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class GOEvidenceCode implements java.io.Serializable, Comparable<GOEvidenceCode> {
    public static final GOEvidenceCode IC = new GOEvidenceCode( "IC" );
    public static final GOEvidenceCode IDA = new GOEvidenceCode( "IDA" );
    public static final GOEvidenceCode IEA = new GOEvidenceCode( "IEA" );
    public static final GOEvidenceCode IEP = new GOEvidenceCode( "IEP" );
    public static final GOEvidenceCode IGI = new GOEvidenceCode( "IGI" );
    public static final GOEvidenceCode IMP = new GOEvidenceCode( "IMP" );
    public static final GOEvidenceCode IPI = new GOEvidenceCode( "IPI" );
    public static final GOEvidenceCode ISS = new GOEvidenceCode( "ISS" );
    public static final GOEvidenceCode NAS = new GOEvidenceCode( "NAS" );
    public static final GOEvidenceCode ND = new GOEvidenceCode( "ND" );
    public static final GOEvidenceCode RCA = new GOEvidenceCode( "RCA" );
    public static final GOEvidenceCode TAS = new GOEvidenceCode( "TAS" );
    public static final GOEvidenceCode NR = new GOEvidenceCode( "NR" );
    public static final GOEvidenceCode EXP = new GOEvidenceCode( "EXP" );
    public static final GOEvidenceCode ISA = new GOEvidenceCode( "ISA" );
    public static final GOEvidenceCode ISM = new GOEvidenceCode( "ISM" );
    /**
     * Inferred from Genomic Context; This evidence code can be used whenever information about the genomic context of a
     * gene product forms part of the evidence for a particular annotation. Genomic context includes, but is not limited
     * to, such things as identity of the genes neighboring the gene product in question (i.e. synteny), operon
     * structure, and phylogenetic or other whole genome analysis. "We recommend making an entry in the with/from column
     * when using this evidence code. In cases where operon structure or synteny are the compelling evidence, include
     * identifier(s) for the neighboring genes in the with/from column. In casees where metabolic reconstruction is the
     * compelling evidence, and there is an identifier for the pathway or system, that should be entered in the
     * with/from column. When multiple entries are placed in the with/from field, they are separated by pipes."
     */
    public static final GOEvidenceCode IGC = new GOEvidenceCode( "IGC" );

    public static final GOEvidenceCode ISO = new GOEvidenceCode( "ISO" );
    /**
     * Added by Gemma: Inferred from Imported Annotation. To be distinguished from IEA or IC, represents annotations
     * that were present in imported data, and which have unknown evidence in the original source (though generally put
     * there manually).
     */
    public static final GOEvidenceCode IIA = new GOEvidenceCode( "IIA" );
    /**
     * A type of phylogenetic evidence whereby an aspect of a descendant is inferred through the characterization of an
     * aspect of a ancestral gene.
     */
    public static final GOEvidenceCode IBA = new GOEvidenceCode( "IBA" );
    /**
     * A type of phylogenetic evidence whereby an aspect of an ancestral gene is inferred through the characterization
     * of an aspect of a descendant gene.
     */
    public static final GOEvidenceCode IBD = new GOEvidenceCode( "IBD" );
    /**
     * A type of phylogenetic evidence characterized by the loss of key sequence residues. Annotating with this evidence
     * codes implies a NOT annotation. This evidence code is also referred to as IMR (inferred from Missing Residues).
     */
    public static final GOEvidenceCode IKR = new GOEvidenceCode( "IKR" );
    /**
     * Inferred from Rapid Divergence. A type of phylogenetic evidence characterized by rapid divergence from ancestral
     * sequence. Annotating with this evidence codes implies a NOT annotation.
     */
    public static final GOEvidenceCode IRD = new GOEvidenceCode( "IRD" );
    /**
     * Inferred from Missing Residues. Represents a NOT association. IMR is a synonym of IKR.
     */
    public static final GOEvidenceCode IMR = new GOEvidenceCode( "IMR" );
    /**
     * Inferred from experimental data (RGD code)
     */
    public static final GOEvidenceCode IED = new GOEvidenceCode( "IED" );
    /**
     * Inferred from association of genotype and phenotype (RGD code)
     */
    public static final GOEvidenceCode IAGP = new GOEvidenceCode( "IAGP" );
    /**
     * Inferred from phenotype manipulation (RGD code)
     */
    public static final GOEvidenceCode IPM = new GOEvidenceCode( "IPM" );
    /**
     * Quantitative Trait Measurement (RGD code)
     */
    public static final GOEvidenceCode QTM = new GOEvidenceCode( "QTM" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1672679992320181566L;
    private static final Map<String, GOEvidenceCode> values = new LinkedHashMap<>( 28, 1 );
    private static List<String> literals = new ArrayList<>( 28 );
    private static List<String> names = new ArrayList<>( 28 );
    private static List<GOEvidenceCode> valueList = new ArrayList<>( 28 );

    static {
        GOEvidenceCode.values.put( GOEvidenceCode.IC.value, GOEvidenceCode.IC );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IC );
        GOEvidenceCode.literals.add( GOEvidenceCode.IC.value );
        GOEvidenceCode.names.add( "IC" );
        GOEvidenceCode.values.put( GOEvidenceCode.IDA.value, GOEvidenceCode.IDA );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IDA );
        GOEvidenceCode.literals.add( GOEvidenceCode.IDA.value );
        GOEvidenceCode.names.add( "IDA" );
        GOEvidenceCode.values.put( GOEvidenceCode.IEA.value, GOEvidenceCode.IEA );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IEA );
        GOEvidenceCode.literals.add( GOEvidenceCode.IEA.value );
        GOEvidenceCode.names.add( "IEA" );
        GOEvidenceCode.values.put( GOEvidenceCode.IEP.value, GOEvidenceCode.IEP );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IEP );
        GOEvidenceCode.literals.add( GOEvidenceCode.IEP.value );
        GOEvidenceCode.names.add( "IEP" );
        GOEvidenceCode.values.put( GOEvidenceCode.IGI.value, GOEvidenceCode.IGI );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IGI );
        GOEvidenceCode.literals.add( GOEvidenceCode.IGI.value );
        GOEvidenceCode.names.add( "IGI" );
        GOEvidenceCode.values.put( GOEvidenceCode.IMP.value, GOEvidenceCode.IMP );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IMP );
        GOEvidenceCode.literals.add( GOEvidenceCode.IMP.value );
        GOEvidenceCode.names.add( "IMP" );
        GOEvidenceCode.values.put( GOEvidenceCode.IPI.value, GOEvidenceCode.IPI );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IPI );
        GOEvidenceCode.literals.add( GOEvidenceCode.IPI.value );
        GOEvidenceCode.names.add( "IPI" );
        GOEvidenceCode.values.put( GOEvidenceCode.ISS.value, GOEvidenceCode.ISS );
        GOEvidenceCode.valueList.add( GOEvidenceCode.ISS );
        GOEvidenceCode.literals.add( GOEvidenceCode.ISS.value );
        GOEvidenceCode.names.add( "ISS" );
        GOEvidenceCode.values.put( GOEvidenceCode.NAS.value, GOEvidenceCode.NAS );
        GOEvidenceCode.valueList.add( GOEvidenceCode.NAS );
        GOEvidenceCode.literals.add( GOEvidenceCode.NAS.value );
        GOEvidenceCode.names.add( "NAS" );
        GOEvidenceCode.values.put( GOEvidenceCode.ND.value, GOEvidenceCode.ND );
        GOEvidenceCode.valueList.add( GOEvidenceCode.ND );
        GOEvidenceCode.literals.add( GOEvidenceCode.ND.value );
        GOEvidenceCode.names.add( "ND" );
        GOEvidenceCode.values.put( GOEvidenceCode.RCA.value, GOEvidenceCode.RCA );
        GOEvidenceCode.valueList.add( GOEvidenceCode.RCA );
        GOEvidenceCode.literals.add( GOEvidenceCode.RCA.value );
        GOEvidenceCode.names.add( "RCA" );
        GOEvidenceCode.values.put( GOEvidenceCode.TAS.value, GOEvidenceCode.TAS );
        GOEvidenceCode.valueList.add( GOEvidenceCode.TAS );
        GOEvidenceCode.literals.add( GOEvidenceCode.TAS.value );
        GOEvidenceCode.names.add( "TAS" );
        GOEvidenceCode.values.put( GOEvidenceCode.NR.value, GOEvidenceCode.NR );
        GOEvidenceCode.valueList.add( GOEvidenceCode.NR );
        GOEvidenceCode.literals.add( GOEvidenceCode.NR.value );
        GOEvidenceCode.names.add( "NR" );
        GOEvidenceCode.values.put( GOEvidenceCode.EXP.value, GOEvidenceCode.EXP );
        GOEvidenceCode.valueList.add( GOEvidenceCode.EXP );
        GOEvidenceCode.literals.add( GOEvidenceCode.EXP.value );
        GOEvidenceCode.names.add( "EXP" );
        GOEvidenceCode.values.put( GOEvidenceCode.ISA.value, GOEvidenceCode.ISA );
        GOEvidenceCode.valueList.add( GOEvidenceCode.ISA );
        GOEvidenceCode.literals.add( GOEvidenceCode.ISA.value );
        GOEvidenceCode.names.add( "ISA" );
        GOEvidenceCode.values.put( GOEvidenceCode.ISM.value, GOEvidenceCode.ISM );
        GOEvidenceCode.valueList.add( GOEvidenceCode.ISM );
        GOEvidenceCode.literals.add( GOEvidenceCode.ISM.value );
        GOEvidenceCode.names.add( "ISM" );
        GOEvidenceCode.values.put( GOEvidenceCode.IGC.value, GOEvidenceCode.IGC );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IGC );
        GOEvidenceCode.literals.add( GOEvidenceCode.IGC.value );
        GOEvidenceCode.names.add( "IGC" );
        GOEvidenceCode.values.put( GOEvidenceCode.ISO.value, GOEvidenceCode.ISO );
        GOEvidenceCode.valueList.add( GOEvidenceCode.ISO );
        GOEvidenceCode.literals.add( GOEvidenceCode.ISO.value );
        GOEvidenceCode.names.add( "ISO" );
        GOEvidenceCode.values.put( GOEvidenceCode.IIA.value, GOEvidenceCode.IIA );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IIA );
        GOEvidenceCode.literals.add( GOEvidenceCode.IIA.value );
        GOEvidenceCode.names.add( "IIA" );
        GOEvidenceCode.values.put( GOEvidenceCode.IBA.value, GOEvidenceCode.IBA );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IBA );
        GOEvidenceCode.literals.add( GOEvidenceCode.IBA.value );
        GOEvidenceCode.names.add( "IBA" );
        GOEvidenceCode.values.put( GOEvidenceCode.IBD.value, GOEvidenceCode.IBD );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IBD );
        GOEvidenceCode.literals.add( GOEvidenceCode.IBD.value );
        GOEvidenceCode.names.add( "IBD" );
        GOEvidenceCode.values.put( GOEvidenceCode.IKR.value, GOEvidenceCode.IKR );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IKR );
        GOEvidenceCode.literals.add( GOEvidenceCode.IKR.value );
        GOEvidenceCode.names.add( "IKR" );
        GOEvidenceCode.values.put( GOEvidenceCode.IRD.value, GOEvidenceCode.IRD );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IRD );
        GOEvidenceCode.literals.add( GOEvidenceCode.IRD.value );
        GOEvidenceCode.names.add( "IRD" );
        GOEvidenceCode.values.put( GOEvidenceCode.IMR.value, GOEvidenceCode.IMR );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IMR );
        GOEvidenceCode.literals.add( GOEvidenceCode.IMR.value );
        GOEvidenceCode.names.add( "IMR" );
        GOEvidenceCode.values.put( GOEvidenceCode.IED.value, GOEvidenceCode.IED );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IED );
        GOEvidenceCode.literals.add( GOEvidenceCode.IED.value );
        GOEvidenceCode.names.add( "IED" );
        GOEvidenceCode.values.put( GOEvidenceCode.IAGP.value, GOEvidenceCode.IAGP );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IAGP );
        GOEvidenceCode.literals.add( GOEvidenceCode.IAGP.value );
        GOEvidenceCode.names.add( "IAGP" );
        GOEvidenceCode.values.put( GOEvidenceCode.IPM.value, GOEvidenceCode.IPM );
        GOEvidenceCode.valueList.add( GOEvidenceCode.IPM );
        GOEvidenceCode.literals.add( GOEvidenceCode.IPM.value );
        GOEvidenceCode.names.add( "IPM" );
        GOEvidenceCode.values.put( GOEvidenceCode.QTM.value, GOEvidenceCode.QTM );
        GOEvidenceCode.valueList.add( GOEvidenceCode.QTM );
        GOEvidenceCode.literals.add( GOEvidenceCode.QTM.value );
        GOEvidenceCode.names.add( "QTM" );
        GOEvidenceCode.valueList = Collections.unmodifiableList( GOEvidenceCode.valueList );
        GOEvidenceCode.literals = Collections.unmodifiableList( GOEvidenceCode.literals );
        GOEvidenceCode.names = Collections.unmodifiableList( GOEvidenceCode.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    GOEvidenceCode() {
    }

    private GOEvidenceCode( String value ) {
        this.value = value;
    }

    /**
     * @param value the value to create the GOEvidenceCode from.
     * @return Creates an instance of GOEvidenceCode from <code>value</code>.
     */
    public static GOEvidenceCode fromString( String value ) {
        final GOEvidenceCode typeValue = GOEvidenceCode.values.get( value );
        if ( typeValue == null ) {
            /*
             * Customization to permit database values to change before code does. Previously this would throw an
             * exception.
             */
            // throw new IllegalArgumentException("invalid value '" + value + "', possible values are: " + literals);
            return null;
        }
        return typeValue;
    }

    /**
     * Returns an unmodifiable list containing the literals that are known by this enumeration.
     *
     * @return A List containing the actual literals defined by this enumeration, this list can not be modified.
     */
    public static List<String> literals() {
        return GOEvidenceCode.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return GOEvidenceCode.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<GOEvidenceCode> values() {
        return GOEvidenceCode.valueList;
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( GOEvidenceCode that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     *
     * @return the underlying value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof GOEvidenceCode && ( ( GOEvidenceCode ) object ).getValue()
                .equals( this.getValue() ) );
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return String.valueOf( value );
    }

    /**
     * This method allows the deserialization of an instance of this enumeration type to return the actual instance that
     * will be the singleton for the JVM in which the current thread is running. Doing this will allow users to safely
     * use the equality operator <code>==</code> for enumerations because a regular deserialized object is always a
     * newly constructed instance and will therefore never be an existing reference; it is this
     * <code>readResolve()</code> method which will intercept the deserialization process in order to return the proper
     * singleton reference. This method is documented here:
     * <a href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return GOEvidenceCode.fromString( this.value );
    }
}