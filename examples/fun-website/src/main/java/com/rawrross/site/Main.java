package com.rawrross.site;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.rawrross.server.HTTPRequest;
import com.rawrross.server.HTTPResponse;
import com.rawrross.server.HTTPResponse.HttpStatusCode;
import com.rawrross.server.HTTPServer;
import com.rawrross.server.RequestHandler;
import com.rawrross.site.endpoint.Endpoint;
import com.rawrross.site.endpoint.Fortune;
import com.rawrross.site.endpoint.Index;
import com.rawrross.site.endpoint.Pokemon;

public class Main implements RequestHandler {

	public static void main(String[] args) throws IOException {
		new Main();
	}

	private HTTPServer server;

	private HashMap<String, Endpoint> endpoints;

	public Main() throws IOException {
		endpoints = new HashMap<>();
		endpoints.put("/", new Index());
		endpoints.put("/fortune", new Fortune());
		endpoints.put("/pokemon", new Pokemon());

		server = new HTTPServer(8080);
		server.setRequestHandler(this);
	}

	@Override
	public void handleRequest(HTTPRequest request, HTTPResponse response) throws IOException {
		String uri = request.getUri();
		String closest = null;

		for (String e : endpoints.keySet()) {
			if (uri.startsWith(e) && (closest == null || closest.length() < e.length())) {
				closest = e;
			}
		}

		Endpoint e = endpoints.get(closest);

		if (e == null) {
			response.setErrorStatus(HttpStatusCode.NOT_FOUND, request.getUri());
		} else {
			boolean isPage = uri.matches("^" + Pattern.quote(closest) + "\\/?(index.html)?");

			if (isPage) {
				e.getPage(request, response);
			} else {
				e.getFile(request, response);
			}
		}
	}

	public static void getResourceFile(HTTPRequest request, HTTPResponse response) {
		Path path = Path.of("resources", request.getUri());

		if (Files.exists(path) && !Files.isDirectory(path)) {
			response.setBody(path);
		} else {
			response.setErrorStatus(HttpStatusCode.NOT_FOUND, request.getUri());
		}
	}

}
