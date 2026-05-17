package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
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
