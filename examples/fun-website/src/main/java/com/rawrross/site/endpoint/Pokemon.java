package com.rawrross.site.endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rawrross.server.HTTPRequest;
import com.rawrross.server.HTTPResponse;
import com.rawrross.server.HTTPServer;
import com.rawrross.site.Main;

public class Pokemon implements Endpoint {

	/** Type -> ID list */
	private HashMap<String, ArrayList<Integer>> pokemonIds;
	/** ID -> Pokemon */
	private HashMap<Integer, PokeData> pokemon;
	private String[] types;

	private class PokeData {

		final int id;
		final String name;
		final String prefix, suffix;
		final String type1, type2;
		final boolean isDualType;
		final boolean isHalfNormal;

		PokeData(String csvLine) {
			String[] fields = csvLine.split(",");

			id = Integer.parseInt(fields[0]);
			name = fields[1];
			type1 = fields[2];
			type2 = (fields[3].isBlank()) ? null : fields[3];
			prefix = fields[4];
			suffix = fields[5];
			isDualType = type2 != null;
			isHalfNormal = (isDualType && type1.equals("normal"));
		}

	}

	public Pokemon() throws IOException {
		String typesTxt = new String(Pokemon.class.getResourceAsStream("/pokemon/types.txt").readAllBytes(),
				HTTPServer.DEFAULT_CHARSET);
		String pokemonCsv = new String(Pokemon.class.getResourceAsStream("/pokemon/pokemon.csv").readAllBytes(),
				HTTPServer.DEFAULT_CHARSET);

		types = typesTxt.strip().split("\r?\n");
		pokemonIds = new HashMap<>();
		pokemon = new HashMap<>();

		for (String t : types) {
			pokemonIds.put(t, new ArrayList<>());
		}

		// Associate each pokemon with ID and types
		String[] lines = pokemonCsv.strip().split("\r?\n");
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];

			PokeData p = new PokeData(line);
			pokemon.put(p.id, p);

			pokemonIds.compute(p.type1, (t, list) -> {
				list.add(p.id);
				return list;
			});

			if (p.isDualType) {
				pokemonIds.compute(p.type2, (t, list) -> {
					list.add(p.id);
					return list;
				});
			}
		}
	}

	@Override
	public void getPage(HTTPRequest request, HTTPResponse response) {
		String type1Param = request.getParameter("type1");
		String type2Param = request.getParameter("type2");

		boolean showInputForm = (type1Param == null || type2Param == null);

		// Check type parameters
		if (!showInputForm) {
			type1Param = type1Param.toLowerCase();
			type2Param = type2Param.toLowerCase();

			// Either type is not random or type is unknown
			if ((!type1Param.equals("random") && !pokemonIds.containsKey(type1Param))
					|| (!type2Param.equals("random") && !pokemonIds.containsKey(type2Param))) {
				showInputForm = true;
			}
		}

		Document doc = Document.createShell("");

		doc.select("html")
				.attr("lang", "en");
		doc.head()
				.appendElement("title")
				.text("Pokemon Fusion");
		doc.head()
				.appendElement("link")
				.attr("rel", "stylesheet")
				.attr("href", "/form.css");
		doc.head()
				.appendElement("link")
				.attr("rel", "stylesheet")
				.attr("href", "/pokemon/style.css");

		doc.body()
				.appendElement("a")
				.attr("href", "/")
				.text("<< index");
		doc.body()
				.appendElement("h1")
				.text("Pokemon Fusion");

		Element formContainer = new Element("div")
				.addClass("form-container");

		Element form = formContainer.appendElement("form")
				.attr("method", "get")
				.attr("action", "/pokemon")
				.addClass("form");

		if (showInputForm) {
			doc.body()
					.appendElement("p")
					.text("Select two random Pokemon with the specified types and create a fusion with them!");

			Element formTable = form.appendElement("div")
					.addClass("form-table");

			// Type 1
			Element selectType1 = createTypeSelection("type1", "Select first type");
			formTable.appendChild(selectType1);

			// Type 2
			Element selectType2 = createTypeSelection("type2", "Select second type");
			formTable.appendChild(selectType2);

			// Submit button
			form.appendElement("button")
					.attr("type", "submit")
					.text("Create Fusion");
		} else {
			// Select a random pokemon type if requested
			String type1 = (type1Param.equals("random") ? types[(int) (Math.random() * types.length)] : type1Param);
			String type2 = (type2Param.equals("random") ? types[(int) (Math.random() * types.length)] : type2Param);

			// Get all pokemon of the given types
			ArrayList<Integer> ids1 = pokemonIds.get(type1);
			ArrayList<Integer> ids2 = pokemonIds.get(type2);

			// Select random pokemon
			int pokeId1 = ids1.get((int) (Math.random() * ids1.size()));
			int pokeId2 = ids2.get((int) (Math.random() * ids2.size()));
			PokeData poke1 = pokemon.get(pokeId1);
			PokeData poke2 = pokemon.get(pokeId2);
			String fusionName = getFusionName(pokeId1, pokeId2);
			String fusionType1;
			String fusionType2;

			// Assumes "Normal" is always 1st type

			if (Objects.equals(poke1.type1, poke2.type1) && Objects.equals(poke1.type2, poke2.type2)) {
				// Pokemon have same types
				fusionType1 = poke1.type1;
				fusionType2 = poke1.type2;
			} else if (poke1.isHalfNormal && Objects.equals(poke1.type2, poke2.type1) && !poke2.isDualType) {
				// Fusing half-normal pokemon with matching single-type pokemon
				fusionType1 = poke1.type1;
				fusionType2 = poke1.type2;
			} else if (poke2.isHalfNormal && Objects.equals(poke2.type2, poke1.type1) && !poke1.isDualType) {
				// Reverse of previous case
				fusionType1 = poke2.type1;
				fusionType2 = poke2.type2;
			} else {
				// Prefer other types over Normal type
				if (poke1.isDualType && Objects.equals(poke1.type1, "normal"))
					fusionType1 = poke1.type2;
				else
					fusionType1 = poke1.type1;

				// Prevent duplicate type
				if (poke2.isDualType && !fusionType1.equals(poke2.type2))
					fusionType2 = poke2.type2;
				else if (!fusionType1.equals(poke2.type1))
					fusionType2 = poke2.type1;
				else
					fusionType2 = null; // Fusion only has one type
			}

			// "Normal" type goes first
			if (Objects.equals(fusionType2, "normal")) {
				fusionType2 = fusionType1;
				fusionType1 = "normal";
			}

			doc.body()
					.appendElement("p")
					.text("Fusing %s type with %s type! Refresh the page to get another fusion."
							.formatted(type1, type2));

			Element fusionDiv = doc.body()
					.appendElement("div")
					.addClass("fusion");

			Element pokeCardLeft = fusionDiv.appendElement("div")
					.addClass("card")
					.addClass("operand");
			fusionDiv.appendElement("span")
					.addClass("operator")
					.text("+");
			Element pokeCardRight = fusionDiv.appendElement("div")
					.addClass("card")
					.addClass("operand");
			fusionDiv.appendElement("span")
					.addClass("operator")
					.text("=");
			Element cardResult = fusionDiv.appendElement("div")
					.addClass("card")
					.addClass("result");

			pokeCardLeft.appendElement("img")
					.attr("src", getPokemonImgUrl(pokeId1))
					.attr("alt", poke1.name);
			Element cardLeftTitle = pokeCardLeft.appendElement("div")
					.addClass("card-title");
			cardLeftTitle.appendElement("img")
					.attr("src", "/pokemon/type/" + poke1.type1 + ".png");
			if (poke1.isDualType) {
				cardLeftTitle.appendElement("img")
						.attr("src", "/pokemon/type/" + poke1.type2 + ".png");
			}
			cardLeftTitle.appendElement("span")
					.text(poke1.name);

			pokeCardRight.appendElement("img")
					.attr("src", getPokemonImgUrl(pokeId2))
					.attr("alt", poke2.name);
			Element cardRightTitle = pokeCardRight.appendElement("div")
					.addClass("card-title");
			cardRightTitle.appendElement("img")
					.attr("src", "/pokemon/type/" + poke2.type1 + ".png");
			if (poke2.isDualType) {
				cardRightTitle.appendElement("img")
						.attr("src", "/pokemon/type/" + poke2.type2 + ".png");
			}
			cardRightTitle.appendElement("span")
					.text(poke2.name);

			cardResult.appendElement("img")
					.attr("src", getFusionImgUrl(pokeId1, pokeId2))
					.attr("alt", fusionName);
			Element cardResultTitle = cardResult.appendElement("div")
					.addClass("card-title");
			cardResultTitle.appendElement("img")
					.attr("src", "/pokemon/type/" + fusionType1 + ".png");
			if (fusionType2 != null) {
				cardResultTitle.appendElement("img")
						.attr("src", "/pokemon/type/" + fusionType2 + ".png");
			}
			cardResultTitle.appendElement("span")
					.text(fusionName);

			// Go Again button
			form.appendElement("button")
					.attr("type", "submit")
					.text("Select New Types");
		}

		formContainer.appendTo(doc.body());

		// Credits
		doc.body().appendElement("p")
				.appendElement("i")
				.text("Fusion names and images by ")
				.appendElement("a")
				.attr("target", "_blank")
				.attr("href", "https://pokemon.alexonsager.net/")
				.text("pokemon.alexonsager.net");

		doc.body().appendElement("p")
				.appendElement("i")
				.text("Pokemon Type icons from Pokemon Sleep");

		response.setBody(doc);
	}

	@Override
	public void getFile(HTTPRequest request, HTTPResponse response) {
		Main.getResourceFile(request, response);
	}

	private Element createTypeSelection(String name, String label) {
		Element div = new Element("div")
				.addClass("form-row");

		// Label
		div.appendElement("label")
				.attr("for", name)
				.text(label);

		// Select
		Element select = div.appendElement("select")
				.id(name)
				.attr("name", name)
				.attr("required", true);

		// Default option
		select.appendElement("option")
				.attr("selected", true)
				.attr("value", "random")
				.text("--- random ---");

		// Pokemon type options
		for (String type : types) {
			select.appendElement("option")
					.attr("value", type)
					.text(StringUtils.capitalize(type));
		}

		return div;
	}

	private String getFusionName(int id1, int id2) {
		String prefix = pokemon.get(id1).prefix;
		String suffix = pokemon.get(id2).suffix;
		String fusionName;

		// Based on name fusion code by Alex Onsager
		if (prefix.endsWith(".")) {
			// Prefix ends with '.' - capitalize suffix (ex: Mr. Mime + other)
			fusionName = prefix + " " + WordUtils.capitalize(suffix);
		} else if (prefix.endsWith(suffix.substring(0, 2))) {
			// Last 2 letters in prefix == first 2 letters in suffix
			fusionName = prefix + suffix.substring(2);
		} else if (prefix.endsWith(suffix.substring(0, 1))) {
			// Last letter in prefix == first letter in suffix
			fusionName = prefix + suffix.substring(1);
		} else {
			fusionName = prefix + suffix;
		}

		return fusionName;
	}

	private static String getPokemonImgUrl(int id) {
		return "https://images.alexonsager.net/pokemon/%d.png".formatted(id);
	}

	private static String getFusionImgUrl(int id1, int id2) {
		return "https://images.alexonsager.net/pokemon/fused/%d/%d.%d.png".formatted(id2, id2, id1);
	}

}
