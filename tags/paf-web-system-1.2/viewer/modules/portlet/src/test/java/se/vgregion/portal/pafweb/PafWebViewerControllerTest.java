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

import static javax.portlet.PortletRequest.P3PUserInfos.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.portlet.MockEvent;
import org.springframework.mock.web.portlet.MockEventRequest;
import org.springframework.mock.web.portlet.MockEventResponse;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ModelMap;

import se.vgregion.portal.auditlog.AuditLogInfoContainer;
import se.vgregion.portal.auditlog.AuditLogInfoContainerFactory;
import se.vgregion.portal.auditlog.PatientAccessAuditLogger;
import se.vgregion.portal.patient.event.PatientEvent;

/**
 * This action do that and that, if it has something special it is.
 * 
 * @author <a href="mailto:david.rosell@redpill-linpro.com">David Rosell</a>
 */
public class PafWebViewerControllerTest {
    private PafWebViewerController controller;
    private ModelMap model;

    @Mock
    PatientAccessAuditLogger auditLoggerMock;

    @Mock
    AuditLogInfoContainerFactory containerFactoryMock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        controller = new PafWebViewerController();
        ReflectionTestUtils.setField(controller, "auditLogger", auditLoggerMock, PatientAccessAuditLogger.class);
        ReflectionTestUtils.setField(controller, "auditLogInfoContainerFactory", containerFactoryMock,
                AuditLogInfoContainerFactory.class);
        model = new ModelMap();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testView() throws Exception {
        assertFalse(model.containsKey("patient"));

        String result = controller.view(model);

        // Test create patient - PatientEvent if not present
        assertEquals(PafWebViewerController.VIEW_JSP, result);
        Object patientObj = model.get("patient");

        assertNotNull(patientObj);
        assertTrue(patientObj instanceof PatientEvent);

        // Test do not touch patient if present
        controller.view(model);
        Object patientObj2 = model.get("patient");

        assertSame(patientObj, patientObj2);
    }

    @Test
    public void testJumpoutUserNotLogedIn() throws Exception {
        controller.view(model);

        RenderRequest mockReq = new MockRenderRequest();
        String result = controller.jumpout(mockReq, model);
        assertEquals(PafWebViewerController.VIEW_JSP, result);
        assertTrue(model.containsKey("error"));
    }

    @Test
    public void testJumpoutNoPatient() throws Exception {
        controller.view(model);

        RenderRequest mockReq = new MockRenderRequest();
        Map<String, String> userInfo = new HashMap<String, String>();
        userInfo.put(USER_LOGIN_ID.toString(), "testuser");
        mockReq.setAttribute(PortletRequest.USER_INFO, userInfo);

        String result = controller.jumpout(mockReq, model);
        assertEquals(PafWebViewerController.VIEW_JSP, result);
        assertTrue(model.containsKey("error"));
    }

    @Test
    public void testJumpoutNoConfig() throws Exception {
        model.addAttribute("patient", new PatientEvent("19531212-1212"));

        RenderRequest mockReq = new MockRenderRequest();
        Map<String, String> userInfo = new HashMap<String, String>();
        userInfo.put(USER_LOGIN_ID.toString(), "testuser");
        mockReq.setAttribute(PortletRequest.USER_INFO, userInfo);

        String result = controller.jumpout(mockReq, model);
        assertEquals(PafWebViewerController.VIEW_JSP, result);
        assertTrue(model.containsKey("error"));
    }

    @Test
    public void testJumpoutGetSecure() throws Exception {
        controller.pafUrl = "";
        model.addAttribute("patient", new PatientEvent("19531212-1212"));

        RenderRequest mockReq = new MockRenderRequest();
        Map<String, String> userInfo = new HashMap<String, String>();
        userInfo.put(USER_LOGIN_ID.toString(), "testuser");
        mockReq.setAttribute(PortletRequest.USER_INFO, userInfo);

        String result = controller.jumpout(mockReq, model);

        assertEquals(PafWebViewerController.JUMPOUT_JSP, result);
        assertFalse(model.containsKey("error"));

        assertFalse(model.containsKey("url"));
        assertFalse(model.containsKey("mode"));
        assertFalse(model.containsKey("user"));
        assertFalse(model.containsKey("pid"));

        assertTrue(model.containsKey("jumpout"));
        assertTrue(model.containsKey("jump"));
        final String jumpoutUrl = model.get("jumpout").toString();
        assertFalse(jumpoutUrl.contains("&MODE="));

        verify(containerFactoryMock).getAuditLogInfoContainer(anyString(), any(PortletRequest.class));
        verify(auditLoggerMock).logRequestParametersInAuditLog(any(AuditLogInfoContainer.class));
    }

    @Test
    public void testJumpoutGetInsecure() throws Exception {
        controller.pafUrl = "";
        controller.pafAccessSecurityLevel = "insecure";
        model.addAttribute("patient", new PatientEvent("19531212-1212"));

        RenderRequest mockReq = new MockRenderRequest();
        Map<String, String> userInfo = new HashMap<String, String>();
        userInfo.put(USER_LOGIN_ID.toString(), "testuser");
        mockReq.setAttribute(PortletRequest.USER_INFO, userInfo);

        controller.jumpout(mockReq, model);

        String jumpoutUrl = model.get("jumpout").toString();
        assertTrue(jumpoutUrl.contains("&MODE="));

        // configuration pafAccessSecurityLevel != insecure -> no MODE parameter
        controller.pafAccessSecurityLevel = "anythingElse";
        controller.jumpout(mockReq, model);

        jumpoutUrl = model.get("jumpout").toString();
        assertFalse(jumpoutUrl.contains("&MODE="));

        verify(containerFactoryMock, times(2)).getAuditLogInfoContainer(anyString(), any(PortletRequest.class));
        verify(auditLoggerMock, times(2)).logRequestParametersInAuditLog(any(AuditLogInfoContainer.class));
    }

    @Test
    public void testJumpoutPostSecure() throws Exception {
        controller.pafUrl = "";
        controller.pafAccessMode = "post";
        model.addAttribute("patient", new PatientEvent("19531212-1212"));

        RenderRequest mockReq = new MockRenderRequest();
        Map<String, String> userInfo = new HashMap<String, String>();
        userInfo.put(USER_LOGIN_ID.toString(), "testuser");
        mockReq.setAttribute(PortletRequest.USER_INFO, userInfo);

        String result = controller.jumpout(mockReq, model);

        assertEquals(PafWebViewerController.JUMPOUT_JSP, result);
        assertFalse(model.containsKey("error"));

        assertTrue(model.containsKey("url"));
        assertFalse(model.containsKey("mode"));
        assertTrue(model.containsKey("user"));
        assertTrue(model.containsKey("pid"));

        assertFalse(model.containsKey("jumpout"));
        assertTrue(model.containsKey("jump"));

        verify(containerFactoryMock).getAuditLogInfoContainer(anyString(), any(PortletRequest.class));
        verify(auditLoggerMock).logRequestParametersInAuditLog(any(AuditLogInfoContainer.class));
    }

    @Test
    public void testJumpoutPostInsecure() throws Exception {
        controller.pafUrl = "";
        controller.pafAccessMode = "post";
        controller.pafAccessSecurityLevel = "insecure";
        model.addAttribute("patient", new PatientEvent("19531212-1212"));

        RenderRequest mockReq = new MockRenderRequest();
        Map<String, String> userInfo = new HashMap<String, String>();
        userInfo.put(USER_LOGIN_ID.toString(), "testuser");
        mockReq.setAttribute(PortletRequest.USER_INFO, userInfo);

        controller.jumpout(mockReq, model);

        assertTrue(model.containsKey("mode"));

        verify(containerFactoryMock).getAuditLogInfoContainer(anyString(), any(PortletRequest.class));
        verify(auditLoggerMock).logRequestParametersInAuditLog(any(AuditLogInfoContainer.class));
    }

    @Test
    public void testChangeListnerValidPatient() throws Exception {
        PatientEvent pEvent = new PatientEvent("19121212-1212");
        Event mockEvent = new MockEvent("{http://vgregion.se/patientcontext/events}pctx.change", pEvent);
        EventRequest mockReq = new MockEventRequest(mockEvent);
        EventResponse mockRes = new MockEventResponse();

        controller.changeListner(mockReq, mockRes, model);
        assertSame(model.get("patient"), pEvent);
        assertEquals("jumpout", mockRes.getRenderParameterMap().get("render")[0]);
    }

    @Test
    public void testChangeListnerNoPatient() throws Exception {
        PatientEvent pEvent = new PatientEvent("");
        Event mockEvent = new MockEvent("{http://vgregion.se/patientcontext/events}pctx.change", pEvent);
        EventRequest mockReq = new MockEventRequest(mockEvent);
        EventResponse mockRes = new MockEventResponse();

        controller.changeListner(mockReq, mockRes, model);
        assertSame(model.get("patient"), pEvent);
        assertFalse(mockRes.getRenderParameterMap().containsKey("render"));
    }

    @Test
    public void testChangeListnerInvalidPatient() throws Exception {
        PatientEvent pEvent = new PatientEvent("19121212-1212a");
        Event mockEvent = new MockEvent("{http://vgregion.se/patientcontext/events}pctx.change", pEvent);
        EventRequest mockReq = new MockEventRequest(mockEvent);
        EventResponse mockRes = new MockEventResponse();

        controller.changeListner(mockReq, mockRes, model);
        assertSame(model.get("patient"), pEvent);
        assertFalse(mockRes.getRenderParameterMap().containsKey("render"));
    }

    @Test
    public void testResetListner() throws Exception {
        PatientEvent pEvent = new PatientEvent("19121212-1212a");
        model.addAttribute("patient", pEvent);

        controller.resetListner(model);
        PatientEvent pModel = (PatientEvent) model.get("patient");
        assertNotSame(pEvent, pModel);

        assertEquals("", pModel.getInputText());
        assertNull(pModel.getPersonNummer());
    }
}
