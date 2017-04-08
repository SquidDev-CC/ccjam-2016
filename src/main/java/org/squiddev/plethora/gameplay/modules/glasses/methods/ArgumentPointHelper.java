package org.squiddev.plethora.gameplay.modules.glasses.methods;

import dan200.computercraft.api.lua.LuaException;
import org.squiddev.plethora.api.method.ArgumentHelper;
import org.squiddev.plethora.gameplay.modules.glasses.objects.object2d.Point2D;
import org.squiddev.plethora.gameplay.modules.glasses.objects.object3d.Point3D;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static org.squiddev.plethora.api.method.ArgumentHelper.getTable;

public class ArgumentPointHelper {
	public static Point2D getPoint2D(@Nonnull Object[] args, int index) throws LuaException {
		return getPoint2D(getTable(args, index));
	}


	public static Point2D getPoint2D(Map<?, ?> point) throws LuaException {
		Object xObj, yObj;
		if (point.containsKey("x")) {
			xObj = point.get("x");
			yObj = point.get("y");

			if (!(xObj instanceof Number)) throw badKey(xObj, "x", "number");
			if (!(yObj instanceof Number)) throw badKey(yObj, "y", "number");
		} else {
			xObj = point.get(1.0);
			yObj = point.get(2.0);

			if (!(xObj instanceof Number)) throw badKey(xObj, "1", "number");
			if (!(yObj instanceof Number)) throw badKey(yObj, "2", "number");
		}

		return new Point2D(((Number) xObj).floatValue(), ((Number) yObj).floatValue());
	}

	public static Point3D getPoint3D(@Nonnull Object[] args, int index) throws LuaException {
		return getPoint3D(getTable(args, index));
	}


	public static Point3D getPoint3D(Map<?, ?> point) throws LuaException {
		Object xObj, yObj, zObj;
		if (point.containsKey("x")) {
			xObj = point.get("x");
			yObj = point.get("y");
			zObj = point.get("z");

			if (!(xObj instanceof Number)) throw badKey(xObj, "x", "number");
			if (!(yObj instanceof Number)) throw badKey(yObj, "y", "number");
			if (!(zObj instanceof Number)) throw badKey(zObj, "z", "number");
		} else {
			xObj = point.get(1.0);
			yObj = point.get(2.0);
			zObj = point.get(3.0);

			if (!(xObj instanceof Number)) throw badKey(xObj, "1", "number");
			if (!(yObj instanceof Number)) throw badKey(yObj, "2", "number");
			if (!(zObj instanceof Number)) throw badKey(zObj, "3", "number");
		}

		return new Point3D(((Number) xObj).floatValue(), ((Number) yObj).floatValue(), ((Number) zObj).floatValue());
	}

	@Nonnull
	public static LuaException badKey(@Nullable Object object, @Nonnull String key, @Nonnull String expected) {
		return new LuaException("Expected " + expected + " for key " + key + ", got " + ArgumentHelper.getType(object));
	}
}
