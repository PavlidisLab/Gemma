package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SingleCellConfig {

    @Bean
    public SingleCellSparsityMetrics singleCellSparsityMetrics() {
        return new SingleCellSparsityMetrics();
    }
}
