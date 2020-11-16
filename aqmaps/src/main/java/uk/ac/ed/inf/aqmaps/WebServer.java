package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Polygon;

public class WebServer {

	private String serverUrl;

	public WebServer(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	private String request(String url) throws IOException, InterruptedException {
		final var client = HttpClient.newHttpClient();
		final var request = HttpRequest.newBuilder().uri(URI.create(url)).build();
		final var response = client.send(request, BodyHandlers.ofString());
		return response.body();
	}

	public ArrayList<Sensor> parseSensors(String year, String month, String day)
			throws IOException, InterruptedException {
		var requestUrl = this.serverUrl + "maps/" + String.join("/", year, month, day) + "/air-quality-data.json";
		var responseString = this.request(requestUrl);
		final Type list_type = new TypeToken<ArrayList<Sensor>>() {
		}.getType();
		ArrayList<Sensor> sensors = new Gson().fromJson(responseString, list_type);
		return sensors;
	}

	public Address getAddressFromLocation(String locationString) throws IOException, InterruptedException {
		var words = locationString.split("\\.");
		var requestUrl = this.serverUrl + "words/" + String.join("/", words) + "/details.json";
		var responseString = this.request(requestUrl);
		var address = new Gson().fromJson(responseString, Address.class);
		return address;
	}

	public ArrayList<Polygon> getBuildings() throws IOException, InterruptedException {
		var requestUrl = this.serverUrl + "buildings/no-fly-zones.geojson";
		var responseString = this.request(requestUrl);
		var features = FeatureCollection.fromJson(responseString).features();
		var listOfPolygons = features.stream().map(f -> (Polygon) f.geometry()).collect(Collectors.toList());
		return new ArrayList<Polygon>(listOfPolygons);

	}

}
