package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Drone {

	private RoutePlanner routePlanner;
	private BuildingPlanner buildingPlanner;
	private ArrayList<String> flightPath = new ArrayList<String>(150); // Flightpath log to be written to file

	public ArrayList<String> getFlightPath() {
		return flightPath;
	}

	private Sensor[] orderedSensors; // An array of sensors in the desired visit order, as computed by the algorithm
	private ArrayList<Feature> markers = new ArrayList<Feature>(33); // GeoJSON features containing the sensors

	public ArrayList<Feature> getMarkers() {
		return markers;
	}

	private String[] colours = { "#00ff00", "#40ff00", "#80ff00", "#c0ff00", "#ffc000", "#ff8000", "#ff4000",
			"#ff0000" };
	private ArrayList<Polygon> noFlyZones;

	public Drone(ArrayList<Sensor> sensors, ArrayList<Polygon> noFlyZones, double[] start) {

		this.routePlanner = new RoutePlanner(sensors, noFlyZones, start); // Object to assist with routefinding
		this.buildingPlanner = new BuildingPlanner(noFlyZones); // Object to assist with building avoidance
		this.orderedSensors = this.routePlanner.generateOrdering(); // Get the desired order of sensors to visit
		this.noFlyZones = routePlanner.getNoFlyZones(); // Load the no fly zones into class for convenient access
	}

	public LineString fly() {
		var points = new ArrayList<Point>();
		// Ready the coordinates of the sensors
		var orderedCoordinates = new double[routePlanner.getNumberOfNodes() + 1][2];
		// The start point occurs twice, hence the +1 in the size above
		for (int i = 0; i < orderedCoordinates.length; i++) {
			orderedCoordinates[i] = routePlanner.getCoordinates()[routePlanner.getOrdering().get(i)];
		}
		var moves = 0;
		var prev = orderedCoordinates[0]; // Holds the current position of the drone
		points.add(Point.fromLngLat(prev[0], prev[1])); // Add the start point
		for (int node = 1; node < orderedCoordinates.length; node++) {
			// Iterate through all the nodes in the sequence to visit after starting
			var next = orderedCoordinates[node];

			if (routePlanner.inside(prev, next, this.noFlyZones)) {
				// If the straight line between the current point and the next sensor intersects
				// a no fly zone
				var vis = buildingPlanner.calcVisibiltyGraph(prev, next);
				var order = buildingPlanner.aStar(vis, vis[vis.length - 1], vis.length - 2, vis.length - 1);
				var markers = buildingPlanner.getVisibilityCoordinates(prev, next);
				var trail = new ArrayList<double[]>();
				order.forEach(t -> {
					trail.add(markers.get(t));
				});
				for (int node1 = 0; node1 < trail.size() - 2; node1++) {
					var second = trail.get(node1 + 1);
					var third = trail.get(node1 + 2);
					while (routePlanner.proper_inside(prev, third, this.noFlyZones)) {
						moves++;
						if (moves >= 150) {
							break;
						}
						var chosenAngle = 0;
						var theta = routePlanner.calcAngle(prev, second);
						var thetaApprox = Math.toRadians(Math.round(theta / 10.0) * 10);
						chosenAngle = (int) (Math.round(theta / 10.0) * 10);
						var proposedJump = new double[2];
						proposedJump[0] = prev[0] + 0.0003 * Math.cos(thetaApprox);
						proposedJump[1] = prev[1] + 0.0003 * Math.sin(thetaApprox);
						if (routePlanner.proper_inside(prev, proposedJump, this.noFlyZones)) {
							var minDst = Double.MAX_VALUE;
							var minJump = new double[2];
							for (int th = 0; th < 360; th += 10) {
								var angle = Math.toRadians(th);
								proposedJump[0] = prev[0] + 0.0003 * Math.cos(angle);
								proposedJump[1] = prev[1] + 0.0003 * Math.sin(angle);
								var proposedDst = routePlanner.calcDst(proposedJump, second);
								if (proposedDst < minDst
										&& !routePlanner.proper_inside(prev, proposedJump, this.noFlyZones)) {
									minDst = proposedDst;
									minJump = proposedJump.clone();
									chosenAngle = th;
								}
							}
							proposedJump = minJump.clone();
						}
						this.logMove(moves, prev, chosenAngle, proposedJump, "null");
						prev = proposedJump.clone();
						points.add(Point.fromLngLat(prev[0], prev[1]));
					}
				}
			}
			var dst = routePlanner.calcDst(prev, next);
			do {
				moves++;
				if (moves > 150) {
					break;
				}
				var chosenAngle = 0; // Holds the angle of the current move
				var theta = routePlanner.calcAngle(prev, next); // Find the true angle
				var thetaApprox = Math.toRadians(Math.round(theta / 10.0) * 10); // Find the closest legal angle
				var proposedJump = new double[2]; // Temporary variable to hold the move being considered
				// Try setting the move to go along the nearest legal angle
				proposedJump[0] = prev[0] + 0.0003 * Math.cos(thetaApprox);
				proposedJump[1] = prev[1] + 0.0003 * Math.sin(thetaApprox);
				chosenAngle = (int) (Math.round(theta / 10.0) * 10);
				if (routePlanner.proper_inside(prev, proposedJump, this.noFlyZones)) {
					// If the proposed move is illegal, try every possible legal angle and pick the
					// one which ends up closest to the next sensor
					var minDst = Double.MAX_VALUE; // Set min dist to infinity for now
					var minJump = new double[2];
					for (int th = 0; th < 360; th += 10) {
						var angle = Math.toRadians(th);
						proposedJump[0] = prev[0] + 0.0003 * Math.cos(angle);
						proposedJump[1] = prev[1] + 0.0003 * Math.sin(angle);
						var proposedDst = routePlanner.calcDst(proposedJump, next);
						if (proposedDst < minDst && !routePlanner.proper_inside(prev, proposedJump, this.noFlyZones)) {
							// Update the preferred move with this if it is better
							minDst = proposedDst;
							minJump = proposedJump.clone();
							chosenAngle = th;
						}
					}
					proposedJump = minJump.clone();
				}
				var loc = "null";
				if (routePlanner.calcDst(proposedJump, next) < 0.0002 && node < orderedCoordinates.length - 1) {
					loc = this.orderedSensors[node - 1].getLocation();
					this.sensorToMarker(this.orderedSensors[node - 1]);
				}
				this.logMove(moves, prev, chosenAngle, proposedJump, loc);
				prev = proposedJump.clone();
				points.add(Point.fromLngLat(prev[0], prev[1]));
				dst = routePlanner.calcDst(prev, next);
			} while (dst >= 0.0002);
		}
		System.out.println("Drone flight terminated after " + (points.size() - 1) + " moves");
		var moveList = points.stream().map(p -> Feature.fromGeometry(p)).collect(Collectors.toList());
		moveList.add(Feature.fromGeometry(LineString.fromLngLats(points)));
		var fc = FeatureCollection.fromFeatures(moveList);
		System.out.println(fc.toJson());
		return LineString.fromLngLats(points);
	}

	public String readSensor(Sensor s) {
		// First check the battery level
		var batteryLevel = s.getBattery();
		if (batteryLevel < 10.0) {
			return "null";
		}
		// If it's a valid reading, return it
		return s.getReading();
	}

	public void logMove(int moveNumber, double[] initialPos, int angle, double[] finalPos, String loc) {
		// Add the results of one jump to the flightpath log
		var logString = String.valueOf(moveNumber) + ",";
		logString += String.valueOf(initialPos[1]) + ",";
		logString += String.valueOf(initialPos[0]) + ",";
		logString += String.valueOf(angle) + ",";
		logString += String.valueOf(finalPos[1]) + ",";
		logString += String.valueOf(finalPos[0]) + ",";
		logString += loc;
		flightPath.add(logString);
	}

	public void sensorToMarker(Sensor s) {
		var reading = this.readSensor(s);
		var location = s.getLocation();
		var lng = s.getAddress().getCoordinates().getLng();
		var lat = s.getAddress().getCoordinates().getLat();
		var colour = "";
		var symbol = "";
		if (reading.equals("null")) {
			colour = "#000000";
		} else {
			// Perform integer division by 32 to find which range it is in
			var colour_range = (int) (Double.valueOf(reading) / 32);
			colour = this.colours[colour_range];
		}
		if (reading.equals("null")) {
			symbol = "cross";
		} else if (Double.valueOf(reading) < 128) {
			symbol = "lighthouse";
		} else {
			symbol = "danger";
		}

		var p = Point.fromLngLat(lng, lat);
		var feature = Feature.fromGeometry(p);
		feature.addStringProperty("location", location);
		feature.addStringProperty("rgb-string", colour);
		feature.addStringProperty("marker-color", colour);
		feature.addStringProperty("marker-symbol", symbol);
		markers.add(feature);
	}

}