Ext.namespace('Gemma');

Gemma.DIFF_THRESHOLD = 0.01;
Gemma.MAX_DIFF_RESULTS = 125;

/**
 *
 * Top level container for all sections of platform info
 *
 * To open the page at a specific tab, include ?tab=[tabName] suffix in the URL. Tab names are each tab's itemId.
 *
 * @class Gemma.PlatformPage
 * @extends Ext.TabPanel
 *
 */
Gemma.PlatformPage = Ext.extend(Ext.TabPanel, {

    defaults: {
        width: 850
    },
    deferredRender: true,
    initialTab: 'details',
    listeners: {
        'tabchange': function (tabPanel, newTab) {
            newTab.fireEvent('tabChanged');
        },
        'beforetabchange': function (tabPanel, newTab, currTab) {
            // if false is returned, tab isn't changed
            if (currTab) {
                return currTab.fireEvent('leavingTab');
            }
            return true;
        }
    },

    checkURLforInitialTab: function () {
        this.loadSpecificTab = (document.URL.indexOf("?") > -1 && (document.URL.indexOf("tab=") > -1));
        if (this.loadSpecificTab) {
            var param = Ext.urlDecode(document.URL.substr(document.URL.indexOf("?") + 1));
            if (param.tab) {
                if (this.getComponent(param.tab) !== undefined) {
                    this.initialTab = param.tab;
                }
            }
        }else{
            this.initialTab = "details";
        }
    },

    /**
     * @memberOf Gemma.P
     */
    initComponent: function () {

        var platformId = this.platformId;
        var isAdmin = Ext.get("hasAdmin").getValue() === 'true';

        Gemma.PlatformPage.superclass.initComponent.call(this);

        // DETAILS TAB
        var details = new Gemma.PlatformDetails({
            title: 'Overview',
            itemId: 'details',
            platformId: platformId
        });
        details.on('changeTab', function (tabName) {
            this.setActiveTab(tabName);
        }, this);
        this.add(details);

        this.add(new Gemma.PlatformElementsPanel({
            title: 'Elements',
            itemId: 'elements',
            loadOnlyOnRender: true,
            platformId: platformId
        }));

        this.add(new Gemma.ExpressionExperimentGrid({
            title: 'Datasets',
            itemId: 'experiments',
            loadOnlyOnRender: true,
            platformId: platformId
        }));

        this.adjustForIsAdmin(isAdmin);

        this.checkURLforInitialTab();
        this.setActiveTab(this.initialTab);

        // duplicated from experiment details
        Gemma.Application.currentUser.on("logIn", function (userName, isAdmin) {
            var appScope = this;
            appScope.adjustForIsAdmin(isAdmin);
        }, this);
        Gemma.Application.currentUser.on("logOut", function () {
            this.adjustForIsAdmin(false, false);
        }, this);
    },

    // hide/show 'refresh' link to admin tabs.
    adjustForIsAdmin: function (isAdmin) {
        /* HISTORY TAB */
        if (isAdmin && !this.historyTab) {
            this.historyTab = new Gemma.AuditTrailGrid({
                title: 'History',
                itemId: 'history',
                bodyBorder: false,
                collapsible: false,
                viewConfig: {
                    forceFit: true
                },
                auditable: {
                    id: this.platformId,
                    classDelegatingFor: "ubic.gemma.model.expression.arrayDesign.ArrayDesign"
                },
                loadOnlyOnRender: true
            });
            this.add(this.historyTab);
        } else if (this.historyTab) {
            this.historyTab.setVisible(isAdmin);
        }

        /* ADMIN TOOLS TAB */
        if (isAdmin && !this.toolTab) {
            var panel = this;
            ArrayDesignController.getDetails(this.platformId, {
                callback: function (platformDetails) {
                    panel.toolTab = new Gemma.CurationTools({
                        title: 'Curation',
                        itemId: 'admin',
                        curatable: platformDetails,
                        auditable : {
                            id: panel.platformId,
                            classDelegatingFor: "ubic.gemma.model.expression.arrayDesign.ArrayDesign"
                        },
                        listeners: {
                            'reloadNeeded': function () {
                                var myMask = new Ext.LoadMask(Ext.getBody(), {
                                    msg: "Refreshing..."
                                });
                                myMask.show();
                                var reloadToAdminTab = document.URL;
                                reloadToAdminTab = reloadToAdminTab.replace(/&*tab=\w*/, '');
                                reloadToAdminTab += '&tab=admin';
                                window.location.href = reloadToAdminTab;
                            }
                        }
                    });

                    panel.add(panel.toolTab);
                }.createDelegate(this),
                errorHandler: function (er, exception) {
                    Ext.Msg.alert("Error", er + "\n" + exception.stack);
                }
            });

        } else if (this.toolTab) {
            this.toolTab.setVisible(isAdmin);
        }
    }
});
