#
# Rewrite an AnnData file
#

import sys

try:
    import anndata
except ImportError as e:
    print('You need anndata to run this script. Install it with "pip install anndata".')
    raise e

try:
    input_file, output_file = sys.argv[1:]
except Exception as e:
    print('Usage: python rewrite-anndata.py input_file output_file')
    raise e

print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file)
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)
