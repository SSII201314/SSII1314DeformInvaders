package com.android.opengl;

import com.main.model.GamePreferences;

public enum TTipoTexturasRenderer
{
	Personaje, Video, Juego;
	
	public int getNumCharacters()
	{
		switch(this)
		{
			case Personaje:
				return GamePreferences.NUM_TYPE_CHARACTER_DESIGN;
			case Video:
				return GamePreferences.NUM_TYPE_CHARACTER_VIDEO;
			case Juego:
				return GamePreferences.NUM_TYPE_CHARACTER_JUEGO;
			default:
				return 0;
		}
	}
	
	public int getNumTextures()
	{
		int numTexturasPersonaje = GamePreferences.NUM_TYPE_CHARACTER_DESIGN + (GamePreferences.NUM_TYPE_STICKERS * GamePreferences.NUM_TYPE_CHARACTER_DESIGN) + GamePreferences.NUM_TYPE_BUBBLES;
		
		switch(this)
		{
			case Personaje:
				return numTexturasPersonaje;
			case Video:
				return numTexturasPersonaje;
			case Juego:
				return numTexturasPersonaje + GamePreferences.NUM_TYPE_OPPONENTS + (GamePreferences.NUM_TYPE_STICKERS * GamePreferences.NUM_TYPE_ENEMIES);
			default:
				return 0;
		}
	}
}
