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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import se.vgregion.portal.patient.event.PatientEvent;
import se.vgregion.portal.patient.event.PersonNummer;

import javax.portlet.*;
import java.util.Map;

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

    public static final String VIEW_JSP = "view";
    public static final String JUMPOUT_JSP = "jumpout";

    @Value("${paf.url}")
    private String pafUrl;

    @Value("${paf.mode}")
    private String pafMode;

    @Value("${paf.access.security.level}")
    private String pafAccessSecurityLevel;

    @Value("${paf.access.mode}")
    private String pafAccessMode;

    @RenderMapping
    public String view(ModelMap model) {
        LOGGER.debug("view");
        if (!model.containsKey("patient")) {
            model.addAttribute("patient", new PatientEvent());
        }

        return VIEW_JSP;
    }

    @RenderMapping(params = "render=jumpout")
    public String jumpout(RenderRequest request, RenderResponse response, ModelMap model) {
        PatientEvent patient = (PatientEvent) model.get("patient");
        String uid = lookupUid(request);

        if ("post".equals(pafAccessMode)) {
            model.addAttribute("url", pafUrl);
            if ("insecure".equals(pafAccessSecurityLevel)) {
                model.addAttribute("mode", pafMode);
            }
            model.addAttribute("user", uid);
            model.addAttribute("pid", patient.getPersonNummer().getFull());
        } else { // default get
            String jumpoutUrl = pafUrl + "?USER="+uid+"&PID=" + patient.getPersonNummer().getFull();
            if ("insecure".equals(pafAccessSecurityLevel)) {
                jumpoutUrl += "&MODE="+pafMode;
            }
            model.addAttribute("jumpout", jumpoutUrl);
            LOGGER.debug(jumpoutUrl);
        }
        model.addAttribute("accessMode", pafAccessMode);
        model.addAttribute("level", pafAccessSecurityLevel);
        model.addAttribute("jump", "open");

        return JUMPOUT_JSP;
    }

    @EventMapping("{http://vgregion.se/patientcontext/events}pctx.change")
    public void changeListner(EventRequest request, EventResponse response, ModelMap model) {
        Event event = request.getEvent();
        PatientEvent patient = (PatientEvent) event.getValue();
        LOGGER.debug("Input: " + patient.getInputText());

        model.addAttribute("patient", patient);
        if (patient.getPersonNummer() != null && patient.getPersonNummer().getType() != PersonNummer.Type.INVALID) {
            response.setRenderParameter("render", "jumpout");
        }
    }


    @EventMapping("{http://vgregion.se/patientcontext/events}pctx.reset")
    public void resetListner(ModelMap model) {
        LOGGER.debug("PafWeb reset");
        model.addAttribute("patient", new PatientEvent());
    }


    private String lookupUid(PortletRequest req) {
        Map<String, ?> userInfo = (Map<String, ?>) req.getAttribute(PortletRequest.USER_INFO);
        String userId;
        if (userInfo != null) {
            userId = (String) userInfo.get(PortletRequest.P3PUserInfos.USER_LOGIN_ID.toString());
        } else {
            userId = (String) "";
        }
        return userId;
    }

}
