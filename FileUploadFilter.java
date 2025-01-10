package org.kie.bc.client;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileUploadFilter implements Filter {

    private String allowedExtensions;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        allowedExtensions = filterConfig.getServletContext().getInitParameter("allowedExtensions");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Check if the request is a POST request to /business-central/documentUploadServlet
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().endsWith("/business-central/documentUploadServlet")) {

            System.out.println(request.getRequestURI());
            request.setAttribute("allowedExtensions", allowedExtensions);
/*
            // Read the POST body and extract the filename
            String filename = getFilenameFromRequestBody(request);
            System.out.println("Filename: " + filename);

            // Validate the file extension
            String fileExtension = getFileExtension(filename);
            if (!isValidExtension(fileExtension)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file extension.");
                return;
            }*/
        }

        filterChain.doFilter(request, response);
    }

    private String getFilenameFromRequestBody(HttpServletRequest request) throws IOException {
        String sb;
        final ServletInputStream inputStream = request.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        sb = reader.lines().collect(Collectors.joining());
        String requestBody = sb;

        // Extract the filename from the request body
        String filename = null;
        String patternString = "filename=\"([^\"]+)\"";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(requestBody);
        if (matcher.find()) {
            filename = matcher.group(1);
        }
        return filename;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ""; // No extension found
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private boolean isValidExtension(String fileExtension) {
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return false;
        }
        List<String> exts = Arrays.asList(allowedExtensions.split(","));
        return exts.contains(fileExtension.toLowerCase());
    }

    @Override
    public void destroy() {
        // Cleanup code, if needed
    }
}
