package mchorse.bbs_mod.settings.ui;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

public class UISettingsOverlayPanel extends UIOverlayPanel
{
    public UIScrollView options;

    private Settings settings;
    private UIIcon currentButton;

    public UISettingsOverlayPanel()
    {
        super(UIKeys.CONFIG_TITLE);

        this.options = new UIScrollView(ScrollDirection.VERTICAL);
        this.options.scroll.scrollSpeed = 51;

        this.options.full(this.content);
        this.options.column().scroll().vertical().stretch().padding(10).height(20);

        for (Settings settings : BBSMod.getSettings().modules.values())
        {
            UIIcon icon = new UIIcon(settings.icon, (b) ->
            {
                this.selectConfig(settings.getId(), b);
            });

            icon.tooltip(L10n.lang(UIValueFactory.getTitleKey(settings)), Direction.LEFT);
            this.icons.add(icon);
        }

        this.add(this.options);
        this.selectConfig("bbs", this.icons.getChildren(UIIcon.class).get(1));
        this.markContainer();
    }

    public void selectConfig(String mod, UIIcon currentButton)
    {
        this.settings = BBSMod.getSettings().modules.get(mod);
        this.currentButton = currentButton;

        this.refresh();
    }

    public void refresh()
    {
        if (this.settings == null)
        {
            return;
        }

        this.options.removeAll();

        boolean first = true;

        for (ValueGroup category : this.settings.categories.values())
        {
            if (!category.isVisible())
            {
                continue;
            }

            String catTitleKey = UIValueFactory.getCategoryTitleKey(category);
            String catTooltipKey = UIValueFactory.getCategoryTooltipKey(category);

            UILabel label = UI.label(L10n.lang(catTitleKey)).labelAnchor(0, 1).background(() -> BBSSettings.primaryColor(Colors.A50));
            List<UIElement> options = new ArrayList<>();

            label.tooltip(L10n.lang(catTooltipKey), Direction.BOTTOM);
            this.options.add(label);

            for (BaseValue value : category.getAll())
            {
                if (!value.isVisible())
                {
                    continue;
                }

                List<UIElement> elements = UIValueMap.create(value, this);

                for (UIElement element : elements)
                {
                    options.add(element);
                }
            }

            UIElement firstContainer = UI.column(5, 0, 20, label, options.remove(0)).marginTop(first ? 0 : 24);

            this.options.add(firstContainer);

            for (UIElement element : options)
            {
                this.options.add(element);
            }

            first = false;
        }

        this.resize();
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        if (this.currentButton != null)
        {
            this.currentButton.area.render(context.batcher, BBSSettings.primaryColor(Colors.A100));
        }
    }
}