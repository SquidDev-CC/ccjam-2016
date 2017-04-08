package org.squiddev.plethora.gameplay.modules;

import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.squiddev.plethora.api.Constants;
import org.squiddev.plethora.api.PlethoraAPI;
import org.squiddev.plethora.api.method.IContextBuilder;
import org.squiddev.plethora.api.minecart.IMinecartUpgradeHandler;
import org.squiddev.plethora.api.module.AbstractModuleHandler;
import org.squiddev.plethora.api.module.IModuleAccess;
import org.squiddev.plethora.api.module.IModuleRegistry;
import org.squiddev.plethora.api.reference.EntityReference;
import org.squiddev.plethora.gameplay.ConfigGameplay;
import org.squiddev.plethora.gameplay.ItemBase;
import org.squiddev.plethora.gameplay.Plethora;
import org.squiddev.plethora.gameplay.client.RenderHelpers;
import org.squiddev.plethora.gameplay.client.entity.RenderLaser;
import org.squiddev.plethora.gameplay.modules.glasses.*;
import org.squiddev.plethora.utils.Helpers;

import javax.annotation.Nonnull;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.squiddev.plethora.gameplay.ConfigGameplay.Kinetic.launchMax;
import static org.squiddev.plethora.gameplay.ConfigGameplay.Laser.maximumPotency;
import static org.squiddev.plethora.gameplay.ConfigGameplay.Laser.minimumPotency;
import static org.squiddev.plethora.gameplay.Plethora.ID;
import static org.squiddev.plethora.gameplay.registry.Packets.*;

public final class ItemModule extends ItemBase {
	public static final String INTROSPECTION = "introspection";
	public static final String LASER = "laser";
	public static final String SCANNER = "scanner";
	public static final String SENSOR = "sensor";
	public static final String KINETIC = "kinetic";
	public static final String CHAT = "chat";
	public static final String GLASSES = "glasses";

	public static final int INTROSPECTION_ID = 0;
	public static final int LASER_ID = 1;
	public static final int SCANNER_ID = 2;
	public static final int SENSOR_ID = 3;
	public static final int KINETIC_ID = 4;
	public static final int CHAT_ID = 5;
	public static final int GLASSES_ID = 6;

	private static final int MODULES = 7;

	private static final int[] TURTLE_MODULES = new int[]{
		LASER_ID,
		SCANNER_ID,
		SENSOR_ID,
	};

	private static final int[] POCKET_MODULES = new int[]{
		LASER_ID,
		SCANNER_ID,
		SENSOR_ID,
		INTROSPECTION_ID,
		KINETIC_ID,
		CHAT_ID,
	};

	public static final int[] MINECART_MODULES = new int[]{
		LASER_ID,
		SCANNER_ID,
		SENSOR_ID,
		INTROSPECTION_ID,
		KINETIC_ID,
	};

	private static final int MAX_TICKS = 72000;
	private static final int USE_TICKS = 30;

	/**
	 * We multiply the gaussian by this number.
	 * This is the change in velocity for each axis after normalisation.
	 *
	 * @see net.minecraft.entity.projectile.EntityThrowable#setThrowableHeading(double, double, double, float, float)
	 */
	private static final float LASER_MAX_SPREAD = (float) (0.1 / 0.007499999832361937);

	public ItemModule() {
		super("module");
		setHasSubtypes(true);
	}

	public static String getName(int id) {
		switch (id) {
			case INTROSPECTION_ID:
				return INTROSPECTION;
			case LASER_ID:
				return LASER;
			case SCANNER_ID:
				return SCANNER;
			case SENSOR_ID:
				return SENSOR;
			case KINETIC_ID:
				return KINETIC;
			case CHAT_ID:
				return CHAT;
			case GLASSES_ID:
				return GLASSES;
			default:
				return "unknown";
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName() + ".module_" + getName(stack.getItemDamage());
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> out) {
		for (int i = 0; i < MODULES; i++) {
			out.add(new ItemStack(this, 1, i));
		}
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		switch (stack.getItemDamage()) {
			case INTROSPECTION_ID:
			case CHAT_ID:
				if (!world.isRemote && !(player instanceof FakePlayer)) {
					if (player.isSneaking()) {
						UUID id = player.getGameProfile().getId();
						if (id != null) {
							NBTTagCompound compound = getTag(stack);
							compound.setLong("id_lower", id.getLeastSignificantBits());
							compound.setLong("id_upper", id.getMostSignificantBits());
							compound.setString("bound_name", player.getName());
						}
					} else {
						if (stack.getItemDamage() == INTROSPECTION_ID) {
							player.displayGUIChest(player.getInventoryEnderChest());
						}
					}
				}

				return stack;
			case LASER_ID:
			case KINETIC_ID:
				player.setItemInUse(stack, MAX_TICKS);
				return stack;
			default:
				return stack;
		}
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int remaining) {
		if (world.isRemote) return;

		// Get the number of ticks the laser has been used for
		// We use a float we'll have to cast it later anyway
		float ticks = MAX_TICKS - remaining;
		if (ticks > USE_TICKS) ticks = USE_TICKS;
		if (ticks < 0) ticks = 0;

		switch (stack.getItemDamage()) {
			case LASER_ID: {
				double potency = (ticks / USE_TICKS) * (maximumPotency - minimumPotency) + minimumPotency;
				double inaccuracy = (USE_TICKS - ticks) / USE_TICKS * LASER_MAX_SPREAD;

				world.spawnEntityInWorld(new EntityLaser(world, player, (float) inaccuracy, (float) potency));
				break;
			}
			case KINETIC_ID: {
				launch(player, player.rotationYaw, player.rotationPitch, (ticks / USE_TICKS) * launchMax);
				break;
			}
			default:
				super.onPlayerStoppedUsing(stack, world, player, remaining);
		}
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		if (stack.getItemDamage() == LASER_ID || stack.getItemDamage() == KINETIC_ID) {
			return EnumAction.BOW;
		} else {
			return super.getItemUseAction(stack);
		}
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		if (stack.getItemDamage() == LASER_ID || stack.getItemDamage() == KINETIC_ID) {
			return MAX_TICKS;
		} else {
			return super.getMaxItemUseDuration(stack);
		}
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> out, boolean um) {
		super.addInformation(stack, player, out, um);

		String entity = getEntityName(stack);
		if (entity != null) {
			out.add("Bound to " + entity);
		}
	}

	//region Registering
	@Override
	@SideOnly(Side.CLIENT)
	public void clientPreInit() {
		for (int i = 0; i < MODULES; i++) {
			Helpers.setupModel(this, i, "module_" + getName(i));
		}

		RenderingRegistry.registerEntityRenderingHandler(EntityLaser.class, new IRenderFactory<EntityLaser>() {
			@Override
			public Render<EntityLaser> createRenderFor(RenderManager renderManager) {
				return new RenderLaser(renderManager);
			}
		});
	}

	@Override
	public void preInit() {
		super.preInit();
		EntityRegistry.registerModEntity(EntityLaser.class, ID + ":laser", 0, Plethora.instance, 64, 10, true);


		IModuleRegistry registry = PlethoraAPI.instance().moduleRegistry();
		for (int id : TURTLE_MODULES) {
			ItemStack stack = new ItemStack(this, 1, id);
			registry.registerTurtleUpgrade(stack);
		}

		for (int id : POCKET_MODULES) {
			ItemStack stack = new ItemStack(this, 1, id);
			registry.registerPocketUpgrade(stack);
		}

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new CanvasHandler());

		Plethora.network.registerMessage(new MessageCanvasAdd.Handler(), MessageCanvasAdd.class, CANVAS_ADD_MESSAGE, Side.CLIENT);
		Plethora.network.registerMessage(new MessageCanvasRemove.Handler(), MessageCanvasRemove.class, CANVAS_REMOVE_MESSAGE, Side.CLIENT);
		Plethora.network.registerMessage(new MessageCanvasUpdate.Handler(), MessageCanvasUpdate.class, CANVAS_UPDATE_MESSAGE, Side.CLIENT);
	}

	@Override
	public void init() {
		super.init();

		GameRegistry.addShapedRecipe(new ItemStack(this, 1, INTROSPECTION_ID),
			"GCG",
			"CHC",
			"GCG",
			'G', Items.gold_ingot,
			'H', Items.diamond_helmet,
			'C', Blocks.ender_chest
		);

		GameRegistry.addShapedRecipe(new ItemStack(this, 1, LASER_ID),
			"III",
			"GDR",
			"  I",
			'D', Items.diamond,
			'I', Items.iron_ingot,
			'G', Blocks.glass,
			'R', Items.redstone
		);

		GameRegistry.addShapedRecipe(new ItemStack(this, 1, SCANNER_ID),
			"EDE",
			"IGI",
			"III",
			'G', Blocks.glass,
			'I', Items.iron_ingot,
			'E', Items.ender_pearl,
			'D', Blocks.dirt
		);

		GameRegistry.addShapedRecipe(new ItemStack(this, 1, SENSOR_ID),
			"ERE",
			"IGI",
			"III",
			'G', Blocks.glass,
			'I', Items.iron_ingot,
			'E', Items.ender_pearl,
			'R', Items.rotten_flesh
		);

		GameRegistry.addShapedRecipe(new ItemStack(this, 1, KINETIC_ID),
			"RGR",
			"PBP",
			"RGR",
			'G', Items.gold_ingot,
			'R', Items.redstone,
			'P', Blocks.piston,
			'B', Blocks.redstone_block
		);

		GameRegistry.addShapedRecipe(new ItemStack(this, 1, CHAT_ID),
			" RS",
			"WRN",
			"IIS",
			'R', Items.redstone,
			'S', Blocks.stone,
			'I', Items.iron_ingot,
			'N', Blocks.noteblock,
			'W', Blocks.wool
		);

		GameRegistry.addRecipe(new ItemStack(this, 1, GLASSES_ID),
			"MIM",
			"GGG",
			"IAI",
			'M', PeripheralItemFactory.create(PeripheralType.WiredModem, null, 1),
			'I', Items.iron_ingot,
			'G', Blocks.glass,
			'A', Items.iron_helmet
		);
	}

	//endregion

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound tag) {
		return new ItemModuleHandler(stack);
	}

	private static final class ItemModuleHandler extends AbstractModuleHandler implements ICapabilityProvider {
		private final ItemStack stack;
		private ResourceLocation moduleId;
		private IMinecartUpgradeHandler minecart;

		private ItemModuleHandler(ItemStack stack) {
			this.stack = stack;
		}

		@Override
		public boolean hasCapability(Capability<?> capability, EnumFacing enumFacing) {
			if (stack.getItemDamage() >= MODULES) return false;

			if (capability == Constants.MODULE_HANDLER_CAPABILITY) return true;
			if (capability == Constants.MINECART_UPGRADE_HANDLER_CAPABILITY) {
				return minecart != null || Arrays.binarySearch(MINECART_MODULES, stack.getItemDamage()) != -1;
			}

			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getCapability(Capability<T> capability, EnumFacing enumFacing) {
			if (stack.getItemDamage() >= MODULES) return null;

			if (capability == Constants.MODULE_HANDLER_CAPABILITY) return (T) this;

			if (capability == Constants.MINECART_UPGRADE_HANDLER_CAPABILITY) {
				if (minecart != null) {
					return (T) minecart;
				} else if (Arrays.binarySearch(MINECART_MODULES, stack.getItemDamage()) != -1) {
					return (T) (minecart = PlethoraAPI.instance().moduleRegistry().toMinecartUpgrade(this));
				} else {
					return null;
				}
			}

			return null;
		}

		@Nonnull
		@Override
		public ResourceLocation getModule() {
			// Cache the ID
			ResourceLocation id = moduleId;
			if (id == null) {
				return this.moduleId = new ResourceLocation(Plethora.RESOURCE_DOMAIN, getName(stack.getItemDamage()));
			} else {
				return id;
			}
		}

		@Override
		public void getAdditionalContext(@Nonnull IModuleAccess access, @Nonnull IContextBuilder builder) {
			Entity entity = getEntity(stack);
			if (entity != null) {
				builder.addContext(entity, new EntityReference<Entity>(entity));
			}

			switch (stack.getItemDamage()) {
				case CHAT_ID: {
					// Add a chat listener if we've got an entity (and are a chat module).
					Object owner = access.getOwner();
					Entity ownerEntity = owner instanceof Entity ? (Entity) owner : entity;

					if (ownerEntity != null) {
						ChatListener.Listener listener = new ChatListener.Listener(access, ownerEntity);
						builder.addContext(listener);
						builder.addAttachable(listener);
					}
					break;
				}
				case GLASSES_ID: {
					// Add a chat listener if we've got an entity (and are a chat module).
					Object owner = access.getOwner();
					EntityPlayerMP ownerEntity = owner instanceof EntityPlayerMP ? (EntityPlayerMP) owner : null;

					if (ownerEntity != null && !(ownerEntity instanceof FakePlayer)) {
						GlassesInstance glasses = new GlassesInstance(access, ownerEntity);
						builder.addContext(glasses);
						builder.addAttachable(glasses);
					}
					break;
				}
			}
		}

		@Nonnull
		@Override
		public Pair<IBakedModel, Matrix4f> getModel(float delta) {
			Matrix4f matrix = new Matrix4f();
			matrix.setIdentity();

			// We flip the laser so it points forwards on turtles.
			if (stack.getItemDamage() == LASER_ID) {
				delta = (float) ((delta + Math.PI) % (2 * Math.PI));
			}

			matrix.setRotation(new AxisAngle4f(0f, 1f, 0f, delta));

			return Pair.of(
				RenderHelpers.getMesher().getItemModel(stack),
				matrix
			);
		}
	}

	private static Entity getEntity(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null && tag.hasKey("id_lower", 99)) {
			MinecraftServer server = MinecraftServer.getServer();
			if (server == null) return null;
			return server.getEntityFromUuid(new UUID(tag.getLong("id_upper"), tag.getLong("id_lower")));
		} else {
			return null;
		}
	}

	private static String getEntityName(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null && tag.hasKey("bound_name", 8)) {
			return tag.getString("bound_name");
		} else {
			return null;
		}
	}

	private static final double TERMINAL_VELOCITY = -2;

	public static void launch(Entity entity, float yaw, float pitch, float power) {
		float motionX = -MathHelper.sin(yaw / 180.0f * (float) Math.PI) * MathHelper.cos(pitch / 180.0f * (float) Math.PI);
		float motionZ = MathHelper.cos(yaw / 180.0f * (float) Math.PI) * MathHelper.cos(pitch / 180.0f * (float) Math.PI);
		float motionY = -MathHelper.sin(pitch / 180.0f * (float) Math.PI);

		power /= MathHelper.sqrt_float(motionX * motionX + motionY * motionY + motionZ * motionZ);

		entity.addVelocity(motionX * power, motionY * power * ConfigGameplay.Kinetic.launchYScale, motionZ * power);
		entity.velocityChanged = true;

		if (ConfigGameplay.Kinetic.launchFallReset && motionY > 0) {
			if (entity.motionY > 0) {
				entity.fallDistance = 0;
			} else if (entity.motionY > TERMINAL_VELOCITY) {
				entity.fallDistance *= entity.motionY / TERMINAL_VELOCITY;
			}
		}

		if (ConfigGameplay.Kinetic.launchFloatReset && entity instanceof EntityPlayerMP) {
			((EntityPlayerMP) entity).playerNetServerHandler.floatingTickCount = 0;
		}
	}
}
