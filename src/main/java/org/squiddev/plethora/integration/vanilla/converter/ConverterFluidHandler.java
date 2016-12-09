package org.squiddev.plethora.integration.vanilla.converter;

import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.squiddev.plethora.api.converter.IConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@IConverter.Inject(ICapabilityProvider.class)
public class ConverterFluidHandler implements IConverter<ICapabilityProvider, IFluidHandler> {
	@Nullable
	@Override
	public IFluidHandler convert(@Nonnull ICapabilityProvider from) {
		return from.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
	}
}
