package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.function.Function;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UIStructureOverlayPanel extends UIStringOverlayPanel
{
    private UIIcon saveButton;
    private String currentSelectedStructure = "";

    public UIStructureOverlayPanel(IKey title, Consumer<Link> callback)
    {
        super(title, true, getStructureFiles(), (str) ->
        {
            if (callback != null)
            {
                callback.accept(str.isEmpty() ? null : Link.create(str));
            }
        }, displayFormatter());

        // Agregar botón de guardar estructura
        this.saveButton = new UIIcon(Icons.DOWNLOAD, (b) -> this.saveStructure());
        this.saveButton.tooltip(IKey.raw("Guardar estructura"));
        this.icons.add(this.saveButton); // Agregar al contenedor de iconos

        this.updateSaveButtonVisibility();
    }

    @Override
    protected void accept(String string)
    {
        this.currentSelectedStructure = string;
        this.updateSaveButtonVisibility();
        super.accept(string);
    }

    private void updateSaveButtonVisibility()
    {
        boolean isWorldStructure = !this.currentSelectedStructure.isEmpty() && 
                                   this.isFromWorld(this.currentSelectedStructure);
        this.saveButton.setVisible(isWorldStructure);
    }

    private void saveStructure()
    {
        if (this.currentSelectedStructure.isEmpty()) {
            return;
        }

        try {
            Link sourceLink = Link.create(this.currentSelectedStructure);
            File sourceFile = BBSMod.getProvider().getFile(sourceLink);
            
            if (sourceFile == null || !sourceFile.exists()) {
                System.err.println("[BBS] No se pudo encontrar el archivo fuente: " + this.currentSelectedStructure);
                return;
            }

            // Obtener el nombre del archivo sin la ruta
            String fileName = sourceLink.path;
            if (fileName.startsWith("structures/")) {
                fileName = fileName.substring("structures/".length());
            }

            // Crear la carpeta de destino si no existe
            File assetsStructuresDir = new File(BBSMod.getAssetsFolder(), "structures");
            if (!assetsStructuresDir.exists()) {
                assetsStructuresDir.mkdirs();
            }

            // Archivo de destino
            File destFile = new File(assetsStructuresDir, fileName);

            // Copiar el archivo
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[BBS] Estructura guardada: " + sourceFile.getAbsolutePath() + " -> " + destFile.getAbsolutePath());

            // Refrescar la lista de estructuras
            this.strings.list.clear();
            this.strings.list.add(getStructureFiles());
            this.strings.list.sort();
            this.strings.list.update();

        } catch (IOException e) {
            System.err.println("[BBS] Error al guardar estructura: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isFromWorld(String linkString)
    {
        if (linkString == null || linkString.isEmpty()) {
            return false;
        }

        try {
            Link link = Link.create(linkString);
            File worldFolder = BBSMod.getWorldFolder();
            
            if (worldFolder == null) {
                return false;
            }

            // Verificar si el archivo existe en las carpetas del mundo
            String rel = link.path.substring("structures/".length());
            File candidate1 = new File(worldFolder, "generated/minecraft/structures/" + rel);
            File candidate2 = new File(worldFolder, "generated/structures/" + rel);

            return candidate1.exists() || candidate2.exists();
        } catch (Exception e) {
            return false;
        }
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