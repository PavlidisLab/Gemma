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
 * set config
	stateId: [...],
	for this particular tutorial to stay hidden once closed
 * 
 * @param {Object[]} elementToText
 */
Gemma.Tutorial.ControlPanel = Ext.extend(Ext.Panel, {
	tips: [],
	targetEls: [],
	currIndex: 0,
	padding: 10,
	bodyStyle: 'background-color: #FFFDE9;line-height:22px',
	defaults: {
		bodyStyle: 'background: transparent;margin-left:auto;margin-right:auto;width:600px;',
		border: false,
		padding: 5
	},
		
	stateful: true,
	// what describes the state of this panel - in this case it is the "hidden" field
	getState: function(){
		return {
			hidden: this.hidden
		};
	},
	// specify when the state should be saved - in this case after panel was hidden or shown
	stateEvents: ['hide', 'show'],
	
	initComponent: function(){
		this.currIndex = 0;
		Gemma.Tutorial.ControlPanel.superclass.initComponent.call(this);
		this.add([{
			layout: 'hbox',
			flex: 1,
			height: 30,
			items: [{
				html: 'You are now using a tutorial. How fun! click the "next" and "previous" buttons.',
				border: false,
				bodyStyle: 'background: transparent;',
				flex: 1
			}, {
				xtype: 'button',
				cls: 'transparent-btn',
				tooltip: 'Close this tutorial',
				icon: '/Gemma/images/icons/cross.png',
				handler: this.hideTutorial,
				scope: this,
				flex: 0
			}]
		}, {
			ref: 'controlBtns',
			layout: {
				type: 'hbox',
				pack: 'end'
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
		this.tips = [];
		var i;
		var progressBtns = [];
		for (i = 0; i < elementToText.length; i++) {
			this.targetEls.push(elementToText[i].element);
			// make tips
			this.tips.push(this.initTip(elementToText[i]));
			// make nav buttons for tips
			this.controlBtns.insert(1 + i, {
				xtype: 'button',
				ref: 'progBtn' + i,
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
		if (this.currIndex === (this.tips.length - 1)) {
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
		if (this.currIndex) this.hideTip(this.tips[this.currIndex], this.currIndex);
		this.showTip(this.tips[--this.currIndex], this.currIndex);
	},
	hideTutorial: function(){
		this.currIndex = -1;
		this.hideAllTips();
		this.hide();
		this.fireEvent('tutorialHidden');
		//this.destroy();
	},
	hideAllTips: function(){
		var i;
		for (i = 0; i < this.tips.length; i++) {
			this.hideTip(this.tips[i], i);
		}
	},
	hideTip: function(tip, index){
		if (!tip) return;
		tip.hide();
		this.controlBtns['progBtn' + index].toggle(false);
		
	},
	showTip: function(tip, index){
		if (!tip) return;
		tip.show();
		this.controlBtns['progBtn' + index].toggle(true);
		this.updateBtnDisabling();
	},
	initTip: function(elementToText){
		var element, tipTitle, tipBody, tipConfig;
		element = elementToText.element;
		tipTitle = elementToText.title;
		tipBody = elementToText.text;
		tipConfig = elementToText.tipConfig;
		// need this for override of onShow
		var topScope = this;
		var defaultConfigs = {
			cls: 'x-tip-yellow',
			anchorToTarget: true,
			anchor: 'top',
			trackMouse: false,
			target: element.el, // The overall target element.
			// overwrite the onShow method to control when the tool tip is shown
			// we don't want its show/hide state to be effected by hovering on the target element	
			onShow: function(){
				// if the tip is supposed to be shown according to the tutorial's controls, then show it
				var onId = (topScope.tips[topScope.currIndex])? topScope.tips[topScope.currIndex].id : -1;
				if ( onId === this.id) {
					element.addClass('highlightToggleBorderOn');
					if (this.floating) {
						return this.el.show();
					}
					Ext.Panel.superclass.onShow.call(this);
				}
				// in any other case, don't show it
				return false;
			},
			hidden: true,
			padding: 10,
			renderTo: Ext.getBody(), // Render immediately so that tip.body can be referenced prior to the element show.
			html: tipBody,
			title: tipTitle,
			autoHide: false,
			draggable: true, // need this to be true so clicking outside the tip doesn't close it
			closable: true,
			listeners: {
				'render': function(){
					this.body.on('click', function(e){
						e.stopEvent();
						this.fireEvent('nextTip');
					}, this, {
						delegate: 'a'
					});
				},
				'hide': function(){
					element.removeClass('highlightToggleBorderOn');
				}
			}
		};
		// need to apply configs before creating tooltip to control anchor config
		Ext.apply(defaultConfigs, tipConfig);
		
		var tip = new Ext.ToolTip(defaultConfigs);
		return tip;
	}
});

Ext.reg('Tutorial.ControlPanel', Gemma.Tutorial.ControlPanel);
