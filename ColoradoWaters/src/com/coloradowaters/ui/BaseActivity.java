package com.coloradowaters.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.coloradowaters.R;

public class BaseActivity extends Activity
{
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( getContentView() );
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
	
	
	protected int getContentView()
	{
		return R.layout.activity_empty;
	}
	
	
	protected static void showFragment( FragmentTransaction ft, Fragment f )
	{
		if( ( f != null ) && f.isHidden()) ft.show( f );
    }
	
	
	protected static void hideFragment( FragmentTransaction ft, Fragment f )
	{
	    if( ( f != null ) && !f.isHidden() ) ft.hide( f );
	}
}
