package com.rawrross.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rawrross.server.exception.BadRequestException;

/**
 * Represents an HTTP request; can be used to get the HTTP method, request
 * headers, query parameters, etc.
 * 
 * @author Randy Ross
 */
public class HTTPRequest {

	public enum HTTPMethod {
		GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE, PATCH
	}

	private HTTPMethod method;
	private String version;
	private String uri;

	private ArrayList<String> lines;
	private HashMap<String, String> headers;
	private HashMap<String, String> params;

	/**
	 * Create an HTTP request object reading from the given socket input stream.
	 * Blocks until the request is fully read, or a socket read timeout occurs.
	 * <p>
	 * Use {@link #checkParseException()} to check for exceptions in parsing the
	 * request.
	 * 
	 * @param in The socket input stream to read from.
	 * @throws SocketTimeoutException If the socket times out while reading.
	 * @throws IOException            If there is an issue reading from the input
	 *                                stream.
	 */
	HTTPRequest(InputStream in) throws SocketTimeoutException, IOException {
		lines = new ArrayList<>();
		headers = new HashMap<>();
		params = new HashMap<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(in, HTTPServer.DEFAULT_CHARSET));

		while (true) {
			String line = reader.readLine();
			if (line == null || line.isBlank())
				break;

			lines.add(line);
			parseLine(line);
		}

		if (lines.isEmpty())
			throw new SocketTimeoutException();

		if (method == null || uri == null)
			parseException = new BadRequestException("Invalid HTTP request");
	}

	private Exception parseException;

	/**
	 * Throws any exception that occurred while parsing the HTTP request.
	 * 
	 * @throws Exception
	 */
	void checkParseException() throws Exception {
		if (parseException != null)
			throw parseException;
	}

	private void parseLine(String line) {
		if (line.matches("^(\\w+) (.+) (.+)$")) {
			parseHTTPMethod(line);
		} else {
			parseHTTPHeader(line);
		}
	}

	private void parseHTTPMethod(String line) {
		Matcher matcher = Pattern.compile("(\\w+) (.+) (.+)").matcher(line);

		try {
			matcher.find();
			method = HTTPMethod.valueOf(matcher.group(1));

			uri = matcher.group(2).split("\\?")[0];
			uri = URLDecoder.decode(uri, HTTPServer.DEFAULT_CHARSET);

			version = matcher.group(3);
		} catch (Exception e) {
			this.parseException = new BadRequestException("Invalid starting line '" + line + "'");
			return;
		}

		if (method == null) {
			parseException = new BadRequestException("Invalid HTTP method '" + line + "'");
			return;
		}
		if (uri == null) {
			parseException = new BadRequestException("Invalid URI '" + line + "'");
			return;
		}

		parseQueryString(matcher.group(2));
	}

	private void parseQueryString(String target) {
		Matcher m = Pattern.compile(".+\\?(.*)").matcher(target);
		if (m.find()) {
			String[] queryList = m.group(1).split("&");
			for (String query : queryList) {
				try {
					String[] data = query.split("=", 2);
					String field = URLDecoder.decode(data[0], HTTPServer.DEFAULT_CHARSET);
					String value = URLDecoder.decode(data[1], HTTPServer.DEFAULT_CHARSET);
					this.params.put(field, value);
				} catch (Exception e) {
				}
			}
		}
	}

	private void parseHTTPHeader(String line) {
		try {
			// TODO headers w/ multiple values, eg connection, cookies
			Matcher matcher = Pattern.compile("^(.+): (.+)$").matcher(line);
			matcher.find();
			String name = matcher.group(1).toLowerCase();
			String value = matcher.group(2);
			headers.put(name, value);
		} catch (Exception e) {
			this.parseException = e;
			return;
		}
	}

	/**
	 * Retrieve each line of this HTTP request.
	 * 
	 * @return An ArrayList of Strings.
	 */
	public ArrayList<String> getLines() {
		return lines;
	}

	/**
	 * Get the HTTP method of this request.
	 * 
	 * @return An HTTPMethod enum object. May be <code>null</code> if this request
	 *         is invalid.
	 */
	public HTTPMethod getMethod() {
		return method;
	}

	/**
	 * Get the requested URI.
	 * 
	 * @return The URI as a String. May be <code>null</code> if this request is
	 *         invalid.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Get the HTTP version of this request.
	 * 
	 * @return The request version as a String. May be <code>null</code> if this
	 *         request is invalid.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Test if the given header name exists in this request.
	 * 
	 * @param header The name of the desired header, case-<i>insensitive</i>.
	 * @return <code>True</code> if the header is present.
	 */
	public boolean hasHeader(String header) {
		return headers.containsKey(header);
	}

	/**
	 * Get the value associated with the given header name.
	 * 
	 * @param header The name of the desired header, case-<i>insensitive</i>.
	 * @return The value of the header as a String, or <code>null</code> if the
	 *         header is not present.
	 */
	public String getHeader(String header) {
		// TODO headers w/ multiple values, eg connection, cookies
		return headers.get(header.toLowerCase());
	}

	/**
	 * Test if the given URI parameter name exists in this request.
	 * 
	 * @param field The name of the desired parameter.
	 * @return <code>True</code> if the parameter is present.
	 */
	public boolean hasParameter(String field) {
		return params.containsKey(field);
	}

	/**
	 * Get the value associated with the given URI parameter name.
	 * 
	 * @param field The name of the desired parameter.
	 * @return The value of the parameter as a String, or <code>null</code> if the
	 *         parameter is not present.
	 */
	public String getParameter(String field) {
		return params.get(field);
	}

	/**
	 * Test whether or not a keep-alive connection has been requested.
	 * 
	 * @return <code>True</code> if the <code>Connection</code> header is set to
	 *         <code>keep-alive</code>.
	 */
	public boolean isConnectionKeepAlive() {
		return headers.getOrDefault("connection", "").equalsIgnoreCase("keep-alive");
	}

}
