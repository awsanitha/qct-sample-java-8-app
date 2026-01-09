package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.InputStream;

public class Sha512ShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

	@Override
	protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) {
		try {
			final HashCode hash = Hashing.sha512().hashBytes(inputStream.readAllBytes());
			return "\"" + hash + "\"";
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
