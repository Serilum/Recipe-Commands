package com.natamus.recipecommands;


import com.natamus.recipecommands.util.Recipes;

public class ModCommon {

	public static void init() {
		load();
	}

	private static void load() {
		Recipes.InitRecipes();
	}
}