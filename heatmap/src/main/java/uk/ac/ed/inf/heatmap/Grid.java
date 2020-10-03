package uk.ac.ed.inf.heatmap;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Grid {

	// Declare instance variables which are used by and populated by instance
	// methods

	// The set of 11 latitude points that define the heat map grid
	private double[] lat_points = new double[11];
	// The set of 11 longitude points that define the heat map grid
	private double[] lng_points = new double[11];
	// The set of points that from the 100 polygons
	private List<List<List<Point>>> box_coordinates_list;
	// The set of 100 polygon features
	private List<Feature> boxes;
	// The ordered set of colours for the air quality ranges
	private static String[] colours = { "#00ff00", "#40ff00", "#80ff00", "#c0ff00", "#ffc000", "#ff8000", "#ff4000",
			"#ff0000" };

	public Grid(double[] nw_corner, double[] se_corner) {
		// Find the width (lng_width) and height (lat_width) of each polygon
		var lat_width = (se_corner[0] - nw_corner[0]) / 10;
		var lng_width = (se_corner[1] - nw_corner[1]) / 10;
		// Find and populate the 11 lng and lat points that make up the grid
		for (var i = 0; i < 11; i++) {
			this.lng_points[i] = nw_corner[1] + i * lng_width;
			this.lat_points[i] = nw_corner[0] + i * lat_width;
		}
	}

	/**
	 * Uses the lng and lat grid values to compute the 5 points for each polygon
	 */
	public void computePolygonCoordinates() {
		var box_coordinates_list = new ArrayList<List<List<Point>>>();
		// Loop for each of the 100 polygons
		for (var box = 0; box < 100; box++) {
			// Find the row (lng) and col (lat) of the polygon in the grid
			var lat_idx = box / 10;
			var lng_idx = box % 10;
			// This will be the coordinates for each polygon (list of list of points)
			var box_geometry = new ArrayList<List<Point>>();
			// This will hold the 5 points defining the polygon
			var box_vertices = new ArrayList<Point>();
			// Add the 5 points, which are the latitude and longitude array indexed at the
			// row and column, one along horizontally, one along vertically, one along
			// horizontally and vertically, and the first point again
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx], lat_points[lat_idx]));
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx + 1], lat_points[lat_idx]));
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx + 1], lat_points[lat_idx + 1]));
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx], lat_points[lat_idx + 1]));
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx], lat_points[lat_idx]));
			box_geometry.add(box_vertices);
			box_coordinates_list.add(box_geometry);
		}
		// Update the relevant instance variable with the coordinates
		this.box_coordinates_list = box_coordinates_list;
	}

	/**
	 * Uses the prediction values and the coordinates calculated prior to create the
	 * 100 polygon features
	 */
	public void createFeaturesFromPoints(int[] predictions) {
		var boxes = new ArrayList<Feature>();
		// Loop for each of the polygons
		for (var polygon = 0; polygon < 100; polygon++) {
			// Create a single feature from the polygon formed by the relevant coordinates
			var box = Feature.fromGeometry(Polygon.fromLngLats(this.box_coordinates_list.get(polygon)));
			// Find the colour given the prediction value
			var box_colour = this.predictionToColour(predictions[polygon]);
			// Add the properties (colour, opacity) to the feature
			box.addNumberProperty("fill-opacity", 0.75);
			box.addStringProperty("rgb-string", box_colour);
			box.addStringProperty("fill", box_colour);
			// Add the feature to the list
			boxes.add(box);
		}
		// Update the relevant instance variable with the features
		this.boxes = boxes;
	}

	/**
	 * Convert a prediction value integer to the correct colour
	 */
	public String predictionToColour(int prediction_value) {
		// Perform integer division by 32 to find which range it is in
		var colour_range = prediction_value / 32;
		// Index the ordered colours by the computed range value
		return colours[colour_range];
	}

	/**
	 * Getter for the feature list
	 */
	public List<Feature> getFeatures() {
		return this.boxes;
	}
}