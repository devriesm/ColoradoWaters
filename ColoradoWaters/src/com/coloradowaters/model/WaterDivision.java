package com.coloradowaters.model;

import java.util.ArrayList;
import java.util.List;

import com.coloradowaters.R;

import android.content.Context;
import android.content.res.Resources;

public class WaterDivision
{
	
	public int id;
	public String name;
	public List<WaterDistrict> districts;
	
	
	
	public static List<WaterDivision> getWaterDivisions( Context context )
	{
		Resources res = context.getResources();
		List<WaterDivision> divisions = new ArrayList<WaterDivision>();
		
		String[] divisionNames = res.getStringArray( R.array.water_divisions_names );
		int[] divisionIds = res.getIntArray( R.array.water_divisions_ids );
		
		for( int i = 0; i < divisionNames.length; i++ )
		{
			WaterDivision div = new WaterDivision();
			div.name = divisionNames[i];
			div.id = divisionIds[i];
			
			divisions.add( div );
		}
		
		return divisions;
	}
	
	
	@Override
	public String toString()
	{
		return name;
	}
}
