package ubic.gemma.web.util.dwr;

import lombok.Value;

@Value
public class DwrException {
    String javaClassName;
    String message;
}
