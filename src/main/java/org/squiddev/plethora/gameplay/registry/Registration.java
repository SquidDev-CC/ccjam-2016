package org.squiddev.plethora.gameplay.registry;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.squiddev.plethora.api.Constants;
import org.squiddev.plethora.api.PlethoraAPI;
import org.squiddev.plethora.api.module.IModuleRegistry;
import org.squiddev.plethora.core.PlethoraCore;
import org.squiddev.plethora.gameplay.ItemBlockBase;
import org.squiddev.plethora.gameplay.Plethora;
import org.squiddev.plethora.gameplay.keyboard.ItemKeyboard;
import org.squiddev.plethora.gameplay.modules.BlockManipulator;
import org.squiddev.plethora.gameplay.modules.ItemModule;
import org.squiddev.plethora.gameplay.modules.TileManipulator;
import org.squiddev.plethora.gameplay.modules.TurtleUpgradeKinetic;
import org.squiddev.plethora.gameplay.neural.ItemNeuralConnector;
import org.squiddev.plethora.gameplay.neural.ItemNeuralInterface;
import org.squiddev.plethora.gameplay.redstone.BlockRedstoneIntegrator;
import org.squiddev.plethora.gameplay.redstone.TileRedstoneIntegrator;
import org.squiddev.plethora.gameplay.tiny.BlockTinyTurtle;
import org.squiddev.plethora.gameplay.tiny.ItemTinyTurtle;

import java.util.Objects;

import static org.squiddev.plethora.gameplay.modules.PlethoraModules.*;

/**
 * The proxy class
 */
@Mod.EventBusSubscriber(modid = Plethora.ID)
public final class Registration {
	public static ItemNeuralInterface itemNeuralInterface;
	public static ItemNeuralConnector itemNeuralConnector;
	public static ItemModule itemModule;
	public static ItemKeyboard itemKeyboard;
	public static ItemTinyTurtle itemTinyTurtle;
	public static BlockManipulator blockManipulator;
	public static BlockRedstoneIntegrator blockRedstoneIntegrator;
	public static BlockTinyTurtle blockTinyTurtle;

	private Registration() {
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		event.getRegistry().registerAll(
			blockManipulator = new BlockManipulator(),
			blockRedstoneIntegrator = new BlockRedstoneIntegrator(),
			blockTinyTurtle = new BlockTinyTurtle()
		);

		registerTiles();

		ComputerCraftAPI.registerPeripheralProvider(blockManipulator);
	}

	private static void registerTiles() {
		GameRegistry.registerTileEntity(TileManipulator.class, Objects.requireNonNull(blockManipulator.getRegistryName()));
		GameRegistry.registerTileEntity(TileRedstoneIntegrator.class, Objects.requireNonNull(blockRedstoneIntegrator.getRegistryName()));

		// Prevent wrapping by accident
		FMLInterModComms.sendMessage(PlethoraCore.ID, Constants.IMC_BLACKLIST_PERIPHERAL, TileManipulator.class.getName());
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		event.getRegistry().registerAll(
			itemNeuralInterface = new ItemNeuralInterface(),
			itemNeuralConnector = new ItemNeuralConnector(),
			itemModule = new ItemModule(),
			itemKeyboard = new ItemKeyboard(),
			new ItemBlockBase(blockManipulator),
			new ItemBlockBase(blockRedstoneIntegrator),
			itemTinyTurtle = new ItemTinyTurtle(blockTinyTurtle)
		);

		registerUpgrades();
	}

	public static void registerUpgrades() {
		IModuleRegistry registry = PlethoraAPI.instance().moduleRegistry();
		for (int id : TURTLE_MODULES) {
			ItemStack stack = new ItemStack(itemModule, 1, id);
			registry.registerTurtleUpgrade(stack);
		}

		{ // Kinetic augment gets a special upgrade.
			ItemStack kineticStack = new ItemStack(itemModule, 1, KINETIC_ID);
			ComputerCraftAPI.registerTurtleUpgrade(new TurtleUpgradeKinetic(
				kineticStack,
				kineticStack.getCapability(Constants.MODULE_HANDLER_CAPABILITY, null),
				kineticStack.getTranslationKey() + ".adjective")
			);
		}

		for (int id : POCKET_MODULES) {
			ItemStack stack = new ItemStack(itemModule, 1, id);
			registry.registerPocketUpgrade(stack);
		}

		PlethoraAPI.instance().moduleRegistry().registerPocketUpgrade(new ItemStack(itemKeyboard));
	}
}

