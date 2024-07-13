package com.rawrross.site.endpoint;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rawrross.server.HTTPRequest;
import com.rawrross.server.HTTPResponse;
import com.rawrross.site.Main;

public class Index implements Endpoint {

	@Override
	public void getPage(HTTPRequest request, HTTPResponse response) {
		Document doc = Document.createShell("");

		doc.select("html").attr("lang", "en");
		doc.head()
				.appendElement("title")
				.text("Index");

		doc.body()
				.appendElement("h1")
				.text("Welcome");

		Element p1 = doc.body().appendElement("p");
		p1.appendElement("a")
				.attr("href", "/fortune")
				.text("Fortune");
		p1.appendText(" - Get a (mis)fortune!");

		Element p2 = doc.body().appendElement("p");
		p2.appendElement("a")
				.attr("href", "/pokemon")
				.text("Pokemon Fusion");
		p2.appendText(" - Create a fusion of two Pokemon! Based on a program by ");
		p2.appendElement("a")
				.attr("href", "https://pokemon.alexonsager.net/")
				.text("Alex Onsager");

		response.setBody(doc);
	}

	@Override
	public void getFile(HTTPRequest request, HTTPResponse response) {
		Main.getResourceFile(request, response);
	}

}
