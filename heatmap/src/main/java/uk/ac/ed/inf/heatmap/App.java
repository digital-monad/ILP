package uk.ac.ed.inf.heatmap;

import com.mapbox.geojson.FeatureCollection;

public class App {

	// Store the coordinates of 2 corners of the heat map
	private static final double[] nw_corner = { 55.946233, -3.192473 };
	private static final double[] se_corner = { 55.942617, -3.184319 };

	public static void main(String[] args) {
		var filename = args[0]; // Extract the filename as the first command line argument
		var predictions = FileIO.readPredictionsToArray(filename); // Read the predictions file to a 1x100 int array
		var edinburghGrid = new Grid(nw_corner, se_corner); // Create a custom Grid specifying the corners
		edinburghGrid.computePolygonCoordinates(); // Compute the vertices of the 100 polygons
		edinburghGrid.createFeaturesFromPoints(predictions); // Bundle the vertices into 100 Features
		// Create FeatureCollection from the 100 Features
		var feature_collection = FeatureCollection.fromFeatures(edinburghGrid.getFeatures());
		FileIO.writeToFile("heatmap.geojson", feature_collection.toJson()); // Write geojson to file heatmap.geojson
	}
}