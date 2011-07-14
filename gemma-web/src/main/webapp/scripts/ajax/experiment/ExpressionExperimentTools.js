Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
/**
 *
 * Panel containing the most interesting info about an experiment.
 * Used as one tab of the EE page
 *
 * pass in the ee details obj as experimentDetails
 *
 * @class Gemma.ExpressionExperimentDetails
 * @extends Ext.Panel
 *
 */
Gemma.ExpressionExperimentTools = Ext.extend(Ext.Panel, {
    experimentDetails: null,
    border: false,
    tbar: new Ext.Toolbar,
    
    save: function(){
        var snField = Ext.getCmp('shortname');
        var dField = Ext.getCmp('description');
        var nField = Ext.getCmp('name');
        var shortName = snField.getValue();
        var description = dField.getValue();
        var name = nField.getValue();
        
        var entity = {
            entityId: this.eeId
        };
        
        if (shortName != snField.originalValue) {
            entity.shortName = shortName;
        }
        
        if (description != dField.originalValue) {
            entity.description = description;
        }
        
        if (name != nField.originalValue) {
            entity.name = name;
        }
        
        ExpressionExperimentController.updateBasics(entity, function(data){
        
            Ext.getCmp('update-button-region').hide();
            
            var k = Ext.getCmp('shortname');
            k.setValue(data.shortName);
            
            k = Ext.getCmp('name');
            k.setValue(data.name);
            
            k = Ext.getCmp('description');
            k.setValue(data.description);
            
        }
.createDelegate(this));
    },
    
    savePubMed: function(){
        var pubmedId = Ext.getCmp('pubmed-id-field').getValue();
        ExpressionExperimentController.updatePubMed(this.eeId, pubmedId, {
            callback: function(data){
                var k = new Gemma.WaitHandler();
                k.on('done', function(e){
                    // var html = this.getPubMedHtml(e);
                    // Ext.getCmp('pubmed-region-wrap').remove(Ext.getCmp('pubmed-region'));
                    // Ext.DomHelper.append('pubmed-region-wrap',
                    // html);
                    window.location.reload();
                }, this);
                k.handleWait(data, false);
            }
.createDelegate(this)
        });
        
    },
    
    removePubMed: function(){
        Ext.Msg.show({
            title: 'Really delete?',
            msg: 'Are you sure you want to delete the reference? This cannot be undone.',
            buttons: Ext.Msg.YESNO,
            fn: function(btn, text){
                if (btn == 'yes') {
                    ExpressionExperimentController.removePrimaryPublication(this.eeId, {
                        callback: function(data){
                            var k = new Gemma.WaitHandler();
                            k.on('done', function(success){
                            
                                if (success) {
                                    var r = Ext.getCmp('pubmed-region-wrap');
                                    r.remove(Ext.getCmp('pubmed-region'));
                                    var form = this.getPubMedForm(this.eeId);
                                    r.add(form);
                                    r.doLayout();
                                }
                                
                            }, this);
                            k.handleWait(data, false);
                        }
.createDelegate(this)
                    });
                }
            },
            scope: this
        });
    },
    
    getPubMedHtml: function(e){
        var pubmedUrl = e.primaryCitation +
        '&nbsp; <a target="_blank" ext:qtip="Go to PubMed (in new window)"' +
        ' href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&dopt=AbstractPlus&list_uids=' +
        e.pubmedId +
        '&query_hl=2&itool=pubmed_docsum"><img src="/Gemma/images/pubmed.gif" ealt="PubMed" /></a>&nbsp;&nbsp';
        
        if (this.editable) {
            // Add the 'delete' button.
            pubmedUrl = pubmedUrl +
            '<span style="cursor:pointer" onClick="Ext.getCmp(\'ee-details-panel\').removePubMed()">' +
            '<img src="/Gemma/images/icons/cross.png"  ext:qtip="Remove publication"  /></a>&nbsp;';
        }
        
        var pubmedRegion = {
            id: 'pubmed-region',
            xtype: 'panel',
            baseCls: 'x-plain-panel',
            html: pubmedUrl,
            width: 380
        };
        return pubmedRegion;
    },
    
    getPubMedForm: function(e){
        var pubmedRegion = new Ext.Panel({
            baseCls: 'x-plain-panel',
            disabledClass: 'disabled-plain',
            id: 'pubmed-region',
            width: 150,
            layout: 'table',
            layoutConfig: {
                columns: 2
            },
            defaults: {
                disabled: !this.editable,
                disabledClass: 'disabled-plain',
                fieldClass: 'x-bare-field'
            },
            items: [{
                xtype: 'numberfield',
                allowDecimals: false,
                minLength: 7,
                maxLength: 9,
                allowNegative: false,
                emptyText: this.isAdmin || this.isUser ? 'Enter pubmed id' : 'Not Available',
                width: 100,
                id: 'pubmed-id-field',
                enableKeyEvents: true,
                listeners: {
                    'keyup': {
                        fn: function(e){
                            if (Ext.getCmp('pubmed-id-field').isDirty() &&
                            Ext.getCmp('pubmed-id-field').isValid()) {
                                // show save
                                // button
                                Ext.getCmp('update-pubmed-region').show(true);
                            }
                            else {
                                Ext.getCmp('update-pubmed-region').hide(true);
                            }
                        },
                        scope: this
                    }
                }
            }, {
                baseCls: 'x-plain-panel',
                id: 'update-pubmed-region',
                html: '<span style="cursor:pointer" onClick="Ext.getCmp(\'ee-details-panel\').savePubMed(' +
                e.id +
                ',[\'shortname\',\'name\',\'description\'])" ><img src="/Gemma/images/icons/database_save.png" title="Click to save changes" alt="Click to save changes"/></span>',
                hidden: true
            }]
        });
        return pubmedRegion;
    },
    renderStatus: function(ee){
        var result = '';
        if (ee.validatedFlag) {
            result = result + '<img src="/Gemma/images/icons/emoticon_smile.png" alt="validated" title="validated"/>';
        }
        
        if (ee.troubleFlag) {
            result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" title="trouble"/>';
        }
        
        if (ee.hasMultiplePreferredQuantitationTypes) {
            result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ' +
            'title="This experiment has multiple \'preferred\' quantitation types. ' +
            'This isn\'t necessarily a problem but is suspicious."/>';
        }
        
        if (ee.hasMultipleTechnologyTypes) {
            result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ' +
            'title="This experiment seems to mix array designs with different technology types."/>';
        }
        
        if (this.editable) {
            result = result +
            Gemma.SecurityManager.getSecurityLink('ubic.gemma.model.expression.experiment.ExpressionExperimentImpl', ee.id, ee.isPublic, ee.isShared, this.editable);
        }
        
        return result || "No flags";
        
    },
    
    initComponent: function(){
        Gemma.ExpressionExperimentTools.superclass.initComponent.call(this);
        var manager = new Gemma.EEManager({
            editable: this.editable,
        });
        var refreshButton = new Ext.Button({
            text: 'Refresh',
            icon: '/Gemma/images/icons/arrow_refresh_small.png',
            tootltip: 'Refresh statistics',
            handler: function(){
                manager.updateEEReport(this.experimentDetails.id);
            },
            scope: this
        
        });
        var editEEButton = new Ext.Button({
            text: 'Edit',
            icon: '/Gemma/images/icons/wrench.png',
            toolTip: 'Go to editor page for this experiment',
            handler: function(){
                window.open('/Gemma/expressionExperiment/editExpressionExperiment.html?id=' +
                this.experimentDetails.id);
            },
            scope: this
        });
        //if (this.isAdmin) { // add this back!
        var deleteEEButton = new Ext.Button({
            text: 'Delete Experiment',
            icon: '/Gemma/images/icons/cross.png',
            toolTip: 'Delete the experiment from the system',
            handler: function(){
                manager.deleteExperiment(this.experimentDetails.id);
            },
            scope: this
        });
        
        var manager = new Gemma.EEManager({
            editable: this.editable,
            id: "eemanager"
        });
        this.manager = manager;
        var e = this.experimentDetails;
        
        var pubmedRegion = {};
        
        if (e.pubmedId) {
            // display the citation, with link out and delete
            // button.
            pubmedRegion = this.getPubMedHtml(e);
        }
        else {
            // offer to create a citation link.
            pubmedRegion = this.getPubMedForm(e);
        }
        
        var taggerurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').tagger(' + e.id + ',' +
        e.taxonId +
        ',' +
        this.editable +
        ',' +
        (e.validatedAnnotations !== null) +
        ')"><a><img src="/Gemma/images/icons/pencil.png" alt="view tags" title="view tags"/> Edit tags<a></span>';
        
        tagView = new Gemma.AnnotationDataView({
            fieldLabel: taggerurl,
            readParams: [{
                id: e.id,
                classDelegatingFor: "ExpressionExperimentImpl"
            }]
        });
        
        manager.on('tagsUpdated', function(){
            tagView.store.reload();
        });
        
        manager.on('done', function(){
            /*
             * After a process that requires refreshing the page.
             */
            window.location.reload();
        });
        
        manager.on('reportUpdated', function(data){
            ob = data[0];
            var k = Ext.get('coexpressionLinkCount-region');
            Ext.DomHelper.overwrite(k, {
                html: ob.coexpressionLinkCount
            });
            k.highlight();
            k = Ext.get('processedExpressionVectorCount-region');
            Ext.DomHelper.overwrite(k, {
                html: ob.processedExpressionVectorCount
            });
            k.highlight();
        });
        
        manager.on('differential', function(){
            window.location.reload(true);
        });
        
        var descriptionArea = new Ext.form.TextArea({
            fieldLabel: 'Description',
            allowBlank: true,
            grow: true,
            growMax: 120,
            readOnly: !this.editable,
            disabledClass: 'disabled-plain',
            growMin: 40,
            emptyText: 'No description provided',
            enableKeyEvents: true,
            listeners: {
                'keyup': {
                    fn: function(field, event){
                        if (field.isDirty() && field.isValid()) {
                            this.fireEvent('validValueChange');
                        }
                        else {
                            this.fireEvent('invalidValueChange');
                        }
                    }
                },
				scope:this
            },
            value: e.description,
			style:'width:100%'
            //width: 700
        });
        
        var nameArea = new Ext.form.TextArea({
            fieldLabel: 'Name',
            allowBlank: false,
            grow: true,
            growMax: 300,
            readOnly: !this.editable,
            disabledClass: 'disabled-plain',
            growMin: 10,
            growAppend: '',
            emptyText: 'No description provided',
            enableKeyEvents: true,
            listeners: {
                'keyup': {
                    fn: function(field, event){
                        if (field.isDirty() && field.isValid()) {
                            this.fireEvent('validValueChange');
                        }
                        else {
                            this.fireEvent('invalidValueChange');
                        }
                    }
                },
				scope:this
            },
            value: e.name,
			style:'width:100%'
            //width: 700
        });
		var saveBtn = new Ext.Button({
                text: 'Save',
                disabled: true,
				toolTip:'Save your changes'
            })
       this.on('validValueChange',function(){
                saveBtn.enable();
           });
	   this.on('invalidValueChange',function(){
                saveBtn.disable();
           });
		   
		this.reset = function(){
			this.fieldEditor.shortname.setValue(e.shortName);
			nameArea.setValue(e.name);
			descriptionArea.setValue(e.description);
           saveBtn.disable();
		};
        var fieldEditor = new Ext.form.FormPanel({
            frame: true,
            padding: 10,
            fieldWidth: 80,
            ref: 'fieldEditor',
            buttons: [saveBtn, {
                text: 'Reset',
                ref: 'resetBtn',
				toolTip:'Reset all fields to saved values',
				handler: this.reset.createDelegate(this)
            }],
            defaults: {
                border: false
            },
            items: [{
                fieldLabel: 'Short name',
                xtype: 'textfield',
                ref: 'shortname',
                enableKeyEvents: true,
                allowBlank: false,
                disabledClass: 'disabled-plain',
                //fieldClass : 'x-bare-field',
                readOnly: !this.editable,
                listeners: {
                    'keyup': {
                        fn: function(field, event){
                            if (field.isDirty() && field.isValid()) {
                                this.fireEvent('validValueChange');
                            }
                            else {
                                this.fireEvent('invalidValueChange');
                            }
                        }
                    },
					scope:this
                },
                width: 250,
                value: e.shortName
            
            }, nameArea, descriptionArea, {
                fieldLabel: 'Publication',
                xtype: 'panel',
                id: 'pubmed-region-wrap',
                layout: 'fit',
                bodyBorder: false,
                baseCls: 'x-plain-panel',
                disabled: false,
                items: [pubmedRegion]
            }, tagView, {
                fieldLabel: 'Status',
                html: this.renderStatus(e)
            }            /*
             * authors
             */
            ]
        });
        //}
        
        this.getTopToolbar().addButton(refreshButton);
        this.getTopToolbar().addItem('-');
        this.getTopToolbar().addButton(editEEButton);
        this.getTopToolbar().addItem('-');
        this.getTopToolbar().addButton(deleteEEButton);
        this.add(fieldEditor);
        this.add({
            border: false,
            padding: 10,
            html: '<h4>Analyses:<br></h4>' +
            'Missing values: ' +
            this.missingValueAnalysisRenderer(this.experimentDetails) +
            '<br>Proccessed Vector Computation:  ' +
            this.processedVectorCreateRenderer(this.experimentDetails) +
            '<br>Differential Expression Analysis:  ' +
            this.differentialAnalysisRenderer(this.experimentDetails) +
            '<br>Link Analysis:  ' +
            this.linkAnalysisRenderer(this.experimentDetails)
        });
        
    },
    
    linkAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doLinks(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="link analysis" title="link analysis"/></span>';
        if (ee.dateLinkAnalysis) {
            var type = ee.linkAnalysisEventType;
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="Analysis was OK"';
            if (type == 'FailedLinkAnalysisEventImpl') {
                color = 'red';
                qtip = 'ext:qtip="Analysis failed"';
            }
            else 
                if (type == 'TooSmallDatasetLinkAnalysisEventImpl') {
                    color = '#CCC';
                    qtip = 'ext:qtip="Analysis was too small"';
                    suggestRun = false;
                }
            
            return '<span style="color:' + color + ';" ' + qtip + '>' +
            Ext.util.Format.date(ee.dateLinkAnalysis, 'y/M/d') +
            '&nbsp;' +
            (suggestRun ? runurl : '');
        }
        else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
        
    },
    missingValueAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doMissingValues(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="missing value computation" title="missing value computation"/></span>';
        
        /*
         * Offer missing value analysis if it's possible (this might need
         * tweaking).
         */
        if (ee.technologyType != 'ONECOLOR' && ee.hasEitherIntensity) {
        
            if (ee.dateMissingValueAnalysis) {
                var type = ee.missingValueAnalysisEventType;
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedMissingValueAnalysisEventImpl') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }
                
                return '<span style="color:' + color + ';" ' + qtip + '>' +
                Ext.util.Format.date(ee.dateMissingValueAnalysis, 'y/M/d') +
                '&nbsp;' +
                (suggestRun ? runurl : '');
            }
            else {
                return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }
            
        }
        else {
            return '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>';
        }
    },
    
    processedVectorCreateRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doProcessedVectors(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="processed vector computation" title="processed vector computation"/></span>';
        
        if (ee.dateProcessedDataVectorComputation) {
            var type = ee.processedDataVectorComputationEventType;
            var color = "#000";
            
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedProcessedVectorComputationEventImpl') { // note:
                // no
                // such
                // thing.
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            
            return '<span style="color:' + color + ';" ' + qtip + '>' +
            Ext.util.Format.date(ee.dateProcessedDataVectorComputation, 'y/M/d') +
            '&nbsp;' +
            (suggestRun ? runurl : '');
        }
        else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
    },
    
    differentialAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doDifferential(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" title="differential expression analysis"/></span>';
        
        if (ee.numPopulatedFactors > 0) {
            if (ee.dateDifferentialAnalysis) {
                var type = ee.differentialAnalysisEventType;
                
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedDifferentialExpressionAnalysisEventImpl') { // note:
                    // no
                    // such
                    // thing.
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }
                
                return '<span style="color:' + color + ';" ' + qtip + '>' +
                Ext.util.Format.date(ee.dateDifferentialAnalysis, 'y/M/d') +
                '&nbsp;' +
                (suggestRun ? runurl : '');
            }
            else {
                return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }
        }
        else {
            return '<span style="color:#CCF;">NA</span>';
        }
    },
    renderProcessedExpressionVectorCount: function(e){
        return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
    }
});

