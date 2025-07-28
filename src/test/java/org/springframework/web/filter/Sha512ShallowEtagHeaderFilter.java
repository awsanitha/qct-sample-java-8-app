package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Custom implementation of ShallowEtagHeaderFilter that uses SHA-512 for ETag generation.
 * This class is adapted for Spring Framework 6.x compatibility.
 */
public class Sha512ShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

    /**
     * Default constructor for Sha512ShallowEtagHeaderFilter.
     */
    public Sha512ShallowEtagHeaderFilter() {
        // Use default constructor
    }

    /**
     * Creates a SHA-512 based ETag value from the given byte array.
     * This method is called internally by the parent class.
     * 
     * @param bytes the bytes to calculate the ETag value from
     * @return the calculated ETag value
     */
    protected String createETagValue(byte[] bytes) {
		final HashCode hash = Hashing.sha512().hashBytes(bytes);
		return "\"" + hash + "\"";
	}
}
