package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraIdentifiers {
    @XmlElement(name = "PRIMARY_ID")
    private SraPrimaryId primaryId;
    @XmlElement(name = "SUBMITTER_ID")
    private List<SraSubmitterId> submitterIds;
    @XmlElement(name = "EXTERNAL_ID")
    private List<SraExternalId> externalIds;
}
