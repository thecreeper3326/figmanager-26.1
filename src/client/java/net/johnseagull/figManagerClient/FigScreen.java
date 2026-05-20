package net.johnseagull.figManagerClient;


import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.johnseagull.figManager.Fig;
import net.johnseagull.figManager.FigGroup;
import net.johnseagull.figManager.FigManager;
import net.johnseagull.figManager.FigPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;

import static java.lang.Math.*;
import static net.johnseagull.figManagerClient.FigManagerClient.clientLogger;
import static org.apache.logging.log4j.core.util.ReflectionUtil.setFieldValue;
@SuppressWarnings("unchecked")
public class FigScreen<T extends AbstractWidget & Renderable> extends Screen {
    public boolean canSave = true;

    public List<String> thingsKeepingYouFromSaving = new ArrayList<>();
    private static int howFarYouveScrolled = 0;
    private static int amountOfWidgetsOnScreen = 0;
    private static Field[] fieldsInFigs;
    private List<T> coolListOfOptionWidgets = Lists.newArrayList();
    private final Screen parent;
    private int widthOfTheWidget;
    public float widthRatioFloat;
    public static Map<String, Map<String,Object>> tempOptions = new HashMap<>();
    public static Map<String, List<String>> tempStringList =  new HashMap<>();


    FigButton save = new FigButton(width - 90, 7, 80, 20, Component.literal("Save"), btn -> {
        try {
            save(coolListOfOptionWidgets);
        } catch (IllegalAccessException e) {
            clientLogger.error(e.getMessage());
        }
    }, 0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 2);

    private static final int tw = 6;
    int h;
    int a;
    FigBox thumb = new FigBox(
            width - 8,
            35,
            6,
            max(20, (int) ((double) height / a * h)), 0xFF606060, 0xFF303030, 0xFF202020, 0xFF000000, 1);

    public FigScreen(Component title, float optionWidth, Object figs, Screen parent) {
        super(title);
        this.parent = parent;
        if (optionWidth < 1f) {
            widthOfTheWidget = (int) (optionWidth * width);
        } else {
            widthOfTheWidget = (int) optionWidth;
        }
        widthRatioFloat = optionWidth;
        Class<?> FIGCLASS = figs.getClass();
        fieldsInFigs = FIGCLASS.getDeclaredFields();
    }

    public void addOptions() throws IllegalAccessException {
        int y = 35;
        int x = 5;
        for (Field field : fieldsInFigs) {
            field.setAccessible(true);
            Object value = field.get(FigManager.FIGS);
            if (!field.getName().equals("instance")) {
                if (value instanceof FigGroup t) {
                    int c = t.columns;
                    int w = (width - (c * 5)) / c;
                    int widthOfTheWidgetw = (int) (w * t.ratio);
                    int wc = 1;
                    int wx;

                    List<String> tc = t.value;
                    for (String s : tc) {

                        wx = 5 + (wc - 1) * w;
                        for (Field f : fieldsInFigs) {
                            if (f.getName().equals(s)) {
                                try {
                                    Field v = FigManager.FIGS.getClass().getField(s);
                                    Object vv = v.get(FigManager.FIGS);
                                    StringWidget label;
                                    if (vv instanceof Fig k ) {
                                        if (t.hL) {
                                            label = new StringWidget(wx + (widthOfTheWidgetw + 5), y + 22, w - widthOfTheWidgetw - 5, 20, Component.literal(k.name), font);
                                       } else {
                                            label = new StringWidget(wx, y + 22, w - 5, 20, Component.literal(k.name), font);
                                        }
                                        label.setTooltip(Tooltip.create(Component.literal(k.description)));
                                        this.addRenderableWidget(label);

                                        addWidget(f.getName(),k,wx,y+22,widthOfTheWidgetw,20);
                                    }
                                } catch (Exception e) {
                                    clientLogger.debug(e.getMessage());
                                }
                            }

                        }

                        wc++;
                        if (!t.hL) {
                            if (wc > c) {
                                y += 44;
                                wc = 1;
                                amountOfWidgetsOnScreen += 2;
                            }
                        } else {
                            if (wc > c) {
                                y += 22;
                                wc = 1;
                                amountOfWidgetsOnScreen++;
                            }
                        }
                        wx = 5 + (wc - 1) * w;
                    }
                    if (wc > 1) {
                        y += 22;
                        amountOfWidgetsOnScreen++;
                    }


                }
                if (value instanceof Fig.DividerFig t) {
                    y += 22;
                    if (field.getType() == Fig.DividerFig.class) {
                        Style style = Style.EMPTY;
                        style = style.withColor(t.color).withBold(t.bold).withItalic(t.italic).withUnderlined(t.underline);

                        if (!t.multiline) {
                            StringWidget label = new StringWidget(x, y, width - 30, 20, Component.literal(t.value).withStyle(style), font);
                            label.active = false;
                            this.addRenderableWidget(label);
                        }
                        if (t.multiline) {
                            StringWidget label = new StringWidget(x, y - 5, width - 30, 20, Component.literal(t.value).withStyle(style), font);
                            label.active = false;
                            this.addRenderableWidget(label);
                            StringWidget label2 = new StringWidget(x, y + 5, width - 30, 20, Component.literal(t.value2).withStyle(style), font);
                            label2.active = false;
                            this.addRenderableWidget(label2);
                        }
                        amountOfWidgetsOnScreen++;
                    }
                }
                if (value instanceof Fig t && !t.rendered) {
                    y+=22;
                    addWidget(field.getName(),t,x,y,widthOfTheWidget,20);
                    addLabel(t,y);
                }

                {
                    //map + list figs are a work in progress and are super broken at the moment
                }
//                if (value instanceof Fig.MapFig t) {
//                    Map<String, Object> tempV;
//
//                    if (!temp.containsKey(t.id)) {
//                        tempV = new HashMap<>();
//                        if (t.value != null) {
//                            tempV.putAll(t.value);
//                        }
//                        temp.put(t.id, tempV);
//                    } else {
//                        tempV = new HashMap<>(temp.get(t.id));
//                        temp.put(t.id, tempV);
//                    }
//
//                    y += 22;
//                    StringWidget label = new StringWidget(x, y, width - 120, 20, Component.literal(t.name), font);
//                    label.setTooltip(Tooltip.create(Component.literal(t.description)));
//                    this.addRenderableWidget(label);
//
//                    EditBox k = new EditBox(font, x, y+22, (width/2), 20, Component.literal(field.getName()+"%k"));
//                    EditBox v = new EditBox(font, x+(width/2)+10, y+22, (width/2)-30, 20, Component.literal(field.getName()+"%v"));
//
//                    FigButton remove = new FigButton(width-65,y+2,50,16,Component.literal("Remove"), button -> {
//                        tempV.remove(k.getValue());
//                    },0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 2);
//                    remove.inList = true;
//                    this.addRenderableWidget(remove);
//                    this.addRenderableWidget(k);
//                    this.addRenderableWidget(v);
//
//                    if (t.itemType.equals("string")) {
//                        FigButton set = new FigButton(width-115,y+2,45,16,Component.literal("Set"), button -> {
//                            if (!k.getValue().equals("")) {
//                                tempV.put(k.getValue(),v.getValue());
//                            }
//                        },0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 2);
//
//                        set.inList = true;
//                        this.addRenderableWidget(set);
//                        y+=22;
//                    }
//                    if (t.itemType.equals("int")) {
//                        FigButton set = new FigButton(width-115,y+2,45,16,Component.literal("Set"), button -> {
//                            if (!k.getValue().equals("")) {
//                                tempV.put(k.getValue(),Integer.parseInt(v.getValue()));
//                            }
//                        },0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 2);
//
//                        set.inList = true;
//                        this.addRenderableWidget(set);
//                    }
//                    if (t.itemType.equals("float")) {
//                        FigButton set = new FigButton(width-115,y+2,45,16,Component.literal("Set"), button -> {
//                            if (!k.getValue().equals("")) {
//                                tempV.put(k.getValue(),Float.parseFloat(v.getValue()));
//                            }
//                        },0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 2);
//
//                        set.inList = true;
//                        this.addRenderableWidget(set);
//                    }
//                    if (t.itemType.equals("boolean")) {
//                        FigButton set = new FigButton(width-115,y+2,45,16,Component.literal("Set"), button -> {
//                            if (!k.getValue().equals("")) {
//                                tempV.put(k.getValue(),Boolean.getBoolean(v.getValue()));
//                            }
//                        },0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 2);
//
//                        set.inList = true;
//                        this.addRenderableWidget(set);
//                    }
//                    y+=22;
//
//                    FigList a = new FigList(x, y, width-10, t.dispLength*22, Component.empty(),2);
//
//                    for (Map.Entry<String, Object> entry : tempV.entrySet()) {
//                        a.addWidget(entry.getKey(),entry.getValue().toString());
//                    }
//                    amountOfWidgetsOnScreen += t.dispLength+2;
//                    this.addRenderableWidget(a);
//                }

//                if (value instanceof Fig.ListFig t) {
//
//                    List<String> tempV;
//                    if (!tempString.containsKey(t.id)) {
//                        tempV = new ArrayList<>(t.value);
//                        tempString.put(t.id, tempV);
//                    } else {
//                        tempV = tempString.get(t.id);
//                    }
//                    y += 22;
//                    FigBox bg = new FigBox(0, y, width, 44+(t.dispLength*22), 0xFF202020, 0xFF070707);
//                    bg.active = false;
//                    bg.inList = true;
//                    StringWidget label = new StringWidget(x, y, width - 170, 20, Component.literal(t.name), font);
//                    label.setTooltip(Tooltip.create(Component.literal(t.description)));
//                    this.addRenderableWidget(label);
//                    EditBox k = new EditBox(font, x, y+22, ((width-14))-5, 20, Component.literal(field.getName()+"%k"));
//
//
//                    FigList a = new FigList(x, y+44, width-14, t.dispLength*22, Component.empty(),1);
//                    for (String entry : tempV) {
//                        a.addWidget(entry);
//                    }
//                    a.active = false;
//                    FigButton clear = new FigButton(width-65,y+2,50,16,Component.literal("Clear"), button -> {
//                        tempV.clear();
//                        a.clearWidgets();
//                    },0xFFA05050, 0xFF301110, 0xFF151515, 0xFF000000, 1);
//                    clear.inList = true;
//                    clear.setTooltip(Tooltip.create(Component.literal("Clears the whole list, be careful!").withStyle(ChatFormatting.RED)));
//                    clear.setTooltipDelay(Duration.ofMillis(200));
//                    FigButton remove = new FigButton(width-115,y+2,50,16,Component.literal("Remove"), button -> {
//                        tempV.remove(k.getValue());
//                        a.clearWidgets();
//                        for (String entry : tempV) {
//                            a.addWidget(entry);
//                        }
//                    },0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 1);
//
//                    remove.inList = true;
//                    this.addRenderableWidget(clear);
//                    this.addRenderableWidget(remove);
//                    this.addRenderableWidget(k);
//
//                    FigButton set = new FigButton(width-165,y+2,50,16,Component.literal("Set"), button -> {
//                    },0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 1);
//                    set.inList = true;
//                    this.addRenderableWidget(set);
//                    y+=22;
//
//                    if (t.itemType.equals("string")) {
//                        set.onPress = button -> {
//                            if (!k.getValue().equals("")) {
//                                tempV.add(k.getValue());
//                                a.clearWidgets();
//                                for (Object entry : tempV) {
//                                    a.addWidget(entry.toString());
//                                }
//                            }
//                        };
//                    }
//                    y+=t.dispLength*22;
//
//                    amountOfWidgetsOnScreen += t.dispLength+2;
//                    this.addRenderableWidget(a);
//                
                
            }
        }


    }
    public void setResponder(EditBox box, String type, Object min, Object max, String name) {
        box.setResponder(e -> {

            try {
                if (type.equals("int")) {
                    int intMin = (int) min;
                    int intMax = (int) max;
                    if (Integer.parseInt(box.getValue()) > intMax || Integer.parseInt(box.getValue()) < intMin) {
                        box.setTextColor(0xFFFF2020);
                        if (!thingsKeepingYouFromSaving.contains(name)) {
                            thingsKeepingYouFromSaving.add(name);
                        }
                    } else {
                        box.setTextColor(0xFFFFFFFF);
                        thingsKeepingYouFromSaving.remove(name);
                    }
                }
                if (type.equals("float")) {
                    float floatMin = (float) min;
                    float floatMax = (float) max;
                    if (Float.parseFloat(box.getValue()) > floatMax || Float.parseFloat(box.getValue()) < floatMin) {
                        box.setTextColor(0xFFFF2020);
                        if (!thingsKeepingYouFromSaving.contains(name)) {
                            thingsKeepingYouFromSaving.add(name);
                        }
                    } else {
                        box.setTextColor(0xFFFFFFFF);
                        thingsKeepingYouFromSaving.remove(name);
                    }
                }
                if (type.equals("string")) {
                    int stringMax = (int) max;
                    box.setResponder(s -> {
                        if (s.length() > stringMax) {
                            box.setTextColor(0xFFFF2020);
                            if (!thingsKeepingYouFromSaving.contains(name)) {
                                thingsKeepingYouFromSaving.add(name);
                            }
                        } else {
                            box.setTextColor(0xFFFFFFFF);
                            thingsKeepingYouFromSaving.remove(name);

                        }
                    });
                }

            } catch (Exception ex) {
                box.setTextColor(0xFFFF2020);
            }
        });
    }

    public void addWidget(String name, Fig fig, int x, int y, int w, int h) {
        if (!fig.rendered) {
            if (fig.widgetType.equals("box")) {

                EditBox box = new EditBox(font, x, y, w, h, Component.literal(name));
                box.setMessage(Component.literal(fig.dataType + name));
                box.setMaxLength(1024);
                if (fig.dataType.equals("int_")) {
                    Fig.IntFig t = (Fig.IntFig) fig;
                    box.setValue(String.valueOf(t.value));
                    setResponder(box, "int", t.min, t.max, name);
                    coolListOfOptionWidgets.add((T) box);
                }
                if (fig.dataType.equals("float_")) {
                    Fig.FloatFig t = (Fig.FloatFig) fig;
                    box.setValue(String.valueOf(t.value));
                    box.setMessage(Component.literal(t.dataType + name));
                    setResponder(box, "float", t.min, t.max, name);
                    coolListOfOptionWidgets.add((T) box);
                }
                if (fig.dataType.equals("string_")) {
                    assert fig instanceof Fig.StringFig;
                    Fig.StringFig t = (Fig.StringFig) fig;
                    box.setValue(t.value);
                    setResponder(box,"string",0,t.max,name);
                    box.setMessage(Component.literal(t.dataType+name));
                    coolListOfOptionWidgets.add((T) box);
                }
                this.addRenderableWidget(box);
                fig.rendered = true;
            }
            if (fig.widgetType.equals("check")) {
                Fig.BooleanFig t = (Fig.BooleanFig) fig;
                FigCheckbox toggle = FigCheckbox.builder(Component.literal(name), font).selected(t.value).build();
                toggle.setX(x);
                toggle.setY(y+5);
                toggle.w = w;
                toggle.col1 = 0xFFA0A0A0; toggle.col2 = 0xFF505050; toggle.bdr1 = 0xFF151515; toggle.bdr2 = 0xFF000000;
                coolListOfOptionWidgets.add((T) toggle);
                this.addRenderableWidget(toggle);
                fig.rendered = true;
            }
            amountOfWidgetsOnScreen++;
        }
    }
    public void addLabel(Fig t, int y) {
        StringWidget label = new StringWidget(round(widthOfTheWidget) + 10, y, width - round(widthOfTheWidget) - 30, 20, Component.literal(t.name), font);
        label.setTooltip(Tooltip.create(Component.literal(t.description)));
        this.addRenderableWidget(label);
    }

    public void save(List<T> options) throws IllegalAccessException {
        Field[] fields = FigManager.FIGS.getClass().getDeclaredFields();
        for (T option : options) {
            for (Field field : fields) {
                try {
                    Object value = field.get(FigManager.FIGS);
//                    if (value instanceof Fig.MapFig f) {
//                        Fig.MapFig tempFig = new Fig.MapFig(
//                                f.name,
//                                f.description,
//                                f.maxLength,
//                                f.dispLength,
//                                f.itemType,
//                                f.key,
//                                f.valueString,
//                                f.keyDesc,
//                                f.valueDesc
//                        );
//                        tempFig.value = temp.get(f.id);
//                        setFieldValue(field, FigManager.FIGS, tempFig);
//                            clientLogger.error(f.id);
//                            clientLogger.error(f.value.toString());
//
//
//                    }
//                    if (value instanceof Fig.ListFig f) {
//
//
//                        clientLogger.error(tempString.get(f.id).toString());
//                        Fig.ListFig tempFig = new Fig.ListFig(
//                                f.name,
//                                f.description,
//                                f.maxLength,
//                                f.dispLength,
//                                f.itemType,
//                                f.key,
//                                f.valueString,
//                                f.keyDesc,
//                                f.valueDesc
//                        );
//                        tempFig.value = tempString.get(f.id);
//                        setFieldValue(field, FigManager.FIGS, tempFig);
//
//                        clientLogger.error(f.id);
//                        clientLogger.error(f.value.toString());
//                    }
                    if (option instanceof EditBox) {
                        try {
                            String msg = option.getMessage().getString();

                            if (value instanceof Fig.IntFig f && msg.equals(f.dataType + field.getName())) {
                                setFieldValue(field, FigManager.FIGS, new Fig.IntFig(f.name, f.description, Integer.parseInt(((EditBox) option).getValue()), f.min, f.max));
                                break;
                            }
                            if (value instanceof Fig.FloatFig f && msg.equals(f.dataType + field.getName())) {
                                setFieldValue(field, FigManager.FIGS, new Fig.FloatFig(f.name, f.description, Float.parseFloat(((EditBox) option).getValue()), f.min, f.max));
                                break;
                            }
                            if (value instanceof Fig.StringFig f && msg.equals(f.dataType + field.getName())) {
                                setFieldValue(field, FigManager.FIGS, new Fig.StringFig(f.name, f.description, ((EditBox) option).getValue(), f.max));
                                break;
                            }
                        } catch (NumberFormatException e) {
                            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Format error for " + option.getMessage()).withStyle(ChatFormatting.RED));
                        }
                    }
                    if (option instanceof FigCheckbox) {
                        String msg = option.getMessage().getString();

                        if (value instanceof Fig.BooleanFig f && msg.equals(field.getName()))  {
                            if (field.getType() == Fig.BooleanFig.class) {
                                setFieldValue(field, FigManager.FIGS, new Fig.BooleanFig(f.name, f.description, ((FigCheckbox) option).selected()));
                                break;
                            }
                        
                        }
                    }
                } catch (IllegalAccessException | NullPointerException _) {
                }


            }
        }
        List<Object> newFigs = FigManager.validate(FigManager.FIGS);
        Object correctedFigs = newFigs.get(0);
        int errorCount = (int) newFigs.get(1);
        List<String> errors = (List<String>) newFigs.get(2);

        if (errorCount != 0) {
            this.minecraft.getToastManager().addToast(SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.literal("Error").withStyle(ChatFormatting.RED), Component.nullToEmpty(errorCount + " errors occurred, see chat/logs")));
        }
        try {
            if (errorCount != 0) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(errorCount + " options failed to process:").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                clientLogger.error(errorCount + " errors occured:");
                for (String error : errors) {
                    clientLogger.error(error);
                }
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(""));
                for (String error : errors) {
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal(error).withStyle(ChatFormatting.RED));
                }

                Minecraft.getInstance().player.sendSystemMessage(Component.literal(""));
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Figs that were invalid were reset.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else {
                this.onClose();
            }
            ClientPlayNetworking.send(
                    new FigPacket(FigManager.toString(correctedFigs))
            );
            FigManager.FIGS = correctedFigs;
            FigManager.save(FigManager.name);
        } catch (IllegalStateException | NullPointerException _) { // person is in main menu
            if (errorCount != 0) {
                this.minecraft.getToastManager().addToast(SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.literal("Warning").withStyle(ChatFormatting.YELLOW), Component.nullToEmpty("Saved valid figs, invalid ones were reset")));
            } else {
                this.onClose();
            }
            //put them in a local copy if evrrything goes wrong
            FigManager.FIGS = correctedFigs;
            FigManager.save(FigManager.name);
        }
        tempStringList.clear();
    }

    protected void init() {
        tempStringList.clear();
        for (Field field : fieldsInFigs) {
            field.setAccessible(true);
            try {
                Object value = field.get(FigManager.FIGS);
                if (value instanceof Fig f) {
                    f.rendered = false;
                }
            } catch (Exception e) {
                clientLogger.error("Resetting fig render states failed, may fail to render on next open.");
            }
        }
        if (widthRatioFloat < 1f) {
            widthOfTheWidget = (int) (widthRatioFloat * width);
        } else {
            widthOfTheWidget = (int) widthRatioFloat;
        }
        amountOfWidgetsOnScreen = 0;
        howFarYouveScrolled = 0;
        this.addRenderableWidget(new FigBox(0, 0, width, height / 2, 0xFF000000, 0xAA000000)).active = false;
        this.addRenderableWidget(new FigBox(0, height / 2, width, height / 2, 0xAA000000, 0xFF000000)).active = false;
        try {
            addOptions();

        } catch (IllegalAccessException e) {
        }
        this.addRenderableWidget(new FigBox(0, height - 35, width, 15, 0x00000000, 0xAA000000)).active = false;
        this.addRenderableWidget(new FigBox(0, 35, width, 15, 0xAA000000, 0x00000000)).active = false;
        this.addRenderableWidget(new FigBox(width - 10, 0, 10, height, 0xFF000000, 0xFF101010)).active = false;
        this.addRenderableWidget(new FigBox(0, 0, width, 35, 0xFF202020, 0xFF070707)).active = false;
        this.addRenderableWidget(new FigBox(0, height - 20, width, 20, 0xFF202020, 0xFF070707)).active = false;

        int w = width;

        FigButton close = new FigButton(w - 90, 7, 80, 20, Component.literal("Discard"), btn -> {
            tempOptions.clear();

            tempStringList.clear();
            onClose();
        }, 0xFFA05050, 0xFF301110, 0xFF151515, 0xFF000000, 2);
        close.setX(width - 100 - close.getWidth());
        this.addRenderableWidget(close);
        thumb = new FigBox(
                width - 8,
                35,
                6,
                10, 0xFF606060, 0xFF303030, 0xFF202020, 0xFF000000, 1);
        this.addRenderableWidget(thumb);
        save = new FigButton(width - 90, 7, 80, 20, Component.literal("Save"), btn -> {
            try {
                save(coolListOfOptionWidgets);
            } catch (IllegalAccessException e) {
                clientLogger.error(e.getMessage());
            }
        }, 0xFFA0A0A0, 0xFF505050, 0xFF151515, 0xFF000000, 2);
        this.addRenderableWidget(save);
        StringWidget title = new StringWidget(10, 10, 1000, 15, Component.literal(FigManager.name + " - Fig menu"), font);
        StringWidget subtitle = new StringWidget(10, height - 17, 1000, 15, Component.literal(FigManager.name + FigManager.version).withStyle(ChatFormatting.GRAY), font);
        StringWidget credit = new StringWidget(width - 100, height - 17, 1000, 15, Component.literal("TheCreeper3326").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), font);
        title.active = true;
        subtitle.active = true;
        credit.active = true;
        this.addRenderableWidget(credit);
        this.addRenderableWidget(title);
        this.addRenderableWidget(subtitle);



    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {


        boolean listHovered = false;
        for (Object child : this.children()) {
            if (child instanceof FigList<?> widget) {
                if(widget.hovered){
                    listHovered = true;
                    widget.shift(scrollY);
                }
            }
        }
        int max = (amountOfWidgetsOnScreen * 22) - (height - 55) + 40;
        if (!listHovered) {
            if (scrollY < 0 && abs(howFarYouveScrolled) < max) {
                shift((int) scrollY * 22);
            } else if (scrollY > 0 && howFarYouveScrolled < 0) {
                shift((int) scrollY * 22);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int max = (amountOfWidgetsOnScreen * 22) - (height - 55) + 40;
        if (event.isDown() && abs(howFarYouveScrolled) < max) {
            shift(22);
        } else if (event.isUp() && howFarYouveScrolled < 0) {
            shift(-22);
        }
        return super.keyPressed(event);
    }

    public void shift(int offset) {

        howFarYouveScrolled += offset;
        for (Object child : this.children()) {
            if (child instanceof EditBox widget) {
                widget.setY((widget.getY() + offset));
            }
            if (child instanceof FigCheckbox widget) {
                widget.setY((widget.getY() + offset));
            }
            if (child instanceof StringWidget widget) {
                if (!widget.active) {
                    widget.setY((widget.getY() + offset));
                }
            }
            if (child instanceof FigButton widget) {
                if (widget.inList) {
                    widget.setY((widget.getY() + offset));
                }
            }
            if (child instanceof FigToggleButton widget) {
                if (widget.inList) {
                    widget.setY((widget.getY() + offset));
                }
            }
            if (child instanceof FigList<?> widget) {
               widget.Y += offset;
            }
            if (child instanceof FigBox widget) {
                if(widget.inList){
                    widget.y += offset;
                }
            }


            int max = (amountOfWidgetsOnScreen * 22) - (height - 55) + 40;
            float progress = ((float) howFarYouveScrolled / max);
            int trackSpace = (height - 55) - thumb.height;
            thumb.y = 35 - (int) (min(progress, 1.0f) * trackSpace);
        }
    }


    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {




        if (thingsKeepingYouFromSaving.isEmpty()) {
            save.col1 = 0xFFA0A0A0;
            save.col2 = 0xFF505050;
            save.setTooltip(Tooltip.create(Component.literal("")));
            save.active = true;
        } else {
            save.col1 = 0x50A0A0A0;
            save.col2 = 0x50505050;
            save.setTooltip(Tooltip.create(Component.literal("Invalid Fig Values, check the following: "+ thingsKeepingYouFromSaving.toString())));
            save.active = false;
        }
        for (Object c : this.children()) {
            if (c instanceof StringWidget w) {
                if (!w.active) {
                    if (w.getY()<20) {
                        w.setTooltipDelay(Duration.ofDays(100000));
                    } else {
                        w.setTooltipDelay(Duration.ofMillis(0));
                    }

                }
            }
            if (c instanceof EditBox w) {
                if (w.getY()<30) {
                    w.visible = false;
                } else {
                    w.visible = true;
                }

            }
            if (c instanceof FigCheckbox w) {
                if (w.getY()<30) {
                    w.visible = false;
                } else {
                    w.visible = true;
                }

            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);

    }
}
