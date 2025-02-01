package com.guh.audit;

import com.guh.audit.services.AuditActionService;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/audit")
public class AuditServlet extends HttpServlet {

    @EJB
    private AuditActionService auditActionService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String ipAddress = req.getParameter("ipAddress");
        String sessionId = req.getParameter("sessionId");

        if (username != null && ipAddress != null && sessionId != null) {
            auditActionService.logLoginAction(username, ipAddress, sessionId);
            resp.getWriter().write("Login action logged successfully!");
        } else {
            resp.getWriter().write("Missing parameters: username, ipAddress, or sessionId");
        }
    }
}