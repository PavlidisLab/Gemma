#
# Eliminate zeroes from an AnnData object
#

import sys

try:
    import anndata
    from scipy.sparse import csr_matrix
except ImportError as e:
    print('You need anndata and scipy to run this script. Install it with "pip install anndata".')
    raise e

try:
    input_file, output_file = sys.argv[1:]
except Exception as e:
    print('Usage: python pack-anndata.py input_file output_file')
    raise e

print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file)
print("Converting /X to CSR...")
df.X = csr_matrix(df.X)
for layer in df.layers:
    print("Converting /layers/" + layer + " to CSR...")
    df.layers[layer] = csr_matrix(df.layers[layer])
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)
