package jre.servlet;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

/** This class can be used to deny access to "src", among other things */
public class noaccess extends HttpServlet 
{   
    public void doGet (HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter    out = response.getWriter();
        try {
            out.println("<html><title>Forbidden Access</title><body><p><h2 align=\"center\"> No Access </h2></body></html>");
        } catch (Exception e) {         
            out.println(e.getMessage());
        }

    }   
}
