package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public final class FileIO {

	private FileIO() {
	}

	private static String[] colours = { "#00ff00", "#40ff00", "#80ff00", "#c0ff00", "#ffc000", "#ff8000", "#ff4000",
			"#ff0000" };

	public static int[] readPredictionsToArray(String filename) {
		var predictions_array = new int[100];
		try {
			var predictions = new File(filename);
			var reader = new Scanner(predictions);
			for (var i = 0; i < 10; i++) {
				var data = reader.nextLine().replaceAll(" ", "");
				var text_data = data.split(",");
				for (var x = 0; x < 10; x++) {
					predictions_array[10 * i + x] = Integer.parseInt(text_data[x]);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error - File was not found");
			e.printStackTrace();
		}
		return predictions_array;
	}

	public static void writeToFile(String filename, String content) {
		try {
			var myWriter = new FileWriter(filename);
			myWriter.write(content);
			myWriter.close();
			System.out.println("Successfully wrote to the file " + filename);
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	public static String predictionToColour(int prediction_value) {
		var colour_range = prediction_value / 32;
		return colours[colour_range];
	}

}
