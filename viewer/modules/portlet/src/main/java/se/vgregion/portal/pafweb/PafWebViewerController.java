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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import se.vgregion.portal.patient.event.PatientEvent;

import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderResponse;

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

    @Autowired
    private PafSecurity pafSecurity;

    @RenderMapping
    public String view(ModelMap model) {
        LOGGER.debug("view");
        if (!model.containsKey("patient")) {
            model.addAttribute("patient", new PatientEvent());
        }

        return VIEW_JSP;
    }

    @RenderMapping(params="render=jumpout")
    public String jumpout(RenderResponse response, ModelMap model) {
        PatientEvent patient = (PatientEvent) model.get("patient");

        String jumpoutUrl = pafSecurity.getPafUrl()+"?MODE="+pafSecurity.getPafMode()+"&USER=susro3&PID="+patient.getPersonNumber();
        LOGGER.debug(jumpoutUrl);
        model.addAttribute("jumpout", jumpoutUrl);

        return JUMPOUT_JSP;
    }

    @EventMapping("{http://vgregion.se/patientcontext/events}pctx.change")
    public void changeListner(EventRequest request, EventResponse response, ModelMap model) {
        Event event = request.getEvent();
        PatientEvent patient = (PatientEvent)event.getValue();
        LOGGER.debug("PafWeb personnummer: "+patient.getPersonNumber());

        model.addAttribute("patient", patient);

        response.setRenderParameter("render", "jumpout");
    }


    @EventMapping("{http://vgregion.se/patientcontext/events}pctx.reset")
    public void resetListner(ModelMap model) {
        LOGGER.debug("PafWeb reset");
        model.addAttribute("patient", new PatientEvent());
    }
}
