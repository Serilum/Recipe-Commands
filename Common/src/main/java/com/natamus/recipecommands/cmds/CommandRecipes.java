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
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
			.then(Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE))
			.executes((command) -> {
				CommandSourceStack source = command.getSource();
				
				try {
					sendRecipe(command);
				}
				catch (Exception ex) {
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
				.then(Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE))
				.executes((command) -> {
					CommandSourceStack source = command.getSource();
					
					try {
						sendRecipe(command);
					}
					catch (Exception ex) {
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
	private static void sendRecipe(CommandContext<CommandSourceStack> command) throws CommandSyntaxException, NoSuchMethodError {
		CommandSourceStack source = command.getSource();
		Player player = source.getPlayerOrException();
		Level level = player.level();
		if (level.isClientSide) {
			return;
		}

		Registry<Item> itemRegistry = level.registryAccess().lookupOrThrow(Registries.ITEM);

		RecipeHolder<?> recipeHolder = ResourceKeyArgument.getRecipe(command, "recipe");
		Recipe<?> recipe = recipeHolder.value();
		String recipeName = recipeHolder.id().toString();
		if (recipeName.contains(":")) {
			recipeName = recipeName.split(":")[1];
		}
		
		List<String> items = new ArrayList<String>();
		HashMap<String, Integer> itemcount = new HashMap<String, Integer>();
		
		List<Ingredient> ingredients = recipe.placementInfo().ingredients();
		for (Ingredient ingredient : ingredients) {
			List<Holder<Item>> possiblestacks = ingredient.items();
			if (possiblestacks.size() == 0) {
				continue;
			}

			Optional<ResourceKey<Item>> optionalItemResourceKey = possiblestacks.get(0).unwrapKey();
			if (optionalItemResourceKey.isEmpty()) {
				continue;
			}

			Optional<Holder.Reference<Item>> optionalItemHolder = itemRegistry.get(optionalItemResourceKey.get());
			if (optionalItemHolder.isEmpty()) {
				continue;
			}

			ItemStack itemstack = new ItemStack(optionalItemHolder.get());
			Item item = itemstack.getItem();
			String itemname = item.toString();
			if (possiblestacks.size() > 1 && !itemname.equalsIgnoreCase("cobblestone")) {
				Set<TagKey<Item>> tags = itemstack.getTags().collect(Collectors.toSet());
				if (tags.size() > 0) {
					TagKey<Item> tag = tags.iterator().next();
					itemname = tag.location().getPath();
				}
			}

			if (itemname.contains(":")) {
				itemname = itemname.split(":")[1];
			}
			
			itemname = StringFunctions.capitalizeEveryWord(itemname);
			
			if (items.contains(itemname)) {
				itemcount.compute(itemname, (k, currentcount) -> currentcount + 1);
				continue;
			}
			
			items.add(itemname);
			itemcount.put(itemname, 1);
		}
		
		Collections.sort(items);
		
		List<String> pattern = new ArrayList<String>();
		HashMap<String, String> itemkeys = new HashMap<String, String>();

		String outputName = recipeName;
		try {
			ItemStack output = recipe.assemble(null, level.registryAccess());
			outputName = output.getItem().toString();

			if (outputName.contains(":")) {
				outputName = outputName.split(":")[1];
			}
		}
		catch (Exception ignored) { }

		String jsonRecipe = Recipes.jsonrecipes.get(recipeName);
		if (jsonRecipe == null) {
			jsonRecipe = Recipes.jsonrecipes.get(outputName);
		}
		
		String shape = "shaped";
		if (jsonRecipe != null) {
			Gson gson = new Gson();
			Map<String, ?> map = gson.fromJson(jsonRecipe, Map.class);
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

		String formattedOutputName = StringFunctions.capitalizeEveryWord(outputName.replace("_", " "));
		
		MessageFunctions.sendMessage(source, formattedOutputName + " has a " + shape + " recipe.", ChatFormatting.DARK_GREEN, true);
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