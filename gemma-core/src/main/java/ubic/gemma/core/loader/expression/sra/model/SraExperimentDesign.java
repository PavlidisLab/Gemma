package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraExperimentDesign {
    @XmlElement(name = "DESIGN_DESCRIPTION")
    private String description;
    @XmlElement(name = "LIBRARY_DESCRIPTOR")
    private SraLibraryDescriptor libraryDescriptor;
}
