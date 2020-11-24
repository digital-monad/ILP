package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class RoutePlanner {

	private double[][] coordinates;
	public double[][] dstMatrix;
	private ArrayList<Polygon> noFlyZones;
	private int numberOfNodes = 34;
	private ArrayList<Sensor> sensors = new ArrayList<Sensor>();
	private double[] start;
	private int[] ordering;

	public RoutePlanner(ArrayList<Sensor> sensors, ArrayList<Polygon> noFlyZones, double[] start) {
		double[][] coordinates = new double[this.numberOfNodes][2];
		coordinates[0] = start;
		for (int i = 1; i < this.numberOfNodes; i++) {
			coordinates[i][0] = sensors.get(i - 1).getAddress().getCoordinates().getLng();
			coordinates[i][1] = sensors.get(i - 1).getAddress().getCoordinates().getLat();
		}
		this.coordinates = coordinates;
		this.calcDstMatrix();
		this.noFlyZones = noFlyZones;
		this.start = start;
		this.sensors = sensors;
//		for (int i = 0; i < this.numberOfNodes; i++) {
//			this.sensors.add(sensors.get(i-1));
//		}

	}

	public void calcDstMatrix() {
		double[][] dstMatrix = new double[this.numberOfNodes][this.numberOfNodes];
		for (int row = 0; row < this.numberOfNodes; row++) {
			for (int col = 0; col < this.numberOfNodes; col++) {
				if (row > col) {
					dstMatrix[row][col] = dstMatrix[col][row];
				} else if (row == col) {
					dstMatrix[row][col] = 0;
				} else {
					dstMatrix[row][col] = this.calcDst(this.coordinates[row], this.coordinates[col]);
				}
			}
		}
		this.dstMatrix = dstMatrix;
	}

	public double calcDst(double[] point1, double[] point2) {
		// TODO - Remove the square root
		var dst = Math.sqrt(Math.pow(point1[0] - point2[0], 2) + Math.pow(point1[1] - point2[1], 2));
		return dst;
	}

	public double calcAngle(double[] point1, double[] point2) {
		var delta_lng = point2[0] - point1[0];
		var delta_lat = point2[1] - point1[1];
		return Math.toDegrees(Math.atan2(delta_lng, delta_lat));
	}

	public Sensor[] greedyAlgorithm() {
		var ordering = new int[this.numberOfNodes];
		for (int i = 0; i < ordering.length; i++) {
			ordering[i] = i;
		}

		for (int idx = 0; idx < this.numberOfNodes - 2; idx++) {
			var rest = Arrays.copyOfRange(ordering, idx + 1, ordering.length);
			var minDst = 100.0;
			var closestSensorIdx = 0;
			for (int sensorIdx = 0; sensorIdx < rest.length; sensorIdx++) {
				var dst = this.dstMatrix[ordering[idx]][ordering[idx + sensorIdx + 1]];
				if (dst < minDst) {
					closestSensorIdx = idx + sensorIdx + 1;
					minDst = dst;
				}
			}
			var swapSensor = ordering[closestSensorIdx];
			ordering[closestSensorIdx] = ordering[idx + 1];
			ordering[idx + 1] = swapSensor;

		}
		var augmentedOrdering = new int[this.numberOfNodes + 1];
		for (int i = 0; i < ordering.length; i++) {
			augmentedOrdering[i] = ordering[i];
		}
		augmentedOrdering[this.numberOfNodes] = 0;
		var sensorOrdering = new Sensor[this.numberOfNodes - 1];
		for (int i = 1; i < this.numberOfNodes; i++) {
			sensorOrdering[i - 1] = this.sensors.get(ordering[i] - 1);
		}
		this.ordering = augmentedOrdering;
		return sensorOrdering;
	}

	public LineString createRoute() {
		var points = new ArrayList<Point>();
		var orderedCoordinates = new double[this.numberOfNodes + 1][2];
		for (int i = 0; i < orderedCoordinates.length; i++) {
			orderedCoordinates[i] = this.coordinates[this.ordering[i]];
		}
		var steps = 0;
		var prev = orderedCoordinates[0];
		for (int i = 1; i < orderedCoordinates.length; i++) {
			var next = orderedCoordinates[i];
			var dst = this.calcDst(prev, next);
			do {
				steps++;
				if (steps > 150) {
					break;
				}
				var theta = this.calcAngle(prev, next);
				var thetaApprox = Math.toRadians(Math.round(theta / 10.0) * 10);
				var proposedJump = new double[2];
				proposedJump[0] = prev[0] + 0.0003 * Math.sin(thetaApprox);
				proposedJump[1] += prev[1] + 0.0003 * Math.cos(thetaApprox);
				while (this.inside(prev, proposedJump)) {
					thetaApprox += 10 * Math.PI / 180;
					proposedJump[0] = prev[0] + 0.0003 * Math.sin(thetaApprox);
					proposedJump[1] = prev[1] + 0.0003 * Math.cos(thetaApprox);
				}
				prev = proposedJump;
				dst = this.calcDst(prev, next);
				var q = Point.fromLngLat(prev[0], prev[1]);
				points.add(q);
			} while (dst > 0.0002);
		}
		System.out.println(points.size());

		return LineString.fromLngLats(points);

	}

	public boolean inside(double[] segmentStart, double[] segmentEnd) {
		for (Polygon building : this.noFlyZones) {
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

	public boolean proper_inside(double[] segmentStart, double[] segmentEnd) {
		for (Polygon building : this.noFlyZones) {
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

	public double[][] calcVisibiltyGraph(double[] start, double[] end) {
		var temp = new ArrayList<Feature>();
		var nodes = new ArrayList<double[]>();
		for (Polygon building : this.noFlyZones) {
			var vertices = building.coordinates().get(0);
			vertices = vertices.subList(0, vertices.size() - 1);
			var lnglats = vertices.stream().map(p -> new double[] { p.longitude(), p.latitude() })
					.collect(Collectors.toList());
			nodes.addAll(lnglats);
		}
		nodes.add(start);
		nodes.add(end);
		var visibilityGraph = new double[nodes.size()][nodes.size()];
		for (int row = 0; row < nodes.size(); row++) {
			for (int col = 0; col < nodes.size(); col++) {
				if (row > col) {
					visibilityGraph[row][col] = visibilityGraph[col][row];
				} else if (row == col) {
					visibilityGraph[row][col] = 0;
				} else {
					if (this.proper_inside(nodes.get(row), nodes.get(col))) {
						visibilityGraph[row][col] = 0;
					} else {
						visibilityGraph[row][col] = this.calcDst(nodes.get(row), nodes.get(col));
						var p1 = Point.fromLngLat(nodes.get(row)[0], nodes.get(row)[1]);
						var p2 = Point.fromLngLat(nodes.get(col)[0], nodes.get(col)[1]);
						var points = new ArrayList<Point>();
						points.add(p1);
						points.add(p2);
						temp.add(Feature.fromGeometry(LineString.fromLngLats(points)));
					}
				}
			}
		}
		var fc = FeatureCollection.fromFeatures(temp);
		System.out.println(fc.toJson());
		return visibilityGraph;
	}

}
