package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;

import java.util.function.Function;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class UIStructureOverlayPanel extends UIStringOverlayPanel
{
    public UIStructureOverlayPanel(IKey title, Consumer<Link> callback)
    {
        super(title, true, getStructureFiles(), (str) ->
        {
            if (callback != null)
            {
                callback.accept(str.isEmpty() ? null : Link.create(str));
            }
        }, displayFormatter());
    }

    private static Function<String, String> displayFormatter()
    {
        return (s) ->
        {
            if (s == null || s.isEmpty()) return s;

            // Mostrar solo el nombre de archivo sin prefijo ni extensión
            int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
            String name = slash >= 0 ? s.substring(slash + 1) : s;
            if (name.toLowerCase().endsWith(".nbt"))
            {
                name = name.substring(0, name.length() - 4);
            }
            // Añadir prefijo visual "structures/" para dar contexto
            return "structures/" + name;
        };
    }

    private static Set<String> getStructureFiles()
    {
        Set<String> locations = new HashSet<>();

        try
        {
            for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("structures")))
            {
                String pathLower = link.path.toLowerCase();

                if (pathLower.endsWith(".nbt"))
                {
                    locations.add(link.toString());
                }
            }
        }
        catch (Exception e)
        {
            // Si no hay carpeta structures o hay algún error, devolver conjunto vacío
        }

        return locations;
    }
}