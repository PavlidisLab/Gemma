<link rel="stylesheet" type="text/css"
	href="/Gemma/scripts/lib/jShowOff/jshowoff.css" />


<div id="frontPageSlideShow" width="100%" align="center" style="background: url(/Gemma/images/slideShow/bg_body_colour2.png) repeat; margin-bottom:30px; border-top: 1px solid gainsboro; border-bottom:1px solid gainsboro;">
<!-- div width="100%" align="center" style="background: #c1d1d5"-->
	<div id="thumbfeatures" style="overflow: hidden; background: url(/Gemma/images/slideShow/bg_body_white_fade_strip.png) repeat">
		<div title="Gemma">
			<table height="250px" width="650px">
				<tr>
					<td>
						<img src="/Gemma/images/slideShow/gemma-lg190-188_glow.png">
					</td>
					<td width="500px">
						<h1 style="text-align: center;">
							Welcome to Gemma!
						</h1>
						<h2 style="text-align:center">Database and tools for meta-analysis of gene expression data</h2>
					</td>
				</tr>
			</table>
		</div>
		<div title="DatabaseChart" id="dataChart">
			<table height="250px" width="650px">
				<tr>
					<td>
						
						<img
							src="http://chart.apis.google.com/chart?chs=280x240&cht=p&chco=224499&chd=${ googleData}
									&chds=a&chdl=${ googleLabels}&chf=bg,s,FFFFFF00
									&chdlp=b|l&chdls=2f2f2f,13"
							width="280" height="240" alt="" />
						<img style="position:absolute;top:123px;left:200px" src="/Gemma/images/slideShow/humanOutline_35_3.png"/>
						<img style="position:absolute;top:60px;left:150px" src="/Gemma/images/slideShow/mouseOutline65_simplified.png"/>
						<!-- img style="position:absolute;top:17px;left:300px" src="/Gemma/images/slideShow/rightBrace.png"/-->
					</td>
					<td class="slideTextTD" style="width:350px">
						<h2 style="text-align: center;">
							Microarray Database
						</h2>
						<ul style="clear: both;">
							<li>
							<span style="color:#EC7F2C;font-size:14pt">Over 3000 data sets </span>
								<a title="" target="_blank"
									href="/Gemma/expressionExperiment/showAllExpressionExperiments.html">[+]</a>
							</li>
							<c:if
								test="${not empty updatedExpressionExperimentCount || not empty stats.updatedArrayDesignCount}">

								<li>
									<c:if test="${not empty updatedExpressionExperimentCount}">
									${ updatedExpressionExperimentCount} experiment<c:if
											test="${updatedExpressionExperimentCount > 1}">s</c:if>

									</c:if>
									<c:if test="${not empty stats.updatedArrayDesignCount}">

										<c:if test="${not empty updatedExpressionExperimentCount}">
										and
									</c:if>
									
									${ stats.updatedArrayDesignCount} array design<c:if
											test="${stats.updatedArrayDesignCount > 1}">s</c:if>

									</c:if>
									updated in the last week
									<a title="" target="_blank"
										href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">[+]</a>
								</li>
							</c:if>
							<c:if
								test="${not empty newExpressionExperimentCount || not empty stats.updatedArrayDesignCount}">
								<li>
									<c:if test="${not empty newExpressionExperimentCount}">
									${ newExpressionExperimentCount} experiment<c:if
											test="${newExpressionExperimentCount > 1}">s</c:if>

									</c:if>
									<c:if test="${not empty stats.updatedArrayDesignCount}">
										<c:if test="${not empty newExpressionExperimentCount}">
										and
									</c:if>
									${ stats.newArrayDesignCount} array design<c:if
											test="${stats.newArrayDesignCount > 1}">s</c:if>
									</c:if>
									added in the last week
									<a title="" target="_blank"
										href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">[+]</a>
								</li>
							</c:if>
							<c:if
								test="${ empty newExpressionExperimentCount && empty stats.updatedArrayDesignCount && empty updatedExpressionExperimentCount && empty stats.updatedArrayDesignCount}">
								Ongoing updates and additions <a title="" target="_blank"
										href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">[+]</a>
								</c:if>
							<li>
								2000+ published papers
								<a title="" target="_blank"
									href="/Gemma/bibRef/showAllEeBibRefs.html">[+]</a>
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
						<img src="/Gemma/images/slideShow/exSlide1.png" height="130px" style="margin:15px;padding:3px; border: 3px double black">
					</td><td>
						<img src="/Gemma/images/slideShow/exSlide2.jpg" height="130px">
					</td>
					<td class="slideTextTD">
						<h2 style="text-align: center;">
							Differential and Coexpression Analysis
						</h2>
						<ul style="clear: both">
							<li>
								Analyse sets of data 
							</li>
							<li>
								Visualise results
							</li>
							<li>
								Prosper
							</li>
						</ul>
					</td>
				</tr>
			</table>
		</div>
		<div title="My Gemma">
			<table style="width:750px;margin-right:100px">
				<tr>
					<td>
						<img src="/Gemma/images/slideShow/myGemma.png" style="padding:15px">
					</td>
					<td class="slideTextTD" >
						<h2 style="text-align: center; margin-right:50px;">
							My Gemma
						</h2>
						<ul style="clear: both; margin-left:0px; margin-right:50px;">
							<li>
								Analyse your private data with Gemma tools
							</li>
							<li>
								Save sets of genes and experiments for analysis
							</li>
							<li>
								Share your data selectively
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


	jQuery(document).ready(function() {
		jQuery('#thumbfeatures').jshowoff( {
			cssClass : 'thumbFeatures',
			effect : 'slideLeft',
			autoPlay: false, // default: true
			speed: 9000 // default: 3000 (ms)
			
		});
	});
	
</script>