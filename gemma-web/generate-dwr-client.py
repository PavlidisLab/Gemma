#!/usr/bin/env python

#
# This script generates the DWR client so that it can be bundled with the remaining static assets
#

import re

import requests

gemma_host_url = 'https://gemma.msl.ubc.ca'
header = "/* this code is generated, see generate-dwr-client.sh for details */"
dwr_script_dir = "src/main/webapp/scripts/api/dwr"

controllers = """
AnnotationController
ArrayDesignController
ArrayDesignRepeatScanController
AuditController
BatchInfoFetchController
BibliographicReferenceController
BioAssayController
BioMaterialController
CharacteristicBrowserController
CoexpressionSearchController
CompositeSequenceController
DEDVController
DiffExMetaAnalyzerController
DifferentialExpressionAnalysisController
DifferentialExpressionSearchController
EmptyController
ExperimentalDesignController
ExpressionDataFileUploadController
ExpressionExperimentController
ExpressionExperimentDataFetchController
ExpressionExperimentLoadController
ExpressionExperimentReportGenerationController
ExpressionExperimentSetController
FeedReader
FileUploadController
GeneController
GenePickerController
GeneSetController
GeoRecordBrowserController
IndexService
JavascriptLogger
LinkAnalysisController
PreprocessController
ProgressStatusService
SearchService
SecurityController
SignupController
SvdController
SystemMonitorController
TaskCompletionController
TwoChannelMissingValueController
UserListController
""".split()

with open(dwr_script_dir + '/index.js', 'w') as index:
    index.write(header + '\n')

    with open(dwr_script_dir + '/engine.js', 'w') as f:
        res = requests.get(gemma_host_url + '/dwr/engine.js')
        res.raise_for_status()
        e = res.text
        e = e.replace("if (dwr == null) ", "")
        e = e.replace("if (dwr.engine == null) ", "")
        e = e.replace("if (DWREngine == null) ", "")
        f.write(header + '\n')
        f.write(e)
        f.write('window.dwr = dwr;\n')
        f.write('window.DWREngine = DWREngine;\n')
    index.write("import './engine'\n")

    with open(dwr_script_dir + '/util.js', 'w') as f:
        res = requests.get(gemma_host_url + '/dwr/util.js')
        res.raise_for_status()
        u = res.text
        u = u.replace("if (dwr == null) var dwr = {};", "")
        u = u.replace("if (dwr.util == null) ", "")
        u = u.replace("if (DWRUtil == null) ", "")
        f.write(header + '\n')
        f.write(u)
        f.write("window.DWRUtil = DWRUtil;\n")
    index.write("import './util'\n")

    wrote_models = False
    for controller in controllers:
        res = requests.get(gemma_host_url + '/dwr/interface/' + controller + '.js')
        res.raise_for_status()
        with open(dwr_script_dir + '/interface/' + controller + '.js', 'w') as f:
            m, c = res.text.split("// Provide a default path to dwr.engine")
            c = c.split("if (" + controller + " == null) ")[1]
            if not wrote_models:
                with open(dwr_script_dir + '/models.js', 'w') as mf:
                    mf.write(header + "\n")
                    mf.write(m)
                    matches = re.findall(r'function (.+)\(\) \{', m)
                    for match in matches:
                        mf.write("window." + match + " = " + match + "\n")
                    wrote_models = True
                index.write("import './models'\n")
            f.write(header + '\n')
            f.write(c)
            f.write("window." + controller + " = " + controller + "\n")
        index.write("import './interface/" + controller + "'\n")

    index.write("import './overrides'\n")
