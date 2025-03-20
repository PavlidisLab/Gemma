Ext.namespace('Gemma');

/**
 * need to set geneId as config
 */
Gemma.GeneDetails = Ext.extend(Ext.Panel, {

    padding: 10,
    defaults: {
        border: false,
        flex: 0
    },
    layoutConfig: {
        align: 'stretch'
    },
    layout: 'vbox',

    /**
     * @private
     * @param homologues
     * @param mainGeneSymbol
     * @returns {String}
     */
    renderHomologues: function (homologues, mainGeneSymbol) {
        var homologueStr = '';
        if (homologues && homologues.length > 0) {
            homologues.sort(function (a, b) {
                var A = a.taxonCommonName.toLowerCase();
                var B = b.taxonCommonName.toLowerCase();
                if (A < B)
                    return -1;
                if (A > B)
                    return 1;
                return 0;
            });

            var j, homologue;
            for (j = 0; j < homologues.length; j++) {
                homologue = homologues[j];
                homologueStr += "<a title='View this homologous gene in Gemma' href='" + Gemma.CONTEXT_PATH + "/gene/showGene.html?id="
                    + homologue.id
                    + "'>"
                    + homologue.officialSymbol
                    + "&nbsp;["
                    + homologue.taxonCommonName
                    + "]</a>&nbsp;&nbsp;&nbsp;";
            }
        } else {
            homologueStr = "None defined"; // or if not available...
        }
        return homologueStr;
    },

    /**
     * @private
     * @param geneSets
     * @returns {Array}
     */
    renderGeneSets: function (geneSets) {
        var geneSetLinks = [];
        if (geneSets != null && geneSets.length > 0) {
            geneSets.sort(function (a, b) {
                var A = a.name.toLowerCase();
                var B = b.name.toLowerCase();
                if (A < B)
                    return -1;
                if (A > B)
                    return 1;
                return 0;
            });

            for (var i = 0; i < geneSets.length; i++) {
                if (geneSets[i] && geneSets[i].name && geneSets[i].id) {
                    geneSetLinks.push('<a target="_blank" href="' + Gemma.CONTEXT_PATH + '/geneSet/showGeneSet.html?id=' + geneSets[i].id
                        + '">' + geneSets[i].name + '</a>');
                }
            }
        } else {
            geneSetLinks.push('Not currently a member of any gene group');
        }
        return geneSetLinks;
    },

    /**
     * @private
     * @param geneDetails
     * @returns {String}
     */
    renderMultifunctionality: function (geneDetails) {

        var text;
        if (geneDetails.multifunctionalityRank) {
            text = geneDetails.numGoTerms + " GO Terms; Overall multifunctionality "
                + geneDetails.multifunctionalityRank.toFixed(2);
            // text += "&nbsp;<img style='cursor:pointer' src='" + Gemma.CONTEXT_PATH + "/images/magnifier.png' ext:qtip='View the GO term tab'"
            //     + "onClick='Ext.getCmp(&#39;" + this.id + "&#39;).changeTab(&#39;goGrid&#39;)'>";
        } else {
            text = "[ Not available ]";
        }

        return new Ext.Panel({
            border: false,
            html: text,
            listeners: {
                'afterrender': function (c) {
                    window.jQuery('#multifuncHelp').qtip({
                        content: Gemma.HelpText.WidgetDefaults.GeneDetails.multifuncTT,
                        style: {
                            name: 'cream'
                        }
                    });
                }
            }
        });
    },

    /**
     * @memberOf Gemma.GeneDetailsTab
     * @private
     * @param geneDetails
     * @returns {String}
     */
    renderPhenotypes: function (geneDetails) {
        var text;
        if (geneDetails.phenotypes && geneDetails.phenotypes.length > 0) {
            var phenotypes = geneDetails.phenotypes;
            phenotypes.sort(function (a, b) {
                var A = a.value.toLowerCase();
                var B = b.value.toLowerCase();
                if (A < B)
                    return -1;
                if (A > B)
                    return 1;
                return 0;
            });
            var i = 0;
            var text = '';
            var limit = Math.min(3, phenotypes.length);
            for (i = 0; i < limit; i++) {
                text += '<a target="_blank" ext:qtip="View all genes for this phenotype" href="'
                    + Gemma.LinkRoots.phenotypePage + phenotypes[i].urlId + '">' + phenotypes[i].value + '</a>';
                if ((i + 1) !== limit) {
                    text += ', ';
                }
            }
            if (limit < phenotypes.length) {
                text += ', ' + (phenotypes.length - limit) + ' more';
            }
            text += "&nbsp;<img style='cursor:pointer' src='" + Gemma.CONTEXT_PATH + "/images/magnifier.png' ext:qtip='View the phenotype tab'"
                + "onClick='Ext.getCmp(&#39;" + this.id + "&#39;).changeTab(&#39;phenotypes&#39;)'>";

        } else {
            text = "[ None ]";
        }

        return new Ext.Panel({
            border: false,
            html: text,
            listeners: {
                'afterrender': function (c) {
                    window.jQuery('#phenotypeHelp').qtip({
                        content: Gemma.HelpText.WidgetDefaults.GeneDetails.phenotypeTT,
                        style: {
                            name: 'cream'
                        }
                    });
                }
            }
        });

    },

    changeTab: function (tabName) {
        this.fireEvent('changeTab', tabName);
    },

    /**
     * @private
     * @param geneDetails
     * @returns {Ext.Panel}
     */
    renderNodeDegree: function (geneDetails) {

        if (geneDetails.nodeDegreesPos && geneDetails.nodeDegreesPos.length > 1) {
            // Note: we need a panel here so we can pick up the rendering event so jquery can do its work.
            return new Ext.Panel(
                {
                    border: false,
                    html: '<span id="nodeDegreeSpark">...</span> Max support '
                    + (geneDetails.nodeDegreesPos.length - 1)
                    + "&nbsp;<img style='cursor:pointer' src='" + Gemma.CONTEXT_PATH + "/images/magnifier.png' ext:qtip='View the coexpression tab'"
                    + "onClick='Ext.getCmp(&#39;" + this.id + "&#39;).changeTab(&#39;coex&#39;)'>",
                    listeners: {
                        'afterrender': function (c) {
                            /*
                             * Compute cumulative counts
                             */
                            var cumul = [];
                            cumul[geneDetails.nodeDegreesPos.length - 1] = 0;
                            for (var j = geneDetails.nodeDegreesPos.length - 1; j >= 0; j--) {
                                cumul[j - 1] = geneDetails.nodeDegreesPos[j] + cumul[j];
                            }
                            cumul.pop();

                            var cumulNeg = [];
                            if (geneDetails.nodeDegreesNeg.length > 0) {
                                cumulNeg[geneDetails.nodeDegreesNeg.length - 1] = 0;
                                for (var j = geneDetails.nodeDegreesNeg.length - 1; j >= 0; j--) {
                                    cumulNeg[j - 1] = geneDetails.nodeDegreesNeg[j] + cumulNeg[j];
                                }
                                cumulNeg.pop();
                            }

                            /*
                             * Build array of arrays for plot
                             */
                            var nd = []; // support values
                            var ndr = []; // relative ranks

                            var max = -1;
                            for (var i = 0; i < cumul.length; i++) {
                                var v = Math.log(cumul[i] + 0.01) / Math.log(10.0);
                                nd.push([i + 1, v]);
                                ndr.push([i + 1, , geneDetails.nodeDegreePosRanks[i]]);
                                if (v > max) {
                                    max = v;
                                }
                            }

                            var ndneg = [];
                            var ndrneg = [];
                            for (var i = 0; i < cumulNeg.length; i++) {
                                var v = Math.log(cumulNeg[i] + 0.01) / Math.log(10.0);
                                ndneg.push([i + 1,]);
                                ndrneg.push([i + 1, geneDetails.nodeDegreeNegRanks[i]]);
                                if (v > max) {
                                    max = v;
                                }
                            }


                            jQuery('#nodeDegreeSpark').sparkline(
                                nd,
                                {
                                    height: 40,
                                    chartRangeMin: -1,
                                    chartRangeMax: max,
                                    width: 150,
                                    tooltipFormatter: function (spl, ops, fields) {
                                        if (fields.y) {
                                            return "Positive correlation links at support level " + fields.x
                                                + " or higher = " + Math.pow(10, fields.y).toFixed(0)
                                                + "  (Plot is log10 scaled)";
                                        } else {
                                            return "";
                                        }
                                    }
                                });

                            // plot negative links on same axis
                            if (cumulNeg.length > 0) {
                                jQuery('#nodeDegreeSpark').sparkline(
                                    ndneg,
                                    {
                                        composite: true,
                                        height: 40,
                                        chartRangeMin: -1,
                                        chartRangeMax: max,
                                        width: 150,
                                        tooltipFormatter: function (spl, ops, fields) {
                                            if (fields.y) {
                                                return " Negative correlation links at support level " + fields.x
                                                    + " or higher = " + Math.pow(10, fields.y).toFixed(0)
                                                    + "  (Plot is log10 scaled)";
                                            } else {
                                                return "";
                                            }
                                        }
                                    });
                            }
                            window.jQuery("#nodeDegreeHelp").qtip({
                                content: Gemma.HelpText.WidgetDefaults.GeneDetails.nodeDegreeTT,
                                style: {
                                    name: 'cream'
                                }
                            });

                        }
                    }
                });
        } else {
            // unavailable; show help anyway.
            return new Ext.Panel({
                border: false,
                html: "[ Not available ]",
                listeners: {
                    'afterrender': function (c) {
                        window.jQuery("#nodeDegreeHelp").qtip({
                            content: Gemma.HelpText.WidgetDefaults.GeneDetails.nodeDegreeTT,
                            style: {
                                name: 'cream'
                            }
                        });
                    }
                }
            });
        }
    },

    /**
     * @private
     * @param aliases
     * @returns
     */
    renderAliases: function (aliases) {
        if (aliases != null && aliases.length > 0) {
            aliases.sort();
            return aliases.join(', ');
        }
        return 'None available';
    },

    initComponent: function () {
        Gemma.GeneDetails.superclass.initComponent.call(this);

        // need to do this on render so we can show a load mask
        this.on('afterrender', function () {
            if (!this.loadMask && this.getEl()) {
                this.loadMask = new Ext.LoadMask(this.getEl(), {
                    msg: Gemma.StatusText.Loading.generic,
                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                });
            }
            this.loadMask.show();

            GeneController.loadGeneDetails(this.geneId, function (geneDetails) {
                this.loadMask.hide();
                this.add([
                    {
                        html: '<div style="font-weight: bold;">'
                        + geneDetails.name
                        + '<br />'
                        + geneDetails.officialName
                        + '&nbsp;&nbsp;<a target="_blank" '
                        + 'href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids='
                        + geneDetails.ncbiId
                        + '"><img ext:qtip="View NCBI record in a new window" alt="NCBI Gene Link" src="' + Gemma.CONTEXT_PATH + '/images/logo/ncbi.gif"/></a>'
                        + '<br/></div>'

                    },
                    {
                        layout: 'form',
                        labelWidth: 105,
                        labelAlign: 'right',
                        labelSeparator: ':',
                        labelStyle: 'font-weight:bold;',
                        flex: 1,
                        defaults: {
                            border: false
                        },

                        items: [
                            {
                                fieldLabel: 'Taxon',
                                html: geneDetails.taxonCommonName
                            },
                            {
                                fieldLabel: 'Aliases',
                                html: this.renderAliases(geneDetails.aliases)
                            },

                            {
                                fieldLabel: 'Homologues',
                                html: this.renderHomologues(geneDetails.homologues,
                                    geneDetails.name)
                            },
                            {
                                fieldLabel: 'Gene Groups',
                                html: this.renderGeneSets(geneDetails.geneSets).join(', ')
                            },
                            {
                                fieldLabel: 'Functions'
                                + '&nbsp;<i id="multifuncHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                items: this.renderMultifunctionality(geneDetails)
                            },
                            /*
                            {
                                fieldLabel: 'Coexpression'
                                + '&nbsp;<i id="nodeDegreeHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                items: this.renderNodeDegree(geneDetails)
                            },


                            {
                                fieldLabel: 'Phenotypes &nbsp;<i id="phenotypeHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                items: this.renderPhenotypes(geneDetails),
                                hidden: !(geneDetails.taxonId == 1 || geneDetails.taxonId == 2
                                    || geneDetails.taxonId == 3 || geneDetails.taxonId == 13 || geneDetails.taxonId == 14)
                            },
                             */
                            {
                                fieldLabel: 'Elements'
                                + '&nbsp; <i id="elementsHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                items: new Ext.Panel(
                                    {
                                        border: false,
                                        html: geneDetails.compositeSequenceCount
                                        + " on "
                                        + geneDetails.platformCount
                                        + " different platforms&nbsp;"
                                        + "&nbsp;<img style='cursor:pointer' src='" + Gemma.CONTEXT_PATH + "/images/magnifier.png' ext:qtip='View all the elements for this gene'"
                                        + "onClick='Ext.getCmp(&#39;" + this.id
                                        + "&#39;).changeTab(&#39;elements&#39;)'>",
                                        listeners: {
                                            'afterrender': function (c) {
                                                window.jQuery('#elementsHelp')
                                                    .qtip(
                                                        {
                                                            content: Gemma.HelpText.WidgetDefaults.GeneDetails.probesTT,
                                                            style: {
                                                                name: 'cream'
                                                            }
                                                        });
                                            }
                                        }
                                    })
                            }
                        ]
                    }]);
                this.syncSize();
            }.createDelegate(this));
        });
    }
});