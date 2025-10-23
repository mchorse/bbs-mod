package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.ui.utils.Area;
import org.joml.Vector2i;

import java.util.List;
import java.util.Stack;

public class BoxPacker
{
    public static Vector2i pack(List<Area> boxes, int padding)
    {
        Stack<Area> areas = new Stack<>();
        int w = getInitialWidth(boxes, padding);
        int finalW = 0;
        int finalH = 0;

        boxes.sort((a, b) -> b.h - a.h);
        areas.add(new Area(0, 0, w, Integer.MAX_VALUE));

        for (Area box : boxes)
        {
            for (int i = areas.size() - 1; i >= 0; i--)
            {
                Area area = areas.get(i);

                if (box.w > area.w || box.h > area.h)
                {
                    continue;
                }

                box.x = area.x;
                box.y = area.y;

                finalH = Math.max(finalH, box.y + box.h);
                finalW = Math.max(finalW, box.x + box.w);

                if (box.w == area.w && box.h == area.h)
                {
                    Area last = areas.pop();

                    if (i < areas.size())
                    {
                        areas.set(i, last);
                    }
                }
                else if (box.h == area.h)
                {
                    area.x += box.w;
                    area.w -= box.w;

                }
                else if (box.w == area.w)
                {
                    area.y += box.h;
                    area.h -= box.h;
                }
                else
                {
                    areas.add(new Area(area.x + box.w, area.y, area.w - box.w, box.h));

                    area.y += box.h;
                    area.h -= box.h;
                }

                break;
            }
        }

        /* Remove padding from boxes and add them to the final area */
        if (padding != 0)
        {
            for (Area glyph : boxes)
            {
                glyph.w -= padding;
                glyph.h -= padding;
                glyph.x += padding;
                glyph.y += padding;
            }

            finalW += padding;
            finalH += padding;
        }

        return new Vector2i(finalW, finalH);
    }

    private static int getInitialWidth(List<Area> glyphs, int padding)
    {
        int totalArea = 0;
        int maxW = 0;

        for (Area box : glyphs)
        {
            box.w += padding;
            box.h += padding;

            totalArea += box.w * box.h;
            maxW = Math.max(maxW, box.w);
        }

        return (int) Math.max(Math.ceil(Math.sqrt(totalArea)), maxW);
    }
}