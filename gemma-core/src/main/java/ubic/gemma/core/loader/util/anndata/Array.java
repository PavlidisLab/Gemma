package ubic.gemma.core.loader.util.anndata;

public interface Array<T> extends AutoCloseable {

    T get( int i );

    int size();

    @Override
    void close();
}
