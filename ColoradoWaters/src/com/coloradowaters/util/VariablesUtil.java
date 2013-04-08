package com.coloradowaters.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.coloradowaters.R;

public class VariablesUtil
{
	
	public static int getVariableIndex( Context context, String variable )
	{
		if( variable == null )
			return -1;
		
		int index = -1;
		
		String[] varKeyArray = context.getResources().getStringArray( R.array.variable_keys_array );
		for( int i = 0; i < varKeyArray.length; i++ )
		{
			String key = varKeyArray[i];
			if( variable.contains( key ) )
			{
				index = i;
				break;
			}
		}
		return index;
	}
	
	
	public static String getLabelForVariable( Context context, String variable )
	{
		if( variable == null )
			return null;
		
		int index = getVariableIndex( context, variable );
		String[] varLabelArray = context.getResources().getStringArray( R.array.variable_labels_array );
		
		if( index <= -1 )
		{
			return variable;
		}else
		{
			return varLabelArray[index];
		}
	}
	
	
	public static String getMeasurementForVariable( Context context, String variable )
	{
		if( variable == null )
			return null;
		
		int index = getVariableIndex( context, variable );
		String[] varMeasurementArray = context.getResources().getStringArray( R.array.variable_measure_array );
		
		if( index <= -1 )
		{
			return variable;
		}else
		{
			return varMeasurementArray[index];
		}
	}
	
}
