Ext.namespace('Gemma.Tutorial');

/**
 * Format of param array's elements must be:
 * 	
 *  var elementToText = [];
	elementToText.push({
		element: this.visualizationPanel,
		title: 'Forth',
		text: 'Look out--here comes science!  <br><a href="#">Next</a>',
		tipConfig:{
			anchor: 'left'
		}
	});
 * 
 * @param {Object[]} elementToText
 */
Gemma.Tutorial.ControlPanel = Ext.extend(Ext.Panel, {
	tips: [],
	targetEls: [],
	currIndex: 0,
	padding: 10,
	bodyStyle: 'background-color: #FFFDE9;line-height:22px',
	defaults:{
		bodyStyle:'background: transparent;margin-left:auto;margin-right:auto;width:600px;',
		border: false,
		padding:5
	},
	initComponent: function(){
		this.currIndex = 0;
		Gemma.Tutorial.ControlPanel.superclass.initComponent.call(this);
		this.add([{
			xtype:'button',
			text:'close',
			handler:this.closeTutorial,
			scope: this
		},{
			html: 'You are now using a tutorial. How fun! click the "next" and "previous" buttons.'
		}, {
			ref: 'controlBtns',
			layout: {
				type:'hbox',
				pack:'end'
			},
			
			items: [{
				xtype: 'button',
				ref: 'prevBtn',
				text: "Previous",
				disabled: true,
				handler: this.showPrevTip,
				scope: this
			}, {
				xtype: 'button',
				ref: 'nextBtn',
				text: 'Next',
				handler: this.showNextTip,
				scope: this
			}]
		}]);
	},
	initTips: function(elementToText){
		this.currIndex = 0;
		var i;
		var progressBtns = [];
		for (i = 0; i < elementToText.length; i++) {
			this.targetEls.push(elementToText[i].element);
			// make tips
			this.tips.push(Gemma.Tutorial.initTip(elementToText[i]));
			// make nav buttons for tips
			this.controlBtns.insert(1+i, {
				xtype: 'button',
				ref: 'progBtn'+i,
				icon: '/Gemma/images/icons/bullet_black.png',
				cls: 'transparent-btn',
				toggleHandler: function(button, state){
					if (state) {
						button.setIcon('/Gemma/images/icons/bullet_blue.png');
					} else {
						button.setIcon('/Gemma/images/icons/bullet_black.png');
					}
				},
				enableToggle: true,
				handler: this.playTips.createDelegate(this, [i]),
				scope: this
			});
		}
		this.doLayout();
	},
	/**
	 * clears any existing tips and restarts the series from the index param
	 * @param {Object} index index to start playing tips from
	 */
	playTips: function(index){
		this.hideAllTips();
		this.currIndex = index;
		this.showTip(this.tips[index], index);
	},
	updateBtnDisabling: function(){
		if (this.currIndex === 0) {
			this.controlBtns.prevBtn.disable();
		} else {
			this.controlBtns.prevBtn.enable();
		}
		if (this.currIndex === (this.tips.length-1)) {
			this.controlBtns.nextBtn.disable();
		} else {
			this.controlBtns.nextBtn.enable();
		}
	},
	showNextTip: function(){
		this.hideTip(this.tips[this.currIndex], this.currIndex);
		this.showTip(this.tips[++this.currIndex], this.currIndex);
	},
	showPrevTip: function(){
		if(this.currIndex)
		this.hideTip(this.tips[this.currIndex], this.currIndex);
		this.showTip(this.tips[--this.currIndex], this.currIndex);
	},
	closeTutorial: function(){
		this.hideAllTips();
		this.resetToOriginalTips();
		this.hide();
	},
	hideAllTips: function(){
		var i;
		for (i = 0; i < this.tips.length; i++) {
			this.hideTip(this.tips[i], i);
		}
	},
	hideTip: function(tip, index){
		if(!tip) return;
		tip.hide();
		this.controlBtns['progBtn'+index].toggle(false);
		//element.removeClass('highlightToggleBorderOn');
	},
	showTip: function(tip, index){
		if (tip) {
			tip.show();
		}
		this.controlBtns['progBtn'+index].toggle(true);
		this.updateBtnDisabling();
		//element.addClass('highlightToggleBorderOn');
	},
	resetToOriginalTips: function(){
		for (i = 0; i < this.targetEls.length; i++) {
			delete this.targetEls[i].tip;
			this.targetEls[i].tip = this.targetEls[i].originalTip;
		}
	}
});

Ext.reg('Tutorial.ControlPanel', Gemma.Tutorial.ControlPanel);

Gemma.Tutorial.initTip = function(elementToText){
	var element, tipTitle, tipBody, tipConfig;
	element = elementToText.element;
	tipTitle = elementToText.title;
	tipBody = elementToText.text;
	tipConfig = elementToText.tipConfig;
	if (element.tip ) {
		element.originalTip = element.tip; 
	}
	var tip = new Ext.ToolTip({
			cls: 'x-tip-yellow',
			anchorToTarget: true,
			hidden: true,
			padding: 10,
			target: element.el, // The overall target element.
			trackMouse: false,
			renderTo: Ext.getBody(), // Render immediately so that tip.body can be referenced prior to the element show.
			html: tipBody,
			title: tipTitle,
			anchor: 'right',
			autoHide: false,
			draggable:true,
			closable: true,
			listeners: {
				'render': function(){
					this.body.on('click', function(e){
						e.stopEvent();
						this.fireEvent('nextTip');
					}, this, {
						delegate: 'a'
					});
				}
		}
	});
	
	Ext.apply(tip, tipConfig);
	return tip;
}
Gemma.Tutorial.showHelp = function(elementToText){

	var hideTip = function(element){
		if(!element.tip) return;
		element.tip.hide();
		element.removeClass('highlightToggleBorderOn');
	}
		
	var currDef, currTip;
	var playTips = function (i) {
			if (i < elementToText.length) {
				currDef = elementToText[i];
				currTip = Gemma.Tutorial.initTip(currDef);
				currTip.on('nextTip', function(){
					hideTip(currDef.element);
					playTips(++i);
				}, this);
			} else {
				hideTip(currDef.element);
			}
			return;
		}.createDelegate(this);
		
	playTips(0);
		

};