package com.coloradowaters.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

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
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.coloradowaters.R;
import com.coloradowaters.model.ColoradoWaterSMS;
import com.coloradowaters.model.WaterDistrict;
import com.coloradowaters.model.WaterDivision;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

public class WaterDistrictsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<WaterDistrict>>
{
	private static final String TAG = WaterDistrictsListFragment.class.getSimpleName();
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";
	private static final int LOADER_ID = 0;
	
	
	private WaterDistrictsListAdapter mAdapter = null;
	private List<WaterDivision> mDivisions = null;
	private List<WaterDistrict> mDistricts = null;
	private int mListViewStatePosition;
    private int mListViewStateTop;
	
	
	@Override
	public void onActivityCreated( Bundle savedInstanceState )
	{
		super.onActivityCreated( savedInstanceState );
		mDivisions = WaterDivision.getWaterDivisions( getActivity() );
		
		mAdapter = new WaterDistrictsListAdapter( getActivity(), mDivisions, null );
		setListAdapter( mAdapter );
		
		getLoaderManager().initLoader( LOADER_ID, null, this );
		
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
		
		View view = inflater.inflate( R.layout.fragment_sticky_list_headers, null );
		return view;
	}
	
	
	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );
		
		( (StickyListHeadersListView) getListView() ).setDrawingListUnderStickyHeader( false );
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
	public Loader<List<WaterDistrict>> onCreateLoader( int id, Bundle args )
	{
		return new WaterDistrictsLoader( getActivity() );
	}
	
	
	@Override
	public void onLoadFinished( Loader<List<WaterDistrict>> loader, List<WaterDistrict> data )
	{
		if( data != null )
			mDistricts = data;
		
		mAdapter.update( mDivisions, mDistricts );
		mAdapter.notifyDataSetChanged();
		
		if( mListViewStatePosition != -1 && isAdded() )
		{
			getListView().setSelectionFromTop( mListViewStatePosition, mListViewStateTop );
			mListViewStatePosition = -1;
		}
	}
	
	
	@Override
	public void onLoaderReset( Loader<List<WaterDistrict>> loader )
	{
		// no-op
	}
	
	
	@Override
	public void onListItemClick( ListView l, View v, int position, long id )
	{
		WaterDistrict district = mAdapter.getItem( position );
		
		Intent waterDistrictIntent = new Intent( getActivity(), StationsListActivity.class );
		waterDistrictIntent.putExtra( ColoradoWaterSMS.EXTRA_DIVISION, district.divisionId );
		waterDistrictIntent.putExtra( ColoradoWaterSMS.EXTRA_WATER_DISTRICT, district.id );
		waterDistrictIntent.putExtra( Intent.EXTRA_TITLE, district.name );
		startActivity( waterDistrictIntent );
	}
	
	
	private static class WaterDistrictsLoader extends AsyncTaskLoader< List<WaterDistrict> >
	{
		private List<WaterDistrict> mWaterDistricts;
		private boolean mIsLoading;
		private boolean mHasErrors;
		
		
		public WaterDistrictsLoader( Context context )
		{
			super( context );
			init();
		}
		
		
		private void init()
		{
			mWaterDistricts = null;
			mIsLoading = true;
			mHasErrors = false;
		}
		
		
		@Override
		public List<WaterDistrict> loadInBackground()
		{
			List<WaterDistrict> waterDistricts = null;
			
			SoapObject soapObject = new SoapObject( ColoradoWaterSMS.NAMESPACE, ColoradoWaterSMS.GET_WATER_DISTRICTS );
			SoapSerializationEnvelope soapSerializationEnvelope = new SoapSerializationEnvelope( SoapEnvelope.VER11 );
			soapSerializationEnvelope.setOutputSoapObject( soapObject );
			HttpTransportSE httpTransportSE = new HttpTransportSE( ColoradoWaterSMS.SERVICE_URL );
			SoapObject result = null;
			try
			{
				httpTransportSE.call( ColoradoWaterSMS.NAMESPACE + ColoradoWaterSMS.GET_WATER_DISTRICTS, soapSerializationEnvelope );
				result = (SoapObject) soapSerializationEnvelope.getResponse();
				
				if( result != null )
				{
					waterDistricts = new ArrayList<WaterDistrict>();
					for( int i = 0; i < result.getPropertyCount(); i++ )
					{
						WaterDistrict district = new WaterDistrict();
						SoapObject obj = (SoapObject) result.getProperty( i );
						district.divisionId = Integer.parseInt( obj.getPropertyAsString( "div" ) );
						district.id = Integer.parseInt( obj.getPropertyAsString( "wd" ) );
						district.name = obj.getPropertyAsString( "waterDistrictName" );
						
						waterDistricts.add( district );
					}
				}
			}catch( IOException e )
			{
				mHasErrors = true;
			}catch( XmlPullParserException e )
			{
				mHasErrors = true;
			}
			
			Collections.sort( waterDistricts, WATER_DISTRICTS_COMPARATOR );
			
			return waterDistricts;
		}
		
		
		@Override
		public void deliverResult( List<WaterDistrict> data )
		{
			mIsLoading = false;
			if( data != null )
			{
				mWaterDistricts = data;
			}
			
			if( isStarted() )
				super.deliverResult( ( data == null ) ? null : new ArrayList<WaterDistrict>( data ) );
		}
		
		
		@Override
		protected void onStartLoading()
		{
			if( mWaterDistricts != null )
				deliverResult( mWaterDistricts );
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
			mWaterDistricts = null;
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
	
	
	
	private static final class WaterDistrictsListAdapter extends BaseAdapter implements StickyListHeadersAdapter, SectionIndexer
	{
		private List<WaterDivision> mDivisions;
		private List<WaterDistrict> mWaterDistricts;
		private LayoutInflater mInflater;
		
		
		public WaterDistrictsListAdapter( Context context, List<WaterDivision> divisions, List<WaterDistrict> districts )
		{
			mDivisions = divisions;
			mWaterDistricts = districts;
			mInflater = LayoutInflater.from( context );
		}
		
		
		public void update( List<WaterDivision> divisions, List<WaterDistrict> districts )
		{
			mDivisions = divisions;
			mWaterDistricts = districts;
		}
		
		
		@Override
		public int getCount()
		{
			return ( mWaterDistricts == null ) ? 0 : mWaterDistricts.size();
		}
		
		
		@Override
		public WaterDistrict getItem( int position )
		{
			return ( mWaterDistricts == null ) ? null : mWaterDistricts.get( position );
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
			{
				convertView = mInflater.inflate( R.layout.view_list_item, parent, false );
			}
			
			TextView tv = (TextView) convertView.findViewById( android.R.id.text1 );
			tv.setText( getItem( position ).name );
			
			return convertView;
		}
		
		
		@Override
		public int getPositionForSection( int section )
		{
			if( section >= mDivisions.size() )
				section = mDivisions.size() - 1;
			else if( section < 0 )
				section = 0;
			
			int position = 0;
			int divId = mDivisions.get( section ).id;
			
			for( int i = 0; i < mWaterDistricts.size(); i++ )
			{
				if( divId == mWaterDistricts.get( i ).id )
				{
					position = i;
					break;
				}
			}
			
			return position;
		}
		
		
		@Override
		public int getSectionForPosition( int position )
		{
			if( position >= mWaterDistricts.size() )
				position = mWaterDistricts.size() - 1;
			else if( position < 0 )
				position = 0;
			
			int divId = mWaterDistricts.get( position ).divisionId;
			for( int i = 0; i < mDivisions.size(); i++ )
			{
				
				 if( mDivisions.get( i ).id == divId )
					 return i;
			}
			
			return 0;
		}
		
		
		@Override
		public WaterDivision[] getSections()
		{
			WaterDivision[] div = new WaterDivision[ mDivisions.size() ];
			return mDivisions.toArray( div );
		}
		
		
		@Override
		public View getHeaderView( int position, View convertView, ViewGroup parent )
		{
			if( convertView == null )
				convertView = mInflater.inflate( R.layout.view_list_section, parent, false );
			
			int divisionId = (int) getHeaderId( position );
			WaterDivision div = null;
			
			for( WaterDivision d : mDivisions )
			{
				if( divisionId == d.id )
				{
					div = d;
					break;
				}
			}
			
			if( div != null )
			{
				TextView tv = (TextView) convertView.findViewById( R.id.header_text );
				tv.setText( div.name );
			}
			return convertView;
		}
		
		
		@Override
		public long getHeaderId( int position )
		{
			return getItem( position ).divisionId;
		}
	}
	
	
	private static final Comparator<WaterDistrict> WATER_DISTRICTS_COMPARATOR = new Comparator<WaterDistrict>()
	{
		@Override
		public int compare( WaterDistrict lhs, WaterDistrict rhs )
		{
			return lhs.divisionId - rhs.divisionId;
		}
	};
}
