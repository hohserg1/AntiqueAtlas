package hunternif.mc.atlas.recipe;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.item.ItemAriadneThread;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nonnull;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ARIADNE_THREAD;

public class RecipeAriadneThreadPathClearing extends ShapelessOreRecipe {
    public RecipeAriadneThreadPathClearing() {
        super(new ResourceLocation(AntiqueAtlasMod.ID, "atlas"), new ItemStack(ARIADNE_THREAD), ARIADNE_THREAD);
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == ARIADNE_THREAD) {
                ItemStack r = stack.copy();
                r.setCount(1);
                ItemAriadneThread.clearPath(r);
                return r;
            }
        }
        return ItemStack.EMPTY;
    }
}
