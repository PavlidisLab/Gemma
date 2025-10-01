package ubic.gemma.core.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public class FutureUtils {

    /**
     * Map a function over a collection in parallel.
     * <p>
     * If any of the call fails, all the pending jobs are cancelled. When that happens, you may control whether to
     * interrupt running tasks with the mayInterruptIfRunning parameter.
     * <p>
     * Use this for simple parallelization scenarios. Alternatives such as {@link Stream#parallel()} and
     * {@link java.util.concurrent.CompletableFuture} should be considered for more complex workloads.
     *
     * @param func                  function to apply
     * @param collection            collection of elements to process
     * @param executorService       an executor service for running the tasks
     * @param mayInterruptIfRunning if true, the remaining tasks will be interrupted if they are running as per {@link Future#cancel(boolean)}
     */
    public static <T, U> List<U> parallelMap( Function<T, U> func, List<T> collection, ExecutorService executorService, boolean mayInterruptIfRunning ) {
        List<Future<U>> futures = new ArrayList<>( collection.size() );
        for ( T item : collection ) {
            futures.add( executorService.submit( () -> func.apply( item ) ) );
        }
        return resolveFutures( futures, mayInterruptIfRunning );
    }

    /**
     * Map a function over a range of integers in parallel.
     *
     * @see #parallelMap(Function, List, ExecutorService, boolean)
     */
    public static <U> List<U> parallelMapRange( IntFunction<U> func, int startInclusive, int endExclusive, ExecutorService executorService, boolean mayInterruptIfRunning ) {
        List<Future<U>> futures = new ArrayList<>( endExclusive - startInclusive );
        for ( int i = startInclusive; i < endExclusive; i++ ) {
            int finalI = i;
            futures.add( executorService.submit( () -> func.apply( finalI ) ) );
        }
        return resolveFutures( futures, mayInterruptIfRunning );
    }

    private static <U> List<U> resolveFutures( Collection<Future<U>> futures, boolean mayInterruptIfRunning ) {
        List<U> result = new ArrayList<>( futures.size() );
        Iterator<Future<U>> it = futures.iterator();
        while ( it.hasNext() ) {
            try {
                result.add( it.next().get() );
            } catch ( Exception e ) {
                it.forEachRemaining( f -> {
                    if ( !f.isDone() ) {
                        f.cancel( mayInterruptIfRunning );
                    }
                } );
                throw handleException( e );
            }
        }
        return result;
    }

    private static RuntimeException handleException( Throwable e ) {
        if ( e instanceof ExecutionException ) {
            return handleException( e.getCause() );
        } else if ( e instanceof RuntimeException ) {
            return ( RuntimeException ) e;
        } else {
            return new RuntimeException( e );
        }
    }
}
