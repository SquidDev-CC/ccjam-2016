package org.squiddev.plethora.integration.vanilla.meta;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.squiddev.plethora.api.Injects;
import org.squiddev.plethora.api.meta.BasicMetaProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic properties for fluid stacks
 */
@Injects
public final class MetaFluidStack extends BasicMetaProvider<FluidStack> {
	private static final MetaFluidStack INSTANCE = new MetaFluidStack();

	public MetaFluidStack() {
		super("Provides information about a fluid, as well as how much is currently stored.");
	}

	@Nonnull
	public static Map<String, ?> getBasicMeta(@Nonnull FluidStack fluidStack) {
		return INSTANCE.getMeta(fluidStack);
	}

	@Nonnull
	@Override
	public Map<String, ?> getMeta(@Nonnull FluidStack fluidStack) {
		Map<String, Object> data = new HashMap<>();
		data.put("amount", fluidStack.amount);

		Fluid fluid = fluidStack.getFluid();
		if (fluid != null) {
			data.put("name", fluid.getName());
			data.put("id", FluidRegistry.getDefaultFluidName(fluid));
			data.put("rawName", fluid.getUnlocalizedName(fluidStack));
			data.put("displayName", fluid.getLocalizedName(fluidStack));
		}

		return data;
	}

	@Nullable
	@Override
	public FluidStack getExample() {
		return FluidRegistry.getFluidStack("water", 525);
	}
}
