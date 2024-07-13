package com.rawrross.site.endpoint;

import com.rawrross.server.HTTPRequest;
import com.rawrross.server.HTTPResponse;

public interface Endpoint {
	
	public void getPage(HTTPRequest request, HTTPResponse response);

	public void getFile(HTTPRequest request, HTTPResponse response);

}
