package org.squiddev.plethora.gameplay.tiny;

import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.squiddev.plethora.gameplay.ItemBlockBase;

import javax.annotation.Nonnull;

public class ItemTinyTurtle extends ItemBlockBase implements IComputerItem, IColouredItem {
	private static final String TAG_COLOUR = "colour";
	private static final String TAG_ID = "computer_id";

	public ItemTinyTurtle(BlockTinyTurtle turtle) {
		super(turtle);
	}

	@Override
	public int getColour(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		return nbt != null && nbt.hasKey(TAG_COLOUR) ? nbt.getInteger(TAG_COLOUR) : -1;
	}

	@Override
	public ItemStack withColour(ItemStack stack, int colour) {
		ItemStack copy = stack.copy();
		setColourDirect(copy, colour);
		return copy;
	}

	@Override
	public ItemStack withFamily(@Nonnull ItemStack stack, @Nonnull ComputerFamily computerFamily) {
		return computerFamily == ComputerFamily.Advanced ? stack : ItemStack.EMPTY;
	}

	@Override
	public int getComputerID(@Nonnull ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		return nbt != null && nbt.hasKey(TAG_ID) ? nbt.getInteger(TAG_ID) : -1;
	}

	@Override
	public String getLabel(@Nonnull ItemStack stack) {
		return stack.hasDisplayName() ? stack.getDisplayName() : null;
	}

	@Override
	public ComputerFamily getFamily(@Nonnull ItemStack itemStack) {
		return ComputerFamily.Advanced;
	}

	public static void setup(ItemStack stack, int computerId, String label, int colour) {
		if (computerId >= 0) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag == null) stack.setTagCompound(tag = new NBTTagCompound());
			tag.setInteger(TAG_ID, computerId);
		}

		if (label != null) stack.setStackDisplayName(label);

		setColourDirect(stack, colour);
	}

	private static void setColourDirect(ItemStack stack, int colour) {
		NBTTagCompound tag = stack.getTagCompound();
		if (colour == -1) {
			if (tag != null) tag.removeTag("colour");
		} else {
			if (tag == null) stack.setTagCompound(tag = new NBTTagCompound());
			tag.setInteger("colour", colour);
		}
	}

	@Override
	public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState newState) {
		if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) return false;
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileTinyTurtle)) return false;

		TileTinyTurtle computer = (TileTinyTurtle) tile;
		computer.setup(getComputerID(stack), getLabel(stack), getColour(stack));
		return true;
	}
}
