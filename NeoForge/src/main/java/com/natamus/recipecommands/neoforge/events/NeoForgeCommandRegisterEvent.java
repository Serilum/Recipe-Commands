package com.natamus.recipecommands.neoforge.events;

import com.natamus.recipecommands.cmds.CommandRecipes;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class NeoForgeCommandRegisterEvent {
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent e) {
		CommandRecipes.register(e.getDispatcher());
	}
}