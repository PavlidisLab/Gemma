import arrayExpressLogo from '../../../../images/logo/arrayexpress-logo-large-text.png';
import geoLogo from '../../../../images/logo/geo-logo.png';
import sraLogo from '../../../../images/logo/sra-logo.png';
import biorxivLogo from '../../../../images/logo/biorxiv-logo.png';
import arxivLogo from '../../../../images/logo/arxiv-logo.svg';
import zenodoLogo from '../../../../images/logo/zenodo-logo.svg';
import synapseLogo from '../../../../images/logo/synapse-logo-text.svg';
import cellxgeneLogo from '../../../../images/logo/cellxgene-logo-inverted.png';
import ucscCellBrowserLogo from '../../../../images/logo/ucsc-cellbrowser-logo.png';
import bioStudiesLogo from '../../../../images/logo/biostudies-logo.png';
import pubMedLogo from '../../../../images/logo/pubmed-logo-blue.svg';
import ncbiGeneLogo from '../../../../images/logo/ncbi-symbol.svg';

/**
 * Utilities for working with external databases.
 *
 * This should be kept in sync with ExternalDatabaseUtils.java and ExternalDatabaseWebUtils.java.
 * @author poirigui
 */
export default {
   /**
    * Declarations of all external databases known to Gemma.
    */
   externalDatabases : [
      {
         name : 'ArrayExpress',
         logo : arrayExpressLogo
      },
      {
         name : 'GEO',
         logo : geoLogo
      },
      {
         name : 'SRA',
         logo : sraLogo
      },
      {
         name : 'CELLxGENE',
         logo : cellxgeneLogo
      },
      {
         name : 'BioStudies',
         logo : bioStudiesLogo
      },
      {
         name : 'UCSC Cell Browser',
         logo : ucscCellBrowserLogo
      },
      {
         name : 'PubMed',
         logo : pubMedLogo
      },
      {
         name : 'bioRxiv',
         logo : biorxivLogo
      },
      {
         name : 'arXiv',
         logo : arxivLogo
      },
      {
         name : 'Zenodo',
         logo : zenodoLogo
      },
      {
         name : 'Synapse',
         logo : synapseLogo
      },
      {
         name : 'gene',
         logo : ncbiGeneLogo
      }
   ]
};
