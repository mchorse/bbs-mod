package mchorse.bbs_mod.ui.selectors;

import com.mojang.brigadier.StringReader;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.selectors.EntitySelector;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.UINestedEdit;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.TextLine;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.Identifier;

import java.util.List;

public class UISelectorsOverlayPanel extends UIOverlayPanel
{
    public UISelectorList selectors;

    public UIElement column;
    public UIToggle enabled;
    public UINestedEdit form;
    public UITextbox entity;
    public UITextbox name;
    public UITextarea<TextLine> nbt;

    private EntitySelector current;

    public UISelectorsOverlayPanel()
    {
        super(UIKeys.SELECTORS_TITLE);

        this.selectors = new UISelectorList((l) -> this.setSelector(l.get(0), false));
        this.selectors.setList(BBSModClient.getSelectors().selectors);
        this.selectors.update();

        this.enabled = new UIToggle(UIKeys.CAMERA_PANELS_ENABLED, (b) ->
        {
            this.current.enabled = b.getValue();

            BBSModClient.getSelectors().update();
        });

        this.form = new UINestedEdit((editing) ->
        {
            UIFormPalette.open(this.getParent(UIOverlay.class), editing, this.current.form, true, (form) ->
            {
                this.current.form = FormUtils.copy(form);

                BBSModClient.getSelectors().update();
            });
        });
        this.entity = new UITextbox(100, (t) ->
        {
            String id = t.trim();

            try
            {
                this.current.entity = id.isEmpty() ? null : new Identifier(id);
            }
            catch (Exception e)
            {
                this.current.entity = null;
            }

            BBSModClient.getSelectors().update();
        });
        this.name = new UITextbox(100, (t) ->
        {
            this.current.name = t;

            BBSModClient.getSelectors().update();
        });
        this.nbt = new UITextarea<>((t) ->
        {
            try
            {
                if (t.trim().isEmpty())
                {
                    this.current.nbt = null;
                }
                else
                {
                    this.current.nbt = (new StringNbtReader(new StringReader(t))).parseCompound();
                }

                BBSModClient.getSelectors().update();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });

        this.nbt.background().wrap().h(80);

        this.selectors.context((menu) ->
        {
            menu.action(Icons.ADD, UIKeys.SELECTORS_CONTEXT_ADD, () ->
            {
                EntitySelector element = new EntitySelector();

                this.selectors.add(element);
                this.setSelector(element, true);
                BBSModClient.getSelectors().update();
            });

            if (this.current != null)
            {
                menu.action(Icons.REMOVE, UIKeys.SELECTORS_CONTEXT_REMOVE, () ->
                {
                    List<EntitySelector> list = this.selectors.getList();

                    list.remove(this.current);
                    this.setSelector(list.isEmpty() ? null : list.get(0), true);
                    BBSModClient.getSelectors().update();
                });
            }
        });

        this.column = UI.column(5, 10,
            this.enabled,
            this.form,
            UI.label(UIKeys.SELECTORS_ENTITY_ID).marginTop(6),
            this.entity,
            UI.label(UIKeys.SELECTORS_NAME_TAG).marginTop(6),
            this.name,
            UI.label(UIKeys.SELECTORS_NBT).marginTop(6),
            this.nbt
        );

        this.selectors.relative(this.content).w(1F).hTo(this.column.area);
        this.column.relative(this.content).y(1F).w(1F).anchor(0F, 1F);

        this.add(this.column, this.selectors);
        this.onClose((e) -> BBSModClient.getSelectors().save());

        this.setSelector(this.selectors.getList().isEmpty() ? null : this.selectors.getList().get(0), true);
    }

    private void setSelector(EntitySelector selector, boolean select)
    {
        this.current = selector;

        this.column.setVisible(selector != null);

        if (selector != null)
        {
            this.enabled.setValue(selector.enabled);
            this.form.setForm(selector.form);
            this.entity.setText(selector.entity == null ? "" : selector.entity.toString());
            this.name.setText(selector.name);
            this.nbt.setText(selector.nbt == null ? "" : selector.nbt.toString());
        }

        if (select)
        {
            this.selectors.setCurrentScroll(selector);
        }
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        this.content.area.render(context.batcher, Colors.A100);
    }
}