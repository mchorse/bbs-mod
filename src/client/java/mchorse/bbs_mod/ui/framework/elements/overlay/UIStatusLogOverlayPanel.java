package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;
import java.util.function.Consumer;

public class UIStatusLogOverlayPanel extends UIOverlayPanel
{
    public UIStatusList list;

    public UIStatusLogOverlayPanel(IKey title)
    {
        super(title);

        this.list = new UIStatusList(null);
        this.list.full(this.content);

        this.content.add(this.list);
    }

    public static class UIStatusList extends UIList<Pair<Integer, IKey>>
    {
        public UIStatusList(Consumer<List<Pair<Integer, IKey>>> callback)
        {
            super(callback);

            this.scroll.scrollItemSize = 16;
        }

        @Override
        protected void renderElementPart(UIContext context, Pair<Integer, IKey> element, int i, int x, int y, boolean hover, boolean selected)
        {
            int h = this.scroll.scrollItemSize;

            context.batcher.box(x, y, x + 2, y + h, Colors.A100 | element.a);
            context.batcher.gradientHBox(x + 2, y, x + 24, y + h, Colors.A25 | element.a, element.a);

            super.renderElementPart(context, element, i, x + 2, y, hover, selected);
        }

        @Override
        protected String elementToString(UIContext context, int i, Pair<Integer, IKey> element)
        {
            return element.b.get();
        }
    }
}