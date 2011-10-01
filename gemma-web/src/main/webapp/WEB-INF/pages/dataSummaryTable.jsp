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
						<h2>
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