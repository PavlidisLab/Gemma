package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraExperimentDesign {
    @XmlElement(name = "DESIGN_DESCRIPTION")
    private String description;
    @XmlElement(name = "LIBRARY_DESCRIPTOR")
    private SraLibraryDescriptor libraryDescriptor;
}
