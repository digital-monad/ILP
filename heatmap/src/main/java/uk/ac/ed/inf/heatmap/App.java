package uk.ac.ed.inf.heatmap;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	double[] nw_corner = {55.946233,-3.192473};
    	double[] se_corner = {55.942617,-3.184319};
    	double lat_width = (se_corner[0] - nw_corner[0])/10;
    	double lng_width = (se_corner[1] - nw_corner[1])/10;
    	double[] lat_points = new double[11];
    	double[] lng_points = new double[11];
    	for(int i = 0; i < 11; i++) {
    		lng_points[i] = nw_corner[1] + i*lng_width;
    		lat_points[i] = nw_corner[0] + i*lat_width;
    	}
    	
    	Point[][][] points = new Point[100][1][5];
    	List<List<List<Point>>> zoints = new ArrayList<>();
    	
    	
    	for(int box = 0; box < 100; box++) {
    		int lng_idx = box/10;
    		int lat_idx = box % 10;
    		List<List<Point>> box_geometry = new ArrayList<>();
    		List<Point> box_vertices = new ArrayList<>();
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx],lat_points[lat_idx]));
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx+1],lat_points[lat_idx]));
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx+1],lat_points[lat_idx+1]));
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx],lat_points[lat_idx+1]));
    		box_vertices.add(Point.fromLngLat(lng_points[lng_idx],lat_points[lat_idx]));
    		box_geometry.add(box_vertices);
    		zoints.add(box_geometry);
    		
			/*
			 * points[box][0][0] = Point.fromLngLat(lng_points[lng_idx],
			 * lat_points[lat_idx]); points[box][0][1] =
			 * Point.fromLngLat(lng_points[lng_idx], lat_points[lat_idx+1]);
			 * points[box][0][2] = Point.fromLngLat(lng_points[lng_idx+1],
			 * lat_points[lat_idx+1]); points[box][0][3] =
			 * Point.fromLngLat(lng_points[lng_idx+1], lat_points[lat_idx]);
			 * points[box][0][4] = Point.fromLngLat(lng_points[lng_idx],
			 * lat_points[lat_idx]);
			 */
    	}
    	
    	List<Feature> boxes = new ArrayList<Feature>();
    	
    	for(int polygon = 0; polygon < 100; polygon++) {
    		Feature box = Feature.fromGeometry(Polygon.fromLngLats(zoints.get(polygon)));
    		boxes.add(box);
    	}
    	
    	FeatureCollection fin = FeatureCollection.fromFeatures(boxes);
    	System.out.println(fin.toJson());
    	
    	
    }
}
