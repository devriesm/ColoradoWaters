package com.coloradowaters.ui;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coloradowaters.R;
import com.coloradowaters.model.ColoradoWaterSMS;
import com.coloradowaters.model.CurrentCondition;
import com.coloradowaters.model.Station;
import com.coloradowaters.util.VariablesUtil;

public class StationConditionsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<CurrentCondition>>
{
	private ConditionsAdapter mAdapter;
	
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		mAdapter = new ConditionsAdapter( getActivity() );
		setListAdapter( mAdapter );
	}
	
	
	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );
	}
	
	
	@Override
	public void onActivityCreated( Bundle savedInstanceState )
	{
		super.onActivityCreated( savedInstanceState );
		
		setListShown( false );
		getListView().setClipToPadding( false );
		
		getLoaderManager().initLoader( ColoradoWaterSMS.CURRENT_CONDITIONS_LOADER_ID, getArguments(), this );
	}
	
	
	@Override
	public Loader<List<CurrentCondition>> onCreateLoader( int id, Bundle args )
	{
		Station station = args.getParcelable( ColoradoWaterSMS.EXTRA_STATION );
		return new CurrentConditionsLoader( getActivity(), station );
	}
	
	
	@Override
	public void onLoadFinished( Loader<List<CurrentCondition>> loader, List<CurrentCondition> data )
	{
		mAdapter.setData( data );
		mAdapter.notifyDataSetChanged();
		setListShown( true );
		
	}
	
	
	@Override
	public void onLoaderReset( Loader<List<CurrentCondition>> loader )
	{
		// no-op
	}
	
	
	private static final class CurrentConditionsLoader extends AsyncTaskLoader<List<CurrentCondition>>
	{
		private Station mStation;
		private boolean mHasErrors = false;
		private boolean mIsLoading = false;
		private List<CurrentCondition> mData = null;
		private SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm" );
		
		
		public CurrentConditionsLoader( Context context, Station station )
		{
			super( context );
			mStation = station;
			mIsLoading = true;
		}
		
		
		@Override
		public List<CurrentCondition> loadInBackground()
		{
			List<CurrentCondition> conditions = null;
			
			SoapObject request = new SoapObject( ColoradoWaterSMS.NAMESPACE, ColoradoWaterSMS.GET_CURRENT_CONDITIONS );
			
			PropertyInfo divProp = new PropertyInfo();
			divProp.setName( "Div" );
			divProp.setValue( mStation.div );
			divProp.setType( Integer.class );
			divProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( divProp );
			
			PropertyInfo wdProp = new PropertyInfo();
			wdProp.setName( "WD" );
			wdProp.setValue( mStation.wd );
			wdProp.setType( Integer.class );
			wdProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( wdProp );
			
			PropertyInfo abbrevProp = new PropertyInfo();
			abbrevProp.setName( "Abbrev" );
			abbrevProp.setValue( mStation.abbrev );
			abbrevProp.setType( String.class );
			abbrevProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( abbrevProp );

			SoapSerializationEnvelope soapSerializationEnvelope = new SoapSerializationEnvelope( SoapEnvelope.VER12 );
			soapSerializationEnvelope.setOutputSoapObject( request );
			HttpTransportSE httpTransportSE = new HttpTransportSE( ColoradoWaterSMS.SERVICE_URL );
			SoapObject result = null;
			
			try
			{
				httpTransportSE.call( ColoradoWaterSMS.NAMESPACE + ColoradoWaterSMS.GET_CURRENT_CONDITIONS, soapSerializationEnvelope );
				result = (SoapObject) soapSerializationEnvelope.getResponse();
				
				if( result != null )
				{
					
					conditions = new ArrayList<CurrentCondition>();
					for( int i = 0; i < result.getPropertyCount(); i++ )
					{
						SoapObject obj = (SoapObject) result.getProperty( i );
						CurrentCondition cond = new CurrentCondition();
						cond.abbrev = obj.getPropertyAsString( "abbrev" );
						cond.amount = obj.getPropertyAsString( "amount" );
						
						if( obj.hasProperty( "currentShift" ) )
							cond.currentShift = obj.getPropertyAsString( "currentShift" );
							
						cond.dataSource = obj.getPropertyAsString( "dataSource" );
						cond.div = Integer.parseInt( obj.getPropertyAsString( "div" ) );
						cond.gageHeight = obj.getPropertyAsString( "gageHeight" );
						
						try
						{
							cond.transDate = df.parse( obj.getPropertyAsString( "transDateTime" ) );
						}catch( ParseException e ){}
						
						cond.variable = obj.getPropertyAsString( "variable" );
						cond.wd = Integer.parseInt( obj.getPropertyAsString( "wd" ) );
						
						conditions.add( cond );
						
					}
				}
			}catch( IOException e )
			{
				mHasErrors = true;
			}catch( XmlPullParserException e )
			{
				mHasErrors = true;
			}
			
			return conditions;
		}
		
		
		@Override
		public void deliverResult( List<CurrentCondition> data )
		{
			mIsLoading = false;
			if( data != null )
				mData = data;
			
			if( isStarted() )
				super.deliverResult( data );
		}
		
		
		@Override
		protected void onStartLoading()
		{
			mIsLoading = true;
			if( mData != null )
				deliverResult( mData );
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
			mData = null;
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
	
	
	public static class ConditionsAdapter extends BaseAdapter
	{
		private WeakReference<Context> mContext;
		private List<CurrentCondition> mConditions;
		
		public ConditionsAdapter( Context context )
		{
			mContext = new WeakReference<Context>( context );
		}
		
		
		@Override
		public int getCount()
		{
			return ( mConditions == null ) ? 0 : mConditions.size();
		}
		
		
		@Override
		public CurrentCondition getItem( int position )
		{
			if( mConditions == null || mConditions.isEmpty() || position >= mConditions.size() || position < 0 )
				return null;
			
			return mConditions.get( position );
		}
		
		
		@Override
		public long getItemId( int position )
		{
			return position;
		}
		
		
		@Override
		public boolean areAllItemsEnabled()
		{
			return false;
		}
		
		
		@Override
		public boolean isEnabled( int position )
		{
			return false;
		}
		
		
		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			if( convertView == null )
			{
				LayoutInflater inflater = LayoutInflater.from( mContext.get() );
				convertView = inflater.inflate( R.layout.view_current_condition, null );
			}
			CurrentCondition cond = getItem( position );
			
			if( cond != null )
			{
			
				TextView label = (TextView) convertView.findViewById( R.id.condition_label );
				TextView value = (TextView) convertView.findViewById( R.id.condition_value );
				
				label.setText( VariablesUtil.getLabelForVariable( mContext.get(), cond.variable ) );
				
				//TODO - ugh
				value.setText( cond.amount + " " + VariablesUtil.getMeasurementForVariable( mContext.get(), cond.variable ) );
			}
			
			return convertView;
		}
		
		
		public void setData( List<CurrentCondition> data )
		{
			mConditions = data;
		}
		
	}
}
