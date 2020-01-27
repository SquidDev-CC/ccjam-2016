package org.squiddev.plethora.core.wrapper;

import dan200.computercraft.api.lua.LuaException;
import net.minecraft.util.ResourceLocation;
import org.squiddev.plethora.api.method.IMethod;
import org.squiddev.plethora.api.method.IPartialContext;
import org.squiddev.plethora.api.method.IUnbakedContext;
import org.squiddev.plethora.api.method.MethodResult;
import org.squiddev.plethora.api.module.IModuleContainer;
import org.squiddev.plethora.core.ConfigCore;
import org.squiddev.plethora.core.RegisteredMethod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

final class MethodInstance<T> extends RegisteredMethod<T> implements IMethod<T> {
	final Method method;

	private final String name;
	private final String documentation;
	final boolean worldThread;
	private final ContextInfo[] requiredContext;
	final int totalContext;
	final ResourceLocation[] modules;
	private final Class<?>[] markerIfaces;
	private final Class<?> subtarget;

	private volatile Delegate<T> delegate;

	MethodInstance(
		Method method, Class<T> target, String modId, String name, String documentation, boolean worldThread,
		ContextInfo[] requiredContext, int totalContext,
		ResourceLocation[] modules, Class<?>[] markerIfaces, Class<?> subtarget
	) {
		super(
			method.getDeclaringClass().getName() + "#" + method.getName() + "(" + target.getSimpleName() + ")",
			modId, target
		);

		this.method = method;
		this.name = name;
		this.documentation = documentation;
		this.worldThread = worldThread;
		this.requiredContext = requiredContext;
		this.totalContext = totalContext;
		this.modules = modules;
		this.markerIfaces = markerIfaces;
		this.subtarget = subtarget;

		// If strict
		if (ConfigCore.Testing.strict) delegate = MethodClassLoader.INSTANCE.build(this);
	}

	@Override
	public boolean canApply(@Nonnull IPartialContext<T> context) {
		// Ensure we have all required modules.
		if (modules != null) {
			IModuleContainer moduleContainer = IModuleContainer.class.isAssignableFrom(target()) ? (IModuleContainer) context.getTarget() : context.getModules();
			for (ResourceLocation module : modules) {
				if (!moduleContainer.hasModule(module)) return false;
			}
		}

		// Ensure we have all required context info
		for (ContextInfo info : requiredContext) {
			if (info.key == null) {
				if (!context.hasContext(info.klass)) return false;
			} else {
				boolean any = false;
				for (String key : info.key) {
					if (context.hasContext(key, info.klass)) {
						any = true;
						break;
					}
				}

				if (!any) return false;
			}
		}

		return true;
	}

	@Nonnull
	@Override
	public MethodResult apply(@Nonnull IUnbakedContext<T> context, @Nonnull Object[] args) throws LuaException {
		Delegate<T> delegate = this.delegate;
		if (delegate == null) {
			synchronized (this) {
				if ((delegate = this.delegate) == null) {
					this.delegate = delegate = MethodClassLoader.INSTANCE.build(this);
				}
			}
		}

		return delegate.apply(context, args);
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String getDocString() {
		return documentation;
	}

	@Override
	public boolean has(@Nonnull Class<?> iface) {
		if (markerIfaces == null) return false;
		for (Class<?> klass : markerIfaces) {
			if (iface.isAssignableFrom(klass)) return true;
		}
		return false;
	}

	@Nonnull
	@Override
	public Collection<ResourceLocation> getModules() {
		return modules == null ? Collections.emptyList() : Arrays.asList(modules);
	}

	@Nullable
	@Override
	public Class<?> getSubTarget() {
		return subtarget;
	}

	@Override
	public IMethod<T> method() {
		return this;
	}

	static class ContextInfo {
		private final String[] key;
		private final Class<?> klass;

		ContextInfo(@Nullable String[] key, @Nonnull Class<?> klass) {
			this.key = key;
			this.klass = klass;
		}
	}
}
