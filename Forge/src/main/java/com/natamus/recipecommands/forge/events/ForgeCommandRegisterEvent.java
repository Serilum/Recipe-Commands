package com.natamus.recipecommands.forge.events;

import com.natamus.recipecommands.cmds.CommandRecipes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeCommandRegisterEvent {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent e) {
    	CommandRecipes.register(e.getDispatcher());
    }
}