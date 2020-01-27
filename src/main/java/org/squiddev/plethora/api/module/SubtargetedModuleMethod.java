package org.squiddev.plethora.api.module;

import dan200.computercraft.api.lua.LuaException;
import net.minecraft.util.ResourceLocation;
import org.squiddev.plethora.api.method.ContextKeys;
import org.squiddev.plethora.api.method.IPartialContext;
import org.squiddev.plethora.api.method.IUnbakedContext;
import org.squiddev.plethora.api.method.MethodResult;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * A top-level module method which requires a particular context object to execute.
 */
public abstract class SubtargetedModuleMethod<T> extends ModuleContainerMethod {
	private final Class<T> klass;

	public SubtargetedModuleMethod(String name, Set<ResourceLocation> modules, Class<T> klass, String docs) {
		this(name, modules, klass, 0, docs);
	}

	public SubtargetedModuleMethod(String name, Set<ResourceLocation> modules, Class<T> klass, int priority, String docs) {
		super(name, modules, priority, docs);
		this.klass = klass;
	}

	public static <T> SubtargetedModuleMethod<T> of(String name, ResourceLocation module, Class<T> klass, String docs, Delegate<IModuleContainer> delegate) {
		return new SubtargetedModuleMethod<T>(name, Collections.singleton(module), klass, docs) {
			@Nonnull
			@Override
			public MethodResult apply(@Nonnull IUnbakedContext<IModuleContainer> context, @Nonnull Object[] args) throws LuaException {
				return delegate.apply(context, args);
			}
		};
	}

	@Override
	public boolean canApply(@Nonnull IPartialContext<IModuleContainer> context) {
		if (!super.canApply(context)) return false;
		if (context.hasContext(ContextKeys.ORIGIN, klass)) return true;

		for (ResourceLocation module : getModules()) {
			if (context.hasContext(module.toString(), klass)) return true;
		}
		return false;
	}

	@Nonnull
	@Override
	public Class<T> getSubTarget() {
		return klass;
	}
}
