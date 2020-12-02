package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	private ArrayList<Integer> ordering;

	public RoutePlanner(ArrayList<Sensor> sensors, ArrayList<Polygon> noFlyZones, double[] start) {
		double[][] coordinates = new double[this.numberOfNodes][2];
		coordinates[0] = start;
		for (int i = 1; i < this.numberOfNodes; i++) {
			coordinates[i][0] = sensors.get(i - 1).getAddress().getCoordinates().getLng();
			coordinates[i][1] = sensors.get(i - 1).getAddress().getCoordinates().getLat();
		}
		this.coordinates = coordinates;
		this.noFlyZones = noFlyZones;
		this.start = start;
		this.sensors = sensors;
		this.calcDstMatrix();

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
					if (this.proper_inside(this.coordinates[row], this.coordinates[col])) {
						dstMatrix[row][col] += 0.0005;
					}
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
		return Math.toDegrees(Math.atan2(delta_lat, delta_lng));
	}

	public boolean tryReverse(int i, int j) {
		// Consider the effect of reversing the segment of the permutation between i and
		// j
		var permCopy = new ArrayList<Integer>(this.ordering);
		var currentCost = this.dstMatrix[permCopy.get(i - 1)][permCopy.get(i)]
				+ this.dstMatrix[permCopy.get(j)][permCopy.get(j + 1)];
		var newCost = this.dstMatrix[permCopy.get(i - 1)][permCopy.get(j)]
				+ this.dstMatrix[permCopy.get(i)][permCopy.get(j + 1)];
		if (newCost < currentCost) {
			var firstSegment = permCopy.subList(0, i);
			var lastSegment = permCopy.subList(j + 1, permCopy.size());
			var midSegment = permCopy.subList(i, j + 1);
			Collections.reverse(midSegment);
			var changedOrdering = new ArrayList<Integer>();
			changedOrdering.addAll(firstSegment);
			changedOrdering.addAll(midSegment);
			changedOrdering.addAll(lastSegment);
			this.ordering = new ArrayList<Integer>(changedOrdering);
			return true;
		}
		return false;
	}

	public void twoOptHeuristic() {
		var order = new ArrayList<Integer>(this.ordering);
		var better = true;
		while (better) {
			better = false;
			for (int j = 1; j < order.size() - 1; j++) {
				for (int i = 1; i < j; i++) {
					if (this.tryReverse(i, j)) {
						better = true;
					}
				}
			}
		}
	}

	public Sensor[] nearestInsertion() {
		var perm = new ArrayList<Integer>();
		for (int i = 1; i < this.dstMatrix.length; i++) {
			perm.add(i);
		}

		var cycle = new ArrayList<Integer>();
		cycle.add(0);
		cycle.add(0);

		while (cycle.size() < this.dstMatrix.length + 1) {
			// Find the node in perm which is closest to any node in cycle
			var minOverallDst = Double.MAX_VALUE;
			var overallNode = 0;
			for (int i : cycle) {
				// Find the closest node to i
				var minDst = Double.MAX_VALUE;
				var node = 0;
				for (int n : perm) {
					if (this.dstMatrix[n][i] < minDst) {
						minDst = this.dstMatrix[n][i];
						node = n;
					}
				}
				if (minDst < minOverallDst) {
					minOverallDst = minDst;
					overallNode = node;
				}
			}
			var minAdditionalCost = Double.MAX_VALUE;
			var bestLocation = 0;
			// Check all the edges of cycle
			for (int cycleEdge = 0; cycleEdge < cycle.size() - 1; cycleEdge++) {
				var edgeStart = cycle.get(cycleEdge);
				var edgeEnd = cycle.get(cycleEdge + 1);
				// Check the additional cost of inserting the node into this edge
				var additionalCost = this.dstMatrix[edgeStart][overallNode] + this.dstMatrix[overallNode][edgeEnd]
						- this.dstMatrix[edgeStart][edgeEnd];
				if (additionalCost < minAdditionalCost) {
					minAdditionalCost = additionalCost;
					bestLocation = cycleEdge;
				}
			}
			cycle.add(bestLocation + 1, overallNode);
			perm.remove(Integer.valueOf(overallNode));
		}
		var sensors = cycle.subList(1, cycle.size() - 1);
		var sensorList = new Sensor[this.sensors.size()];
		for (int s = 0; s < sensors.size(); s++) {
			sensorList[s] = this.sensors.get(sensors.get(s) - 1);
		}
		this.ordering = new ArrayList<Integer>(cycle);
		return sensorList;
	}

	public LineString createRoute() {
		var points = new ArrayList<Point>();
		var orderedCoordinates = new double[this.numberOfNodes + 1][2];
		for (int i = 0; i < orderedCoordinates.length; i++) {
			orderedCoordinates[i] = this.coordinates[this.ordering.get(i)];
		}
		var steps = 0;
		var prev = orderedCoordinates[0];
		points.add(Point.fromLngLat(prev[0], prev[1]));
		for (int i = 1; i < orderedCoordinates.length; i++) {
			var next = orderedCoordinates[i];

			if (this.inside(prev, next)) {
				var vis = this.calcVisibiltyGraph(prev, next);
				var order = this.aStar(vis, vis[vis.length - 1], vis.length - 2, vis.length - 1);
				var markers = this.getVisibilityCoordinates(prev, next);
				var trail = new ArrayList<double[]>();
				order.forEach(t -> {
					trail.add(markers.get(t));
				});
				var legalPath = this.pathFinder(trail);
				points.addAll(legalPath);
				prev = new double[] { legalPath.get(legalPath.size() - 1).longitude(),
						legalPath.get(legalPath.size() - 1).latitude() };
			}
			var dst = this.calcDst(prev, next);
			do {
				steps++;
				if (steps > 150) {
					break;
				}
				var theta = this.calcAngle(prev, next);
				var thetaApprox = Math.toRadians(Math.round(theta / 10.0) * 10);
				var proposedJump = new double[2];
				proposedJump[0] = prev[0] + 0.0003 * Math.cos(thetaApprox);
				proposedJump[1] = prev[1] + 0.0003 * Math.sin(thetaApprox);
				if (this.proper_inside(prev, proposedJump)) {
					var minDst = Double.MAX_VALUE;
					var minJump = new double[2];
					for (int th = 0; th < 360; th += 10) {
						var angle = Math.toRadians(th);
						proposedJump[0] = prev[0] + 0.0003 * Math.cos(angle);
						proposedJump[1] = prev[1] + 0.0003 * Math.sin(angle);
						var proposedDst = this.calcDst(proposedJump, next);
						if (proposedDst < minDst && !this.proper_inside(prev, proposedJump)) {
							minDst = proposedDst;
							minJump = proposedJump.clone();
						}
					}
					proposedJump = minJump.clone();
				}
				prev = proposedJump.clone();
				points.add(Point.fromLngLat(prev[0], prev[1]));
				dst = this.calcDst(prev, next);
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

	public ArrayList<double[]> getVisibilityCoordinates(double[] start, double[] end) {
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
		return nodes;
	}

	public double[][] calcVisibiltyGraph(double[] start, double[] end) {
		var temp = new ArrayList<Feature>();
		var nodes = this.getVisibilityCoordinates(start, end);
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
//		System.out.println(fc.toJson());
		return visibilityGraph;
	}

	public ArrayList<Integer> aStar(double[][] graph, double[] heuristic, int start, int goal) {

		var distances = new double[graph.length];
		Arrays.fill(distances, Integer.MAX_VALUE);
		distances[start] = 0;

		double[] priorities = new double[graph.length];
		Arrays.fill(priorities, Integer.MAX_VALUE);
		priorities[start] = heuristic[start];

		boolean[] visited = new boolean[graph.length];

		var branches = new ArrayList<ArrayList<Integer>>();
		for (double[] element : graph) {
			branches.add(new ArrayList<Integer>());
		}

		branches.get(start).add(start);

		while (true) {

			double lowestPriority = Integer.MAX_VALUE;
			int lowestPriorityIndex = -1;
			for (int i = 0; i < priorities.length; i++) {
				if (priorities[i] < lowestPriority && !visited[i]) {
					lowestPriority = priorities[i];
					lowestPriorityIndex = i;
				}
			}

			if (lowestPriorityIndex == -1) {
				return null;
			} else if (lowestPriorityIndex == goal) {
				return branches.get(lowestPriorityIndex);
			}

			for (int i = 0; i < graph[lowestPriorityIndex].length; i++) {
				if (graph[lowestPriorityIndex][i] != 0 && !visited[i]) {
					if (distances[lowestPriorityIndex] + graph[lowestPriorityIndex][i] < distances[i]) {
						distances[i] = distances[lowestPriorityIndex] + graph[lowestPriorityIndex][i];
						priorities[i] = distances[i] + heuristic[i];
						branches.get(i).clear();
						branches.get(i).addAll(branches.get(lowestPriorityIndex));
						branches.get(i).add(i);
					}
				}
			}

			visited[lowestPriorityIndex] = true;
		}
	}

	public ArrayList<Point> pathFinder(ArrayList<double[]> trail) {
		var points = new ArrayList<Point>();
		var markers = new ArrayList<Feature>();
		for (double[] d : trail) {
			markers.add(Feature.fromGeometry(Point.fromLngLat(d[0], d[1])));
		}
		points.add(Point.fromLngLat(trail.get(0)[0], trail.get(0)[1]));
		var curr = trail.get(0);
		for (int node = 0; node < trail.size() - 2; node++) {
			var dest = trail.get(node + 1);
			var next = trail.get(node + 2);
			var count = 0;
			while (this.proper_inside(curr, next)) {
				count++;
				if (count > 10) {
					break;
				}
				var theta = this.calcAngle(curr, dest);
				var thetaApprox = Math.toRadians(Math.round(theta / 10.0) * 10);
				var proposedJump = new double[2];
				proposedJump[0] = curr[0] + 0.0003 * Math.cos(thetaApprox);
				proposedJump[1] = curr[1] + 0.0003 * Math.sin(thetaApprox);
				if (this.proper_inside(curr, proposedJump)) {
					var minDst = Double.MAX_VALUE;
					var minJump = new double[2];
					for (int i = 0; i < 360; i += 10) {
						var angle = Math.toRadians(i);
						proposedJump[0] = curr[0] + 0.0003 * Math.cos(angle);
						proposedJump[1] = curr[1] + 0.0003 * Math.sin(angle);
						var proposedDst = this.calcDst(proposedJump, dest);
						if (proposedDst < minDst && !this.proper_inside(curr, proposedJump)) {
							minDst = proposedDst;
							minJump = proposedJump.clone();
						}
					}
					proposedJump = minJump.clone();
				}
				curr = proposedJump.clone();
				points.add(Point.fromLngLat(curr[0], curr[1]));
			}
		}
		var ls = LineString.fromLngLats(points);
		markers.add(Feature.fromGeometry(ls));
		var fc = FeatureCollection.fromFeatures(markers);
//		System.out.println(fc.toJson());
//		System.out.println(points.size());
		return points;
	}

}
