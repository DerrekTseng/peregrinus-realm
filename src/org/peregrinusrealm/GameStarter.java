package org.peregrinusrealm;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class GameStarter {
	
	public static final int ScreenWidth = 1280;
	public static final int ScreenHeight = 720;
	public static final int ForegroundFPS = 60;
	
	public static void main(String[] args) {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
		configuration.setTitle("My First Game");
		configuration.setWindowedMode(GameStarter.ScreenWidth, GameStarter.ScreenHeight);
		configuration.setResizable(true);
		configuration.setForegroundFPS(ForegroundFPS);
	}

}
