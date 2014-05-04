<%@ include file="/common/taglibs.jsp"%>
<head>
	<jwr:script src="/scripts/json.js" />


	<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />

	<script type="text/javascript">
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

	Ext.onReady( function() {

		Ext.QuickTips.init();
		Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

		var manager = new Gemma.EEManager( {
			editable : true,
			id : "eemanager"
		});

		manager.on('done', function() {
			window.location.reload(true);
		});

	});
</script>


</head>
<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentEditCommand" />
<spring:bind path="expressionExperiment.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<img src="<c:url value="/images/iconWarning.gif"/>"
					alt="<fmt:message key="icon.warning"/>" class="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>

<title><fmt:message key="expressionExperiment.title" />
	${expressionExperiment.shortName}</title>
<form method="post"
	action="<c:url value="/expressionExperiment/editExpressionExperiment.html"/>">


	<h2>
		Editing:
		<a
			href="<c:url value="/expressionExperiment/showExpressionExperiment.html?id=${expressionExperiment.id}" />">${expressionExperiment.shortName}</a>
	</h2>
	<h3>
		Quantitation Types
	</h3>
	<c:choose>
		<c:when
			test='<%=expressionExperiment.getQuantitationTypes()
									.size() == 0%>'>
									No quantitation types! Data may be corrupted (likely data import error)
		</c:when>
		<c:otherwise>
			<table>
				<tr>
					<th>
						Name
					</th>
					<th>
						Desc
					</th>
					<th>
						Pref?
					</th>
					<th>
						Ratio?
					</th>
					<th>
						Bkg?
					</th>
					<th>
						BkgSub?
					</th>
					<th>
						Norm?
					</th>
					<th>
						Type
					</th>
					<th>
						Spec.Type
					</th>
					<th>
						Scale
					</th>
					<th>
						Rep.
					</th>
				</tr>



				<c:forEach var="index" begin="0"
					end="<%=expressionExperiment.getQuantitationTypes()
										.size() - 1%>"
					step="1">
					<spring:nestedPath
						path="expressionExperiment.quantitationTypes[${index}]">
						<tr>
							<td>
								<spring:bind path="name">
									<input type="text" size="20"
										name="<c:out value="${status.expression}"/>"
										value="<c:out value="${status.value}"/>" />
								</spring:bind>
							</td>
							<td>
								<spring:bind path="description">
									<input type="text" size="35"
										name="<c:out value="${status.expression}"/>"
										value="<c:out value="${status.value}"/>" />
								</spring:bind>
							</td>
							<td>
								<spring:bind path="isPreferred">
									<input id="preferredCheckbox" type="checkbox"
										name="${status.expression}"
										<c:if test="${status.value == true}">checked="checked"</c:if> />
									<input type="hidden"
										name="_<c:out value="${status.expression}"/>">
								</spring:bind>
							</td>
							<td>
								<spring:bind path="isRatio">
									<input id="ratioCheckbox" type="checkbox"
										name="${status.expression}"
										<c:if test="${status.value == true}">checked="checked"</c:if> />
									<input type="hidden"
										name="_<c:out value="${status.expression}"/>">
								</spring:bind>
							</td>
							<td>
								<spring:bind path="isBackground">
									<input id="backgroundCheckbox" type="checkbox"
										name="${status.expression}"
										<c:if test="${status.value == true}">checked="checked"</c:if> />
									<input type="hidden"
										name="_<c:out value="${status.expression}"/>">
								</spring:bind>
							</td>
							<td>
								<spring:bind path="isBackgroundSubtracted">
									<input id="bkgsubCheckbox" type="checkbox"
										name="${status.expression}"
										<c:if test="${status.value == true}">checked="checked"</c:if> />
									<input type="hidden"
										name="_<c:out value="${status.expression}"/>">
								</spring:bind>
							</td>
							<td>
								<spring:bind path="isNormalized">
									<input id="normCheckbox" type="checkbox"
										name="${status.expression}"
										<c:if test="${status.value == true}">checked="checked"</c:if> />
									<input type="hidden"
										name="_<c:out value="${status.expression}"/>">
								</spring:bind>
							</td>
							<td>
								<spring:bind path="generalType">
									<select name="${status.expression}">
										<c:forEach items="${generalQuantitationTypes}" var="type">
											<option value="${type}"
												<c:if test="${status.value == type}">selected</c:if>>
												${type}
											</option>
										</c:forEach>
									</select>
									<span class="fieldError">${status.errorMessage}</span>
								</spring:bind>

							</td>
							<td>
								<spring:bind path="type">
									<select name="${status.expression}">
										<c:forEach items="${standardQuantitationTypes}" var="type">
											<option value="${type}"
												<c:if test="${status.value == type}">selected</c:if>>
												${type}
											</option>
										</c:forEach>
									</select>
									<span class="fieldError">${status.errorMessage}</span>
								</spring:bind>

							</td>
							<td>
								<spring:bind path="scale">
									<select name="${status.expression}">
										<c:forEach items="${scaleTypes}" var="type">
											<option value="${type}"
												<c:if test="${status.value == type}">selected</c:if>>
												${type}
											</option>
										</c:forEach>
									</select>
									<span class="fieldError">${status.errorMessage}</span>
								</spring:bind>

							</td>
							<td>
								<spring:bind path="representation">
									<select name="${status.expression}">
										<c:forEach items="${representations}" var="type">
											<option value="${type}"
												<c:if test="${status.value == type}">selected</c:if>>
												${type}
											</option>
										</c:forEach>
									</select>
									<span class="fieldError">${status.errorMessage}</span>
								</spring:bind>

							</td>
						</tr>
					</spring:nestedPath>
				</c:forEach>

			</table>
		</c:otherwise>
	</c:choose>

	<h3>
		Biomaterials and Assays
	</h3>

	<p>
		<a
			href="<c:url value="/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=${expressionExperiment.id}"/>">
			Click for details and QC</a>
	</p>

	<p>
		<input type="button"
			onClick="Ext.getCmp('eemanager').unmatchBioAssays(${expressionExperiment.id})"
			value="Unmatch all bioassays" />
	</p>



	<Gemma:assayView expressionExperiment="${expressionExperiment}"
		edit="true"></Gemma:assayView>


	<script language="JavaScript" type="text/javascript">
	// all the bioassays
	var dragItems = document.getElementsByClassName('dragItem');

	var windowIdArray = new Array(dragItems.length);
	for (j = 0; j < dragItems.length; j++) {
		windowIdArray[j] = dragItems[j].id;
	}

	for (i = 0; i < windowIdArray.length; i++) {
		var windowId = windowIdArray[i];
		//set to be draggable
		new Draggable(windowId, {
			revert :true,
			ghosting :true
		});

		//set to be droppable, using scriptaculous framework, see dragdrop.js
		Droppables
				.add(
						windowId,
						{
							overlap :'vertical',
							accept :'dragItem',
							hoverclass :'drophover',
							onDrop : function(element, droppableElement) {
								// error check
							// if between columns (ArrayDesigns), do not allow
							if (element.getAttribute('arrayDesign') == droppableElement
									.getAttribute('arrayDesign')) {
								// initialize variables
								var removeFromElement = element
										.getAttribute('material');
								var removeFromDroppable = droppableElement
										.getAttribute('material');
								// swap the assays
								var temp = element.getAttribute('assay');
								element.setAttribute('assay', droppableElement
										.getAttribute('assay'));
								droppableElement.setAttribute('assay', temp);

								// retrieve the JSON object and parse it
								var materialString = document
										.getElementById('assayToMaterialMap').value;
								var materialMap = Ext.util.JSON
										.decode(materialString);

								// write the new values into the materialMap
								materialMap[element.getAttribute('assay')]
										.push(element.getAttribute('material'));
								materialMap[droppableElement
										.getAttribute('assay')]
										.push(droppableElement
												.getAttribute('material'));

								// remove the old values from the materialMap
								var elementToRemove;
								for (k = 0; k < materialMap[element
										.getAttribute('assay')].length; k++) {
									if (materialMap[element
											.getAttribute('assay')][k] = removeFromElement) {
										elementToRemove = k;
										break;
									}
								}

								materialMap[element.getAttribute('assay')]
										.splice(k, 1);
								for (k = 0; k < materialMap[droppableElement
										.getAttribute('assay')].length; k++) {
									if (materialMap[droppableElement
											.getAttribute('assay')][k] = removeFromDroppable) {
										elementToRemove = k;
										break;
									}
								}
								materialMap[droppableElement
										.getAttribute('assay')].splice(k, 1);

								// serialize the JSON object
								document.getElementById('assayToMaterialMap').value = Ext.util.JSON
										.encode(materialMap);

								// swap inner HTML
								var content1 = element.innerHTML;
								var content2 = droppableElement.innerHTML;
								droppableElement.innerHTML = content1;
								element.innerHTML = content2;
							} else {
								new Effect.Highlight(droppableElement.id, {
									delay :0,
									duration :0.25,
									startcolor :'#ff0000',
									endcolor :'#ff0000'
								});
								new Effect.Highlight(droppableElement.id, {
									delay :0.5,
									duration :0.25,
									startcolor :'#ff0000',
									endcolor :'#ff0000'
								});
							}
						}
						});
	}
</script>

	<table>
		<tr>
			<td>
				<input type="submit" class="button" name="save"
					value="<fmt:message key="button.save"/>" />
				<input type="submit" class="button" name="cancel"
					value="<fmt:message key="button.cancel"/>" />
			</td>
		</tr>
	</table>

</form>