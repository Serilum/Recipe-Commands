package com.natamus.recipecommands.cmds;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.natamus.collective.functions.MessageFunctions;
import com.natamus.collective.functions.StringFunctions;
import com.natamus.recipecommands.util.Recipes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public class CommandRecipes {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("recipes")
			.requires((iCommandSender) -> iCommandSender.getEntity() instanceof ServerPlayer)
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				sendUsage(source);
				return 1;
			})
			.then(Commands.argument("recipe", ResourceLocationArgument.id()).suggests(SuggestionProviders.ALL_RECIPES)
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				try {
					sendRecipe(command);
				}
				catch (CommandSyntaxException ex) {
					MessageFunctions.sendMessage(source, "Unable to find recipe.", ChatFormatting.RED);
				}
				return 1;
			}))
		);
		dispatcher.register(Commands.literal("rec")
				.requires((iCommandSender) -> iCommandSender.getEntity() instanceof ServerPlayer)
				.executes((command) -> {
					CommandSourceStack source = command.getSource();
					
					sendUsage(source);
					return 1;
				})
				.then(Commands.argument("recipe", ResourceLocationArgument.id()).suggests(SuggestionProviders.ALL_RECIPES)
				.executes((command) -> {
					CommandSourceStack source = command.getSource();
					
					try {
						sendRecipe(command);
					}
					catch (CommandSyntaxException ex) {
						MessageFunctions.sendMessage(source, "Unable to find recipe.", ChatFormatting.RED);
					}
					return 1;
				}))
			);
	}
	
	private static void sendUsage(CommandSourceStack source) {
		MessageFunctions.sendMessage(source, "Recipe Commands Usage:", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendMessage(source, " /rec <recipe>", ChatFormatting.DARK_GREEN);
	}
	
	@SuppressWarnings("unchecked")
	private static void sendRecipe(CommandContext<CommandSourceStack> command) throws CommandSyntaxException {
		CommandSourceStack source = command.getSource();
		Player player = source.getPlayerOrException();
		Level level = player.level();
		if (level.isClientSide) {
			return;
		}
		
		RecipeHolder<?> recipeHolder = ResourceLocationArgument.getRecipe(command, "recipe");
		Recipe<?> recipe = recipeHolder.value();
		String recipeName = recipeHolder.toString();
		if (recipeName.contains(":")) {
			recipeName = recipeName.split(":")[1];
		}
		
		List<String> items = new ArrayList<String>();
		HashMap<String, Integer> itemcount = new HashMap<String, Integer>();
		
		List<Ingredient> ingredients = recipe.getIngredients();
		for (Ingredient ingredient : ingredients) {
			ItemStack[] possiblestacks = ingredient.getItems();
			if (possiblestacks.length == 0) {
				continue;
			}
			
			ItemStack itemstack = possiblestacks[0];
			Item item = itemstack.getItem();
			String itemname = item.toString();
			if (possiblestacks.length > 1 && !itemname.equalsIgnoreCase("cobblestone")) {
				Set<TagKey<Item>> tags = itemstack.getTags().collect(Collectors.toSet());
				if (tags.size() > 0) {
					TagKey<Item> tag = tags.iterator().next();
					itemname = tag.location().getPath();
				}
			}
			
			itemname = StringFunctions.capitalizeEveryWord(itemname);
			
			if (items.contains(itemname)) {
				int currentcount = itemcount.get(itemname);
				itemcount.put(itemname, currentcount+1);
				continue;
			}
			
			items.add(itemname);
			itemcount.put(itemname, 1);
		}
		
		Collections.sort(items);
		
		List<String> pattern = new ArrayList<String>();
		HashMap<String, String> itemkeys = new HashMap<String, String>();
		
		String shape = "shaped";
		if (Recipes.jsonrecipes.containsKey(recipeName)) {
			String jsonrecipe = Recipes.jsonrecipes.get(recipeName);
			Gson gson = new Gson();
			Map<String, ?> map = gson.fromJson(jsonrecipe, Map.class);
			String rawjson = map.toString();
			if (rawjson.contains("shapeless")) {
				shape = "shapeless";
			}
			
			pattern = (List<String>) map.get("pattern");
			
			String[] spl1 = rawjson.split("key=\\{");
			if (spl1.length > 1) {
				String keys = spl1[1].split("}},")[0];
				for (String keyraw : keys.split(", ")) {
					String[] keyspl = keyraw.split("=[{,\\[]");
					if (keyspl.length <= 1) {
						continue;
					}
					
					String key = keyspl[0];
					if (Recipes.replacekeys.containsKey(key)) {
						key = Recipes.replacekeys.get(key);
					}
					String itemvalue = keyspl[1].split(":")[1].replaceAll("}", "");
					itemkeys.put(itemvalue, key);
				}
			}
		}
		
		ItemStack output = recipe.getResultItem(level.registryAccess());
		String outputname = output.getItem().toString();
		outputname = StringFunctions.capitalizeEveryWord(outputname.replace("_", " "));
		
		MessageFunctions.sendMessage(source, outputname + " has a " + shape + " recipe.", ChatFormatting.DARK_GREEN, true);
		MessageFunctions.sendMessage(source, " Ingredients:", ChatFormatting.DARK_GREEN);
		for (String itemname : items) {
			int count = itemcount.get(itemname);
			String todisplayname = itemname;
			if (shape.equalsIgnoreCase("shaped")) {
				String shapeditemname = itemname.toLowerCase().replace(" ", "_").split("/")[0];
				if (itemkeys.containsKey(shapeditemname)) {
					String itemkey = itemkeys.get(shapeditemname);
					todisplayname += " (" + itemkey + ")";
				}
			}
			
			todisplayname = todisplayname.replace("_", " ");
			
			MessageFunctions.sendMessage(source, "  " + count + "x " + todisplayname, ChatFormatting.DARK_GREEN);
		}
		
		if (shape.equalsIgnoreCase("shaped") && pattern != null) {
			if (pattern.size() > 0) {
				MessageFunctions.sendMessage(source, " Pattern:", ChatFormatting.DARK_GREEN);
				
				for (String line : pattern) {
					for (String toreplace : Recipes.replacekeys.keySet()) {
						line = line.replaceAll(toreplace, Recipes.replacekeys.get(toreplace));
					}
					MessageFunctions.sendMessage(source, "  " + line.replace(" ", "_"), ChatFormatting.DARK_GREEN);
				}
			}
		}
	}
}