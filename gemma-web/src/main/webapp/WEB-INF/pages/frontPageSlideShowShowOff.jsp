<link rel="stylesheet" type="text/css"
	href="/Gemma/scripts/lib/jShowOff/jshowoff.css" />


						<div id="divForChart1"></div>
<div width="100%" align="center" style="background: url(/Gemma/images/slideShow/bg_body_colour2.png) repeat; margin-bottom:30px; border-top: 1px solid gainsboro; border-bottom:1px solid gainsboro;">
<!-- div width="100%" align="center" style="background: #c1d1d5"-->
	<div id="thumbfeatures" style="overflow: hidden">
		<div title="Gemma">
			<table height="250px" width="750px">
				<tr>
					<td>
						<img src="/Gemma/images/slideShow/gemma-lg190-188_glow.png">
					</td>
					<td width="500px">
						<h1 style="text-align: center;">
							Welcome to Gemma!
						</h1>
						<ul style="clear: both;">
							<li>
								Database and software for meta-analysis of gene expression data 
							</li>
							<li>
								Data from 1000s of published microarray data sets, updated and added to weekly.
							</li>
							<li>
								Coexpression and differential expression analysis results can be searched and visualized 
							</li>
						</ul>
					</td>
				</tr>
			</table>
		</div>
		<div title="Database" id="dataSummary">
			<table id="dataSummaryTable">
				<tr>
					<td>
						<div class="roundedcornr_box_777249" style="margin-bottom: 15px;padding:10px; 
										background: url(/Gemma/images/slideShow/white50trans.png) repeat; -moz-border-radius: 15px;
										border-radius: 15px;">
							<!-- div class="roundedcornr_top_777249" style="height:15px">
								<div></div>
							</div-->
							<div class="roundedcornr_content_777249">
								<div style="font-size: small; padding-bottom: 5px;">
									<b> <a target="_blank"
										href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">Updates
											in the last week</a> </b>
								</div>

								<div id="dataSummary"
									style="margin-left: 15px; margin-right: 15px">
									<table style="white-space: nowrap">
										<tr>
											<td style="padding-right: 10px">
												<span style="white-space: nowrap"> <!-- for IE --> <strong>Data
														Summary</strong> </span>
											</td>
											<td style="padding-right: 10px" align="right">
												Total
											</td>
											<c:if test="${not empty updatedExpressionExperimentCount || not empty stats.updatedArrayDesignCount}">
												<td align="right" style="padding-right: 10px">
													Updated
												</td>
											</c:if>
											<c:if test="${not empty newExpressionExperimentCount || not empty stats.newArrayDesignCount || not empty stats.newBioAssayCount}">
												<td align="right">
													New
												</td>
											</c:if>
										</tr>
										<tr>
											<td style="padding-right: 10px">

												<span style="white-space: nowrap"> <!-- for IE --> <a
													href='<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>'>
														Expression Experiments: </a> </span>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ expressionExperimentCount}" /> </b>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ updatedExpressionExperimentCount}" />
												</b>&nbsp;&nbsp;
											</td>
											<td align="right">
												<b><c:out value="${ newExpressionExperimentCount}" /> </b>&nbsp;
											</td>
										</tr>
										<c:forEach var="taxon" items="${ taxonCount }">
											<tr>
												<td style="padding-right: 10px">

													<span style="white-space: nowrap"> <!-- for IE -->
														&emsp; <a
														href='/Gemma/expressionExperiment/showAllExpressionExperiments.html?taxonId=<c:out value="${ taxon.key.id}" />'>
															<c:out value="${ taxon.key.scientificName}" /> </a> 
													</span>
												</td>
												<td align="right" style="padding-right: 10px">
													<c:out value="${ taxon.value}" />
												</td>
												<td align="right" style="padding-right: 10px">
													<c:out value="${ updatedPerTaxonCount[taxon.key]}" />
													&nbsp;&nbsp;
												</td>
												<td align="right">
													<c:out value="${ newPerTaxonCount[taxon.key]}" />
													&nbsp;
												</td>
											</tr>
										</c:forEach>
										<tr>
											<td style="padding-right: 10px">
											
											<span style="white-space: nowrap"> <!-- for IE -->
												<a href='<c:url value="/arrays/showAllArrayDesigns.html"/>'>
													Array Designs: </a>
													</span>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ stats.arrayDesignCount }" /> </b>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ stats.updatedArrayDesignCount}" />
												</b>&nbsp;&nbsp;
											</td>
											<td align="right">
												<b><c:out value="${ stats.newArrayDesignCount}" /> </b>&nbsp;
											</td>
										</tr>
										<tr>
											<td style="padding-right: 10px">
											
											<span style="white-space: nowrap"> <!-- for IE -->
												Assays:
												</span>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ stats.bioAssayCount }" /> </b>
											</td>
											<td align="right" style="padding-right: 10px">
												&nbsp;&nbsp;
											</td>
											<td align="right">
												<b><c:out value="${ stats.newBioAssayCount}" /> </b>&nbsp;
											</td>
										</tr>
									</table>
								</div>
							</div>
							<!-- div class="roundedcornr_bottom_777249"  style="height:15px">
								<div></div>
							</div-->
						</div>
					</td>
					<td class="slideTextTD">
						<h2 style="text-align: center;">
							Microarray Database
						</h2>
						<ul style="clear: both;">
							<li>
								1000s of public microarray data sets <a title="" target="_blank" href="/Gemma/expressionExperiment/showAllExpressionExperiments.html">[more]</a>
							</li>
							<li>
								1000s of published papers <a title="" target="_blank" href="/Gemma/bibRef/showAllEeBibRefs.html">[more]</a>
							</li>
							<li>
								Ongoing updates and additions <a title="" target="_blank" href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">[more]</a>
							</li>
						</ul>
					</td>
				</tr>
			</table>
		</div>
		<div title="Differential Expression">
			<table>
				<tr>
					<td>
						<img src="/Gemma/images/slideShow/exSlide1.png" height="230px" style="padding:15px">
					</td>
					<td class="slideTextTD">
						<h2 style="float: center">
							Differential Expression Visualisation
						</h2>
						<ul style="clear: both">
							<li>
								something about gemma something about gemma
							</li>
							<li>
								something about gemma something about gemma
							</li>
							<li>
								something about gemma something about gemma
							</li>
						</ul>
					</td>
				</tr>
			</table>
		</div>
		<div title="Coexpression">
			<table>
				<tr>
					<td>
						<img src="/Gemma/images/slideShow/exSlide2.jpg" height="230px">
					</td>
					<td class="slideTextTD" >
						<h2 style="float: center">
							Coexpression Visualisation
						</h2>
						<ul style="clear: both;">
							<li>
								something about gemma something about gemma
							</li>
							<li>
								something about gemma something about gemma
							</li>
							<li>
								something about gemma something about gemma
							</li>
						</ul>
					</td>
				</tr>
			</table>
		</div>
	</div>
</div>
<script type="text/javascript">

jQuery.noConflict();

// including this in a separate file causes errors because I'm not sure how to make sure the jQuery.js file is included before it
/*

Title:		jShowOff: a jQuery Content Rotator Plugin
Author:		Erik Kallevig
Version:	0.1.2
Website:	http://ekallevig.com/jshowoff
License: 	Dual licensed under the MIT and GPL licenses.
File name:  jquery.jshowoff.min.js

*/

(function($){$.fn.jshowoff=function(settings){var config={animatePause:true,autoPlay:true,changeSpeed:600,controls:true,controlText:{play:'Play',pause:'Pause',next:'Next',previous:'Previous'},effect:'fade',hoverPause:true,links:true,speed:3000};if(settings)$.extend(true,config,settings);if(config.speed<(config.changeSpeed+20)){alert('jShowOff: Make speed at least 20ms longer than changeSpeed; the fades aren\'t always right on time.');return this;};this.each(function(i){var $cont=$(this);var gallery=$(this).children().remove();var timer='';var counter=0;var preloadedImg=[];var howManyInstances=$('.jshowoff').length+1;var uniqueClass='jshowoff-'+howManyInstances;var cssClass=config.cssClass!=undefined?config.cssClass:'';$cont.css('position','relative').wrap('<div class="jshowoff '+uniqueClass+'" />');var $wrap=$('.'+uniqueClass);$wrap.css('position','relative').addClass(cssClass);$(gallery[0]).clone().appendTo($cont);preloadImg();if(config.controls){addControls();if(config.autoPlay==false){$('.'+uniqueClass+'-play').addClass(uniqueClass+'-paused jshowoff-paused').text(config.controlText.play);};};if(config.links){addSlideLinks();$('.'+uniqueClass+'-slidelinks a').eq(0).addClass(uniqueClass+'-active jshowoff-active');};if(config.hoverPause){$cont.hover(function(){if(isPlaying())pause('hover');},function(){if(isPlaying())play('hover');});};if(config.autoPlay&&gallery.length>1){timer=setInterval(function(){play();},config.speed);};if(gallery.length<1){$('.'+uniqueClass).append('<p>For jShowOff to work, the container element must have child elements.</p>');};function transitionTo(gallery,index){var oldCounter=counter;if((counter>=gallery.length)||(index>=gallery.length)){counter=0;var e2b=true;}
else if((counter<0)||(index<0)){counter=gallery.length-1;var b2e=true;}
else{counter=index;}
if(config.effect=='slideLeft'){var newSlideDir,oldSlideDir;function slideDir(dir){newSlideDir=dir=='right'?'left':'right';oldSlideDir=dir=='left'?'left':'right';};counter>=oldCounter?slideDir('left'):slideDir('right');$(gallery[counter]).clone().appendTo($cont).slideIt({direction:newSlideDir,changeSpeed:config.changeSpeed});if($cont.children().length>1){$cont.children().eq(0).css('position','absolute').slideIt({direction:oldSlideDir,showHide:'hide',changeSpeed:config.changeSpeed},function(){$(this).remove();});};}else if(config.effect=='fade'){$(gallery[counter]).clone().appendTo($cont).hide().fadeIn(config.changeSpeed,function(){if($.browser.msie)this.style.removeAttribute('filter');});if($cont.children().length>1){$cont.children().eq(0).css('position','absolute').fadeOut(config.changeSpeed,function(){$(this).remove();});};}else if(config.effect=='none'){$(gallery[counter]).clone().appendTo($cont);if($cont.children().length>1){$cont.children().eq(0).css('position','absolute').remove();};};if(config.links){$('.'+uniqueClass+'-active').removeClass(uniqueClass+'-active jshowoff-active');$('.'+uniqueClass+'-slidelinks a').eq(counter).addClass(uniqueClass+'-active jshowoff-active');};};function isPlaying(){return $('.'+uniqueClass+'-play').hasClass('jshowoff-paused')?false:true;};function play(src){if(!isBusy()){counter++;transitionTo(gallery,counter);if(src=='hover'||!isPlaying()){timer=setInterval(function(){play();},config.speed);}
if(!isPlaying()){$('.'+uniqueClass+'-play').text(config.controlText.pause).removeClass('jshowoff-paused '+uniqueClass+'-paused');}};};function pause(src){clearInterval(timer);if(!src||src=='playBtn')$('.'+uniqueClass+'-play').text(config.controlText.play).addClass('jshowoff-paused '+uniqueClass+'-paused');if(config.animatePause&&src=='playBtn'){$('<p class="'+uniqueClass+'-pausetext jshowoff-pausetext">'+config.controlText.pause+'</p>').css({fontSize:'62%',textAlign:'center',position:'absolute',top:'40%',lineHeight:'100%',width:'100%'}).appendTo($wrap).addClass(uniqueClass+'pauseText').animate({fontSize:'600%',top:'30%',opacity:0},{duration:500,complete:function(){$(this).remove();}});}};function next(){goToAndPause(counter+1);};function previous(){goToAndPause(counter-1);};function isBusy(){return $cont.children().length>1?true:false;};function goToAndPause(index){$cont.children().stop(true,true);if((counter!=index)||((counter==index)&&isBusy())){if(isBusy())$cont.children().eq(0).remove();transitionTo(gallery,index);pause();};};function preloadImg(){$(gallery).each(function(i){$(this).find('img').each(function(i){preloadedImg[i]=$('<img>').attr('src',$(this).attr('src'));});});};function addControls(){$wrap.append('<p class="jshowoff-controls '+uniqueClass+'-controls"><a class="jshowoff-play '+uniqueClass+'-play" href="#null">'+config.controlText.pause+'</a> <a class="jshowoff-prev '+uniqueClass+'-prev" href="#null">'+config.controlText.previous+'</a> <a class="jshowoff-next '+uniqueClass+'-next" href="#null">'+config.controlText.next+'</a></p>');$('.'+uniqueClass+'-controls a').each(function(){if($(this).hasClass('jshowoff-play'))$(this).click(function(){isPlaying()?pause('playBtn'):play();return false;});if($(this).hasClass('jshowoff-prev'))$(this).click(function(){previous();return false;});if($(this).hasClass('jshowoff-next'))$(this).click(function(){next();return false;});});};function addSlideLinks(){$wrap.append('<p class="jshowoff-slidelinks '+uniqueClass+'-slidelinks"></p>');$.each(gallery,function(i,val){var linktext=$(this).attr('title')!=''?$(this).attr('title'):i+1;$('<a class="jshowoff-slidelink-'+i+' '+uniqueClass+'-slidelink-'+i+'" href="#null">'+linktext+'</a>').bind('click',{index:i},function(e){goToAndPause(e.data.index);return false;}).appendTo('.'+uniqueClass+'-slidelinks');});};});return this;};})(jQuery);(function($){$.fn.slideIt=function(settings,callback){var config={direction:'left',showHide:'show',changeSpeed:600};if(settings)$.extend(config,settings);this.each(function(i){$(this).css({left:'auto',right:'auto',top:'auto',bottom:'auto'});var measurement=(config.direction=='left')||(config.direction=='right')?$(this).outerWidth():$(this).outerHeight();var startStyle={};startStyle['position']=$(this).css('position')=='static'?'relative':$(this).css('position');startStyle[config.direction]=(config.showHide=='show')?'-'+measurement+'px':0;var endStyle={};endStyle[config.direction]=config.showHide=='show'?0:'-'+measurement+'px';$(this).css(startStyle).animate(endStyle,config.changeSpeed,callback);});return this;};})(jQuery);

/*EOF*/

	jQuery(document).ready(function() {
		jQuery('#thumbfeatures').jshowoff( {
			cssClass : 'thumbFeatures',
			effect : 'slideLeft',
			autoPlay: false, // default: true
			speed: 9000 // default: 3000 (ms)
			
		});
	});


/*
Ext.onReady(function(){
	console.log(Ext.get('divForChart'));
	

		
		var ctx, up, down, diffExpressed, interesting;
			//if (Ext.get('canvasforchart')) {
				ctx = Ext.get('canvasforchart').dom.getContext("2d");
				drawOneColourMiniPie(ctx, 12, 12, 14, '#1f6568', .40 * 360, 'black');

			//}
	
	
	
	
	var chartStore = new Ext.data.JsonStore({
	    data: ${ json},
	    storeId: 'myStore',
	    // reader configs
	    root: 'taxonCounts',
	    fields: ['taxon', 'count']
	});
	
	new Ext.chart.PieChart({
	//renderTo:'divForChart',
	            store: chartStore,
	            dataField: 'count',
	            categoryField: 'taxon',
	            //extra styles get applied to the chart defaults
	            extraStyle:
	            {
	                legend:
	                {
	                    display: 'bottom',
	                    padding: 5,
	                    font:
	                    {
	                        family: 'Tahoma',
	                        size: 13
	                    }
	                }
	            }
            });
	
	var chart = new Ext.Panel({
		html:'my ext panel'+${ json},
		renderTo: 'divForChart'	,
		frame:true,
		width: 400,
        height: 200,
		items:{
	            store: chartStore,
	            xtype: 'piechart',
	            dataField: 'count',
	            categoryField: 'taxon',
	            //extra styles get applied to the chart defaults
	            extraStyle:
	            {
	                legend:
	                {
	                    display: 'bottom',
	                    padding: 5,
	                    font:
	                    {
	                        family: 'Tahoma',
	                        size: 13
	                    }
	                }
	            }
            }
	});

	jQuery(document).ready(function() {
		jQuery('#thumbfeatures').jshowoff( {
			cssClass : 'thumbFeatures',
			effect : 'slideLeft',
			autoPlay: false, // default: true
			speed: 9000 // default: 3000 (ms)
			
		});
	});

});*/
	
</script>