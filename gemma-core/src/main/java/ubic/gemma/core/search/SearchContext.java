package ubic.gemma.core.search;

import lombok.Value;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Value
public class SearchContext {
    @Nullable
    Highlighter highlighter;
    @Nullable
    Consumer<Throwable> issueReporter;
}
