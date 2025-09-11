package mchorse.bbs_mod.ui.utils.presets;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.presets.DataManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIDataContextMenu extends UIContextMenu
{
    public UIElement row;
    public UIIcon copy;
    public UIIcon paste;
    public UIIcon reset;
    public UIIcon save;

    public UISearchList<String> entries;

    private DataManager manager;
    private String group;
    private MapType data;
    private Supplier<MapType> supplier;
    private Consumer<MapType> callback;
    private String copyGroup = "_CopyPose";

    public UIDataContextMenu(DataManager manager, String group, Supplier<MapType> supplier, Consumer<MapType> callback)
    {
        this.manager = manager;
        this.group = group;
        this.supplier = supplier;
        this.callback = callback;
        this.data = this.manager.getData(group);

        this.copy = new UIIcon(Icons.COPY, (b) -> Window.setClipboard(this.supplier.get(), this.copyGroup));
        this.copy.tooltip(UIKeys.POSE_CONTEXT_COPY);
        this.paste = new UIIcon(Icons.PASTE, (b) ->
        {
            MapType data = Window.getClipboardMap(this.copyGroup);

            if (data != null)
            {
                this.send(data);
            }
        });
        this.paste.tooltip(UIKeys.POSE_CONTEXT_PASTE);
        this.reset = new UIIcon(Icons.REFRESH, (b) -> this.send(new MapType()));
        this.reset.tooltip(UIKeys.POSE_CONTEXT_RESET);
        this.save = new UIIcon(Icons.SAVED, (b) ->
        {
            String name = this.entries.search.getText();

            if (!name.isEmpty())
            {
                this.manager.saveData(this.group, name, this.supplier.get());

                this.data = this.manager.getData(group);

                this.fillPoses();
                this.entries.search.setText("");
            }
        });
        this.save.tooltip(UIKeys.POSE_CONTEXT_SAVE);

        this.entries = new UISearchList<>(new UIStringList((l) -> this.send(this.data.getMap(l.get(0)))));
        this.entries.search.filename();
        this.entries.search.placeholder(UIKeys.POSE_CONTEXT_NAME);

        this.row = UI.row(this.copy, this.paste, this.reset, this.save);

        this.row.relative(this).xy(5, 5).w(1F, -10).h(20);
        this.entries.relative(this).xy(5, 25).w(1F, -10).hTo(this.area, 1F, -5);

        this.add(this.row);
        this.add(this.entries);

        this.fillPoses();
    }

    public UIDataContextMenu tooltips(String copyGroup, IKey copy, IKey paste, IKey reset, IKey save, IKey name)
    {
        this.copyGroup = copyGroup;
        this.copy.tooltip(copy);
        this.paste.tooltip(paste);
        this.reset.tooltip(reset);
        this.save.tooltip(save);
        this.entries.search.placeholder(name);

        return this;
    }

    private void send(MapType map)
    {
        if (this.callback != null)
        {
            this.callback.accept(map);
        }
    }

    private void fillPoses()
    {
        this.entries.list.clear();
        this.entries.list.add(this.data.keys());
        this.entries.list.sort();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void setMouse(UIContext context)
    {
        /* Padding from both side + 4 icon 20px + 3 margin 5px */
        int size = this.row.getChildren().size();

        int i = size * 20 + (size - 1) * 5;

        this.xy(context.mouseX(), context.mouseY()).w(10 + i).h(10 + 40 + UIStringList.DEFAULT_HEIGHT * 12).bounds(context.menu.overlay, 5);
    }
}