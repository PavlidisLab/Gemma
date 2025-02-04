#!/usr/bin/env python3

#
# This script generates the DWR client so that it can be bundled with the remaining static assets
#

import os
import re
import requests
from bs4 import BeautifulSoup
from getpass import getpass
from os.path import dirname, join

gemma_host_url = os.getenv('GEMMA_HOST') or 'https://gemma.msl.ubc.ca'
header = "/* this code is generated, see generate-dwr-client.py for details */"
dwr_script_dir = join(dirname(__file__), "src/main/webapp/scripts/api/dwr")
session_id = os.getenv('GEMMA_SESSION_ID') or getpass('Supply your JSESSIONID for ' + gemma_host_url + ': ')

with requests.Session() as session:
    session.cookies['JSESSIONID'] = session_id
    res = session.get(gemma_host_url + '/dwr/index.html')
    res.raise_for_status()
    s = BeautifulSoup(res.text, features='html.parser')
    controllers = []
    for l in s.find_all('a'):
        if l.get('href').startswith('/dwr/test/'):
            controllers.append(l.get('href')[len('/dwr/test/'):])
    controllers.sort()

    print(f'Found {len(controllers)} DWR controllers')

    with open(dwr_script_dir + '/index.js', 'w') as index:
        index.write(header + '\n')

        with open(dwr_script_dir + '/engine.js', 'w') as f:
            res = session.get(gemma_host_url + '/dwr/engine.js')
            res.raise_for_status()
            e = res.text
            e = e.replace("if (dwr == null) ", "")
            e = e.replace("if (dwr.engine == null) ", "")
            e = e.replace("if (DWREngine == null) ", "")
            f.write(header + '\n')
            f.write(e)
            f.write('window.dwr = dwr;\n')
            f.write('window.DWREngine = DWREngine;\n')
            f.write('export default dwr;\n')
        index.write("import dwr from './engine'\n")
        print('Wrote engine.js')

        with open(dwr_script_dir + '/util.js', 'w') as f:
            res = session.get(gemma_host_url + '/dwr/util.js')
            res.raise_for_status()
            u = res.text
            u = u.replace("if (dwr == null) var dwr = {};", "")
            u = u.replace("if (dwr.util == null) ", "")
            u = u.replace("if (DWRUtil == null) ", "")
            f.write(header + '\n')
            f.write(u)
            f.write("window.DWRUtil = DWRUtil;\n")
            f.write('export default dwr.util;\n')
        index.write("import './util'\n")
        print('Wrote util.js')

        wrote_models = False
        for controller in controllers:
            res = session.get(gemma_host_url + '/dwr/interface/' + controller + '.js')
            res.raise_for_status()
            with open(dwr_script_dir + '/interface/' + controller + '.js', 'w') as f:
                m, c = res.text.split("// Provide a default path to dwr.engine")
                c = c.split("if (" + controller + " == null) ")[1]
                if not wrote_models:
                    with open(dwr_script_dir + '/models.js', 'w') as mf:
                        mf.write(header + "\n")
                        mf.write(m)
                        matches = sorted(re.findall(r'function (.+)\(\) \{', m))
                        for match in matches:
                            mf.write("window." + match + " = " + match + "\n")
                        mf.write("module.exports = {" + ', '.join(matches) + "};\n")
                        print('Wrote models.js')
                        wrote_models = True
                    index.write("import './models'\n")
                f.write(header + '\n')
                f.write(c)
                f.write("window." + controller + " = " + controller + ";\n")
                f.write("export default " + controller + ";\n")
                print('Wrote interface/' + controller + '.js')
            index.write("import './interface/" + controller + "'\n")

        index.write("import './overrides'\n")
        index.write("export default dwr;")
        print('Wrote index.js')
