package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "EXPERIMENT_PACKAGE_SET")
public class SraExperimentPackageSet {
    @XmlElement(name = "EXPERIMENT_PACKAGE")
    private List<SraExperimentPackage> experimentPackages;
}
