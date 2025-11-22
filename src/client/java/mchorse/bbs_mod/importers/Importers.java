package mchorse.bbs_mod.importers;

import mchorse.bbs_mod.importers.types.GIFImporter;
import mchorse.bbs_mod.importers.types.IImporter;
import mchorse.bbs_mod.importers.types.OldSkinImporter;
import mchorse.bbs_mod.importers.types.PNGImporter;
import mchorse.bbs_mod.importers.types.ToPNGImporter;
import mchorse.bbs_mod.importers.types.ToWAVImporter;
import mchorse.bbs_mod.importers.types.WAVImporter;
import mchorse.bbs_mod.importers.types.StructureImporter;
import mchorse.bbs_mod.ui.UIKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry for file importers that are being dragged into the folder.
 *
 * Following importers would be nice to have:
 *
 * - Gif to sequence of PNGs
 * - mp3/mp4/flac/aiff to wav
 * - PNG 1.7 skin to PNG 1.8 skin
 * - Models (as folders, as multiple files, etc.)
 * - Jpeg to PNG
 * - PNG copy
 * - WAV to WAV mono
 */
public class Importers
{
    private final static List<IImporter> importers = new ArrayList<>();

    static
    {
        importers.add(new ToPNGImporter(UIKeys.IMPORTER_JPEG, ".jpg", ".jpeg"));
        importers.add(new ToPNGImporter(UIKeys.IMPORTER_WEBP, ".webp"));
        importers.add(new ToWAVImporter(UIKeys.IMPORTER_MPEG, ".mp3", ".mp4"));
        importers.add(new ToWAVImporter(UIKeys.IMPORTER_FLAC, ".flac"));
        importers.add(new ToWAVImporter(UIKeys.IMPORTER_AIFF, ".aiff"));
        importers.add(new ToWAVImporter(UIKeys.IMPORTER_OGG, ".ogg"));
        importers.add(new GIFImporter());
        importers.add(new OldSkinImporter());
        importers.add(new PNGImporter());
        importers.add(new WAVImporter());
        importers.add(new StructureImporter());
    }

    public static List<IImporter> getImporters()
    {
        return Collections.unmodifiableList(importers);
    }
}