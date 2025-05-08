package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraLibraryLayout {
    @XmlElement(name = "PAIRED")
    @XmlJavaTypeAdapter(IsElementPresent.class)
    private Boolean paired;

    @XmlElement(name = "SINGLE")
    @XmlJavaTypeAdapter(IsElementPresent.class)
    private Boolean single;

    public boolean isPaired() {
        return paired != null && paired;
    }

    public boolean isSingle() {
        return single != null && single;
    }

    private static class IsElementPresent extends XmlAdapter<String, Boolean> {
        @Override
        public Boolean unmarshal( String v ) {
            return Boolean.TRUE;
        }

        @Override
        public String marshal( Boolean v ) {
            return v ? "" : null;
        }
    }
}
