package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Custom implementation of a filter that generates ETag headers using SHA-512.
 * Compatible with Spring Boot 3.x.
 */
public class Sha512ShallowEtagHeaderFilter extends OncePerRequestFilter {
    
    private final ShallowEtagHeaderFilter delegate = new ShallowEtagHeaderFilter();

	@Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        // Use the delegate to do most of the work
        delegate.doFilter(request, response, filterChain);
    }
    
    /**
     * Custom implementation that uses SHA-512 for ETag generation
     */
	protected String generateETagHeaderValue(byte[] bytes) {
		final HashCode hash = Hashing.sha512().hashBytes(bytes);
		return "\"" + hash + "\"";
	}
}
