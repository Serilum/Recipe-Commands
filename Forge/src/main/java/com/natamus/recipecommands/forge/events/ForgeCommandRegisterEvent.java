package com.natamus.recipecommands.forge.events;

import com.natamus.recipecommands.cmds.CommandRecipes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ForgeCommandRegisterEvent {
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent e) {
    	CommandRecipes.register(e.getDispatcher());
    }
}