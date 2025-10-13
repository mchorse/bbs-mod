package mchorse.bbs_mod.forms.sections;

import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.utils.watchdog.IWatchDogListener;
import mchorse.bbs_mod.utils.watchdog.WatchDogEvent;

import java.nio.file.Path;
import java.util.List;

public abstract class FormSection implements IWatchDogListener
{
    protected FormCategories parent;

    public FormSection(FormCategories parent)
    {
        this.parent = parent;
    }

    public abstract void initiate();

    public abstract List<FormCategory> getCategories();

    @Override
    public void accept(Path path, WatchDogEvent event)
    {}
}