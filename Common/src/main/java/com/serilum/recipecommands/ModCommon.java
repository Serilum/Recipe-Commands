package com.serilum.recipecommands;


import com.serilum.recipecommands.util.Recipes;

public class ModCommon {

	public static void init() {
		load();
	}

	private static void load() {
		Recipes.InitRecipes();
	}
}