package com.nurkiewicz.download;

import java.io.File;
import java.net.URL;
import java.util.UUID;

public class FileExamples {

	public static final UUID TXT_FILE_UUID = UUID.fromString("9d8ef24c-f3a2-4037-b9a0-9852b0bb9ea9");
	public static final FilePointer TXT_FILE = txtFile();
	public static final UUID NOT_FOUND_UUID = UUID.fromString("480fb3cd-10ca-45df-aa73-1ef8eae4b7ba");

	private static FileSystemPointer txtFile() {
		final URL resource = FileStorageStub.class.getResource("/download.txt");
		final File file = new File(resource.getFile());
		return new FileSystemPointer(file);
	}


}
