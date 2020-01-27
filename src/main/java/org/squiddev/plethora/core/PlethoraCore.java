package org.squiddev.plethora.core;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.api.IMC;
import mcjty.xnet.XNet;
import net.minecraft.entity.Entity;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.squiddev.plethora.api.Constants;
import org.squiddev.plethora.api.IPeripheralHandler;
import org.squiddev.plethora.api.method.ICostHandler;
import org.squiddev.plethora.api.module.IModuleHandler;
import org.squiddev.plethora.api.vehicle.IVehicleUpgradeHandler;
import org.squiddev.plethora.core.capabilities.*;
import org.squiddev.plethora.core.executor.TaskRunner;
import org.squiddev.plethora.core.wrapper.PlethoraMethodRegistry;
import org.squiddev.plethora.gameplay.Plethora;
import org.squiddev.plethora.gameplay.PlethoraFakePlayer;
import org.squiddev.plethora.integration.vanilla.IntegrationVanilla;
import org.squiddev.plethora.utils.Helpers;

import java.util.Objects;

import static org.squiddev.plethora.core.PlethoraCore.*;

@Mod(modid = ID, name = NAME, version = VERSION, dependencies = DEPENDENCIES, guiFactory = "org.squiddev.plethora.core.client.gui.GuiConfigCore")
@Mod.EventBusSubscriber(modid = ID)
public class PlethoraCore {
	public static final String ID = "plethora-core";
	public static final String NAME = "Plethora Core";
	public static final String VERSION = "${mod_version}";
	public static final String DEPENDENCIES = "required-after:forge@[${forge_version},);required-after:computercraft@[${cct_version},);required-after:cctweaked@[${cct_version},)";
	public static final ResourceLocation PERIPHERAL_HANDLER_KEY = new ResourceLocation(Plethora.ID, "peripheralHandler");

	public static final Logger LOG = LogManager.getLogger(ID);

	private ASMDataTable asmData;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		// Setup the config file
		ConfigCore.init(event.getSuggestedConfigurationFile());

		asmData = event.getAsmData();

		// Register capabilities
		CapabilityManager.INSTANCE.register(ICostHandler.class, new DefaultStorage<>(), DefaultCostHandler::new);
		CapabilityManager.INSTANCE.register(IModuleHandler.class, new DefaultStorage<>(), DefaultModuleHandler::new);
		CapabilityManager.INSTANCE.register(IPeripheral.class, new DefaultStorage<>(), DefaultPeripheral::new);
		CapabilityManager.INSTANCE.register(IPeripheralHandler.class, new DefaultStorage<>(), DefaultPeripheral::new);
		CapabilityManager.INSTANCE.register(IVehicleUpgradeHandler.class, new DefaultStorage<>(), DefaultVehicleUpgradeHandler::new);

		// Provide tiles with an IPeripheral capability.
		ComputerCraftAPI.registerPeripheralProvider((world, pos, side) -> {
			TileEntity te = world.getTileEntity(pos);
			return te != null && te.hasCapability(Constants.PERIPHERAL_CAPABILITY, side)
				? te.getCapability(Constants.PERIPHERAL_CAPABILITY, side) : null;
		});

		// Integration modules. Generally just listen to capability events
		IntegrationVanilla.setup();
		FMLInterModComms.sendFunctionMessage(XNet.MODID, "getXNet", "org.squiddev.plethora.integration.xnet.NetworkChannelType$Setup");
		FMLInterModComms.sendMessage("opencomputers", IMC.BLACKLIST_PERIPHERAL, MethodWrapperPeripheral.class.getName());
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Objects.requireNonNull(asmData, "asmData table cannot be null: this means preInit was not fired");

		// Load various objects from annotations
		long start = System.currentTimeMillis();

		Registry.register(asmData);
		PlethoraMethodRegistry.loadAsm(asmData);
		buildRegistries();

		long finish = System.currentTimeMillis();

		LOG.info(
			"Loaded {} methods and {} metadata providers in {} seconds",
			MethodRegistry.instance.providers.size(), MetaRegistry.instance.providers.size(), (finish - start) * 1e-3
		);

		ConfigCore.configuration.save();
	}

	static void buildRegistries() {
		TransferRegistry.instance.build();
		ConverterRegistry.instance.build();
		MetaRegistry.instance.build();
		MethodRegistry.instance.build();
	}

	@Mod.EventHandler
	public static void loadComplete(FMLLoadCompleteEvent event) {
		// Register our fallback peripheral provider at the very last instance.
		ComputerCraftAPI.registerPeripheralProvider(new PeripheralProvider());
	}

	@Mod.EventHandler
	public static void onServerStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandPlethora());
	}

	@Mod.EventHandler
	public static void onServerStart(FMLServerStartedEvent event) {
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
			DefaultCostHandler.reset();
			TaskRunner.SHARED.reset();
		}
	}

	@Mod.EventHandler
	public static void onServerStopped(FMLServerStoppedEvent event) {
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
			DefaultCostHandler.reset();
			TaskRunner.SHARED.reset();
		}
	}

	@Mod.EventHandler
	public static void onMessageReceived(FMLInterModComms.IMCEvent event) {
		for (FMLInterModComms.IMCMessage m : event.getMessages()) {
			if (m.isStringMessage()) {
				if (Constants.IMC_BLACKLIST_PERIPHERAL.equalsIgnoreCase(m.key)) {
					PlethoraCore.LOG.debug("Blacklisting peripheral " + m.getStringValue() + " due to IMC from " + m.getSender());
					PeripheralProvider.addToBlacklist(m.getStringValue());
				} else if (Constants.IMC_BLACKLIST_MOD.equalsIgnoreCase(m.key)) {
					PlethoraCore.LOG.debug("Blacklisting mod " + m.getStringValue() + " due to IMC from " + m.getSender());
					Helpers.blacklistMod(m.getStringValue());
				}
			}
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			DefaultCostHandler.update();
			TaskRunner.SHARED.update();
		}
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
		if (eventArgs.getModID().equals(PlethoraCore.ID)) {
			ConfigCore.sync();
			buildRegistries();
		}
	}

	@SubscribeEvent
	public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
		ModuleRegistry.instance.addRecipes(event.getRegistry());
	}

	@SubscribeEvent
	public static void onEntitySpawn(EntityJoinWorldEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof PlethoraFakePlayer)) return;

		event.setCanceled(true);
		event.getWorld().playerEntities.remove(entity);

		LOG.error("Attempted to spawn PlethoraFakePlayer ({}). This should NEVER happen.", entity, new IllegalStateException("Stacktrace as follows:"));
	}
}
