package org.squiddev.plethora.integration.vanilla.meta;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.squiddev.plethora.api.Injects;
import org.squiddev.plethora.api.meta.BaseMetaProvider;
import org.squiddev.plethora.api.method.IPartialContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Displays fluids contained inside a container
 */
@Injects
public final class MetaFluidHandler extends BaseMetaProvider<IFluidHandler> {
	@Nonnull
	@Override
	public Map<String, ?> getMeta(@Nonnull IPartialContext<IFluidHandler> context) {
		IFluidHandler handler = context.getTarget();

		IFluidTankProperties[] properties = handler.getTankProperties();
		List<Map<String, ?>> tanks = new ArrayList<>(properties.length);
		for (IFluidTankProperties tank : properties) {
			if (handler.equals(tank)) continue;
			// Add any tanks which are not part of this one.
			tanks.add(context.makePartialChild(tank).getMeta());
		}

		return tanks.isEmpty() ? Collections.emptyMap() : Collections.singletonMap("tanks", tanks);
	}

	@Nonnull
	@Override
	public IFluidHandler getExample() {
		return new FluidHandlerItemStack(new ItemStack(Items.WATER_BUCKET), 1000) {
			final FluidStack stack = FluidRegistry.getFluidStack("water", 525);

			@Nullable
			@Override
			public FluidStack getFluid() {
				return stack;
			}
		};
	}
}
