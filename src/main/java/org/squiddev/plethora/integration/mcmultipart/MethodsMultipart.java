package org.squiddev.plethora.integration.mcmultipart;

import mcmultipart.MCMultiPart;
import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.slot.IPartSlot;
import org.squiddev.plethora.api.meta.TypedMeta;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.TypedLuaObject;
import org.squiddev.plethora.api.method.wrapper.FromTarget;
import org.squiddev.plethora.api.method.wrapper.Optional;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;
import org.squiddev.plethora.utils.Helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.squiddev.plethora.integration.mcmultipart.IntegrationMcMultipart.getBasicMeta;

public final class MethodsMultipart {
	private MethodsMultipart() {
	}

	@PlethoraMethod(modId = MCMultiPart.MODID, doc = "-- Get a list of all parts in the multipart.")
	public static List<Map<String, ?>> listParts(@FromTarget IMultipartContainer container) {
		return Helpers.map(container.getParts().values(), IntegrationMcMultipart::getBasicMeta);
	}

	@PlethoraMethod(modId = MCMultiPart.MODID, doc = "-- Get a lookup of slot to parts.")
	public static Map<String, ?> listSlottedParts(@FromTarget IMultipartContainer container) {
		Map<String, Map<String, ?>> parts = new HashMap<>();
		for (Map.Entry<IPartSlot, ? extends IPartInfo> slot : container.getParts().entrySet()) {
			parts.put(slot.getKey().getRegistryName().toString().toLowerCase(Locale.ENGLISH), getBasicMeta(slot.getValue()));
		}

		return parts;
	}

	@Optional
	@PlethoraMethod(modId = MCMultiPart.MODID, doc = "-- Get a reference to the part in the specified slot.")
	public static TypedLuaObject<IPartInfo> getSlottedPart(final IContext<IMultipartContainer> context, IPartSlot slot) {
		IMultipartContainer container = context.getTarget();

		IPartInfo part = container.get(slot).orElse(null);
		return part == null
			? null
			: context.makeChild(part, new ReferenceMultipart(container, part)).getObject();
	}

	@Optional
	@PlethoraMethod(modId = MCMultiPart.MODID, doc = "-- Get the metadata of the part in the specified slot.")
	public static TypedMeta<IPartInfo, ?> getSlottedPartMeta(final IContext<IMultipartContainer> context, IPartSlot slot) {
		IMultipartContainer container = context.getTarget();

		IPartInfo part = container.get(slot).orElse(null);
		return part == null ? null : context.makePartialChild(part).getMeta();
	}
}
