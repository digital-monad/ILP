package uk.ac.ed.inf.aqmaps;

import java.io.IOException;

public class App {
	public static void main(String[] args) throws IOException, InterruptedException {
		// Grab the map dates from command line arguments
		var day = args[0];
		var month = args[1];
		var year = args[2];
		var start = new double[2];
		// Read the start point
		start[1] = Double.valueOf(args[3]);
		start[0] = Double.valueOf(args[4]);
		var randomSeed = Integer.valueOf(args[5]);
		var port = args[6];
		// Create the webserver object which handles parsing server content
		var webServer = new WebServer("http://localhost:" + port + "/");
		// Extract relevant information from webserver
		var sensors = webServer.parseSensors(year, month, day);
		sensors.forEach(s -> {
			try {
				s.setAddress(webServer.getAddressFromLocation(s.getLocation()));
			} catch (IOException | InterruptedException e) {
				System.out.println("There's a problem with the webserver!");
				e.printStackTrace();
			}
		});
		var noFlyZones = webServer.getBuildings();
		var drone = new Drone(sensors, noFlyZones, start);
		// Instruct the drone to visit the sensors and report the flight
		var flightLineString = drone.fly();
		var flightPath = drone.getFlightPath();
		// Retrieve flight data and send to log files
		var flightpathFileName = "flightpath-" + day + "-" + month + "-" + year + ".txt";
		FileOutput.logFlightPath(flightPath, flightpathFileName);
		var sensorMarkers = drone.getMarkers();
		var geojsonFileName = "readings-" + day + "-" + month + "-" + year + ".geojson";
		FileOutput.createGeoJSON(flightLineString, sensorMarkers, geojsonFileName);

	}
}
