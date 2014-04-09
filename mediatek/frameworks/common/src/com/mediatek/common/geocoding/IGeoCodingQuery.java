package com.mediatek.common.geocoding;

public interface IGeoCodingQuery {
	// Exposed methods to clients
	public static final String GET_INSTANCE = "getInstance";
	public String queryByNumber(String number);
}
