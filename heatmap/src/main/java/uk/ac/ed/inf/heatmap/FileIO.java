package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Helper class with static methods for dealing with file operations
 *
 * @author Rohan
 *
 */
public final class FileIO {

	private FileIO() {
		// This is a helper class that is never instantiated, hence private
		// constructor
	}

	/**
	 * Reads the predictions file into a 1x100 (1-D) integer array
	 */
	public static int[] readPredictionsToArray(String filename) {
		// Prepare the output array (size 100)
		var predictions_array = new int[100];
		try {
			var predictions = new File(filename);
			var reader = new Scanner(predictions);
			// Loop through the 10 rows of data
			for (var row = 0; row < 10; row++) {
				// Read line and strip spaces
				var data = reader.nextLine().replaceAll(" ", "");
				// Split the data on commas
				var text_data = data.split(",");
				// Loop through the 10 integer strings
				for (var column = 0; column < 10; column++) {
					// Insert parsed integer into next slot in array
					predictions_array[10 * row + column] = Integer.parseInt(text_data[column]);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// If the predictions file is not found
			System.out.println("Error - File was not found");
			e.printStackTrace();
		}
		return predictions_array;
	}

	/**
	 * Writes the content of the input 'content' to destination 'filename'
	 */
	public static void writeToFile(String filename, String content) {
		try {
			var writer = new FileWriter(filename);
			writer.write(content);
			writer.close();
			System.out.println("Successfully wrote to the file " + filename);
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

}