Ext.namespace('Gemma', 'Gemma.PlatformDetails');
/**
 * Need to set platformId as config.
 * <p>
 * Note: 'edit' functionality not implemented here.
 *
 * @memberOf Gemma
 */
Gemma.PlatformDetails = Ext
    .extend(
        Ext.Panel,
        {
            padding: 10,
            defaults: {
                border: false
                // flex : 0
            },
            // layoutConfig : {
            // align : 'stretch'
            // },
            // layout : 'vbox',

            changeTab: function (tabName) {
                this.fireEvent('changeTab', tabName);
            },

            renderTaxon: function (platformDetails) {
                return new Ext.Panel({
                    border: false,
                    html: platformDetails.taxon,
                    listeners: {
                        'afterrender': function (c) {
                            window.jQuery('#taxonHelp').qtip({
                                content: "The primary taxon for sequences on this platform (i.e., what it was designed for).",
                                style: {
                                    name: 'cream'
                                }
                            });
                        }
                    }
                });
            },

            /**
             * @memberOf Gemma.PlatformDetails
             */
            renderMerged: function (pd) {
                var text = '';

                if (pd.isAffymetrixAltCdf) {
                    text = text + '<i class="orange fa fa-exclamation-circle fa-lg" ></i>&nbsp;'
                        + "This platform is an alternative to a 'standard' " + "gene-level Affymetrix probe layout: "
                        + Gemma.arrayDesignLink(pd.alternative) + ". Data sets using it will be switched to the "
                        + "canonical one when raw data are available. We do not provide annotation files for this platform.";
                    text = text + "<br />";
                }

                if (pd.merger != null) {
                    text = text + "Merged into: " + Gemma.arrayDesignLink(pd.merger);
                    text = text + "<br />";
                }

                if (pd.mergees != null && pd.mergees.length > 0) {
                    text = text + "Merges: ";
                    for (var i = 0; i < pd.mergees.length; i++) {
                        var s = pd.mergees[i];
                        text = text + Gemma.arrayDesignLink(s) + "&nbsp;";
                    }
                    text = text + "<br />";
                }

                if (pd.subsumer != null) {
                    text = text + "Subsumed by: " + Gemma.arrayDesignLink(pd.subsumer);
                    text = text + "<br />";
                }

                if (pd.subsumees != null && pd.subsumees.length > 0) {
                    text = text + "Subsumes: ";
                    for (var i = 0; i < pd.subsumees.length; i++) {
                        var s = pd.subsumees[i];
                        text = text + Gemma.arrayDesignLink(s) + "&nbsp;";
                    }
                    text = text + "<br />";
                }

                if (pd.switchedExpressionExperimentCount > 0 ) { //&& Ext.get("hasAdmin").getValue() == 'true'
                    text = text + '<br/><i style="color:#3366cc" class="fa fa-exclamation-circle fa-lg"></i>&nbsp;'
                        +  pd.switchedExpressionExperimentCount + " experiments "
                        + " were switched from this platform" + '<br/>';
                }

                if (text === "") {
                    text = "(None)";
                }

                return new Ext.Panel({
                    border: false,
                    padding: 5,
                    html: text,
                    listeners: {
                        'afterrender': Gemma.helpTip("#mergedHelp",
                            "Relationship this platform has with others, such as merging. "
                            + "Platforms are merged in part to accomodate experiments in"
                            + " which different related platforms are used for different samples. "
                            + "Subsumption means that all the sequences contained on a platform "
                            + "are represented on another "
                            + "(but is only computed for selected platforms as a precursor to merging)."
                            + " In other cases experiments are switched to a different platform after loading.")
                    }
                });
            },

            renderReport: function (platformDetails) {

                var text = '';

                var updateT = '';
                var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
                if (isAdmin) {
                    updateT = '&nbsp;<input type="button" value="Refresh report" onClick="Gemma.ArrayDesign.updateArrayDesignReport('
                        + platformDetails.id + ')" />';
                }

                if (platformDetails.technologyType == 'SEQUENCING' || platformDetails.alternative) {
                    text = "Not supported for this platform type";
                } else {
                    text = {
                        tag: "ul",
                        style: 'padding:8px;background:#DFDFDF;width:400px',
                        id: platformDetails.id + "_report",
                        children: [

                            {
                                tag: 'li',
                                html: 'Elements: ' + platformDetails.designElementCount,
                                style: platformDetails.dateCached == null ? 'display:none' : ''
                            },
                            {
                                tag: 'li',
                                html: 'With sequence: '
                                + platformDetails.numProbeSequences
                                + '&nbsp;<span style="font-size:smaller;color:grey">(Number of elements with sequences)</span>',
                                style: platformDetails.numProbeSequences == null
                                || platformDetails.technologyType == "SEQUENCING" || platformDetails.technologyType == 'GENELIST' ? 'display:none' : ''
                            },
                            {
                                tag: 'li',
                                html: 'With alignments: '
                                + platformDetails.numProbeAlignments
                                + '&nbsp;<span style="font-size:smaller;color:grey">(Number of elements with at least one genome alignment)</span>',
                                style: platformDetails.numProbeAlignments == null
                                || platformDetails.technologyType == "SEQUENCING" || platformDetails.technologyType == 'GENELIST' ? 'display:none' : ''
                            },
                            {
                                tag: 'li',
                                html: 'Mapped to genes: '
                                + platformDetails.numProbesToGenes
                                + '&nbsp;<span style="font-size:smaller;color:grey">(Number of elements mapped to at least one gene)</span>',
                                style: platformDetails.numProbesToGenes === null || platformDetails.technologyType == 'GENELIST' ? 'display:none' : ''
                            },
                            {
                                tag: 'li',
                                html: 'Unique genes: '
                                + platformDetails.numGenes
                                + '&nbsp;<span style="font-size:smaller;color:grey">(Number of distinct genes represented on the platform)</span>',
                                style: platformDetails.numGenes === null ? 'display:none' : ''
                            },
                            {
                                tag: 'li',
                                html: (platformDetails.dateCached != null ? 'As of ' + platformDetails.dateCached
                                    + "&nbsp;" : 'No report available')
                                + updateT
                            }]
                    };
                }

                return new Ext.Panel({
                    border: false,
                    html: text,
                    listeners: {
                        'afterrender': Gemma.helpTip("#reportHelp",
                            "Platform elements in Gemma are mapped to genes either using sequence analysis (most array-based platforms)"
                            + " or directly (gene list platforms used for RNA-seq and for some model organisms)")
                    }
                });
            },

            renderAnnotationFileLinks: function (platformDetails) {
                var text = '';

                if (platformDetails.troubled) {
                    text += 'Not supported (troubled)';
                } else if (platformDetails.isAffymetrixAltCdf) {
                    text += 'Not supported (Affymetrix alt)';
                } else if (platformDetails.technologyType == 'SEQUENCING') {
                    text += 'Not supported (raw sequencing platform; see corresponding "Generic" gene list platforms for this taxon)';
                } else {
                    if (platformDetails.noParentsAnnotationLink)
                        text += '<a ext:qtip="Recommended version for ermineJ" class="annotationLink" href="'
                            + platformDetails.noParentsAnnotationLink + '" >Basic</a>&nbsp;&nbsp;';
                    if (platformDetails.allParentsAnnotationLink)
                        text += '<a class="annotationLink" href="' + platformDetails.allParentsAnnotationLink
                            + '" >All terms</a>&nbsp;&nbsp;';
                    if (platformDetails.bioProcessAnnotationLink)
                        text += '<a ext:qtip="Biological process terms only" class="annotationLink" href="'
                            + platformDetails.bioProcessAnnotationLink + '" >BP only</a>';
                }

                return new Ext.Panel(
                    {
                        border: false,
                        html: text,
                        listeners: {
                            'afterrender': Gemma
                                .helpTip(
                                    "#annotationHelp",
                                    "<p>Download annotation files for this platforms, when supported. The files include gene information as well as "
                                    + "GO terms and are compatible with ermineJ. "
                                    + "Up to three versions of GO annotations are provided:"
                                    + "<p>The 'Basic' version includes only directly annotated terms. "
                                    + "<p>The 'All terms' version includes inferred terms based on propagation in the term hierarchy. "
                                    + "<p>'BP only' includes only terms from the biological process ontology. "
                                    + "<p>For ermineJ and uses where GO annotations are not needed, "
                                    + "we recommend using the 'basic' one as it is the smallest and the other information can be inferred. ")
                        }
                    });
            },

            renderExternalAccesions: function (platformDetails) {

                var text = "";

                var er = platformDetails.externalReferences;
                if (er == null || er.length == 0) {
                    text = "None";
                } else {

                    for (var i = 0; i < er.length; i++) {

                        var dbr = er[i];
                        var ac = dbr.accession;

                        var db = dbr.externalDatabase.name;

                        if (db == "GEO") {
                            text = text + ac + "&nbsp;<a "
                                + " target='_blank' href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" + ac
                                + "'><img  ext:qtip='NCBI page for this entry' src='" + Gemma.CONTEXT_PATH
                                + "/images/logo/geoTiny.png' /></a>";
                        } else if (db == "ArrayExpress") {
                            text = text
                                + ac
                                + "&nbsp;<a title='ArrayExpress page for this entry'"
                                + " target='_blank' href='http://www.ebi.ac.uk/microarray-as/aer/result?queryFor=Experiment&eAccession="
                                + ac + "'><img  ext:qtip='NCBI page for this entry' src='" + Gemma.CONTEXT_PATH
                                + "/images/logo/arrayExpressTiny.png' /></a>";

                        } else {
                            text = text + "&nbsp;" + ac + " (" + databaseEntry.getExternalDatabase().getName() + ")";
                        }

                    }
                }

                return new Ext.Panel({
                    border: false,
                    html: text,
                    listeners: {
                        'afterrender': Gemma.helpTip("#sourcesHelp", "Identifiers in other systems, if any")
                    }
                });
            },

            promptForAlternateName: function (id) {
                var dialog = new Ext.Window({
                    title: "Enter a new alternate name",
                    modal: true,
                    layout: 'fit',
                    autoHeight: true,
                    width: 300,
                    closeAction: 'hide',
                    easing: 3,
                    defaultType: 'textfield',
                    items: [{
                        id: "alternate-name-textfield",
                        fieldLabel: 'Name',
                        name: 'name',
                        listeners: {
                            afterrender: function (field) {
                                field.focus();
                            }
                        }
                    }],

                    buttons: [{
                        text: 'Cancel',
                        handler: function (button) {
                            button.ownerCt.ownerCt.hide();
                        }
                    }, {
                        text: 'Save',
                        handler: function (button) {
                            var name = Ext.get("alternate-name-textfield").getValue();
                            this.addAlternateName(id, name);
                            button.ownerCt.ownerCt.hide();
                        },
                        scope: this
                    }]

                });

                dialog.show();

            },

            addAlternateName: function (id, newName) {

                var callParams = [];

                callParams.push(id, newName);

                var delegate = function (data) {
                    if (Ext.get("alternate-names") !== null) {
                        Ext.DomHelper.overwrite("alternate-names", data);
                    }
                };
                var errorHandler = function (e, ex) {
                    Ext.Msg.alert("Error", er + "\n" + exception.stack);
                };

                callParams.push({
                    callback: delegate,
                    errorHandler: errorHandler
                });

                ArrayDesignController.addAlternateName.apply(this, callParams);

            },

            renderExperimentLink: function (platformDetails) {
                var text = platformDetails.expressionExperimentCount + "";
                if (platformDetails.expressionExperimentCount > 0) {
                    text += "&nbsp;<img style='cursor:pointer' src='" + Gemma.CONTEXT_PATH
                        + "/images/magnifier.png' ext:qtip='View the experiments tab'" + "onClick='Ext.getCmp(&#39;"
                        + this.id + "&#39;).changeTab(&#39;experiments&#39;)'>";
                }
                return new Ext.Panel({
                    border: false,
                    html: text,
                    listeners: {
                        'afterrender': Gemma.helpTip("#experimentsHelp",
                            "How many experiments use this platform. Click the icon to access them")
                    }
                });
            },

            renderDescription: function (description) {
                return new Ext.Panel({
                    border: false,
                    html: '<div class="clob">' + description + "</div>",
                    listeners: {
                        'afterrender': Gemma.helpTip("#descriptionHelp",
                            "The description includes that obtained from the data provider (i.e., GEO)"
                            + " but may include additional information added by Gemma.")
                    }
                });
            },

            renderStatus: function (platformDetails) {
                var text = '';

                if (platformDetails.troubled) {
                    text = '<i class="red fa fa-exclamation-triangle fa-lg" ext:qtip="' + platformDetails.troubleDetails
                        + '"></i> Unusable';
                    if (platformDetails.blackListed) {
                        text = text + '&nbsp;&nbsp;<i class="black fa fa-exclamation-triangle fa-lg"></i>Blacklisted';
                    }
                } else {
                    text = 'Usable. ';
                }

                if (platformDetails.technologyType == 'SEQUENCING') {
                    text = text + 'This platform is only used as a placeholder prior to renalysis of raw sequencing data';
                }


                return new Ext.Panel({
                    border: false,
                    html: text,
                    listeners: {
                        'afterrender': Gemma.helpTip("#statusHelp",
                            "Information on curation status, whether the platform is usable, etc.")
                    }
                });
            },

            renderElementsLink: function (platformDetails) {
                var text = '';
                if (platformDetails.technologyType == 'SEQUENCING') {
                    text = 'Raw sequencing platform; no elements defined';
                } else {
                    var text = platformDetails.designElementCount;
                    text += "&nbsp;<img style='cursor:pointer' src='" + Gemma.CONTEXT_PATH
                        + "/images/magnifier.png' ext:qtip='View the elements tab'" + "onClick='Ext.getCmp(&#39;" + this.id
                        + "&#39;).changeTab(&#39;elements&#39;)'>";
                }

                return new Ext.Panel({
                    border: false,
                    html: text,
                    listeners: {
                        'afterrender': Gemma.helpTip("#numElementsHelp",
                            "How many elements (e.g. probes) the platform has. Click the icon to view in details (when available)")
                    }
                });
            },

            renderAlternateNames: function (platformDetails) {

                var text = '<span id="alternate-names">'
                    + (platformDetails.alternateNames.length > 0 ? platformDetails.alternateNames : '') + '</span>';

                var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
                if (isAdmin) {
                    text = text + '&nbsp;<img  style="cursor:pointer" onClick="Ext.getCmp(&#39;' + this.id
                        + '&#39;).promptForAlternateName(' + platformDetails.id
                        + ');return false;" ext:qtip="Add a new alternate name for this design" src="' + Gemma.CONTEXT_PATH
                        + '/images/icons/add.png" />';
                }
                return new Ext.Panel({
                    border: false,
                    html: text,
                    listeners: {
                        'afterrender': Gemma.helpTip("#aliasHelp", "Other names used for this platform")
                    }
                });
            },

            renderType: function (platformDetails) {
                return new Ext.Panel({
                    border: false,
                    html: platformDetails.colorString,
                    listeners: {
                        'afterrender': Gemma.helpTip("#typeHelp",
                            "Array platforms are one-color, two-color, or dual mode if they can be used either way."
                            + " 'Gene list' indicates a generic platform based on the taxon's genome annotations.")
                    }
                });
            },

            initComponent: function () {
                Gemma.PlatformDetails.superclass.initComponent.call(this);

                // need to do this on render so we can show a load mask
                this
                    .on(
                        'afterrender',
                        function () {
                            if (!this.loadMask && this.getEl()) {
                                this.loadMask = new Ext.LoadMask(this.getEl(), {
                                    msg: Gemma.StatusText.Loading.generic,
                                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                                });
                            }
                            this.loadMask.show();

                            ArrayDesignController
                                .getDetails(
                                    this.platformId,
                                    {
                                        callback: function (platformDetails) {
                                            // console.log( platformDetails );
                                            this.loadMask.hide();
                                            this
                                                .add([
                                                    {
                                                        html: '<div style="font-weight: bold; font-size:1.2em;">'
                                                        + platformDetails.shortName + '<br />' + platformDetails.name
                                                        + '</div>'

                                                    },
                                                    {
                                                        layout: 'form',
                                                        labelWidth: 140,
                                                        labelAlign: 'right',
                                                        labelSeparator: ':',
                                                        labelStyle: 'font-weight:bold;',
                                                        flex: 1,
                                                        id: platformDetails.id + '_features',
                                                        defaults: {
                                                            border: false
                                                        },
                                                        // have an ArrayDesignValueObjectExt
                                                        items: [
                                                            {
                                                                fieldLabel: "Aliases "
                                                                + '&nbsp;<i id="aliasHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderAlternateNames(platformDetails)
                                                            },
                                                            {
                                                                fieldLabel: 'Taxon'
                                                                + '&nbsp<i id="taxonHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderTaxon(platformDetails)
                                                            },
                                                            {
                                                                fieldLabel: 'Platform type'
                                                                + '&nbsp<i id="typeHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderType(platformDetails)
                                                            },
                                                            {
                                                                fieldLabel: 'Number of elements'
                                                                + '&nbsp<i id="numElementsHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderElementsLink(platformDetails)
                                                            },
                                                            {
                                                                fieldLabel: 'Number of datasets'
                                                                + '&nbsp<i id="experimentsHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderExperimentLink(platformDetails)
                                                            },
                                                            {
                                                                fieldLabel: 'Description'
                                                                + '&nbsp<i id="descriptionHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderDescription(platformDetails.description)
                                                            },
                                                            {
                                                                fieldLabel: 'Sources'
                                                                + '&nbsp<i id="sourcesHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderExternalAccesions(platformDetails)
                                                            },
                                                            {
                                                                fieldLabel: 'Relationships'
                                                                + '&nbsp<i id="mergedHelp"  class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderMerged(platformDetails)
                                                            },
                                                            {
                                                                fieldLabel: 'Status'
                                                                + '&nbsp<i id="statusHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                                                items: this.renderStatus(platformDetails)
                                                            }]
                                                        /*
                                                           * Edit button?
                                                           */
                                                    }]);

                                            // add the last two items. could use display:none instead.
                                            Ext
                                                .getCmp(platformDetails.id + '_features')
                                                .add(
                                                    {
                                                        fieldLabel: 'Annotation files'
                                                        + '&nbsp<i id="annotationHelp"  class="qtp fa fa-question-circle fa-fw"></i>',
                                                        items: this.renderAnnotationFileLinks(platformDetails)
                                                    });

                                            Ext.getCmp(platformDetails.id + '_features').add(
                                                {
                                                    fieldLabel: 'Gene map summary'
                                                    + '&nbsp<i id="reportHelp"  class="qtp fa fa-question-circle fa-fw"></i>',
                                                    items: this.renderReport(platformDetails)
                                                });

                                            this.syncSize();
                                        }.createDelegate(this),
                                        errorHandler: function (er, exception) {
                                            Ext.Msg.alert("Error", er + "\n" + exception.stack);
                                            // console.log( exception.stack );
                                        }
                                    });
                        });
            }

        });
