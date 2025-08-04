package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Custom implementation of a filter that adds ETag headers using SHA-512 hashing
 * Compatible with Spring Boot 3.x
 */
public class Sha512ShallowEtagHeaderFilter extends OncePerRequestFilter {

    /**
     * Process the request and response with SHA-512 ETag generation
     */
	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        // Create a response wrapper to capture the response content
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        // Continue with the filter chain
        filterChain.doFilter(request, responseWrapper);
        
        // Generate ETag if needed
        if (!response.isCommitted() && request.getMethod().equals("GET") && response.getStatus() == 200) {
            byte[] responseBytes = responseWrapper.getContentAsByteArray();
            if (responseBytes.length > 0) {
                // Generate SHA-512 ETag
                String etag = generateSha512Etag(responseBytes);
                response.setHeader("ETag", etag);
            }
        }
        
        // Complete the response
        responseWrapper.copyBodyToResponse();
    }
    
    /**
     * Generate an ETag value using SHA-512 hashing
     */
    protected String generateSha512Etag(byte[] body) {
        final HashCode hash = Hashing.sha512().hashBytes(body);
		return "\"" + hash + "\"";
	}
}
