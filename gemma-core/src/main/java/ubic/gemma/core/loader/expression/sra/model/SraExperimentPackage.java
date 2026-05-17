package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraExperimentPackage {
    @XmlElement(name = "EXPERIMENT")
    private SraExperiment experiment;
    @XmlElement(name = "SUBMISSION")
    private SraSubmission submission;
    @XmlElement(name = "Organization")
    private SraOrganization organization;
    @XmlElement(name = "RUN_SET")
    private List<SraRunSet> runSets;
}
