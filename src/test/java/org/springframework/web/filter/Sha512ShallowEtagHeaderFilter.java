package org.springframework.web.filter;

import com.google.common.hash.Hashing;

import java.io.IOException;
import java.io.InputStream;

public class Sha512ShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

	@Override
	protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
		final String hash = Hashing.sha512().hashBytes(inputStream.readAllBytes()).toString();
		return "\"" + hash + "\"";
	}
}
