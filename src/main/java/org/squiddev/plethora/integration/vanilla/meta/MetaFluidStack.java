package org.squiddev.plethora.integration.vanilla.meta;

import com.google.common.collect.Maps;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.squiddev.plethora.api.meta.BasicMetaProvider;
import org.squiddev.plethora.api.meta.IMetaProvider;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Basic properties for fluid stacks
 */
@IMetaProvider.Inject(value = FluidStack.class)
public class MetaFluidStack extends BasicMetaProvider<FluidStack> {
	@Nonnull
	@Override
	public Map<Object, Object> getMeta(@Nonnull FluidStack fluidStack) {
		Map<Object, Object> data = Maps.newHashMap();
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
}
