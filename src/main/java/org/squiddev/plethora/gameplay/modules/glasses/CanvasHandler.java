package org.squiddev.plethora.gameplay.modules.glasses;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.squiddev.plethora.gameplay.Plethora;
import org.squiddev.plethora.gameplay.modules.PlethoraModules;
import org.squiddev.plethora.gameplay.neural.NeuralHelpers;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraftforge.common.util.Constants.NBT;
import static org.squiddev.plethora.gameplay.neural.ItemComputerHandler.MODULE_DATA;

public class CanvasHandler {
	public static final int ID_2D = 0;
	public static final int ID_3D = 1;

	public static final int WIDTH = 512;
	public static final int HEIGHT = 512 / 16 * 9;

	private static AtomicInteger id = new AtomicInteger(0);
	private static final HashSet<CanvasServer> server = Sets.newHashSet();

	private static final Int2ObjectMap<CanvasClient> client = new Int2ObjectOpenHashMap<>();

	public static int nextId() {
		return id.getAndIncrement();
	}

	public static void addServer(CanvasServer canvas) {
		synchronized (server) {
			server.add(canvas);
			Plethora.network.sendTo(canvas.getAddMessage(), canvas.getPlayer());
		}
	}

	public static void removeServer(CanvasServer canvas) {
		synchronized (server) {
			server.remove(canvas);
			Plethora.network.sendTo(canvas.getRemoveMessage(), canvas.getPlayer());
		}
	}

	public static void addClient(CanvasClient canvas) {
		synchronized (client) {
			client.put(canvas.id, canvas);
		}
	}

	public static void removeClient(CanvasClient canvas) {
		synchronized (client) {
			client.remove(canvas.id);
		}
	}

	public static CanvasClient getClient(int id) {
		synchronized (client) {
			return client.get(id);
		}
	}

	public static void clear() {
		synchronized (server) {
			server.clear();
		}
		synchronized (client) {
			client.clear();
		}
	}

	@SubscribeEvent
	public void update(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;

		synchronized (server) {
			for (CanvasServer canvas : server) {
				MessageCanvasUpdate update = canvas.getUpdateMessage();
				if (update != null) {
					Plethora.network.sendTo(update, canvas.getPlayer());
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	private static CanvasClient getCanvas() {
		EntityPlayer playerMP = Minecraft.getMinecraft().player;
		ItemStack stack = NeuralHelpers.getStack(playerMP);

		if (stack.isEmpty()) return null;

		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || !tag.hasKey(MODULE_DATA, NBT.TAG_COMPOUND)) return null;

		NBTTagCompound modules = tag.getCompoundTag(MODULE_DATA);
		if (!modules.hasKey(PlethoraModules.GLASSES_S, NBT.TAG_COMPOUND)) {
			return null;
		}

		NBTTagCompound data = modules.getCompoundTag(PlethoraModules.GLASSES_S);
		if (!data.hasKey("id", NBT.TAG_ANY_NUMERIC)) return null;

		int id = data.getInteger("id");
		return getClient(id);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void render2DOverlay(RenderGameOverlayEvent.Post event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.HELMET) return;

		CanvasClient canvas = getCanvas();
		if (canvas == null) return;

		// If we've no font renderer then we're probably not quite ready yet
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.fontRenderer == null) return;

		GlStateManager.pushMatrix();

		// The hotbar renders at -90 (See GuiIngame#renderTooltip)
		GlStateManager.translate(0, 0, -100);

		ScaledResolution resolution = event.getResolution();
		GlStateManager.scale(resolution.getScaledWidth_double() / WIDTH, resolution.getScaledHeight_double() / HEIGHT, 2);

		synchronized (canvas) {
			canvas.drawChildren(canvas.getChildren(ID_2D).iterator());
		}

		GlStateManager.color(1.0f, 1.0f, 1.0f);
		GlStateManager.enableTexture2D();
		GlStateManager.enableCull();

		GlStateManager.popMatrix();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onWorldRender(RenderWorldLastEvent event) {
		CanvasClient canvas = getCanvas();
		if (canvas == null) return;

		synchronized (canvas) {
			canvas.drawChildren(canvas.getChildren(ID_3D).iterator());
		}
	}
}
