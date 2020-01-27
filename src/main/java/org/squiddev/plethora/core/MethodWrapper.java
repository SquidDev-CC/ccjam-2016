package org.squiddev.plethora.core;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Handles the base for various Lua objects
 *
 * @see dan200.computercraft.api.peripheral.IPeripheral
 * @see dan200.computercraft.api.lua.ILuaObject
 */
public class MethodWrapper {
	private final List<RegisteredMethod<?>> methods;
	private final List<UnbakedContext<?>> contexts;

	private final String[] names;

	public MethodWrapper(List<RegisteredMethod<?>> methods, List<UnbakedContext<?>> contexts) {
		this.contexts = contexts;
		this.methods = methods;

		String[] names = this.names = new String[methods.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = methods.get(i).method().getName();
		}
	}

	@Nonnull
	public String[] getMethodNames() {
		return names;
	}

	public RegisteredMethod<?> getMethod(int i) {
		return methods.get(i);
	}

	public UnbakedContext<?> getContext(int i) {
		return contexts.get(i);
	}

	protected static Object[] getReferences(IComputerAccess access, ILuaContext context) {
		return new Object[]{ access, context };
	}

	/**
	 * Check if the methods are the same on both.
	 *
	 * This isn't a perfect test for equality: it doesn't check contexts are the same
	 *
	 * @param other The item to match against
	 * @return Whether the methods are equal
	 */
	boolean equalMethods(MethodWrapper other) {
		// Do the easy version: check they are the same items with same order.
		if (methods.equals(other.methods)) return true;

		if (methods.size() != other.methods.size()) return false;

		/*
			Doubt this is possible but better safe than sorry?
			We *could* make a hash set but this path is not going to be visited much, also not sure if there would be
			any efficiency gain: the method count is pretty small.
		  */
		for (RegisteredMethod method : methods) {
			if (!other.methods.contains(method)) return false;
		}
		return true;
	}
}
