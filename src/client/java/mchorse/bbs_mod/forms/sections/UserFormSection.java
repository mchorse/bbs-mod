package mchorse.bbs_mod.forms.sections;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.categories.RecentFormCategory;
import mchorse.bbs_mod.forms.categories.UserFormCategory;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.utils.watchdog.WatchDogEvent;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserFormSection extends FormSection
{
    public List<UserFormCategory> categories = new ArrayList<>();

    public static File getUserCategoriesFile(int index)
    {
        return BBSMod.getSettingsPath("forms/" + index + ".json");
    }

    public UserFormSection(FormCategories parent)
    {
        super(parent);
    }

    @Override
    public void initiate()
    {
        this.categories.clear();

        File folder = getUserCategoriesFile(0).getParentFile();

        if (folder.isDirectory())
        {
            this.loadNewCategories();
        }
    }

    private void loadNewCategories()
    {
        /* Just in case 420 categories, because it's blazing */
        for (int i = 0; i < 420; i++)
        {
            File file = getUserCategoriesFile(i);

            if (!file.isFile())
            {
                break;
            }

            UserFormCategory category = new UserFormCategory(IKey.EMPTY, this.parent.visibility.get(UUID.randomUUID().toString()), this);

            try
            {
                MapType data = (MapType) DataToString.read(file);

                category.fromData(data);
                this.categories.add(category);
            }
            catch (Exception e)
            {
                System.err.println("Failed to load user form category: " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<FormCategory> getCategories()
    {
        List<FormCategory> categoryList = new ArrayList<>();

        for (UserFormCategory category : this.categories)
        {
            categoryList.add(category);
        }

        return categoryList;
    }

    public void writeUserCategories()
    {
        for (int i = 0; i < this.categories.size(); i++)
        {
            this.writeUserCategory(i, this.categories.get(i));
        }
    }

    public void writeUserCategories(UserFormCategory formCategory)
    {
        int index = this.categories.indexOf(formCategory);

        if (formCategory != null)
        {
            this.writeUserCategory(index, formCategory);
        }
    }

    private void writeUserCategory(int index, FormCategory category)
    {
        File file = getUserCategoriesFile(index);

        file.getParentFile().mkdirs();

        try
        {
            DataToString.write(file, category.toData(), true);
        }
        catch (Exception e)
        {
            System.err.println("Failed to save user category: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    public void addUserCategory(UserFormCategory category)
    {
        int index = 0;

        for (FormCategory formCat : this.categories)
        {
            if (formCat instanceof RecentFormCategory || formCat instanceof UserFormCategory)
            {
                index += 1;
            }
            else
            {
                break;
            }
        }

        this.categories.add(index, category);
        this.parent.markDirty();
        this.writeUserCategories();
    }

    public void removeUserCategory(UserFormCategory category)
    {
        File lastFile = getUserCategoriesFile(this.categories.size() - 1);

        if (lastFile.isFile())
        {
            lastFile.delete();
        }

        this.categories.remove(category);
        this.parent.markDirty();
        this.parent.visibility.remove(category.visible.getId());

        this.writeUserCategories();
    }
}