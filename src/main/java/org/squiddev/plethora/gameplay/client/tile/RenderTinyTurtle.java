package org.squiddev.plethora.gameplay.client.tile;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.squiddev.plethora.gameplay.tiny.BlockTinyTurtle;
import org.squiddev.plethora.gameplay.tiny.TileTinyTurtle;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import java.util.List;

public class RenderTinyTurtle extends TileEntitySpecialRenderer<TileTinyTurtle> {
	private static final ModelResourceLocation MODEL = new ModelResourceLocation("plethora:tiny_turtle", "facing=north");

	@Override
	public void render(TileTinyTurtle te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		renderTurtleAt(te, x, y, z, partialTicks);
		super.render(te, x, y, z, partialTicks, destroyStage, alpha);
	}

	private void renderTurtleAt(TileTinyTurtle turtle, double x, double y, double z, float partialTicks) {
		IBlockState state = turtle.getWorld().getBlockState(turtle.getPos());
		GlStateManager.pushMatrix();

		String label = turtle.getLabel();

		GlStateManager.translate(x, y, z);
		GlStateManager.translate(0.5f, 0.5f, 0.5f);
		GlStateManager.rotate(180.0f - turtle.getFacing().getHorizontalAngle(), 0.0f, 1.0f, 0.0f);
		if (label != null && (label.equals("Dinnerbone") || label.equals("Grumm"))) {
			// Flip the model and swap the cull face as winding order will have changed.
			GlStateManager.scale(1.0f, -1.0f, 1.0f);
			GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
		}
		GlStateManager.translate(-0.5f, -0.5f, -0.5f);

		int colour = turtle.getColour();

		renderModel(state, MODEL, new int[]{colour == -1 ? BlockTinyTurtle.DEFAULT_COLOUR : colour});
		renderUpgrade(state, turtle, TurtleSide.Left, partialTicks);
		renderUpgrade(state, turtle, TurtleSide.Right, partialTicks);

		GlStateManager.cullFace(GlStateManager.CullFace.BACK);

		GlStateManager.popMatrix();
	}

	private void renderUpgrade(IBlockState state, TileTinyTurtle turtle, TurtleSide side, float partialTicks) {
		ITurtleUpgrade upgrade = turtle.getUpgrade(side);
		if (upgrade == null) return;
		GlStateManager.pushMatrix();

		GlStateManager.translate(0.5f, 0.0f, 0.5f);
		GlStateManager.scale(0.5f, 0.5f, 0.5f);
		GlStateManager.translate(-0.5f, 0.5f, -0.5f);

		Pair<IBakedModel, Matrix4f> pair = upgrade.getModel(null, side);
		if (pair != null) {
			if (pair.getRight() != null) ForgeHooksClient.multiplyCurrentGlMatrix(pair.getRight());
			if (pair.getLeft() != null) renderModel(state, pair.getLeft(), null);
		}

		GlStateManager.popMatrix();
	}

	private void renderModel(IBlockState state, ModelResourceLocation model, int[] tints) {
		Minecraft mc = Minecraft.getMinecraft();
		ModelManager modelManager = mc.getRenderItem().getItemModelMesher().getModelManager();
		renderModel(state, modelManager.getModel(model), tints);
	}

	private void renderModel(IBlockState state, IBakedModel model, int[] tints) {
		Tessellator tessellator = Tessellator.getInstance();
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		renderQuads(tessellator, model.getQuads(state, null, 0), tints);
		for (EnumFacing facing : EnumFacing.VALUES) {
			renderQuads(tessellator, model.getQuads(state, facing, 0), tints);
		}
	}

	private void renderQuads(Tessellator tessellator, List<BakedQuad> quads, int[] tints) {
		BufferBuilder buffer = tessellator.getBuffer();
		VertexFormat format = DefaultVertexFormats.ITEM;
		buffer.begin(GL11.GL_QUADS, format);

		for (BakedQuad quad : quads) {
			VertexFormat quadFormat = quad.getFormat();
			if (quadFormat != format) {
				tessellator.draw();
				format = quadFormat;
				buffer.begin(GL11.GL_QUADS, quadFormat);
			}

			int index = quad.getTintIndex();
			int colour = tints != null && index >= 0 && index < tints.length ? tints[index] | -16777216 : -1;
			LightUtil.renderQuadColor(buffer, quad, colour);
		}

		tessellator.draw();
	}

	@Override
	protected void drawNameplate(TileTinyTurtle te, @Nonnull String label, double x, double y, double z, int maxDistance) {
		Entity entity = rendererDispatcher.entity;
		double distance = te.getDistanceSq(entity.posX, entity.posY, entity.posZ);

		if (distance <= (double) (maxDistance * maxDistance)) {
			float yaw = rendererDispatcher.entityYaw;
			float pitch = rendererDispatcher.entityPitch;
			EntityRenderer.drawNameplate(this.getFontRenderer(), label, (float) x + 0.5f, (float) y + 1f, (float) z + 0.5f, 0, yaw, pitch, false, false);
		}
	}
}
