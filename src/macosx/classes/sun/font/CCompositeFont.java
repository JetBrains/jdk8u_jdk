package sun.font;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public final class CCompositeFont extends CompositeFont {
    private final List<CFont> fallbackFonts = new ArrayList<>();

    public CCompositeFont(CFont font) {
        super(new PhysicalFont[]{font});
        mapper = new CCompositeGlyphMapper(this);
    }

    @Override
    public synchronized int getNumSlots() {
        return super.getNumSlots();
    }

    @Override
    public PhysicalFont getSlotFont(int slot) {
        if (slot == 0) return super.getSlotFont(0);
        synchronized (this) {
            return fallbackFonts.get(slot - 1);
        }
    }

    @Override
    synchronized FontStrike getStrike(FontStrikeDesc desc, boolean copy) {
        return super.getStrike(desc, copy);
    }

    @Override
    synchronized void removeFromCache(FontStrikeDesc desc) {
        super.removeFromCache(desc);
    }

    @Override
    protected synchronized int getValidatedGlyphCode(int glyphCode) {
        return super.getValidatedGlyphCode(glyphCode);
    }

    @Override
    public boolean hasSupplementaryChars() {
        return false;
    }

    @Override
    public boolean useAAForPtSize(int ptsize) {
        return true;
    }

    public synchronized int findSlot(String fontName) {
        for (int slot = 0; slot < numSlots; slot++) {
            PhysicalFont slotFont = getSlotFont(slot);
            if (fontName.equals(slotFont.getPostscriptName())) {
                return slot;
            }
        }
        return -1;
    }

    public synchronized int addSlot(CFont font) {
        int slot = findSlot(font.getPostscriptName());
        if (slot >= 0) return slot;
        fallbackFonts.add(font);
        lastFontStrike = new SoftReference<>(null);
        strikeCache.clear();
        return numSlots++;
    }
}
