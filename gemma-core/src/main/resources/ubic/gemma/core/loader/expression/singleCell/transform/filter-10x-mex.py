import os
import sys
from os.path import basename, join

import pandas as pd
import scipy.io as sp_io
from cellranger import cr_io
from cellranger.cell_calling_helpers import call_initial_cells, call_additional_cells, FilterMethod
from cellranger.feature_ref import FeatureDef, FeatureReference
from cellranger.library_constants import GENE_EXPRESSION_LIBRARY_TYPE
from cellranger.matrix import CountMatrix
from cellranger.mtx_to_matrix_converter import load_mtx
from cellranger.utils import get_gem_group_from_barcode


def detect_gem_groups(mtx):
    gem_groups = set()
    for bc in mtx.bcs:
        gem_group = get_gem_group_from_barcode(bc)
        gem_groups.add(gem_group)
        
    gem_groups = sorted(gem_groups)
    return gem_groups


def save_features_tsv(feature_ref, base_dir, compress):
    """
    Save a FeatureReference to a tsv file.

    The Cell Ranger implementation in cr.save_features.tsv expects features to be bytes.
    """
    out_features_fn = join(base_dir, "features.tsv")
    if compress:
        out_features_fn += ".gz"

    with cr_io.open_maybe_gzip(out_features_fn, "wb") as f:
        for feature_def in feature_ref.feature_defs:
            f.write(feature_def.id.encode())
            f.write(b"\t")
            f.write(feature_def.name.encode() if feature_def.name else b"")
            f.write(b"\t")
            f.write(feature_def.feature_type.encode())
            f.write(b"\n")


def save_mex(matrix, base_dir):
    matrix.save_mex(base_dir, save_features_tsv)


def from_legacy_mtx(genome_dir):
    barcodes_tsv = (os.path.join(genome_dir, "barcodes.tsv.gz"))
    genes_tsv = (os.path.join(genome_dir, "features.tsv.gz"))
    matrix_mtx = (os.path.join(genome_dir, "matrix.mtx.gz"))
    for filepath in [barcodes_tsv, genes_tsv, matrix_mtx]:
        if not os.path.exists(filepath):
            raise OSError(f"Required file not found: {filepath}")
    barcodes = pd.read_csv(
        barcodes_tsv,
        delimiter="\t",
        header=None,
        usecols=[0],
    ).values.squeeze()
    genes = pd.read_csv(
        genes_tsv,
        delimiter="\t",
        header=None,
        usecols=[0],
    ).values.squeeze()
    feature_defs = [
        FeatureDef(idx, gene_id, None, GENE_EXPRESSION_LIBRARY_TYPE, {})
        for (idx, gene_id) in enumerate(genes)
    ]
    feature_ref = FeatureReference(feature_defs, [])

    matrix = sp_io.mmread(matrix_mtx)
    mat = CountMatrix(feature_ref, barcodes, matrix)
    return mat


def main():
    mtx_dir = sys.argv[1] 
    outdir = sys.argv[2] 
    genome_name = sys.argv[3]
    # make chem optional
    if len(sys.argv) > 4:
        chem = sys.argv[4]
    else:
        chem = None
    #
    if not mtx_dir and not outdir and not genome_name:
        raise ValueError("Both mtx_dir, outdir and genome_name must be provided.")

    try:
        mtx = load_mtx(mtx_dir=mtx_dir)
    except KeyError:
        # this is usually caused by missing columns in legacy MTX formats, see https://github.com/PavlidisLab/Gemma/issues/1269
        # It will be addressed at some point in Gemma, so this logic can be safely removed.
        mtx = from_legacy_mtx(mtx_dir)
    mtx.tocsc()
    mtx.feature_ref.add_tag('genome', dict(), genome_name)

    gem_groups = detect_gem_groups(mtx)
    print("Detected gem groups: " + ', '.join(map(str, gem_groups)))

    sample = basename(mtx_dir)

    filtered_metrics_groups, filtered_bcs_groups = call_initial_cells(
        matrix=mtx,
        genomes=[genome_name],
        sample=sample,
        unique_gem_groups=gem_groups,
        method=FilterMethod.ORDMAG_NONAMBIENT,
        recovered_cells=None,
        cell_barcodes=None,
        force_cells=None,
        feature_types=["Gene Expression"],
        chemistry_description=chem,
        target_features=None,
        has_cmo_data=False,
        num_probe_barcodes=None
    )

    filtered_bcs_groups, nonambient_summary, emptydrops_threshold = call_additional_cells(
        matrix=mtx,
        unique_gem_groups=gem_groups,
        genomes=[genome_name],
        filtered_bcs_groups=filtered_bcs_groups,
        feature_types=["Gene Expression"],
        chemistry_description=chem,
        probe_barcode_sample_id=None,
        num_probe_barcodes=None,
        emptydrops_minimum_umis=500
    )

    # filter CountMatrix to only include filtered barcodes

    filtered_bcs = []
    for (gem_group, _), barcodes in filtered_bcs_groups.items():
        filtered_bcs.extend(barcodes)
    print('Found cells: ' + str(len(filtered_bcs)))
    filtered_matrix = mtx.select_barcodes_by_seq(filtered_bcs)
    save_mex(filtered_matrix, outdir)

        
if __name__ == "__main__":
    main()

