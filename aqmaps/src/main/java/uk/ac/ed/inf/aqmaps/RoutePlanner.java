package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;

import com.mapbox.geojson.Polygon;

public class RoutePlanner extends Planner {

	private double[][] coordinates;
	public double[][] dstMatrix;
	private ArrayList<Polygon> noFlyZones;
	private int numberOfNodes = 34;
	private ArrayList<Sensor> sensors = new ArrayList<Sensor>();
	private double[] start;

	public ArrayList<Polygon> getNoFlyZones() {
		return noFlyZones;
	}

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

	public double[][] getCoordinates() {
		return coordinates;
	}

	public int getNumberOfNodes() {
		return numberOfNodes;
	}

	public ArrayList<Integer> getOrdering() {
		return ordering;
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
					if (this.proper_inside(this.coordinates[row], this.coordinates[col], this.noFlyZones)) {
						dstMatrix[row][col] += 0.0005;
					}
				}
			}
		}
		this.dstMatrix = dstMatrix;
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

	public void nearestInsertion() {
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
		this.ordering = new ArrayList<Integer>(cycle);
	}

	public Sensor[] generateOrdering() {
		// Applies the combination of algorithms to produce a good ordering of sensors
		// for the drone to visit
		this.nearestInsertion();
		this.twoOptHeuristic();
		var sensorList = new Sensor[this.sensors.size()];
		var sensorOrdering = this.ordering.subList(1, this.ordering.size() - 1);
		for (int s = 0; s < sensorOrdering.size(); s++) {
			sensorList[s] = this.sensors.get(sensorOrdering.get(s) - 1);
		}
		return sensorList;
	}

}