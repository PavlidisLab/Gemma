Ext.namespace('Gemma');

/**
 * need to set geneId as config
 */
Gemma.GeneAllenBrainAtlasImages = Ext.extend(Ext.Panel, {
    geneId: null,
    padding: 10,
    defaults: {
        border: false,
        autoScroll: false
    },
    listeners: {
        'afterrender': function (c) {
            jQuery('i[title]').qtip();
        }
    },


    initComponent: function () {

        Gemma.GeneAllenBrainAtlasImages.superclass.initComponent.call(this);
        this.on('render', function () {
            if (!this.loadMask) {
                this.loadMask = new Ext.LoadMask(this.getEl(), {
                    msg: Gemma.StatusText.Loading.generic,
                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                });
            }
            this.loadMask.show();
            var self = this;
            GeneController.loadGeneDetails(this.geneId, function (gene) {
                GeneController.getGeneABALink(gene.id, function (geneUrl) {
                    GeneController.loadAllenBrainImages(gene.id, function (imageObjects) {

                        self.loadMask.hide();

                        var img = imageObjects[0];
                        self.add({
                            html: '<h3>Allen Brain Atlas expression pattern' + '<i class="qtp fa fa-question-circle fa-fw" title="'
                            + Gemma.HelpText.WidgetDefaults.GeneAllenBrainAtlasImages.helpTT + '">'
                            + '</i>' + '<a title="Go to Allen Brain Atlas details for '
                            + gene.officialSymbol + '" href="' + geneUrl + '" target="_blank">'
                            + '<img src="' + ctxBasePath + '/images/logo/aba-icon.png" height="20" width="20" /> </a>' + '</h3>'
                        });
                        var i;
                        var imgs = "";
                        for (i = 0; i < imageObjects.length; i++) {
                            img = imageObjects[i];

                            var smallExpLink = img.url + "=expression";
                            var largeExpLink = smallExpLink.replace("downsample=5", "downsample=3");

                            imgs +=
                                '<span style="cursor: pointer; padding: 8px">'
                                + '<a title="'+ img.displayName + ', click to enlarge" '
                                + 'onClick="Gemma.geneLinkOutPopUp( &#34; ' + largeExpLink + ' &#34; )">'
                                + '<img src="' + smallExpLink + '" /> </a>' +
                                '</span>';
                        }

                        if (imgs.length > 0) {
                            self.add({
                                html: '' + imgs
                            });
                        } else {
                            self.add({
                                html: 'No images available'
                            });
                        }

                        self.doLayout();

                    });
                });
            }.createDelegate(self));
        });
    }
});
Ext.reg('geneallenbrainatlasimages', Gemma.GeneAllenBrainAtlasImages);

Gemma.geneLinkOutPopUp = function (abaImageUrl) {

    if (abaImageUrl === null) {
        return;
    }

    var abaWindowId = "geneDetailsAbaWindow";
    var win = Ext.getCmp(abaWindowId);
    if (win) {
        win.close();
    }

    win = new Ext.Window({
        html: "<img src='" + abaImageUrl + "'>",
        id: abaWindowId,
        stateful: false
    });
    win.show(this);

};