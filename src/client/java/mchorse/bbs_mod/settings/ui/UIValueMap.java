package mchorse.bbs_mod.settings.ui;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.value.ValueKeyCombo;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.ui.ValueLanguage;
import mchorse.bbs_mod.settings.values.ui.ValueVideoSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIKeybind;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UILabelOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.ui.utils.Label;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.FFMpegUtils;
import mchorse.bbs_mod.utils.OS;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UIValueMap
{
    private static Map<Class<? extends BaseValue>, IUIValueFactory<? extends BaseValue>> factories = new HashMap<>();

    static
    {
        register(ValueBoolean.class, (value, ui) ->
        {
            UIToggle toggle = UIValueFactory.booleanUI(value, null);

            toggle.resetFlex();

            return Arrays.asList(toggle);
        });

        register(ValueDouble.class, (value, ui) ->
        {
            UITrackpad trackpad = UIValueFactory.doubleUI(value, null);

            trackpad.w(90);

            return Arrays.asList(UIValueFactory.column(trackpad, value));
        });

        register(ValueFloat.class, (value, ui) ->
        {
            UITrackpad trackpad = UIValueFactory.floatUI(value, null);

            trackpad.w(90);

            return Arrays.asList(UIValueFactory.column(trackpad, value));
        });

        register(ValueInt.class, (value, ui) ->
        {
            if (value.getSubtype() == ValueInt.Subtype.COLOR || value.getSubtype() == ValueInt.Subtype.COLOR_ALPHA)
            {
                UIColor color = UIValueFactory.colorUI(value, null);

                color.w(90);

                return Arrays.asList(UIValueFactory.column(color, value));
            }
            else if (value.getSubtype() == ValueInt.Subtype.MODES)
            {
                UICirculate button = new UICirculate(null);

                for (IKey key : value.getLabels())
                {
                    button.addLabel(key);
                }

                button.callback = (b) -> value.set(button.getValue());
                button.setValue(value.get());
                button.w(90);

                return Arrays.asList(UIValueFactory.column(button, value));
            }

            UITrackpad trackpad = UIValueFactory.intUI(value, null);

            trackpad.w(90);

            return Arrays.asList(UIValueFactory.column(trackpad, value));
        });

        register(ValueLanguage.class, (value, ui) ->
        {
            UIButton button = new UIButton(UIKeys.LANGUAGE_PICK, (b) ->
            {
                List<Label<String>> labels = BBSModClient.getL10n().getSupportedLanguageLabels();
                UILabelOverlayPanel<String> panel = new UILabelOverlayPanel<>(UIKeys.LANGUAGE_PICK_TITLE, labels, (str) -> value.set(str.value));

                panel.set(value.get());
                UIOverlay.addOverlay(ui.getContext(), panel);
            });

            button.w(90);

            UIText credits = new UIText().text(UIKeys.LANGUAGE_CREDITS).updates();

            return Arrays.asList(UIValueFactory.column(button, value), credits.marginBottom(8));
        });

        register(ValueLink.class, (value, ui) ->
        {
            UIButton pick = new UIButton(UIKeys.TEXTURE_PICK_TEXTURE, (button) ->
            {
                UITexturePicker.open(ui.getContext(), value.get(), value::set);
            });

            pick.w(90);

            return Arrays.asList(UIValueFactory.column(pick, value));
        });

        register(ValueString.class, (value, ui) ->
        {
            UITextbox textbox = UIValueFactory.stringUI(value, null);

            textbox.w(90);

            if (value == BBSSettings.videoEncoderPath && OS.CURRENT == OS.WINDOWS)
            {
                textbox.context((menu) ->
                {
                    menu.action(Icons.SEARCH, UIKeys.GENERAL_FFMPEG_FIND, () ->
                    {
                        textbox.getContext().replaceContextMenu((submenu) ->
                        {
                            File[] files = File.listRoots();
                            File file = files.length == 0 ? new File("C:\\") : files[0];
                            Optional<Path> ffmpeg = FFMpegUtils.findFFMpeg(file.toPath());

                            if (ffmpeg.isPresent())
                            {
                                Path path = ffmpeg.get();
                                String pathString = path.toAbsolutePath().toString();

                                submenu.action(Icons.VIDEO_CAMERA, IKey.constant(pathString), () ->
                                {
                                    textbox.setText(pathString);
                                    value.set(pathString);
                                });
                            }
                        });
                    });
                });
            }

            return Arrays.asList(UIValueFactory.column(textbox, value));
        });

        register(ValueKeyCombo.class, (value, ui) ->
        {
            UILabel label = UI.label(value.get().label, 0).labelAnchor(0, 0.5F);
            UIKeybind keybind = new UIKeybind(value::set).mouse().escape();

            keybind.setKeyCombo(value.get());
            keybind.w(100);

            return Arrays.asList(UI.row(label, keybind).tooltip(value.get().label));
        });

        register(ValueVideoSettings.class, (value, ui) ->
        {
            UIButton button = new UIButton(UIKeys.VIDEO_SETTINGS_EDIT, (b) ->
            {
                UIOverlay.addOverlay(ui.getContext(), new UIVideoSettingsOverlayPanel(value));
            });

            return Arrays.asList(button);
        });
    }

    public static <T extends BaseValue> void register(Class<T> clazz, IUIValueFactory<T> factory)
    {
        factories.put(clazz, factory);
    }

    public static <T extends BaseValue> List<UIElement> create(T value, UIElement element)
    {
        IUIValueFactory<T> factory = (IUIValueFactory<T>) factories.get(value.getClass());

        return factory == null ? Collections.emptyList() : factory.create(value, element);
    }

    public static interface IUIValueFactory <T extends BaseValue>
    {
        public List<UIElement> create(T value, UIElement element);
    }
}