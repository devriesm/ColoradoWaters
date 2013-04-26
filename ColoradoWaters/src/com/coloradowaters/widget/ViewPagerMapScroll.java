package com.coloradowaters.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.coloradowaters.R;

public class ViewPagerMapScroll extends ViewPager
{
	private static final String TAG = ViewPagerMapScroll.class.getSimpleName();
	
	
	public ViewPagerMapScroll( Context context )
	{
		super( context );
	}
	
	
	public ViewPagerMapScroll( Context context, AttributeSet attrs )
	{
		super( context, attrs );
	}
	
	
	@Override
	protected boolean canScroll( View view, boolean checkV, int dx, int x, int y )
	{
		if( view instanceof RelativeLayout && view.getId() == R.id.map_container )
		{
			return true;
		}
		
		return super.canScroll( view, checkV, dx, x, y );
		
	}
	
}
