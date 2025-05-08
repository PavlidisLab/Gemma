package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "EXPERIMENT_PACKAGE_SET")
public class SraExperimentPackageSet {
    @XmlElement(name = "EXPERIMENT_PACKAGE")
    private List<SraExperimentPackage> experimentPackages;
}
