package com.coloradowaters.ui;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.math3.util.Precision;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coloradowaters.R;
import com.coloradowaters.model.ColoradoWaterSMS;
import com.coloradowaters.model.CurrentCondition;
import com.coloradowaters.model.Station;
import com.coloradowaters.model.StationVariable;
import com.coloradowaters.model.StreamFlowTransmission;
import com.coloradowaters.ui.StationDetailsActivity.OnLoaderComplete;
import com.coloradowaters.ui.StationDetailsFragment.ValueLabelAdapter.LabelOrientation;
import com.coloradowaters.util.VariablesUtil;
import com.michaelpardo.android.widget.chartview.ChartView;
import com.michaelpardo.android.widget.chartview.LabelAdapter;
import com.michaelpardo.android.widget.chartview.LinearSeries;
import com.michaelpardo.android.widget.chartview.LinearSeries.LinearPoint;

public class StationDetailsFragment extends Fragment implements OnLoaderComplete
{
	private static final String TAG = StationDetailsFragment.class.getSimpleName();
	private static final int STATION_VAR_LOADER_ID = 1;
	private static final int STREAM_FLOW_LOADER_ID = 2;
	private static final int CURRENT_CONDITIONS_LOADER_ID = 3;
	
	private TextView mTitleText;
	private TextView mProviderText;
	private ChartView mChartView;
	private ProgressBar mChartProgress;
	private TextView mChartErrorView;
	private TextView mChartVariable;
	private ListView mListView;
	
	
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		View view = inflater.inflate( R.layout.fragment_station_details, null );
		return view;
	}
	
	
	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );
		
		mTitleText = (TextView) view.findViewById( R.id.title_view );
		mProviderText = (TextView) view.findViewById( R.id.provider_view );
		mChartView = (ChartView) view.findViewById( R.id.chart_view );
		mChartProgress = (ProgressBar) view.findViewById( R.id.chart_progress_view );
		mChartErrorView = (TextView) view.findViewById( R.id.chart_error_view );
		mChartVariable = (TextView) view.findViewById( R.id.chart_variable );
		mListView = (ListView) view.findViewById( R.id.conditions_list );
		
		mChartErrorView.setVisibility( View.GONE );
		
		Station station = getArguments().getParcelable( ColoradoWaterSMS.EXTRA_STATION );
		
		mTitleText.setText( station.name );
		mProviderText.setText( station.dataProvider );
		
	}
	
	
	@Override
	public void onActivityCreated( Bundle savedInstanceState )
	{
		super.onActivityCreated( savedInstanceState );
		
		mListView.setAdapter( new ConditionsAdapter( getActivity() ) );
		
		getLoaderManager().initLoader( STATION_VAR_LOADER_ID, getArguments(), new StationVariableLoaderListener( getActivity(), this ) );
		getLoaderManager().initLoader( STREAM_FLOW_LOADER_ID, getArguments(), new StreamFlowLoaderListener( getActivity(), this ) );
		getLoaderManager().initLoader( CURRENT_CONDITIONS_LOADER_ID, getArguments(), new CurrentConditionsLoaderListener( getActivity(), this ) );
	}
	
	
	@Override
	public void onLoaderComplete( int loaderId, Object data )
	{
		if( loaderId == STATION_VAR_LOADER_ID )
		{
			if( data != null )
			{
				List<StationVariable> variables = (List<StationVariable>) data;
				onStationVariablesLoaded( variables );
			}
		}else if( loaderId == STREAM_FLOW_LOADER_ID )
		{
			if( data != null )
			{
				List<StreamFlowTransmission> transmissions = (List<StreamFlowTransmission>) data;
				onStreamFlowTransmissionLoaded( transmissions );
			}
		}else if( loaderId == CURRENT_CONDITIONS_LOADER_ID )
		{
			//init current conditions adapter
			if( data != null )
			{
				List<CurrentCondition> conditions = (List<CurrentCondition>) data;
				ConditionsAdapter adapter = (ConditionsAdapter) mListView.getAdapter();
				adapter.setData( conditions );
				adapter.notifyDataSetChanged();
				
			}
		}
	}
	
	
	private void onStationVariablesLoaded( List<StationVariable> variables )
	{
		if( variables != null && !variables.isEmpty() )
		{
			StationVariable var = null;
			for( StationVariable v : variables )
			{
				if( v.variable.equals( "DISCHRG" ) )
				{
					var = v;
					break;
				}
			}
			
			if( var == null )
			{
				for( StationVariable v : variables )
				{
					if( v.variable.equals( "STORAGE" ) )
					{
						var = v;
						break;
					}
				}
			}
			
			if( var == null )
				var = variables.get( 0 );
			
			mChartVariable.setText( VariablesUtil.getLabelForVariable( getActivity(), var.variable ) + " (" + VariablesUtil.getMeasurementForVariable( getActivity(), var.variable ) + ")" );
			Log.e( TAG, "Var : " + var.variable );
			
			Loader loader = getLoaderManager().getLoader( STREAM_FLOW_LOADER_ID );
			if( loader != null && loader instanceof StreamFlowLoader )
			{
				Calendar endCal = Calendar.getInstance( TimeZone.getTimeZone( "America/Denver" ) );
				endCal.set( Calendar.SECOND, 0 );
				endCal.set( Calendar.HOUR, 0 );
				
				Calendar startCal = (Calendar) endCal.clone();
				startCal.set( Calendar.HOUR, -168 );
				
				( (StreamFlowLoader) loader ).setVariable( var );
				( (StreamFlowLoader) loader ).setStartDate( startCal.getTime() );
				( (StreamFlowLoader) loader ).setEndDate( endCal.getTime() );
				loader.forceLoad();
			}
			
		}else
		{
			mChartView.setVisibility( View.GONE );
		}
	}
	
	
	private void onStreamFlowTransmissionLoaded( List<StreamFlowTransmission> data )
	{
		LinearSeries series = new LinearSeries();
		series.setLineColor( getResources().getColor( R.color.main_color_cw ) );
		series.setLineWidth( 5 );
		
		for (int i = 0; i < data.size(); i++) {
			series.addPoint(new LinearPoint( i, data.get( i ).amount ) );
		}
		double maxY = series.getMaxY();
		Log.e( TAG, "Max Y : " + maxY );
		
		int rounded = (int) Precision.round( maxY, -2 );
		Log.e( TAG, "ROUNDED : " + rounded );
		if( rounded < maxY )
			rounded = rounded + 100;
		else if( rounded == 0 )
			rounded = 100;
		
		series.extendRange( 0, 0 );
		series.extendRange( 0, rounded );
		
		if( data.size() > 0 )
		{
			mChartView.addSeries( series );
			mChartView.setLeftLabelAdapter( new ValueLabelAdapter( getActivity(), LabelOrientation.VERTICAL, data ) );
			mChartView.setBottomLabelAdapter( new ValueLabelAdapter( getActivity(), LabelOrientation.HORIZONTAL, data ) );
			mChartView.setVisibility( View.VISIBLE );
			mChartErrorView.setVisibility( View.GONE );
		}else
		{
			mChartErrorView.setVisibility( View.VISIBLE );
			//mChartView.setVisibility( View.INVISIBLE );
		}
		
		mChartProgress.setVisibility( View.GONE );
	}
	
	
	private static final class StationVariableLoader extends AsyncTaskLoader<List<StationVariable>>
	{
		private Station mStation;
		private List<StationVariable> mVariables = null;
		private boolean mHasErrors = false;
		private boolean mIsLoading = false;

		public StationVariableLoader( Context context, Station station )
		{
			super( context );
			mStation = station;
		}
		
		
		@Override
		public List<StationVariable> loadInBackground()
		{
			List<StationVariable> variables = null;
			
			SoapObject request = new SoapObject( ColoradoWaterSMS.NAMESPACE, ColoradoWaterSMS.GET_TRANSMITTING_STATION_VARIABLES );
			
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
				httpTransportSE.call( ColoradoWaterSMS.NAMESPACE + ColoradoWaterSMS.GET_TRANSMITTING_STATION_VARIABLES, soapSerializationEnvelope );
				result = (SoapObject) soapSerializationEnvelope.getResponse();
				
				if( result != null )
				{
					variables = new ArrayList<StationVariable>();
					for( int i = 0; i < result.getPropertyCount(); i++ )
					{
						SoapObject obj = (SoapObject) result.getProperty( i );
						
						StationVariable var = new StationVariable();
						var.variable = obj.getPropertyAsString( "variable" );
						
						variables.add( var );
					}
				}
			}catch( IOException e )
			{
				mHasErrors = true;
			}catch( XmlPullParserException e )
			{
				mHasErrors = true;
			}
			
			
			return variables;
		}
		
		
		@Override
		public void deliverResult( List<StationVariable> data )
		{
			mIsLoading = false;
			if( data != null )
				mVariables = data;
			
			if( isStarted() )
				super.deliverResult( data );
		}
		
		
		@Override
		protected void onStartLoading()
		{
			mIsLoading = true;
			if( mVariables != null )
				deliverResult( mVariables );
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
			mVariables = null;
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
	
	
	private static class StationVariableLoaderListener implements LoaderManager.LoaderCallbacks<List<StationVariable>>
	{
		private WeakReference<Context> mContext;
		private WeakReference<OnLoaderComplete> mOnLoaderComplete;
		
		public StationVariableLoaderListener( Context context, OnLoaderComplete listener )
		{
			this.mContext = new WeakReference<Context>( context );
			mOnLoaderComplete = new WeakReference<OnLoaderComplete>( listener );
		}
		
		
		@Override
		public Loader<List<StationVariable>> onCreateLoader( int id, Bundle args)
		{
			Station station = args.getParcelable( ColoradoWaterSMS.EXTRA_STATION );
			return new StationVariableLoader( mContext.get(), station );
		}
		
		
		@Override
		public void onLoadFinished( Loader<List<StationVariable>> loader, List<StationVariable> data )
		{
			mOnLoaderComplete.get().onLoaderComplete( loader.getId(), data );
		}
		
		
		@Override
		public void onLoaderReset( Loader<List<StationVariable>> loader )
		{
			// no-op
		}
	};
	
	
	private static final class StreamFlowLoader extends AsyncTaskLoader<List<StreamFlowTransmission>>
	{
		private Station mStation;
		private StationVariable mVariable;
		private Date mStartDate;
		private Date mEndDate;
		private List<StreamFlowTransmission> mStreamFlowTransmissions = null;
		private boolean mHasErrors = false;
		private boolean mIsLoading = false;
		private SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd" );
		

		public StreamFlowLoader( Context context, Station station )
		{
			super( context );
			mStation = station;
		}
		
		
		public void setVariable( StationVariable variable )
		{
			mVariable = variable;
		}
		
		public void setStartDate( Date startDate )
		{
			mStartDate = startDate;
		}
		
		
		public void setEndDate( Date endDate )
		{
			mEndDate = endDate;
		}
		
		
		@Override
		public List<StreamFlowTransmission> loadInBackground()
		{
			mIsLoading = true;
			
			List<StreamFlowTransmission> data = new ArrayList<StreamFlowTransmission>();
			
			SoapObject request = new SoapObject( ColoradoWaterSMS.NAMESPACE, ColoradoWaterSMS.GET_PROVISIONAL_DATA );
			
			PropertyInfo abbrevProp = new PropertyInfo();
			abbrevProp.setName( "Abbrev" );
			abbrevProp.setValue( mStation.abbrev );
			abbrevProp.setType( String.class );
			abbrevProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( abbrevProp );
			
			PropertyInfo varProp = new PropertyInfo();
			varProp.setName( "Variable" );
			varProp.setValue( mVariable.variable );
			varProp.setType( String.class );
			varProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( varProp );
			
			PropertyInfo startProp = new PropertyInfo();
			startProp.setName( "StartDate" );
			startProp.setValue( df.format( mStartDate ) );
			startProp.setType( String.class );
			startProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( startProp );
			
			PropertyInfo endProp = new PropertyInfo();
			endProp.setName( "EndDate" );
			endProp.setValue( df.format( mEndDate ) );
			endProp.setType( String.class );
			endProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( endProp );
			
			PropertyInfo aggrProp = new PropertyInfo();
			aggrProp.setName( "Aggregation" );
			aggrProp.setValue( "H" );
			aggrProp.setType( String.class );
			aggrProp.setNamespace( ColoradoWaterSMS.NAMESPACE );
			request.addProperty( aggrProp );

			SoapSerializationEnvelope soapSerializationEnvelope = new SoapSerializationEnvelope( SoapEnvelope.VER12 );
			soapSerializationEnvelope.setOutputSoapObject( request );
			HttpTransportSE httpTransportSE = new HttpTransportSE( ColoradoWaterSMS.SERVICE_URL );
			SoapObject result = null;
			
			try
			{
				httpTransportSE.call( ColoradoWaterSMS.NAMESPACE + ColoradoWaterSMS.GET_PROVISIONAL_DATA, soapSerializationEnvelope );
				result = (SoapObject) soapSerializationEnvelope.getResponse();
				
				if( result != null )
				{
					SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm" );
					data = new ArrayList<StreamFlowTransmission>();
					
					for( int i = 0; i < result.getPropertyCount(); i++ )
					{
						SoapObject obj = (SoapObject) result.getProperty( i );
						StreamFlowTransmission trans = new StreamFlowTransmission();
						
						if( obj.hasProperty( "amount" ) )
							trans.amount = Double.parseDouble( obj.getPropertyAsString( "amount" ) );
						
						if( obj.hasProperty( "transDateTime" ) )
						{
							try
							{
								trans.date = df.parse( obj.getPropertyAsString( "transDateTime" ) );
							}catch( ParseException e ){}
						}
						
						if( obj.hasProperty( "transFlag" ) )
							trans.transmissionFlag = obj.getPropertyAsString( "transFlag" );
						
						if( obj.hasProperty( "resultCount" ) )
							trans.resultCount = Integer.parseInt( obj.getPropertyAsString( "resultCount" ) );
						
						data.add( trans );
					}
				}
			}catch( IOException e )
			{
				mHasErrors = true;
			}catch( XmlPullParserException e )
			{
				mHasErrors = true;
			}
			
			
			return data;
		}
		
		
		@Override
		public void deliverResult( List<StreamFlowTransmission> data )
		{
			mIsLoading = false;
			if( data != null )
				mStreamFlowTransmissions = data;
			
			if( isStarted() )
				super.deliverResult( data );
		}
		
		
		@Override
		protected void onStartLoading()
		{
			super.onStartLoading();
		}
	}
	
	
	private static class StreamFlowLoaderListener implements LoaderManager.LoaderCallbacks<List<StreamFlowTransmission>>
	{
		WeakReference<Context> mContext;
		WeakReference<OnLoaderComplete> mOnLoaderComplete;
		
		public StreamFlowLoaderListener( Context context, OnLoaderComplete listener )
		{
			mContext = new WeakReference<Context>( context );
			mOnLoaderComplete = new WeakReference<OnLoaderComplete>( listener );
		}
		
		
		@Override
		public Loader<List<StreamFlowTransmission>> onCreateLoader( int id, Bundle args )
		{
			Station station = args.getParcelable( ColoradoWaterSMS.EXTRA_STATION );
			return new StreamFlowLoader( mContext.get(), station );
		}
		
		
		@Override
		public void onLoadFinished( Loader<List<StreamFlowTransmission>> loader, List<StreamFlowTransmission> data )
		{
			mOnLoaderComplete.get().onLoaderComplete( loader.getId(), data );
		}
		
		
		@Override
		public void onLoaderReset( Loader<List<StreamFlowTransmission>> loader )
		{
			// no-op
		}
	};
	
	
	private static final class CurrentConditionsLoader extends AsyncTaskLoader<List<CurrentCondition>>
	{
		private Station mStation;
		private boolean mHasErrors = false;
		private boolean mIsLoading = false;
		private List<CurrentCondition> mData = null;
		
		
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
					SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm" );
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
	
	
	private static class CurrentConditionsLoaderListener implements LoaderManager.LoaderCallbacks<List<CurrentCondition>>
	{
		WeakReference<Context> mContext;
		WeakReference<OnLoaderComplete> mOnLoaderComplete;
		
		
		public CurrentConditionsLoaderListener( Context context, OnLoaderComplete listener )
		{
			mContext = new WeakReference<Context>( context );
			mOnLoaderComplete = new WeakReference<StationDetailsActivity.OnLoaderComplete>( listener );
		}
		
		
		@Override
		public Loader<List<CurrentCondition>> onCreateLoader( int id, Bundle args )
		{
			Station station = args.getParcelable( ColoradoWaterSMS.EXTRA_STATION );
			return new CurrentConditionsLoader( mContext.get(), station );
		}
		
		
		@Override
		public void onLoadFinished( Loader<List<CurrentCondition>> loader, List<CurrentCondition> data )
		{
			mOnLoaderComplete.get().onLoaderComplete( loader.getId(), data );
		}
		
		
		@Override
		public void onLoaderReset( Loader<List<CurrentCondition>> loader )
		{
			// no-op
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
	
	
	public static class ValueLabelAdapter extends LabelAdapter
	{
		public enum LabelOrientation
		{
			HORIZONTAL, VERTICAL
		}
		
		private WeakReference<Context> mContext;
		private LabelOrientation mOrientation;
		private List<StreamFlowTransmission> mData;
		private SimpleDateFormat df = new SimpleDateFormat( "M/d" );
		
		
		public ValueLabelAdapter( Context context, LabelOrientation orientation, List<StreamFlowTransmission> data )
		{
			mOrientation = orientation;
			mContext = new WeakReference<Context>( context );
			mData = data;
		}
		

		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			if( convertView == null )
				convertView = View.inflate( mContext.get(), R.layout.view_chart_label, null );
			
			TextView tv = (TextView) convertView.findViewById( android.R.id.text1 );
			
			Double d = getItem( position );
			
			if( mOrientation.equals( LabelOrientation.HORIZONTAL ) )
			{
				int i = d.intValue();
				if( i >= mData.size() )
				{
					tv.setText( "" );
				}else
				{
					if( position == 0 )
						tv.setGravity( Gravity.LEFT | Gravity.CENTER_VERTICAL );
					else if( position == getCount() - 1 )
						tv.setGravity( Gravity.RIGHT | Gravity.CENTER_VERTICAL );
					
					StreamFlowTransmission trans = mData.get( i );
					Log.e( TAG, "I : " + i );
					tv.setText( df.format( trans.date ) );
				}
			}else
			{
				if( position == 0 )
					tv.setGravity( Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL );
				else if( position == getCount() - 1 )
					tv.setGravity( Gravity.TOP | Gravity.CENTER_HORIZONTAL );
				
				tv.setText( String.format( "%.1f", d ) );
			}
			
			return convertView;
		}
	}
}
