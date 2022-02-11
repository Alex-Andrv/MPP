import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Alexandr Andreev
 */
public class Solution implements MonotonicClock {
    // При чтении выдает последние записанное
    // или то, которое сейчас пишут
    private final RegularInt c1 = new RegularInt(0);
    private final RegularInt c2 = new RegularInt(0);
    private final RegularInt c3 = new RegularInt(0);

    private final RegularInt _c1 = new RegularInt(0);
    private final RegularInt _c2 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {      // с1 с2 с3
        c1.setValue(time.getD1());              //  o  o  o
        c2.setValue(time.getD2());              //  u  o  o
        _c1.setValue(time.getD1());             //  u  u  o
        c3.setValue(time.getD3());              //  u  u  u
        _c2.setValue(time.getD2());
    }

    @NotNull
    @Override
    public Time read() {
        int copy_f2 = _c2.getValue();
        int copy_c3 = c3.getValue();
        int copy_f1 = _c1.getValue();
        int copy_c2 = c2.getValue();
        int copy_c1 = c1.getValue();
        copy_c2 = copy_f1 == copy_c1 ? copy_c2 : 0;
        copy_c3 = copy_f2 == copy_c2 && copy_f1 == copy_c1 ? copy_c3 : 0;
        return new Time(copy_c1, copy_c2, copy_c3);
    }
}
