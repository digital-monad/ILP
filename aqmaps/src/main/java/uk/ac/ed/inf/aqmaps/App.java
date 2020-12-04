package uk.ac.ed.inf.aqmaps;

import java.io.IOException;

public class App {
	public static void main(String[] args) throws IOException, InterruptedException {
		var webServer = new WebServer("http://localhost:80/");
		var sensors = webServer.parseSensors("2020", "11", "11");
		sensors.forEach(s -> {
			try {
				s.setAddress(webServer.getAddressFromLocation(s.getLocation()));
			} catch (IOException | InterruptedException e) {
				System.out.println("Yer webserver ainnie workin lad! :(");
				e.printStackTrace();
			}
		});
		var noFlyZones = webServer.getBuildings();
		double[] start = { -3.1878, 55.9444 };
		var drone = new Drone(sensors, noFlyZones, start);
		var l = drone.fly();

	}
}
