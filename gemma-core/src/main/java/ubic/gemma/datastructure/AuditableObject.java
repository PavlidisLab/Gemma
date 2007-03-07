package ubic.gemma.datastructure;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jsantos
 *
 */
public class AuditableObject implements Serializable {
    // small holder struct to ease serialization of the list


        private static final long serialVersionUID = -7862129089784691035L;
        public String type = null;
        public Long id = null;
        public Date date = null;

}
