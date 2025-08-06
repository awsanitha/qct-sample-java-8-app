package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Custom implementation of ETag filter that uses SHA-512 for ETag generation.
 * Based on Spring's ShallowEtagHeaderFilter but with SHA-512 instead of MD5.
 */
public class Sha512ShallowEtagHeaderFilter extends OncePerRequestFilter {

    private static final String HEADER_ETAG = "ETag";
    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String DIRECTIVE_NO_STORE = "no-store";

    /**
     * The default value is "false" so that the filter may delay the generation of
     * an ETag until the last asynchronously dispatched thread.
     */
    private boolean writeWeakETag = false;

	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpServletResponse responseToUse = response;
        if (!isAsyncDispatch(request) && !response.isCommitted()) {
            responseToUse = new ContentCachingResponseWrapper(response);
        }

        filterChain.doFilter(request, responseToUse);

        if (!isAsyncStarted(request) && !response.isCommitted()) {
            updateResponse(request, responseToUse);
        }
    }

    private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper responseWrapper = (ContentCachingResponseWrapper) response;
            HttpServletResponse rawResponse = (HttpServletResponse) responseWrapper.getResponse();
            int statusCode = rawResponse.getStatus();

            if (rawResponse.getHeader(HEADER_ETAG) == null && isEligibleForEtag(request, responseWrapper, statusCode)) {
                byte[] body = responseWrapper.getContentAsByteArray();
                String eTag = generateETag(body);
                rawResponse.setHeader(HEADER_ETAG, eTag);

                String ifNoneMatch = request.getHeader(HEADER_IF_NONE_MATCH);
                if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
                    rawResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Generate an ETag value from the given response body byte array.
     * Uses SHA-512 for stronger security.
     */
    protected String generateETag(byte[] body) {
        HashCode hash = Hashing.sha512().hashBytes(body);
        String etag = "\"" + hash + "\"";
        if (this.writeWeakETag) {
            etag = "W/" + etag;
        }
        return etag;
    }

    /**
     * Determine if the given request and response are eligible for ETag generation.
     */
    protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response, int statusCode) {
        if (statusCode >= 200 && statusCode < 300 && HttpMethod.GET.matches(request.getMethod())) {
            String cacheControl = response.getHeader(HEADER_CACHE_CONTROL);
            return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
        }
        return false;
    }

    /**
     * Set whether the ETag value should be written as a weak ETag.
     */
    public void setWriteWeakETag(boolean writeWeakETag) {
        this.writeWeakETag = writeWeakETag;
	}
}
