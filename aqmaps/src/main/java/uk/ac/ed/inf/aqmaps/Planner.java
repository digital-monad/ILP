package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.mapbox.geojson.Polygon;

public abstract class Planner {

	public double calcDst(double[] point1, double[] point2) {
		// TODO - Remove the square root
		var dst = Math.sqrt(Math.pow(point1[0] - point2[0], 2) + Math.pow(point1[1] - point2[1], 2));
		return dst;
	}

	public double calcAngle(double[] point1, double[] point2) {
		var delta_lng = point2[0] - point1[0];
		var delta_lat = point2[1] - point1[1];
		var angle = Math.toDegrees(Math.atan2(delta_lat, delta_lng));
		if (angle < 0) {
			angle += 360;
		}
		return angle;
	}

	public boolean inside(double[] segmentStart, double[] segmentEnd, ArrayList<Polygon> noFlyZones) {
		for (Polygon building : noFlyZones) {
			for (int i = 0; i < building.coordinates().get(0).size() - 1; i++) {
				var lng1 = building.coordinates().get(0).get(i).coordinates().get(0);
				var lat1 = building.coordinates().get(0).get(i).coordinates().get(1);
				var lng2 = building.coordinates().get(0).get(i + 1).coordinates().get(0);
				var lat2 = building.coordinates().get(0).get(i + 1).coordinates().get(1);
				var lng3 = segmentStart[0];
				var lat3 = segmentStart[1];
				var lng4 = segmentEnd[0];
				var lat4 = segmentEnd[1];
				var intersect = Line2D.linesIntersect(lng1, lat1, lng2, lat2, lng3, lat3, lng4, lat4);
				if (intersect) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean proper_inside(double[] segmentStart, double[] segmentEnd, ArrayList<Polygon> noFlyZones) {
		for (Polygon building : noFlyZones) {
			var visitedVertices = new HashSet<ArrayList<Double>>();
			for (int i = 0; i < building.coordinates().get(0).size() - 1; i++) {
				var lng1 = building.coordinates().get(0).get(i).coordinates().get(0);
				var lat1 = building.coordinates().get(0).get(i).coordinates().get(1);
				var lng2 = building.coordinates().get(0).get(i + 1).coordinates().get(0);
				var lat2 = building.coordinates().get(0).get(i + 1).coordinates().get(1);
				var lng3 = segmentStart[0];
				var lat3 = segmentStart[1];
				var lng4 = segmentEnd[0];
				var lat4 = segmentEnd[1];
				var point1 = new ArrayList<Double>();
				point1.add(lng1);
				point1.add(lat1);
				visitedVertices.add(point1);
				if ((Arrays.equals(segmentStart, new double[] { lng1, lat1 })
						&& Arrays.equals(segmentEnd, new double[] { lng2, lat2 }))
						|| (Arrays.equals(segmentEnd, new double[] { lng1, lat1 })
								&& Arrays.equals(segmentStart, new double[] { lng2, lat2 }))) {
					break;

				} else {
					var point2 = new ArrayList<Double>();
					var point3 = new ArrayList<Double>();
					point2.add(lng3);
					point2.add(lat3);
					point3.add(lng4);
					point3.add(lat4);
					if (visitedVertices.contains(point2) && visitedVertices.contains(point3)) {
						return true;
					}
					var side1 = Line2D.relativeCCW(lng1, lat1, lng2, lat2, lng3, lat3);
					var side2 = Line2D.relativeCCW(lng1, lat1, lng2, lat2, lng4, lat4);
					if (side1 != side2 && side1 != 0 && side2 != 0) {
						var side3 = Line2D.relativeCCW(lng3, lat3, lng4, lat4, lng1, lat1);
						var side4 = Line2D.relativeCCW(lng3, lat3, lng4, lat4, lng2, lat2);
						if (side3 != side4 && side3 != 0 && side4 != 0) {
							return true;
						}
					}
				}

			}
		}
		return false;

	}
}
