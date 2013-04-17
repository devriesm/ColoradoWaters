package com.coloradowaters.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.coloradowaters.R;
import com.coloradowaters.model.ColoradoWaterSMS;
import com.coloradowaters.model.Station;

public class StationsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<Station>>
{
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";
	private static final int LOADER_ID = 0;
	
	private StationsListAdapter mAdapter;
	private int mListViewStatePosition;
	private int mListViewStateTop;
	
	
	@Override
	public void onActivityCreated( Bundle savedInstanceState )
	{
		super.onActivityCreated( savedInstanceState );
		
		mAdapter = new StationsListAdapter( getActivity() );
		setListAdapter( mAdapter );
		setEmptyText( getString( R.string.text_empty_stations_list ) );
		setListShown( false );
		getListView().setClipToPadding( false );
		getLoaderManager().initLoader( LOADER_ID, getArguments(), this );
	}
	
	
	@Override
	public StationsListAdapter getListAdapter()
	{
		return (StationsListAdapter) super.getListAdapter();
	}
	
	
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		if( savedInstanceState != null )
		{
			mListViewStatePosition = savedInstanceState.getInt( STATE_POSITION, -1 );
			mListViewStateTop = savedInstanceState.getInt( STATE_TOP, 0 );
		}else
		{
			mListViewStatePosition = -1;
			mListViewStateTop = 0;
		}
		
		return super.onCreateView( inflater, container, savedInstanceState );
	}
	
	
	@Override
	public void onSaveInstanceState( Bundle outState )
	{
		if( isAdded() )
		{
			View v = getListView().getChildAt( 0 );
			int top = ( v == null ) ? 0 : v.getTop();
			outState.putInt( STATE_POSITION, getListView().getFirstVisiblePosition() );
			outState.putInt( STATE_TOP, top );
		}
		super.onSaveInstanceState( outState );
	}
	
	
	@Override
	public void onListItemClick( ListView l, View v, int position, long id )
	{
		Station station = getListAdapter().getItem( position );
		Intent intent = new Intent( getActivity(), StationDetailsActivity.class );
		intent.putExtra( ColoradoWaterSMS.EXTRA_STATION, station );
		intent.putExtra( Intent.EXTRA_TITLE, station.name );
		startActivity( intent );
	}
	
	
	@Override
	public Loader<List<Station>> onCreateLoader( int id, Bundle args )
	{
		if( args != null )
			return new StationsLoader( getActivity(), args.getInt( ColoradoWaterSMS.EXTRA_DIVISION, 0 ), args.getInt( ColoradoWaterSMS.EXTRA_WATER_DISTRICT, 0 ) );
		else
			return null;
	}
	
	
	@Override
	public void onLoadFinished( Loader<List<Station>> loader, List<Station> data )
	{
		if( data != null )
		{
			mAdapter.update( data );
			mAdapter.notifyDataSetChanged();
		}
		
		setListShown( true );
		
		if( mListViewStatePosition != -1 && isAdded() )
		{
			getListView().setSelectionFromTop( mListViewStatePosition, mListViewStateTop );
			mListViewStatePosition = -1;
		}
	}
	
	
	@Override
	public void onLoaderReset( Loader<List<Station>> loader )
	{
		// no-op
	}
	
	
	private static final class StationsLoader extends AsyncTaskLoader<List<Station>>
	{
		private int mDivision;
		private int mWaterDistrict;
		private List<Station> mStations;
		private boolean mHasErrors;
		private boolean mIsLoading;
		
		
		public StationsLoader( Context context, int division, int waterDistrict )
		{
			super( context );
			mDivision = division;
			mWaterDistrict = waterDistrict;
			
			init();
		}
		
		
		private void init()
		{
			mStations = null;
			mHasErrors = false;
			mIsLoading = true;
		}
		
		
		@Override
		public List<Station> loadInBackground()
		{
			List<Station> stations = null;
			
			SoapObject request = new SoapObject( ColoradoWaterSMS.NAMESPACE, ColoradoWaterSMS.GET_TRANSMITTING_STATIONS );
			
			PropertyInfo divProp = new PropertyInfo();
			divProp.setName( "Div" );
			divProp.setValue( mDivision );
			divProp.setType( Integer.class );
			divProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( divProp );
			
			PropertyInfo wdProp = new PropertyInfo();
			wdProp.setName( "WD" );
			wdProp.setValue( mWaterDistrict );
			wdProp.setType( Integer.class );
			wdProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( wdProp );
			
			SoapSerializationEnvelope soapSerializationEnvelope = new SoapSerializationEnvelope( SoapEnvelope.VER11 );
			soapSerializationEnvelope.setOutputSoapObject( request );
			HttpTransportSE httpTransportSE = new HttpTransportSE( ColoradoWaterSMS.SERVICE_URL );
			SoapObject result = null;
			
			try
			{
				httpTransportSE.call( ColoradoWaterSMS.NAMESPACE + ColoradoWaterSMS.GET_TRANSMITTING_STATIONS, soapSerializationEnvelope );
				result = (SoapObject) soapSerializationEnvelope.getResponse();
				
				if( result != null )
				{
					stations = new ArrayList<Station>();
					
					for( int i = 0; i < result.getPropertyCount(); i++ )
					{
						
						SoapObject obj = (SoapObject) result.getProperty( i );
						Station station = new Station();
						station.abbrev = obj.getPropertyAsString( "abbrev" );
						station.dataProvider = obj.getPropertyAsString( "DataProvider" );
						station.dataProviderAbbrev = obj.getPropertyAsString( "DataProviderAbbrev" );
						station.div = Integer.parseInt( obj.getPropertyAsString( "div" ) );
						station.wd = Integer.parseInt( obj.getPropertyAsString( "wd" ) );
						station.name = WordUtils.capitalizeFully( obj.getPropertyAsString( "stationName" ) );
						String utmX = obj.getPropertyAsString( "UTM_x" );
						String utmY = obj.getPropertyAsString( "UTM_y" );
						
						try
						{
							station.utmX = Double.parseDouble( utmX );
							station.utmY = Double.parseDouble( utmY );

							UTMRef ref = new UTMRef( station.utmX, station.utmY, ColoradoWaterSMS.UTM_LAT_ZONE, ColoradoWaterSMS.UTM_LNG_ZONE );
							LatLng latLng = ref.toLatLng();
							
							station.latLng = new com.google.android.gms.maps.model.LatLng( latLng.getLat(), latLng.getLng() );
							stations.add( station );
						}catch( Exception e )
						{
							//can't find lat/lng, typically this means some test data, skip it.
						}
					}
				}
			}catch( IOException e )
			{
				mHasErrors = true;
			}catch( XmlPullParserException e )
			{
				mHasErrors = true;
			}
			
			return stations;
		}
		
		
		@Override
		public void deliverResult( List<Station> data )
		{
			mIsLoading = false;
			if( data != null )
			{
				mStations = data;
			}
			
			if( isStarted() )
				super.deliverResult( ( data == null ) ? null : new ArrayList<Station>( data ) );
		}
		
		
		@Override
		protected void onStartLoading()
		{
			if( mStations != null )
				deliverResult( mStations );
			else
				forceLoad();
		}
		
		
		@Override
		protected void onStopLoading()
		{
			mIsLoading = false;
			cancelLoad();
		}
		
		
		@Override
		protected void onReset()
		{
			super.onReset();
			onStopLoading();
			mStations = null;
		}
		
		
		public boolean isLoading()
		{
			return mIsLoading;
		}
		
		
		public boolean hasErrors()
		{
			return mHasErrors;
		}
		
		
		public void refresh()
		{
			reset();
			startLoading();
		}
	}
	
	
	private static final class StationsListAdapter extends BaseAdapter
	{
		private LayoutInflater mInflater;
		private List<Station> mStations;
		
		
		public StationsListAdapter( Context context )
		{
			mInflater = LayoutInflater.from( context );
		}
		
    	
		@Override
		public int getCount()
		{
			return ( mStations == null ) ? 0 : mStations.size();
		}
		
		
		@Override
		public Station getItem( int position )
		{
			return ( mStations == null ) ? null : mStations.get( position );
		}
		
		
		@Override
		public long getItemId( int position )
		{
			return position;
		}
		
		
		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			if( convertView == null )
				convertView = mInflater.inflate( R.layout.view_station_list_item, parent, false );
			
			TextView tv1 = (TextView) convertView.findViewById( android.R.id.text1 );
			TextView tv2 = (TextView) convertView.findViewById( android.R.id.text2 );
			Station station = getItem( position );
			tv1.setText( ( station == null ) ? "" : station.name );
			tv2.setText( ( station == null ) ? "" : station.dataProvider );
			return convertView;
		}
		
		
		public void update( List<Station> stations )
		{
			mStations = stations;
		}
	}
}
