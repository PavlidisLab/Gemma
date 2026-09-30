#
# Transpose an AnnData object
#

import sys
import os
import resource


def memory_limit(ratio):
    """
    Cap the memory this process can commit to a fraction of the host's available memory, so that an oversized
    dataset fails with a MemoryError instead of taking the host down.

    RLIMIT_DATA is used rather than RLIMIT_AS: it counts private writable mappings (the heap and the anonymous
    mappings backing NumPy arrays), whereas RLIMIT_AS also counts address space that is reserved but never used,
    such as the per-thread malloc arenas that threads create.

    Either limit also counts the stack and buffers that OpenBLAS reserves for each of its threads, one per core,
    which is several GiB on a many-core host before any data is loaded; hitting the limit while those threads start
    crashes the interpreter instead of raising a MemoryError. This script does not need BLAS, so it is restricted to
    a single thread.

    The ratio can be overridden with GEMMA_PYTHON_MEMORY_LIMIT_RATIO; set it to 0 to disable the limit. This is a
    no-op where /proc/meminfo is not available (e.g. macOS).
    """
    os.environ.setdefault('OPENBLAS_NUM_THREADS', '1')
    os.environ.setdefault('OMP_NUM_THREADS', '1')
    ratio = float(os.environ.get('GEMMA_PYTHON_MEMORY_LIMIT_RATIO', ratio))
    if ratio <= 0:
        return
    try:
        with open('/proc/meminfo') as f:
            available_memory = next(int(line.split()[1]) * 1024 for line in f if line.startswith('MemAvailable:'))
    except (OSError, StopIteration):
        print('Could not determine available memory from /proc/meminfo, not limiting memory usage.')
        return
    limit = int(available_memory * ratio)
    soft, hard = resource.getrlimit(resource.RLIMIT_DATA)
    # never raise an existing limit
    if soft != resource.RLIM_INFINITY:
        limit = min(limit, soft)
    if hard != resource.RLIM_INFINITY:
        limit = min(limit, hard)
    print('Limiting memory usage to %.1f GiB.' % (limit / 1024 ** 3))
    resource.setrlimit(resource.RLIMIT_DATA, (limit, hard))


memory_limit(.9)

try:
    import anndata
    from scipy.sparse import issparse, isspmatrix_csr
except ImportError as e:
    print('You need anndata and scipy to run this script. Install it with "pip install anndata".')
    raise e

try:
    input_file, output_file = sys.argv[1:]
except Exception as e:
    print('Usage: python transpose-anndata.py input_file output_file')
    raise e

print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file).transpose()
# make sure that the main data and layers are efficiently accessible by row by
# either 1) stored in CSR or 2) being stored in row-major format.
if issparse(df.X) and not isspmatrix_csr(df.X):
    print("Rewriting /X to CSR...")
    df.X = df.X.tocsr()
for layer in df.layers:
    if issparse(df.layers[layer]) and not isspmatrix_csr(df.layers[layer]):
        print("Rewriting /layers/" + layer + " to CSR...")
        df.layers[layer] = df.layers[layer].tocsr()
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)
