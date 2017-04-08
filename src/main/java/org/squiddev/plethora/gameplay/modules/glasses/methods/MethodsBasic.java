package org.squiddev.plethora.gameplay.modules.glasses.methods;

import dan200.computercraft.api.lua.LuaException;
import org.squiddev.plethora.api.method.BasicMethod;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.IUnbakedContext;
import org.squiddev.plethora.api.method.MethodResult;
import org.squiddev.plethora.gameplay.modules.glasses.BaseObject;
import org.squiddev.plethora.gameplay.modules.glasses.CanvasServer;
import org.squiddev.plethora.gameplay.modules.glasses.objects.Colourable;
import org.squiddev.plethora.gameplay.modules.glasses.objects.Scalable;
import org.squiddev.plethora.gameplay.modules.glasses.objects.Textable;

import static org.squiddev.plethora.api.method.ArgumentHelper.*;

public class MethodsBasic {
	@BasicMethod.Inject(value = BaseObject.class, doc = "function():int -- Get the id for this object.")
	public static MethodResult getID(IUnbakedContext<BaseObject> context, Object[] args) throws LuaException {
		return MethodResult.result(context.safeBake().getTarget().id);
	}

	@BasicMethod.Inject(value = BaseObject.class, doc = "function() -- Remove this object from the canvas.")
	public static MethodResult remove(IUnbakedContext<BaseObject> context, Object[] args) throws LuaException {
		IContext<BaseObject> baked = context.safeBake();
		baked.getContext(CanvasServer.class).remove(baked.getTarget());
		return MethodResult.result();
	}

	@BasicMethod.Inject(value = Colourable.class, doc = "function():int -- Get the colour for this object.")
	public static MethodResult getColour(IUnbakedContext<Colourable> context, Object[] args) throws LuaException {
		return MethodResult.result(context.safeBake().getTarget().getColour());
	}

	@BasicMethod.Inject(value = Colourable.class, doc = "function():int -- Get the color for this object.")
	public static MethodResult getColor(IUnbakedContext<Colourable> context, Object[] args) throws LuaException {
		return getColour(context, args);
	}

	@BasicMethod.Inject(value = Colourable.class, doc = "function(colour|r:int, [g:int, b:int], [alpha:int]):number -- Set the colour for this object.")
	public static MethodResult setColour(IUnbakedContext<Colourable> context, Object[] args) throws LuaException {
		Colourable object = context.safeBake().getTarget();
		switch (args.length) {
			case 1:
				object.setColour(getInt(args, 0));
				break;
			case 3: {
				int r = getInt(args, 0) & 0xFF;
				int g = getInt(args, 1) & 0xFF;
				int b = getInt(args, 2) & 0xFF;

				object.setColour((r << 24) | (g << 16) | (b << 8) | object.getColour() & 0xFF);
				break;
			}

			case 4: {
				int r = getInt(args, 0) & 0xFF;
				int g = getInt(args, 1) & 0xFF;
				int b = getInt(args, 3) & 0xFF;
				int a = getInt(args, 4) & 0xFF;

				object.setColour((r << 24) | (g << 16) | (b << 8) | a);
				break;
			}
		}
		return MethodResult.empty();
	}

	@BasicMethod.Inject(value = Colourable.class, doc = "function(color|r:int, [g:int, b:int], [alpha:int]):number -- Set the color for this object.")
	public static MethodResult setColor(IUnbakedContext<Colourable> object, Object[] args) throws LuaException {
		return setColour(object, args);
	}

	@BasicMethod.Inject(value = Colourable.class, doc = "function():int -- Get the alpha for this object.")
	public static MethodResult getAlpha(IUnbakedContext<Colourable> context, Object[] args) throws LuaException {
		return MethodResult.result(context.safeBake().getTarget().getColour() & 0xFF);
	}

	@BasicMethod.Inject(value = Colourable.class, doc = "function(alpha:int) -- Set the alpha for this object.")
	public static MethodResult setAlpha(IUnbakedContext<Colourable> context, Object[] args) throws LuaException {
		Colourable object = context.safeBake().getTarget();
		object.setColour((object.getColour() & ~0xFF) | (getInt(args, 0) & 0xFF));
		return MethodResult.empty();
	}

	@BasicMethod.Inject(value = Scalable.class, doc = "function():number -- Get the scale for this object.")
	public static MethodResult getScale(IUnbakedContext<Scalable> context, Object[] args) throws LuaException {
		Scalable object = context.safeBake().getTarget();
		return MethodResult.result(object.getScale());
	}

	@BasicMethod.Inject(value = Scalable.class, doc = "function(scale:number) -- Set the scale for this object.")
	public static MethodResult setScale(IUnbakedContext<Scalable> context, Object[] args) throws LuaException {
		Scalable object = context.safeBake().getTarget();

		float thickness = getFloat(args, 0);
		if (thickness <= 0) throw new LuaException("Scale must be > 0");
		object.setScale(thickness);
		return MethodResult.empty();
	}

	@BasicMethod.Inject(value = Textable.class, doc = "function():string -- Get the text for this object.")
	public static MethodResult getText(IUnbakedContext<Textable> context, Object[] args) throws LuaException {
		Textable object = context.safeBake().getTarget();
		return MethodResult.result(object.getText());
	}

	@BasicMethod.Inject(value = Textable.class, doc = "function(text:string) -- Set the text for this object.")
	public static MethodResult setText(IUnbakedContext<Textable> context, Object[] args) throws LuaException {
		Textable object = context.safeBake().getTarget();
		object.setText(getString(args, 0));
		return MethodResult.empty();
	}
}
