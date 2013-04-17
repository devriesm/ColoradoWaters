package com.coloradowaters.model;


public interface ColoradoWaterSMS
{
	public static final String NAMESPACE = "http://www.dwr.state.co.us/";
	public static final String WSDL = "http://www.dwr.state.co.us/SMS_WebService/ColoradoWaterSMS.asmx?WSDL";
	public static final String SERVICE_URL = "http://www.dwr.state.co.us/SMS_WebService/ColoradoWaterSMS.asmx";
	
	public static final String GET_WATER_DISTRICTS = "GetWaterDistricts";
	public static final String GET_TRANSMITTING_STATIONS = "GetSMSTransmittingStations";
	public static final String GET_TRANSMITTING_STATION_VARIABLES = "GetSMSTransmittingStationVariables";
	public static final String GET_CURRENT_CONDITIONS = "GetSMSCurrentConditions";
	public static final String GET_PROVISIONAL_DATA = "GetSMSProvisionalData";
	
	public static final char UTM_LAT_ZONE = 'N';
	public static final int UTM_LNG_ZONE = 13;
	
	
	public static final String EXTRA_DIVISION = "division";
	public static final String EXTRA_WATER_DISTRICT = "water_district";
	public static final String EXTRA_STATION = "station";
	
	
	public static final int STATION_VAR_LOADER_ID = 1;
	public static final int STREAM_FLOW_LOADER_ID = 2;
	public static final int CURRENT_CONDITIONS_LOADER_ID = 3;
}
