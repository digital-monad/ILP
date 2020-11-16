package uk.ac.ed.inf.aqmaps;

public class Address {

	private Coordinate coordinates;

	public Coordinate getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Coordinate coordinates) {
		this.coordinates = coordinates;
	}

	public static class Coordinate {
		private double lng;

		public double getLng() {
			return lng;
		}

		public void setLng(double lng) {
			this.lng = lng;
		}

		public double getLat() {
			return lat;
		}

		public void setLat(double lat) {
			this.lat = lat;
		}

		private double lat;
	}

}
