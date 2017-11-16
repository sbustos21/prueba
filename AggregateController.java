package com.cgi.ferrovial.vialivre.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.cgi.ferrovial.vialivre.dto.aggregate.AggregateDDIssueDTO;
import com.cgi.ferrovial.vialivre.dto.aggregate.AggregateDTO;
import com.cgi.ferrovial.vialivre.dto.aggregate.AggregateInvoiceDTO;
import com.cgi.ferrovial.vialivre.dto.common.ClientErpSapDTO;
import com.cgi.ferrovial.vialivre.dto.dataParam.SettI18nDataDTO;
import com.cgi.ferrovial.vialivre.dto.dataParam.extra.ExtraChargeTypeDTO;
import com.cgi.ferrovial.vialivre.dto.reports.ReportDataDTO;
import com.cgi.ferrovial.vialivre.exception.ViaLivreGUIException;
import com.cgi.ferrovial.vialivre.model.JsonResponse;
import com.cgi.ferrovial.vialivre.model.aggregate.ActionsAggregateResponse;
import com.cgi.ferrovial.vialivre.model.aggregate.AggregateApprovalPendingResponse;
import com.cgi.ferrovial.vialivre.model.aggregate.AggregateInvoice;
import com.cgi.ferrovial.vialivre.model.aggregate.AggregateSearchResponse;
import com.cgi.ferrovial.vialivre.model.aggregate.form.AggregateInvoiceForm;
import com.cgi.ferrovial.vialivre.model.aggregate.form.AggregateSearchForm;
import com.cgi.ferrovial.vialivre.model.aggregate.form.ApprovalPendingForm;
import com.cgi.ferrovial.vialivre.model.common.Bank;
import com.cgi.ferrovial.vialivre.model.frontoffice.DebtDocsSearchResponse;
import com.cgi.ferrovial.vialivre.model.virtualDocument.response.VirtualDocumentNotifSearchResponse;
import com.cgi.ferrovial.vialivre.model.virtualDocument.response.VirtualDocumentWSResponse;
import com.cgi.ferrovial.vialivre.service.AggregateService;
import com.cgi.ferrovial.vialivre.service.DataParamService;
import com.cgi.ferrovial.vialivre.service.FrontOfficeService;
import com.cgi.ferrovial.vialivre.service.VirtualDocumentService;
import com.cgi.ferrovial.vialivre.util.AggregateUtility;
import com.cgi.ferrovial.vialivre.util.LogUtility;
import com.cgi.ferrovial.vialivre.util.VialivreUtility;
import com.cgi.ferrovial.vialivre.util.constants.ViaLivreGUIConstants;
import com.cgi.ferrovial.vialivre.util.messages.Messages;
import com.cgi.ferrovial.vialivre.validator.AggregateValidator;
import com.cgi.ferrovial.vialivre.validator.ApprovalPendingValueValidator;
/**
 * Clase que se comunica con el javascript para el paso de información que mostrar en la GUI sobre la búsqueda de Agregados
 *
 */
@Controller
public class AggregateController {
	private static final Logger LOGGER = Logger.getLogger(AggregateController.class);

	@Autowired
	private AggregateValidator aggregateValidator;
	
	@Autowired
	private ApprovalPendingValueValidator approvalPendingValueValidator;

	@Autowired
	private AggregateService aggregateService;
	
	@Autowired
	private DataParamService dataParamService;

	@Autowired
	private VirtualDocumentService virtualDocumentService;
	
	@Autowired
	private FrontOfficeService frontOfficeService;
	
	/**
	 * 
	 * @return AggregateSearchForm
	 */
	@ModelAttribute("aggregateSearchForm")
	public AggregateSearchForm getAggregateSearchFormFormObject() {
		return new AggregateSearchForm();
	}
	/**
	 * 
	 * @return AggregateDDIssueDTO
	 */
	@ModelAttribute("aggregateDDIssueForm")
	public AggregateDDIssueDTO getAggregateDDIssueFormFormObject() {
		return new AggregateDDIssueDTO();
	}
	
	/**
	 * 
	 * @param dataBinder
	 */
	@InitBinder
	public void initBinder(WebDataBinder dataBinder) {
		dataBinder.setAutoGrowCollectionLimit(ViaLivreGUIConstants._5000);		
	}
	
	/**
	 * 
	 * @param oidAggregate
	 * @return ModelAndView
	 */
	@PreAuthorize("hasPermission(#oidAggregate, 'vialivre.Aggregate', 'ag.send')")
	@RequestMapping(value = "/goToAggrApprovalPending")
	public ModelAndView goToAggrApprovalPending(@RequestParam(value = "oidAggregate", required = true) Long oidAggregate) {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, 
	            null != oidAggregate ? oidAggregate.getClass().getName() : null);
		
		ModelAndView model = new ModelAndView("aggregate/approvalPending");
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> errores = new HashMap<String, String>();
		
		try {
			LogUtility.writeDebugLog(Messages.getString("debug.invoice.aggregate.1",oidAggregate), LOGGER);
			AggregateApprovalPendingResponse aggregateApprovalPendingResponse = new AggregateApprovalPendingResponse();
			
			AggregateDTO aggregateDTO = aggregateService.getAggregateFromOid(oidAggregate);
			aggregateApprovalPendingResponse.setAggregateDTO(aggregateDTO);
			
			List<ExtraChargeTypeDTO> lstRefExtraCharge = dataParamService.getAllExtraChargeTypes();
			String jsonErrors = mapper.writeValueAsString("");
			String jsonRefExtraCharge = mapper.writeValueAsString(lstRefExtraCharge);
			
			model.addObject("errors", jsonErrors);
			model.addObject("aggregateToApproval", aggregateApprovalPendingResponse);
			model.addObject("oidAggregate", oidAggregate);
			model.addObject("lstItemsReasons", jsonRefExtraCharge);
		} catch (ViaLivreGUIException e) {
			// Se redirecciona al jsp de error
			model = new ModelAndView(ViaLivreGUIConstants.ERROR_PAGE_REDIRECT);
			errores.put(e.getThrowExceptionClassName(), e.getMessageLog());
			model.addObject("errors", errores);
			LogUtility.writeErrorLog(Messages.getString("error.redirectpendingapproval.aggregate", e.getThrowExceptionClassName(), oidAggregate), LOGGER, e);
		} catch (Exception e) {
			// Se redirecciona al jsp de error
			model = new ModelAndView(ViaLivreGUIConstants.ERROR_PAGE_REDIRECT);
			errores.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			model.addObject("errors", errores);
			LogUtility.writeErrorLog(Messages.getString("error.redirectpendingapproval.aggregate", this.getClass().getName(), oidAggregate), LOGGER, e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, model);
		return model;
	}
	
	/**
	 * 
	 * @param approvalPendingForm
	 * @param result
	 * @return JsonResponse
	 */
	@PreAuthorize("hasPermission(#approvalPendingForm.oidAggregate, 'vialivre.Aggregate', 'ag.send')")
	@RequestMapping(value = "/approvalPending", method = RequestMethod.POST)
	public @ResponseBody JsonResponse approvalPending(@Valid @ModelAttribute("approvalPendingForm") ApprovalPendingForm approvalPendingForm, BindingResult result) {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, approvalPendingForm);

		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
		
		approvalPendingValueValidator.validate(approvalPendingForm, result);
		
		try {
			if (!result.hasErrors()) {
				VirtualDocumentWSResponse virtualDocumentWSResponse = aggregateService.sendAggr(approvalPendingForm.getOidAggregate(),approvalPendingForm.getLstApprovalPendingValues(),approvalPendingForm.getObservations());
	
				if (virtualDocumentWSResponse.getResult().equals(ViaLivreGUIConstants.WS_OK_RESULT)) {
					jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
					 AggregateSearchResponse aggregateSearchResponse = AggregateUtility.aggregateDTOToAggregateSearchResponse(aggregateService.getAggregateFromOid(approvalPendingForm.getOidAggregate()));
					jsonResponse.setResult(aggregateSearchResponse);
				} else {
					if (virtualDocumentWSResponse.getResult().equals(ViaLivreGUIConstants.WS_KO_RESULT)) {
						jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
						Integer size = virtualDocumentWSResponse.getCodes().size();
						for (int i = 0; i < size; i++) {
							String message = virtualDocumentWSResponse.getCodes().get(i) + "." + virtualDocumentWSResponse.getMessages().get(i);
							errors.put(virtualDocumentWSResponse.getMessages().get(i), message);
						}
						jsonResponse.setResult(errors);
					}
				}
			} else {
				jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
				List<FieldError> fieldErrors = result.getFieldErrors();
				for (FieldError fieldError : fieldErrors) {
					String[] resolveMessageCodes = result.resolveMessageCodes(fieldError.getCode());
					if (resolveMessageCodes.length > 0) {
						String message = resolveMessageCodes[resolveMessageCodes.length - 1];
						errors.put(fieldError.getField(), message);
					}
				}
				jsonResponse.setResult(errors);
			}
				
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.approvalpending.aggregate", e.getThrowExceptionClassName(), approvalPendingForm.getOidAggregate()), e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.approvalpending.aggregate", this.getClass().getName(), approvalPendingForm.getOidAggregate()), e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
		return jsonResponse;
	}

	
	/**
	 * Redirecciona a la página de búsqueda de agregados pasando a la vista la
	 * lista de estados
	 * 
	 * @return ModelAndView
	 */
	@PreAuthorize("hasAuthority('ag.read')")
	@RequestMapping(value = "/goAggregateSearch")
	public ModelAndView getAggregatePage() {
		ModelAndView model = new ModelAndView("aggregate/aggregateSearch");
		Map<String, String> errors = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			List<SettI18nDataDTO> listAggregateStatus = aggregateService.getAllAggregateStatus();
			List<SettI18nDataDTO> listMethodPayment = aggregateService.getAllMethodPayment();
			List<SettI18nDataDTO> listVirtualDocumetTypes = virtualDocumentService.getAllVirtualDocumentType();
			List<Bank> listBanks = dataParamService.getAllBanks(ViaLivreGUIConstants.NLI_CONCESSION);	
			String days = aggregateService.getDaysSearchAggrergate();
			List<String> listLanguages = aggregateService.getAllLanguages();

			String jsonAggegrateStatus = mapper.writeValueAsString(listAggregateStatus);
			String jsonMethodPayment = mapper.writeValueAsString(listMethodPayment);
			String jsonSchema = mapper.writeValueAsString("");
			String jsonDays = mapper.writeValueAsString(days);
			String jsonVirtualDocumetTypes = mapper.writeValueAsString(listVirtualDocumetTypes);
			String jsonLanguages = mapper.writeValueAsString(listLanguages);
			
			model.addObject("listAggregateStatus", jsonAggegrateStatus);
			model.addObject("listMethodPayment", jsonMethodPayment);
			model.addObject("listVirtualDocumetTypes",jsonVirtualDocumetTypes);
			model.addObject("listBanks", listBanks);			
			model.addObject("schema", jsonSchema);		
			model.addObject("days", jsonDays);
			model.addObject("listLanguages", jsonLanguages);

		} catch (ViaLivreGUIException e) {
			// Se redirecciona al jsp de error
			model = new ModelAndView(ViaLivreGUIConstants.ERROR_PAGE_REDIRECT);
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			model.addObject("errors", errors);
			LOGGER.error(Messages.getString("error.redirectsearch.aggregate", e.getThrowExceptionClassName()), e);
		} catch (Exception e) {
			// Se redirecciona al jsp de error
			model = new ModelAndView(ViaLivreGUIConstants.ERROR_PAGE_REDIRECT);
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			model.addObject("errors", errors);
			LOGGER.error(Messages.getString("error.redirectsearch.aggregate", this.getClass().getName()), e);
		}
		if (LOGGER.isDebugEnabled()){
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			LOGGER.debug(Messages.getString("debug.return", stack.length < 2 ? "" : stack[1].getMethodName(), model.getClass().getName(), model ));
		}
		return model;
	}

	/**
	 * Realiza la acción de búsqueda de agregados en la pantalla de búsqueda de
	 * agregados. Retorna a la vista un JSON con los resultados de la búsqueda
	 * 
	 * @param aggregateSearchForm
	 * @param result
	 * @return JsonResponse
	 */
	@PreAuthorize("hasAuthority('ag.read')")
	@RequestMapping(value = "/searchAggregate", method = RequestMethod.POST)
	public @ResponseBody JsonResponse search(@Valid @ModelAttribute("aggregateSearchForm") AggregateSearchForm aggregateSearchForm, BindingResult result) {
		Long inicioEjecucion = System.currentTimeMillis();
		VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, aggregateSearchForm);
		
		aggregateValidator.validate(aggregateSearchForm, result);

		// Se comprueban los posibles errores en la validacion del formulario
		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
		try {
			if (!result.hasErrors()) {
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
				List<AggregateSearchResponse> aggregateSearch = aggregateService.getSearchAggregate(aggregateSearchForm);
				jsonResponse.setResult(aggregateSearch);
			} else {
				jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
				List<FieldError> fieldErrors = result.getFieldErrors();
				for (FieldError fieldError : fieldErrors) {
					String[] resolveMessageCodes = result.resolveMessageCodes(fieldError.getCode());
					if (resolveMessageCodes.length > 0) {
						String message = resolveMessageCodes[resolveMessageCodes.length - 1];
						errors.put(fieldError.getField(), message);
					}
				}
				jsonResponse.setResult(errors);
			}
			jsonResponse.setEjecutionTime(VialivreUtility.msgRunTime(inicioEjecucion));
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.search.aggregate", e.getThrowExceptionClassName()), e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.search.aggregate", this.getClass().getName()), e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.TRACE, jsonResponse);
		return jsonResponse;
	}

	/**
	 * Reliza la acción de pasar a enviado un determinado documento agregado
	 * 
	 * @param oidAggregate
	 * @return JsonResponse
	 */
	@PreAuthorize("hasPermission(#oidAggregate, 'vialivre.Aggregate', 'ag.send')")
	@RequestMapping(value = "/sendAggregate", method = RequestMethod.POST)
	public @ResponseBody JsonResponse sendAggregate(@P("oidAggregate") @RequestParam(value = "oidAggregate", required = true) Long oidAggregate) {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, oidAggregate);
		
		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
		try {
			VirtualDocumentWSResponse virtualDocumentWSResponse = aggregateService.sendAggr(oidAggregate);

			if (virtualDocumentWSResponse.getResult().equals(ViaLivreGUIConstants.WS_OK_RESULT)) {
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
				jsonResponse.setResult(oidAggregate);
			} else {
				if (virtualDocumentWSResponse.getResult().equals(ViaLivreGUIConstants.WS_KO_RESULT)) {
					jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
					Integer size = virtualDocumentWSResponse.getCodes().size();
					for (int i = 0; i < size; i++) {
						String message = virtualDocumentWSResponse.getCodes().get(i) + "." + virtualDocumentWSResponse.getMessages().get(i);
						errors.put(virtualDocumentWSResponse.getMessages().get(i), message);
					}
					jsonResponse.setResult(errors);
				}
			}
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.send.aggregate", e.getThrowExceptionClassName(), oidAggregate), LOGGER, e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.send.aggregate", this.getClass().getName(), oidAggregate), LOGGER, e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
		return jsonResponse;
	}

	/**
	 * Realiza los chequeos necesarios para visualizar los botones de acciones
	 * en la pantalla de gestión de agregados para un determinado documento
	 * 
	 * @param oidAggregate
	 * @return JsonResponse
	 */
	@PreAuthorize("hasAuthority('ag.read')")
	@RequestMapping(value = "/checkActionsAggregate", method = RequestMethod.POST)
	public @ResponseBody JsonResponse checkActionsAggregate(@P("oidAggregate") @RequestParam(value = "oidAggregate", required = true) Long oidAggregate) {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, oidAggregate);
		
		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
		try {
			ActionsAggregateResponse actionsAggregateResponse = aggregateService.checkActionsAggregate(oidAggregate);
			jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
			jsonResponse.setResult(actionsAggregateResponse);
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.checkactions.aggregate", e.getThrowExceptionClassName(), oidAggregate), LOGGER, e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.checkactions.aggregate", this.getClass().getName(), oidAggregate), LOGGER, e);
		}
		return jsonResponse;
	}
	
	/**
	 * 
	 * @param oidAggregate
	 * @param statusAggregateId
	 * @param reasonCancelAggregate
	 * @return JsonResponse
	 */
	@PreAuthorize("hasPermission(#oidAggregate, 'vialivre.Aggregate', 'ag.cancel')")
	@RequestMapping(value = "/cancelAggregate", method = RequestMethod.POST)
	public @ResponseBody JsonResponse cancelAggregate(@P("oidAggregate") @RequestParam(value = "oidAggregate", required = true) Long oidAggregate,
			@RequestParam(value = "statusAggregateId", required = true) String statusAggregateId, @RequestParam(value = "reasonCancelAggregate", required = true) Integer reasonCancelAggregate) {
		VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, 
                (null != oidAggregate ? oidAggregate.getClass().getName() : null) 
                    + " - " + (null != statusAggregateId ? statusAggregateId.getClass().getName() : null) 
					+ " - " + (null != reasonCancelAggregate ? reasonCancelAggregate.getClass().getName() : null) 
                    + oidAggregate + " - " + statusAggregateId + " - " + reasonCancelAggregate);
		
		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
		try{
			VirtualDocumentWSResponse virtualDocumentWSResponse = aggregateService.cancelAggr(oidAggregate,reasonCancelAggregate);	
			if (virtualDocumentWSResponse.getResult().equals(ViaLivreGUIConstants.WS_OK_RESULT)) {
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
				jsonResponse.setResult(oidAggregate);
			} else {
				if (virtualDocumentWSResponse.getResult().equals(ViaLivreGUIConstants.WS_KO_RESULT)) {
					jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
					Integer size = virtualDocumentWSResponse.getCodes().size();
					for (int i = 0; i < size; i++) {
						String message = virtualDocumentWSResponse.getCodes().get(i) + "_" + virtualDocumentWSResponse.getMessages().get(i);
						errors.put(virtualDocumentWSResponse.getMessages().get(i), message);
					}
					jsonResponse.setResult(errors);
				}
			}
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.cancel.aggregate", e.getThrowExceptionClassName(), oidAggregate), LOGGER, e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.cancel.aggregate", this.getClass().getName(), oidAggregate), LOGGER, e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
		return jsonResponse;
	}
	
	/**
	 * 
	 * @param oidAggregate
	 * @param statusAggregateId
	 * @param collectionDate
	 * @param idAggregate
	 * @return JsonResponse
	 */
	@PreAuthorize("hasAuthority('ag.pay')")
	@RequestMapping(value = "/validateAggregateDocuments", method = RequestMethod.POST)
	public @ResponseBody JsonResponse validateAggregateDocuments(@P("oidAggregate") @RequestParam(value = "oidAggregate", required = true) Long oidAggregate,
			@RequestParam(value = "statusAggregateId", required = true) String statusAggregateId,
			@RequestParam(value = "collectionDate", required = true) Date collectionDate,
			@RequestParam(value = "idAggregate", required = true) String idAggregate) {
		VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, 
	            (null != oidAggregate ? oidAggregate.getClass().getName() : null) 
                + " - " + (null != statusAggregateId ? statusAggregateId.getClass().getName() : null)
                + " - " + (null != collectionDate ? collectionDate.getClass().getName() : null)
                + " - " + (null != idAggregate ? idAggregate.getClass().getName() : null)
                + oidAggregate + " - " + statusAggregateId + " - " + collectionDate + " - " + idAggregate);
		
		JsonResponse jsonResponse = new JsonResponse();

		Map<String, String> errors = new HashMap<String, String>();
		
		try{
			List<DebtDocsSearchResponse> list = new ArrayList<DebtDocsSearchResponse>();
			DebtDocsSearchResponse debtDocsSearchResponse= new DebtDocsSearchResponse();
			debtDocsSearchResponse.setIdDebtDoc(idAggregate);
			debtDocsSearchResponse.setTypeDebtDoc("AG");
			List<VirtualDocumentNotifSearchResponse> listVdoc =  virtualDocumentService.searchVDocNotifByAggregate(oidAggregate);
			Integer concession = ViaLivreGUIConstants.SESSION_TEMPLATE_NLI;
			 for(VirtualDocumentNotifSearchResponse doc: listVdoc){
				 concession = doc.getConcesion();
			 }	
			debtDocsSearchResponse.setConcesion(concession);
			list.add(debtDocsSearchResponse);
			
			List<es.indra.www._2013._06.freeflow.webservices.architecture.Error> errorList = frontOfficeService.validateDebtDoc(list);
			jsonResponse.setResult(errorList);
			if (errorList.isEmpty()) {
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
			} else {
				jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			}
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.validate.debtDocument", e.getThrowExceptionClassName(), null), e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.validate.debtDocument", this.getClass().getName(), null), e);
		}

		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
		return jsonResponse;
	}
	
	/**
	 * 
	 * @param oidAggregate
	 * @param statusAggregateId
	 * @param collectionDate
	 * @param methodPayment
	 * @return JsonResponse
	 */
	@PreAuthorize("hasAuthority('ag.pay')")
	@RequestMapping(value = "/paymentAggregate", method = RequestMethod.POST)
	public @ResponseBody JsonResponse paymentAggregate(@P("oidAggregate") @RequestParam(value = "oidAggregate", required = true) Long oidAggregate,
			@RequestParam(value = "statusAggregateId", required = true) String statusAggregateId,
			@RequestParam(value = "collectionDate", required = true) Date collectionDate,
			@RequestParam(value = "methodPayment", required = true) Integer methodPayment) {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, 
	            (null != oidAggregate ? oidAggregate.getClass().getName() : null) 
                + " - " + (null != statusAggregateId ? statusAggregateId.getClass().getName() : null)
                + " - " + (null != collectionDate ? collectionDate.getClass().getName() : null)
                + " - " + (null != methodPayment ? methodPayment.getClass().getName() : null)
                + oidAggregate + " - " + statusAggregateId + " - " + collectionDate + " - " + methodPayment);
		
		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
		try{
			VirtualDocumentWSResponse virtualDocumentWSResponse = null;	 
			if (collectionDate==null){
				virtualDocumentWSResponse = aggregateService.paymentAggr(oidAggregate, methodPayment,null);
			} else {				
				LogUtility.writeDebugLog("collectionDate: " + collectionDate, LOGGER);
				virtualDocumentWSResponse = aggregateService.paymentAggr(oidAggregate, methodPayment,collectionDate);
			}
			if (virtualDocumentWSResponse.getResult().equals(ViaLivreGUIConstants.WS_OK_RESULT)) {
				AggregateDTO aggregateDTO = aggregateService.getAggregateFromOid(oidAggregate);
				jsonResponse.setResult(aggregateDTO);
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
			} else {
				if (virtualDocumentWSResponse.getResult().equals(ViaLivreGUIConstants.WS_KO_RESULT)) {
					jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
					Integer size = virtualDocumentWSResponse.getCodes().size();
					for (int i = 0; i < size; i++) {
						String message = virtualDocumentWSResponse.getCodes().get(i) + "_" + virtualDocumentWSResponse.getMessages().get(i);
						errors.put(virtualDocumentWSResponse.getMessages().get(i), message);
					}	
					jsonResponse.setResult(errors);
				}
			}
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.payment.aggregate", e.getThrowExceptionClassName(), oidAggregate), LOGGER, e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.payment.aggregate", this.getClass().getName(), oidAggregate), LOGGER, e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
		return jsonResponse;
	}
	
	/**
	 * 
	 * @param oidAggregate
	 * @param statusAggregateId
	 * @return JsonResponse
	 * @throws ViaLivreGUIException
	 */
	@PreAuthorize("hasPermission(#oidAggregate, 'vialivre.Aggregate', 'ag.issue')")
	@RequestMapping(value = "/issueNTAggregate", method = RequestMethod.POST)
	public @ResponseBody JsonResponse issueNTAggregate(@P("oidAggregate") @RequestParam(value = "oidAggregate", required = true) Long oidAggregate,
			@RequestParam(value = "statusAggregateId", required = true) String statusAggregateId) throws ViaLivreGUIException {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, 
	            (null != oidAggregate ? oidAggregate.getClass().getName() : null) 
					+ " - " + (null != statusAggregateId ? statusAggregateId.getClass().getName() : null)
					+ oidAggregate + " - " + statusAggregateId);
		
		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
		
		AggregateDTO aggregateDTO = aggregateService.getAggregateFromOid(oidAggregate);		
		ClientErpSapDTO clientERPSAP = aggregateService.getClientERPSAP(aggregateDTO.getIdDocument());
		String clientERP = null;
		String clientSAP = null;
		if (clientERPSAP != null){
			if (clientERPSAP.getIdERP()!=null){
			clientERP = clientERPSAP.getIdERP();
			} else {
				clientERP = "";
			}
			if (clientERPSAP.getIdSAP()!=null){
				clientSAP = clientERPSAP.getIdSAP();
			} else {
				clientSAP = "";
			}
		} else {
			clientERP = "";
			clientSAP = "";
		}
		
		try{
			VirtualDocumentWSResponse virtualDocumentWSResponse = null;			
			List<AggregateInvoiceDTO> listAggregateInvoice = new ArrayList<AggregateInvoiceDTO>();			
			// Se llama al WS de Emission con la operación 2 correspondiente a Emitir. 1 -> Grabar, 2 -> Emitir
			virtualDocumentWSResponse = aggregateService.issueAggr(oidAggregate, listAggregateInvoice,ViaLivreGUIConstants.EMISSION_AGGREGATE,clientERP,clientSAP);	
			if (ViaLivreGUIConstants.WS_OK_RESULT.equals(virtualDocumentWSResponse.getResult())) {
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
				AggregateSearchResponse aggregateSearchResponse = null;
				aggregateSearchResponse = AggregateUtility.aggregateDTOToAggregateSearchResponse(aggregateService.getAggregateFromOid(oidAggregate));
				jsonResponse.setResult(aggregateSearchResponse);				
			} else {
				if (ViaLivreGUIConstants.WS_KO_RESULT.equals(virtualDocumentWSResponse.getResult())) {
					jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);	
					Integer size = virtualDocumentWSResponse.getCodes().size();
					for (int i = 0; i < size; i++) {
						String message = virtualDocumentWSResponse.getCodes().get(i) + "_" + virtualDocumentWSResponse.getMessages().get(i);
						errors.put(virtualDocumentWSResponse.getMessages().get(i), message);
					}	
					jsonResponse.setResult(errors);
				}
			}
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.issue.aggregate", e.getThrowExceptionClassName(), oidAggregate), LOGGER, e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.issue.aggregate", this.getClass().getName(), oidAggregate), LOGGER, e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
		return jsonResponse;
	}

	/**
	 * 
	 * @param documentNum
	 * @param concatenateDVs
	 * @param payment
	 * @return ResponseEntity<byte[]>
	 */
	@PreAuthorize("hasAuthority('ag.read')")
	@RequestMapping(value = "/downloadAGIssuePDF")
	public ResponseEntity<byte[]> downloadAGIssuePDF(@RequestParam(value = "documentNum", required = true) String documentNum,
			@RequestParam(value = "concatenateDVs", required = false) String concatenateDVs, 
			@RequestParam(value = "payment", required = false) String payment) {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, 
	            (null != documentNum ? documentNum.getClass().getName() : null) 
					+ " - " + (null != concatenateDVs ? concatenateDVs.getClass().getName() : null)
					+ " - " + (null != payment ? payment.getClass().getName() : null)
					+ documentNum + " - " + concatenateDVs + " - " + payment);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		headers.set("Content-type","application/x-download");
		ResponseEntity<byte[]> response = null;
		Boolean concatenate = null;
		Boolean pdfPayment = null;
		try {
			concatenate = Boolean.parseBoolean(concatenateDVs);		
			pdfPayment = Boolean.parseBoolean(payment);	
		} catch (Exception e){
			LogUtility.writeErrorLog(Messages.getString("error.downloadissue.aggregate", this.getClass().getName(), documentNum), LOGGER, e);
			response = new ResponseEntity<byte[]>(null, headers, HttpStatus.BAD_REQUEST);
		}
		
		try {
		    ReportDataDTO pdfData = aggregateService.generatePdfAg(documentNum, concatenate, pdfPayment);
			if (null != pdfData){
				byte[] newContents = pdfData.getFileData();
				headers.set("Content-disposition", "attachment; filename=" + pdfData.getFileName());
				headers.set("Set-Cookie", "fileDownload=true; path=/");

				response = new ResponseEntity<byte[]>(newContents, headers, HttpStatus.OK);
			} else {
				LogUtility.writeErrorLog(Messages.getString("error.downloadissue.aggregate", this.getClass().getName(), documentNum), LOGGER);
				response = new ResponseEntity<byte[]>(null, headers, HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			LogUtility.writeErrorLog(Messages.getString("error.downloadissue.aggregate", this.getClass().getName(), documentNum), LOGGER, e);
			response = new ResponseEntity<byte[]>(null, headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, response);
		return response;
	}
	
	/**
	 * 
	 * @param documentNum
	 * @return ResponseEntity<byte[]>
	 */
	@PreAuthorize("hasAuthority('ag.read')")
	@RequestMapping(value = "/downloadAGApprovalPendingPDF")
	public ResponseEntity<byte[]> downloadAGApprovalPendingPDF(@RequestParam(value = "documentNum", required = true) String documentNum) {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, 
	            (null != documentNum ? documentNum.getClass().getName() : null));
		
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		headers.set("Content-type","application/x-download");
		ResponseEntity<byte[]> response = null;
		
		try {
		    ReportDataDTO pdfData = aggregateService.generateApprovalPdfAg(documentNum);

			if (null != pdfData){
				byte[] newContents = pdfData.getFileData();
				headers.set("Content-disposition", "attachment; filename=" + pdfData.getFileName());
				headers.set("Set-Cookie", "fileDownload=true; path=/");

				response = new ResponseEntity<byte[]>(newContents, headers, HttpStatus.OK);
			} else {
				LogUtility.writeErrorLog(Messages.getString("error.downloadapprovalpending.aggregate", this.getClass().getName(), documentNum), LOGGER);
				response = new ResponseEntity<byte[]>(null, headers, HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			LogUtility.writeErrorLog(Messages.getString("error.downloadapprovalpending.aggregate", this.getClass().getName(), documentNum), LOGGER, e);
			response = new ResponseEntity<byte[]>(null, headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, response);
		return response;
	}

	/**
	 * 
	 * @param aggregateInvoiceForm
	 * @return JsonResponse
	 */
	@PreAuthorize("hasPermission(#aggregateInvoiceForm.oidAggregate, 'vialivre.Aggregate', 'ag.ap.issue') or hasPermission(#aggregateInvoiceForm.oidAggregate, 'vialivre.Aggregate', 'ag.issue')")
	@RequestMapping(value = "/issueAggregate", method = RequestMethod.POST)
	public @ResponseBody JsonResponse issueAggregate(@P("aggregateInvoiceForm") @ModelAttribute("aggregateInvoiceForm") AggregateInvoiceForm aggregateInvoiceForm) {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, aggregateInvoiceForm);
		
		JsonResponse jsonResponse = new JsonResponse();

		Map<String, String> errors = new HashMap<String, String>();
		Long oidAggregate = null;
		try{
			List<AggregateInvoice> lstAggregateInvoice = aggregateInvoiceForm.getLstAggregateInvoice();
			oidAggregate = aggregateInvoiceForm.getOidAggregate();
			Integer typeIssue = aggregateInvoiceForm.getTypeIssue();
			String clientERP = aggregateInvoiceForm.getIdClientERP();
			String clientSAP = aggregateInvoiceForm.getIdClientSAP();
			List<AggregateInvoiceDTO> listAggregateInvoice = new ArrayList<AggregateInvoiceDTO>();
			for (AggregateInvoice aggregateInvoice : lstAggregateInvoice) {
				 AggregateInvoiceDTO aggregateInvoiceDTO = AggregateUtility.agregateInvoiceToAggregateInvoiceDTO(aggregateInvoice);	
				listAggregateInvoice.add(aggregateInvoiceDTO);
			}
			
			VirtualDocumentWSResponse virtualDocumentWSResponse = aggregateService.issueAggr(oidAggregate, listAggregateInvoice,typeIssue,clientERP,clientSAP);
		
			if (virtualDocumentWSResponse.getMessages().isEmpty()) {
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
				 AggregateSearchResponse aggregateSearchResponse = AggregateUtility.aggregateDTOToAggregateSearchResponse(aggregateService.getAggregateFromOid(oidAggregate));
				jsonResponse.setResult(aggregateSearchResponse);
			} else {
				jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
				Integer size = virtualDocumentWSResponse.getCodes().size();
				for (int i = 0; i < size; i++) {
					String message = virtualDocumentWSResponse.getCodes().get(i) + "_" + virtualDocumentWSResponse.getMessages().get(i);
					errors.put(virtualDocumentWSResponse.getMessages().get(i), message);
				}
				jsonResponse.setResult(errors);
			}
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.issue.aggregate", e.getThrowExceptionClassName(), oidAggregate), e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.issue.aggregate", this.getClass().getName(), oidAggregate), e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
		return jsonResponse;
	}
	
	/**
	 * 
	 * @param aggregateDDIssueForm
	 * @return JsonResponse
	 */
    @PreAuthorize("hasPermission(#aggregateDDIssueForm.oidAggregate, 'vialivre.Aggregate', 'ag.issue')")
    @RequestMapping(value = "/issueDDAggregate", method = RequestMethod.POST)
    public @ResponseBody JsonResponse issueDDAggregate(@P("aggregateDDIssueForm") @ModelAttribute("aggregateDDIssueForm") AggregateDDIssueDTO aggregateDDIssueForm) {
        VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, aggregateDDIssueForm);
        
        JsonResponse jsonResponse = new JsonResponse();

        Map<String, String> errors = new HashMap<String, String>();
        Long oidAggregate = null;
        try{
            oidAggregate = aggregateDDIssueForm.getOidAggregate();
            Integer meanOfPayment = aggregateDDIssueForm.getMeanOfPayment();
            Long bankId = aggregateDDIssueForm.getBankId();
            String bankAccount = aggregateDDIssueForm.getBankAccount();
            
            VirtualDocumentWSResponse virtualDocumentWSResponse = aggregateService.issueDDAggr(oidAggregate, ViaLivreGUIConstants.EMISSION_DD_AGGREGATE, meanOfPayment, bankId, bankAccount );
        
            if (virtualDocumentWSResponse.getMessages().isEmpty()) {
                jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
                 AggregateSearchResponse aggregateSearchResponse = AggregateUtility.aggregateDTOToAggregateSearchResponse(aggregateService.getAggregateFromOid(oidAggregate));
                jsonResponse.setResult(aggregateSearchResponse);
            } else {
                jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
                Integer size = virtualDocumentWSResponse.getCodes().size();
                for (int i = 0; i < size; i++) {
                    String message = virtualDocumentWSResponse.getCodes().get(i) + "_" + virtualDocumentWSResponse.getMessages().get(i);
                    errors.put(virtualDocumentWSResponse.getMessages().get(i), message);
                }
                jsonResponse.setResult(errors);
            }
        } catch (ViaLivreGUIException e) {
            errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
            jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
            jsonResponse.setResult(errors);
            LOGGER.error(Messages.getString("error.issue.aggregate", e.getThrowExceptionClassName(), oidAggregate), e);
        } catch (Exception e) {
            errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
            jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
            jsonResponse.setResult(errors);
            LOGGER.error(Messages.getString("error.issue.aggregate", this.getClass().getName(), oidAggregate), e);
        }
        
        VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
        return jsonResponse;
    }
	
    /**
     * 
     * @param aggregateInvoiceForm
     * @return JsonResponse
     */
    @PreAuthorize("hasAnyAuthority('ag.issue', 'ag.pay')")
    @RequestMapping(value = "/validateAggregate", method = RequestMethod.POST)
    public @ResponseBody JsonResponse validateAggregate(@P("aggregateInvoiceForm") @ModelAttribute("aggregateInvoiceForm") AggregateInvoiceForm aggregateInvoiceForm) {
        VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, aggregateInvoiceForm);
        
        JsonResponse jsonResponse = new JsonResponse();

        Map<String, String> errors = new HashMap<String, String>();
        Long oidAggregate = null;
        try{
            oidAggregate = aggregateInvoiceForm.getOidAggregate();
            List<es.indra.www._2013._06.freeflow.webservices.architecture.Error> errorList = new ArrayList<es.indra.www._2013._06.freeflow.webservices.architecture.Error>();
            aggregateService.validateAggregate(oidAggregate, errorList);
        
            jsonResponse.setResult(errorList);
            if (errorList.isEmpty()) {
                jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
            } else {
                jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
            }
        } catch (ViaLivreGUIException e) {
            errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
            jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
            jsonResponse.setResult(errors);
            LOGGER.error(Messages.getString("error.validate.aggregate", e.getThrowExceptionClassName(), oidAggregate), e);
        } catch (Exception e) {
            errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
            jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
            jsonResponse.setResult(errors);
            LOGGER.error(Messages.getString("error.validate.aggregate", this.getClass().getName(), oidAggregate), e);
        }
        
        VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
        return jsonResponse;
    }
    
    /**
     * 
     * @param oidAggregate
     * @param statusAggregateId
     * @return JsonResponse
     * @throws ViaLivreGUIException
     */
	@PreAuthorize("hasPermission(#oidAggregate, 'vialivre.Aggregate', 'ag.ap.issue') or hasPermission(#oidAggregate, 'vialivre.Aggregate', 'ag.issue')")
	@RequestMapping(value = "/forwardingRefSIBS", method = RequestMethod.POST)
	public @ResponseBody JsonResponse forwardingRefSIBS(@P("oidAggregate") @RequestParam(value = "oidAggregate", required = true) Long oidAggregate,
			@RequestParam(value = "statusAggregateId", required = true) String statusAggregateId) throws ViaLivreGUIException {
	    VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, 
	            (null != oidAggregate ? oidAggregate.getClass().getName() : null) 
					+ " - " + (null != statusAggregateId ? statusAggregateId.getClass().getName() : null)
					+ oidAggregate + " - " + statusAggregateId);
		
		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
		
		AggregateDTO aggregateDTO = aggregateService.getAggregateFromOid(oidAggregate);		
		ClientErpSapDTO clientERPSAP = aggregateService.getClientERPSAP(aggregateDTO.getIdDocument());
		String clientERP = null;
		String clientSAP = null;
		if (clientERPSAP != null){
			clientERP = clientERPSAP.getIdERP();
			clientSAP = clientERPSAP.getIdSAP();
		} else {
			clientERP = "";
			clientSAP = "";
		}
		
		try{
			VirtualDocumentWSResponse virtualDocumentWSResponse = null;			
			List<AggregateInvoiceDTO> listAggregateInvoice = new ArrayList<AggregateInvoiceDTO>();		
			


			// Se llama al WS de Emission con la operación 2 correspondiente a Emitir. 1 -> Grabar, 2 -> Emitir, 3 -> Reenviar Referencias SIBS
			virtualDocumentWSResponse = aggregateService.issueAggr(oidAggregate, listAggregateInvoice,ViaLivreGUIConstants.FORWARDING_SIBS_AGGREGATE,clientERP,clientSAP);	
			if (ViaLivreGUIConstants.WS_OK_RESULT.equals(virtualDocumentWSResponse.getResult())) {
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
				AggregateSearchResponse aggregateSearchResponse = null;
				aggregateSearchResponse = AggregateUtility.aggregateDTOToAggregateSearchResponse(aggregateService.getAggregateFromOid(oidAggregate));
				jsonResponse.setResult(aggregateSearchResponse);
			} else {
				if (ViaLivreGUIConstants.WS_KO_RESULT.equals(virtualDocumentWSResponse.getResult())) {
					jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);	
					Integer size = virtualDocumentWSResponse.getCodes().size();
					for (int i = 0; i < size; i++) {
						String message = virtualDocumentWSResponse.getCodes().get(i) + "_" + virtualDocumentWSResponse.getMessages().get(i);
						errors.put(virtualDocumentWSResponse.getMessages().get(i), message);
					}	
					jsonResponse.setResult(errors);
				}
			}
		} catch (ViaLivreGUIException e) {
			errors.put(e.getThrowExceptionClassName(), e.getMessageLog());
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.issue.aggregate", e.getThrowExceptionClassName(), oidAggregate), LOGGER, e);
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LogUtility.writeErrorLog(Messages.getString("error.issue.aggregate", this.getClass().getName(), oidAggregate), LOGGER, e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, jsonResponse);
		return jsonResponse;
	}
	
	/**
	 * 
	 * @param agSearchForm
	 * @param result
	 * @return JsonResponse
	 * @throws ViaLivreGUIException
	 */
	@RequestMapping(value = "/validateGenerateExportAg", method = RequestMethod.POST)
	public @ResponseBody JsonResponse validateGenerateExportAg(@Valid @ModelAttribute("aggregateSearchForm") AggregateSearchForm agSearchForm, BindingResult result) throws ViaLivreGUIException {
		Long inicioEjecucion = System.currentTimeMillis();
		VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, agSearchForm);
		
		//Validar campos obligatorios
		aggregateValidator.validateAggregateXLS(agSearchForm, result);
		
		// Se comprueban los posibles errores en la validacion del formulario
		JsonResponse jsonResponse = new JsonResponse();
		Map<String, String> errors = new HashMap<String, String>();
				
		try {
			if (!result.hasErrors()) {
				jsonResponse.setResult(ViaLivreGUIConstants.SUCCESS_RESPONSE);
				jsonResponse.setStatus(ViaLivreGUIConstants.SUCCESS_RESPONSE);
			} else {
				jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
				List<FieldError> fieldErrors = result.getFieldErrors();
				for (FieldError fieldError : fieldErrors) {
					String[] resolveMessageCodes = result.resolveMessageCodes(fieldError.getCode());
					if (resolveMessageCodes.length > 0) {
						String message = resolveMessageCodes[resolveMessageCodes.length - 1];
						errors.put(fieldError.getField(), message);
					}
				}
				jsonResponse.setResult(errors);
			}
			jsonResponse.setEjecutionTime(VialivreUtility.msgRunTime(inicioEjecucion));
		} catch (Exception e) {
			errors.put(this.getClass().getName(), ViaLivreGUIConstants.GENERAL_ERROR);
			jsonResponse.setStatus(ViaLivreGUIConstants.FAIL_RESPONSE);
			jsonResponse.setResult(errors);
			LOGGER.error(Messages.getString("error.search.aggregate", this.getClass().getName()), e);
		}
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.TRACE, jsonResponse);
		return jsonResponse;
	}
	
	/**
	 * 
	 * @param agSearchForm
	 * @param result
	 * @return ResponseEntity<byte[]>
	 * @throws ViaLivreGUIException
	 */
	@PreAuthorize("hasAuthority('ag.read')")
	@RequestMapping(value = "/generateExportAg", method = RequestMethod.POST)
	public ResponseEntity<byte[]> generateExportAg (@Valid @ModelAttribute("aggregateSearchForm") AggregateSearchForm agSearchForm, BindingResult result) throws ViaLivreGUIException {
		VialivreUtility.paramLog(LOGGER, ViaLivreGUIConstants.DEBUG, agSearchForm);	
		
		//Validar campos obligatorios
		aggregateValidator.validateAggregateXLS(agSearchForm, result);
		
		HttpHeaders headers = new HttpHeaders();		
		headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		headers.set("Content-type","application/x-download");
		ResponseEntity<byte[]> response = null;
		MultiValueMap<String, String> errors = new LinkedMultiValueMap<String, String>();
		 
		  try {
			  
			 if (!result.hasErrors()) {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				String userName = auth.getName();
				ReportDataDTO xlsData = aggregateService.generateExportAggregate(agSearchForm, userName); 
				if (null != xlsData){
					byte[] newContents = xlsData.getFileData();
					headers.set("Content-disposition", "attachment; filename=" + xlsData.getFileName());
					headers.set("Set-Cookie", "fileDownload=true; path=/");
					response = new ResponseEntity<byte[]>(newContents, headers, HttpStatus.OK);	
					
				} else {
					LOGGER.error(Messages.getString("error.downloadxlsaggregate", this.getClass().getName(), 1));
					response = new ResponseEntity<byte[]>(null, headers, HttpStatus.NOT_FOUND);
				}
			} else {				
				List<FieldError> fieldErrors = result.getFieldErrors();
				for (FieldError fieldError : fieldErrors) {
					String[] resolveMessageCodes = result.resolveMessageCodes(fieldError.getCode());
					if (resolveMessageCodes.length > 0) {
						List<String> message = new ArrayList<String>();
						message.add(resolveMessageCodes[resolveMessageCodes.length - 1]);
						errors.put(fieldError.getField(), message);
					}
				}	
				LOGGER.error(Messages.getString("error.downloadxlsaggregate", this.getClass().getName(), 1));
				response = new ResponseEntity<byte[]>(null, headers, HttpStatus.NOT_FOUND);	
			}

		} catch (Exception e) {
			LOGGER.error(Messages.getString("error.downloadxlsaggregate", this.getClass().getName(), 1), e);
			response = new ResponseEntity<byte[]>(null, headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		 
		
		VialivreUtility.returnLog(LOGGER, ViaLivreGUIConstants.DEBUG, "PROPOSAL");
		return response;
	}
	
	/**
	 * 
	 * @param dataParamService
	 */
	public void setDataParamService(DataParamService dataParamService) {
		this.dataParamService = dataParamService;
	}
	
	/**
	 * 
	 * @return AggregateValidator
	 */
	public AggregateValidator getAggregateValidator() {
		return aggregateValidator;
	}

	/**
	 * 
	 * @param aggregateValidator
	 */
	public void setAggregateValidator(AggregateValidator aggregateValidator) {
		this.aggregateValidator = aggregateValidator;
	}
	
	/**
	 * 
	 * @return ApprovalPendingValueValidator
	 */
	public ApprovalPendingValueValidator getApprovalPendingValueValidator() {
		return approvalPendingValueValidator;
	}

	/**
	 * 
	 * @param approvalPendingValueValidator
	 */
	public void setApprovalPendingValueValidator(ApprovalPendingValueValidator approvalPendingValueValidator) {
		this.approvalPendingValueValidator = approvalPendingValueValidator;
	}

}
