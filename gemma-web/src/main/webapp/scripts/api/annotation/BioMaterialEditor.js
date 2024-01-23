Ext.namespace('Gemma');

String.prototype.hashCode = function() {
    var hash = 0, i, chr;
    if (this.length === 0) return hash;
    for (i = 0; i < this.length; i++) {
        chr   = this.charCodeAt(i);
        hash  = ((hash << 5) - hash) + chr;
        hash |= 0; // Convert to 32bit integer
    }
    return "" + Math.abs(hash) + "a";
};

Gemma.BioMaterialEditor = function (config) {
    this.originalConfig = config;
    this.expressionExperiment = {
        id: config.eeId,
        classDelegatingFor: "ExpressionExperiment"
    };

    Gemma.BioMaterialEditor.superclass.constructor.call(this, config);
};

/**
 * Grid with list of biomaterials for editing experimental design parameters.
 */
Ext.extend(Gemma.BioMaterialEditor, Ext.Panel, {

    firstInitDone: false,

    /*
     * We make two ajax calls; the first gets the biomaterials, the second gets the experimentalfactors. These are run in
     * succession so both values can be given to the BioMaterialGrid constructor. We could make a method that gets them
     * both at once...
     */
    firstCallback: function (data) {

        // second ajax call.
        ExperimentalDesignController.getExperimentalFactors(this.expressionExperiment, function (factorData) {
            config = {
                factors: factorData,
                bioMaterials: data
            };
            Ext.apply(config, this.originalConfig);

            this.grid = new Gemma.BioMaterialGrid(config);
            this.grid.init = this.init.createDelegate(this);

            this.add(this.grid);

            this.loadMask.hide();

            this.doLayout(false, true);

            this.firstInitDone = true;

        }.createDelegate(this));
    },

    /**
     * Gets called on startup but also when a refresh is needed.
     *
     * @memberOf Gemma.BioMaterialEditor
     */
    init: function () {
        var loadMaskTarget = this.el !== null ? this.el : Ext.getBody();

        this.loadMask = new Ext.LoadMask(loadMaskTarget, {
            msg: Gemma.StatusText.waiting
        });

        this.loadMask.show();
        ExperimentalDesignController
            .getBioMaterials(this.expressionExperiment, this.firstCallback.createDelegate(this));
    }

});

Gemma.BioMaterialGrid = Ext.extend(Gemma.GemmaGridPanel, {

    loadMask: true,
    autoExpandColumn: 'bm-column',
    fvMap: {}, // for columns of factors
    bmCharMap: {}, // for extra columns of biomaterial characteristics
    rowsExpanded: false,

    characteristics: {}, // ones we will display in columns

    /**
     * See ExperimentalDesignController.getExperimentalFactors and ExperimentalFactorValueObject AND
     * FactorValueValueObject to see layout of the object that is passed.
     *
     * @param factors,
     *           fetched with getExperimentalFactors.
     * @memberOf Gemma.BioMaterialGrid
     */
    createColumns: function (factors) {
        var columns = [this.rowExpander, {
            id: "bm-column",
            header: "BioMaterial",
            dataIndex: "bmName",
            sortable: true,
            width: 120,
            tooltip: 'BioMaterial (sample) name/details'
        }, {
            id: "ba-column",
            header: "BioAssay",
            width: 150,
            dataIndex: "baName",
            sortable: true,
            tooltip: 'BioAssay name/details'
        }, {
            id: "date-column",
            header: "BA Date",
            width: 40,
            dataIndex: "baDate",
            sortable: true,
            tooltip: 'BioAssay processing date (primarily available for microarrays only)'
        }];

        this.factorValueEditors = [];

        /*
         * sort by id to give consistency.
         */
        factors.sort(function (a, b) {
            return a.id - b.id;
        });

        for (var i = 0; i < factors.length; i++) {
            var factor = factors[i];
            var factorId = "factor" + factor.id;

            var editor;
            var continuous = factor.type == "continuous";
            if (continuous) {

                editor = new Ext.form.NumberField({
                    id: factorId + '-valueeditor',
                    lazyInit: false,
                    lazyRender: true,
                    record: this.fvRecord,
                    continuous: continuous, // might be useful.
                    data: factor.values
                });
            } else {

                /*
                 * Create one factorValueCombo per factor. It contains all the factor values.
                 */
                editor = new Gemma.FactorValueCombo({
                    id: factorId + '-valueeditor',
                    lazyInit: false,
                    lazyRender: true,
                    record: this.fvRecord,
                    continuous: continuous,
                    data: factor.values
                });

                // console.log("Categorical");
            }

            this.factorValueEditors[factorId] = editor;

            // factorValueValueObjects
            if (factor.values) {
                for (var j = 0; j < factor.values.length; j++) {
                    fv = factor.values[j];
                    var fvs = fv.factorValue; // descriptive string formed on server side.
                    this.fvMap["fv" + fv.id] = fvs;
                }
            }

            /*
             * Generate a function to render the factor values as displayed in the cells. At this point factorValue
             * contains all the possible values for this factor.
             */
            var rend = null;

            // if (!continuous) {
            rend = this.createValueRenderer();
            // }

            /*
             * Define the column for this particular factor.
             */
            var ue = null;
            if (this.editable) {
                ue = editor;
            }

            // text used for header of the column.
            var label = factor.description ? factor.description : factor.name
                + (factor.name === factor.description || factor.description === "" ? "" : " (" + factor.description
                    + ")");

            columns.push({
                id: factorId,
                header: label,
                dataIndex: factorId,
                renderer: rend,
                editor: ue,
                width: 120,
                tooltip: label,
                sortable: true,
                continuous: continuous
            });

        }

        /*
        Create columns for the biomaterial characteristics we want to display.
         */
        for (var category in this.characteristics) {
            if (!this.characteristics.hasOwnProperty(category)) {
                continue;
            }

            columns.push({
                id: "char" + category.hashCode(),
                header: category + " (raw characteristic)",
                dataIndex: "char" + category.hashCode(),
                width: 120,
                tooltip: category + ": A non-constant Biomaterial characteristic displayed for reference purposes.",
                sortable: true
            });
        }

        return columns;
    },

    /**
     * See ExperimentalDesignController.getBioMaterials BioMaterialValueObject to see layout of the object that is
     * passed. *
     *
     * @param biomaterial
     *           A template so we know how the records will be laid out.
     */
    createRecord: function () {

        var fields = [{
            name: "id",
            type: "int"
        }, {
            name: "bmName",
            type: "string"
        }, {
            name: "bmDesc",
            type: "string"
        }, {
            name: "bmChars",
            type: "string"
        }, {
            name: "baName",
            type: "string"
        }, {
            name: "baDesc",
            type: "string"
        }, {
            name: "baDate",
            type: "string"
        }];

        // Add one slot in the record per factor. The name of the fields will be like
        // 'factor428' to ensure uniqueness. This must be used as the dataIndex for the columnModel.
        if (this.factors) {
            for (var i = 0; i < this.factors.length; i++) {
                var factor = this.factors[i];
                var o = {
                    name: "factor" + factor.id, // used to access this later
                    type: "string"
                };
                fields.push(o);
            }
        }

        // Add one slot in the record per biomaterial characteristic.
        // just look at one biomaterial as a representative.
        var bm = this.bioMaterials[0];
        if (bm.characteristicValues) {
            this.characteristics = bm.characteristicValues;
            for (var category in bm.characteristicValues) {
                if (!bm.characteristicValues.hasOwnProperty(category)) {
                    continue;
                }
                var o = {
                    name: "char" + category.hashCode(),
                    type: "string"
                };
                fields.push(o);
            }
        }

        var record = Ext.data.Record.create(fields);
        return record;
    },

    initComponent: function () {

        this.record = this.createRecord();

        var data = this.transformData();

        Ext.apply(this, {
            plugins: this.rowExpander,
            store: new Ext.data.Store({
                proxy: new Ext.data.MemoryProxy(data),
                reader: new Ext.data.ArrayReader({}, this.record)
            })
        });

        // must be done separately.
        Ext.apply(this, {
            columns: this.createColumns(this.factors)
        });

        /*
         * Always show the toolbar, for regular user functions like toggleExpand
         */
        this.tbar = new Gemma.BioMaterialToolbar({
            edId: this.edId,
            editable: this.editable
        });

        Gemma.BioMaterialGrid.superclass.initComponent.call(this);

        /*
         * Event handlers for toolbar buttons.
         *
         */
        this.getTopToolbar().on("toggleExpand", function () {
            if (this.rowsExpanded) {
                this.rowExpander.collapseAll();
                this.getTopToolbar().expandButton.setText("Expand all");
                this.rowsExpanded = false;
            } else {
                this.rowExpander.expandAll();
                this.getTopToolbar().expandButton.setText("Collapse all");
                this.rowsExpanded = true;
            }

        }, this);

        this.getTopToolbar().on(
            "refresh",
            function () {
                if (this.store.getModifiedRecords().length > 0) {
                    Ext.Msg.confirm(Gemma.HelpText.CommonWarnings.LoseChanges.title,
                        Gemma.HelpText.CommonWarnings.LoseChanges.text,

                        function (but) {
                            if (but == 'yes') {
                                this.init();
                            }
                        }.createDelegate(this));
                } else {
                    this.init();
                }

            }, this);

        this.getTopToolbar().on("filter", function (text) {
            this.searchForText(text);
        }, this);

        if (this.editable) {

            /**
             * Editing of a specific record fires this.
             */
            this.on("afteredit", function (e) {
                var factorId = this.getColumnModel().getColumnId(e.column);
                var editor = this.factorValueEditors[factorId];

                if (editor.continuous) {
                    // e.record.set(factorId, editor.value); // use the value, not the id
                } else {
                    var fvvo = editor.getFactorValue();
                    e.record.set(factorId, fvvo.id);
                }

                // if (e.originalValue != e.value) {
                this.getTopToolbar().saveButton.enable();
                this.getView().refresh();
                // }

            }, this);

            /**
             * Bulk update biomaterial -> factorvalue associations (must click save to persist)
             */
            this.getTopToolbar().on("apply", function (factor, factorValue) {
                var selected = this.getSelectionModel().getSelections();
                for (var i = 0; i < selected.length; ++i) {
                    selected[i].set(factor, factorValue);
                }
                this.getView().refresh();
            }, this);

            /**
             * Save edited records to the db.
             */
            this.getTopToolbar().on("save", function () {
                // console.log("Saving ...");
                this.loadMask.show();
                var edited = this.getEditedRecords();
                var bmvos = [];
                for (var i = 0; i < edited.length; ++i) {
                    var row = edited[i];
                    var bmvo = {
                        id: row.id,
                        factorIdToFactorValueId: {},
                        characteristicValues: {} // we don't edit this.
                    };

                    for (var j in row) {
                        if (typeof j == 'string' && j.indexOf("factor") >= 0) {
                            // console.log(j + "...." + row[j]);
                            bmvo.factorIdToFactorValueId[j] = row[j];
                        }
                    }
                    bmvos.push(bmvo);
                }

                /*
                 * When we return from the server, reload the factor values.
                 */
                var callback = this.init; // check

                ExperimentalDesignController.updateBioMaterials(bmvos, callback);
            }.createDelegate(this), this);

            this.getSelectionModel().on("selectionchange", function (model) {
                var selected = model.getSelections();
                this.getTopToolbar().revertButton.disable();
                for (var i = 0; i < selected.length; ++i) {
                    if (selected[i].dirty) {
                        this.getTopToolbar().revertButton.enable();
                        break;
                    }
                }
            }.createDelegate(this), this);

            this.getSelectionModel().on("selectionchange", function (model) {
                this.enableApplyOnSelect(model);
            }.createDelegate(this.getTopToolbar()), this.getTopToolbar());

            this.getTopToolbar().on("undo", this.revertSelected, this);
        }

        this.getStore().load({
            params: {},
            callback: function () {
                this.getStore().sort("bmName");
                this.getStore().fireEvent("datachanged");
            },
            scope: this
        });
    },

    /**
     * Turn the incoming biomaterial valueObjects into an array structure that can be loaded into an ArrayReader.
     */
    transformData: function (incoming) {
        var data = [];
        for (var i = 0; i < this.bioMaterials.length; ++i) {
            var bmvo = this.bioMaterials[i];

            // format for display.
            var chars = "";
            bmvo.characteristics.forEach(function (element) {
                if (!element.category) {
                    chars += "[No category] = " + element.value;
                } else {
                    chars += element.category + "  = " + element.value;
                }
                chars += "<br/>";
            });

            /*
             * This order must match the record!
             */
            data[i] = [bmvo.id, bmvo.name, bmvo.description, chars, bmvo.assayName,
                bmvo.assayDescription, Gemma.Renderers.dateRenderer(bmvo.assayProcessingDate)];

            var factors = bmvo.factors;

            /*
             * Use this to keep the order the same as the record.
             */
            for (var j = 0; j < this.factors.length; j++) {
                var factor = this.factors[j];
                var factorId = "factor" + factor.id;
                var k = bmvo.factorIdToFactorValueId[factorId];
                if (k) {
                    data[i].push(k);
                } else {
                    data[i].push(""); // no value assigned.
                }
            }

            // same treatment for the characteristics.
            // note that we might have none.
            for (var c in this.characteristics) {
                if (!this.characteristics.hasOwnProperty(c)) {
                    continue;
                }
                var cval = bmvo.characteristicValues[c];
                if (cval) {
                    data[i].push(cval);
                } else {
                    data[i].push(""); // shouldn't happen if we picked useful characteristics well.
                }
            }

        }
        return data;
    },

    /**
     * Represents a FactorValueValueObject; used in the Store for the ComboBoxes.
     */
    fvRecord: Ext.data.Record.create([{
        name: "charId",
        type: "int"
    }, {
        name: "id",
        type: "string",
        convert: function (v) {
            return "fv" + v;
        }
    }, {
        name: "factor",
        type: 'string'
    }, {
        name: "category",
        type: "string"
    }, {
        name: "categoryUri",
        type: "string"
    }, {
        name: "value",
        type: "string"
    }, {
        name: "valueUri",
        type: "string"
    }, {
        name: "factorValue", // human-readable string
        type: "string"
    }]),

    reloadFactorValues: function () {
        for (var i in this.factorValueEditors) {
            var factorId = this.factorValueEditors[i];
            if (typeof factorId == 'string' && factorId.substring(0, 6) == "factor") {
                var editor = this.factorValueEditors[factorId];
                var column = this.getColumnModel().getColumnById(factorId);

                // this should not fire if it's a continuous variable; this is for combos.
                if (editor.setExperimentalFactor) {
                    editor.setExperimentalFactor(editor.experimentalFactor.id, function (r, options, success) {
                        this.fvMap = {};
                        for (var i = 0; i < r.length; ++i) {
                            var rec = r[i];
                            this.fvMap["fv" + rec.get("id")] = rec.get("factorValue");
                        }
                        var renderer = this.createValueRenderer();
                        column.renderer = renderer;
                        this.getView().refresh();
                    }.createDelegate(this));
                }
            }
        }
        this.getTopToolbar().factorValueCombo.store.reload();
    },

    createValueRenderer : function( ) {
        return function( value ) {
            if ( value === null || value === "" ) {
                return "<i>unassigned</i>";
            }
            if ( value in this.fvMap ) {
                return this.fvMap[value];
            } else {
                return value;
            }
        }.createDelegate( this );
    },

    rowExpander: new Ext.grid.RowExpander(
        {
            tpl: new Ext.Template(
                "<dl style='background-color:#EEE;padding:2px;margin-left:1em;margin-bottom:2px;'><dt>BioMaterial {bmName}</dt><dd>{bmDesc}<br>{bmChars}</dd>",
                "<dt>BioAssay {baName}</dt><dd>{baDesc}</dd></dl>")
        }),

    searchForText: function (text) {
        if (text.length < 1) {
            this.getStore().clearFilter();
            return;
        }
        this.getStore().filterBy(this.filter(text), this, 0);
    },

    filter: function (text) {
        var valueRegEx = new RegExp(Ext.escapeRe(text), 'i');
        var fvColumnRegEx = new RegExp(/^fv\d+$/);
        var columnArr = this.getColumnModel().config;

        return function (r, id) {
            var fields = r.fields;
            var found = false;
            var value;
            fields.each(function (item, index, length) {
                if (!found) {
                    value = r.get(item.name);
                    if (fvColumnRegEx.test(value)) {
                        value = (this.fvMap[value]) ? this.fvMap[value] : value;
                    }
                    if (item.name !== "id" && item.name !== "bmDesc"
                        && item.name !== "baDesc" && valueRegEx.test(value)) {
                        found = true;
                    }
                }
            }, this);
            return found;
        };
    }

});


Gemma.BioMaterialToolbar = Ext.extend(Ext.Toolbar, {

    /**
     * @memberOf Gemma.BioMaterialToolbar
     */
    initComponent: function () {

        this.items = [];
        if (this.editable) {

            this.saveButton = new Ext.Toolbar.Button({
                text: "Save",
                tooltip: "Save changed biomaterials",
                disabled: true,
                handler: function () {
                    this.fireEvent("save");
                    this.saveButton.disable();
                },
                scope: this
            });

            this.revertButton = new Ext.Toolbar.Button({
                text: "Undo",
                tooltip: "Undo changes to selected biomaterials",
                disabled: true,
                handler: function () {
                    this.fireEvent("undo");
                },
                scope: this
            });

            this.factorCombo = new Gemma.ExperimentalFactorCombo({
                width: 200,
                emptyText: "select a factor",
                edId: this.edId
            });

            this.factorCombo.on("select", function (combo, record, index) {

                /*
                 * FIXME, don't enable this if the factor is continuous.
                 */
                this.factorValueCombo.setExperimentalFactor(record.id);
                this.factorValueCombo.enable();
            }, this);

            this.factorValueCombo = new Gemma.FactorValueCombo({
                emptyText: "Select a factor value",
                disabled: true,
                width: 200
            });

            this.factorValueCombo.on("select", function (combo, record, index) {
                this.applyButton.enable();
            }, this);

            this.applyButton = new Ext.Toolbar.Button({
                text: "Apply",
                tooltip: "Apply this value to selected biomaterials",
                disabled: true,
                width: 100,
                handler: function () {
                    // console.log("Apply");
                    var factor = "factor" + this.factorCombo.getValue();
                    var factorValue = "fv" + this.factorValueCombo.getValue();
                    this.fireEvent("apply", factor, factorValue);
                    this.saveButton.enable();
                },
                scope: this
            });

            this.items = [this.saveButton, ' ', this.revertButton, '-', "Bulk changes:", ' ', this.factorCombo, ' ',
                this.factorValueCombo, this.applyButton];
        }

        var textFilter = new Ext.form.TextField({
            ref: 'searchInGrid',
            tabIndex: 1,
            enableKeyEvents: true,
            emptyText: 'Filter samples',
            listeners: {
                "keyup": {
                    fn: function (textField) {
                        this.fireEvent('filter', textField.getValue());
                    },
                    scope: this,
                    options: {
                        delay: 100
                    }
                }
            }
        });

        var refreshButton = new Ext.Toolbar.Button({
            text: "Refresh",
            tooltip: "Reload the data",
            handler: function () {
                this.fireEvent("refresh");
            }.createDelegate(this)

        });

        var expandButton = new Ext.Toolbar.Button({
            ref: 'expandButton',
            text: "Expand all",
            tooltip: "Show/hide all biomaterial details",
            handler: function () {
                this.fireEvent("toggleExpand");
            }.createDelegate(this)
        });

        this.items.push('->');
        this.items.push(textFilter);
        this.items.push(refreshButton);
        this.items.push(expandButton);

        Gemma.BioMaterialToolbar.superclass.initComponent.call(this);

        this.addEvents("revertSelected", "toggleExpand", "apply", "save", "refresh", "undo");
    },

    /**
     * @memberOf Gemma.BioMaterialToolbar
     */
    enableApplyOnSelect: function (model) {
        var selected = model.getSelections();
        if (selected.length > 0 && this.factorValueCombo.getValue()) {
            this.applyButton.enable();
        } else {
            this.applyButton.disable();
        }
    }
});