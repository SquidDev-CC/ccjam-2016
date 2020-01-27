package org.squiddev.plethora.integration;

import dan200.computercraft.api.lua.LuaException;
import org.squiddev.plethora.api.Injects;
import org.squiddev.plethora.api.PlethoraAPI;
import org.squiddev.plethora.api.method.*;

import javax.annotation.Nonnull;
import java.util.Set;

import static dan200.computercraft.api.lua.ArgumentHelper.optString;

/**
 * Lists all available transfer locations
 */
@Injects
public final class MethodTransferLocations extends BasicMethod<IMethodCollection> {
	public MethodTransferLocations() {
		super("getTransferLocations", "function([location:string]):table -- Get a list of all available objects which can be transferred to or from");
	}

	@Override
	public boolean canApply(@Nonnull IPartialContext<IMethodCollection> context) {
		return context.getTarget().has(ITransferMethod.class);
	}

	@Nonnull
	@Override
	public MethodResult apply(@Nonnull final IUnbakedContext<IMethodCollection> context, @Nonnull Object[] args) throws LuaException {
		final String location = optString(args, 0, null);
		return MethodResult.nextTick(() -> {
			IContext<IMethodCollection> baked = context.bake();
			Set<String> locations;
			if (location == null) {
				locations = baked.getTransferLocations();
			} else {
				Object found = baked.getTransferLocation(location);
				if (found == null) throw new LuaException("Location '" + location + "' does not exist");

				locations = PlethoraAPI.instance().transferRegistry().getTransferLocations(found, false);
			}

			return MethodResult.result(locations);
		});
	}
}
