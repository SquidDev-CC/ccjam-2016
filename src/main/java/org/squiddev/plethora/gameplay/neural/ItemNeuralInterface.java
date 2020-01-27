package org.squiddev.plethora.gameplay.neural;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import baubles.common.Baubles;
import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.squiddev.plethora.gameplay.ItemBase;
import org.squiddev.plethora.gameplay.Plethora;
import org.squiddev.plethora.gameplay.client.ModelInterface;
import org.squiddev.plethora.gameplay.registry.Registration;
import org.squiddev.plethora.utils.Helpers;
import org.squiddev.plethora.utils.LoadedCache;
import org.squiddev.plethora.utils.PlayerHelpers;
import org.squiddev.plethora.utils.TinySlot;
import vazkii.botania.api.item.ICosmeticAttachable;
import vazkii.botania.common.lib.LibMisc;

import javax.annotation.Nonnull;
import java.util.List;

import static org.squiddev.plethora.gameplay.neural.ItemComputerHandler.COMPUTER_ID;
import static org.squiddev.plethora.gameplay.neural.ItemComputerHandler.DIRTY;
import static org.squiddev.plethora.gameplay.neural.NeuralHelpers.ARMOR_SLOT;

@Optional.InterfaceList({
	@Optional.Interface(iface = "baubles.api.IBauble", modid = Baubles.MODID),
	@Optional.Interface(iface = "vazkii.botania.api.item.ICosmeticAttachable", modid = LibMisc.MOD_ID)
})
@Mod.EventBusSubscriber(modid = Plethora.ID)
public class ItemNeuralInterface extends ItemArmor implements ISpecialArmor, IComputerItem, IMedia, IBauble, ICosmeticAttachable {
	private static final ArmorMaterial FAKE_ARMOUR = EnumHelper.addArmorMaterial("FAKE_ARMOUR", "iwasbored_fake", -1, new int[]{ 0, 0, 0, 0 }, 0, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 2);
	private static final ISpecialArmor.ArmorProperties FAKE_PROPERTIES = new ISpecialArmor.ArmorProperties(0, 0, 0);
	private static final String NAME = "neuralInterface";

	public ItemNeuralInterface() {
		super(FAKE_ARMOUR, 0, ARMOR_SLOT);

		setRegistryName(new ResourceLocation(Plethora.ID, NAME));
		setTranslationKey(Plethora.RESOURCE_DOMAIN + "." + NAME);
		setCreativeTab(Plethora.getCreativeTab());
	}

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity, EnumHand hand) {
		if (!NeuralRegistry.instance.canEquip(entity)) return false;

		if (entity.getItemStackFromSlot(NeuralHelpers.ARMOR_SLOT).isEmpty() && stack.getCount() == 1) {
			if (!player.getEntityWorld().isRemote) {
				entity.setItemStackToSlot(NeuralHelpers.ARMOR_SLOT, stack.copy());

				// Force dropping when killed
				if (entity instanceof EntityLiving) {
					EntityLiving living = (EntityLiving) entity;
					living.setDropChance(NeuralHelpers.ARMOR_SLOT, 2);
					living.enablePersistence();
				}

				if (!player.capabilities.isCreativeMode) {
					stack.setCount(0);
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	@Nonnull
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
		// Check if the entity we've just hit has a stack in the first slot. If so, use that instead.
		ItemStack stack = player.getHeldItem(hand);
		RayTraceResult hit = PlayerHelpers.findHitGuess(player);
		Entity entity = hit.entityHit;
		if (hit.typeOfHit == RayTraceResult.Type.ENTITY && !(entity instanceof EntityPlayer) && entity instanceof EntityLivingBase) {
			if (((EntityLivingBase) entity).getItemStackFromSlot(NeuralHelpers.ARMOR_SLOT).isEmpty() && stack.getCount() == 1) {
				return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
			}
		}

		if (LoadedCache.hasBaubles()) {
			IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
			for (int slot : NeuralHelpers.getBaubleType().getValidSlots()) {
				if (handler.getStackInSlot(slot).isEmpty()) {
					if (!world.isRemote) {
						handler.setStackInSlot(slot, stack.copy());
						if (!player.capabilities.isCreativeMode) stack.grow(-1);
					}

					return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
				}
			}
		}

		return super.onItemRightClick(world, player, hand);
	}

	private static void onUpdate(ItemStack stack, TinySlot inventory, EntityLivingBase player, boolean forceActive) {
		if (player.getEntityWorld().isRemote) {
			if (forceActive && player instanceof EntityPlayer) ItemComputerHandler.getClient(stack);
		} else {
			NBTTagCompound tag = ItemBase.getTag(stack);
			NeuralComputer neural;

			// Fetch computer
			if (forceActive) {
				neural = ItemComputerHandler.getServer(stack, player, inventory);
				neural.keepAlive();
			} else {
				neural = ItemComputerHandler.tryGetServer(stack);
				if (neural == null) return;
			}

			boolean dirty = false;

			// Sync computer ID
			int newId = neural.getID();
			if (!tag.hasKey(COMPUTER_ID) || tag.getInteger(COMPUTER_ID) != newId) {
				tag.setInteger(COMPUTER_ID, newId);
				dirty = true;
			}

			// Sync Label
			String newLabel = neural.getLabel();
			String label = stack.hasDisplayName() ? stack.getDisplayName() : null;
			if (!Objects.equal(newLabel, label)) {
				if (newLabel == null || newLabel.isEmpty()) {
					stack.clearCustomName();
				} else {
					stack.setStackDisplayName(newLabel);
				}
				dirty = true;
			}

			// Sync and update peripherals
			short dirtyStatus = tag.getShort(DIRTY);
			if (dirtyStatus != 0) {
				tag.setShort(DIRTY, (short) 0);
				dirty = true;
			}


			if (neural.update(player, stack, dirtyStatus)) {
				dirty = true;
			}

			if (dirty && inventory != null) {
				inventory.markDirty();
			}
		}
	}

	@Nonnull
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound oldCapNbt) {
		return new InvProvider(stack);
	}

	@Override
	public boolean setLabel(@Nonnull ItemStack stack, String name) {
		if (name == null) {
			stack.clearCustomName();
		} else {
			stack.setStackDisplayName(name);
		}
		return true;
	}

	@Override
	public String getAudioTitle(@Nonnull ItemStack stack) {
		return null;
	}

	@Override
	public SoundEvent getAudio(@Nonnull ItemStack stack) {
		return null;
	}

	@Override
	public IMount createDataMount(@Nonnull ItemStack stack, @Nonnull World world) {
		int id = getComputerID(stack);
		return id >= 0 ? ComputerCraftAPI.createSaveDirMount(world, "computer/" + id, (long) ComputerCraft.computerSpaceLimit) : null;

	}

	@Override
	public int getComputerID(@Nonnull ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag != null && tag.hasKey(COMPUTER_ID) ? tag.getInteger(COMPUTER_ID) : -1;
	}

	@Override
	public String getLabel(@Nonnull ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return stack.hasDisplayName() ? stack.getDisplayName() : null;
	}

	@Override
	public ComputerFamily getFamily(@Nonnull ItemStack stack) {
		return ComputerFamily.Advanced;
	}

	@Override
	public ItemStack withFamily(@Nonnull ItemStack stack, @Nonnull ComputerFamily family) {
		return stack;
	}

	@Override
	@Optional.Method(modid = Baubles.MODID)
	public BaubleType getBaubleType(ItemStack stack) {
		return NeuralHelpers.getBaubleType();
	}

	@Override
	@Optional.Method(modid = Baubles.MODID)
	public void onWornTick(ItemStack stack, EntityLivingBase player) {
		if (!(player instanceof EntityPlayer)) return;

		IBaublesItemHandler handler = BaublesApi.getBaublesHandler((EntityPlayer) player);
		for (int slot : NeuralHelpers.getBaubleType().getValidSlots()) {
			ItemStack slotStack = handler.getStackInSlot(slot);
			if (slotStack == stack) {
				onUpdate(stack, new TinySlot.BaublesSlot(stack, handler, slot), player, true);
			}
		}
	}

	@Override
	@Optional.Method(modid = Baubles.MODID)
	public void onEquipped(ItemStack stack, EntityLivingBase player) {
	}

	@Override
	@Optional.Method(modid = Baubles.MODID)
	public void onUnequipped(ItemStack stack, EntityLivingBase player) {
	}

	@Override
	@Optional.Method(modid = Baubles.MODID)
	public boolean canEquip(ItemStack stack, EntityLivingBase player) {
		return true;
	}

	@Override
	@Optional.Method(modid = Baubles.MODID)
	public boolean canUnequip(ItemStack stack, EntityLivingBase player) {
		return true;
	}

	@Override
	@Optional.Method(modid = Baubles.MODID)
	public boolean willAutoSync(ItemStack itemstack, EntityLivingBase player) {
		return false;
	}

	@Nonnull
	@Override
	public ItemStack getCosmeticItem(@Nonnull ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag != null && tag.hasKey("cosmetic", Constants.NBT.TAG_COMPOUND)
			? new ItemStack(tag.getCompoundTag("cosmetic"))
			: ItemStack.EMPTY;
	}

	@Override
	public void setCosmeticItem(@Nonnull ItemStack stack, @Nonnull ItemStack cosmetic) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null) stack.setTagCompound(tag = new NBTTagCompound());

		if (cosmetic.isEmpty()) {
			tag.removeTag("cosmetic");
		} else {
			tag.setTag("cosmetic", cosmetic.serializeNBT());
		}
	}

	private static final class InvProvider implements ICapabilityProvider {
		private final IItemHandler inv;

		private InvProvider(ItemStack stack) {
			inv = new NeuralItemHandler(stack);
		}

		@Override
		public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
			return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
			if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
				return (T) inv;
			} else {
				return null;
			}
		}
	}

	//region Armor stuff
	@Override
	public ArmorProperties getProperties(EntityLivingBase entityLivingBase, @Nonnull ItemStack itemStack, DamageSource damageSource, double v, int i) {
		return FAKE_PROPERTIES;
	}

	@Override
	public int getArmorDisplay(EntityPlayer entityPlayer, @Nonnull ItemStack itemStack, int i) {
		return 0;
	}

	@Override
	public void damageArmor(EntityLivingBase entityLivingBase, @Nonnull ItemStack itemStack, DamageSource damageSource, int damage, int slot) {
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World world, List<String> out, ITooltipFlag flag) {
		super.addInformation(stack, world, out, flag);
		out.add(Helpers.translateToLocal(getTranslationKey(stack) + ".desc"));

		NBTTagCompound tag = stack.getTagCompound();
		if (flag.isAdvanced()) {
			if (tag != null && tag.hasKey(COMPUTER_ID)) {
				out.add("Computer ID " + tag.getInteger(COMPUTER_ID));
			}
		}

		if (LoadedCache.hasBotania()) {
			ItemStack cosmetic = getCosmeticItem(stack);
			if (!cosmetic.isEmpty()) {
				out.add(Helpers
					.translateToLocalFormatted("botaniamisc.hasCosmetic", cosmetic.getDisplayName())
					.replaceAll("&", "\u00a7"));
			}
		}
	}

	@Override
	public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
		super.onArmorTick(world, player, stack);
		onUpdate(stack, new TinySlot.InventorySlot(stack, player.inventory), player, true);
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int um1, boolean um2) {
		super.onUpdate(stack, world, entity, um1, um2);
		if (entity instanceof EntityLivingBase) {
			TinySlot slot = entity instanceof EntityPlayer
				? new TinySlot.InventorySlot(stack, ((EntityPlayer) entity).inventory)
				: new TinySlot(stack);
			onUpdate(stack, slot, (EntityLivingBase) entity, false);
		}
	}

	@Nonnull
	@Override
	@SideOnly(Side.CLIENT)
	public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, EntityEquipmentSlot slot, ModelBiped existing) {
		return ModelInterface.getNormal();
	}

	@Nonnull
	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String existing) {
		return Plethora.RESOURCE_DOMAIN + ":textures/models/neural_interface.png";
	}


	/**
	 * Force armor ticks for entities
	 *
	 * @param event Entity armor ticks
	 */
	@SubscribeEvent
	public static void onEntityLivingUpdate(LivingEvent.LivingUpdateEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) return;

		TinySlot slot = NeuralHelpers.getSlot(event.getEntityLiving());
		if (slot != null) {
			onUpdate(slot.getStack(), slot, event.getEntityLiving(), true);
		}
	}

	/**
	 * Call the right click event earlier on.
	 *
	 * @param event The event to handle
	 */
	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!event.isCanceled() && Helpers.onEntityInteract(Registration.itemNeuralInterface, event.getEntityPlayer(), event.getTarget(), event.getHand())) {
			event.setCanceled(true);
		}
	}
	//endregion
}
