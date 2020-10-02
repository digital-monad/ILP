package uk.ac.ed.inf.heatmap;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class App
{
	
	private static final double[] nw_corner = {55.946233,-3.192473};
	private static final double[] se_corner = {55.942617,-3.184319};
	
    public static void main( String[] args )
    {
    	int[] predictions = HeatmapUtils.readPredictionsToArray("C:\\Users\\Rohan\\git\\ILP\\heatmap\\src\\main\\java\\uk\\ac\\ed\\inf\\heatmap\\predictions.txt");
    	double lat_width = (se_corner[0] - nw_corner[0])/10;
    	double lng_width = (se_corner[1] - nw_corner[1])/10;
    	double[] lat_points = new double[11];
    	double[] lng_points = new double[11];
    	for(int i = 0; i < 11; i++) {
    		lng_points[i] = nw_corner[1] + i*lng_width;
    		lat_points[i] = nw_corner[0] + i*lat_width;
    	}
    	
    	var zoints = new ArrayList<List<List<Point>>>();
    	  	
    	for(int box = 0; box < 100; box++) {
    		int lat_idx = box/10;
    		int lng_idx = box % 10;
    		var box_geometry = new ArrayList<List<Point>>();
    		var box_vertices = new ArrayList<Point>();
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx],lat_points[lat_idx]));
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx+1],lat_points[lat_idx]));
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx+1],lat_points[lat_idx+1]));
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx],lat_points[lat_idx+1]));
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx],lat_points[lat_idx]));
    		box_geometry.add(box_vertices);
    		zoints.add(box_geometry);
    	}
    	
    	List<Feature> boxes = new ArrayList<Feature>();
    	
    	for(int polygon = 0; polygon < 100; polygon++) {
    		Feature box = Feature.fromGeometry(Polygon.fromLngLats(zoints.get(polygon)));
    		String box_colour = HeatmapUtils.predictionToColour(predictions[polygon]);
    		box.addNumberProperty("fill-opacity", 0.75);
    		box.addStringProperty("rgb-string", box_colour);
    		box.addStringProperty("fill", box_colour);
    		boxes.add(box);
    	}
    	
    	FeatureCollection fin = FeatureCollection.fromFeatures(boxes);
    	System.out.println(fin.toJson());
    	
    	
    	
    }
}
