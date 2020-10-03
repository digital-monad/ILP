package uk.ac.ed.inf.heatmap;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Grid {

	private double[] lat_points = new double[11];
	private double[] lng_points = new double[11];
	private List<List<List<Point>>> box_coordinates_list;

	public Grid(double[] nw_corner, double[] se_corner) {
		var lat_width = (se_corner[0] - nw_corner[0]) / 10;
		var lng_width = (se_corner[1] - nw_corner[1]) / 10;
		for (var i = 0; i < 11; i++) {
			this.lng_points[i] = nw_corner[1] + i * lng_width;
			this.lat_points[i] = nw_corner[0] + i * lat_width;
		}
	}

	public List<List<List<Point>>> computePolygonCoordinates() {
		var box_coordinates_list = new ArrayList<List<List<Point>>>();
		for (var box = 0; box < 100; box++) {
			var lat_idx = box / 10;
			var lng_idx = box % 10;
			var box_geometry = new ArrayList<List<Point>>();
			var box_vertices = new ArrayList<Point>();
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx], lat_points[lat_idx]));
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx + 1], lat_points[lat_idx]));
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx + 1], lat_points[lat_idx + 1]));
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx], lat_points[lat_idx + 1]));
			box_vertices.add(Point.fromLngLat(lng_points[lng_idx], lat_points[lat_idx]));
			box_geometry.add(box_vertices);
			box_coordinates_list.add(box_geometry);
		}
		this.box_coordinates_list = box_coordinates_list;
		return box_coordinates_list;
	}

	public List<Feature> createFeaturesFromPoints(int[] predictions) {
		var boxes = new ArrayList<Feature>();
		for (var polygon = 0; polygon < 100; polygon++) {
			var box = Feature.fromGeometry(Polygon.fromLngLats(this.box_coordinates_list.get(polygon)));
			var box_colour = FileIO.predictionToColour(predictions[polygon]);
			box.addNumberProperty("fill-opacity", 0.75);
			box.addStringProperty("rgb-string", box_colour);
			box.addStringProperty("fill", box_colour);
			boxes.add(box);
		}
		return boxes;
	}
}
