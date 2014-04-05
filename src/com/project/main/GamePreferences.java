package com.project.main;

public class GamePreferences
{
	// Multitouch
	public static final int NUM_HANDLES = 10;

	// Animaci�n
	public static final int TIME_INTERVAL_ANIMATION = 20;
	public static final int NUM_FRAMES_ANIMATION = 34;

	// Enemigos
	public static final int MAX_TEXTURE_BACKGROUND = 3;
	public static final int MAX_TEXTURE_CHARACTER = 1;
	public static final int MAX_TEXTURE_STICKER = 3;
	public static final int MAX_TEXTURE_BUBBLE = 3;
	public static final int MAX_TEXTURE_HEART = 2;
	public static final int MAX_TEXTURE_OBSTACLE = 1;
	public static final int MAX_TEXTURE_FISSURE = 1;
	public static final int MAX_TEXTURE_ENEMY = 4;

	public static final int NUM_TYPE_ENEMIGOS = 4;
	public static final int NUM_TYPE_STICKERS = 5;

	public static final int TYPE_OBSTACLE = 0;
	public static final int TYPE_ENEMY = 0;
	public static final int TYPE_BOSS = TYPE_ENEMY + MAX_TEXTURE_ENEMY;

	/* FIXME Usar Width de pantalla */

	// Velocidades
	public static final float DIST_MOVIMIENTO_BACKGROUND = 4.0f;
	public static final float DIST_MOVIMIENTO_ENEMY = 10.0f;
	public static final float DIST_MOVIMIENTO_CHARACTER = 30.0f;

	// Niveles
	public static final int NUM_LEVELS = 5;
	public static final int MAX_ENEMIES = 20;

	// Diastancias Escenario
	public static final float DISTANCE_RIGHT = 70.0f;
	public static final float DISTANCE_BOTTOM = 70.0f;

	public static final float DISTANCE_BETWEEN_ENEMY = 700.0f;
	public static final float POS_ENEMIES_INICIO = 1280.0f;
	public static final float POS_ENEMIES_FINAL = POS_ENEMIES_INICIO + MAX_ENEMIES * DISTANCE_BETWEEN_ENEMY;
	public static final float POS_BOSS = POS_ENEMIES_FINAL + DISTANCE_BETWEEN_ENEMY;

	public static final int NUM_ITERATION_BACKGROUND = 5;
}