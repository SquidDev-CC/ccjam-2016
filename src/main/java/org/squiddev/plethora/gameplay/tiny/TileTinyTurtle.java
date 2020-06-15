package org.squiddev.plethora.gameplay.tiny;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.squiddev.plethora.gameplay.TileBase;
import org.squiddev.plethora.utils.Helpers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

public class TileTinyTurtle extends TileBase {
	private static final String TAG_COLOUR = "Colour";
	private static final String TAG_ID = "ComputerId";
	private static final String TAG_LABEL = "Label";

	private static final String TAG_UPGRADE_LEFT = "UpgradeLeft";
	private static final String TAG_UPGRADE_RIGHT = "UpgradeRight";

	private int colour;
	private String label;
	private int id;

	private EnumMap<TurtleSide, ITurtleUpgrade> upgrades = new EnumMap<>(TurtleSide.class);

	public void setup(int id, String label, int colour) {
		this.id = id;
		this.label = label;
		this.colour = colour;

		markDirty();
	}

	public int getId() {
		return id;
	}

	public int getColour() {
		return colour;
	}

	public ITurtleUpgrade getUpgrade(TurtleSide side) {
		return upgrades.get(side);
	}

	public String getLabel() {
		return label;
	}

	public EnumFacing getFacing() {
		IBlockState state = world.getBlockState(pos);
		return state.getBlock() instanceof BlockTinyTurtle ? state.getValue(BlockTinyTurtle.FACING) : EnumFacing.NORTH;
	}

	private EnumFacing getRelative(EnumFacing side) {
		return EnumFacing.byHorizontalIndex(side.getHorizontalIndex() + getFacing().getHorizontalIndex());
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, EnumFacing side, Vec3d hit) {
		ItemStack stack = player.getHeldItem(hand);

		// Try to dye a turtle
		if (stack.getItem() == Items.DYE) {
			// Dye to change turtle colour
			if (!world.isRemote) {
				int dye = stack.getItemDamage() & 0xf;
				Colour currentDye = colour == -1 ? null : Colour.fromHex(colour);
				if (currentDye == null || dye != currentDye.ordinal()) {
					colour = Colour.values()[dye].getHex();
					markForUpdate();

					if (!player.isCreative()) stack.shrink(1);
				}
			}

			return true;
		} else if (stack.getItem() == Items.WATER_BUCKET && colour != -1) {
			// Water to remove turtle colour
			if (!world.isRemote) {
				colour = -1;
				markForUpdate();

				if (!player.isCreative()) {
					player.setHeldItem(hand, new ItemStack(Items.BUCKET));
					player.inventory.markDirty();
				}
			}

			return true;
		} else if (stack.getItem() == Items.NAME_TAG) {
			if (!world.isRemote) {
				label = stack.hasDisplayName() ? stack.getDisplayName() : null;
				markForUpdate();

				if (!player.isCreative()) stack.shrink(1);
			}

			return true;
		}

		// Try to set an upgrade.
		EnumFacing facing = getRelative(side);
		TurtleSide turtleSide = null;
		if (facing == EnumFacing.EAST) turtleSide = TurtleSide.Left;
		if (facing == EnumFacing.WEST) turtleSide = TurtleSide.Right;

		if (turtleSide != null) {
			if (!world.isRemote) {
				if (stack.isEmpty()) {
					removeUpgrade(turtleSide, side);
				} else {
					ITurtleUpgrade upgrade = ComputerCraft.getTurtleUpgrade(stack);
					if (upgrade != null) {
						removeUpgrade(turtleSide, side);
						upgrades.put(turtleSide, upgrade);
						markForUpdate();

						if (!player.isCreative()) stack.shrink(1);
					}
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public void broken() {
		super.broken();

		EnumFacing facing = getFacing();
		dropUpgrade(upgrades.get(TurtleSide.Left), facing.rotateY());
		dropUpgrade(upgrades.get(TurtleSide.Right), facing.rotateYCCW());
	}

	private void removeUpgrade(TurtleSide side, EnumFacing actualSide) {
		ITurtleUpgrade existing = upgrades.remove(side);
		if (existing == null) return;

		markForUpdate();
		dropUpgrade(existing, actualSide);
	}

	private void dropUpgrade(ITurtleUpgrade upgrade, EnumFacing actualSide) {
		if (upgrade == null) return;

		ItemStack dropStack = upgrade.getCraftingItem();
		if (dropStack.isEmpty()) return;

		Vec3d itemPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
			.add(new Vec3d(actualSide.getDirectionVec()).scale(0.75));

		Helpers.spawnItemStack(world, itemPos.x, itemPos.y, itemPos.z, dropStack.copy());
	}

	private void readCommon(NBTTagCompound nbt) {
		id = nbt.hasKey(TAG_ID) ? nbt.getInteger(TAG_ID) : -1;
		label = nbt.hasKey(TAG_LABEL) ? nbt.getString(TAG_LABEL) : null;
		colour = nbt.hasKey(TAG_COLOUR) ? nbt.getInteger(TAG_COLOUR) : -1;

		ITurtleUpgrade leftUpgrade = nbt.hasKey(TAG_UPGRADE_LEFT) ? ComputerCraft.getTurtleUpgrade(nbt.getString(TAG_UPGRADE_LEFT)) : null;
		if (leftUpgrade == null) {
			upgrades.remove(TurtleSide.Left);
		} else {
			upgrades.put(TurtleSide.Left, leftUpgrade);
		}

		ITurtleUpgrade rightUpgrade = nbt.hasKey(TAG_UPGRADE_RIGHT) ? ComputerCraft.getTurtleUpgrade(nbt.getString(TAG_UPGRADE_RIGHT)) : null;
		if (rightUpgrade == null) {
			upgrades.remove(TurtleSide.Right);
		} else {
			upgrades.put(TurtleSide.Right, rightUpgrade);
		}
	}

	private void writeCommon(NBTTagCompound nbt) {
		if (id >= 0) nbt.setInteger(TAG_ID, id);
		if (label != null) nbt.setString(TAG_LABEL, label);
		if (colour != -1) nbt.setInteger(TAG_COLOUR, colour);

		ITurtleUpgrade left = upgrades.get(TurtleSide.Left);
		if (left != null) nbt.setString(TAG_UPGRADE_LEFT, left.getUpgradeID().toString());

		ITurtleUpgrade right = upgrades.get(TurtleSide.Right);
		if (right != null) nbt.setString(TAG_UPGRADE_RIGHT, right.getUpgradeID().toString());
	}

	@Override
	protected void readDescription(NBTTagCompound tag) {
		super.readDescription(tag);
		readCommon(tag);
	}

	@Override
	protected void writeDescription(NBTTagCompound tag) {
		super.writeDescription(tag);
		writeCommon(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		readCommon(nbt);
	}

	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		writeCommon(nbt);

		return super.writeToNBT(nbt);
	}

	@Nullable
	@Override
	public ITextComponent getDisplayName() {
		return label == null ? null : new TextComponentString(label);
	}
}
