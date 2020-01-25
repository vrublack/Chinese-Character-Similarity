package hanzisim;

public class FlatDecomp {
    String comp;
    float centerVertical, centerHorizontal;

    public FlatDecomp(String comp, float left, float right, float top, float bottom) {
        this.comp = comp;
        this.centerVertical = bottom + (top - bottom) / 2;
        this.centerHorizontal = left + (right - left) / 2;
    }
}
