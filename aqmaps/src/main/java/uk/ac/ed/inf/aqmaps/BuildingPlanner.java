package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class BuildingPlanner extends Planner {

	private ArrayList<Polygon> noFlyZones;

	public BuildingPlanner(ArrayList<Polygon> noFlyZones) {
		this.noFlyZones = noFlyZones;
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
					if (this.proper_inside(nodes.get(row), nodes.get(col), this.noFlyZones)) {
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
}
