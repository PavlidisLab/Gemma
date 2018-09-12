Ext.namespace('Gemma');

/**
 * need to set geneId as config
 */
Gemma.GeneAllenBrainAtlasImages = Ext.extend(Ext.Panel, {
    geneId: null,
    padding: 10,
    defaults: {
        border: false,
        autoScroll: true,
        overflow: true
    },
    listeners: {
        'afterrender': function (c) {
            jQuery('i[title]').qtip();
        }
    },

    /**
     * @memberOf Gemma.GeneAllenBrainAtlasImage
     */
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
            GeneController.loadAllenBrainImages(this.geneId, function (imageObjects) {

                this.loadMask.hide();
                if (!imageObjects || imageObjects.length === 0) {
                    this.add({
                        html: 'No images available'
                    });

                } else {

                    var img = imageObjects[0];
                    var homologueText = (img.usingHomologue) ? 'Images are for homologous mouse gene: '
                        + '<a target="_blank" href="' + ctxBasePath + '/gene/showGene.html?id=' + img.abaHomologousMouseGene.id + '">'
                        + img.abaHomologousMouseGene.officialSymbol + ' [' + img.abaHomologousMouseGene.taxonCommonName
                        + ']</a>' : '';
                    this.add({
                        html: '<h3>Allen Brain Atlas expression pattern' + '<i class="qtp fa fa-question-circle fa-fw" title="'
                        + Gemma.HelpText.WidgetDefaults.GeneAllenBrainAtlasImages.helpTT + '">'
                        + '</i>' + '<a title="Go to Allen Brain Atlas details for '
                        + img.queryGeneSymbol + '" href="' + img.abaGeneURL + '" target="_blank">'
                        + '<img src="' + ctxBasePath + '/images/logo/aba-icon.png" height="20" width="20" /> </a>' + '</h3>' + '<p>'
                        + homologueText + '<p/>'
                    });
                    var i;
                    var imgs = "";
                    for (i = 0; i < imageObjects.length; i++) {
                        img = imageObjects[i];
                        var largeLink = img.downloadExpressionPath.replace("downsample=5", "downsample=3");
                        imgs +=
                            '<span style="cursor: pointer; padding: 8px">'
                            + '<a title="Allen Brain Atlas Image for ' + img.queryGeneSymbol + ', click to enlarge" '
                            + 'onClick="Gemma.geneLinkOutPopUp( &#34; ' + largeLink + ' &#34; )">'
                            + '<img src="' + img.downloadExpressionPath + '" /> </a>' +
                            '</span>';
                    }
                    this.add({
                        html: '' + imgs
                    });

                    this.doLayout();
                }

            }.createDelegate(this));
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