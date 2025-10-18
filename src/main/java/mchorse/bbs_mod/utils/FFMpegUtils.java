package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FFMpegUtils
{
    /**
     * People usually are not bright enough, even though everything is stated
     * in the tutorial, they still manage to specify either wrong path to ffmpeg, or
     * they specify the path to the folder...
     *
     * This little method should simplify their lives!
     */
    private static File findFFMPEG(String path)
    {
        File file = new File(path);
        boolean isWin = OS.CURRENT == OS.WINDOWS;

        if (file.isDirectory())
        {
            String subpath = isWin ? "ffmpeg.exe" : "ffmpeg";
            File bin = new File(file, subpath);

            if (bin.isFile())
            {
                return bin;
            }

            bin = new File(file, "bin" + File.pathSeparator + subpath);

            if (bin.isFile())
            {
                return bin;
            }
        }
        else if (isWin && !file.exists())
        {
            File exe = new File(path + ".exe");

            if (exe.exists())
            {
                return exe;
            }
        }

        return file;
    }

    public static File getFFMPEG()
    {
        File encoder = new File(BBSSettings.videoEncoderPath.get());
        File encoderPath = findFFMPEG(BBSSettings.videoEncoderPath.get());

        if (encoderPath.isFile())
        {
            encoder = encoderPath;
        }

        return encoder;
    }

    public static boolean checkFFMPEG()
    {
        return execute(BBSMod.getGameFolder(), "-version");
    }

    public static boolean execute(File folder, String... arguments)
    {
        List<String> args = new ArrayList<String>();

        args.add(getFFMPEG().getAbsolutePath());

        for (String arg : arguments)
        {
            args.add(arg);
        }

        ProcessBuilder builder = new ProcessBuilder(args);
        File log = BBSMod.getSettingsPath("converter.log");

        builder.directory(folder);
        builder.redirectErrorStream(true);
        builder.redirectOutput(log);

        try
        {
            Process start = builder.start();

            return start.waitFor() == 0;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }
}