package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class HeatmapUtils {
	
	private static String[] colours = {"#00ff00","#40ff00","#80ff00","#c0ff00","#ffc000","#ff8000","#ff4000","#ff0000"};
	
	public static int[] readPredictionsToArray(String filename) {
		int[] predictions_array = new int[100];
		try {
			File predictions = new File(filename);
			Scanner reader = new Scanner(predictions);
			for(int i = 0; i < 10; i++) {
				String data = reader.nextLine().replaceAll(" ", "");
				String[] text_data = data.split(",");
				for(int x = 0; x < 10; x++) {
					predictions_array[10*i + x] = Integer.parseInt(text_data[x]);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error - File was not found");
			e.printStackTrace();
		}
		
		return predictions_array;
	}
	
	public static String predictionToColour(int prediction_value) {
		int colour_range = prediction_value/32;
		return colours[colour_range];
	}

}
