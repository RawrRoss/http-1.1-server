package com.rawrross.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

public enum MimeType {

	TEXT_PLAIN("text/plain", "txt"),
	TEXT_HTML("text/html", "html", "htm"),
	TEXT_CSS("text/css", "css"),
	TEXT_JAVASCRIPT("text/javascript", "js"),

	IMAGE_JPEG("image/jpeg", "jpg", "jpeg"),
	IMAGE_PNG("image/png", "png"),
	IMAGE_GIF("image/gif", "gif"),
	IMAGE_WEBP("image/webp", "webp"),

	VIDEO_MP4("video/mp4", "mp4"),
	VIDEO_TS("video/mp2t", "ts"),
	VIDEO_WEBM("video/webm", "webm"),

	APPLICATION_JSON("application/json", "json"),
	APPLICATION_OCTET_STREAM("application/octet-stream", "bin");

	public static boolean typeIsText(String mime) {
		return switch (MIME_TO_ENUM.get(mime)) {
			case TEXT_PLAIN, TEXT_HTML, TEXT_CSS, TEXT_JAVASCRIPT, APPLICATION_JSON -> true;
			default -> false;
		};
	}

	public static boolean typeIsImage(String mime) {
		return switch (MIME_TO_ENUM.get(mime)) {
			case IMAGE_JPEG, IMAGE_PNG, IMAGE_GIF, IMAGE_WEBP -> true;
			default -> false;
		};
	}

	public final String MIME;
	public final String EXTENSION;
	public final String[] EXTENSIONS;

	private MimeType(String mime, String... ext) {
		MIME = mime;
		EXTENSION = ext[0];
		EXTENSIONS = ext;
	}

	private static final Map<String, MimeType> MIME_TO_ENUM;
	private static final Map<String, String> MIME_TO_EXT;
	private static final Map<String, String> EXT_TO_MIME;

	static {
		MIME_TO_ENUM = Arrays.stream(MimeType.values())
				.collect(Collectors.toMap(m -> m.MIME, Function.identity()));

		MIME_TO_EXT = Arrays.stream(MimeType.values())
				.collect(Collectors.toMap(m -> m.MIME, m -> m.EXTENSION));

		EXT_TO_MIME = new HashMap<>();
		for (MimeType m : MimeType.values()) {
			for (String ext : m.EXTENSIONS)
				EXT_TO_MIME.put(ext, m.MIME);
		}
	}

	public static String getMimeFromFilename(Path path) {
		return getMimeFromFilename(path.toString());
	}

	public static String getMimeFromFilename(String file) {
		String ext = FilenameUtils.getExtension(file).toLowerCase();
		String mime = EXT_TO_MIME.get(ext);
		return (mime != null) ? mime : APPLICATION_OCTET_STREAM.MIME;
	}

	public static String getExtensionFromMime(String mime) {
		String ext = MIME_TO_EXT.get(mime);
		if (ext == null) {
			throw new IllegalArgumentException("Unknown MIME Type '" + mime + "'");
		}
		return ext;
	}

}
