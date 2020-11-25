package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Arrays;

public class BuildingAvoidance {

	public static ArrayList<Integer> aStar(double[][] graph, double[][] heuristic, int start, int goal) {

		// This contains the distances from the start node to all other nodes
		var distances = new double[graph.length];
		// Initializing with a distance of "Infinity"
		Arrays.fill(distances, Integer.MAX_VALUE);
		// The distance from the start node to itself is of course 0
		distances[start] = 0;

		// This contains the priorities with which to visit the nodes, calculated using
		// the heuristic.
		double[] priorities = new double[graph.length];
		// Initializing with a priority of "Infinity"
		Arrays.fill(priorities, Integer.MAX_VALUE);
		// start node has a priority equal to straight line distance to goal. It will be
		// the first to be expanded.
		priorities[start] = heuristic[start][goal];

		// This contains whether a node was already visited
		boolean[] visited = new boolean[graph.length];

		var branches = new ArrayList<ArrayList<Integer>>();
		for (double[] element : graph) {
			branches.add(new ArrayList<Integer>());
		}

		branches.get(0).add(0);

		// While there are nodes left to visit...
		while (true) {

			// ... find the node with the currently lowest priority...
			double lowestPriority = Integer.MAX_VALUE;
			int lowestPriorityIndex = -1;
			for (int i = 0; i < priorities.length; i++) {
				// ... by going through all nodes that haven't been visited yet
				if (priorities[i] < lowestPriority && !visited[i]) {
					lowestPriority = priorities[i];
					lowestPriorityIndex = i;
				}
			}

			if (lowestPriorityIndex == -1) {
				// There was no node not yet visited --> Node not found
				return null;
			} else if (lowestPriorityIndex == goal) {
				// Goal node found
				System.out.println("Goal node found!");
				return branches.get(lowestPriorityIndex);
			}

			System.out.println(
					"Visiting node " + lowestPriorityIndex + " with currently lowest priority of " + lowestPriority);

			// ...then, for all neighboring nodes that haven't been visited yet....
			for (int i = 0; i < graph[lowestPriorityIndex].length; i++) {
				if (graph[lowestPriorityIndex][i] != 0 && !visited[i]) {
					// ...if the path over this edge is shorter...
					if (distances[lowestPriorityIndex] + graph[lowestPriorityIndex][i] < distances[i]) {
						// ...save this path as new shortest path
						distances[i] = distances[lowestPriorityIndex] + graph[lowestPriorityIndex][i];
						// ...and set the priority with which we should continue with this node
						priorities[i] = distances[i] + heuristic[i][goal];
						branches.get(i).clear();
						branches.get(i).addAll(branches.get(lowestPriorityIndex));
						branches.get(i).add(i);
						System.out.println("Updating distance of node " + i + " to " + distances[i]
								+ " and priority to " + priorities[i]);
					}
				}
			}

			// Lastly, note that we are finished with this node.
			visited[lowestPriorityIndex] = true;
			// System.out.println("Visited nodes: " + Arrays.toString(visited));
			// System.out.println("Currently lowest distances: " +
			// Arrays.toString(distances));

		}
	}

}
