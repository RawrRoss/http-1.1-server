package com.rawrross.site.endpoint;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rawrross.server.HTTPRequest;
import com.rawrross.server.HTTPResponse;
import com.rawrross.server.HTTPServer;
import com.rawrross.site.Main;

public class Fortune implements Endpoint {

	private String[] fortunes;
	private MessageDigest md5;

	public Fortune() throws IOException {
		// Load fortune strings
		String txt = new String(Fortune.class.getResourceAsStream("/fortune/fortunes.txt").readAllBytes(),
				HTTPServer.DEFAULT_CHARSET);
		fortunes = txt.strip().split("\r?\n");

		// Get MD5 instance if available
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("fortune: " + e.getMessage());
		}
	}

	@Override
	public void getPage(HTTPRequest request, HTTPResponse response) {
		String nameParam = request.getParameter("name");
		String ageParam = request.getParameter("age");
		int ageInt = 0;

		if (ageParam != null) {
			try {
				ageInt = Integer.parseInt(ageParam);
			} catch (NumberFormatException e) {
				ageParam = null;
			}
		}

		boolean showInputForm = (nameParam == null || ageParam == null);
		String welcomeText, fortuneText;

		if (showInputForm) {
			welcomeText = "Welcome to the Fortune Teller";
			fortuneText = "Enter a Name and Age to receive a (mis)fortune!";
		} else {
			welcomeText = "Fortune for '%s', age %d...".formatted(nameParam, ageInt);
			fortuneText = getFortune(nameParam, ageInt);
		}

		Document doc = Document.createShell("");

		doc.select("html").attr("lang", "en");
		doc.head()
				.appendElement("title")
				.text("Fortune");
		doc.head()
				.appendElement("link")
				.attr("rel", "stylesheet")
				.attr("href", "/form.css");
		doc.head()
				.appendElement("link")
				.attr("rel", "stylesheet")
				.attr("href", "/fortune/style.css");

		doc.body()
				.appendElement("a")
				.attr("href", "/")
				.text("<< index");

		Element crystalBall = new Element("img")
				.attr("src", "/fortune/crystal-ball.gif")
				.attr("alt", "");
		Element sparkles = new Element("img")
				.attr("src", "/fortune/sparkles.gif")
				.attr("alt", "");

		// Displays welcome message or fortune title
		Element titleBanner = doc.body()
				.appendElement("div")
				.addClass("banner")
				.id("title");
		titleBanner.appendChild(crystalBall.shallowClone());
		titleBanner.appendElement("span").text(welcomeText);
		titleBanner.appendChild(crystalBall.shallowClone());

		// Displays instructions or fortune message
		Element fortuneBanner = doc.body()
				.appendElement("div")
				.addClass("banner")
				.id("fortune");
		fortuneBanner.appendChild(sparkles.shallowClone());
		fortuneBanner.appendElement("span").text(fortuneText);
		fortuneBanner.appendChild(sparkles.shallowClone());

		Element formContainer = doc.body()
				.appendElement("div")
				.addClass("form-container");

		Element form = formContainer.appendElement("form")
				.attr("method", "get")
				.attr("action", "/fortune")
				.addClass("form");

		if (showInputForm) {
			Element formTable = form.appendElement("div")
					.addClass("form-table");

			Element nameDiv = formTable.appendElement("div")
					.addClass("form-row");

			Element ageDiv = formTable.appendElement("div")
					.addClass("form-row");

			// Name input
			final String form1id = "name";
			nameDiv.appendElement("label")
					.attr("for", form1id)
					.text("Name");
			nameDiv.appendElement("input")
					.attr("type", "text")
					.attr("name", form1id)
					.id(form1id);

			// Age input
			final String form2id = "age";
			ageDiv.appendElement("label")
					.attr("for", form2id)
					.text("Age");
			ageDiv.appendElement("input")
					.attr("type", "number")
					.attr("name", form2id)
					.id(form2id);

			// Submit button
			form.appendElement("button")
					.attr("type", "submit")
					.text("Get Fortune");
		} else {
			// Go Again button
			form.appendElement("button")
					.attr("type", "submit")
					.text("Go Again?");
		}

		Element footer = doc.body()
				.appendElement("div")
				.addClass("footer");

		footer.appendElement("a")
				.attr("target", "_blank")
				.attr("href", "https://tenor.com/view/emoji-emojis-stickers-sparkle-stars-gif-14519905")
				.text("Sparkles");
		footer.appendText(" and ");
		footer.appendElement("a")
				.attr("target", "_blank")
				.attr("href", "https://tenor.com/view/adamjk-emojis-stickers-crystal-ball-see-future-gif-14519843")
				.text("Crystal Ball");
		footer.appendText(" by @adamjk");
		footer.appendElement("br");
		footer.appendElement("a")
				.attr("target", "_blank")
				.attr("href", "https://tenor.com/view/glitter-sparkle-gif-14595151")
				.text("Glitter Background");

		footer.appendText(" by CarmellaAmoroso");

		response.setBody(doc);
	}

	@Override
	public void getFile(HTTPRequest request, HTTPResponse response) {
		Main.getResourceFile(request, response);
	}

	/**
	 * Generate a fortune based on the given <code>name</code> and <code>age</code>.
	 * Repeated calls with the same arguments will produce the same fortune.
	 */
	private String getFortune(String name, int age) {
		long hash = 0;

		if (md5 != null) {
			// Use MD5 hash if available
			md5.reset();

			md5.update(name.getBytes(HTTPServer.DEFAULT_CHARSET));
			md5.update((byte) (age >> 0));
			md5.update((byte) (age >> 8));
			md5.update((byte) (age >> 16));
			md5.update((byte) (age >> 24));

			byte[] hashBytes = md5.digest();
			for (int i = 0; i < Long.BYTES && i < hashBytes.length; i++) {
				hash |= (byte) hashBytes[i] << (8 * i);
			}
		} else {
			// Fallback simple hash function
			hash = (name + age).hashCode();
		}

		return fortunes[(int) Long.remainderUnsigned(hash, fortunes.length)];
	}

}
