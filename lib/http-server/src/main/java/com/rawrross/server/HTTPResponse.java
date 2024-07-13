package com.rawrross.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map.Entry;

import org.jsoup.nodes.Document;

/**
 * Used to construct a response to an HTTP request.
 * 
 * @author Randy Ross
 */
public class HTTPResponse {

	/** HTTP-standard CRLF line break. */
	public static final String LINE_BREAK = "\r\n";

	public enum HttpStatusCode {
		OK(200, "OK"),
		NO_CONTENT(204, "No Content"),

		BAD_REQUEST(400, "Bad Request"),
		FORBIDDEN(403, "Forbidden"),
		NOT_FOUND(404, "Not Found"),
		REQUEST_TIMEOUT(408, "Request Timeout"),

		INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
		NOT_IMPLEMENTED(501, "Not Implemented");

		/** The code number for this status. */
		public final int CODE;
		/** The reason phrase for this status. */
		public final String REASON_PHRASE;

		private HttpStatusCode(int code, String reasonPhrase) {
			this.CODE = code;
			this.REASON_PHRASE = reasonPhrase;
		}

		@Override
		public String toString() {
			return CODE + " " + REASON_PHRASE;
		}
	}

	private HttpStatusCode statusCode;
	private String contentType;
	private byte[] body;
	private Path bodyPath;
	private HashMap<String, String> headers;

	HTTPResponse() {
		headers = new HashMap<>();
	}

	/**
	 * Set the status for this response.
	 * 
	 * @param statusCode The HTTP status code.
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse setStatusCode(HttpStatusCode statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	/**
	 * Set the <code>Content-Type</code> header for this response. If no content
	 * type is set, and it cannot be guessed from the response body, it defaults to
	 * <code>application/octet-stream</code>.
	 * 
	 * @param contentType The content MIME type.
	 * @return This HTTP response for chaining.
	 * @see MimeType
	 */
	public HTTPResponse setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	/**
	 * Set the body of this response to the given String, encoded in the
	 * {@link HTTPServer#DEFAULT_CHARSET default charset}.
	 * 
	 * @param data The response body String.
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse setBody(String data) {
		this.body = data.getBytes(HTTPServer.DEFAULT_CHARSET);
		this.bodyPath = null;
		return this;
	}

	/**
	 * Set the body of this response to the given byte array.
	 * 
	 * @param data An array of bytes.
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse setBody(byte[] data) {
		this.body = data;
		this.bodyPath = null;
		return this;
	}

	/**
	 * Set the body of this response to the contents of the specified file. This
	 * file will be streamed to the client once response transmission begins.
	 * 
	 * @param filePath The path to the desired file.
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse setBody(Path filePath) {
		this.body = null;
		this.bodyPath = filePath;
		return this;
	}

	/**
	 * 
	 * 
	 * @param html An HTML document to use as the response body
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse setBody(Document html) {
		this.body = ("<!DOCTYPE html>" + html.toString()).getBytes(HTTPServer.DEFAULT_CHARSET);
		this.bodyPath = null;
		this.contentType = MimeType.TEXT_HTML.MIME;
		return this;
	}

	/**
	 * Set the body of the response to a generic error page with the given status
	 * and message. Simultaneously sets the status of the response to the given
	 * status code.
	 * 
	 * @param status  The HTTP status code. Used as the webpage's title and
	 *                displayed in large text on the webpage.
	 * @param message Displayed in smaller text on the webpage. Pass null to omit
	 *                the subtitle.
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse setErrorStatus(HttpStatusCode status, String message) {
		this.body = HTTPServer.errDoc(status.toString(), message).getBytes(HTTPServer.DEFAULT_CHARSET);
		this.bodyPath = null;
		this.contentType = MimeType.TEXT_HTML.MIME;
		this.statusCode = status;
		return this;
	}

	/**
	 * Append the given value to the specified response header.
	 * 
	 * @param name  The name of the header to append.
	 * @param value The value to append.
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse addHeader(String name, String value) {
		// TODO append header
		headers.put(name, value);
		return this;
	}

	/**
	 * Set the value of the specified response header to the given value,
	 * overwriting the old value.
	 * 
	 * @param name  The name of the header to set.
	 * @param value The value to set the header to.
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse setHeader(String name, String value) {
		headers.put(name, value);
		return this;
	}

	/**
	 * Remove the specified header and value from this response.
	 * 
	 * @param name The header to remove.
	 * @return This HTTP response for chaining.
	 */
	public HTTPResponse removeHeader(String name) {
		headers.remove(name);
		return this;
	}

	/**
	 * Write this HTTP response to the given output stream.
	 * 
	 * @param out The output stream to write.
	 * @throws IOException
	 */
	void write(OutputStream out) throws IOException {
		long contentLength = 0;
		if (body != null)
			contentLength = body.length;
		else if (bodyPath != null)
			contentLength = Files.size(bodyPath);

		StringBuilder res = new StringBuilder();

		if (statusCode == null)
			statusCode = HttpStatusCode.OK;

		if (contentLength == 0)
			statusCode = HttpStatusCode.NO_CONTENT;

		res.append("HTTP/1.1 " + statusCode);
		res.append(LINE_BREAK);

		res.append("Date: " + HTTPServer.getDate());
		res.append(LINE_BREAK);

		res.append("Content-Length: " + contentLength);
		res.append(LINE_BREAK);

		if (body != null || bodyPath != null) {
			if (contentType == null) {
				if (bodyPath != null)
					contentType = MimeType.getMimeFromFilename(bodyPath);
				else
					contentType = MimeType.APPLICATION_OCTET_STREAM.MIME;
			}

			res.append("Content-Type: " + contentType);
			if (MimeType.typeIsText(contentType))
				res.append("; charset=" + HTTPServer.DEFAULT_CHARSET.name());
			res.append(LINE_BREAK);
		}

		for (Entry<String, String> header : headers.entrySet()) {
			res.append(header.getKey() + ": " + header.getValue());
			res.append(LINE_BREAK);
		}

		res.append(LINE_BREAK);

		out.write(res.toString().getBytes(HTTPServer.DEFAULT_CHARSET));

		if (body != null) {
			out.write(body);
		} else if (bodyPath != null) {
			// Stream file bytes
			try (InputStream in = Files.newInputStream(bodyPath)) {
				int read;
				byte[] buffer = new byte[8 * 1024];
				while ((read = in.read(buffer)) > 0) {
					out.write(buffer, 0, read);
				}
			}
		}
	}

}
