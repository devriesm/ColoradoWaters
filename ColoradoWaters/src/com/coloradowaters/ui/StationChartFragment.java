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
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.coloradowaters.R;
import com.coloradowaters.model.ColoradoWaterSMS;
import com.coloradowaters.model.Station;
import com.coloradowaters.model.StationVariable;
import com.coloradowaters.model.StreamFlowTransmission;
import com.coloradowaters.ui.StationChartFragment.ValueLabelAdapter.LabelOrientation;
import com.coloradowaters.ui.StationDetailsActivity.OnLoaderComplete;
import com.coloradowaters.util.VariablesUtil;
import com.michaelpardo.android.widget.chartview.ChartView;
import com.michaelpardo.android.widget.chartview.LabelAdapter;
import com.michaelpardo.android.widget.chartview.LinearSeries;
import com.michaelpardo.android.widget.chartview.LinearSeries.LinearPoint;

public class StationChartFragment extends Fragment implements OnLoaderComplete
{
	private static final int STATION_VAR_LOADER_ID = 1;
	private static final int STREAM_FLOW_LOADER_ID = 2;
	
	private static final String EXTRA_KEY_SELECTED_FILTER = "selected_filter";
	
	private static final int ONE_DAY = 1;
	private static final int ONE_WEEK = 2;
	private static final int ONE_MONTH = 3;
	
	
	private ChartView mChartView;
	private ProgressBar mProgressBar;
	private TextView mErrorView;
	private TextView mChartVariable;
	
	private int mSelectedFilter = ONE_DAY;
	
	private List<StationVariable> mStationVariables;
	
	
	private final RadioGroup.OnCheckedChangeListener ToggleListener = new RadioGroup.OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged( final RadioGroup radioGroup, final int id )
		{
			if( id == R.id.button_day )
			{
				mSelectedFilter = ONE_DAY;
			}else if( id == R.id.button_week )
			{
				mSelectedFilter = ONE_WEEK;
			}else if( id == R.id.button_month )
			{
				mSelectedFilter = ONE_MONTH;
			}
			
			showLoading();
			onStationVariablesLoaded( mStationVariables );
		}
    };
	
	
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		View view = inflater.inflate( R.layout.fragment_station_chart, container, false );
		
		mChartView = (ChartView) view.findViewById( R.id.chart_view );
		mProgressBar = (ProgressBar) view.findViewById( R.id.chart_progress_view );
		mErrorView = (TextView) view.findViewById( R.id.chart_error_view );
		mChartVariable = (TextView) view.findViewById( R.id.chart_variable );
		
		
		if( savedInstanceState != null )
		{
			mSelectedFilter = savedInstanceState.getInt( EXTRA_KEY_SELECTED_FILTER );
		}
		
		
		( (RadioGroup) view.findViewById( R.id.chart_filter_container ) ).setOnCheckedChangeListener( ToggleListener );
		
		return view;
	}
	
	
	@Override
	public void onActivityCreated( Bundle savedInstanceState )
	{
		super.onActivityCreated( savedInstanceState );
		
		getLoaderManager().initLoader( STATION_VAR_LOADER_ID, getArguments(), new StationVariableLoaderListener( getActivity(), this ) );
		getLoaderManager().initLoader( STREAM_FLOW_LOADER_ID, getArguments(), new StreamFlowLoaderListener( getActivity(), this ) );
		
		showLoading();
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
			}else
			{
				showNoData();
			}
		}else if( loaderId == STREAM_FLOW_LOADER_ID )
		{
			if( data != null )
			{
				List<StreamFlowTransmission> transmissions = (List<StreamFlowTransmission>) data;
				onStreamFlowTransmissionLoaded( transmissions );
			}else
			{
				showNoData();
			}
		}
	}
	
	
	private void onStationVariablesLoaded( List<StationVariable> variables )
	{
		if( variables != null && !variables.isEmpty() )
		{
			mStationVariables = variables;
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
			
			Loader loader = getLoaderManager().getLoader( STREAM_FLOW_LOADER_ID );
			if( loader != null && loader instanceof StreamFlowLoader )
			{
				Calendar endCal = Calendar.getInstance( TimeZone.getTimeZone( "America/Denver" ) );
				endCal.set( Calendar.SECOND, 0 );
				//endCal.set( Calendar.HOUR, 0 );
				
				Calendar startCal = (Calendar) endCal.clone();
				
				if( mSelectedFilter == ONE_DAY )
					startCal.add( Calendar.DATE, -1 );
				else if( mSelectedFilter == ONE_WEEK )
					startCal.add( Calendar.DATE, -7 );
				else if( mSelectedFilter == ONE_MONTH )
					startCal.add( Calendar.MONTH, -1 );
				
				Log.e( "StationChartFragment", "SELECTED FILTER : " + mSelectedFilter );
				Log.e( "StationChartFragment", "Diff : " + ( endCal.getTimeInMillis() - startCal.getTimeInMillis() ) );
				
				( (StreamFlowLoader) loader ).setVariable( var );
				( (StreamFlowLoader) loader ).setStartDate( startCal.getTime() );
				( (StreamFlowLoader) loader ).setEndDate( endCal.getTime() );
				loader.forceLoad();
			}
			
		}else
		{
			showNoData();
		}
	}
	
	
	private void onStreamFlowTransmissionLoaded( List<StreamFlowTransmission> data )
	{
		
		mChartView.clearSeries();
		
		LinearSeries series = new LinearSeries();
		series.setLineColor( getResources().getColor( R.color.main_color_cw ) );
		series.setLineWidth( 5 );
		
		for (int i = 0; i < data.size(); i++) {
			series.addPoint(new LinearPoint( i, data.get( i ).amount ) );
		}
		double maxY = series.getMaxY();
		
		int rounded = (int) Precision.round( maxY, -2 );
		
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
			
			showChart();
		}else
		{
			showNoData();
		}
	}
	
	
	private void showNoData()
	{
		mErrorView.setVisibility( View.VISIBLE );
		mProgressBar.setVisibility( View.GONE );
		mChartView.setVisibility( View.GONE );
		mChartVariable.setVisibility( View.GONE );
	}
	
	
	private void showLoading()
	{
		mErrorView.setVisibility( View.GONE );
		mChartView.setVisibility( View.GONE );
		mChartVariable.setVisibility( View.GONE );
		mProgressBar.setVisibility( View.VISIBLE );
	}
	
	
	private void showChart()
	{
		mErrorView.setVisibility( View.GONE );
		mChartView.setVisibility( View.VISIBLE );
		mChartVariable.setVisibility( View.VISIBLE );
		mProgressBar.setVisibility( View.GONE );
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
		private SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );
		

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
			
			String agg = "H";
			if( mEndDate.getTime() - mStartDate.getTime() > 604800000 )
				agg = "D";
			
			PropertyInfo aggrProp = new PropertyInfo();
			aggrProp.setName( "Aggregation" );
			aggrProp.setValue( agg );
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
					SimpleDateFormat dfHourly = new SimpleDateFormat( "yyyy-MM-dd HH:mm" );
					SimpleDateFormat dfDaily = new SimpleDateFormat( "yyyy-MM-dd" );
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
								trans.date = dfHourly.parse( obj.getPropertyAsString( "transDateTime" ) );
							}catch( ParseException e )
							{
								try
								{
									trans.date = dfDaily.parse( obj.getPropertyAsString( "transDateTime" ) );
								}catch( ParseException e1 ){}
							}
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
					tv.setText( "" );
					if( position > 0 && position < getCount() - 1 )
					{
						StreamFlowTransmission trans = mData.get( i );
						try
						{
							tv.setText( df.format( trans.date ) );
						}catch( Exception e ){}
					}
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
