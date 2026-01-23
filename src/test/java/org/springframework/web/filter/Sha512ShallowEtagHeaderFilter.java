package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

public class Sha512ShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

	@Override
	protected String generateETagHeaderValue(java.io.InputStream inputStream, boolean isWeak) throws java.io.IOException {
		byte[] bytes = inputStream.readAllBytes();
		final HashCode hash = Hashing.sha512().hashBytes(bytes);
		return (isWeak ? "W/" : "") + "\"" + hash + "\"";
	}
}
