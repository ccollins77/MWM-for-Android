package org.metawatch.manager;

public class CoordinateConvert {
	
	public static final double DEG_TO_RAD = Math.PI / 180;
	public static final double RAD_TO_DEG = 180 / Math.PI;
	
	public static class OsGridRef {
		public double easting, northing;
		
		public OsGridRef(double easting, double northing) {
			this.easting = easting;
			this.northing = northing;
		}
		
		@Override
		public String toString() {
			return toString(10);
		}
		
		public String toString(int digits) {
		  // get the 100km-grid indices
		  int e100k = (int) Math.floor(easting/100000), n100k = (int) Math.floor(northing/100000);
		  
		  if (e100k<0 || e100k>6 || n100k<0 || n100k>12) return "";
	
		  // translate those into numeric equivalents of the grid letters
		  int l1 = (int) ((19-n100k) - (19-n100k)%5 + Math.floor((e100k+10)/5));
		  int l2 = (19-n100k)*5%25 + e100k%5;
	
		  // compensate for skipped 'I' and build grid letter-pairs
		  if (l1 > 7) l1++;
		  if (l2 > 7) l2++;
		  char c1 = (char) ('A' + l1);
		  char c2 = (char) ('A' + l2);
	
		  // strip 100km-grid indices from easting & northing, and reduce precision
		  int e = (int) Math.floor((easting%100000)/Math.pow(10,5-digits/2));
		  int n = (int) Math.floor((northing%100000)/Math.pow(10,5-digits/2));
		  int d = digits / 2;
	
		  return String.format("%c%c %0"+d+"d %0"+d+"d", c1, c2, e, n);
		}
		
		public String getLetters() {
		  // get the 100km-grid indices
		  int e100k = (int) Math.floor(easting/100000), n100k = (int) Math.floor(northing/100000);
		  
		  if (e100k<0 || e100k>6 || n100k<0 || n100k>12) return "";
		  // translate those into numeric equivalents of the grid letters
		  int l1 = (int) ((19-n100k) - (19-n100k)%5 + Math.floor((e100k+10)/5));
		  int l2 = (19-n100k)*5%25 + e100k%5;
	
		  // compensate for skipped 'I' and build grid letter-pairs
		  if (l1 > 7) l1++;
		  if (l2 > 7) l2++;
		  char c1 = (char) ('A' + l1);
		  char c2 = (char) ('A' + l2);
		  return String.format("%c%c", c1, c2);
		}
		
		public String getNumbers(int digits) {
		  // strip 100km-grid indices from easting & northing, and reduce precision
		  int e = (int) Math.floor((easting%100000)/Math.pow(10,5-digits/2));
		  int n = (int) Math.floor((northing%100000)/Math.pow(10,5-digits/2));
		  int d = digits / 2;
		  
		  int e100k = (int) Math.floor(easting/100000), n100k = (int) Math.floor(northing/100000);
		  
		  if (e100k<0 || e100k>6 || n100k<0 || n100k>12) return "";
	
		  return String.format("%0"+d+"d %0"+d+"d", e, n);
		}
	}
	
	public static class LatLongRef {
		/**
		 * Stored in DEGREES
		 */
		public double latitude, longitude;
		
		public LatLongRef(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}
		
		public double getLatitude() {
			return latitude;
		}
		public double getLongitude() {
			return longitude;
		}
		public double getLatitudeRad() {
			return latitude * DEG_TO_RAD;
		}
		public double getLongitudeRad() {
			return longitude * DEG_TO_RAD;
		}
	}


	/**
	 * Convert (OSGB36) latitude/longitude to Ordnance Survey grid reference easting/northing coordinate
	 *
	 * @param {latLon} point: OSGB36 latitude/longitude
	 * @return {OsGridRef} OS Grid Reference easting/northing
	 *
	 */
	public static final OsGridRef latLongToOs(LatLongRef latLong) {
	  double lat = latLong.getLatitudeRad();
	  double lon = latLong.getLongitudeRad();
	  
	  double a = 6377563.396, b = 6356256.910;          // Airy 1830 major & minor semi-axes
	  double F0 = 0.9996012717;                         // NatGrid scale factor on central meridian
	  double lat0 = 49 * DEG_TO_RAD, lon0 = -2 * DEG_TO_RAD;  // NatGrid true origin is 49ºN,2ºW
	  double N0 = -100000, E0 = 400000;                 // northing & easting of true origin, metres
	  double e2 = 1 - (b*b)/(a*a);                      // eccentricity squared
	  double n = (a-b)/(a+b), n2 = n*n, n3 = n*n*n;

	  double cosLat = Math.cos(lat), sinLat = Math.sin(lat);
	  double nu = a*F0/Math.sqrt(1-e2*sinLat*sinLat);              // transverse radius of curvature
	  double rho = a*F0*(1-e2)/Math.pow(1-e2*sinLat*sinLat, 1.5);  // meridional radius of curvature
	  double eta2 = nu/rho-1;

	  double Ma = (1 + n + (5/4)*n2 + (5/4)*n3) * (lat-lat0);
	  double Mb = (3*n + 3*n*n + (21/8)*n3) * Math.sin(lat-lat0) * Math.cos(lat+lat0);
	  double Mc = ((15/8)*n2 + (15/8)*n3) * Math.sin(2*(lat-lat0)) * Math.cos(2*(lat+lat0));
	  double Md = (35/24)*n3 * Math.sin(3*(lat-lat0)) * Math.cos(3*(lat+lat0));
	  double M = b * F0 * (Ma - Mb + Mc - Md);              // meridional arc

	  double cos3lat = cosLat*cosLat*cosLat;
	  double cos5lat = cos3lat*cosLat*cosLat;
	  double tan2lat = Math.tan(lat)*Math.tan(lat);
	  double tan4lat = tan2lat*tan2lat;

	  double I = M + N0;
	  double II = (nu/2)*sinLat*cosLat;
	  double III = (nu/24)*sinLat*cos3lat*(5-tan2lat+9*eta2);
	  double IIIA = (nu/720)*sinLat*cos5lat*(61-58*tan2lat+tan4lat);
	  double IV = nu*cosLat;
	  double V = (nu/6)*cos3lat*(nu/rho-tan2lat);
	  double VI = (nu/120) * cos5lat * (5 - 18*tan2lat + tan4lat + 14*eta2 - 58*tan2lat*eta2);

	  double dLon = lon-lon0;
	  double dLon2 = dLon*dLon, dLon3 = dLon2*dLon, dLon4 = dLon3*dLon, dLon5 = dLon4*dLon, dLon6 = dLon5*dLon;

	  double N = I + II*dLon2 + III*dLon4 + IIIA*dLon6;
	  double E = E0 + IV*dLon + V*dLon3 + VI*dLon5;

	  return new OsGridRef(E, N);
	}

	/**
	 * Convert Ordnance Survey grid reference easting/northing coordinate to (OSGB36) latitude/longitude
	 *
	 * @param {OsGridRef} easting/northing to be converted to latitude/longitude
	 * @return {LatLon} latitude/longitude (in OSGB36) of supplied grid reference
	 *
	 * @requires LatLon
	 */
	public static final LatLongRef osGridToLatLong(OsGridRef coord) {
	  double E = coord.easting;
	  double N = coord.northing;

	  double a = 6377563.396, b = 6356256.910;              // Airy 1830 major & minor semi-axes
	  double F0 = 0.9996012717;                             // NatGrid scale factor on central meridian
	  double lat0 = 49*Math.PI/180, lon0 = -2*Math.PI/180;  // NatGrid true origin
	  double N0 = -100000, E0 = 400000;                     // northing & easting of true origin, metres
	  double e2 = 1 - (b*b)/(a*a);                          // eccentricity squared
	  double n = (a-b)/(a+b), n2 = n*n, n3 = n*n*n;

	  double lat=lat0, M=0;
	  do {
	    lat = (N-N0-M)/(a*F0) + lat;

	    double Ma = (1 + n + (5/4)*n2 + (5/4)*n3) * (lat-lat0);
	    double Mb = (3*n + 3*n*n + (21/8)*n3) * Math.sin(lat-lat0) * Math.cos(lat+lat0);
	    double Mc = ((15/8)*n2 + (15/8)*n3) * Math.sin(2*(lat-lat0)) * Math.cos(2*(lat+lat0));
	    double Md = (35/24)*n3 * Math.sin(3*(lat-lat0)) * Math.cos(3*(lat+lat0));
	    M = b * F0 * (Ma - Mb + Mc - Md);                // meridional arc

	  } while (N-N0-M >= 0.00001);  // ie until < 0.01mm

	  double cosLat = Math.cos(lat), sinLat = Math.sin(lat);
	  double nu = a*F0/Math.sqrt(1-e2*sinLat*sinLat);              // transverse radius of curvature
	  double rho = a*F0*(1-e2)/Math.pow(1-e2*sinLat*sinLat, 1.5);  // meridional radius of curvature
	  double eta2 = nu/rho-1;

	  double tanLat = Math.tan(lat);
	  double tan2lat = tanLat*tanLat, tan4lat = tan2lat*tan2lat, tan6lat = tan4lat*tan2lat;
	  double secLat = 1/cosLat;
	  double nu3 = nu*nu*nu, nu5 = nu3*nu*nu, nu7 = nu5*nu*nu;
	  double VII = tanLat/(2*rho*nu);
	  double VIII = tanLat/(24*rho*nu3)*(5+3*tan2lat+eta2-9*tan2lat*eta2);
	  double IX = tanLat/(720*rho*nu5)*(61+90*tan2lat+45*tan4lat);
	  double X = secLat/nu;
	  double XI = secLat/(6*nu3)*(nu/rho+2*tan2lat);
	  double XII = secLat/(120*nu5)*(5+28*tan2lat+24*tan4lat);
	  double XIIA = secLat/(5040*nu7)*(61+662*tan2lat+1320*tan4lat+720*tan6lat);

	  double dE = (E-E0), dE2 = dE*dE, dE3 = dE2*dE, dE4 = dE2*dE2, dE5 = dE3*dE2, dE6 = dE4*dE2, dE7 = dE5*dE2;
	  lat = lat - VII*dE2 + VIII*dE4 - IX*dE6;
	  double lon = lon0 + X*dE - XI*dE3 + XII*dE5 - XIIA*dE7;
	  
	  return new LatLongRef(lat * RAD_TO_DEG, lon * RAD_TO_DEG);
	}
}
