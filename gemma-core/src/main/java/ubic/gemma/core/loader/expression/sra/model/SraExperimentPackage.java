package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
