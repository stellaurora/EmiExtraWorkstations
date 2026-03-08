package com.stellaurora.emiextraworkstations.mixin;

import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = RecipeScreen.class, remap = false)
public abstract class RecipeScreenMixin {

    @Shadow int backgroundHeight;
    @Shadow int backgroundWidth;
    @Shadow int x;
    @Shadow int y;
    @Shadow @Final private static ResourceLocation TEXTURE;

    @Shadow public abstract int getResolveOffset();
    @Shadow public abstract EmiRecipeCategory getFocusedCategory();

    private int emi$getSlotsPerLine() {
        return switch (EmiConfig.workstationLocation) {
            case LEFT, RIGHT -> Math.max(1, (backgroundHeight - getResolveOffset() - 18) / 18);
            case BOTTOM      -> Math.max(1, (backgroundWidth  - getResolveOffset() - 18) / 18);
            default          -> 1;
        };
    }

    private int emi$getLineCount(int total) {
        if (total == 0) return 0;
        return (int) Math.ceil((double) total / emi$getSlotsPerLine());
    }

    private Bounds emi$slotBounds(int i) {
        int slotsPerLine  = emi$getSlotsPerLine();
        int line          = i / slotsPerLine;
        int slot          = i % slotsPerLine;
        int resolveOffset = getResolveOffset();

        return switch (EmiConfig.workstationLocation) {
            case LEFT   -> new Bounds(x - 18 - line * 18,               y + 9 + resolveOffset + slot * 18, 18, 18);
            case RIGHT  -> new Bounds(x + backgroundWidth + line * 18,  y + 9 + resolveOffset + slot * 18, 18, 18);
            case BOTTOM -> new Bounds(x + 5 + resolveOffset + slot * 18, y + backgroundHeight - 23 + line * 18, 18, 18);
            default     -> Bounds.EMPTY;
        };
    }

    @Redirect(
        method = "setPage",
        at = @At(
            value = "INVOKE",
            target = "Ldev/emi/emi/screen/RecipeScreen;getMaxWorkstations()I"
        )
    )
    private int emi$unlimitWorkstationsSetPage(RecipeScreen self) {
        return Integer.MAX_VALUE;
    }

    @Redirect(
        method = "setPage",
        at = @At(
            value = "INVOKE",
            target = "Ldev/emi/emi/screen/RecipeScreen;getWorkstationBounds(I)Ldev/emi/emi/api/widget/Bounds;"
        )
    )
    private Bounds emi$redirectBoundsSetPage(RecipeScreen self, int i) {
        return emi$slotBounds(i);
    }
    
    @Inject(
        method = "render",
        remap = true, 
        at = @At(
            value = "INVOKE",
            target = "Ldev/emi/emi/screen/RecipeScreen;getMaxWorkstations()I",
            remap = false 
        )
    )
    private void emi$drawUnifiedWorkstationPanel(
            GuiGraphics raw, int mouseX, int mouseY, float delta,
            CallbackInfo ci) {
        EmiDrawContext context = EmiDrawContext.wrap(raw);

        List<EmiIngredient> workstations = EmiApi.getRecipeManager().getWorkstations(getFocusedCategory());
        int total         = workstations == null ? 0 : workstations.size();
        int slotsPerLine  = emi$getSlotsPerLine();
        int lineCount     = emi$getLineCount(total);
        int resolveOffset = getResolveOffset();

        int slotsInFirstLine = Math.min(slotsPerLine, total);

        boolean hasContent = total > 0 || RecipeScreen.resolve != null;
        if (!hasContent) return;

        int effectiveLines = Math.max(lineCount, 1);

        switch (EmiConfig.workstationLocation) {
            case LEFT -> {

                int panelX = x - 18 - (effectiveLines - 1) * 18;
                int panelY = y + 9 - resolveOffset;

                int panelW = 10 + 18 * effectiveLines;
                int panelH = (slotsInFirstLine == 0 && resolveOffset > 0)
                    ? 10 + resolveOffset
                    : 10 + 18 * slotsInFirstLine + resolveOffset;
                EmiRenderHelper.drawNinePatch(context, TEXTURE,
                    panelX - 5, panelY - 5, panelW, panelH, 36, 0, 5, 1);
            }
            case RIGHT -> {
                int panelX = x + backgroundWidth;
                int panelY = y + 9 - resolveOffset;
                int panelW = 10 + 18 * effectiveLines;
                int panelH = (slotsInFirstLine == 0 && resolveOffset > 0)
                    ? 10 + resolveOffset
                    : 10 + 18 * slotsInFirstLine + resolveOffset;
                EmiRenderHelper.drawNinePatch(context, TEXTURE,
                    panelX - 5, panelY - 5, panelW, panelH, 47, 0, 5, 1);
            }
            case BOTTOM -> {
                int panelX = x + 5 - resolveOffset;
                int panelY = y + backgroundHeight - 23;
                int panelW = (slotsInFirstLine == 0 && resolveOffset > 0)
                    ? 10 + resolveOffset
                    : 10 + 18 * slotsInFirstLine + resolveOffset;

                int panelH = 10 + 18 * effectiveLines;
                EmiRenderHelper.drawNinePatch(context, TEXTURE,
                    panelX - 5, panelY - 5, panelW, panelH, 58, 0, 5, 1);
            }
        }
    }


    @Redirect(
        method = "render",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Ldev/emi/emi/screen/RecipeScreen;getMaxWorkstations()I",
            remap = false 
        )
    )
    private int emi$suppressVanillaPanel(RecipeScreen self) {
        return 0;
    }
}