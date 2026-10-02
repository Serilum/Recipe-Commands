package com.serilum.recipecommands.forge.events;

import com.serilum.recipecommands.cmds.CommandRecipes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeCommandRegisterEvent {
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent e) {
		CommandRecipes.register(e.getDispatcher());
	}
}