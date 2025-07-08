package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

public class Sha512ShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

	// Updated method signature for Spring Boot 3.x
	protected String generateETagHeaderValue(byte[] bytes, int length) {
		final HashCode hash = Hashing.sha512().hashBytes(bytes, 0, length);
		return "\"" + hash + "\"";
	}
}
