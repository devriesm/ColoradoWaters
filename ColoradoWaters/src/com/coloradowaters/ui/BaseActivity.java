package com.coloradowaters.ui;

import com.coloradowaters.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class BaseActivity extends Activity
{
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_empty );
	}
	
	public static Bundle intentToFragmentArguments( Intent intent )
	{
		Bundle arguments = new Bundle();
		if( intent == null )
		{
			return arguments;
		}
		
		final Uri data = intent.getData();
		if( data != null )
		{
			arguments.putParcelable( "_uri", data );
		}
		
		final Bundle extras = intent.getExtras();
		if( extras != null )
		{
			arguments.putAll( intent.getExtras() );
		}
		
		return arguments;
	}
}
