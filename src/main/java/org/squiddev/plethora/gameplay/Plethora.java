package org.squiddev.plethora.gameplay;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.squiddev.plethora.gameplay.client.RenderOverlay;
import org.squiddev.plethora.gameplay.keyboard.ServerKeyListener;
import org.squiddev.plethora.gameplay.modules.glasses.CanvasHandler;
import org.squiddev.plethora.gameplay.registry.Registry;

import static org.squiddev.plethora.gameplay.Plethora.*;

@Mod(modid = ID, name = NAME, version = VERSION, dependencies = DEPENDENCIES, guiFactory = "org.squiddev.plethora.gameplay.client.gui.GuiConfigGameplay")
public class Plethora {
	public static final String ID = "plethora";
	public static final String NAME = "Plethora";
	public static final String VERSION = "${mod_version}";
	public static final String RESOURCE_DOMAIN = "plethora";
	public static final String DEPENDENCIES = "required-after:ComputerCraft@[${cc_version},);required-after:plethora-core";

	public static CreativeTabs getCreativeTab() {
		return tab;
	}

	@Mod.Instance(ID)
	public static Plethora instance;

	private static PlethoraCreativeTab tab;

	public static SimpleNetworkWrapper network;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ConfigGameplay.init(event.getSuggestedConfigurationFile());

		EntityRegistry.registerModEntity(PlethoraFakePlayer.class, ID + ":fakePlayer", 1, instance, Integer.MAX_VALUE, Integer.MAX_VALUE, false);

		network = NetworkRegistry.INSTANCE.newSimpleChannel(ID);

		tab = new PlethoraCreativeTab();
		Registry.setup();
		Registry.preInit();

		MinecraftForge.EVENT_BUS.register(this);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		Registry.init();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Registry.postInit();
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
		if (eventArgs.modID.equals(Plethora.ID)) {
			ConfigGameplay.sync();
		}
	}

	@EventHandler
	public void onServerStopping(FMLServerStoppedEvent e) {
		RenderOverlay.clearChatMessages();
		ServerKeyListener.clear();
		CanvasHandler.clear();
	}

	@EventHandler
	public void onServerStarting(FMLServerStartedEvent e) {
		RenderOverlay.clearChatMessages();
		ServerKeyListener.clear();
		CanvasHandler.clear();
	}

	private static class PlethoraCreativeTab extends CreativeTabs {
		public PlethoraCreativeTab() {
			super(RESOURCE_DOMAIN);
		}

		@Override
		public Item getTabIconItem() {
			return Registry.itemNeuralInterface;
		}
	}
}
