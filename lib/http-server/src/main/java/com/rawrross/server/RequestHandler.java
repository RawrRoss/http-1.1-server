package com.rawrross.server;

import java.io.IOException;

/**
 * A functional interface for handling HTTP requests.
 */
@FunctionalInterface
public interface RequestHandler {

	/**
	 * Handle an incoming HTTP request, and produce a response. Modify the provided
	 * response object.
	 * 
	 * @param request  The incoming HTTP request.
	 * @param response The HTTP response that is sent back.
	 * @throws IOException
	 */
	void handleRequest(HTTPRequest request, HTTPResponse response) throws IOException;

}
