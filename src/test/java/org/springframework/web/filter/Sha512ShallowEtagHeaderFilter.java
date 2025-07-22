package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Custom implementation of ShallowEtagHeaderFilter that uses SHA-512 for ETag generation
 */
public class Sha512ShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

    /**
     * Generate an ETag value using SHA-512 algorithm
     */
    protected String generateETagValue(byte[] responseBody) {
        final HashCode hash = Hashing.sha512().hashBytes(responseBody);
		return "\"" + hash + "\"";
	}
}
