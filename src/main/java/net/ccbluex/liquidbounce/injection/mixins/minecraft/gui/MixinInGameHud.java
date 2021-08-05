/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.OverlayRenderEvent;
import net.ccbluex.liquidbounce.render.ultralight.RenderLayer;
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

  @Shadow
  private int scaledWidth;

  @Shadow
  private int scaledHeight;

  @Shadow
  protected abstract void renderHotbarItem(int x, int y, float tickDelta,
                                           PlayerEntity player,
                                           ItemStack stack, int seed);

  @Shadow
  protected abstract PlayerEntity getCameraPlayer();

  /**
   * Hook Ultralight in game layer
   */
  @Inject(method = "render", at = @At(value = "HEAD"))
  private void hookInGameLayer(MatrixStack matrices, float tickDelta, CallbackInfo callbackInfo) {
    UltralightEngine.INSTANCE.render(RenderLayer.IN_GAME_LAYER, matrices);
  }

  /**
   * Hook render hud event at the top layer
   */
  @Inject(method = "render", at = @At(value = "INVOKE",
                                      target = "Lnet/minecraft/client/gui/hud/InGameHud;" +
                                               "renderStatusEffectOverlay" +
                                               "(Lnet/minecraft/client/util/math/MatrixStack;)V",
                                      shift = At.Shift.AFTER))
  private void hookRenderEvent(MatrixStack matrices, float tickDelta, CallbackInfo callbackInfo) {
    EventManager.INSTANCE.callEvent(new OverlayRenderEvent(matrices, tickDelta));
  }

  @Inject(method = "renderHotbar", at = @At(value = "INVOKE"), cancellable = true)
  private void cancelRenderHotbar(float tickDelta, MatrixStack matrices,
                                  CallbackInfo callbackInfo) {
    final PlayerEntity playerEntity = this.getCameraPlayer();
    final int halfWidth = this.scaledWidth / 2;
    int x;
    final int y = this.scaledHeight - 22;
    int seed = 1;

    for (int i = 0; i < 9; i++) {
      x = halfWidth - 90 + i * 21 - 2;
      this.renderHotbarItem(x, y, tickDelta, playerEntity, playerEntity.getInventory().main.get(i),
                            seed++);
    }

    final ItemStack offHand = playerEntity.getOffHandStack();

    if (!offHand.isEmpty()) {
      this.renderHotbarItem(halfWidth - 110 - 8, y, tickDelta, playerEntity, offHand, seed++);
    }

    callbackInfo.cancel();
  }

  @Inject(method = "renderStatusBars", at = @At(value = "INVOKE"), cancellable = true)
  private void cancelRenderStatusBars(CallbackInfo callbackInfo) {
    callbackInfo.cancel();
  }

  @Inject(method = "renderExperienceBar", at = @At(value = "INVOKE"), cancellable = true)
  private void cancelRenderExperienceBar(CallbackInfo callbackInfo) {
    callbackInfo.cancel();
  }
}
