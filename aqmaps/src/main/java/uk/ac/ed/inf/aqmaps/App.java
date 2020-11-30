package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class App {
	public static void main(String[] args) throws IOException, InterruptedException {
		var webServer = new WebServer("http://localhost:80/");
		var sensors = webServer.parseSensors("2020", "05", "01");
		sensors.forEach(s -> {
			try {
				s.setAddress(webServer.getAddressFromLocation(s.getLocation()));
			} catch (IOException | InterruptedException e) {
				System.out.println("There was a problem :(");
				e.printStackTrace();
			}
		});
		var noFlyZones = webServer.getBuildings();
		double[] start = { -3.1878, 55.9444 };
		var routePlanner = new RoutePlanner(sensors, noFlyZones, start);
		var listOfFeatures = new ArrayList<Feature>(33);
		var listOfPoints = new ArrayList<Point>(33);
		var ordering = routePlanner.greedyAlgorithm();
		for (int i = 0; i < ordering.length; i++) {
			var s = ordering[i];
			var c = s.getAddress().getCoordinates();
			var p = Point.fromLngLat(c.getLng(), c.getLat());
			listOfPoints.add(p);
			var pointFeature = Feature.fromGeometry(p);
			pointFeature.addNumberProperty("Ordering", i);
			pointFeature.addStringProperty("Location", s.getLocation());
			listOfFeatures.add(pointFeature);
		}
//		var line = Feature.fromGeometry(routePlanner.createRoute());
//		listOfFeatures.add(line);
//		for (Polygon p : noFlyZones) {
//			listOfFeatures.add(Feature.fromGeometry(p));
//		}
//		FeatureCollection fc = FeatureCollection.fromFeatures(listOfFeatures);
//		System.out.println(fc.toJson());
		var s = new double[] { -3.1897741556167603, 55.9422907282583 };
		var e = new double[] { -3.1859225034713745, 55.94469403158613 };
		var test = routePlanner.calcVisibiltyGraph(s, e);
		var grail = new ArrayList<double[]>();
		var trail = routePlanner.aStar(test, test[test.length - 1], test.length - 2, test.length - 1);
		var markers = routePlanner.getVisibilityCoordinates(s, e);
		trail.forEach(t -> {
			grail.add(markers.get(t));
		});
		var ps = routePlanner.trailBlazer(grail);
		var ls = LineString.fromLngLats(ps);
		var f = Feature.fromGeometry(ls);
		var x = FeatureCollection.fromFeature(f);
		System.out.println(x.toJson());

	}
}
