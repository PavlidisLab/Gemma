Ext.namespace('Gemma');

/**
 *
 * Top level container for all sections of expression experiment info
 * Sections are:
 * 1. Details (has editing tools)
 * 2. Experimental design
 * 3. Expression visualisation
 * 4. Diagnostics
 * 5. Quantitation Types ?
 * 6. History (admin only)
 * 7. Admin (running analyses)
 *
 * @class Gemma.ExpressionExperimentPage
 * @extends Ext.TabPanel
 *
 */
Gemma.ExpressionExperimentPage = Ext.extend(Ext.TabPanel, {

    height: 600,
    defaults: {
        autoScroll: true,
        width: 850
    },
    deferredRender: true,
    listeners: {
        'tabchange': function(tabPanel, newTab){
            newTab.fireEvent('tabChanged');
        },
        'beforetabchange': function(tabPanel, newTab, currTab){
            // if false is returned, tab isn't changed
            if (currTab) {
                return currTab.fireEvent('leavingTab');
            }
            return true;
        }
    },
    initComponent: function(){
    
        var eeId = this.eeId;
        if ((Ext.get("hasWritePermission")) && Ext.get("hasWritePermission").getValue() == 'true') {
            this.editable = true;
        }
        var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
        
        Gemma.ExpressionExperimentPage.superclass.initComponent.call(this);
        this.on('render', function(){
            if (!this.loadMask) {
                this.loadMask = new Ext.LoadMask(this.getEl(), {
                    msg: "Loading ...",
                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                });
            }
            this.loadMask.show();
            ExpressionExperimentController.loadExpressionExperimentDetails(eeId, function(experimentDetails){
            
                this.experimentDetails = experimentDetails;
                this.editable = experimentDetails.canCurrentUserEditExperiment;
                
                this.loadMask.hide();
                /*DETAILS TAB*/
                this.add(new Gemma.ExpressionExperimentDetails({
                    title: 'Details',
                    experimentDetails: experimentDetails,
                    editable: this.editable,
                    admin: this.admin
                }));
                
                /*EXPERIMENT DESIGN TAB*/
                
                var batchInfo = '';
                if (experimentDetails.hasBatchInformation) {
                    batchInfo = '<span style="font-size: smaller">This experimental design also ' +
                    'has information on batches, not shown.</span>' +
                    '<br />' +
                    '<span style="color:#DD2222;font-size: smaller"> ' +
                    experimentDetails.batchConfound +
                    ' </span>' +
                    '<span style="color:#DD2222;font-size: smaller"> ' +
                    experimentDetails.batchEffect +
                    ' </span>';
                }
                
                this.add({
                    title: 'Experimental Design',
                    tbar: [{
                        text: 'Show Details',
                        tooltip: 'Go to the design details',
                        icon: '/Gemma/images/magnifier.png',
                        handler: function(){
                            window.open("/Gemma/experimentalDesign/showExperimentalDesign.html?eeid=" + experimentDetails.id);
                        }
                    }],
                    html: '<div id="eeDesignMatrix">Loading...</div>' + batchInfo,
                    layout: 'absolute',
                    //items -> bar chart and table?
                    listeners: {
                        render: function(){
                            DesignMatrix.init({
                                id: experimentDetails.id
                            });
                        }
                    }
                });
                
                /*VISUALISATION TAB*/
                var title = '';
                var downloadLink = '';
                var geneList = [];
                title = "Data for a 'random' sampling of probes";
                downloadLink = String.format("/Gemma/dedv/downloadDEDV.html?ee={0}", eeId);
                var viz = new Gemma.VisualizationWithThumbsPanel({
                    thumbnails: false,
                    downloadLink: downloadLink,
                    params: [[eeId], geneList]
                });
                viz.on('render', function(){
                    viz.loadFromParam({
                        params: [[eeId], geneList]
                    });
                });
                this.add({
                    items: viz,
                    layout: 'fit',
                    padding: 0,
                    title: 'Visualize Expression',
                    tbar: new Gemma.VisualizationWidgetGeneSelectionToolbar({
                        eeId: eeId,
                        visPanel: viz,
                        taxonId: experimentDetails.parentTaxonId
                    })
                });
                
                
                /*DIAGNOSTICS TAB*/
                var refreshDiagnosticsLink = '';
                if (this.editable || isAdmin) {
                    refreshDiagnosticsLink = '<a href="refreshCorrMatrix.html?id=' + experimentDetails.id + '"><img ' +
                    'src="/Gemma/images/icons/arrow_refresh_small.png" title="refresh" ' +
                    'alt="refresh" />Refresh</a><br>';
                }
                this.add({
                    title: 'Diagnostics',
                    html: refreshDiagnosticsLink + experimentDetails.QChtml
                });
                
                /*QUANTITATION TYPES TAB*/
                this.add(new Gemma.ExpressionExperimentQuantitationTypeGrid({
                    title: 'Quantitation Types',
                    eeid: experimentDetails.id
                }));
                
                this.adjustForIsAdmin(isAdmin, this.editable);
                
                Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){
                    var appScope = this;
                    ExpressionExperimentController.canCurrentUserEditExperiment(experimentDetails.id, {
                        callback: function(editable){
                            console.log(this);
                            appScope.adjustForIsAdmin(isAdmin, editable);
                        },
                        scope: appScope
                    });
                    
                }, this);
                
                Gemma.Application.currentUser.on("logOut", function(){
                
                    this.adjustForIsAdmin(false, false);
                    
                }, this);
                
                this.setActiveTab(0);
            }
.createDelegate(this));
        });
    },
    adjustForIsAdmin: function(isAdmin, isEditable){
        /*HISTORY TAB*/
        if ((isAdmin || isEditable) && !this.historyTab) {
            this.historyTab = new Gemma.AuditTrailGrid({
                title: 'History',
                bodyBorder: false,
                collapsible: false,
                viewConfig: {
                    forceFit: true
                },
                auditable: {
                    id: this.experimentDetails.id,
                    classDelegatingFor: "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
                }
            });
            this.add(this.historyTab);
        }
        else 
            if (this.historyTab) {
                this.historyTab.setVisible((isAdmin || isEditable));
            }
        
        /*ADMIN TOOLS TAB*/
        if ((isAdmin || isEditable) && !this.toolTab) {
            this.toolTab = new Gemma.ExpressionExperimentTools({
                experimentDetails: this.experimentDetails,
                title: 'Admin',
                editable: isEditable
            });
            this.add(this.toolTab);
        }
        else 
            if (this.toolTab) {
                this.toolTab.setVisible((isAdmin || isEditable));
            }
    }
});

/**
 * Used to make the correlation heatmap clickable. See ExperimentQCTag.java
 *
 * @param {Object}
 *            bigImageUrl
 */
var popupImage = function(url, width, height){
    url = url + "&nocache=" + Math.floor(Math.random() * 1000);
    var b = new Ext.Window({
        modal: true,
        stateful: false,
        resizable: true,
        autoScroll: true,
        autoHeight: height, // or false.
        width: width || 200,
        // x : 50,
        padding: 10,
        html: '<img src=\"' + url + '"\" />'
    });
    b.show();
};
