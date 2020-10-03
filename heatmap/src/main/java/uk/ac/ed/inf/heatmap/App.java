package uk.ac.ed.inf.heatmap;

import com.mapbox.geojson.FeatureCollection;

public class App {

	private static final double[] nw_corner = { 55.946233, -3.192473 };
	private static final double[] se_corner = { 55.942617, -3.184319 };

	public static void main(String[] args) {
		var predictions = FileIO.readPredictionsToArray("predictions.txt");
		var edinburghGrid = new Grid(nw_corner, se_corner);
		edinburghGrid.computePolygonCoordinates();
		var features = edinburghGrid.createFeaturesFromPoints(predictions);
		var feature_collection = FeatureCollection.fromFeatures(features);
		FileIO.writeToFile("target\\heatmap.geojson", feature_collection.toJson());
	}
}