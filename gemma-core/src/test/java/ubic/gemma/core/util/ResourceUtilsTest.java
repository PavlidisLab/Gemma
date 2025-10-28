package ubic.gemma.core.util;

import org.junit.Test;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.ResourceUtils.getSourceCodeLocation;

public class ResourceUtilsTest {

    @Test
    public void test() {
        assertThat( getSourceCodeLocation( requireNonNull( getClass().getResource( "/ubic/gemma/core/util/ResourceUtils.class" ) ) ) )
                .endsWith( "gemma-core/src/main/java/ubic/gemma/core/util/ResourceUtils.java" );
        assertThat( getSourceCodeLocation( requireNonNull( getClass().getResource( "/sql/init-data.sql" ) ) ) )
                .endsWith( "gemma-core/src/main/resources/sql/init-data.sql" );
    }
}