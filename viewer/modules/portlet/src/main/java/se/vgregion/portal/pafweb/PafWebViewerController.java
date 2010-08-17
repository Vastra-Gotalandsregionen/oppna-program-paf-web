/**
 * Copyright 2010 Västra Götalandsregionen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public
 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *   Boston, MA 02111-1307  USA
 *
 */

package se.vgregion.portal.pafweb;

import java.util.Map;

import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;

import se.vgregion.portal.auditlog.AuditLogInfoContainer;
import se.vgregion.portal.auditlog.AuditLogInfoContainerFactory;
import se.vgregion.portal.auditlog.PatientAccessAuditLogger;
import se.vgregion.portal.patient.event.PatientEvent;
import se.vgregion.portal.patient.event.PersonNummer;

/**
 * This action do that and that, if it has something special it is.
 * 
 * @author <a href="mailto:david.rosell@redpill-linpro.com">David Rosell</a>
 */

@Controller
@RequestMapping("VIEW")
@SessionAttributes("patient")
public class PafWebViewerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PafWebViewerController.class);

    /**
     * jsp name.
     */
    public static final String VIEW_JSP = "view";
    /**
     * jsp name.
     */
    public static final String JUMPOUT_JSP = "jumpout";

    @Autowired
    private AuditLogInfoContainerFactory auditLogInfoContainerFactory;

    @Autowired
    private PatientAccessAuditLogger auditLogger;

    @Value("${paf.url}")
    String pafUrl;

    @Value("${paf.mode}")
    String pafMode;

    @Value("${paf.access.security.level}")
    String pafAccessSecurityLevel;

    @Value("${paf.access.mode}")
    String pafAccessMode;

    /**
     * First splash screen.
     * 
     * @param model
     *            Model map
     * @return view path
     */
    @RenderMapping
    public String view(ModelMap model) {
        if (!model.containsKey("patient")) {
            model.addAttribute("patient", new PatientEvent(""));
        }

        return VIEW_JSP;
    }

    /**
     * Jumpout screen for the PafWeb web application.
     * 
     * @param request
     *            to access current user
     * @param model
     *            Model map
     * @return jumpout screen path
     */
    @RenderMapping(params = "render=jumpout")
    public String jumpout(RenderRequest request, ModelMap model) {
        PatientEvent patient = (PatientEvent) model.get("patient");
        String uid = lookupUid(request);
        // Validation
        // 1: user
        if ("".equals(uid)) {
            // TODO: send error correctly
            model.addAttribute("error", "Användaren okänd");
            return VIEW_JSP;
        }
        // 2: Valid patient identifier (PNo, ReservNo, ...)
        if (null == patient.getPersonNummer()) {
            // TODO: send error correctly
            model.addAttribute("error", "Patient okänd");
            return VIEW_JSP;
        }
        // 3: configuration
        if (null == pafUrl) {
            // TODO: send error correctly
            model.addAttribute("error", "Paf konfiguration saknas");
            return VIEW_JSP;
        }

        if ("post".equals(pafAccessMode)) {
            model.addAttribute("url", pafUrl);
            if ("insecure".equals(pafAccessSecurityLevel)) {
                model.addAttribute("mode", pafMode);
            }
            model.addAttribute("user", uid);
            model.addAttribute("pid", patient.getPersonNummer().getFull());
        } else { // default get
            String jumpoutUrl = pafUrl + "?USER=" + uid + "&PID=" + patient.getPersonNummer().getFull();
            if ("insecure".equals(pafAccessSecurityLevel)) {
                jumpoutUrl += "&MODE=" + pafMode;
            }
            model.addAttribute("jumpout", jumpoutUrl);
            LOGGER.debug(jumpoutUrl);
        }
        model.addAttribute("accessMode", pafAccessMode);
        model.addAttribute("level", pafAccessSecurityLevel);
        model.addAttribute("jump", "open");

        // Log to audit log
        logSearchRequest(patient.getPersonNummer().getFull(), request);

        return JUMPOUT_JSP;
    }

    /**
     * Listen for the patient context change event.
     * 
     * @param request
     *            EventRequest event
     * @param response
     *            Event response to redirect flow
     * @param model
     *            Model map to communicate
     */
    @EventMapping("{http://vgregion.se/patientcontext/events}pctx.change")
    public void changeListner(EventRequest request, EventResponse response, ModelMap model) {
        Event event = request.getEvent();
        PatientEvent patient = (PatientEvent) event.getValue();

        model.addAttribute("patient", patient);
        if (patient.getPersonNummer() != null && patient.getPersonNummer().getType() != PersonNummer.Type.INVALID) {
            response.setRenderParameter("render", "jumpout");
        }
    }

    /**
     * Listen for the patient context reset event.
     * 
     * @param model
     *            clean Model map
     */
    @EventMapping("{http://vgregion.se/patientcontext/events}pctx.reset")
    public void resetListner(ModelMap model) {
        model.addAttribute("patient", new PatientEvent(""));
    }

    private String lookupUid(PortletRequest req) {
        Map<String, ?> userInfo = (Map<String, ?>) req.getAttribute(PortletRequest.USER_INFO);
        String userId;
        if (userInfo != null) {
            userId = (String) userInfo.get(PortletRequest.P3PUserInfos.USER_LOGIN_ID.toString());
        } else {
            userId = "";
        }
        return userId;
    }

    private void logSearchRequest(String personId, PortletRequest request) {
        // Log search to audit log
        AuditLogInfoContainer container = auditLogInfoContainerFactory.getAuditLogInfoContainer(personId, request);
        auditLogger.logRequestParametersInAuditLog(container);
    }

}
