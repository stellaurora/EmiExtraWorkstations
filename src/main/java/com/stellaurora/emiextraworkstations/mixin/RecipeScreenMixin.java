package com.stellaurora.emiextraworkstations.mixin;

import com.stellaurora.emiextraworkstations.Config;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RecipeScreen.class, remap = false)
public abstract class RecipeScreenMixin {

    @Shadow int backgroundHeight;
    @Shadow int backgroundWidth;
    @Shadow int x;
    @Shadow int y;
    @Shadow @Final private static ResourceLocation TEXTURE;
    @Shadow private List<WidgetGroup> currentPage;
    @Shadow private int tabPage;
    @Shadow private int tab;
    @Shadow private int page;

    @Shadow public abstract int getResolveOffset();
    @Shadow public abstract EmiRecipeCategory getFocusedCategory();
    @Shadow public abstract void setPage(int tp, int t, int p);

    private int emi$scrollLineOffset = 0;
    private boolean emi$preserveScrollOnSetPage = false;
    
    private int emi$getSlotsPerLine() {
        return switch (EmiConfig.workstationLocation) {
            case LEFT, RIGHT -> Math.max(1, (backgroundHeight - getResolveOffset() - 18) / 18);
            case BOTTOM      -> Math.max(1, (backgroundWidth  - getResolveOffset() - 18) / 18);
            default          -> 1;
        };
    }

    private int emi$getMaxLines() {
        return switch (EmiConfig.workstationLocation) {
            case LEFT, RIGHT -> Math.max(1, Config.MAX_COLUMNS.get());
            case BOTTOM      -> Math.max(1, Config.MAX_ROWS.get());
            default          -> 1;
        };
    }

    private int emi$getPageSize() {
        return emi$getSlotsPerLine() * emi$getMaxLines();
    }

    private int emi$getVisibleStartIndex() {
        return emi$scrollLineOffset * emi$getSlotsPerLine();
    }

    private int emi$getVisibleCount(int total) {
        int start = emi$getVisibleStartIndex();
        if (start >= total) {
            return 0;
        }
        return Math.min(emi$getPageSize(), total - start);
    }

    private int emi$getMaxScrollLines(int total) {
        int slotsPerLine = emi$getSlotsPerLine();
        int totalLines = total == 0 ? 0 : (int) Math.ceil((double) total / slotsPerLine);
        return Math.max(0, totalLines - emi$getMaxLines());
    }

    private void emi$clampScroll(int total) {
        int max = emi$getMaxScrollLines(total);
        if (emi$scrollLineOffset < 0) {
            emi$scrollLineOffset = 0;
        } else if (emi$scrollLineOffset > max) {
            emi$scrollLineOffset = max;
        }
    }

    private int emi$getLineCount(int total) {
        if (total == 0) return 0;
        return Math.min(
            (int) Math.ceil((double) total / emi$getSlotsPerLine()),
            emi$getMaxLines()
        );
    }

    private Bounds emi$slotBounds(int i) {
        int slotsPerLine  = emi$getSlotsPerLine();
        int line          = i / slotsPerLine;
        int slot          = i % slotsPerLine;
        int resolveOffset = getResolveOffset();

        return switch (EmiConfig.workstationLocation) {
            case LEFT   -> new Bounds(x - 18 - line * 18,                y + 9 + resolveOffset + slot * 18, 18, 18);
            case RIGHT  -> new Bounds(x + backgroundWidth + line * 18,   y + 9 + resolveOffset + slot * 18, 18, 18);
            case BOTTOM -> new Bounds(x + 5 + resolveOffset + slot * 18, y + backgroundHeight - 23 + line * 18, 18, 18);
            default     -> Bounds.EMPTY;
        };
    }

    private boolean emi$isInsideRect(double px, double py, int rx, int ry, int rw, int rh) {
        return px >= rx && px < rx + rw && py >= ry && py < ry + rh;
    }

    private boolean emi$isMouseOverWorkstationPanel(double mouseX, double mouseY, int total) {
        int visibleTotal  = emi$getVisibleCount(total);
        int slotsPerLine  = emi$getSlotsPerLine();
        int lineCount     = emi$getLineCount(visibleTotal);
        int resolveOffset = getResolveOffset();
        int slotsInFirstLine = Math.min(slotsPerLine, visibleTotal);

        boolean hasContent = visibleTotal > 0 || RecipeScreen.resolve != null;
        if (!hasContent) {
            return false;
        }

        int effectiveLines = Math.max(lineCount, 1);

        int panelX;
        int panelY;
        int panelW;
        int panelH;

        switch (EmiConfig.workstationLocation) {
            case LEFT -> {
                panelX = x - 18 - (effectiveLines - 1) * 18;
                panelY = y + 9 - resolveOffset;
                panelW = 10 + 18 * effectiveLines;
                panelH = (slotsInFirstLine == 0 && resolveOffset > 0)
                    ? 10 + resolveOffset
                    : 10 + 18 * slotsInFirstLine + resolveOffset;
            }
            case RIGHT -> {
                panelX = x + backgroundWidth;
                panelY = y + 9 - resolveOffset;
                panelW = 10 + 18 * effectiveLines;
                panelH = (slotsInFirstLine == 0 && resolveOffset > 0)
                    ? 10 + resolveOffset
                    : 10 + 18 * slotsInFirstLine + resolveOffset;
            }
            case BOTTOM -> {
                panelX = x + 5 - resolveOffset;
                panelY = y + backgroundHeight - 23;
                panelW = (slotsInFirstLine == 0 && resolveOffset > 0)
                    ? 10 + resolveOffset
                    : 10 + 18 * slotsInFirstLine + resolveOffset;
                panelH = 10 + 18 * effectiveLines;
            }
            default -> {
                return false;
            }
        }

        return emi$isInsideRect(mouseX, mouseY, panelX - 5, panelY - 5, panelW, panelH);
    }

    @Redirect(
        method = "setPage",
        at = @At(
            value = "INVOKE",
            target = "Ldev/emi/emi/screen/RecipeScreen;getMaxWorkstations()I"
        )
    )
    private int emi$disableVanillaWorkstationsSetPage(RecipeScreen self) {
        return 0;
    }

    @Inject(method = "setPage", at = @At("HEAD"))
    private void emi$resetWorkstationScroll(CallbackInfo ci) {
        if (!emi$preserveScrollOnSetPage) {
            emi$scrollLineOffset = 0;
        }
        emi$preserveScrollOnSetPage = false;
    }

    @Inject(method = "setPage", at = @At("TAIL"))
    private void emi$appendScrolledWorkstations(CallbackInfo ci) {
        List<EmiIngredient> workstations = EmiApi.getRecipeManager().getWorkstations(getFocusedCategory());
        int total = workstations == null ? 0 : workstations.size();
        if (total <= 0) {
            return;
        }

        emi$clampScroll(total);
        int start = emi$getVisibleStartIndex();
        int visibleCount = emi$getVisibleCount(total);
        if (visibleCount <= 0) {
            return;
        }

        WidgetGroup widgets = new WidgetGroup(null, 0, 0, 0, 0);
        for (int i = 0; i < visibleCount; i++) {
            int actualIndex = start + i;
            Bounds bounds = emi$slotBounds(i);
            widgets.add(new SlotWidget(workstations.get(actualIndex), bounds.x(), bounds.y()));
        }
        currentPage.add(widgets);
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
        emi$clampScroll(total);
        int visibleTotal  = emi$getVisibleCount(total);
        int slotsPerLine  = emi$getSlotsPerLine();
        int lineCount     = emi$getLineCount(visibleTotal);
        int resolveOffset = getResolveOffset();

        int slotsInFirstLine = Math.min(slotsPerLine, visibleTotal);

        boolean hasContent = visibleTotal > 0 || RecipeScreen.resolve != null;
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

    @Inject(method = "mouseScrolled", remap = true, at = @At("HEAD"), cancellable = true)
    private void emi$scrollWorkstations(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        List<EmiIngredient> workstations = EmiApi.getRecipeManager().getWorkstations(getFocusedCategory());
        int total = workstations == null ? 0 : workstations.size();
        emi$clampScroll(total);

        if (!emi$isMouseOverWorkstationPanel(mouseX, mouseY, total)) {
            return;
        }

        int maxScrollLines = emi$getMaxScrollLines(total);
        if (maxScrollLines <= 0) {
            return;
        }

        if (amount == 0.0D) {
            return;
        }

        int before = emi$scrollLineOffset;
        if (amount < 0.0D) {
            emi$scrollLineOffset = Math.min(maxScrollLines, emi$scrollLineOffset + 1);
        } else if (amount > 0.0D) {
            emi$scrollLineOffset = Math.max(0, emi$scrollLineOffset - 1);
        }

        if (emi$scrollLineOffset != before) {
            cir.setReturnValue(true);
            emi$preserveScrollOnSetPage = true;
            setPage(tabPage, tab, page);
        }
    }
}