package ubic.gemma.core.ontology.jena;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary for The Gemma Factor Value Ontology (TGFVO).
 * @author poirigui
 */
public class TGFVO {

    /**
     * Namespace used by TGFVO.
     * <p>
     * This namespace is reserved for classes and properties that are necessary to connect factor values and their
     * annotations.
     */
    public static String NS = "http://gemma.msl.ubc.ca/ont/TGFVO#";

    /**
     * Relates a {@link ubic.gemma.model.expression.experiment.FactorValue} to one of its annotation which can be either
     * a subject or an object of one if its {@link ubic.gemma.model.expression.experiment.Statement} or its measurement.
     */
    public static Property hasAnnotation = ResourceFactory.createProperty( NS + "hasAnnotation" );
    /**
     * Inverse of {@link #hasAnnotation}.
     */
    public static Property annotationOf = ResourceFactory.createProperty( NS + "annotationOf" );
    /**
     * Relates a {@link ubic.gemma.model.expression.experiment.FactorValue} to its measurement.
     */
    public static Property hasMeasurement = ResourceFactory.createProperty( NS + "hasMeasurement" );
    /**
     * Inverse of {@link #hasMeasurement}.
     */
    public static Property measurementOf = ResourceFactory.createProperty( NS + "measurementOf" );

    /**
     * Represents a factor value {@link ubic.gemma.model.common.measurement.Measurement}.
     */
    public static Resource Measurement = ResourceFactory.createResource( NS + "Measurement" );
    /**
     * Relates a {@link ubic.gemma.model.common.measurement.Measurement} to its unit, if any.
     */
    public static Property hasUnit = ResourceFactory.createProperty( NS + "hasUnit" );
    /**
     * Relates a {@link ubic.gemma.model.common.measurement.Measurement} to its representation.
     */
    public static Property hasRepresentation = ResourceFactory.createProperty( NS + "hasRepresentation" );
    /**
     * Relates a {@link ubic.gemma.model.common.measurement.Measurement} to its value.
     */
    public static Property hasValue = ResourceFactory.createProperty( NS + "hasValue" );
}