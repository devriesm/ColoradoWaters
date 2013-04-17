package com.coloradowaters.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.coloradowaters.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;

public class StationMapFragment extends MapFragment
{
	private static final String EXTRA_KEY_OPTIONS = "options";
	
	private GoogleMapOptions mOptions;
	
	
	public static final StationMapFragment newInstance( GoogleMapOptions options )
	{
		StationMapFragment frag = new StationMapFragment();
		
		Bundle args = new Bundle();
		args.putParcelable( EXTRA_KEY_OPTIONS, options );
		
		frag.setArguments( args );
		
		return frag;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		
		Bundle args = getArguments();
		
		if( args != null && args.containsKey( EXTRA_KEY_OPTIONS ) )
		{
			mOptions = args.getParcelable( EXTRA_KEY_OPTIONS );
		}
	}
	
	
	
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		View mapView = super.onCreateView( inflater, container, savedInstanceState );
		
		View layout = inflater.inflate( R.layout.fragment_station_map, container, false );
		
		RelativeLayout mapContainer = (RelativeLayout) layout.findViewById( R.id.map_container );
		
		mapContainer.addView( mapView, 0 );
		
		return layout;
	}
	
	
	@Override
	public void onActivityCreated( Bundle savedInstanceState )
	{
		super.onActivityCreated( savedInstanceState );
		
		if( mOptions != null )
		{
			CameraUpdate update = CameraUpdateFactory.newCameraPosition( mOptions.getCamera() );
			getMap().animateCamera( update );
			
			getMap().setMapType( mOptions.getMapType() );
		}
	}
	
}
