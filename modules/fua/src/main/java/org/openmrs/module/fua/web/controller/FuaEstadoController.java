package org.openmrs.module.fua.web.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.fua.FuaEstado;
import org.openmrs.module.fua.api.FuaEstadoService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/module/fua/estado")
public class FuaEstadoController {

  private static final String FORM_VIEW = "/module/fua/pages/addFuaEstado";
  private static final String OPENMRS_MSG_ATTR = "openmrs_msg";
  private static final String OPENMRS_ERROR_ATTR = "openmrs_error";

  protected final Log log = LogFactory.getLog(getClass());

  private final FuaEstadoService fuaEstadoService;

  public FuaEstadoController(FuaEstadoService fuaEstadoService) {
    this.fuaEstadoService = fuaEstadoService;
  }

  @RequestMapping(method = RequestMethod.GET)
  public String onGet(
      ModelMap model, @RequestParam(value = "estadoId", required = false) Integer estadoId) {
    FuaEstado estado = estadoId != null ? fuaEstadoService.getEstado(estadoId) : new FuaEstado();
    model.addAttribute("estado", estado);
    model.addAttribute("estados", fuaEstadoService.getAllEstados());
    return FORM_VIEW;
  }

  @RequestMapping(method = RequestMethod.POST)
  public String onPost(
      HttpSession httpSession,
      @ModelAttribute("estado") FuaEstado estado,
      BindingResult errors,
      @RequestParam(required = false, value = "action") String action) {
    if (errors.hasErrors()) {
      return FORM_VIEW;
    }

    if (!Context.isAuthenticated()) {
      errors.reject("fua.auth.required");
    } else if ("purge".equals(action)) {
      try {
        fuaEstadoService.purgeEstado(estado);
        httpSession.setAttribute(OPENMRS_MSG_ATTR, "fua.estado.delete.success");
      } catch (Exception ex) {
        httpSession.setAttribute(OPENMRS_ERROR_ATTR, "fua.estado.delete.failure");
        log.error("Error deleting FuaEstado", ex);
      }
    } else {
      fuaEstadoService.saveEstado(estado);
      httpSession.setAttribute(OPENMRS_MSG_ATTR, "fua.estado.saved");
    }
    return "redirect:/module/fua/estado";
  }

  @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<FuaEstado> getAllEstados() {
    return fuaEstadoService.getAllEstados();
  }

  @RequestMapping(
      value = "/create",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public FuaEstado createEstado(@RequestBody FuaEstado nuevoEstado) {
    return fuaEstadoService.saveEstado(nuevoEstado);
  }
}
