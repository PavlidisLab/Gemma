<%-- jshowoff.css is included in the bundle --%>
<div id="sloganText">
	<h2 style="text-align: center; color: grey; font-size: 1.8em">
		Tools and database for meta-analysis of functional genomics data
	</h2>
</div>

<div id="frontPageSlideShow" align="center">
	<div id="features" style="overflow: hidden;">
		<div title="1" id="dataChart">
			<table>
				<tr>
					<td class="slideImageTD">
						<%-- Width here needs to be enough to fit the labels at left and right edges --%> 
						<img src="https://chart.apis.google.com/chart?chs=340x240&cht=p&chco=224499&chd=${ googleData}
									&chds=a&chdl=${ googleLabels}&chf=bg,s,FFFFFF00
									&chdlp=b|l&chdls=2f2f2f,13"
							width="340" height="240" alt="" /> 
						<img style="position: absolute; top: 123px; left: 250px" src="${pageContext.request.contextPath}/images/slideShow/humanOutline_35_3.png" />
						<img style="position: absolute; top: 60px; left: 200px"	src="${pageContext.request.contextPath}/images/slideShow/mouseOutline65_simplified.png" />
						<!-- img style="position:absolute;top:17px;left:300px" src="/Gemma/images/slideShow/rightBrace.png"/-->
					</td>
					<td class="slideTextTD">Over <b>10000</b> curated expression studies	</td>
				</tr>
			</table>
		</div>
		<div title="2">
			<table>
				<tr>
					<td class="slideImageTD"><img src="${pageContext.request.contextPath}/images/slideShow/diffEx.png" style="margin-top: 10px" /></td>
					<td align=center class="slideTextTD">
						Search and visualise <b>differential expression</b> patterns across genes and conditions
					</td>
				</tr>
			</table>
		</div>
		<div title="3">
			<table>
				<tr>
					<td class="slideImageTD"><img class="rounded" src="${pageContext.request.contextPath}/images/slideShow/coex.png" style="margin-top: 15px;">
					</td>
					<td class="slideTextTD">
						Search <b>coexpression</b> patterns across studies and visualise the results in a network
					</td>
				</tr>
			</table>
		</div>
		<div title="4">
			<table>
				<tr>
					<td class="slideImageTD"><img src="${pageContext.request.contextPath}/images/slideShow/myGemma2.png" style="padding: 15px"></td>
					<td class="slideTextTD">
						<b>Create and share</b> your own gene and experiment groups
					</td>
				</tr>
			</table>
		</div>
		<div title="5">
			<table>
				<tr>
					<td class="slideImageTD"><img src="${pageContext.request.contextPath}/images/slideShow/neurocarta.png" style="padding: 15px"></td>
					<td class="slideTextTD">
						<div>
							<img src="${pageContext.request.contextPath}/images/logo/phenocarta-45p.png">
						</div> 
						Browse over 60,000 evidence-based <b>gene-to-phenotype associations</b> and create your own
					</td>
				</tr>
			</table>
		</div>
	</div>
</div>
<script type="text/javascript">
   jQuery.noConflict();

   jQuery( document ).ready( function() {

      jQuery( '#features' ).jshowoff( {
         //cssClass : 'thumbFeatures',
         effect : 'slideLeft',
         autoPlay : true, // default: true
         controls : false,
         speed : 5000, // default: 3000 (ms)
         hoverPause : false
      // default: true, was buggy
      } );

   } );
</script>