package mchorse.bbs_mod.ui.forms.categories;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.DataStringifier;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.categories.UserFormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.forms.sections.UserFormSection;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UIFormCategory extends UIElement
{
    public static final int HEADER_HEIGHT = 20;
    public static final int CELL_WIDTH = 60;
    public static final int CELL_HEIGHT = 80;

    public UIFormList list;
    public FormCategory category;
    public Form selected;

    private int last;
    private String search = "";
    private List<Form> searched = new ArrayList<>();

    public UIFormCategory(FormCategory category, UIFormList list)
    {
        this.category = category;
        this.list = list;

        this.context((menu) ->
        {
            FormCategories formCategories = BBSModClient.getFormCategories();
            UserFormSection userForms = formCategories.getUserForms();

            menu.action(Icons.EDIT, UIKeys.GENERAL_EDIT, () ->
            {
                this.list.palette.toggleEditor();
            });

            if (this.selected instanceof ModelForm)
            {
                menu.action(Icons.FOLDER, UIKeys.FORMS_CATEGORIES_CONTEXT_OPEN_MODEL_FOLDER, () ->
                {
                    ModelForm form = (ModelForm) this.selected;

                    UIUtils.openFolder(BBSMod.getAssetsPath(ModelManager.MODELS_PREFIX + form.model.get() + "/"));
                });
            }

            /* Guardar estructura: aparece sólo cuando la forma seleccionada es StructureForm
             * y el archivo proviene de la carpeta del mundo (generated/.../structures). */
            if (this.selected instanceof StructureForm)
            {
                StructureForm s = (StructureForm) this.selected;
                String relPath = s.structureFile.get();

                if (relPath != null && !relPath.isEmpty())
                {
                    try
                    {
                        Link link = Link.assets(relPath);
                        File src = BBSMod.getProvider().getFile(link);
                        File world = BBSMod.getWorldFolder();

                        if (src != null && world != null)
                        {
                            String base1 = new File(world, "generated/minecraft/structures").getAbsolutePath();
                            String base2 = new File(world, "generated/structures").getAbsolutePath();
                            String abs = src.getAbsolutePath();

                            boolean isWorldStructure = abs.startsWith(base1) || abs.startsWith(base2);

                            if (isWorldStructure)
                            {
                                menu.action(Icons.DOWNLOAD, IKey.raw("Guardar estructura"), () ->
                                {
                                    try
                                    {
                                        File assetsStructures = new File(BBSMod.getAssetsFolder(), "structures");
                                        if (!assetsStructures.exists()) assetsStructures.mkdirs();

                                        String sub = relPath.startsWith("structures/") ? relPath.substring("structures/".length()) : relPath;
                                        File dst = new File(assetsStructures, sub);

                                        /* Asegurar subcarpetas */
                                        File parent = dst.getParentFile();
                                        if (parent != null && !parent.exists()) parent.mkdirs();

                                        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);

                                        System.out.println("[BBS] Estructura guardada: " + src.getAbsolutePath() + " -> " + dst.getAbsolutePath());

                                        /* Forzar refresco de la lista de formas */
                                        BBSModClient.getFormCategories().markDirty();
                                    }
                                    catch (Exception e)
                                    {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        }
                    }
                    catch (Exception ignored)
                    {}
                }
            }

            menu.action(Icons.ADD, UIKeys.FORMS_CATEGORIES_CONTEXT_ADD_CATEGORY, () ->
            {
                UIOverlay.addOverlay(this.getContext(), new UIPromptOverlayPanel(
                    UIKeys.FORMS_CATEGORIES_ADD_CATEGORY_TITLE,
                    UIKeys.FORMS_CATEGORIES_ADD_CATEGORY_DESCRIPTION,
                    (str) ->
                    {
                        userForms.addUserCategory(new UserFormCategory(IKey.constant(str), formCategories.visibility.get(UUID.randomUUID().toString()), userForms));
                        list.setupForms(formCategories);
                    }
                ));
            });

            if (this.selected != null)
            {
                menu.action(Icons.COPY, UIKeys.FORMS_CATEGORIES_CONTEXT_COPY_FORM, () -> Window.setClipboard(FormUtils.toData(this.selected)));
                menu.action(Icons.COPY, UIKeys.FORMS_CATEGORIES_CONTEXT_COPY_TO_CATEGORY, () ->
                {
                    this.getContext().replaceContextMenu((m) ->
                    {
                        for (UserFormCategory formCategory : userForms.categories)
                        {
                            if (formCategory == this.category)
                            {
                                continue;
                            }

                            m.action(Icons.ADD, UIKeys.FORMS_CATEGORIES_CONTEXT_COPY_TO.format(formCategory.getProcessedTitle()), () ->
                            {
                                formCategory.addForm(FormUtils.copy(this.selected));
                            });
                        }
                    });
                });
                menu.action(Icons.COPY, UIKeys.FORMS_CATEGORIES_CONTEXT_COPY_COMMAND, () ->
                {
                    MapType data = FormUtils.toData(this.selected);
                    DataStringifier stringifier = new DataStringifier();
                    String name = MinecraftClient.getInstance().player.getGameProfile().getName();

                    stringifier.jsonLike();
                    stringifier.indent = "";

                    Window.setClipboard("/bbs morph " + name + " " + stringifier.toString(data));
                });

                Collection<PlayerListEntry> playerList = MinecraftClient.getInstance().getNetworkHandler().getPlayerList();

                if (playerList.size() > 1)
                {
                    menu.action(Icons.ARROW_RIGHT, UIKeys.FORMS_CATEGORIES_CONTEXT_SHARE_FORM, () ->
                    {
                        this.getContext().replaceContextMenu((newMenu) ->
                        {
                            for (PlayerListEntry entry : playerList)
                            {
                                if (entry.getProfile().getId().equals(MinecraftClient.getInstance().player.getGameProfile().getId()))
                                {
                                    continue;
                                }

                                newMenu.action(Icons.ARROW_RIGHT, IKey.constant(entry.getProfile().getName()), () ->
                                {
                                    ClientNetwork.sendSharedForm(this.selected, entry.getProfile().getId());
                                });
                            }
                        });
                    });
                }
            }
        });

        this.h(20);
    }

    public void search(String search)
    {
        this.search = search.toLowerCase();

        this.searched.clear();

        if (search.isEmpty())
        {
            return;
        }

        for (Form form : this.category.getForms())
        {
            if (form.getFormId().toLowerCase().contains(search) || form.getDisplayName().toLowerCase().contains(search))
            {
                this.searched.add(form);
            }
        }
    }

    public List<Form> getForms()
    {
        if (this.search.isEmpty())
        {
            return this.category.getForms();
        }

        return this.searched;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context))
        {
            int x = context.mouseX - this.area.x;
            int y = context.mouseY - this.area.y - HEADER_HEIGHT;
            int perRow = this.area.w / CELL_WIDTH;

            if (y < 0)
            {
                if (x < this.area.x + 30 + context.batcher.getFont().getWidth(this.category.title.get()))
                {
                    this.category.visible.set(!this.category.visible.get());

                    return true;
                }
                else
                {
                    return super.subMouseClicked(context);
                }
            }

            x /= CELL_WIDTH;
            y /= CELL_HEIGHT;

            List<Form> forms = this.getForms();
            int i = x + y * perRow;

            if (i >= 0 && i < forms.size())
            {
                this.select(forms.get(i), true);
            }
            else
            {
                this.select(null, true);
            }
        }

        return super.subMouseClicked(context);
    }

    public void select(Form form, boolean notify)
    {
        if (this.list != null)
        {
            this.list.selectCategory(this, form, notify);
        }

        this.selected = form;
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        context.batcher.textCard(this.category.getProcessedTitle(), this.area.x + 26, this.area.y + 6);

        if (this.category.visible.get())
        {
            context.batcher.icon(Icons.MOVE_DOWN, this.area.x + 16, this.area.y + 5, 0.5F, 0F);
        }
        else
        {
            context.batcher.icon(Icons.MOVE_UP, this.area.x + 16, this.area.y + 4, 0.5F, 0F);
        }

        List<Form> forms = this.getForms();
        int h = HEADER_HEIGHT;
        int x = 0;
        int i = 0;
        int perRow = this.area.w / CELL_WIDTH;

        if (!forms.isEmpty() && this.category.visible.get())
        {
            for (Form form : forms)
            {
                if (i == perRow)
                {
                    h += CELL_HEIGHT;
                    x = 0;
                    i = 0;
                }

                int cx = this.area.x + x;
                int cy = this.area.y + h;
                boolean isSelected = this.selected == form;

                context.batcher.clip(cx, cy, CELL_WIDTH, CELL_HEIGHT, context);

                if (isSelected)
                {
                    context.batcher.box(cx, cy, cx + CELL_WIDTH, cy + CELL_HEIGHT, Colors.A50 | BBSSettings.primaryColor.get());
                    context.batcher.outline(cx, cy, cx + CELL_WIDTH, cy + CELL_HEIGHT, Colors.A50 | BBSSettings.primaryColor.get(), 2);
                }

                FormUtilsClient.renderUI(form, context, cx, cy, cx + CELL_WIDTH, cy + CELL_HEIGHT);

                context.batcher.unclip(context);

                x += CELL_WIDTH;
                i += 1;
            }

            h += CELL_HEIGHT;
        }

        if (this.last != h)
        {
            this.last = h;

            UIElement container = this.getParentContainer();

            if (container != null)
            {
                this.h(h);
                container.resize();
            }
        }
    }
}