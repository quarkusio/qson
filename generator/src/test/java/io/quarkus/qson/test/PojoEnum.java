package io.quarkus.qson.test;

import java.util.List;
import java.util.Map;

public class PojoEnum {
    Color color;
    List<Color> colorList;
    Map<String, Color> colorMap;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public List<Color> getColorList() {
        return colorList;
    }

    public void setColorList(List<Color> colorList) {
        this.colorList = colorList;
    }

    public Map<String, Color> getColorMap() {
        return colorMap;
    }

    public void setColorMap(Map<String, Color> colorMap) {
        this.colorMap = colorMap;
    }
}
