package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.util.StringUtils;

/**
 * Custom implementation of ETag filter that uses SHA-512 for ETag generation.
 * This is a standalone implementation that doesn't extend ShallowEtagHeaderFilter.
 */
public class Sha512ShallowEtagHeaderFilter extends OncePerRequestFilter {

	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Only filter GET requests
        if (!"GET".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Wrap the response to capture content
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        // Process the request
        filterChain.doFilter(request, responseWrapper);
        
        // Generate ETag
        String eTag = generateETag(responseWrapper);
        if (StringUtils.hasText(eTag)) {
            response.setHeader(HttpHeaders.ETAG, eTag);
        }
        
        // Complete the response
        responseWrapper.copyBodyToResponse();
    }
    
    /**
     * Generate an ETag value using SHA-512 hash algorithm.
     */
    protected String generateETag(ContentCachingResponseWrapper response) {
        byte[] bytes = response.getContentAsByteArray();
        if (bytes.length == 0) {
            return null;
        }
		final HashCode hash = Hashing.sha512().hashBytes(bytes);
		return "\"" + hash + "\"";
	}
}
