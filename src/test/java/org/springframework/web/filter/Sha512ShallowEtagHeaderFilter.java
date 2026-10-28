package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class Sha512ShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

	@Override
	protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
		final byte[] bytes = IOUtils.toByteArray(inputStream);
		final HashCode hash = Hashing.sha512().hashBytes(bytes);
		return "\"" + hash + "\"";
	}
}
